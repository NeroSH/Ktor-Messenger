package ru.anvarov.extensions

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

const val SECOND = 1000L
const val MINUTE = 60 * SECOND
const val HOUR = 60 * MINUTE
const val DAY = 24 * HOUR

fun Date.format(pattern: String="HH:mm:ss dd.MM.yy") : String {
    val dateFormat = SimpleDateFormat(pattern, Locale("ru"))
    return dateFormat.format((this))
}

fun Date.add(value:Int, units: TimeUnits = TimeUnits.SECOND) : Date {
    var time = this.time

    time += when (units) {
        TimeUnits.SECOND -> value * SECOND
        TimeUnits.MINUTE -> value * MINUTE
        TimeUnits.HOUR -> value * HOUR
        TimeUnits.DAY -> value * DAY
    }
    this.time = time
    return this
}

fun Date.humanizeDiff(date: Date = Date()) : String {
    val differenceInMillies = (this.time - date.time)
    val value: Long = abs(differenceInMillies)
    return when (differenceInMillies < 0 ) {
        true -> when {
            value >= DAY * 364 ->
                "более года назад"
            value >= DAY ->
                TimeUnits.DAY.plural((value / DAY).toInt()) + " назад"
            value >= HOUR ->
                TimeUnits.HOUR.plural((value / HOUR).toInt()) + " назад"
            value >= MINUTE ->
                TimeUnits.MINUTE.plural((value / MINUTE).toInt()) + " назад"
            else -> "только что"
        }
        false -> when {
            value >= DAY * 364 ->
                "более чем через год"
            value >= DAY ->
                "через " + TimeUnits.DAY.plural((value / DAY).toInt())
            value >= HOUR ->
                "через " + TimeUnits.HOUR.plural((value / HOUR).toInt())
            value >= MINUTE ->
                "через " + TimeUnits.MINUTE.plural((value / MINUTE).toInt())
            else -> "только что"
        }
    }
}

fun TimeUnits.plural(value: Int) : String = when (this) {
    TimeUnits.SECOND -> when (value % 10) {
        1 ->  "$value секунду"
        2, 3, 4 -> "$value секунды"
        else ->  "$value секуд"
    }
    TimeUnits.MINUTE -> when (value % 10) {
        1 ->  "$value минуту"
        2, 3, 4 -> "$value минуты"
        else ->  "$value минут"
    }
    TimeUnits.HOUR -> when (value % 10) {
        1 ->  "$value час"
        2, 3, 4 -> "$value часа"
        else ->  "$value часов"
    }
    TimeUnits.DAY -> when (value % 10) {
        1 ->  "$value день"
        2, 3, 4 -> "$value дня"
        else ->  "$value дней"
    }
}

enum class TimeUnits {
    SECOND,
    MINUTE,
    HOUR,
    DAY
}
