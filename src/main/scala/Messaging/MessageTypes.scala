package Messaging

trait MessageTypes

@SerialVersionUID(1L)
case class Subscribe(advertisementID: (Int, Int), subscriberID: Int) extends Serializable with MessageTypes
@SerialVersionUID(1L)
case class Unsubscribe(advertisementID: (Int, Int), subscriberID: Int) extends Serializable with MessageTypes

@SerialVersionUID(1L)
case class Advertise(advertisement: Advertisement) extends Serializable with MessageTypes
@SerialVersionUID(1L)
case class Unadvertise(advertisement: Advertisement) extends Serializable with MessageTypes

@SerialVersionUID(1L)
case class AckRequest(messageID: Int, nodeID: Int) extends Serializable with MessageTypes
@SerialVersionUID(1L)
case class AckResponse(messageID: Int, nodeID: Int) extends Serializable with MessageTypes

@SerialVersionUID(1L)
case class Publish(content: Any, publicationID: Int) extends Serializable with MessageTypes
@SerialVersionUID(1L)
case class TimedPublishRequest(publicationID: Int, subscriberID: Int, timestamp: Int) extends Serializable with MessageTypes //with Timestamp
