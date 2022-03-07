package Routing

import Messaging.Subscription

class RoutingTable {

  private var table = scala.collection.mutable.Map[(Int, Int), (Int, String, List[Int => Boolean])]()

  def addRoute(ID: (Int, Int), Destination: Int, pClass: String, pAttribute: List[Int => Boolean]): Unit = {
    table += (ID -> (Destination, pClass, pAttribute))
  }

  def getRoute(ID: (Int, Int)): (Int, String, List[Int => Boolean]) = {
    table(ID)
  }

  def hasRoute(ID: (Int, Int)): Boolean = {
    table.contains(ID)
  }

  def deleteRoute(ID: (Int, Int)): Unit = {
    table -= ID
  }

  def findMatch(subscription: Subscription): List[(Int, Int)] = {
    // TODO match subscription class and attributes against the advertisements in the table
    null
  }

}
