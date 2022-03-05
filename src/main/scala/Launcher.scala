import scala.sys.exit
import java.lang.Thread.sleep
import scala.io.Source

object Launcher {

  private var nodeList = Map[Int, String]()
  private val usage = """
  Usage: Launcher
    --type    client / broker
    --ID      num

    In case of client:
      --EB    IP of edge broker

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
    var IP_EB = ""
    var neighbours : List[String] = List()

    while(arglist.nonEmpty) {
      arglist match {
        case "--type" :: value :: tail =>
          node_type = value
          arglist = tail
        case "--ID" :: value :: tail =>
          node_ID = value.toInt
          arglist = tail
        case "--EB" :: value :: tail =>
          IP_EB = value
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
    println("Type: " + node_type)
    println("ID: " + node_ID)
    println("IP: " + IP_EB)
    println("Neighbours: " + neighbours)

    // Parse and print node list
    parseNodeList()
    println(getNodeList())

    // TODO Create a client or a broker based on type
    configureSocket()
    initializeNode()
    startNode()

    // TODO Delete this loop when the actual nodes are initialized with tasks
    while(true) {
      sleep(1000)
      println("Waiting for some tasks, don't exit the program :)")
    }

  }
}
