package Messaging

trait MessageContent

@SerialVersionUID(1L)
case class Advertisement(var ID: (Int, Int),
                         var pClass: String,
                         var pAttributes: List[Int => Boolean]) extends Serializable with  MessageContent

@SerialVersionUID(1L)
case class Publication(ID: (Int, Int), content: Any) extends Serializable with  MessageContent

@SerialVersionUID(1L)
case class Subscription(var ID: (Int, Int),
                        var pClass: String,
                        var pAttributes: List[Int => Boolean]) extends Serializable with  MessageContent