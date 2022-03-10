import Nodes._
import Misc.ResourceUtilities
import Nodes.ClientType.ClientType

import scala.sys.exit

object Launcher extends Thread {

  private val usage =
    """
  Usage: Launcher
    --type    client or broker
    --ID      ID of the client

    In case of client:
      --BID   ID of edge broker
      --mode  publisher or subscriber

    In case of broker:
      --endpoints list of connected clients e.g. "1 4"
  """

  def initializeNode(node_type: String, node_ID: Int, broker_ID: Int, endpoints: List[Int], node_mode: ClientType,savePath:String): Node = {
    if (node_type == "client") {
      new Client(node_ID, broker_ID, node_mode,savePath)
    } else {
      new Broker(node_ID, endpoints,savePath)
    }
  }

  def startNode(node: Node): Unit = {
    println("\nNode is listening on " + node.getNodeIP() + ":" + node.getNodePort())
    node.execute()
  }

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println(usage)
      exit(1)
    }

    var arglist = args.toList
    var node_type = ""
    var node_mode: ClientType = null
    var node_ID = 0
    var broker_ID = 0
    var endpoints: List[Int] = null
    var savePath:String=""

    while (arglist.nonEmpty) {
      arglist match {
        case "--type" :: value :: tail =>
          if (value == "broker" || value == "client") {
            node_type = value
            arglist = tail
          } else {
            println("Undefined Node Type")
            exit(1)
          }
        case "--savepath" :: value ::tail=>
          savePath=value
          arglist=tail
        case "--ID" :: value :: tail =>
          node_ID = value.toInt
          arglist = tail
        case "--BID" :: value :: tail =>
          broker_ID = value.toInt
          arglist = tail
        case "--mode" :: value :: tail =>
          if (value == "publisher" || value == "subscriber") {
            node_mode = ClientType.values.find(_.toString.toLowerCase() == value.toLowerCase()).get
            arglist = tail
          } else {
            println("Undefined Node Type")
            exit(1)
          }
        case "--endpoints" :: value :: tail =>
          endpoints = value.split(' ').map(_.toInt).toList
          arglist = tail
        case option =>
          println("Unknown option " + option)
          exit(1)
      }
    }

    println("Successfully initialized Launcher with the following start-up parameters:")
    println("Type: " + node_type)
    println("ID: " + node_ID)
    println("Save path:"+ savePath)
    if (node_type == "client") println("BID: " + broker_ID + "\nMode: " + node_mode)
    if (node_type == "broker") println("Endpoints: " + endpoints)

    // Parse and print node list
    val nodeList = ResourceUtilities.getNodeList()
    println("\nSuccessfully parsed Node List:")
    println(nodeList)

    // Parse Broker Network if instance is of type Broker
    if (node_type == "broker") {
      val brokerTopology = ResourceUtilities.getBrokerTopology()
      println("\nSuccessfully parsed Broker Topology:")
      println(brokerTopology)
    }

    val node = initializeNode(node_type, node_ID, broker_ID, endpoints, node_mode,savePath)
    startNode(node)
    println(s"\nSuccessfully initialized the Node $node_type $node_ID")
  }
}
