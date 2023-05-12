import cats.implicits._
import com.twilio.Twilio
import com.twilio.`type`.PhoneNumber
import com.twilio.rest.api.v2010.account.Message
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.util._

class TwilioManager(config: Config) extends LazyLogging {

  private val smsEnabled = Try(config.getBoolean("twilio.sms.enabled")).getOrElse(false)
  private val smsDebug   = Try(config.getBoolean("twilio.sms.debug")).getOrElse(false)
  private val accountSid = config.getString("twilio.sms.accountSID")
  private val authToken  = config.getString("twilio.sms.authToken")
  private val fromNumber = config.getString("twilio.sms.fromNumber")

  def sendSms(msg: String, toNumber: String): Either[String, Option[Message]] = {
    Twilio.init(accountSid, authToken)

    (smsEnabled, smsDebug) match {
      case (false, _) =>
        logger.debug("Twilio is disabled")
        Either.right(None)
      case (_, true)  =>
        logger.debug("Twilio is debug")
        Either.right(None)
      case _          =>
        Try {
          logger.debug("sending sms to " + toNumber)
          Message
            .creator(new PhoneNumber(toNumber), new PhoneNumber(fromNumber), msg)
            .create()
        } match {
          case Success(message) =>
            logger.debug(s"Message sent to $toNumber with ID ${message.getSid}")
            Either.right(Some(message))
          case Failure(error)   =>
            logger.debug(s"Twilio failed :${error.getMessage}")
            Either.left(error.getMessage)
        }
    }
  }

}
