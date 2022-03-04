package Communication

import Messages.{Message, SenderInfo}

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

class SenderSocket(val senderInfo: SenderInfo ) {
  def sendMessage( messageToSend:Message,portReceiver: Int,  addressReceiver:String ): Message = {

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

      System.out.println("Connection ended")
    }
    //return message
    messageToSend
  }
}