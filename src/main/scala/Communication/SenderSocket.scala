package Communication

import Messaging.Message

import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import scala.collection.mutable
import java.io.File

@SerialVersionUID(1L)
class SenderSocket() extends Serializable {

  private val messageStoreUnit=mutable.Queue[Message]()
  private var currentMessageFile=""

  def saveMessageSent(): Unit={


  }


  def sendMessage(messageToSend: Message, addressReceiver: String, portReceiver: Int): Unit = {

    messageStoreUnit.enqueue(messageToSend)
    val sChannel = SocketChannel.open

    sChannel.configureBlocking(true)

    if (sChannel.connect(new InetSocketAddress(addressReceiver, portReceiver))) {
      val oos = new ObjectOutputStream(sChannel.socket.getOutputStream)
      oos.writeObject(messageToSend)
      oos.close()
    }
  }
}