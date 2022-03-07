package Nodes

import Communication.{ReceiverSocket, SenderSocket, SocketData}
import Messaging.Message
import Misc.ResourceUtilities
import org.apache.commons.net.ntp.TimeStamp

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

  def getMessageID(): (Int, Int) = {
    (ID, counters("message"))
  }

  def getCurrentTimestamp(): TimeStamp = {
    TimeStamp.getCurrentTime
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

  def startAckTimer(a: (Int, Int)): Unit = {
    // TODO No idea yet what this should do
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
