object MessageTypes extends Enumeration {
  type MessageType = Value
  val Advertise, Subscribe, Publish, Unadvertise, Unsubscribe = Value
}

class Message {

  // Properties
  var messageType : MessageTypes.MessageType = MessageTypes.Advertise
  var mClass : String = ""
  var mAttributeList : Map[String, Int] = Map()
  var mSenderID : Int = 0
  var mPublicationID : Int = 0

  // Methods
  def printMessage = {
    println("Message type: " + messageType)
    println("Class: " + mClass)
    println("Sender ID: " + mSenderID)
    println("Publication ID: " + mPublicationID)
    println("Attributes: " + mAttributeList)
  }
}
