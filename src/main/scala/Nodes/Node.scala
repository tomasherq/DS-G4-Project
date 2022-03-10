package Nodes

import Communication.{ReceiverSocket, SenderSocket, SocketData}
import Messaging.{Advertisement, Message, Subscription}
import Misc.ResourceUtilities
import org.apache.commons.net.ntp.TimeStamp

import scala.collection.mutable
import scala.language.implicitConversions
import scala.io.Source

abstract class Node(val ID: Int, val savePath:String) {

  protected val SocketData: SocketData = ResourceUtilities.getNodeSocketData(ID)
  protected val receiver: ReceiverSocket = new ReceiverSocket(SocketData)
  protected val sender: SenderSocket = new SenderSocket()
  protected val randomGenerator: scala.util.Random = scala.util.Random

  protected val counters: mutable.Map[String, Int] = scala.collection.mutable.Map[String, Int]()
  protected val timestamps: mutable.Map[(String, (Int, Int)), TimeStamp] = scala.collection.mutable.Map[(String, (Int, Int)), TimeStamp]()
  protected val ACKS: mutable.Map[(String, (Int, Int), Int), Boolean] = scala.collection.mutable.Map[(String, (Int, Int), Int), Boolean]()


  protected val subscriptionList = scala.collection.mutable.Map[(Int, Int), Subscription]()
  protected val advertisementList = scala.collection.mutable.Map[(Int, Int), Advertisement]()




  def getNodeIP(): String = {
    SocketData.address
  }

  def getNodePort(): Int = {
    SocketData.port
  }

  def getMessageID(): (Int, Int) = {
    (ID, counters("Message"))
  }

  def getCurrentTimestamp(): TimeStamp = {
    TimeStamp.getCurrentTime
  }

  /**
   * sendMessage wrapper for client -> broker
   */
  def sendMessage(message: Message, DestinationID: Int): Unit = {
    val DestinationSocketData = ResourceUtilities.getNodeSocketData(DestinationID)
    counters += ("Message" -> (counters("Message")+1))
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
    counters += ("Message" -> 1)
    counters += ("Advertisements" -> 1)
    counters += ("Subscriptions" -> 1)
  }

  def execute(): Unit = {
    randomGenerator.setSeed(100)
    initializeCounters()
  }
}
