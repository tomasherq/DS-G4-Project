package Nodes

import Messaging._
import Routing.RoutingEntry

class Broker(override val address: String, override val ID: Int, override val port: Int, override val receiverPort: Int) extends Node( address, ID,  port, receiverPort) {


  // TODO may need to be changed, see needed datastructures
  private var subscriptionList = scala.collection.mutable.Map[Int, Subscription]()
  private var advertisementList = scala.collection.mutable.Map[Int, Advertisement]()
  private var subscriberList = scala.collection.mutable.Map[Int, Advertisement]()

  // TODO needed datastructures
  // SRT : RoutingTable
  // PRT : RoutingTable
  // NB: overlay neighbours (from file BrokenNetwork)
  // Acks : Hashmap, per msg and per link. Store all received acks.
  // Groups: List of all known groups by this broker
  // IsActive: HashMap<Integer, Bool>, AdvertiseID: Int, isActive: Bool
  // StoredPubs: HashMap<Integer, List<Integer>>: Stores per publisher a list of publications.

  // TODO change to instance of RoutingTable with RoutingEntry elements
  var routingTable = scala.collection.mutable.Map[Int, RoutingEntry]()

  /**
   * This method will lookup a candidate edge broker that can reach the client if this broker is not its edge broker
   */
  def forwardMessage(): Unit = {
    // TODO To implement
  }

  /**
   * TODO Routing table method wrappers
   */
//  def addRoute(SocketData: SocketData): Unit = {
//    routingTable += (SocketData.id -> new RoutingEntry(SocketData.address,SocketData.port))
//  }
//  def deleteRoute(name:String): Unit = {
//    routingTable -= (name)
//  }

  /**
   * Advertisement methods
   */
  def receiveAdvertisement(message: Message): Unit = {

    val content:Advertise=message.content.asInstanceOf[Advertise]

    if(!subscriptionList.contains(content.advertisementID)) {
      subscriptionList += (content.advertisementID -> new Subscription(message.SocketData.ID))
    }
  }
  def receiveUnadvertisement(message: Message): Unit = {
    val content:Advertise = message.content.asInstanceOf[Advertise]

    if(subscriptionList.contains(content.advertisementID)) {
      subscriptionList -= (content.advertisementID)
    }
  }

  /**
   *  Subscription methods
   */
  def receiveSubscription(message: Message): Unit = {
    // TODO To be implemented
  }

  def receiveUnsubscription(message: Message): Unit = {
    // TODO To be implemented
  }

  /**
   * Publication methods
   */
  def receivePubRequest(message: Message): Unit = {
    // TODO To be implemented
  }

  /**
   * Ack methods
   */
  def sendAckResponse(): Unit = {
    // TODO To be implemented
  }

  def receiveAckRequest(message: Message): Unit = {
    // TODO To be implemented
  }

  override def execute(): Unit = {
    super.execute()
    val t = new Thread(receiver)
    t.start()

    while (true) {
      Thread.sleep(1000)
      println("Waiting for messages...")

      while (!receiver.isQueueEmpty) {
        println("Parsing a new message...")
        val message = receiver.getFirstFromQueue()

        if(!routingTable.contains(message.SocketData.ID) ) {
          //addRoute(message.SocketData)
        }

        // TODO define all types
        message.content match {
          case _ : Advertise => receiveAdvertisement(message)
        }

        receiver.emptyQueue // Process the message, this should be individual
      }
    }
  }
}
