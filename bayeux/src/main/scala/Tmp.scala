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
  
  def dispatch(messages: List[Message]): Unit = {
    for(message <- messages){
      message.channel match {
        case META_SUBSCRIBE => subscribe(message)
        case META_UNSUBSCRIBE => unsubscribe(message)
        case META_HANDSHAKE => handshake(message)
        case META_CONNECT => connect(message)
        case META_DISCONNECT => disconnect(message)
        case _ => publish(message)
      }
    }
  }
  
  private def publish(message: Message): Unit = {
    message.channel.publish(message)
    val ack = Message(isResponse = true,
            channel = message.channel,
            successful = true, 
            id = message.id, 
            data = message.data,
            ext = message.ext)
    writeMessage(ack)
  }
    
  private def handshake(message: Message): Unit = {
    if(message.version != Bayeux.VERSION) 
      Bayeux.error(message, List[Int](Bayeux.ERROR_INVALID_VERSION),List[String](message.version), "the version specified is incompatible with this implementation of Bayeux")
    else if(!message.supportedConnectionTypes.get.contains(Bayeux.SUPPORTED_CONNECTION_TYPES.head)) 
      Bayeux.error(message, List[Int](Bayeux.ERROR_UNSUPPORTED_CONNECTION_TYPES), message.supportedConnectionTypes.get, "none of the supported connection types match those supported by this implementation of Bayeux")
    else 
      writeMessage(
        Message(
          channel = Bayeux.META_HANDSHAKE, 
          client = Some(Client()),
          successful = true,
          id = message.id,
          advice = Bayeux.DEFAULT_ADVICE,
          isResponse = true
        )
      )
  }
  
  private def subscribe(message: Message): Unit = {
    if(!message.client.isDefined) missingClient(message)
    else if(!message.subscription.isDefined) writeMessage(error(message,List[Int](Bayeux.ERROR_NO_SUBSCRIPTION_SPECIFIED),List[String](),"no subscription was specified"))
    else if(message.subscription.get.matches("^\\/meta\\/.*")) writeMessage(error(message,List[Int](Bayeux.ERROR_SUBSCRIPTION_TO_META_CHANNEL),List[String](),"you attempted to subscribe to a meta channel"))
    else if(message.subscription.get.matches("^\\/service\\/.*")) writeMessage(error(message, List[Int](Bayeux.ERROR_SUBSCRIPTION_TO_SERVICE_CHANNEL),List[String](),"you attempted to subscribe to a service channel"))
    else{
      message.channel.subscribe(message.client.get)
      writeMessage(Message(
          channel = message.channel, 
          client = message.client,
          successful = true,
          subscription = message.subscription,
          id = message.id,
          isResponse = true))
    }
  }
  private def unsubscribe(message: Message): Unit = {
    if(!message.client.isDefined) missingClient(message)
    else if(!message.subscription.isDefined) writeMessage(error(message,List[Int](Bayeux.ERROR_NO_SUBSCRIPTION_SPECIFIED),List[String](),"no subscription was specified"))
    else{
      Channel(message.subscription.get).unsubscribe(message.client.get)
      writeMessage(Message(channel = message.channel,
          client = message.client,
          successful = true,
          subscription = message.subscription,
          id = message.id,
          isResponse = true)
      )
    }
  }
  
  private def disconnect(message: Message): Unit = {
    if(!message.client.isDefined) missingClient(message)
    else{
      message.client.get.disconnect
      writeMessage(
        Message(channel = message.channel, 
                client = message.client,
                successful = true,
                id = message.id,
                isResponse = true
        )
      )
    }
  }
  
  private def connect(message: Message): Unit = {
    if(!message.client.isDefined) missingClient(message)
    else if(!message.connectionType.isDefined) writeMessage(error(message,List[Int](Bayeux.ERROR_MISSING_CONNECTION_TYPE), List[String](null), "a connectionType was not specified"))
    else if(!Bayeux.SUPPORTED_CONNECTION_TYPES.contains(message.connectionType.get)) writeMessage(error(message, List[Int](Bayeux.ERROR_UNSUPPORTED_CONNECTION_TYPE), List[String](message.connectionType.get),"the connectionType specified is unsupported"))
    else message.client.get.enqueue(message, None)
  }
  
  private def missingClient(message: Message) = {
    writeMessage(error(message,
      List[Int](Bayeux.ERROR_MISSING_CLIENT_ID),
      List[String](null),
      "either a clientId was not sent, or it was not found"))
  }
  
  private[bayeux] def error(message: Message, codes: List[Int], args: List[String], copy: String): Message = {
    Message(isResponse = true,
      channel = message.channel,
      id = message.id,
      successful = false,
      error = Some(String.format("%s:%s:%s", codes.mkString(" "), args.mkString(","), copy))
    )
  }
  
  private[bayeux] def writeMessages(messages: List[Message]): Unit = ()
  private[bayeux] def writeMessage(message: Message): Unit = writeMessages(List(message))
  
  
  val META_SUBSCRIBE = Channel("/meta/subscribe")
  val META_UNSUBSCRIBE = Channel("/meta/unsubscribe")
  val META_HANDSHAKE = Channel("/meta/handshake")
  val META_CONNECT = Channel("/meta/connect")
  val META_DISCONNECT = Channel("/meta/disconnect")
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