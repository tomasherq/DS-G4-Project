import scala.sys.exit
import java.lang.Thread.sleep
import scala.io.Source

import Nodes._

object Launcher extends Thread {

  private var nodeList = Map[Int, String]()
  private var brokerNetwork = Map[Int, List[Int]]()
  private var port = 5000

  private val usage = """
  Usage: Launcher
    --type    client or broker
    --ID      ID of the client

    In case of client:
      --BID   ID of edge broker
  """

  def parseNodeList() = {

    val filename = "NodeList.txt"

    nodeList = Source.fromResource(filename).getLines
      .map(line => {
        val Array(id,ip,_*) = line.split(' ')
        id.toInt -> (ip) }).toMap
  }

  def parseBrokerNetwork() = {

    val filename = "BrokerNetwork.txt"

    brokerNetwork = Source.fromResource(filename).getLines
      .map(line => {
        val entry = line.split(' ')
        (entry.head.toInt, entry.tail.map(_.toInt).toList)}).toMap
  }

  def getNodeList(): Map[Int, String] = nodeList

  def getBrokerNetwork(): Map[Int, List[Int]] = brokerNetwork

  def initializeNode(node_type: String, node_ID: Int, broker_ID: Int) = {
    if (node_type == "client") {
      new Client(nodeList(node_ID), node_ID, port, port+1)
    } else {
      new Broker(nodeList(node_ID), node_ID, port, port+1)
    }
  }

  def startNode(node: Node) = {
    node.execute()
  }

  def main(args: Array[String]): Unit = {

    // Parsing the cmd line arguments (we can use scopt either)
    if (args.length == 0) {
      println(usage)
      exit(1)
    }

    var arglist = args.toList
    var node_type = ""
    var node_ID = 0
    var broker_ID = 0

    while(arglist.nonEmpty) {
      arglist match {
        case "--type" :: value :: tail =>
          if (value == "broker" || value== "client") {
            node_type = value
            arglist = tail
          } else {
            println("Undefined Node Type")
            exit(1)
          }
        case "--ID" :: value :: tail =>
          node_ID = value.toInt
          arglist = tail
        case "--BID" :: value :: tail =>
          broker_ID = value.toInt
          arglist = tail
        case option =>
          println("Unknown option " + option)
          exit(1)
      }
    }

    // Test argument parse
    println("Succesfully initalized Launcher with the following start-up parameters:")
    println("Type: " + node_type)
    println("ID: " + node_ID)
    if (node_type == "client") println("BID: " + broker_ID)

    // Parse and print node list
    parseNodeList()
    println("\nSuccesfully parsed Node List:")
    println(getNodeList())

    // Parse Broker Network if instance is of type Broker
    if (node_type == "broker") {
      parseBrokerNetwork()
      println("\nSuccesfully parsed Broker Network:")
      println(getBrokerNetwork())
    }

    val node = initializeNode(node_type, node_ID, broker_ID)
    startNode(node)
    println(s"\nSuccesfully initalized the Node $node_type $node_ID")

    // TODO Delete this loop when the actual nodes are initialized with tasks
    while(true) {
      sleep(1000)
      println("Waiting for some tasks, don't exit the program :)")
    }
  }
}
