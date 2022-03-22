package Nodes

import Messaging.GuaranteeType._
import Messaging._
import Nodes.ClientType.{ClientType, PUBLISHER, SUBSCRIBER}

import scala.collection.mutable
import scala.util.Random

class Client(override val ID: Int, val brokerID: Int, val mode: ClientType) extends Node(ID) {

  private val publicationList = mutable.Map[(Int, Int), Publication]()
  private val publicationsReceivedList = mutable.Map[(Int, Int), Publication]()
  private val waitingForACK = mutable.Map[(String, (Int, Int)),Int]()

  protected var numberOfSimulations = 0
  protected var simulationLimit = 5


  /**
   * Advertisement methods
   */
  def sendAdvertisement(pClass: String, pAttributes: (String, Int), guarantee: GuaranteeType): Unit = {
    println("Sending Advertisement to " + brokerID)

    val adID: (Int, Int) = (ID, counters("Advertisements"))
    val advertisement = Advertisement(adID, pClass, pAttributes)
    val content = Advertise(advertisement, guarantee)

    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp), brokerID)

    advertisementList += (adID -> advertisement)
    if (guarantee == ACK) waitingForACK += ((content.getClass.getName, adID) -> 1)
    counters += ("Advertisements" -> (counters("Advertisements") + 1))

    println(advertisementList)
  }

  def sendUnadvertisement(advertisement: Advertisement, guarantee: GuaranteeType): Unit = {
    println("Sending Unadvertisement to " + brokerID)

    val content = Unadvertise(advertisement, guarantee)
    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp), brokerID)

    advertisementList -= advertisement.ID
    if (guarantee == ACK) waitingForACK += ((content.getClass.getName, content.advertisement.ID) -> 1)

    println(advertisementList)
  }

  /**
   * Subscription methods
   */
  def sendSubscription(pClass: String, pAttributes: (String, Int), guarantee: GuaranteeType): Unit = {
    println("Sending Subscription to " + brokerID)

    val subID: (Int, Int) = (ID, counters("Subscriptions"))
    val subscription = Subscription(subID, pClass, pAttributes)
    val content = Subscribe(subscription, guarantee)

    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp), brokerID)

    subscriptionList += (subID -> subscription)
    if (guarantee == ACK) waitingForACK += ((content.getClass.getName, subID) -> 1)
    counters += ("Subscriptions" -> (counters("Subscriptions") + 1))

    println(subscriptionList)
  }

  def sendUnsubscription(subscription: Subscription, guarantee: GuaranteeType): Unit = {
    println("Sending Unsubscription to " + brokerID)

    val content = Unsubscribe(subscription, guarantee)
    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp), brokerID)

    subscriptionList -= subscription.ID
    if (guarantee == ACK) waitingForACK += ((content.getClass.getName, content.subscription.ID) -> 1)

    println(subscriptionList)
  }

  /**
   * Publication methods
   */
  def sendPublication(pClass: String, pAttributes: (String, Int), pContent: Int, guarantee: GuaranteeType): Unit = {
    println("Sending Publication to " + brokerID)

    val pubID: (Int, Int) = (ID, counters("Publications"))
    val publication = Publication(pubID, pClass, pAttributes, pContent)
    val content = Publish(publication, guarantee)

    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp), brokerID)

    publicationList += (pubID -> publication)
    if (guarantee == ACK) waitingForACK += ((content.getClass.getName, pubID) -> 1)
    counters += ("Publications" -> (counters("Publications") + 1))

    println(publicationList)
  }

  def receivePublication(message: Message): Unit = {
    val content: Publish = message.content.asInstanceOf[Publish]
    val pubID: (Int, Int) = content.publication.ID

    println("Receiving Publication from " + message.sender.ID + " with message: "
      + content.publication.pContent + " "
      + content.publication.pClass + " "
      + content.publication.pAttributes)

    publicationsReceivedList += (pubID -> content.publication)
  }

  /**
   * Ack methods
   */
  def receiveACK(message: Message): Unit = {
    println("Receiving ACK from " + message.sender.ID)

    val ACK = message.content.asInstanceOf[AckResponse]
    val messageType = ACK.messageType
    val ackCounter = waitingForACK((messageType, ACK.ID))

    waitingForACK -= ((messageType, ACK.ID))

    if (ACK.timeout && ackCounter < 4) {

      waitingForACK += (messageType, ACK.ID) -> (ackCounter + 1)

      ACK.messageType match {
        case "Messaging.Subscribe" =>
          val subscription = subscriptionList(ACK.ID)
          sendSubscription(subscription.pClass, subscription.pAttributes,GuaranteeType.ACK)
        case "Messaging.Advertise" =>
          val advertisement = advertisementList(ACK.ID)
          sendAdvertisement(advertisement.pClass, advertisement.pAttributes, GuaranteeType.ACK)
        //TODO: Add unsub/unad. This requires a rewriting of the way we store these.
        case "Messaging.Publish" =>
          val publication = publicationList(ACK.ID)
          //sendPublication()
        case "Messaging.Unsubscribe" =>
        case "Messaging.Unadvertise" =>
      }
      println("Resending message " + ACK.ID + " " + messageType)
    } else {
      println("Successfully installed " + ACK.ID + " " + messageType)
    }
  }

  /**
   * Simulate random  behaviour
   */
  private def simulateClientBehaviour(): Unit = {
    val option = randomGenerator.nextInt(300)

    var simulationExecution = false


    //TODO Make nice simulation with all types of messages. Also change topology. 12 broker nodes.
    // 1 with pub sub, 1 with pub, 1 with sub, 1 with sub sub, 1 with pub pub.
    // 5 EB, 7 B
    if (mode == PUBLISHER) {
      option match {
        case x if advertisementList.isEmpty  && x == 15 =>
          sendAdvertisement("Test", ("gt",10), NONE)
          simulationExecution = true
//        case x if advertisementList.nonEmpty && x == 5 =>
//          if (!waitingForACK.contains(("Messaging.Advertise", advertisementList.head._1))) {
//            sendUnadvertisement(advertisementList.head._2, ACK)
//          }
        case x if advertisementList.nonEmpty && x == 5 =>
          if (!waitingForACK.contains(("Messaging.Advertise", advertisementList.head._1))) {
              sendPublication("Test", ("gt", 10), 22, NONE)
          }
          simulationExecution = true
        case _ =>
      }
    }
    if (mode == SUBSCRIBER) {
      option match {
        case x if x == 10  =>
          sendSubscription("Test", ("gt", 8), NONE)
          simulationExecution = true
//        case x if subscriptionList.nonEmpty && x == 5 =>
//          if (!waitingForACK.contains(("Messaging.Subscribe", subscriptionList.head._1))) {
//            sendUnsubscription(subscriptionList.head._2, ACK)
//          }
        case _ =>
      }
    }

    if (simulationExecution) numberOfSimulations += 1

    if (numberOfSimulations == simulationLimit) println("Simulation Limit Reached. Now only listening to new messages.")
  }

  /**
   * Open ReceiverSocket and actively listen for messages.
   * Simulate random Client Pub/Sub behaviour.
   */
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
          case _ : AckResponse => receiveACK(message)
          case _ : Publish => receivePublication(message)
        }
      }

      if (numberOfSimulations < simulationLimit) simulateClientBehaviour()
    }
  }
}
