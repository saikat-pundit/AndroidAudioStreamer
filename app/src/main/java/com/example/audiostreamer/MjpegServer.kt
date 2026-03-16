package com.example.audiostreamer

import android.util.Log
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class MjpegServer(private val port: Int) {
    @Volatile var isRunning = false
    @Volatile var currentJpeg: ByteArray? = null
    private var serverThread: Thread? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        serverThread = Thread {
            val serverSocket = ServerSocket(port)
            while (isRunning) {
                try {
                    val client = serverSocket.accept()
                    handleClient(client)
                } catch (e: Exception) {
                    Log.e("MjpegServer", "Accept error", e)
                }
            }
            serverSocket.close()
        }
        serverThread?.start()
    }

    fun stop() {
        isRunning = false
        serverThread?.interrupt()
    }

    private fun handleClient(socket: Socket) {
        Thread {
            try {
                val out: OutputStream = socket.getOutputStream()
                // Send the standard MJPEG HTTP headers
                out.write(("HTTP/1.0 200 OK\r\n" +
                        "Connection: close\r\n" +
                        "Cache-Control: no-cache\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Content-type: multipart/x-mixed-replace; boundary=--BoundaryString\r\n\r\n").toByteArray())

                // Continuously stream the latest JPEG frame
                while (isRunning && socket.isConnected) {
                    currentJpeg?.let { jpeg ->
                        out.write(("--BoundaryString\r\n" +
                                "Content-type: image/jpeg\r\n" +
                                "Content-Length: ${jpeg.size}\r\n\r\n").toByteArray())
                        out.write(jpeg)
                        out.write("\r\n\r\n".toByteArray())
                        out.flush()
                    }
                    Thread.sleep(33) // ~30 fps target
                }
            } catch (e: Exception) {
                // Client naturally disconnected
            } finally {
                socket.close()
            }
        }.start()
    }
}
