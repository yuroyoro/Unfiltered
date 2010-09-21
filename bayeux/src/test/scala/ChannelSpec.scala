package unfiltered.bayeux

// specs
import org.specs._

// akka imports
import se.scalablesolutions.akka.actor.TypedActor

object ChannelSpec extends Specification{
  
  "Channel should" should {
    "should be created by name with the apply method" in {
      val channel = Channel("/channel/test1") must not(throwA[Exception])
    }
    
    "should return the same Channel when apply is called multiple times for the same channel name" in {
      val channel1 = Channel("/channel/test2")
      val channel2 = Channel("/channel/test2")
      assert(channel1 eq channel2)
      
    }
    
    "should subscribe a Client" in {
      val client = Client("Dustin!")
      val channel = Channel("/channel/test3")
      channel.subscribe(client)
      channel.subscribers must be equalTo(Map("Dustin!" -> client))
    }
    
    "should unsubscribe a Client" in {
      val client = Client("Whitney!")
      val channel = Channel("/channel/test4")
      channel.subscribe(client)
      channel.subscribers must be equalTo(Map("Whitney!" -> client))
      channel.unsubscribe(client)
      channel.subscribers must be equalTo(Map())
    }
    
    "should publish a message to subscribed Clients" in {
      object TmpClient extends Client{
        val id = "tmp"
        var messageWasEnqueued = false
        def id_=(clientId: String) = ()
        def enqueue(message: Message, clientId: Option[String]): Unit = messageWasEnqueued = true
        def messages = null
        def removeSubscription(channel: Channel): Unit = ()
        def disconnect: Unit = ()
        def addSubscription(channel: Channel): Unit = ()
        def getSubscriptions(): Set[Channel] = Set[Channel]()
        def subscriptions = null
      }
      
      val channel = Channel("/channel/test4")
      channel.subscribe(TmpClient)
      channel.publish(Message(channel = channel))
      Thread.sleep(100)
      TmpClient.messageWasEnqueued must be equalTo(true)
      
    }
    
    "should throw an exception with null channel name" in {
      Channel(null) must throwA[IllegalArgumentException]
    }
    
    "should throw an IllegalArgumentsException when the beginning character isn't a /" in {
      Channel("chat/scala") must throwA[IllegalArgumentException]
  	}

    "should throw an IllegalArgumentsException when one of the characters isn't the type allowed in URL Encoding" in {
  		Channel("/ch!at/scala") must throwA[IllegalArgumentException]
  	}

    "should not throw an IllegalArgumentException when + is in the segments" in {
      Channel("/ch+at/scala") must not(throwA[IllegalArgumentException])
  	}

    "should not throw an IllegalArgumentException when %20 is in the segments" in {
      Channel("/ch%20at/scala") must not(throwA[IllegalArgumentException])
  	}
  	
  	"should throw an IllegalArgumentException if the channel name has wildcards when the apply() method is called" in {
  	  Channel("/**") must throwA[IllegalArgumentException]
  	}

    "should throw an IllegalArgumentException the last character is a slash" in {
  	    Channel("/chat/") must throwA[IllegalArgumentException]
  	}
  	
    "should return a list of one element when no wildcard is used with getChannels" in {
  	    val channel = Channel("/chat/scala")
  	    Channel.getChannels("/chat/scala") must be equalTo(List(channel))
  	} 

    "should return multiple matches on the /** pattern" in {
  	    val channelOne = Channel("/test5/one")
  	    val channelOneSubA = Channel("/test5/one/a")
  	    Channel.getChannels("/test5/**") must be equalTo(List(channelOne, channelOneSubA))
  	}

    "should return multiple matches on the /** pattern (testing multiple channels that don't apply to the pattern)" in {
  	    val channelOne = Channel("/test6/one")
  	    val channelOneSubA = Channel("/test6/one/a")
  	    val channelBlah = Channel("/test6a/foo")
  	    Channel.getChannels("/test6/**") must be equalTo(List(channelOneSubA, channelOne))
  	}

    "should return single level matches on the /* pattern " in {
  	    val channelOne = Channel("/test7/one")
  	    val channelOneSubA = Channel("/test7/one/a")
  	    val channelBlah = Channel("/test7a/foo")
  	    Channel.getChannels("/test7/*") must be equalTo(List(channelOne))
  	}
  	
  	"should match with unapply" in {
  	  val channel = Channel("/foo/bar")
  	  channel match {
  	    case Channel("/foo/bar") => true must be equalTo(true)
  	    case _ => true must be equalTo(false)
  	  }
  	  
  	  channel match {
  	    case Channel("/foo/nomatch") => true must be equalTo(false)
  	    case _ => true must be equalTo(true)
  	  }
  	}
    
  }
}