package Routing

import Messaging.Subscription

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class RoutingTable {

  private val table = mutable.Map[(Int, Int), (Int, String, (String, Int))]()

  def addRoute(ID: (Int, Int), Destination: Int, pClass: String, pAttribute: (String, Int)): Unit = {
    table += (ID -> (Destination, pClass, pAttribute))
  }

  def getRoute(ID: (Int, Int)): (Int, String, (String, Int)) = {
    table(ID)
  }

  def hasRoute(ID: (Int, Int)): Boolean = {
    table.contains(ID)
  }

  def deleteRoute(ID: (Int, Int)): Unit = {
    table -= ID
  }

  def findMatch(subscription: Subscription): List[(Int, Int)] = {
    val matches: ListBuffer[(Int, Int)] = ListBuffer[(Int, Int)]()

    for (key <- table.keys) {
      val routeInfo = getRoute(key)

      if (routeInfo._2.equals(subscription.pClass))
      {
        var validSubscription = false
        val valueRoute = routeInfo._3._2
        val valueSub = subscription.pAttributes._2

        if (routeInfo._3._1.equals(subscription.pAttributes._1) || subscription.pAttributes._1.equals("ne")) {

          validSubscription = routeInfo._3._1 match {
            case "gt" => valueRoute > valueSub
            case "gte" => valueRoute >= valueSub
            case "lt" => valueRoute < valueSub
            case "lte" => valueRoute <= valueSub
            case "e" => valueRoute == valueSub
          }

          if (subscription.pAttributes._1.equals("ne") && routeInfo._3._1.contains("e")) {
            validSubscription = validSubscription && valueRoute != valueSub
          }
        }

        if(validSubscription) {
          matches += key
        }
      }
    }
    matches.toList
  }
}
