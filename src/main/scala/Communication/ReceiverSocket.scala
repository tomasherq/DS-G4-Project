package Communication

import Messaging.Message

import java.io.ObjectInputStream
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import scala.collection.mutable

class ReceiverSocket(val SocketData: SocketData) extends Thread {

  private val messageQueue = mutable.Queue[Message]()
  private val queueLimit=20

  def getFirstFromQueue(): Message = {
    messageQueue.dequeue()
  }

  def isQueueEmpty(): Boolean = {
    messageQueue.isEmpty
  }

  override def run(): Unit = {
    val socketChannelReceiver = ServerSocketChannel.open
    socketChannelReceiver.configureBlocking(true)

    socketChannelReceiver.socket.bind(new InetSocketAddress(SocketData.address, SocketData.port))

    while (true) {
      val connectionToSocket = socketChannelReceiver.accept
      val ois = new ObjectInputStream(connectionToSocket.socket.getInputStream)
      val messageReceived = ois.readObject.asInstanceOf[Message]
      // messageQueue.size < queueLimit || true
      if (true) {
        messageQueue.enqueue(messageReceived)
      } else {
        println("[DROP] Message dropped from " + SocketData.ID)
      }
    }
  }
}