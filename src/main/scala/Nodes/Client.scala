package Nodes

import Messaging.GuaranteeType._
import Messaging._
import Nodes.ClientType.{ClientType, PUBLISHER, SUBSCRIBER}

class Client(override val ID: Int, val brokerID: Int, val mode: ClientType) extends Node(ID) {

  private val subscriptionList = scala.collection.mutable.Map[(Int, Int), Subscription]()
  private val advertisementList = scala.collection.mutable.Map[(Int, Int), Advertisement]()

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
    counters += ("Advertisements" -> (counters("Advertisements")+1))

    println(advertisementList)
    // TODO finish method
  }

  def sendUnadvertisement(advertisement: Advertisement, guarantee: GuaranteeType): Unit = {
    println("Sending Unadvertisement to " + brokerID)

    val content = Unadvertise(advertisement, guarantee)
    sendMessage(new Message(getMessageID(), SocketData, brokerID, content, getCurrentTimestamp()), brokerID)

    advertisementList -= ((ID, counters("Advertisements")))

    println(advertisementList)
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

  def receiveACK(message: Message): Unit = {
    println("Receiving ACK from " + message.sender.ID)

    val ACK = message.content.asInstanceOf[AckResponse]
    val messageType = ACK.messageType

    println("Succesfully installed " + ACK.ID + " " + messageType)
  }

  /**
   * Simulate random  behaviour
   */
  private def simulateClientBehaviour(): Unit = {
    val option = randomGenerator.nextInt(5000)

    if (mode == PUBLISHER) {
      option match {
        case x if x > 0 && x <= 19 => sendAdvertisement("Test", List((x: Int) => x < 10), ACK)
        //case 20 => sendUnadvertisement()
        //case x if x > 20 && x <= 29 => sendUnadvertisement()
        //case 30 => sendACK()
        case _ =>
      }
    }
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
