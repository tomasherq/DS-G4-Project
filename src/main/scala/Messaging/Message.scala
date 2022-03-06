package Messaging

import Communication.SocketData
import org.apache.commons.net.ntp.TimeStamp

@SerialVersionUID(1L)
class Message(val ID: (Int, Int),
              val sender: SocketData,
              val destination: Int,
              val content: MessageTypes,
              val timestamp: TimeStamp) extends Serializable
