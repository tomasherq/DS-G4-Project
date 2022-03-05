package Routing

import Communication.SocketData

class RoutingTable {

  private val table = scala.collection.mutable.Map[Int, RoutingEntry]()

  def addRoute(SocketData: SocketData): Unit = {
    table += (SocketData.ID -> new RoutingEntry(SocketData.address, SocketData.port))
  }

  def getRoute(ID: Int): RoutingEntry = {
    if (hasRoute(ID)) {
      table(ID)
    } else null
  }

  def hasRoute(ID: Int): Boolean = {
    table.contains(ID)
  }

  def deleteRoute(ID: Int): Unit = {
    table -= ID
  }

}
