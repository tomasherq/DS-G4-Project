package Messaging

class Subscribe(val advertisementID: Int, val subscriberID: Int)
class Unsubscribe(val advertisementID: Int, val subscriberID: Int)

class Advertise(val advertisementID: Int)
class Unadvertise(val advertisementID: Int)

class AckRequest(val messageID: Int, val nodeID: Int)
class AckResponse(val messageID: Int, val nodeID: Int)

class Publish(val content: Any, val publicationID: Int)
class TimedPublishRequest(val publicationID: Int, val subscriberID: Int, val timestamp: Int) //with Timestamp
