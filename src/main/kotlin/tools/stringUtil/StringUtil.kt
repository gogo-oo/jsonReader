package tools.stringUtil

//
//
//
//
//

@Suppress("NOTHING_TO_INLINE")
inline operator fun StringBuilder.plusAssign(any: Any) {
    this.append(any.toString())
}

fun String.times(count: Int): String {
    if (count <= 0) {
        return ""
    }
    val result = StringBuilder()
    for (i in 0 until count) {
        result += this
    }
    return result.toString()
}
