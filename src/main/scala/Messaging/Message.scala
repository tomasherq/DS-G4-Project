package Messaging

import Communication.SocketData

@SerialVersionUID(1L)
class Message(val ID: Int,
              val SocketData: SocketData,
              val receiverID: Int,
              val content: MessageTypes,
              val timestamp: Int) extends Serializable
