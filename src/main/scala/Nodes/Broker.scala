package Nodes

import Messaging.GuaranteeType._
import Messaging._
import Misc.ResourceUtilities
import Routing.RoutingTable

class Broker(override val ID: Int, val endpoints: List[Int]) extends Node(ID) {


  private val lastHops = scala.collection.mutable.Map[(String, (Int, Int)), Int]()

  private val SRT = new RoutingTable()
  private val PRT = new RoutingTable()
  private val NB = ResourceUtilities.getNeighbours(ID)
  private val IsActive = scala.collection.mutable.Map[(Int, Int), Boolean]()
  private val StoredPubs = scala.collection.mutable.Map[(Int, Int), List[Message]]()

  // Currently not used, its for GROUPID guarantee --> I think is related to the pibID guarantee, not interesting
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
      println("Forwarding Unadvertisement to " + hop)
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
      case TIME | GROUPID =>
        // Does this actually make sense?
        val timestampSubscription=message.timestamp

        advs.map(ad=>{
          val publications=
            StoredPubs.get(ad._1,ad._2).asInstanceOf[List[Message]].filter(_.timestamp.compareTo(timestampSubscription)>=0)
          publications.map(publication=>{
            sendMessage(new Message(getMessageID(), SocketData, message.sender.ID, publication.content, getCurrentTimestamp()), message.sender.ID)
          })
        })



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
        if (isEB){
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
            sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp()), hop)
          }
        }
      case TIME | GROUPID =>
        // Please check this
        val timestampSubscription=message.timestamp
        advs.map(ad=>{
          val publications=
            StoredPubs.get(ad._1,ad._2).asInstanceOf[List[Message]].filter(_.timestamp.compareTo(timestampSubscription)<=0)
          publications.map(publication=>{
            sendMessage(new Message(getMessageID(), SocketData, message.sender.ID, publication.content, getCurrentTimestamp()), message.sender.ID)
          })
        })
      case NONE =>
        for (hop <- nextHops) {
          println("Forwarding Unsubscription to " + hop)
          sendMessage(new Message(getMessageID(), SocketData, hop, content, getCurrentTimestamp()), hop)
        }
    }
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
    println("Sending " + messageType +" Ack Response to " + lastHop)

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
        if (ACK.messageType == "Message.Subscribe" && endpoints.contains(ACK.ID._1)) {
          IsActive += (ACK.ID -> true)
        }
        if (ACK.messageType == "Message.Unsubscribe" && endpoints.contains(ACK.ID._1)) {
          IsActive += (ACK.ID -> false)
        }
        val destinationID = lastHops(messageType, ACK.ID)
        println("Sending " + messageType + "  ACK Response to " + destinationID)
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

  def findCoveringSub(subscription: Subscription): List[Int] = {
    // TODO find the covering sub. This is an optimization. s′∈ PRT : s' ⊆ s

    // Does it make sense this? I am trying to find subs that cover this one
    val coverSubs: List[(Int, Int)] = PRT.findMatch(subscription)

    // Once I got the matches I just iterate like this
    var routesCover: Set[Int] = Set[Int]()
    for (sub <- coverSubs) {
      val candidateDestination = PRT.getRoute(sub)._1
      if (NB.contains(candidateDestination)) {
        routesCover += candidateDestination
      }
    }
    routesCover.toList
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
        receivedMessages+=message
        if(receivedMessages.toList.length>MaxNumberOFMessages){
          writeFileMessages("received")
        }
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
