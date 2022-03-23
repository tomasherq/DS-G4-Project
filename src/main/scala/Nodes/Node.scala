package Nodes

import Communication.{ReceiverSocket, SenderSocket, SocketData}
import Messaging.{Advertisement, Message, Subscription}
import Misc.ResourceUtilities

import org.apache.commons.net.ntp.TimeStamp
import scala.collection.mutable
import scala.util.Random
import scala.language.implicitConversions
import net.liftweb.json._
import net.liftweb.json.Serialization.write
import java.io.{File, FileWriter}

abstract class Node(val ID: Int) {

  protected val SocketData: SocketData = ResourceUtilities.getNodeSocketData(ID)
  protected val receiver: ReceiverSocket = new ReceiverSocket(SocketData)
  protected val sender: SenderSocket = new SenderSocket()
  protected val randomGenerator: Random = Random

  protected val counters: mutable.Map[String, Int] = mutable.Map[String, Int]()
  protected val timestamps: mutable.Map[(String, (Int, Int)), Long] = mutable.Map[(String, (Int, Int)), Long]()
  protected val ACKS: mutable.Map[(String, (Int, Int), Int), Boolean] = mutable.Map[(String, (Int, Int), Int), Boolean]()

  protected val subscriptionList: mutable.Map[(Int, Int), Subscription] = mutable.Map[(Int, Int), Subscription]()
  protected val advertisementList: mutable.Map[(Int, Int), Advertisement] = mutable.Map[(Int, Int), Advertisement]()

  protected val messageSaveThreshold = 10

  protected var sentMessages: Set[Message] = Set[Message]()
  protected var receivedMessages: Set[Message] = Set[Message]()

  def getNodeIP: String = {
    SocketData.address
  }

  def getNodePort: Int = {
    SocketData.port
  }

  def getMessageID(): (Int, Int) = {
    counters += ("Message" -> (counters("Message") + 1))
    (ID, counters("Message"))
  }

  def getCurrentTimestamp: Long = {
    TimeStamp.getCurrentTime.getTime
  }

  /**
   * sendMessage wrapper for client -> broker
   */
  def sendMessage(message: Message, DestinationID: Int): Unit = {
    val DestinationSocketData = ResourceUtilities.getNodeSocketData(DestinationID)
    sentMessages += message
    if (sentMessages.toList.length > messageSaveThreshold) {
      writeFileMessages("sent")
    }
    sender.sendMessage(message, DestinationSocketData.address, DestinationSocketData.port)
  }

  def writeFileMessages(option: String): Unit = {

    val location = "/tmp/" + ID.toString + "/" + option + "/"
    val directory = new File(String.valueOf(location))

    if (!directory.exists) {
      directory.mkdirs()
    }

    val numberOfFile = directory.listFiles.length.toString
    val filename = location + option + "_" + numberOfFile + ".ndjson"
    val fileWriter = new FileWriter(filename)

    implicit val formats: DefaultFormats.type = DefaultFormats

    if (option == "received") {
      receivedMessages.foreach(message => {
        val jsonString = write(message)
        fileWriter.write(jsonString)
        fileWriter.write("\n")
      })
      receivedMessages = Set[Message]()
    } else {
      sentMessages.foreach(message => {
        val jsonString = write(message)
        fileWriter.write(jsonString)
        fileWriter.write("\n")
      })
      sentMessages = Set[Message]()
    }
    fileWriter.close()
  }

  def startReceiver(): Unit = {
    val t = new Thread(receiver)
    t.start()
  }

  def initializeCounters(): Unit = {
    counters += ("Message" -> 1)
    counters += ("Advertisements" -> 1)
    counters += ("Subscriptions" -> 1)
    counters += ("Publications" -> 1)
  }

  def execute(): Unit = {
    randomGenerator.setSeed(ID)
    initializeCounters()
  }
}
