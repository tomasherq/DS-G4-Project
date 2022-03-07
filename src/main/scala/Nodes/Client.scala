package Nodes

import Messaging.GuaranteeType._
import Messaging._
import Nodes.ClientType.{ClientType, PUBLISHER, SUBSCRIBER}

class Client(override val ID: Int, val brokerID: Int, val mode: ClientType) extends Node(ID) {

  private val publicationList = scala.collection.mutable.Map[(Int, Int), Publication]()
  private val publicationsReceivedList = scala.collection.mutable.Map[(Int, Int), Publication]()
  private val subscriptionList = scala.collection.mutable.Map[(Int, Int), Subscription]()
  private val advertisementList = scala.collection.mutable.Map[(Int, Int), Advertisement]()
  private val waitingForACK = scala.collection.mutable.Set[(String, (Int, Int))]()

  /**
   * Advertisement methods
   */
  def sendAdvertisement(pClass: String, pAttributes: List[Int => Boolean], guarantee: GuaranteeType): Unit = {
    println("Sending Advertisement to " + brokerID)

    val adID: (Int, Int) = (ID, counters("Advertisements"))
    val advertisement = Advertisement(adID, pClass, pAttributes)
    val content = Advertise(advertisement, guarantee)

    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp()), brokerID)

    advertisementList += (adID -> advertisement)
    if (guarantee == ACK) waitingForACK += ((content.getClass.toString, adID))
    counters += ("Advertisements" -> (counters("Advertisements")+1))

    println(advertisementList)
  }

  def sendUnadvertisement(advertisement: Advertisement, guarantee: GuaranteeType): Unit = {
    println("Sending Unadvertisement to " + brokerID)

    val content = Unadvertise(advertisement, guarantee)
    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp()), brokerID)

    advertisementList -= advertisement.ID
    if (guarantee == ACK) waitingForACK += ((content.getClass.toString, content.advertisement.ID))

    println(advertisementList)
  }

  /**
   * Subscription methods
   */
  def sendSubscription(pClass: String, pAttributes: List[Int => Boolean], guarantee: GuaranteeType): Unit = {
    println("Sending Subscription to " + brokerID)

    val subID: (Int, Int) = (ID, counters("Subscriptions"))
    val subscription = Subscription(subID, pClass, pAttributes)
    val content = Subscribe(subscription, guarantee)

    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp()), brokerID)

    subscriptionList += (subID -> subscription)
    if (guarantee == ACK) waitingForACK += ((content.getClass.toString, subID))
    counters += ("Subscriptions" -> (counters("Subscriptions")+1))

    println(subscriptionList)
  }

  def sendUnsubscription(subscription: Subscription, guarantee: GuaranteeType): Unit = {
    println("Sending Unsubscription to " + brokerID)

    val content = Unsubscribe(subscription, guarantee)
    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp()), brokerID)

    subscriptionList -= subscription.ID
    if (guarantee == ACK) waitingForACK += ((content.getClass.toString, content.subscription.ID))

    println(subscriptionList)
  }

  /**
   * Publication methods
   */
  def sendPublication(): Unit = {
    println("Sending Publication")
    // TODO To be implemented
  }

  def receivePublication(message: Message): Unit = {
    println("Receiving Publication")
    // TODO To be implemented
  }

  /**
   * Ack methods
   */
  def receiveACK(message: Message): Unit = {
    println("Receiving ACK from " + message.sender.ID)

    val ACK = message.content.asInstanceOf[AckResponse]
    val messageType = ACK.messageType

    waitingForACK -= ((messageType, ACK.ID))

    println("Successfully installed " + ACK.ID + " " + messageType)
  }

  /**
   * Simulate random  behaviour
   */
  private def simulateClientBehaviour(): Unit = {
    val option = randomGenerator.nextInt(500)

    if (mode == PUBLISHER) {
      option match {
        case x if x == 1 =>
          sendAdvertisement("Test", List((x: Int) => x < 10), ACK)
        case x if advertisementList.nonEmpty && x == 5 =>
          if (!waitingForACK.contains("Message.Advertise", advertisementList.head._1)) {
            sendUnadvertisement(advertisementList.head._2, ACK)
          }
        case _ =>
      }
    }
    if (mode == SUBSCRIBER) {
      option match {
        case x if x == 10  =>
          sendSubscription("Test", List((x: Int) => x < 10), ACK)
        case x if subscriptionList.nonEmpty && x == 5 =>
          if (!waitingForACK.contains("Message.Subscribe", subscriptionList.head._1)) {
            sendUnsubscription(subscriptionList.head._2, ACK)
          }
        case _ =>
      }
    }
  }

  /**
   * Open ReceiverSocket and actively listen for messages.
   * Simulate random Client Pub/Sub behaviour.
   */
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
          case _ : AckResponse => receiveACK(message)
          case _ : Publication => receivePublication(message)
        }
      }
      simulateClientBehaviour()
    }
  }
}
