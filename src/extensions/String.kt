package ru.anvarov.extensions

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.anvarov.models.Message

fun String.toMessage(): Message = when {
    isNotEmpty() || isNotBlank() -> Json.decodeFromString(this)
    else -> throw Exception("Cannot serialize JSON")
}

fun String.toMessagesList(): List<Message> {
    return Json.decodeFromString(this)
}


