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
  """

  def initializeNode(node_type: String, node_ID: Int, broker_ID: Int, node_mode: ClientType): Node = {
    if (node_type == "client") {
      new Client(node_ID, broker_ID, node_mode)
    } else {
      new Broker(node_ID)
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
        case option =>
          println("Unknown option " + option)
          exit(1)
      }
    }

    println("Successfully initialized Launcher with the following start-up parameters:")
    println("Type: " + node_type)
    println("ID: " + node_ID)
    if (node_type == "client") println("BID: " + broker_ID + "\nMode: " + node_mode)

    // Parse and print node list
    val nodeList = ResourceUtilities.getNodeList()
    println("\nSuccessfully parsed Node List:")
    println(nodeList)

    // Parse Broker Network if instance is of type Broker
    if (node_type == "broker") {
      val brokerNetwork = ResourceUtilities.getBrokerNetwork()
      println("\nSuccessfully parsed Broker Network:")
      println(brokerNetwork)
    }

    val node = initializeNode(node_type, node_ID, broker_ID, node_mode)
    startNode(node)
    println(s"\nSuccessfully initialized the Node $node_type $node_ID")
  }
}
