package com.example.audiostreamer

import android.util.Log
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
// Embedded MJPEG Server Class
class MjpegServer(private val port: Int) {
    @Volatile var isRunning = false
    @Volatile var currentJpeg: ByteArray? = null
    private var serverThread: Thread? = null
    private var serverSocket: ServerSocket? = null // Added to track the socket

    fun start() {
        if (isRunning) return
        isRunning = true
        serverThread = Thread {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    try {
                        // This blocks until a browser connects
                        val client = serverSocket!!.accept() 
                        handleClient(client)
                    } catch (e: Exception) {
                        // Normal exception when serverSocket is closed by stop()
                    }
                }
            } catch (e: Exception) {
                Log.e("MjpegServer", "Port already in use or binding error", e)
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
        serverThread?.start()
    }

    fun stop() {
        isRunning = false
        // Forcibly close the socket to unblock the accept() method and free the port!
        try { serverSocket?.close() } catch (e: Exception) {} 
        serverThread?.interrupt()
    }

    private fun handleClient(socket: Socket) {
        Thread {
            try {
                val out: OutputStream = socket.getOutputStream()
                out.write(("HTTP/1.0 200 OK\r\n" +
                        "Connection: close\r\n" +
                        "Cache-Control: no-cache\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Content-type: multipart/x-mixed-replace; boundary=--BoundaryString\r\n\r\n").toByteArray())

                while (isRunning && socket.isConnected) {
                    currentJpeg?.let { jpeg ->
                        out.write(("--BoundaryString\r\n" +
                                "Content-type: image/jpeg\r\n" +
                                "Content-Length: ${jpeg.size}\r\n\r\n").toByteArray())
                        out.write(jpeg)
                        out.write("\r\n\r\n".toByteArray())
                        out.flush()
                    }
                    Thread.sleep(33) // ~30 fps
                }
            } catch (e: Exception) {
                // Ignore disconnects
            } finally {
                try { socket.close() } catch (e: Exception) {}
            }
        }.start()
    }
}
