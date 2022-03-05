package Nodes

import Messages.{AdvertisementContent, Message}
import Utilities.{Advertisement, Subscription}

class Client(override val address:String, override val name:String, override val port:Int, override val receiverPort:Int) extends Node( address, name,  port, receiverPort) {

  /**
   * Keep track of the advertisements and subscriptions the client created
   */
  private var subscriptionList:scala.collection.mutable.Map[String,Subscription] = scala.collection.mutable.Map[String,Subscription]()
  private var advertisementList = scala.collection.mutable.Map[String,Advertisement]()

  /**
   * Advertisement methods
   */
  def sendAdvertisement(): Unit = {

    val adId = name + counters.get("Advertisements").toString
    val advertisement = new Advertisement(adId)
    val content = new AdvertisementContent(adId)

    // TODO client should only send to known broker, the broker will use routing table

    //routingTable.map(routeInfo=> {  // We send all the ads
    //val message:Message = new Message(getMessageId(),senderInfo,1,routeInfo._1,content,getCurrentTimestamp())
    //sender.sendMessage(message,routeInfo._2.port,routeInfo._2.address)
    //})

    advertisementList += (adId->advertisement)

    // TODO To be implemented

  }

  def sendUnadvertisement(): Unit = {
    // TODO To be implemented
  }

  /**
   * Subscription methods
   */
  def sendSubscription(): Unit = {
    // TODO To be implemented
  }
  def sendUnsubscription(): Unit = {
    // TODO To be implemented
  }

  /**
   * Publication methods
   */
  def sendPublication(): Unit = {
    // TODO To be implemented
  }

  def receivePublication(message: Message): Unit = {
    // TODO To be implemented
  }

  def requestPublication(): Unit = {
    // TODO To be implemented
  }

  /**
   * Ack methods
   */
  def receiveAckResponse(message: Message): Unit = {
    // TODO To be implemented
  }

  def sendAckRequest(): Unit = {
    // TODO To be implemented
  }

  /**
   * Open ReceiverSocket and actively listen for messages.
   * Simulate random Client Pub/Sub behaviour.
   */
  override def execute(): Unit = {
    super.execute()
    val t = new Thread(receiver)
    t.start()

    while (true) {
      Thread.sleep(1000)

      while (!receiver.isQueueEmpty) {
        val message = receiver.getFirstFromQueue()

        // TODO routing table is only known to broker, keep a separate lists of known brokers.
        //if(!routingTable.contains(message.senderInfo.id) ) {
        //addRoute(message.senderInfo)
        //}

        message.messageType match {
          case 6 => receiveAckResponse(message)
          case 7 => receivePublication(message)
          case 8 => receiveAckResponse(message)
        }

        receiver.emptyQueue // Process the message, this should be individual
      }

      val option = randomGenerator.nextInt(100)

      /**
       * Simulate random client behaviour
       */
      option match{
        case x if (x>0 && x<=19) => sendAdvertisement()
        case 20 => sendUnadvertisement()
        case x if (x>20 && x<=29) => sendPublication()
        case 30 => sendAckRequest()
        case _ =>
      }
    }
  }
}
