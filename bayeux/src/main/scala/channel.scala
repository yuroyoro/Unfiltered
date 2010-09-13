package unfiltered.bayeux

object Channel{
  //akka imports
  import se.scalablesolutions.akka.actor.TypedActor
  
  //ccstm imports
  import edu.stanford.ppl.ccstm._
  
  //scala imports
  import scala.collection.immutable.HashMap
    
  private val channels = Ref(HashMap[String, Channel]())
  
  def apply(name: String): Channel = {
    atomic { implicit t =>
      channels().get(name) match {
        case Some(channel) => channel
        case None => 
          val channel = TypedActor.newInstance(classOf[Channel], classOf[ChannelImpl], 1000)
          channels() = channels() + (name -> channel)
          channel.name = name
          channel
      }
    }
  }
  
  private class ChannelImpl extends TypedActor with Channel{
    private var channelName: Option[String] = None
    private var clients: Map[String, Client] = Map[String, Client]()

    def name = channelName.get
    def name_=(name: String) = channelName match{
      case Some(_) => throw new IllegalStateException("you tried to set the channel name when it was already set")
      case None => 
        self.id = name
        channelName = Some(name)
    }
    def subscribe(client: Client): Unit = clients = clients + (client.id -> client)
    def unsubscribe(client: Client): Unit = clients = clients - client.id
    def publish(message: Message): Unit = ()
    def subscribers: Map[String, Client] = clients
    
    override def equals(channel: Any): Boolean = Some(channel.asInstanceOf[Channel].name) == channelName

  }
  
}

trait Channel{
  /**
  @return the name of the Channel
  **/
  def name: String
  
  /**
  Sets the name of the channel.  If the name has already been set, an exception is thrown.
  This is protected because the intent is for the name to be set right after the TypedActor is created
  @param name the name of the Channel
  **/
  protected[bayeux] def name_=(name: String): Unit
  
  /**
  Adds a Client to this channel
  @param client the Client to be added
  **/
  def subscribe(client: Client): Unit
  
  /**
  Removes a Client from this channel
  @param client the Client to remove
  **/
  def unsubscribe(client: Client): Unit
  
  /**
  Publishes the given message to each Client in the Channel
  @param message the message to be broadcast
  **/
  def publish(message: Message): Unit
  
  /**
  All of the subscribers in the channel
  @return the subsribers
  **/
  def subscribers: Map[String, Client]
}
