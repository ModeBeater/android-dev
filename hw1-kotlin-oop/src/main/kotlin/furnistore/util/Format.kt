package furnistore.util

/** Formats a price as tenge: 125000.0 -> "125 000 ₸". */
fun Double.tenge(): String {
    val whole = "%.0f".format(this)
    val grouped = whole.reversed().chunked(3).joinToString(" ").reversed()
    return "$grouped ₸"
}

/** Prints a section header so the console output is easy to read. */
fun section(title: String) {
    println()
    println("== $title ==")
}
