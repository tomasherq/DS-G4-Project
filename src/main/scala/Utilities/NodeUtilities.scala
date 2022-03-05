package Utilities

import Messages.Message

/**
 * The name is given by the table
 * We do not store the next hop, as the receiver only stores where to forward the message
 */
class RoutingEntry(val address:String, val port:Int)

/** Publisher
* It contains a list of the Publications it made with their ids
* It also contains the list of the subscribers ids and the lastACK they received --> Message contains the ACKContent
*/
class Advertisement(val id:String,
                    var publications:scala.collection.mutable.Map[String,Publication]=scala.collection.mutable.Map[String,Publication](),
                    var subscribers:scala.collection.mutable.Map[String,Message]=scala.collection.mutable.Map[String,Message]())

class Publication(val content:String,val timestamp:Int) // The id is defined in the advertisement


/** Subscriber
* Keep the record of all ads and just if we are subscribed or not
* Keep the last publication received or a whole list?
*/
class Subscription(var publisherID:String,
                   var subscribed:Boolean=false,
                   var publications:scala.collection.mutable.Map[String,Publication]=scala.collection.mutable.Map[String,Publication]()
                  )
