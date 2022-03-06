package Nodes

import Misc.ResourceUtilities
import Messaging._
import Nodes.ClientType.{ClientType, PUBLISHER, SUBSCRIBER}

class Client(override val ID: Int, val brokerID: Int, val mode: ClientType) extends Node(ID) {

  // TODO create the logic around these lists
  private val subscriptionList = scala.collection.mutable.Map[Int, Subscription]()
  private val advertisementList = scala.collection.mutable.Map[Int, Advertisement]()

  /**
   * Advertisement methods
   */
  def sendAdvertisement(): Unit = {
    println("Sending Advertisement")

    val adID: Int = ID + counters("Advertisements")
    val advertisement = Advertisement(adID)
    val content = Advertise(adID)

    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp()), brokerID)

    advertisementList += (adID -> advertisement)

    // TODO finish method
  }

  def sendUnadvertisement(): Unit = {
    println("Sending Unadvertisement")
    // TODO To be implemented
  }

  /**
   * Subscription methods
   */
  def sendSubscription(): Unit = {
    println("Sending Subscription")
    // TODO To be implemented
  }
  def sendUnsubscription(): Unit = {
    println("Sending Unsubscription")
    // TODO To be implemented
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

  def requestPublication(): Unit = {
    println("Requesting Publication")
    // TODO To be implemented
  }

  /**
   * Ack methods
   */
  def receiveAckResponse(message: Message): Unit = {
    println("Receiving Ack Response")
    // TODO To be implemented
  }

  def sendAckRequest(): Unit = {
    println("Sending Ack Request")
    // TODO To be implemented
  }

  private def simulateClientBehaviour(): Unit = {
    val option = randomGenerator.nextInt(100)
    /**
     * Simulate random publisher behaviour
     */
    if (mode == PUBLISHER) {
      option match {
        case x if x > 0 && x <= 19 => sendAdvertisement()
        case 20 => sendUnadvertisement()
        case x if x > 20 && x <= 29 => sendPublication()
        case 30 => sendAckRequest()
        case _ =>
      }
    }
    /**
     * Simulate random subscriber behaviour
     */
    if (mode == SUBSCRIBER) {
      option match {
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
      Thread.sleep(1000)
      println("Waiting for messages...")

      while (!receiver.isQueueEmpty) {
        println("Retrieving a new message...")
        val message = receiver.getFirstFromQueue()

        // TODO define all types
        message.content match {
          case _ : AckResponse => receiveAckResponse(message)
          case _ : Publication => receivePublication(message)
        }
        receiver.emptyQueue() // Process the message, this should be individual
      }
      simulateClientBehaviour()
    }
  }
}
