package Nodes

import Messaging.GuaranteeType._
import Messaging._
import Nodes.ClientType.{ClientType, PUBLISHER, SUBSCRIBER}

import scala.collection.mutable

class Client(override val ID: Int, val brokerID: Int, val mode: ClientType) extends Node(ID) {

  private val publicationList = mutable.Map[(Int, Int), Publication]()
  private val publicationsReceivedList = mutable.Map[(Int, Int), Publication]()
  private val waitingForACK = mutable.Map[(String, (Int, Int)),Int]()

  protected var numberOfSimulations = 0
  protected var simulationLimit = 250
  protected var guaranteeType = ACK
  protected var startDelayBaseline = false

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
  }

  def sendUnadvertisement(advertisement: Advertisement, guarantee: GuaranteeType): Unit = {
    println("Sending Unadvertisement to " + brokerID)

    val content = Unadvertise(advertisement, guarantee)
    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp), brokerID)

    advertisementList -= advertisement.ID
    if (guarantee == ACK) waitingForACK += ((content.getClass.getName, content.advertisement.ID) -> 1)
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
  }

  def sendUnsubscription(subscription: Subscription, guarantee: GuaranteeType): Unit = {
    println("Sending Unsubscription to " + brokerID)

    val content = Unsubscribe(subscription, guarantee)
    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp), brokerID)

    subscriptionList -= subscription.ID
    if (guarantee == ACK) waitingForACK += ((content.getClass.getName, content.subscription.ID) -> 1)
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
      sendMessage(new Message(getMessageID(), SocketData, message.destination, message.content, getCurrentTimestamp), message.destination)

      println("Resending message " + ACK.ID + " " + messageType)
    }else if(ACK.timeout  && ackCounter > 3){
      println("Ack counter surpassed: " + ACK.ID + " " + messageType)
    }
    else {
      println("Successfully installed " + ACK.ID + " " + messageType)
    }
  }

  /**
   * Simulate random  behaviour
   */
  private def simulateClientBehaviour(): Unit = {
    val option = randomGenerator.nextInt(2500)

    var simulationExecution = false

    val classes: List[String] = List("Apple", "Tesla", "Disney", "Microsoft")
    val operators: List[String] = List("gt", "lt")
    val randomOperator: String = operators(randomGenerator.nextInt(operators.length))
    val randomClass: String = classes(randomGenerator.nextInt(classes.length))
    val randomValue: Int = (randomGenerator.nextInt(20) * 5) + 5

    if (mode == PUBLISHER) {
      option match {

        case x if advertisementList.size < 10 && x > 0 && x < 50 =>
          sendAdvertisement(randomClass, (randomOperator, randomValue), guaranteeType)
          simulationExecution = true

        case x if advertisementList.size > 9 && x > 0 && x < 100 =>
          if (guaranteeType == NONE && !startDelayBaseline) {
            Thread.sleep(10000)
            startDelayBaseline = true
          }
          val randomAdvertisementKey = advertisementList.keys.toList(randomGenerator.nextInt(advertisementList.size))
          val activeAdvertisement = advertisementList(randomAdvertisementKey)
          val valueAdvertisement = activeAdvertisement.pAttributes._2
          val operatorAdvertisement  = activeAdvertisement.pAttributes._1
          val offset = 101 - valueAdvertisement
          val publicationValue = operatorAdvertisement match {
            case "gt" => valueAdvertisement + randomGenerator.nextInt(offset)
            case "lt" => valueAdvertisement - randomGenerator.nextInt(valueAdvertisement)
          }
          if (guaranteeType!=ACK || !waitingForACK.contains(("Messaging.Advertise", activeAdvertisement.ID))) {
            sendPublication(activeAdvertisement.pClass, activeAdvertisement.pAttributes, publicationValue, guaranteeType)
            simulationExecution = true
          }

        case _ =>
      }
    }

    if (mode == SUBSCRIBER) {
      option match {

        case x if subscriptionList.isEmpty =>
          sendSubscription(randomClass, (randomOperator, randomValue), guaranteeType)
          simulationExecution = true

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

    // Safe wait period for brokers to start
    Thread.sleep(3000)

    // Safe wait period for advertisements + release a subscriber every second
    if (mode == SUBSCRIBER) {
      Thread.sleep(29000 + (ID * 1000))
    }

    while (true) {

      val randomNetworkDelay = 10 + randomGenerator.nextInt(40)
      Thread.sleep(randomNetworkDelay)

      while (!receiver.isQueueEmpty) {
        println("Retrieving a new message...")
        val message = receiver.getFirstFromQueue()

        writeFileMessages("received",message)

        message.content match {
          case _ : AckResponse => receiveACK(message)
          case _ : Publish => receivePublication(message)
        }
      }

      if (numberOfSimulations < simulationLimit) simulateClientBehaviour()
    }
  }
}
