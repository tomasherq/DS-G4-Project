package Nodes

import Messaging.GuaranteeType._
import Messaging._
import Misc.ResourceUtilities
import Routing.RoutingTable
import scala.collection.mutable.ListBuffer

import scala.collection.mutable
import scala.util.Random

class Broker(override val ID: Int, val endpoints: List[Int]) extends Node(ID) {

  private val lastHops = mutable.Map[(String, (Int, Int)), Int]()
  private val SRT = new RoutingTable()
  private val PRT = new RoutingTable()
  private val NB = ResourceUtilities.getNeighbours(ID)
  private val IsActive = mutable.Map[(Int, Int), Boolean]()
  private val StoredPubs = mutable.Map[(Int, Int), List[Message]]()
  private val promiseList = scala.collection.mutable.Map[(Int, Int), Message]()

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
    if (advertisementList.contains(content.advertisement.ID)) {
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
    if (subscriptionList.contains(content.subscription.ID)) {
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
      sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop) // Flood to next hops
    }
    processAdvertisement(content)

    // Check if we have some suitable sub in the promise list
    val promises: List[Message] = findMatchReverse(content.advertisement)
    for (item <- promises) {
      receiveSubscription(new Message(getMessageID(), item.sender, item.destination, item.content, getCurrentTimestamp()))
    }
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
      println("Forwarding Unadvertisement to " + hop)
      sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop) // Flood to next hops
    }
    clearAdvertisement(content)
  }

  /**
   * Subscription methods
   */
  def receiveSubscription(message: Message): Unit = {
    println("Receiving Subscription")

    val content: Subscribe = message.content.asInstanceOf[Subscribe]
    val messageType: String = content.getClass.toString
    val lastHop: Int = message.sender.ID
    val s: (Int, Int) = content.subscription.ID

    lastHops += ((messageType, s) -> lastHop)

    val advs: List[(Int, Int)] = SRT.findMatch(content.subscription)

    var nextHopsSet: Set[Int] = Set[Int]()
    if (advs.isEmpty) {
      promiseList += (content.subscription.ID -> message)
      println("Sub added to promise list: " + promiseList)
    }
    else {
      for (ad <- advs) {
        val candidateDestination = SRT.getRoute(ad)._1
        if (NB.contains(candidateDestination)) {
          nextHopsSet += candidateDestination
        }
      }
    }

    val nextHops: List[Int] = nextHopsSet.toList diff List(lastHop)
    val coverSub: List[Int] = findCoveringSub(content.subscription)
    val isEB = (NB diff List(lastHop)).isEmpty

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
            sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop)
          }
        }
      case TIME => //TODO Check if this works
        val timestampSubscription = message.timestamp
        advs.map(ad => {
          val publications = StoredPubs.get(ad._1, ad._2).asInstanceOf[List[Message]].filter(_.timestamp > timestampSubscription)
          publications.map(publication => {
            sendMessage(new Message(getMessageID(), SocketData, message.sender.ID, publication.content, getCurrentTimestamp), message.sender.ID)
          })
        })
      case NONE =>
        if ((coverSub.nonEmpty || nextHops.isEmpty) && isEB) {
          IsActive += (s -> true)
        }
        for (hop <- nextHops) {
          println("Forwarding Subscription to " + hop)
          sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop)
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

    lastHops += ((messageType, s) -> lastHop)

    val advs: List[(Int, Int)] = SRT.findMatch(content.subscription)
    var nextHopsSet: Set[Int] = Set[Int]()
    for (ad <- advs) {
      val candidateDestination = SRT.getRoute(ad)._1
      if (NB.contains(candidateDestination)) {
        nextHopsSet += candidateDestination
      }
    }

    val nextHops: List[Int] = nextHopsSet.toList diff List(lastHop)
    val coverSub: List[Int] = findCoveringSub(content.subscription)
    val isEB = (NB diff List(lastHop)).isEmpty

    PRT.deleteRoute(s)

    content.guarantee match {
      case ACK =>
        if (isEB) {
          IsActive += (s -> false)
        }
        if (coverSub.nonEmpty || nextHops.isEmpty) {
          if (isEB) {
            IsActive += (s -> false)
          }
          sendACK(messageType, s, lastHop)
        } else {
          for (hop <- nextHops) {
            ACKS += ((messageType, s, hop) -> false)
          }
          startAckTimer(messageType, s)
          for (hop <- nextHops) {
            println("Forwarding Unsubscription to " + hop)
            sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop)
          }
        }
      case TIME => //TODO Check if this works
        val timestampSubscription = message.timestamp
        advs.map(ad => {
          val publications = StoredPubs.get(ad._1, ad._2).asInstanceOf[List[Message]].filter(_.timestamp > timestampSubscription)
          publications.map(publication => {
            sendMessage(new Message(getMessageID(), SocketData, message.sender.ID, publication.content, getCurrentTimestamp), message.sender.ID)
          })
        })
      case NONE =>
        if ((coverSub.nonEmpty || nextHops.isEmpty) && isEB) {
          IsActive += (s -> false)
        }
        for (hop <- nextHops) {
          println("Forwarding Unsubscription to " + hop)
          sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop)
        }
    }
    clearSubscription(content)
  }

  /**
   * Publication methods
   */
  def receivePublication(message: Message): Unit = {
    println("Receiving Publication from " + message.sender.ID)

    val content: Publish = message.content.asInstanceOf[Publish]
    val messageType: String = content.getClass.toString
    val lastHop: Int = message.sender.ID
    val p: (Int, Int) = content.publication.ID

    lastHops += ((messageType, p) -> lastHop)
    val isEB = (NB diff List(lastHop)).isEmpty

    if (isEB) {
      if (StoredPubs.contains(content.publication.ID)) {
        val storedPubList: List[Message] = message :: StoredPubs(content.publication.ID)
        StoredPubs += (content.publication.ID -> storedPubList)
      } else {
        StoredPubs += (content.publication.ID -> List(message))
      }
    }

    val subs: List[(Int, Int)] = PRT.findMatch(content.publication)

    println(subs)

    var nextHopsSet: Set[Int] = Set[Int]()

    for (s <- subs) {
      content.guarantee match {
        case ACK | NONE =>
          //if (IsActive(s)) {
            val candidateDestination = PRT.getRoute(s)._1
            if (NB.contains(candidateDestination)) {
              nextHopsSet += candidateDestination
            }
         // }
        case TIME => //TODO
      }
    }

    val nextHops: List[Int] = nextHopsSet.toList diff List(lastHop)

    for (hop <- nextHops) {
      println("Forwarding Publication to " + hop)
      sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop) // Flood to next hops
    }

    if (content.guarantee == ACK) {
      if ((nextHops intersect NB).isEmpty) {
        sendACK(messageType, p, lastHop)
      } else {
        startAckTimer(messageType, p)
        for (hop <- nextHops) {
          if (NB.contains(hop)) {
            ACKS += ((messageType, p, hop) -> false)
          }
        }
      }
    }
  }

  /**
   * Ack methods
   */
  def sendACK(messageType: String, ID: (Int, Int), lastHop: Int): Unit = {
    println("Sending " + messageType +" Ack Response to " + lastHop)

    val ACK = AckResponse(messageType, ID)
    sendMessage(new Message(getMessageID(), SocketData, lastHop, ACK, getCurrentTimestamp), lastHop)
  }

  def receiveACK(message: Message): Unit = {
    println("Receiving Ack Response from " + message.sender.ID)

    val ACK = message.content.asInstanceOf[AckResponse]
    val messageType = ACK.messageType

    if ((message.timestamp - timestamps(messageType, ACK.ID)) < 1500 && !ACK.timeout) {

      println("Processing of ACK took: " + (message.timestamp - timestamps(messageType, ACK.ID)) + "ms")

      ACKS += ((messageType, ACK.ID, message.sender.ID) -> true)

      if (receivedAllPendingACKS(messageType, ACK.ID, message.sender.ID)) {
        if (ACK.messageType == "Message.Subscribe" && endpoints.contains(ACK.ID._1)) {
          IsActive += (ACK.ID -> true)
        }
        if (ACK.messageType == "Message.Unsubscribe" && endpoints.contains(ACK.ID._1)) {
          IsActive += (ACK.ID -> false)
        }
        val destinationID = lastHops(messageType, ACK.ID)
        println("Sending " + messageType + "  ACK Response to " + destinationID)
        sendMessage(new Message(getMessageID(), SocketData, destinationID, ACK, getCurrentTimestamp), destinationID)
      }
    } else {
      sendTimeOut(ACK)
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

  def findCoveringSub(subscription: Subscription): List[Int] = {
    // TODO Check if this works. s′∈ PRT : s' ⊆ s
    val coverSubs: List[(Int, Int)] = PRT.findMatch(subscription)
    var routesCover: Set[Int] = Set[Int]()
    for (sub <- coverSubs) {
      val candidateDestination = PRT.getRoute(sub)._1
      if (NB.contains(candidateDestination)) {
        routesCover += candidateDestination
      }
    }
    routesCover.toList
  }

  def sendTimeOut(ACK:AckResponse): Unit = {
    val destinationID = lastHops(ACK.messageType, ACK.ID)

    ACK.messageType match {
      case "Message.Subscription" =>
        PRT.deleteRoute(ACK.ID)
        subscriptionList -= ACK.ID
      case "Message.Advertisement" =>
        SRT.deleteRoute(ACK.ID)
        advertisementList -= ACK.ID
      //TODO: Add unsub/unad. This requires a rewriting of the way we store these.
    }
    ACK.timeout = true

    sendMessage(new Message(getMessageID(), SocketData, destinationID, ACK, getCurrentTimestamp), destinationID)
  }

  override def execute(): Unit = {
    super.execute()
    super.startReceiver()

    while (true) {
      val rand = new Random
      val randomNetworkDelay = 20 + rand.nextInt(( 40 - 20) + 1)
      Thread.sleep(randomNetworkDelay)

      while (!receiver.isQueueEmpty) {
        println("Retrieving a new message...")
        val message = receiver.getFirstFromQueue()

        receivedMessages += message
        if (receivedMessages.toList.length > messageSaveThreshold) {
          writeFileMessages("received")
        }

        message.content match {
          case _ : Advertise => receiveAdvertisement(message)
          case _ : Unadvertise => receiveUnadvertisement(message)
          case _ : Subscribe => receiveSubscription(message)
          case _ : Publish => receivePublication(message)
          case _ : Unsubscribe => receiveUnsubscription(message)
          case _ : AckResponse => receiveACK(message)
        }
      }
    }
  }

  def findMatchReverse(advertisement: Advertisement): List[Message] = {
    // TODO: Also match attributes
    val matches: ListBuffer[Message] = ListBuffer[Message]()
    for (item <- promiseList) {
      val pClass = item._2.content.asInstanceOf[Subscribe].subscription.pClass
      if (pClass.equals(advertisement.pClass)){
        matches += item._2
      }
    }
    matches.toList
  }
}
