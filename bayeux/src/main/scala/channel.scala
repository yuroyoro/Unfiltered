package unfiltered.bayeux


object Channel{
  //akka imports
  import se.scalablesolutions.akka.actor._
  
  //ccstm imports
  import edu.stanford.ppl.ccstm._
  
  //scala imports
  import scala.collection.immutable.HashMap
  
  //java
  import java.net.{URLEncoder, URLDecoder}
    
  private val channels = Ref(HashMap[String, Channel]())
  
  /**
  channel names must follow the following paradigm '/foo/bar/baz' and the characters between the slashes must be URL encoded
  **/
  def apply(name: String): Channel = {
    if(name == null || name == "" || name(0) != '/') throw new IllegalArgumentException("Channels must follow the following paradigm '/foo/bar/baz' and the characters between the slashes must be URL encoded")
    if(name.matches(".*\\*.*")) throw new IllegalArgumentException("the apply method does not support wildcards.  for wildcards see getChannels")
    if(name(name.length - 1) == '/') throw new IllegalArgumentException("the last character can not be a /")
		name.split("/").foreach{ s: String =>
			val pluses = s.replaceAll("%20", "+")
			if(!pluses.equals(URLEncoder.encode(URLDecoder.decode(pluses, "UTF-8"), "UTF-8")))
				throw new IllegalArgumentException("Channels must follow the following paradigm '/foo/bar/baz' and the characters between the slashes must be URL encoded")
		}
		
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
  
  def unapply(channel: Channel): Option[String] = Some(channel.name)
  
  /**
	Returns a Set of channels.  Wildcards are supported here.  They work as follows:

	The channel patterns support only trailing wildcards of either "*" to match a single segment or "**" to match multiple segments. Example channel patterns are: 
	
    /foo/\*  (that's suppose to be one star, ignore the backslash - stupid comments!)
        Matches /foo/bar and /foo/boo. Does not match /foo, /foobar or /foo/bar/boo.                        
    "/foo/\*\*  (that's two starts, ignore the backslash - stupid comments!)
        Matches /foo/bar, /foo/boo and /foo/bar/boo. Does not match /foo, /foobar or /foobar/boo
        
    @param name the name of the Channel(s) to fetch
    @return a Set of Channels matching the name specified
	**/
	def getChannels(name: String): List[Channel] = {
	    
	    if(!name.contains("*")) return List(apply(name))
	    
	    if(!(name(name.length - 1) == '*')) (atomic{ implicit t => channels().values}).toList
	    else if(name.length >= 3 && name.substring(name.length - 3) == "/**"){
	        val pattern = name.substring(0, name.length - 2) + ".*"
	        val tmpChannels = atomic{ implicit t => channels()}
	        (for(name <- tmpChannels.keySet if(name.matches(pattern))) yield tmpChannels(name)).toList
	    }else{
	        val pattern = name.substring(0, name.length - 1) + "[^/]*$"
	        val tmpChannels = atomic{ implicit t => channels()}
	        (for(name <- tmpChannels.keySet if(name.matches(pattern))) yield tmpChannels(name)).toList
	    }
	}
  
  private class ChannelImpl() extends TypedActor with Channel{
    private var channelName: Option[String] = None
    private var clients: Map[String, Client] = Map[String, Client]()

    def name = channelName.get
    def name_=(name: String) = channelName match{
      case Some(_) => throw new IllegalStateException("you tried to set the channel name when it was already set")
      case None => 
        self.id = name
        channelName = Some(name)
    }
    def subscribe(client: Client): Unit = {
      clients = clients + (client.id -> client)
      client.addSubscription(this)
    }
    def unsubscribe(client: Client): Unit ={
      clients = clients - client.id
      client.removeSubscription(this)
    }
    
    def publish(message: Message): Unit = {
      val clientId = if(message.client.isDefined) Some(message.client.get.id) else None
      clients.keySet.foreach{name: String => clients(name).enqueue(message, clientId)}
    }
    
    def subscribers: Map[String, Client] = clients

    //override def equals(channel: Any): Boolean = false
    override def toString = if(channelName.isDefined) "Channel(" + channelName.get + ")" else ""
    override def equals(obj: Any): Boolean = {
      println("Equals!")
      super.equals(obj)
    }

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
  
  override def toString: String
}
