package Routing

import Messaging.Subscription

import scala.collection.mutable.ListBuffer

class RoutingTable {

  private val table = scala.collection.mutable.Map[(Int, Int), (Int, String, List[Int => Boolean])]()

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
    // TODO: Also match attributes against the advertisements in the table for content-based pub/sub (Currently topic-based).
    val matches: ListBuffer[(Int, Int)] = ListBuffer[(Int, Int)]()
    for (key <- table.keys) {
      val pClass = getRoute(key)._2
      if (pClass.equals(subscription.pClass)){
        matches += key
      }
    }
    matches.toList
  }
}
