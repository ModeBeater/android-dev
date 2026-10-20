package kz.kbtu.furnistore.domain.util

import java.util.Locale

/** Formats a price as tenge: 125000.0 -> "125 000 ₸". */
fun Double.tenge(): String {
    val whole = "%.0f".format(Locale.US, this)
    val grouped = whole.reversed().chunked(3).joinToString(" ").reversed()
    return "$grouped ₸"
}

/** Formats a discount fraction as a percentage: 0.07 -> "7%". */
fun Double.percent(): String = "${(this * 100).toInt()}%"
