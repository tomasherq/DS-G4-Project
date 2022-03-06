package Misc

import Communication.SocketData

import scala.io.Source

object ResourceUtilities {

  private val nodeList: Map[Int, (String, Int)] = parseNodeList()
  private val brokerNetwork: Map[Int, List[Int]] = parseBrokerNetwork()

  def getNodeSocketData(ID: Int): SocketData = {
    new SocketData(ID, nodeList(ID)._1, nodeList(ID)._2)
  }


  def getNeighbours(ID: Int): List[Int] = {
    brokerNetwork(ID)
  }

  def getNodeList(): Map[Int, (String, Int)] = nodeList

  def getBrokerNetwork(): Map[Int, List[Int]] = brokerNetwork

  private def parseNodeList(): Map[Int, (String, Int)] = {
    val filename = "NodeList.txt"
    val nodeList = Source.fromResource(filename).getLines
      .map(line => {
        val Array(id, ip, port, _*) = line.split(' ')
        id.toInt -> (ip, port.toInt)
      }).toMap
    nodeList
  }

  private def parseBrokerNetwork(): Map[Int, List[Int]] = {
    val filename = "BrokerNetwork.txt"
    val brokerNetwork = Source.fromResource(filename).getLines
      .map(line => {
        val entry = line.split(' ')
        (entry.head.toInt, entry.tail.map(_.toInt).toList)
      }).toMap
    brokerNetwork
  }
}
