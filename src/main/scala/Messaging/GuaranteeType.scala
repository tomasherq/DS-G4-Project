package Messaging

object GuaranteeType extends Enumeration {

  type GuaranteeType = Value
  val NONE, ACK, GROUPID, TIME = Value

}