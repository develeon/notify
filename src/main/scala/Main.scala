import cats.data.NonEmptyList
import cats.effect._
import cats.effect.std.Random
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import scala.io.Source
import scala.language.postfixOps
object Main extends IOApp with LazyLogging {
  private val dataset = "data.txt"
  override def run(args: List[String]): IO[ExitCode] = {
    val conf   = ConfigFactory.parseResources("application.conf")
    val twilio = new TwilioManager(conf)
    for {
      rand       <- Random.scalaUtilRandom[IO]
      data       <- IO(Source.fromResource(dataset).getLines().toList)
      users      <- IO(data.map(row => Candidate.apply(row.split(",").toList)))
      workerList <- IO.fromOption(NonEmptyList.fromList(users.map(bruteforce(_)(rand))))(throw new IllegalStateException("no candidates"))
      result     <- raiseFirstOf(2 minutes, workerList)
      _          <- IO(twilio.sendSms(s"Hei ${result.name}, Du vant søkerflates neste underholdingsbidrag", "+47" + result.mobile.toString))
    } yield result match {
      case BruteforceResult(name, phone) =>
        println(s"$name has mobilenumber $phone notifying")
        ExitCode.Success
      case _                             => ExitCode.Error
    }
  }

  case class BruteforceResult(name: String, mobile: Int)
  def bruteforce(candidate: Main.Candidate)(implicit rand: Random[IO]): IO[Option[BruteforceResult]] = {
    val hint = candidate.getHint
    for {
      _               <- IO(println(s"Thread [${Thread.currentThread().getName}] bruteforcing ${candidate.name} mobilenumber, last digit hint given: $hint"))
      allPhoneNumbers <- rand.shuffleList(List.range(9000000, 10000000) ++ List.range(4000000, 5000000))
      phoneNumber      = allPhoneNumbers.find(suggestion => candidate.checkPhonenumber(suggestion * 10 + hint))
    } yield phoneNumber.map(p => BruteforceResult(candidate.name, p * 10 + hint))
  }

  def raiseFirstOf[T](
    timeout: FiniteDuration,
    workers: NonEmptyList[IO[Option[T]]]
  ): IO[T] = {

    val onlyWorkers: NonEmptyList[IO[T]] = workers.map { ioOpt =>
      ioOpt.flatMap {
        case None        => IO.never
        case Some(value) => IO.pure(value)
      }
    }
    val racePairs: IO[T]                 = onlyWorkers.length match {
      case 1 => onlyWorkers.head
      case _ =>
        onlyWorkers.tail.foldLeft(onlyWorkers.head)((a: IO[T], b: IO[T]) => a.race(b).map(or => or.fold(identity, identity)))
    }
    racePairs.timeout(timeout)
  }

  class Candidate(val name: String, private val mobile: Int) {
    override def toString: String =
      s"$name $mobile"

    def getHint: Int =
      s"$mobile".takeRight(1).toInt

    def checkPhonenumber(suggestion: Int): Boolean =
      suggestion.equals(mobile)
  }

  object Candidate {
    def apply(dataset: List[String]): Candidate =
      dataset match {
        case name :: phonenumber :: _ => new Candidate(name, phonenumber.toInt)
      }
  }
}
