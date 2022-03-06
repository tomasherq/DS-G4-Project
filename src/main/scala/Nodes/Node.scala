package Nodes

import Communication.{ReceiverSocket, SenderSocket, SocketData}
import Messaging.Message
import Misc.ResourceUtilities

import java.sql.Timestamp
import scala.language.implicitConversions

abstract class Node(val ID: Int) {

  protected val SocketData: SocketData = ResourceUtilities.getNodeSocketData(ID)
  protected val receiver: ReceiverSocket = new ReceiverSocket(SocketData)
  protected val sender: SenderSocket = new SenderSocket()
  protected val randomGenerator: scala.util.Random = scala.util.Random
  /**
   * Used in all classes to kep track of publications of an advertisement or messages sent
   */
   var counters: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String, Int]()
  /**
   * This list has to be accessed to see the historic, only remove if ACK sent
   * We have a list of the ones we sent and received
   */
  private var messagesSent = scala.collection.mutable.Map[Int, Message]()
  private var messagesReceived = scala.collection.mutable.Map[Int, Message]()

  def getNodeIP(): String = {
    SocketData.address
  }

  def getNodePort(): Int = {
    SocketData.port
  }

  def getMessageID(): Int = {
    if(!counters.contains("message")) {
      counters += ("message"->0)
    }
    counters("message")
  }

  // TODO Check if we can do this is a better way
  def getCurrentTimestamp(): Int = {
    implicit def date2timestamp(date: java.util.Date): Timestamp = new java.sql.Timestamp(date.getTime)
    val date = new java.util.Date
    date.getTime.toInt

  }

  /**
   * sendMessage wrapper for client -> broker
   */
  def sendMessage(message: Message, DestinationID: Int): Unit = {
    val DestinationSocketData = ResourceUtilities.getNodeSocketData(DestinationID)
    sender.sendMessage(message, DestinationSocketData.address, DestinationSocketData.port)
  }

  def startReceiver(): Unit = {
    val t = new Thread(receiver)
    t.start()
  }

  // TODO Needs to be overridden, need to factor out the seed as static Node field
  def execute(): Unit = {
    // We have to develop a method to make this seed always the same so we can perform experiments
    randomGenerator.setSeed(100)
    counters += ("Advertisements"->0)
  }
}
