package Messaging

import Messaging.GuaranteeType.GuaranteeType

trait MessageContent

@SerialVersionUID(1L)
case class Advertisement(var ID: (Int, Int),
                         var pClass: String,
                         var pAttributes: List[Int => Boolean]) extends Serializable with  MessageContent

@SerialVersionUID(1L)
case class Publication(ID: (Int, Int), content: Any, timestamp: Int) extends Serializable with  MessageContent

@SerialVersionUID(1L)
case class Subscription(var ID: (Int, Int),
                        var pClass: String,
                        var pAttributes: List[Int => Boolean]) extends Serializable with  MessageContent


@SerialVersionUID(1L)
case class Guarantee(guarantee: GuaranteeType) extends Serializable with  MessageContent