package ru.anvarov.extensions

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.anvarov.models.Message

fun Message.toJson( ): String = Json.encodeToString(this)

fun MutableList<Message>.toJson( ): String  {//= "[" + this.forEach { Json.encodeToString(this) } + "]"
    var result = "["
    forEachIndexed { index, message ->
        result += Json.encodeToString(message)
        if (index != size - 1)
            result += ","
    }
//    this.forEach { result += Json.encodeToString(it) + "," }
    result += "]"
    return result
}
