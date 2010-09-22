package unfiltered.bayeux

import se.scalablesolutions.akka.actor.TypedActor

//joda-time
import org.joda.time.{DateTime, DateTimeZone}

// java
import java.io.PrintWriter

import unfiltered.response._

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
  
  private[bayeux] def error(message: Message, codes: List[Int], args: List[String], copy: String)(implicit writer: Option[PrintWriter]): Message = {
    Message(isResponse = true,
      channelName = message.channelName,
      id = message.id,
      successful = false,
      error = Some(String.format("%s:%s:%s", codes.mkString(" "), args.mkString(","), copy))
    )
  }
  
  private[bayeux] def writeMessages(messages: List[Message])(implicit writer: Option[PrintWriter]): Unit = {
    import net.liftweb.json.JsonAST
    import net.liftweb.json.JsonDSL._
    for(message <- messages){
      import Message._
      compact(JsonAST.render(message), writer.get)
    }
  }
  private[bayeux] def writeMessage(message: Message)(implicit writer: Option[PrintWriter]): Unit = writeMessages(List(message))
}

case class Bayeux(message: net.liftweb.json.JsonAST.JValue) extends ResponseWriter{
  
  import Bayeux.error
  
  def write(writer: PrintWriter): Unit = {
    implicit val w = Some(writer)
    dispatch(List(new Message(message)))
  }
  
  private def dispatch(messages: List[Message])(implicit writer: Option[PrintWriter]): Unit = {
    for(message <- messages){
      message.channelName match {
        case Bayeux.META_SUBSCRIBE => subscribe(message)
        case Bayeux.META_UNSUBSCRIBE => unsubscribe(message)
        case Bayeux.META_HANDSHAKE => handshake(message)
        case Bayeux.META_CONNECT => connect(message)
        case Bayeux.META_DISCONNECT => disconnect(message)
        case _ => publish(message)
      }
    }
  }
  
  private def publish(message: Message)(implicit writer: Option[PrintWriter]): Unit = {
    message.channel.publish(message)
    val ack = Message(isResponse = true,
            channelName = message.channelName,
            successful = true, 
            id = message.id, 
            data = message.data,
            ext = message.ext)
    Bayeux.writeMessage(ack)
  }
    
  private def handshake(message: Message)(implicit writer: Option[PrintWriter]): Unit = {
    if(message.version != Bayeux.VERSION) 
      error(message, List[Int](Bayeux.ERROR_INVALID_VERSION),List[String](message.version), "the version specified is incompatible with this implementation of Bayeux")
    else if(!message.supportedConnectionTypes.get.contains(Bayeux.SUPPORTED_CONNECTION_TYPES.head)) 
      Bayeux.error(message, List[Int](Bayeux.ERROR_UNSUPPORTED_CONNECTION_TYPES), message.supportedConnectionTypes.get, "none of the supported connection types match those supported by this implementation of Bayeux")
    else{ 
      val client = Client()
      Bayeux.writeMessage(
        Message(
          channelName = Bayeux.META_HANDSHAKE, 
          clientId = Some(client.id),
          successful = true,
          id = message.id,
          advice = Bayeux.DEFAULT_ADVICE,
          isResponse = true
        )
      )
    }
  }
  
  private def subscribe(message: Message)(implicit writer: Option[PrintWriter]): Unit = {
    if(!message.clientId.isDefined) missingClient(message)
    else if(!message.subscription.isDefined) Bayeux.writeMessage(error(message,List[Int](Bayeux.ERROR_NO_SUBSCRIPTION_SPECIFIED),List[String](),"no subscription was specified"))
    else if(message.subscription.get.matches("^\\/meta\\/.*")) Bayeux.writeMessage(error(message,List[Int](Bayeux.ERROR_SUBSCRIPTION_TO_META_CHANNEL),List[String](),"you attempted to subscribe to a meta channel"))
    else if(message.subscription.get.matches("^\\/service\\/.*")) Bayeux.writeMessage(error(message, List[Int](Bayeux.ERROR_SUBSCRIPTION_TO_SERVICE_CHANNEL),List[String](),"you attempted to subscribe to a service channel"))
    else{
      message.channel.subscribe(Client(message.clientId.get))
      Bayeux.writeMessage(Message(
          channelName = message.channelName, 
          clientId = message.clientId,
          successful = true,
          subscription = message.subscription,
          id = message.id,
          isResponse = true))
    }
  }
  private def unsubscribe(message: Message)(implicit writer: Option[PrintWriter]): Unit = {
    if(!message.clientId.isDefined) missingClient(message)
    else if(!message.subscription.isDefined) Bayeux.writeMessage(error(message,List[Int](Bayeux.ERROR_NO_SUBSCRIPTION_SPECIFIED),List[String](),"no subscription was specified"))
    else{
      Channel(message.subscription.get).unsubscribe(Client(message.clientId.get))
      Bayeux.writeMessage(Message(channelName = message.channelName,
          clientId = message.clientId,
          successful = true,
          subscription = message.subscription,
          id = message.id,
          isResponse = true)
      )
    }
  }
  
  private def disconnect(message: Message)(implicit writer: Option[PrintWriter]): Unit = {
    if(!message.clientId.isDefined) missingClient(message)
    else{
      Client(message.clientId.get).disconnect
      Bayeux.writeMessage(
        Message(channelName = message.channelName, 
                clientId = message.clientId,
                successful = true,
                id = message.id,
                isResponse = true
        )
      )
    }
  }
  
  private def connect(message: Message)(implicit writer: Option[PrintWriter]): Unit = {
    if(!message.clientId.isDefined) missingClient(message)
    else if(!message.connectionType.isDefined) Bayeux.writeMessage(error(message,List[Int](Bayeux.ERROR_MISSING_CONNECTION_TYPE), List[String](null), "a connectionType was not specified"))
    else if(!Bayeux.SUPPORTED_CONNECTION_TYPES.contains(message.connectionType.get)) Bayeux.writeMessage(error(message, List[Int](Bayeux.ERROR_UNSUPPORTED_CONNECTION_TYPE), List[String](message.connectionType.get),"the connectionType specified is unsupported"))
    else Client(message.clientId.get).enqueue(message)
  }
  
  private def missingClient(message: Message)(implicit writer: Option[PrintWriter]) = {
    Bayeux.writeMessage(error(message,
      List[Int](Bayeux.ERROR_MISSING_CLIENT_ID),
      List[String](null),
      "either a clientId was not sent, or it was not found"))
  }  
}