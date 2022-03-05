package Messaging

@SerialVersionUID(100L)
class Subscribe(val advertisementID: Int, val subscriberID: Int) extends Serializable
@SerialVersionUID(1010)
class Unsubscribe(val advertisementID: Int, val subscriberID: Int) extends Serializable

@SerialVersionUID(100L)
class Advertise(val advertisementID: Int) extends Serializable
@SerialVersionUID(100L)
class Unadvertise(val advertisementID: Int) extends Serializable

@SerialVersionUID(100L)
class AckRequest(val messageID: Int, val nodeID: Int) extends Serializable
@SerialVersionUID(100L)
class AckResponse(val messageID: Int, val nodeID: Int) extends Serializable

@SerialVersionUID(100L)
class Publish(val content: Any, val publicationID: Int) extends Serializable
@SerialVersionUID(100L)
class TimedPublishRequest(val publicationID: Int, val subscriberID: Int, val timestamp: Int) extends Serializable //with Timestamp
