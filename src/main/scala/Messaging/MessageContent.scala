package Messaging

/** Publisher
* It contains a list of the Publications it made with their ids
* It also contains the list of the subscribers ids and the lastACK they received --> Message contains the ACKContent
*/
class Advertisement(val ID: Int,
                    var publications:scala.collection.mutable.Map[Int, Publication] = scala.collection.mutable.Map[Int, Publication](),
                    var subscribers:scala.collection.mutable.Map[Int, Message] = scala.collection.mutable.Map[Int, Message]())

class Publication(val content: Any, val timestamp: Int) // The id is defined in the advertisement

/** Subscriber
* Keep the record of all ads and just if we are subscribed or not
* Keep the last publication received or a whole list?
*/
class Subscription(var publisherID: Int,
                   var subscribed: Boolean = false,
                   var publications:scala.collection.mutable.Map[Int, Publication]=scala.collection.mutable.Map[Int, Publication]())
