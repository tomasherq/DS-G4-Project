package Nodes

import Messaging.GuaranteeType._
import Messaging._
import Misc.ResourceUtilities
import Routing.RoutingTable

import scala.collection.mutable.ListBuffer

class Broker(override val ID: Int, val endpoints: List[Int]) extends Node(ID) {

  private val subscriptionList = scala.collection.mutable.Map[(Int, Int), Subscription]()
  private val advertisementList = scala.collection.mutable.Map[(Int, Int), Advertisement]()
  private val lastHops = scala.collection.mutable.Map[(String, (Int, Int)), Int]()

  private val SRT = new RoutingTable()
  private val PRT = new RoutingTable()
  private val NB = ResourceUtilities.getNeighbours(ID)
  private val IsActive = scala.collection.mutable.Map[(Int, Int), Boolean]()
  private val StoredPubs = scala.collection.mutable.Map[(Int, Int), List[Int]]()

  // Currently not used, its for GROUPID guarantee
  private val Groups = List[Int]()

  /**
   * Advertisement methods
   */
  def processAdvertisement(content: Advertise): Unit = {
    if (!advertisementList.contains(content.advertisement.ID)) {
      advertisementList += (content.advertisement.ID -> Advertisement(content.advertisement.ID, content.advertisement.pClass, content.advertisement.pAttributes))
      println(advertisementList)
    }
  }

  def clearAdvertisement(content: Unadvertise): Unit = {
    if(advertisementList.contains(content.advertisement.ID)) {
      advertisementList -= content.advertisement.ID
      println(advertisementList)
    }
  }

  def processSubscription(content: Subscribe): Unit = {
    if (!subscriptionList.contains(content.subscription.ID)) {
      subscriptionList += (content.subscription.ID -> Subscription(content.subscription.ID, content.subscription.pClass, content.subscription.pAttributes))
      println(subscriptionList)
    }
  }

  def clearSubscription(content: Unsubscribe): Unit = {
    if(subscriptionList.contains(content.subscription.ID)) {
      subscriptionList -= content.subscription.ID
      println(subscriptionList)
    }
  }

