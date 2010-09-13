package unfiltered.bayeux

//akka imports
import se.scalablesolutions.akka.actor.TypedActor

trait Client{
  def id: String
  def id_=(clientId: String): Unit
  def enqueue(message: Message): Unit
  def removeSubscription(channel: Channel): Unit
  def disconnect: Unit
  def addSubscription(channel: Channel): Unit
  def getSubscriptions(): Set[Channel]
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
  
  private class ClientImpl extends TypedActor with Client{
    import scala.collection.immutable.Queue
    
    private var subscriptions: Set[Channel] = Set[Channel]()
    private var messages: Queue[Message] = Queue[Message]()
    
    def id = self.id
    def id_=(clientId: String) = self.id = clientId
    def enqueue(message: Message): Unit = messages = messages enqueue message
    def removeSubscription(channel: Channel): Unit = subscriptions = subscriptions - channel
    def disconnect: Unit = {
      subscriptions.foreach(_.unsubscribe(this))
      self.stop
    }
    def addSubscription(channel: Channel): Unit = subscriptions = subscriptions + channel
    def getSubscriptions(): Set[Channel] = subscriptions
  }
}