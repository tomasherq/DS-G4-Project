package Misc

import Communication.SocketData

import scala.io.Source

object ResourceUtilities {

  private val nodeList: Map[Int, (String, Int)] = parseNodeList()
  private val brokerTopology: Map[Int, List[Int]] = parseBrokerTopology()

  def getNodeList(): Map[Int, (String, Int)] = nodeList

  def getBrokerTopology(): Map[Int, List[Int]] = brokerTopology

  def getNodeSocketData(ID: Int): SocketData = {
    new SocketData(ID, nodeList(ID)._1, nodeList(ID)._2)
  }

  def getNeighbours(ID: Int): List[Int] = {
    brokerTopology(ID)
  }

  private def parseNodeList(): Map[Int, (String, Int)] = {
    val filename = "NodeList.txt"
    val nodeList = Source.fromResource(filename).getLines
      .map(line => {
        val Array(id, ip, port, _*) = line.split(' ')
        id.toInt -> (ip, port.toInt)
      }).toMap
    nodeList
  }

  private def parseBrokerTopology(): Map[Int, List[Int]] = {
    val filename = "BrokerTopology.txt"
    val brokerTopology = Source.fromResource(filename).getLines
      .map(line => {
        val entry = line.split(' ')
        (entry.head.toInt, entry.tail.map(_.toInt).toList)
      }).toMap
    brokerTopology
  }
}
