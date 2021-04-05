package li.gravio.common

import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.ClosedByInterruptException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class TcpServer(val port: Int, val logger: ILogger) {
    private var serverSocket: ServerSocket? = null
    private val working = AtomicBoolean(true)

    private val clientHandlers: ArrayList<TcpClientHandler> = ArrayList(5)
    private var thread: Thread? = null

    private val runnable = Runnable {
        var socket: Socket? = null
        try {
            serverSocket = ServerSocket(port)
            logger.log("Listening on $port")
            while (working.get()) {
                if (serverSocket != null) {
                    socket = serverSocket!!.accept()
                    logger.log("New client: $socket")
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())))

                    // Use threads for each client to communicate with them simultaneously
                    val t = TcpClientHandler(reader, writer)
                    t.start()
                    clientHandlers.add(t)
                } else {
                    logger.log("Couldn't create ServerSocket!")
                }
            }
        } catch (e: InterruptedException) {
            // ignored
        } catch (e: ClosedByInterruptException) {
            // ignored
        } catch (e: IOException) {
            e.printStackTrace()
            try {
                socket?.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    fun begin() {
        thread = Thread(runnable)
        thread!!.start()
    }

    fun end() {
        working.set(false)
        thread?.interrupt()
        try {
            serverSocket?.close()
        } catch (e: IOException) {}
        logger.log("Listener shutdown.")
    }

    fun broadcast(msg: String){
        for (cl in clientHandlers)
            if (cl.isAlive)
                cl.write(msg)
    }
}

class TcpClientHandler(private val reader: BufferedReader, private val writer: PrintWriter) : Thread() {
    private var sendQueue: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    override fun run() {
        while (true) {
            try {
                if(reader.ready()){
                    reader.read()
                }

                var msg = sendQueue.poll()
                while (msg != null){
                    writer.println(msg)
                    writer.flush()
                    msg = sendQueue.poll()
                }

                sleep(10L)
            } catch (e: IOException) {
                e.printStackTrace()
                try {
                    reader.close()
                    writer.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                try {
                    reader.close()
                    writer.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    fun write(msg: String){
        sendQueue.offer(msg)
    }
}