  def receiveAdvertisement(message: Message): Unit = {
    println("Receiving Advertisement from " + message.sender.ID)

    val content: Advertise = message.content.asInstanceOf[Advertise]
    val messageType: String = content.getClass.toString
    val lastHop: Int = message.sender.ID
    val a: (Int, Int) = content.advertisement.ID

    lastHops += ((messageType, a) -> lastHop)

    SRT.addRoute(a, lastHop, content.advertisement.pClass, content.advertisement.pAttributes)
    val nextHops: List[Int] = NB diff List(lastHop)

    if (content.guarantee == ACK) {
      if (nextHops.isEmpty) { // Reached an edge broker
        sendACK(messageType, a, lastHop)
      } else {
        startAckTimer(messageType, a)
        for (hop <- nextHops) {
          ACKS += ((messageType, a, hop) -> false)
        }
      }
    }

    for (hop <- nextHops) {
      println("Forwarding Advertisement to " + hop)
      sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp()), hop) // Flood to next hops
    }

    processAdvertisement(content)
  }

  def receiveUnadvertisement(message: Message): Unit = {
    println("Receiving Unadvertisement from " + message.sender.ID)

    val content: Unadvertise = message.content.asInstanceOf[Unadvertise]
    val messageType: String = content.getClass.toString
    val lastHop: Int = message.sender.ID
    val a: (Int, Int) = content.advertisement.ID

    lastHops += ((messageType, a) -> lastHop)

    SRT.deleteRoute(a)
    val nextHops: List[Int] = NB diff List(lastHop)

    if (content.guarantee == ACK) {
      if (nextHops.isEmpty) { // Reached an edge broker
        sendACK(messageType, a, lastHop)
      } else {
        for (hop <- nextHops) {
          startAckTimer(messageType, a)
          ACKS += ((messageType, a, hop) -> false)
        }
      }
    }

    for (hop <- nextHops) {
      println("Forwarding Advertisement to " + hop)
      sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp()), hop) // Flood to next hops
    }

    clearAdvertisement(content)
  }

  /**
   *  Subscription methods
   */
  def receiveSubscription(message: Message): Unit = {
    println("Receiving Subscription")

    val content: Subscribe = message.content.asInstanceOf[Subscribe]
    val messageType: String = content.getClass.toString
    val lastHop: Int = message.sender.ID
    val s: (Int, Int) = content.subscription.ID

    val isEB = (NB diff List(lastHop)).isEmpty

    lastHops += ((messageType, s) -> lastHop)

    val advs: List[(Int, Int)] = SRT.findMatch(content.subscription)

    val nextHops: ListBuffer[Int] = null
    for (ad <- advs) {
      nextHops += SRT.getRoute(ad)._1
    }

    val coverSub: List[Int] = findCoveringSub()

    PRT.addRoute(s, lastHop, content.subscription.pClass, content.subscription.pAttributes)

    content.guarantee match {
      case ACK =>
        if (isEB) {
          IsActive += (s -> false)
        }
        if (coverSub.nonEmpty || nextHops.isEmpty) {
          if (isEB) {
            IsActive += (s -> true)
          }
          sendACK(messageType, s, lastHop)
        } else {
          for (hop <- nextHops) {
            ACKS += ((messageType, s, hop) -> false)
          }
          startAckTimer(messageType, s)
          for (hop <- nextHops) {
            println("Forwarding Subscription to " + hop)
            sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp()), hop)
          }
        }
      case TIME => ???
      case GROUPID => ???
      case NONE =>
        for (hop <- nextHops) {
          println("Forwarding Subscription to " + hop)
          sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp()), hop)
        }
    }
    processSubscription(content)
  }

  def receiveUnsubscription(message: Message): Unit = {
    println("Receiving Unsubscription")

    val content: Unsubscribe = message.content.asInstanceOf[Unsubscribe]
    val messageType: String = content.getClass.toString
    val lastHop: Int = message.sender.ID
    val s: (Int, Int) = content.subscription.ID

    // TODO To be implemented

    clearSubscription(content)
  }

  /**
   * Publication methods
   */
  def receivePublication(message: Message): Unit = {
    println("Receiving Publication")
    // TODO To be implemented
  }

  /**
   * Ack methods
   */
  def sendACK(messageType: String, ID: (Int, Int), lastHop: Int): Unit = {
    println("Sending Ack Response to " + lastHop)

    val ACK = AckResponse(messageType, ID)
    sendMessage(new Message(getMessageID(), SocketData, lastHop, ACK, getCurrentTimestamp()), lastHop)
  }

  def receiveACK(message: Message): Unit = {
    println("Receiving Ack Response from " + message.sender.ID)

    val ACK = message.content.asInstanceOf[AckResponse]
    val messageType = ACK.messageType

    if ((message.timestamp.getTime - timestamps(messageType, ACK.ID).getTime) < 1500) {

      println("Processing of ACK took: " + (message.timestamp.getTime - timestamps(messageType, ACK.ID).getTime) + "ms")

      ACKS += ((messageType, ACK.ID, message.sender.ID) -> true)

      if (receivedAllPendingACKS(messageType, ACK.ID, message.sender.ID)) {
        if ((ACK.messageType == "Message.Subscribe" || ACK.messageType == "Message.Unsubscribe" ) && endpoints.contains(ACK.ID._1)) {
          IsActive += (ACK.ID -> true)
        }
        val destinationID = lastHops(messageType, ACK.ID)
        println("Sending ACK Response to " + destinationID)
        sendMessage(new Message(getMessageID(), SocketData, destinationID, ACK, getCurrentTimestamp()), destinationID)
      }
    } else {
      sendTimeOut()
    }
  }

  def receivedAllPendingACKS(messageType: String, ACKID: (Int, Int), senderID: Int): Boolean = {
    for (neighbourBroker <- NB diff List(senderID)) {
      if (ACKS.contains(messageType, ACKID, neighbourBroker) && !ACKS(messageType, ACKID, neighbourBroker)) {
        return false
      }
    }
    true
  }

  def findCoveringSub(): List[Int] = {
    // TODO find the covering sub. This is an optimization. s′∈ PRT : s' ⊆ s
    null
  }

  def sendTimeOut(): Unit = {
    // TODO To be implemented, not strictly necessary
  }

  override def execute(): Unit = {
    super.execute()
    super.startReceiver()

    while (true) {
      val rand = new scala.util.Random
      val randomNetworkDelay = 20 + rand.nextInt(( 40 - 20) + 1)
      Thread.sleep(randomNetworkDelay)

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
      }
    }
  }
}
