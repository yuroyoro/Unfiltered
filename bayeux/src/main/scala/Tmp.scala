package unfiltered.bayeux

import se.scalablesolutions.akka.actor.TypedActor

//joda-time
import org.joda.time.{DateTime, DateTimeZone}

object Tmp{
  def main(args: Array[String]){
    val client = Client("asdf")
    println(client.id)
    Thread.sleep(1000)
    TypedActor.stop(client)
  }
}

case class Message(
        var advice: Map[String, Any] = Map[String, Any](),
        val channel: Channel,
        val client: Option[Client] = None,
        val connectionType: Option[String] = None,
        val data: Option[Map[String, Any]] = Some(Map[String, Any]()),
        val dateTime: Option[DateTime] = Some(new DateTime(DateTimeZone.UTC)),
        val error: Option[String] = None,
        val ext: Option[Map[String, Any]] = None,
        val id: Option[String] = None,
        val isResponse: Boolean = false,
        val minimumVersion: String = "1.0.0",
        val subscription: Option[String] = None,
        val successful: Boolean = false,
        val supportedConnectionTypes: Option[List[String]] = Some(List(Bayeux.LONG_POLLING)),
        val version: String = "1.0.0"
)

object Bayeux{
  val META_SUBSCRIBE = "/meta/subscribe"
  val META_UNSUBSCRIBE = "/meta/unsubscribe"
  val META_HANDSHAKE = "/meta/handshake"
  val META_CONNECT = "/meta/connect"
  val META_DISCONNECT = "/meta/disconnect"
  val VERSION = "1.0"
  val LONG_POLLING = "long-polling"
  val SUPPORTED_CONNECTION_TYPES = List(LONG_POLLING)
  val TIMEOUT = "timeout"
  var TIMEOUT_VALUE = 1000 * 30 * 1 //one minute
  val INTERVAL = "interval"
  val INTERVAL_VALUE = 0
  val RECONNECT = "reconnect"
  val RETRY = "retry"
  val DEFAULT_ADVICE = Map(INTERVAL -> INTERVAL_VALUE, RECONNECT -> RETRY, TIMEOUT -> TIMEOUT_VALUE)

//error codes - don't know if there is a canonical set
  val ERROR_INVALID_VERSION = 401
  val ERROR_UNSUPPORTED_CONNECTION_TYPES = 402
  val ERROR_MISSING_CLIENT_ID = 403
  val ERROR_MISSING_CONNECTION_TYPE = 404
  val ERROR_UNSUPPORTED_CONNECTION_TYPE = 405
  val ERROR_NO_SUBSCRIPTION_SPECIFIED = 406
  val ERROR_NO_CHANNEL_SPECIFIED = 407
  val ERROR_MISSING_DATA_FIELD = 408
  val ERROR_SUBSCRIPTION_TO_META_CHANNEL = 409
  val ERROR_SUBSCRIPTION_TO_SERVICE_CHANNEL = 410
}