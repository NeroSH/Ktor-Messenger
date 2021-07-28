package ru.anvarov

import extensions.toPair
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.Client
import ru.anvarov.extensions.format
import ru.anvarov.extensions.toJson
import ru.anvarov.extensions.toMessage
import ru.anvarov.models.Message
import java.util.*

const val DEFAULT_PORT = 9005
val clientsMap = mutableMapOf<String, Client>()
val messages = mutableListOf<Message>()
var clientsCout = 0
var verbose: Boolean = false


@KtorExperimentalAPI
fun main(args: Array<String>) {
    /* Setting default configuration
     */
    val PORT = if (args.isEmpty()) {
        DEFAULT_PORT
    } else {
        args[0].toIntOrNull() ?: DEFAULT_PORT
    }
    verbose = if (args.size == 2) {
        args[1] == "-v"
    } else {
        false
    }
    println(
        """
*************************************************************
* usage example:                                            *
*                                                           *
*************************************************************

    Server.exe $DEFAULT_PORT -v    - to see more details
    Server.exe $DEFAULT_PORT       - to see less details

*************************************************************
*                   DEFAULT_PORT = $DEFAULT_PORT                     *
*************************************************************


""".trimIndent()
    )

    val serverSocket: ServerSocket
    try {
        serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
            .tcp()
            .bind(port = PORT)
    } catch (e: Exception) {
        println("Error when starting server: ${e.message}")
        return
    }

    println("""
*************************************************************
Server running at [localhost:${serverSocket.localAddress.port}]
*************************************************************


""".trimIndent())
    runBlocking {
        try {
            while (true) {
                val clientSocket: Socket = serverSocket.accept()
                val read = clientSocket.openReadChannel()
                val write = clientSocket.openWriteChannel(autoFlush = true)
                val newClient: Client
                val name: String?
                name = read.readUTF8Line(Int.MAX_VALUE).toString()
                newClient = when {
                    name.isNullOrEmpty() -> Client(clientSocket = clientSocket, writer = write, reader = read)
                    name in clientsMap.keys -> Client(
                        name + "_${++clientsCout}",
                        clientSocket,
                        writer = write,
                        reader = read
                    )
                    else -> Client(name, clientSocket, writer = write, reader = read)

                }
                sendBroadcast(Message("server", Date().format(), "\tnew user [${newClient.userName}] connected"))
                clientsMap.plusAssign(newClient.toPair())
                val chat = messages.toJson()
                write.writeStringUtf8("${newClient.userName}\n")
                write.writeStringUtf8("$chat\n")

                launch {
                    while (!clientSocket.isClosed) {
                        var json: String?
                        try {
                            json = read.readUTF8Line(Int.MAX_VALUE)
                        } catch (e: Exception) {
                            println("Reading msg error : ${newClient.userName}:${clientSocket.remoteAddress} ${e.message}")
                            break
                        }
                        if (json.isNullOrEmpty()) continue
                        val newMsg = json.toMessage()
                        if (verbose) println("json: $json")

                        try {
                            launch {
                                messages.add(newMsg)
                            }

                            launch {
                                sendBroadcast(newMsg)
                            }
                        } catch (e: Exception) {
                            println(e.message)
                        }
                        delay(10)
                    }
                }
            }
        } catch (e: Exception) {
            println(e.message)
            closeConnection()
        }
    }

}

@KtorExperimentalAPI
suspend fun sendBroadcast(newMsg: Message) {
    with(clientsMap.iterator()) {
        forEach { (name, client) ->
            if (client.clientSocket.isClosed) {
                println("Client $name is closed")
                client.clientSocket.close()
                remove()
            } else if (newMsg.userName != name) {
                if (newMsg.userName == "server") {
                    println("${newMsg.userName} >>> [$name] ${newMsg.text}")
                    try {
                        client.writer.writeStringUtf8("${newMsg.text}\n")
                    } catch (e: Exception) {
                        println("Error server announcing to $name")
                    }
                } else {
                    try {
                        client.writer.writeStringUtf8(">> ${newMsg.userName}: ${newMsg.text}  [${newMsg.time}]\n")
                        if (verbose) println("${newMsg.userName} >>> [$name] ${newMsg.text}")
                    } catch (e: Exception) {
                        println("Error sending message to $name")
                    }
                }
            }
        }
    }
}

fun closeConnection() {
    println("Closing connections")
    clientsMap.forEach { (_, client) ->
        client.clientSocket.close()
    }
}
