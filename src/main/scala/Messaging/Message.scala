package Messaging

import Communication.SocketData

@SerialVersionUID(100L)
class Message(val ID: Int,
              val SocketData: SocketData,
              val receiverID: Int,
              val content: Any,
              val timestamp: Int) extends Serializable
