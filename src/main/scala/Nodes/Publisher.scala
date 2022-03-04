package Nodes

import Messages.{AdvertisementContent, Message}
import Utilities.Advertisement

import java.sql.{Date, Timestamp}


class Publisher(override val address:String, override val name:String, override val port:Int, override val receiverPort:Int) extends Node( address, name,  port, receiverPort) {


  private var advertisementList=scala.collection.mutable.Map[String,Advertisement]()
  private var subscriberList=scala.collection.mutable.Map[String,Advertisement]()



  def sendAdvertisement():Unit={

    // Create the advertisement
    val adId=name+counters.get("Advertisements").toString
    val advertisement=new Advertisement(adId)

    val content=new AdvertisementContent(adId)

    //conversion happens implicitly

    // We send all the ads
    routingTable.map(routeInfo=>{
      val message:Message=new Message(getMessageId(),senderInfo,1,routeInfo._1,content,getCurrentTimestamp())
      sender.sendMessage(message,routeInfo._2.port,routeInfo._2.address)
    })

    // Once we send them successfully, we add them to the list
    advertisementList+=(adId->advertisement)

    // To be implemented
  }
  def sendUnadvertisement():Unit={
    // To be implemented
  }

  def sendPub():Unit={
    // To be implemented
  }

  def receiveSubscription(message:Message):Unit={
    // To be implemented
  }
  def receiveUnsubscription(message:Message):Unit={
    // To be implemented
  }
  def receivePubRequest(message:Message):Unit={
    // To be implemented
  }

  override def execute(): Unit = {
    super.execute()

 
    val t = new Thread(receiver)
    t.start()
    while (true) {
      Thread.sleep(1000)

      while (!receiver.isQueueEmpty) {
        val message = receiver.getFirstFromQueue()

        if(!routingTable.contains(message.senderInfo.id) ){
          addRoute(message.senderInfo)
        }

        message.messageType match{
          case 1 => receiveSubscription(message)
          case 2 => receiveUnsubscription(message)
          case 5 => receiveAckRequest(message)
          case 8 => receiveAckResponse(message)
          case 8 => receivePubRequest(message)
        }

        // Process the message, this should be individual
        receiver.emptyQueue
      }



      // Needed to be done
      val option=randomGenerator.nextInt(100)

      // This is a tentative for a random process
      option match{
        case x if (x>0 && x<=19) => sendAdvertisement()
        case 20 => sendUnadvertisement()
        case x if (x>20 && x<=29) => sendPub()
        case 30 => sendAckRequest()
        case _ =>
      }

    }


  }




}