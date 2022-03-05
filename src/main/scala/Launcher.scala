import scala.sys.exit
import java.lang.Thread.sleep
import scala.io.Source

object Launcher {

  private var nodeList = Map[Int, String]()
  private val usage = """
  Usage: Launcher
    --type    client or broker
    --ID      ID of the client

    In case of client:
      --BID   ID of edge broker

    In case of broker
      --nbrs  "num num ..."
  """

  def parseNodeList() = {

    val filename = "NodeList.txt"

    nodeList = Source.fromResource(filename).getLines
      .map(line => {
        val Array(id,ip,_*) = line.split(' ')
        id.toInt -> (ip) }).toMap

  }

  def getNodeList(): Map[Int, String] = nodeList

  def configureSocket() = ???

  def initializeNode() = ???

  def startNode() = ???

  def main(args: Array[String]) = {

    // Parsing the cmd line arguments (we can use scopt either)
    if (args.length == 0) {
      println(usage)
      exit(1)
    }

    var arglist = args.toList
    var node_type = ""
    var node_ID = 0
    var broker_ID = 0
    var neighbours : List[String] = List()

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
        case "--nbrs" :: value :: tail =>
          neighbours = value.split(' ').toList
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
    if (node_type == "client") println("BID: " + broker_ID) else println("Neighbours: " + neighbours)

    // Parse and print node list
    parseNodeList()
    println("\nSuccesfully parsed Node List:")
    println(getNodeList())

    // TODO Create a client or a broker based on type
    configureSocket()
    initializeNode()
    startNode()
    println(s"\nSuccesfully initalized the Node $node_type $node_ID")

    // TODO Delete this loop when the actual nodes are initialized with tasks
    while(true) {
      sleep(1000)
      println("Waiting for some tasks, don't exit the program :)")
    }

  }
}
