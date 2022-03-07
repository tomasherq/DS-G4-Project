package Messaging

import Messaging.GuaranteeType.GuaranteeType

trait MessageType

@SerialVersionUID(1L)
case class Subscribe(advertisementID: (Int, Int), subscriberID: Int) extends Serializable with MessageType
@SerialVersionUID(1L)
case class Unsubscribe(advertisementID: (Int, Int), subscriberID: Int) extends Serializable with MessageType

@SerialVersionUID(1L)
case class Advertise(advertisement: Advertisement, guarantee: GuaranteeType) extends Serializable with MessageType
@SerialVersionUID(1L)
case class Unadvertise(advertisement: Advertisement, guarantee: GuaranteeType) extends Serializable with MessageType

@SerialVersionUID(1L)
case class AckResponse(messageType: String, ID: (Int, Int)) extends Serializable with MessageType

@SerialVersionUID(1L)
case class Publish(content: Any, publicationID: Int) extends Serializable with MessageType