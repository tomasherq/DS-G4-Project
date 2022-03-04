package Messages


/*
* messageTypes
* 1 == subscription
* 2 == unsubscription
* 3 == advertisement
* 4 == unadvertisement
* 5 == ACK request
* 6 == ACK response
* 7 == Publication
* 8 == Request for a publication
* */

// We have the senderID cause an IP could use multiple ports!
class SenderInfo(val id:String,var address:String,val port:Int)

// We could think of having various receivers
@SerialVersionUID(100L)
class Message(val id:String,val senderInfo: SenderInfo,val messageType:Int,val receiverId:String,val content:Any, val timestamp:Int)

// Can be used for unsubscription if type of message changed
class SubscriberRequestContent(val advertisementID:String,val subscriberID:String)

// Can be used for unadvertisement if type of message changed
class AdvertisementContent(val advertisementID:String)

// Can be used as an ACK request and response
class AckContent(val messageID:String,val nodeID:String)

// The publication depends on the advertisementID
class PublicationContent(val content:String,val publicationID:String)

// Same as a subscription, but you can send a timestamp instead --> Depending on the field we use a type or other
class RequestPublicationMessage(val publicationID:String,val subscriberID:String,val timestamp:Int)




