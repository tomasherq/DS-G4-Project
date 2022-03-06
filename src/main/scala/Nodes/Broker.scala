package Nodes

import Messaging._
import Misc.ResourceUtilities
import Routing.RoutingTable

class Broker(override val ID: Int) extends Node(ID) {

  // TODO may need to be changed, see needed datastructures
  private val subscriptionList = scala.collection.mutable.Map[Int, Subscription]()
  private val advertisementList = scala.collection.mutable.Map[Int, Advertisement]()
  private val subscriberList = scala.collection.mutable.Map[Int, Advertisement]()

  private val SRT = new RoutingTable()
  private val PRT = new RoutingTable()
  private val NB = ResourceUtilities.getNeighbours(ID)
  private val ACKS = scala.collection.mutable.Map[(Int, Int), List[Int]]() //Tuple is (msg, link) -> List of acks
  private val Groups = List[Int]()
  private val IsActive = scala.collection.mutable.Map[Int, Boolean]() // AdvertisementID
  private val StoredPubs = scala.collection.mutable.Map[Int, List[Int]]() // PublisherID -> list of publications

  /**
   * This method will lookup a candidate edge broker that can reach the client if this broker is not its edge broker
   */
  def forwardMessage(): Unit = {
    println("Forwarding Message")
    // TODO To implement
  }

  /**
   * Advertisement methods
   */
  def receiveAdvertisement(message: Message): Unit = {
    println("Receiving Advertisement")

    val content:Advertise=message.content.asInstanceOf[Advertise]

    if(!subscriptionList.contains(content.advertisementID)) {
      subscriptionList += (content.advertisementID -> Subscription(message.SocketData.ID))
    }
  }
  def receiveUnadvertisement(message: Message): Unit = {
    println("Receiving Unadvertisement")
    val content:Advertise = message.content.asInstanceOf[Advertise]

    if(subscriptionList.contains(content.advertisementID)) {
      subscriptionList -= content.advertisementID
    }
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

  /**
   * Publication methods
   */
  def receivePubRequest(message: Message): Unit = {
    println("Receiving Publish Request")
    // TODO To be implemented
  }

  /**
   * Ack methods
   */
  def sendAckResponse(): Unit = {
    println("Sending Ack Response")
    // TODO To be implemented
  }

  def receiveAckRequest(message: Message): Unit = {
    println("Sending Ack Request")
    // TODO To be implemented
  }

  override def execute(): Unit = {
    super.execute()
    super.startReceiver()

    while (true) {
      Thread.sleep(1000)
      println("Waiting for messages...")

      while (!receiver.isQueueEmpty) {
        println("Parsing a new message...")
        val message = receiver.getFirstFromQueue()

        if(SRT.hasRoute(message.SocketData.ID) ) {
          SRT.addRoute(message.SocketData)
        }

        // TODO define all types
        message.content match {
          case _ : Advertise => receiveAdvertisement(message)
        }
        receiver.emptyQueue() // Process the message, this should be individual
      }
    }
  }
}
