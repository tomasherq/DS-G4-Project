package Communication

@SerialVersionUID(100L)
class SocketData(val ID: Int,
                 var address: String,
                 val port: Int) extends Serializable