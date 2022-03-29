package Routing

import Messaging.{Publication, Subscription}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class RoutingTable {

  private val table = mutable.Map[(Int, Int), (Int, String, (String, Int))]()

  def getTable(): mutable.Map[(Int, Int), (Int, String, (String, Int))] = {
    table
  }

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
        val valueAd = routeInfo._3._2
        val valueSub = subscription.pAttributes._2

        if (routeInfo._3._1.equals(subscription.pAttributes._1)) {

          validSubscription = routeInfo._3._1 match {
            case "gt" => valueAd >= valueSub
            case "lt" => valueAd <= valueSub
            case "e" => valueAd == valueSub
            case "ne" => valueAd == valueSub
          }
        }

        if (validSubscription) {
          matches += key
        }
      }
    }
    matches.toList
  }

  def findMatch(publication: Publication): List[(Int, Int)] = {
    val matches: ListBuffer[(Int, Int)] = ListBuffer[(Int, Int)]()

    for (key <- table.keys) {
      val routeInfo = getRoute(key)

      if (routeInfo._2.equals(publication.pClass))
      {
        var validPublication = false
        val valueSub = routeInfo._3._2
        val valuePub = publication.pAttributes._2

        if (routeInfo._3._1.equals(publication.pAttributes._1)) {

          validPublication = routeInfo._3._1 match {
            case "gt" => valueSub <= valuePub
            case "lt" => valueSub >= valuePub
            case "e" => valueSub == valuePub
            case "ne" => valueSub == valuePub
          }
        }

        if (validPublication) {
          matches += key
        }
      }
    }
    matches.toList
  }
}
