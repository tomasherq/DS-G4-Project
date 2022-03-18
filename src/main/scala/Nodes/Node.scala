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

  // Message threshold
  protected val messageSaveThreshold=100



  def writeFileMessages(option:String,message: Message): Unit = {

    val location = "/tmp/" + ID.toString + "/" + option + "/"
    val directory = new File(String.valueOf(location))

    if (!directory.exists) {
      directory.mkdirs()
    }

    implicit val formats: DefaultFormats.type = DefaultFormats
    var filename=""
    if (option == "received") {

      counters += ("SavedMessagesReceived" -> (counters("SavedMessagesReceived") + 1))
      filename = location + option + "_" + (counters("SavedMessagesReceived") / messageSaveThreshold).toString + ".ndjson"

    } else {

      counters += ("SavedMessagesSent" -> (counters("SavedMessagesSent") + 1))
      filename = location + option + "_" + (counters("SavedMessagesSent")/messageSaveThreshold).toString + ".ndjson"

    }
    val jsonString = write(message)
    val fileWriter = new FileWriter(filename, true)
    fileWriter.write(jsonString+"\n")
    fileWriter.close()

  }

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


    writeFileMessages("sent",message)

    sender.sendMessage(message, DestinationSocketData.address, DestinationSocketData.port)
  }

  def startReceiver(): Unit = {
    val t = new Thread(receiver)
    t.start()
  }

  def startAckTimer(messageType: String, ID: (Int, Int)): Unit = {
    timestamps += ((messageType, ID) -> getCurrentTimestamp)
  }

  def initializeCounters(): Unit = {
    counters += ("Message" -> 1)
    counters += ("Advertisements" -> 1)
    counters += ("Subscriptions" -> 1)
    counters += ("Publications" -> 1)
    counters += ("SavedMessagesSent" -> 0)
    counters += ("SavedMessagesReceived" -> 0)
  }

  def execute(): Unit = {
    randomGenerator.setSeed(100)
    initializeCounters()



  }
}
