import Nodes.ClientType.ClientType

import scala.sys.exit
import scala.io.Source
import Nodes._


object Launcher extends Thread {

  private var nodeList = Map[Int, String]()
  private var brokerNetwork = Map[Int, List[Int]]()
  private val port = 5000

  private val usage = """
  Usage: Launcher
    --type    client or broker
    --ID      ID of the client

    In case of client:
      --BID   ID of edge broker
      --mode  publisher or subscriber
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

  def initializeNode(node_type: String, node_ID: Int, broker_ID: Int, node_mode: ClientType) = {
    if (node_type == "client") {
      new Client(nodeList(node_ID), node_ID, port, port+1, broker_ID, node_mode)
    } else {
      new Broker(nodeList(node_ID), node_ID, port, port+1, getBrokerNetwork()(node_ID))
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
    var node_mode: ClientType = null
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
        case "--mode" :: value :: tail =>
          if (value == "publisher" || value== "subscriber") {
            node_mode = ClientType.values.find(_.toString.toLowerCase() == value.toLowerCase()).get
            arglist = tail
          } else {
            println("Undefined Node Type")
            exit(1)
          }
        case option =>
          println("Unknown option " + option)
          exit(1)
      }
    }

    // Test argument parse
    println("Succesfully initalized Launcher with the following start-up parameters:")
    println("Type: " + node_type)
    println("ID: " + node_ID)
    if (node_type == "client") println("BID: " + broker_ID + "\nMode: " + node_mode)

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

    val node = initializeNode(node_type, node_ID, broker_ID, node_mode)
    startNode(node)
    println(s"\nSuccesfully initalized the Node $node_type $node_ID")
  }
}
