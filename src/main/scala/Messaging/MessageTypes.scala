package Messaging

trait MessageTypes

@SerialVersionUID(100L)
case class Subscribe(advertisementID: Int, subscriberID: Int) extends Serializable with MessageTypes
@SerialVersionUID(1010)
case class Unsubscribe(advertisementID: Int, subscriberID: Int) extends Serializable with MessageTypes

@SerialVersionUID(100L)
case class Advertise(advertisementID: Int) extends Serializable with MessageTypes
@SerialVersionUID(100L)
case class Unadvertise(advertisementID: Int) extends Serializable with MessageTypes

@SerialVersionUID(100L)
case class AckRequest(messageID: Int, nodeID: Int) extends Serializable with MessageTypes
@SerialVersionUID(100L)
case class AckResponse(messageID: Int, nodeID: Int) extends Serializable with MessageTypes

@SerialVersionUID(100L)
case class Publish(content: Any, publicationID: Int) extends Serializable with MessageTypes
@SerialVersionUID(100L)
case class TimedPublishRequest(publicationID: Int, subscriberID: Int, timestamp: Int) extends Serializable with MessageTypes //with Timestamp
