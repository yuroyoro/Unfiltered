package unfiltered.bayeux

//joda-time
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat

//liftweb
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.MappingException

object Message{
    
  val ADVICE = "advice"
  val CHANNEL = "channel"
  val CLIENT_ID = "clientId"
  val CONNECTION_TYPE = "connectionType"
  val DATA = "data"
  val DATE_FORMAT = "YYYY-MM-dd'T'hh:mm:ss"
  val ERROR = "error"
  val EXT = "ext"
  val ID = "id"
  val MINIMUM_VERSION = "minimumVersion"
  val SUBSCRIPTION = "subscription"
  val SUCCESSFUL = "successful"
  val SUPPORTED_CONNECTION_TYPES = "supportedConnectionTypes"
  val TIMESTAMP = "timestamp"
  val VERSION = "version"
    
  val timestampFormatter = DateTimeFormat.forPattern(DATE_FORMAT)
      
  implicit def mapToJValue(map: Map[String, Any]): JValue = {
    import net.liftweb.json.JsonAST._
    import net.liftweb.json.JsonDSL._
    import net.liftweb.json.Implicits._
        
    def transform(m: Map[String, Any]): JValue = JObject(m.map{ case (k: String, x) => JField(k, getField(x)) }.toList)
            
    def getField(any: Any): JValue = {
      any match {
        case a: Message => messageToJson(a)
        case a: Boolean => JBool(a)
        case a: Double => JDouble(a)
        case a: Int => JInt(a)
        case a: String => JString(a)
        case a: List[Any] => JArray(a.map(getField))
        case null => JNull
        case a: Map[String, Any] => transform(a)
        case a => JString(a.toString)
      }
    }
    transform(map)
  }
    
  //transforms a message into json
  implicit def messageToJson(message: Message): JValue = {
    import net.liftweb.json.JsonAST
    import net.liftweb.json.JsonAST._
    import net.liftweb.json.JsonDSL._
    
    var json: JObject = (CHANNEL -> message.channelName)
    if(message.clientId.isDefined) json = json ~ (CLIENT_ID -> message.clientId.get)
    if(message.connectionType.isDefined) json = json ~ (CONNECTION_TYPE -> message.connectionType.get)
    if(message.dateTime != null) json = json ~ (TIMESTAMP -> message.timestamp)
    if(message.error.isDefined) json = json ~ (ERROR -> message.error.get)
    if(message.id.isDefined) json = json ~ (ID -> message.id.get)
    if(message.subscription.isDefined) json = json ~ (SUBSCRIPTION -> message.subscription.get)
    if(message.isResponse) json = json ~ (SUCCESSFUL -> message.successful)
    if(message.ext.size > 0) json = json ~ (EXT -> message.ext)
    if(message.advice.size > 0) json = json ~ (ADVICE -> message.advice)
        
    message.channelName match {
      case Bayeux.META_HANDSHAKE => 
        json = json ~ (VERSION -> message.version) ~ (SUPPORTED_CONNECTION_TYPES -> message.supportedConnectionTypes)
        if(message.minimumVersion != null) json = json ~ (MINIMUM_VERSION -> message.minimumVersion)
        ()
      case Bayeux.META_CONNECT => () //add advice
      case Bayeux.META_DISCONNECT => () //add advice
      case Bayeux.META_SUBSCRIBE => () //add advice
      case Bayeux.META_UNSUBSCRIBE => () //add advice
      case _ => json = json ~ (DATA -> message.data)
    }
    json
  }
    
      
  private def extract[T](json: JValue, fieldName: String)(implicit m: Manifest[T]): Option[T] = {
    implicit val formats = net.liftweb.json.DefaultFormats
    try{ Some((json \ fieldName).extract[T]) } catch { case e: MappingException => None }
  }

  //gets the DateTime from Json
  private def extractDateTime(json: JValue): Option[DateTime] = {
    val timestamp = extract[String](json, Message.TIMESTAMP)
    if(timestamp.isDefined) Some(timestampFormatter.withZone(DateTimeZone.UTC).parseDateTime(timestamp.get))
    else None
  }
    
  //extracts the data map.  returns an empty if a map isn't found
  private def extractMap(json: JValue, name: String): Option[Map[String, Any]] = {
    import net.liftweb.json.JsonParser._
    val map = (json \ DATA) match {
      case JField(DATA, obj: JObject) => obj.values
      case _ => Map[String, Any]()
    }
    Some(map)
  }
    
  import net.liftweb.json.JsonAST._
  private def extractSupportedConnectionTypes(json: JValue): Option[List[String]] = Some((for{JString(str) <- (json \ SUPPORTED_CONNECTION_TYPES)} yield str).toList)
    
}

case class Message(
  val advice: Map[String, Any] = Map[String, Any](),
  val channelName: String,
  val clientId: Option[String] = None,
  val connectionType: Option[String] = None,
  val data: Option[Map[String, Any]] = Some(Map[String, Any]()),
  val dateTime: DateTime = new DateTime(DateTimeZone.UTC),
  val error: Option[String] = None,
  val ext: Option[Map[String, Any]] = None,
  val id: Option[String] = None,
  val isResponse: Boolean = false,
  val minimumVersion: String = "1.0.0",
  val subscription: Option[String] = None,
  val successful: Boolean = false,
  val supportedConnectionTypes: Option[List[String]] = Some(List(Bayeux.LONG_POLLING)),
  val version: String = "1.0.0"
){
  def this(json: JValue) = this(
        channelName = Message.extract[String](json, Message.CHANNEL).get,
        clientId = Message.extract[String](json, Message.CLIENT_ID),
        connectionType = Message.extract[String](json, Message.CONNECTION_TYPE),
        error = Message.extract[String](json, Message.ERROR),
        ext = Message.extractMap(json, Message.EXT),
        id = Message.extract[String](json, Message.ID),
        minimumVersion = Message.extract[String](json, Message.MINIMUM_VERSION).get,
        subscription = Message.extract[String](json, Message.SUBSCRIPTION),
        successful = Message.extract[Boolean](json, Message.SUCCESSFUL).get,
        version = Message.extract[String](json, Message.VERSION).get,
        dateTime = Message.extractDateTime(json).get,
        supportedConnectionTypes = Message.extractSupportedConnectionTypes(json),
        data = Message.extractMap(json, Message.DATA)
  )
  
  lazy val client: Option[Client] = if(clientId.isDefined) Some(Client(clientId.get)) else None
  lazy val channel: Channel = Channel(channelName)
  
  val timestamp: String = Message.timestampFormatter.print(dateTime)
}