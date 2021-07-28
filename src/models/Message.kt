package ru.anvarov.models

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val userName: String?,
    val time: String,
    val text: String?
)