package Nodes

import Communication.{ReceiverSocket, SenderSocket, SocketData}
import Messaging.{AckResponse, Message, MessageType}
import Misc.ResourceUtilities
import org.apache.commons.net.ntp.TimeStamp

import scala.language.implicitConversions

abstract class Node(val ID: Int) {

  protected val SocketData: SocketData = ResourceUtilities.getNodeSocketData(ID)
  protected val receiver: ReceiverSocket = new ReceiverSocket(SocketData)
  protected val sender: SenderSocket = new SenderSocket()
  protected val randomGenerator: scala.util.Random = scala.util.Random

  protected val counters = scala.collection.mutable.Map[String, Int]()
  protected val timestamps = scala.collection.mutable.Map[(String, (Int, Int)), TimeStamp]()
  protected val ACKS = scala.collection.mutable.Map[(String, (Int, Int), Int), Boolean]()
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

  def getMessageID(): (Int, Int) = {
    (ID, counters("message"))
  }

  def getCurrentTimestamp(): TimeStamp = {
    TimeStamp.getCurrentTime
  }

  /**
   * Ack methods
   */
  def sendACK(ID: Int, message: Message): Unit = {
    println("Sending Ack Response")

    val ACK = AckResponse(message.content)
    sendMessage(new Message(getMessageID(), SocketData, ID, ACK, getCurrentTimestamp()), ID)
  }

  def receiveACK(message: Message): Unit = {
    println("Receiving Ack Response")
    // TODO To be implemented
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

  def startAckTimer(messageType: String, ID: (Int, Int)): Unit = {
    timestamps += ((messageType, ID) -> TimeStamp.getCurrentTime)
  }

  def initializeCounters(): Unit = {
    counters += ("message" -> 1)
    counters += ("Advertisements" -> 1)
  }

  def execute(): Unit = {
    randomGenerator.setSeed(100)
    initializeCounters()
  }
}
