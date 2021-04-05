package li.gravio.common

import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class TcpClient(val listener: IMessageReceiver, val logger: ILogger) {

    private var mRun = false
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private var mServerAddress: String? = null
    private var mServerPort: Int? = null

    private var mSocket: Socket? = null

    private val runnable = Runnable {
        logger.log("Starting TCP client.")
        mRun = true

        while (mRun) {
            logger.log("Connecting: ${mServerAddress!!}:${mServerPort!!}")
            try {
                val serverAddr = InetAddress.getByName(mServerAddress!!)
                mSocket = Socket()
                mSocket!!.connect(InetSocketAddress(serverAddr, mServerPort!!),3000)
                mSocket!!.soTimeout = 10000
                try {
                    writer =
                            PrintWriter(BufferedWriter(OutputStreamWriter(mSocket!!.getOutputStream())))
                    reader =
                            BufferedReader(InputStreamReader(mSocket!!.getInputStream()))


                    logger.log("Connected.")

                    while (mRun) {
                        val msg = reader!!.readLine()
                        if (msg != null)
                            listener.messageReceived(msg)
                    }
                } catch (e: Exception) {
                    logger.log("SockErr: $e")
                    Thread.sleep(1000)
                } finally {
                    mSocket!!.close()
                }
            } catch (e: Exception) {
                logger.log("ConnErr: $e")
                Thread.sleep(1000)
            }
        }
    }

    fun write(message: String?) {
        if (writer != null && !writer!!.checkError()) {
            writer!!.println(message)
            writer!!.flush()
        }
    }

    fun end() {
        logger.log("Shutting down TCP client.")

        mRun = false
        if (writer != null) {
            writer!!.flush()
            writer!!.close()
        }

        try {
            mSocket?.close()
        }catch (_: Exception) { }

        reader = null
        writer = null
    }

    fun begin(serverIp : String, serverPort : Int) {
        mServerAddress = serverIp
        mServerPort = serverPort

        Thread(runnable).start()
    }

    interface IMessageReceiver {
        fun messageReceived(message: String?)
    }
}