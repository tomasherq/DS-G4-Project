package Nodes

import Messaging.GuaranteeType._
import Messaging._
import Misc.ResourceUtilities
import Routing.RoutingTable

class Broker(override val ID: Int, val endpoints: List[Int]) extends Node(ID) {

  private val subscriptionList = scala.collection.mutable.Map[(Int, Int), Subscription]()
  private val advertisementList = scala.collection.mutable.Map[(Int, Int), Advertisement]()
  private val subscriberList = scala.collection.mutable.Map[(Int, Int), Advertisement]()

  private val SRT = new RoutingTable()
  private val PRT = new RoutingTable()
  private val NB = ResourceUtilities.getNeighbours(ID)
  private val ACKS = scala.collection.mutable.Map[((Int, Int), Int), Boolean]()
  private val Groups = List[Int]()
  private val IsActive = scala.collection.mutable.Map[Int, Boolean]() // AdvertisementID
  private val StoredPubs = scala.collection.mutable.Map[Int, List[Int]]() // PublisherID -> list of publications

  /**
   * Advertisement methods
   */
  def receiveAdvertisement(message: Message): Unit = {
    println("Receiving Advertisement")

    val content: Advertise = message.content.asInstanceOf[Advertise]
    val lastHop: Int = message.sender.ID
    val a: (Int, Int) = content.advertisement.ID

    SRT.addRoute(a, lastHop)
    val nextHops: List[Int] = NB diff List(lastHop)

    if (content.guarantee == ACK) {
      if (nextHops.isEmpty) { // Reached an edge broker
        sendACK(lastHop, message)
      } else {
        for (hop <- nextHops) {
          ACKS += ((a, hop) -> false)
          startAckTimer(a)
        }
      }
    }

    for (hop <- nextHops) {
      sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp()), hop) // Flood to next hops
    }

    // Local processing of the message
    if (!advertisementList.contains(content.advertisement.ID)) {
      advertisementList += (content.advertisement.ID -> Advertisement(content.advertisement.ID, content.advertisement.pClass, content.advertisement.pAttributes))
    }
    println(advertisementList)
  }

  def receiveUnadvertisement(message: Message): Unit = {
    println("Receiving Unadvertisement")

    val content: Unadvertise = message.content.asInstanceOf[Unadvertise]
    val lastHop: Int = message.sender.ID
    val a: (Int, Int) = content.advertisement.ID

    SRT.deleteRoute(a)
    val nextHops: List[Int] = NB diff List(lastHop)

    if (content.guarantee == ACK) {
      if (nextHops.isEmpty) { // Reached an edge broker
        sendACK(lastHop, message)
      } else {
        for (hop <- nextHops) {
          ACKS += ((a, hop) -> false) // TODO Find out if this needs to be a separate entity for unadvertisements?
          startAckTimer(a)
        }
      }
    }

    for (hop <- nextHops) {
      sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp()), hop) // Flood to next hops
    }

    if(advertisementList.contains(content.advertisement.ID)) {
      advertisementList -= content.advertisement.ID
    }
    println(advertisementList)
  }

  /**
   *  Subscription methods
   */
  def receiveSubscription(message: Message): Unit = {
    println("Receiving Subscription")
    // TODO To be implemented
  }

  def receiveUnsubscription(message: Message): Unit = {
    println("Receiving Unsubscription")
    // TODO To be implemented
  }

  override def execute(): Unit = {
    super.execute()
    super.startReceiver()

    while (true) {
      Thread.sleep(1000)
      println("Waiting for messages...")

      while (!receiver.isQueueEmpty) {
        println("Retrieving a new message...")
        val message = receiver.getFirstFromQueue()

        message.content match {
          case _ : Advertise => receiveAdvertisement(message)
          case _ : Unadvertise => receiveUnadvertisement(message)
          case _ : Subscribe => receiveSubscription(message)
          case _ : Unsubscribe => receiveUnsubscription(message)
          case _ : AckResponse => receiveACK(message)
        }
        receiver.emptyQueue() // Process the message, this should be individual
      }
    }
  }
}
