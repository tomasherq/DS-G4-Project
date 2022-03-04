package Nodes

import Messages.{AdvertisementContent, Message}
import Utilities.Subscription


class Subscriber(override val address:String, override val name:String, override val port:Int, override val receiverPort:Int) extends Node( address, name,  port, receiverPort) {

  // Keep the subscription we have
  private var subscriptionList:scala.collection.mutable.Map[String,Subscription]=scala.collection.mutable.Map[String,Subscription]()



  def sendSubscription():Unit={
    // To be implemented
  }
  def sendUnsubscription():Unit={
    // To be implemented
  }
  def receiveAdvertisement(message:Message):Unit={

    val content:AdvertisementContent=message.content.asInstanceOf[AdvertisementContent];

    if(!subscriptionList.contains(content.advertisementID)){
      subscriptionList+=(content.advertisementID->new Subscription(message.senderInfo.id))
    }
  }
  def receiveUnadvertisement(message:Message):Unit={
    val content:AdvertisementContent=message.content.asInstanceOf[AdvertisementContent];

    if(subscriptionList.contains(content.advertisementID)){
      subscriptionList-=(content.advertisementID)
    }
  }
  def receivePublication(message:Message):Unit={
    // To be implemented
  }



  def requestPublication():Unit={
    // To be implemented
  }

  override def execute():Unit={
    super.execute()
    val t = new Thread(receiver)
    t.start()
    var sendMessage:Boolean=false
    while (true) {
      Thread.sleep(1000)

      while (!receiver.isQueueEmpty) {
        val message = receiver.getFirstFromQueue()

        if(!routingTable.contains(message.senderInfo.id) ){
          addRoute(message.senderInfo)
        }

        message.messageType match{
          case 3 => receiveAdvertisement(message)
          case 4 => receiveUnadvertisement(message)
          case 5 => receiveAckRequest(message)
          case 6 => receiveAckResponse(message)
          case 7 => receivePublication(message)
        }

        // Process the message, this should be individual
        receiver.emptyQueue
      }

      if(sendMessage) {

        // Needed to be done

        try {
          /* val message = _sender.sendMessage(receiverPort, "localhost", "Soy server " + i.toString)
           message.printValues()
           i += 1*/
        } catch {
          case _ => println("Bro this socket is of")
        }
      }

    }

  }

}
