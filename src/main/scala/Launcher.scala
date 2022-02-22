import scala.sys.exit

object Launcher {

  val usage = """
  Usage: Launcher
    --type    client / broker
    --ID      num

    In case of client:
      --EB    IP of edge broker

    In case of broker
      --nbrs  "num num ..."
  """

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

    // Test message
    val message : Message = new Message()
    message.printMessage

    // TODO Create a client or a broker based on type
    // Configure socket
    // Start the two threads

  }
}
