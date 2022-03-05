package Nodes

import Communication.{ReceiverSocket, SenderSocket}
import Messages.{Message, SenderInfo}
import Routing.RoutingEntry

class Node(val address:String,val name:String, val port:Int,val receiverPort:Int) {

   val senderInfo:SenderInfo = new SenderInfo(name,address,port)
   val receiver:ReceiverSocket = new ReceiverSocket(senderInfo)
   val sender:SenderSocket = new SenderSocket(senderInfo)
   var routingTable = scala.collection.mutable.Map[String,RoutingEntry]()
   val randomGenerator = scala.util.Random

  /**
   * Used in all classes to kep track of publications of an advertisement or messages sent
   */
   var counters = scala.collection.mutable.Map[String,Int]()


  /**
   * This list has to be accessed to see the historic, only remove if ACK sent
   * We have a list of the ones we sent and received
   */
  private var messagesSent = scala.collection.mutable.Map[String,Message]()
  private var messagesReceived = scala.collection.mutable.Map[String,Message]()

  def getMessageId(): String = {
    if(!counters.contains("message")) {
      counters += ("message"->0)
    }
    name+"-"+counters.get("message").toString
  }

  def getCurrentTimestamp(): Int = {
    // Probably not best way to do this tbh
    implicit def date2timestamp(date: java.util.Date) = new java.sql.Timestamp(date.getTime)
    val date = new java.util.Date
    date.getTime.toInt
  }

  def addRoute(senderInfo: SenderInfo): Unit = {

    routingTable += (senderInfo.id -> new RoutingEntry(senderInfo.address,senderInfo.port))

  }
  def deleteRoute(name:String): Unit = {
    routingTable -= (name)
  }

  // There is no need for the structure to be like this

  def sendAckResponse(): Unit = {
    // TODO To be implemented
  }

  def receiveAckResponse(message: Message): Unit = {
    // TODO To be implemented
  }

  def sendAckRequest(): Unit = {
    // TODO To be implemented
  }

  def receiveAckRequest(message: Message): Unit = {
    // TODO To be implemented
  }

  // Maybe this method needs to be over rid by every class
  // Needs to be defined, I left the code I used to have to know how threads worked
  def execute(): Unit = {
    // We have to develop a method to make this seed always the same so we can perform experiments
    randomGenerator.setSeed(100)
    counters += ("Advertisements"->0)
  }
}


