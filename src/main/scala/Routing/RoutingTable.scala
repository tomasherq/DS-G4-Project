package Routing

class RoutingTable {

  private val table = scala.collection.mutable.Map[(Int, Int), Int]()

  def addRoute(ID: (Int, Int), Destination: Int): Unit = {
    table += (ID -> Destination)
  }

  def getRoute(ID: (Int, Int)): Int = {
    table(ID)
  }

  def hasRoute(ID: (Int, Int)): Boolean = {
    table.contains(ID)
  }

  def deleteRoute(ID: (Int, Int)): Unit = {
    table -= ID
  }

}
