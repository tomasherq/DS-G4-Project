package Communication

import Messages.{Message, SenderInfo}
import java.io.ObjectInputStream
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import scala.collection.mutable

class ReceiverSocket(val senderInfo:SenderInfo) extends Thread {

  private var messageQueue = mutable.Queue[Message]()

  def getFirstFromQueue(): Message = {
    messageQueue.dequeue()
  }

  def isQueueEmpty(): Boolean = {
    messageQueue.isEmpty
  }

  def emptyQueue(): Unit = {
    messageQueue= mutable.Queue[Message]()
  }

  override def run(): Unit = {
    val socketChannelReceiver = ServerSocketChannel.open
    socketChannelReceiver.configureBlocking(true)
    socketChannelReceiver.socket.bind(new InetSocketAddress(senderInfo.address,senderInfo.port))

    while (true) {
      val connectionToSocket = socketChannelReceiver.accept
      val ois = new ObjectInputStream(connectionToSocket.socket.getInputStream)
      val messageReceived = ois.readObject.asInstanceOf[Message]

      messageQueue.enqueue(messageReceived)
      // I don't know if we need this
      Thread.sleep(10)
    }
  }
}