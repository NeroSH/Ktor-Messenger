package ru.anvarov

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.*
import models.Client
import ru.anvarov.extensions.format
import ru.anvarov.extensions.toJson
import ru.anvarov.extensions.toMessagesList
import ru.anvarov.models.Message
import java.net.ConnectException
import java.util.*
import kotlin.system.exitProcess

const val URL = "127.0.0.1"
var chat = listOf<Message>()
lateinit var client: Client

@KtorExperimentalAPI
fun main(args: Array<String>) {
    /* Setting default configuration
     */
    val PORT = if (args.isEmpty()) {
        9005
    } else {
        args[0].toIntOrNull() ?: 9005
    }
    val verbose: Boolean = if (args.size == 2) {
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
    println("port set to: $PORT\n\n")

    print("Enter your name: ")
    val name = readLine()
    val clientSocket: Socket

    runBlocking {
        try {
            clientSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
                .tcp()
                .connect(URL, port = PORT)

        } catch (e: ConnectException) {
            println("Server $URL:$PORT is not reachable")
            println("Error: ${e.message}")
            exitProcess(0)
        }
        if (verbose) println("Connected to server: [$URL:$PORT]")
        val read = clientSocket.openReadChannel()
        val write = clientSocket.openWriteChannel(autoFlush = true)
        write.writeStringUtf8("$name\n")
        val response = read.readUTF8Line()
        client = Client(response!!, clientSocket, write, read)
        println("Welcome, $response!\n")
        val json = client.reader.readUTF8Line()
        if (verbose) println("Chat history: $json")
        try {
            chat = json?.toMessagesList()!!
        } catch (e: Exception) {
            println("Error while deserializing chat\n${e.message}")
        }
        for (msg in chat) {
            println(
                """
                >>${msg.userName}: ${msg.text} [${msg.time}] 
            """.trimIndent()
            )
        }
        try {
            val writingJob = launch(Dispatchers.IO) {
                while (true) {
                    try {
                        if (client.clientSocket.isClosed) return@launch
                        print("msg: ")
                        val text = readLine()
                        if (text.isNullOrEmpty()) continue
                        val time = Date().format("HH:MM:SS")
                        val data = Message(client.userName, time, text)
                        if (verbose) println(data.toJson())
                        client.writer.writeStringUtf8("${data.toJson()}\n")
                        delay(10)
                    } catch (e: IOException) {
                        println("\nError when sending message: ${e.message}")
                        this.cancel()
//                            throw AssertionError("Server is not reachable")
                    }
                }
            }
            launch(Dispatchers.IO) {
                while (client.clientSocket.socketContext.isActive) {
                    var msg: String?
                    try {
                        msg = client.reader.readUTF8Line(Int.MAX_VALUE)
                        println(msg)
                        print("msg: ")
                    } catch (e: IOException) {
                        println("\nError when reading: ${e.message}")
                        client.clientSocket.close()
                        writingJob.cancel()
                        this.cancel()
                    }
                    delay(10)
                }
            }

        } catch (e: Exception) {
            println("closing connections...")
            client.clientSocket.close()
        }
    }
}

