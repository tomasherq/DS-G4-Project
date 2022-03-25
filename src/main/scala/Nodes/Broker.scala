package Nodes

import Messaging.GuaranteeType._
import Messaging._
import Routing.RoutingTable

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class Broker(override val ID: Int, val NB: List[Int]) extends Node(ID) {

  private val lastHops = mutable.Map[(String, (Int, Int)), Int]()
  private val SRT = new RoutingTable()
  private val PRT = new RoutingTable()
  private val IsActive = mutable.Map[(Int, Int), Boolean]()
  private val StoredPubs = mutable.Map[(Int, Int), List[Message]]()
  private val promiseList = mutable.Map[(Int, Int), Message]()

  /**
   * Advertisement methods
   */
  def processAdvertisement(content: Advertise): Unit = {
    if (!advertisementList.contains(content.advertisement.ID)) {
      advertisementList += (content.advertisement.ID -> Advertisement(content.advertisement.ID, content.advertisement.pClass, content.advertisement.pAttributes))
    }
  }

  def clearAdvertisement(content: Unadvertise): Unit = {
    if (advertisementList.contains(content.advertisement.ID)) {
      advertisementList -= content.advertisement.ID
    }
  }

  def processSubscription(content: Subscribe): Unit = {
    if (!subscriptionList.contains(content.subscription.ID)) {
      subscriptionList += (content.subscription.ID -> Subscription(content.subscription.ID, content.subscription.pClass, content.subscription.pAttributes))
    }
  }

  def clearSubscription(content: Unsubscribe): Unit = {
    if (subscriptionList.contains(content.subscription.ID)) {
      subscriptionList -= content.subscription.ID
    }
  }

  def receiveAdvertisement(message: Message): Unit = {
    println("Receiving Advertisement from " + message.sender.ID)

    val content: Advertise = message.content.asInstanceOf[Advertise]
    val messageType: String = content.getClass.getName
    val lastHop: Int = message.sender.ID
    val a: (Int, Int) = content.advertisement.ID

    lastHops += ((messageType, a) -> lastHop)

    SRT.addRoute(a, lastHop, content.advertisement.pClass, content.advertisement.pAttributes)

    val nextHops: List[Int] = NB diff List(lastHop)

    for (hop <- nextHops) {
      println("Forwarding Advertisement to " + hop)
      sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop) // Flood to next hops
    }

    processAdvertisement(content)

    val promises: List[Message] = findPromiseMatch(content.advertisement)
    for (item <- promises) {
      println("[PROMISE] Forwarding subscription to " + item.destination)
      receiveSubscription(item)
      promiseList -= item.content.asInstanceOf[Subscribe].subscription.ID
    }

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

  }

  def receiveUnadvertisement(message: Message): Unit = {
    println("Receiving Unadvertisement from " + message.sender.ID)

    val content: Unadvertise = message.content.asInstanceOf[Unadvertise]
    val messageType: String = content.getClass.getName
    val lastHop: Int = message.sender.ID
    val a: (Int, Int) = content.advertisement.ID

    lastHops += ((messageType, a) -> lastHop)

    SRT.deleteRoute(a)

    val nextHops: List[Int] = NB diff List(lastHop)

    for (hop <- nextHops) {
      println("Forwarding Unadvertisement to " + hop)
      sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop) // Flood to next hops
    }

    clearAdvertisement(content)

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
  }

  /**
   * Subscription methods
   */
  def receiveSubscription(message: Message): Unit = {
    println("Receiving Subscription")

    val content: Subscribe = message.content.asInstanceOf[Subscribe]
    val messageType: String = content.getClass.getName
    val lastHop: Int = message.sender.ID
    val s: (Int, Int) = content.subscription.ID

    lastHops += ((messageType, s) -> lastHop)

    val advs: List[(Int, Int)] = SRT.findMatch(content.subscription)

    var nextHopsSet: Set[Int] = Set[Int]()

    if (advs.isEmpty) {
      promiseList += (content.subscription.ID -> message)
      println("[PROMISE] Subscription added to promise buffer: " + promiseList)
      return
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
    val isEB = (NB diff List(lastHop)).isEmpty

    PRT.addRoute(s, lastHop, content.subscription.pClass, content.subscription.pAttributes)

    content.guarantee match {
      case ACK =>
        if (isEB) {
          IsActive += (s -> false)
        }
        if (nextHops.isEmpty) {
          if (isEB) {
            IsActive += (s -> true)
          }
          sendACK(messageType, s, lastHop)
        } else {
          for (hop <- nextHops) {
            ACKS += ((messageType, s, hop) -> false)
          }
          for (hop <- nextHops) {
            println("Forwarding Subscription to " + hop)
            sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop)
          }
          startAckTimer(messageType, s)
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
        IsActive += (s -> true)
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
    val messageType: String = content.getClass.getName
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
    val isEB = (NB diff List(lastHop)).isEmpty

    PRT.deleteRoute(s)

    content.guarantee match {
      case ACK =>
        if (isEB) {
          IsActive += (s -> false)
        }
        if (nextHops.isEmpty) {
          if (isEB) {
            IsActive += (s -> false)
          }
          sendACK(messageType, s, lastHop)
        } else {
          for (hop <- nextHops) {
            ACKS += ((messageType, s, hop) -> false)
          }
          for (hop <- nextHops) {
            println("Forwarding Unsubscription to " + hop)
            sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp), hop)
          }
          startAckTimer(messageType, s)
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
        if (nextHops.isEmpty && isEB) {
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
    val messageType: String = content.getClass.getName
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

    var nextHopsSet: Set[Int] = Set[Int]()

    for (s <- subs) {
      content.guarantee match {
        case ACK | NONE =>
          if (IsActive.contains(s)) {
            val candidateDestination = PRT.getRoute(s)._1
            nextHopsSet += candidateDestination
          }
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

    if (timestamps.contains(messageType, ACK.ID) && !ACK.timeout) {

      println("Processing of ACK " + ACK.ID + " took: " + (getCurrentTimestamp - timestamps(messageType, ACK.ID)) + "ms")

      timestamps -= ((messageType, ACK.ID))

      ACKS += ((messageType, ACK.ID, message.sender.ID) -> true)

      if (receivedAllPendingACKS(messageType, ACK.ID, message.sender.ID)) {

        if (ACK.messageType == "Messaging.Subscribe") {
          IsActive += (ACK.ID -> true)
        }
        if (ACK.messageType == "Messaging.Unsubscribe") {
          IsActive += (ACK.ID -> false)
        }

        val destinationID = lastHops(messageType, ACK.ID)
        println("Sending " + messageType + "  ACK Response to " + destinationID)
        sendMessage(new Message(getMessageID(), SocketData, destinationID, ACK, getCurrentTimestamp), destinationID)
      }
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

  def findPromiseMatch(advertisement: Advertisement): List[Message] = {
    val matches: ListBuffer[Message] = ListBuffer[Message]()

    for (item <- promiseList) {

      val promisedMessage = item._2.content.asInstanceOf[Subscribe]

      val pClass = promisedMessage.subscription.pClass
      val pAttributes = promisedMessage.subscription.pAttributes
      val valueAdvertisement = advertisement.pAttributes._2
      val valueBoundSubscription = pAttributes._2
      val operatorAdvertisement = advertisement.pAttributes._1
      val operatorSubscription = pAttributes._1

      if (pClass.equals(advertisement.pClass)){

        var validAdvertisement = false

        if (operatorSubscription.equals(operatorAdvertisement)) {

          validAdvertisement = operatorSubscription match {
            case "gt" => valueAdvertisement >= valueBoundSubscription
            case "lt" => valueAdvertisement <= valueBoundSubscription
            case "e" => valueAdvertisement == valueBoundSubscription
            case "ne" => valueAdvertisement == valueBoundSubscription
          }
        }

        if(validAdvertisement) {
          matches += item._2
        }
      }
    }
    matches.toList
  }

  def startAckTimer(messageType: String, ID: (Int, Int)): Unit = {
    timestamps += ((messageType, ID) -> getCurrentTimestamp)

    val t1 = new Thread(new Runnable() {
      override def run(): Unit = {
        while (true) {
          Thread.sleep(200)
          if (!timestamps.contains(messageType, ID)) return
          if (getCurrentTimestamp - timestamps(messageType, ID) > 2000) {
            timestamps -= ((messageType, ID))
            sendTimeOut(AckResponse(messageType, ID, true))
          }
        }
      }
    })
    t1.start()

  }

  def sendTimeOut(ACK: AckResponse): Unit = {
    println("[TIMEOUT] " + ACK.ID + " " + ACK.messageType)
    val destinationID = lastHops(ACK.messageType, ACK.ID)

    ACK.messageType match {
      case "Messaging.Subscribe" =>
        PRT.deleteRoute(ACK.ID)
        subscriptionList -= ACK.ID
      case "Messaging.Advertise" =>
        SRT.deleteRoute(ACK.ID)
        advertisementList -= ACK.ID
      case _ =>
    }

    sendMessage(new Message(getMessageID(), SocketData, destinationID, ACK, getCurrentTimestamp), destinationID)
  }

  override def execute(): Unit = {
    super.execute()
    super.startReceiver()

    while (true) {
      val rand = new Random
      val randomNetworkDelay = 10 + rand.nextInt(20)
      Thread.sleep(randomNetworkDelay)

      while (!receiver.isQueueEmpty) {
        println("Retrieving a new message...")
        val message = receiver.getFirstFromQueue()

        writeFileMessages("received",message)


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
}
