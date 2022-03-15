package Nodes

import Communication.{ReceiverSocket, SenderSocket, SocketData}
import Messaging.{Advertisement, Message, Subscription}
import Misc.ResourceUtilities
import org.apache.commons.net.ntp.TimeStamp

import scala.collection.mutable
import scala.language.implicitConversions
import scala.collection.mutable._
import net.liftweb.json._
import net.liftweb.json.Serialization.write

import java.io.{File, FileWriter}


abstract class Node(val ID: Int) {

  protected val SocketData: SocketData = ResourceUtilities.getNodeSocketData(ID)
  protected val receiver: ReceiverSocket = new ReceiverSocket(SocketData)
  protected val sender: SenderSocket = new SenderSocket()
  protected val randomGenerator: scala.util.Random = scala.util.Random

  protected val counters: mutable.Map[String, Int] = scala.collection.mutable.Map[String, Int]()
  protected val timestamps: mutable.Map[(String, (Int, Int)), Long] = scala.collection.mutable.Map[(String, (Int, Int)), Long]()
  protected val ACKS: mutable.Map[(String, (Int, Int), Int), Boolean] = scala.collection.mutable.Map[(String, (Int, Int), Int), Boolean]()


  protected val subscriptionList = scala.collection.mutable.Map[(Int, Int), Subscription]()
  protected val advertisementList = scala.collection.mutable.Map[(Int, Int), Advertisement]()

  // Message threshold
  protected val messageSaveThreshold=2


  // Save the sent messages with the ID
  protected var sentMessages:Set[Message]=Set[Message]()
  protected var receivedMessages:Set[Message]=Set[Message]()

  def writeFileMessages(option:String):Unit={

    val location="/tmp/"+ID.toString+"/"+option+"/"
    val directory = new File(String.valueOf(location))

    if (!directory.exists) {
      directory.mkdirs()
    }


    val numberOfFile=directory.listFiles.length.toString
    val filename=location+option+"_"+numberOfFile+".ndjson"
    val fileWriter = new FileWriter(filename)

    implicit val formats = DefaultFormats

    if(option=="received") {
      receivedMessages.map(message => {

        val jsonString = write(message)
        fileWriter.write(jsonString)
        fileWriter.write("\n")
      })
      receivedMessages=Set[Message]()
    }else{
      sentMessages.map(message => {
        val jsonString = write(message)
        fileWriter.write(jsonString)
        fileWriter.write("\n")
      })
      sentMessages=Set[Message]()
    }

    // create a JSON string from the Person, then print it
    fileWriter.close()
  }

  def getNodeIP(): String = {
    SocketData.address
  }

  def getNodePort(): Int = {
    SocketData.port
  }

  def getMessageID(): (Int, Int) = {
    (ID, counters("Message"))
  }

  def getCurrentTimestamp(): Long = {
    TimeStamp.getCurrentTime.getTime
  }

  /**
   * sendMessage wrapper for client -> broker
   */
  def sendMessage(message: Message, DestinationID: Int): Unit = {
    val DestinationSocketData = ResourceUtilities.getNodeSocketData(DestinationID)
    counters += ("Message" -> (counters("Message")+1))
    sentMessages+=message
    if(sentMessages.toList.length>messageSaveThreshold){
      writeFileMessages("sent")
    }

    sender.sendMessage(message, DestinationSocketData.address, DestinationSocketData.port)
  }

  def startReceiver(): Unit = {
    val t = new Thread(receiver)
    t.start()
  }

  def startAckTimer(messageType: String, ID: (Int, Int)): Unit = {
    timestamps += ((messageType, ID) -> getCurrentTimestamp())
  }

  def initializeCounters(): Unit = {
    counters += ("Message" -> 1)
    counters += ("Advertisements" -> 1)
    counters += ("Subscriptions" -> 1)
  }

  def execute(): Unit = {
    randomGenerator.setSeed(100)
    initializeCounters()

/*    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      println("yes")
      println(d.listFiles.filter(_.isFile).toList)
    } else {
      List[File]()
    }*/
  }
}
