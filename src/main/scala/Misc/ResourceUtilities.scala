package Misc

import Communication.SocketData

import scala.io.Source

object ResourceUtilities {

  private val nodeList: Map[Int, (String, Int)] = parseNodeList()

  def getNodeList(): Map[Int, (String, Int)] = nodeList

  def getNodeSocketData(ID: Int): SocketData = {
    new SocketData(ID, nodeList(ID)._1, nodeList(ID)._2)
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
}
