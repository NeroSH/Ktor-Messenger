package models

import io.ktor.network.sockets.*
import io.ktor.utils.io.*

data class Client(
    val userName: String = "Anonymus",
    val clientSocket: Socket,
    val writer: ByteWriteChannel = clientSocket.openWriteChannel(autoFlush = true),
    val reader: ByteReadChannel = clientSocket.openReadChannel()
)