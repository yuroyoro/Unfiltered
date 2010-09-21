package unfiltered.bayeux

//akka imports
import se.scalablesolutions.akka.actor.TypedActor

//scala
import scala.collection.immutable.Queue

trait Client{
  def id: String
  private[bayeux] def id_=(clientId: String): Unit
  def enqueue(message: Message, clientId: Option[String]): Unit
  def messages: Queue[Message]
  private[bayeux] def removeSubscription(channel: Channel): Unit
  def disconnect: Unit
  private[bayeux] def addSubscription(channel: Channel): Unit
  def subscriptions(): Set[Channel]
}

object Client{
  //ccstm imports
  import edu.stanford.ppl.ccstm._
  
  //scala imports
  import scala.collection.immutable.HashMap
  
  private val clients = Ref(HashMap[String, Client]())
  
  //Returns a client with the given id.  If the client already exists, that client will be returned. If not
  //a new client will be created and the given id will be set as its id
  def apply(id: String): Client = {
    atomic{ implicit t =>
      clients().get(id) match {
        case Some(client) => client
        case None =>
          val client = TypedActor.newInstance(classOf[Client], classOf[ClientImpl], 1000)
          client.id = id
          clients() = clients() + (id -> client)
          client
      }
    }
  }
  
  //returns a new client with a random id
  def apply(): Client = {
    val uuid = java.util.UUID.randomUUID().toString
    val client = TypedActor.newInstance(classOf[Client], classOf[ClientImpl], 1000)
    client.id = uuid
    atomic{ implicit t =>
      clients() = clients() + (uuid -> client)
    }
    client
  }
  
  def unapply(client: Client): Option[String] = Some(client.id)
  
  private class ClientImpl extends TypedActor with Client{
    
    private var channels: Set[Channel] = Set[Channel]()
    private var messageQueue: Queue[Message] = Queue[Message]()
    
    def id = self.id
    private[bayeux] def id_=(clientId: String) = self.id = clientId
    def messages = messageQueue
    private[bayeux] def removeSubscription(channel: Channel): Unit = channels = channels - channel
    
    def disconnect: Unit = {
      channels.foreach(_.unsubscribe(this))
      self.stop
    }
    
    def addSubscription(channel: Channel): Unit = channels = channels + channel
    def subscriptions(): Set[Channel] = channels
    
    def enqueue(message: Message, clientId: Option[String]): Unit = {
      clientId match {
        case Some(id) if id != self.id => messageQueue = messageQueue enqueue message
        case None => messageQueue = messageQueue enqueue message
        case _ => () // do nothing if the message is from this client, because the message gets written immediately back to the client
      }
    }
    
    private def flush(message: Message): Unit = ()
    
  }
}