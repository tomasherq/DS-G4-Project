package Nodes

import Messages.{AdvertisementContent, Message, SenderInfo}
import Routing.RoutingEntry
import Utilities.{Advertisement, Subscription}

class Broker(override val address:String, override val name:String, override val port:Int, override val receiverPort:Int) extends Node( address, name,  port, receiverPort) {

  private var subscriptionList:scala.collection.mutable.Map[String,Subscription] = scala.collection.mutable.Map[String,Subscription]()
  private var advertisementList = scala.collection.mutable.Map[String,Advertisement]()
  private var subscriberList = scala.collection.mutable.Map[String,Advertisement]()

  // TODO change to instance of RoutingTable with RoutingEntry elements
  var routingTable = scala.collection.mutable.Map[String,RoutingEntry]()

  /**
   * This method will lookup a candidate edge broker that can reach the client if this broker is not its edge broker
   */
  def forwardMessage(): Unit = {
    // TODO To implement
  }

  /**
   * TODO Routing table method wrappers
   */
  def addRoute(senderInfo: SenderInfo): Unit = {
    routingTable += (senderInfo.id -> new RoutingEntry(senderInfo.address,senderInfo.port))
  }
  def deleteRoute(name:String): Unit = {
    routingTable -= (name)
  }

  /**
   * Advertisement methods
   */
  def receiveAdvertisement(message: Message): Unit = {

    val content:AdvertisementContent=message.content.asInstanceOf[AdvertisementContent]

    if(!subscriptionList.contains(content.advertisementID)) {
      subscriptionList += (content.advertisementID -> new Subscription(message.senderInfo.id))
    }
  }
  def receiveUnadvertisement(message: Message): Unit = {
    val content:AdvertisementContent = message.content.asInstanceOf[AdvertisementContent]

    if(subscriptionList.contains(content.advertisementID)) {
      subscriptionList -= (content.advertisementID)
    }
  }

  /**
   *  Subscription methods
   */
  def receiveSubscription(message: Message): Unit = {
    // TODO To be implemented
  }

  def receiveUnsubscription(message: Message): Unit = {
    // TODO To be implemented
  }

  /**
   * Publication methods
   */
  def receivePubRequest(message: Message): Unit = {
    // TODO To be implemented
  }

  /**
   * Ack methods
   */
  def sendAckResponse(): Unit = {
    // TODO To be implemented
  }

  def receiveAckRequest(message: Message): Unit = {
    // TODO To be implemented
  }

  override def execute(): Unit = {
    super.execute()
    val t = new Thread(receiver)
    t.start()

    while (true) {
      Thread.sleep(1000)

      while (!receiver.isQueueEmpty) {
        val message = receiver.getFirstFromQueue()

        if(!routingTable.contains(message.senderInfo.id) ) {
          addRoute(message.senderInfo)
        }

        message.messageType match {
          case 3 => receiveAdvertisement(message)
          // TODO add other cases
        }

        receiver.emptyQueue // Process the message, this should be individual
      }
    }
  }
}
