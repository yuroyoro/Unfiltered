package unfiltered.bayeux

// specs
import org.specs._

// akka imports
import se.scalablesolutions.akka.actor.TypedActor

//scala
import scala.collection.immutable.Queue

object ClientSpec extends Specification{
  
  "Client should" should {
    "should be created by id with the apply method" in {
      Client("Dustin!") must not(throwA[Exception])
    }
    
    "should be created with a random id when apply is used without an id" in {
      val client = Client()
      client.id must not be equalTo(null)
    }
    
    "should enqueue a message" in {
      val client = Client()
      val channel = Channel("/channel/test")
      client.enqueue(Message(channel = channel, id = Some("testing")), None)
      client.messages(0).id.get must be equalTo("testing")
    }
    
    "should unsubscribe from all channels when disconnect is called" in {
      val client = Client()
      val channel = Channel("/channel/cstest1")
      channel.subscribe(client)
      channel.subscribers(client.id) must be equalTo(client)
      client.disconnect
      Thread.sleep(100)
      channel.subscribers.size must be equalTo(0)
    }
    
    "should return the clients subscriptions" in {
      val client = Client()
      val channel = Channel("/channel/cstest1")
      channel.subscribe(client)
      channel.subscribers(client.id) must be equalTo(client)
      val name = client.subscriptions.head.name
      name must be equalTo(channel.name)
    }
    
    "should match with unapply" in {
      val client = Client("asdf")
      client match {
        case Client("asdf") => true must be equalTo(true)
        case _ => true must be equalTo(false)
      }
      
      client match {
        case Client("fdsa") => false must be equalTo(true)
        case _ => true must be equalTo(true)
      }
    }
    
  }
}