package Communication

import Messaging.Message
import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

@SerialVersionUID(100L)
class SenderSocket(val SocketData: SocketData) extends Serializable {

  def sendMessage(messageToSend: Message, addressReceiver: String, portReceiver: Int): Message = {

    val sChannel = SocketChannel.open
    sChannel.configureBlocking(true)

    if (sChannel.connect(new InetSocketAddress(addressReceiver, portReceiver))) {
      val oos = new ObjectOutputStream(sChannel.socket.getOutputStream)
      oos.writeObject(messageToSend)

      val ois = new ObjectInputStream(sChannel.socket.getInputStream)
      val s = ois.readObject.asInstanceOf[Message]

      oos.close()
      ois.close()

      return s
    }
    messageToSend
  }
}