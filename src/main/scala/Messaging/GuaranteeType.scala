package Messaging

object GuaranteeType extends Enumeration {
  type GuaranteeType = Value

  val NONE=Value(1, "None")
  val ACK=Value(2, "ACK")
  val TIME = Value(3, "TIME")
}