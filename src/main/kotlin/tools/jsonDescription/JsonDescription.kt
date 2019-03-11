package tools.jsonDescription

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import tools.stringUtil.plusAssign

sealed class JsonItemDescription {
    override fun toString(): String = this.toJsonString()
}

object Unsupported : JsonItemDescription()

sealed class Supported : JsonItemDescription()

class Obj : Supported() {
    val fields = mutableMapOf<String, Supported>()
}

class Arr : Supported() {
    val itemTypes = mutableListOf<Supported>()
}

sealed class Scalar : Supported() {
    override fun toString(): String = this.simple.toString()
}

object Str_ : Scalar()
object Int_ : Scalar()
object Flt_ : Scalar()//Float
object Bool : Scalar()

val Scalar.simple: Any
    get() = when (this) {
        Str_ -> ""
        Int_ -> 1
        Flt_ -> 1.0
        Bool -> true
    }

fun JsonItemDescription.toJsonString() = when (this) {
    is Supported -> this.toJsonString()
    Unsupported -> "[\"Unsupported\"]"
}

fun Supported.toJsonString(): String {
    val separator = ","
    val itemDescription = this
    val res = StringBuilder()
    when (itemDescription) {
        is Obj -> {
            res += "{"
            var divider = ""
            for ((name, item) in itemDescription.fields) {
                res += divider
                res += """"$name":"""
                res += item.toJsonString()
                divider = separator
            }
            res += "}"
        }
        is Arr -> res += itemDescription.itemTypes.joinToString(separator, "[", "]") { it.toJsonString() }
        is Scalar -> when (itemDescription) {
            Str_ -> {
                res += "\""
                res += itemDescription.simple
                res += "\""

            }
            else -> res += itemDescription.simple
        }
    }
    return res.toString()
}

fun readJsonItemDescription(jsonParser: JsonParser): JsonItemDescription {
    if (null == jsonParser.currentToken) {
        jsonParser.nextToken()
    }
    when (jsonParser.currentToken) {
        JsonToken.VALUE_STRING -> return Str_
        JsonToken.VALUE_NUMBER_INT -> return Int_
        JsonToken.VALUE_NUMBER_FLOAT -> return Flt_
        JsonToken.VALUE_TRUE, JsonToken.VALUE_FALSE -> return Bool
        JsonToken.START_OBJECT -> {
            val res = Obj()
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                val fieldName = jsonParser.currentName
                jsonParser.nextToken()
                val itemDescription = readJsonItemDescription(jsonParser)
                if (itemDescription is Supported) {
                    res.fields[fieldName] = itemDescription
                }
            }
            return res
        }
        JsonToken.START_ARRAY -> {
            val res = Arr()
            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                val itemDescription = readJsonItemDescription(jsonParser)
                if (itemDescription is Supported) {
                    if (res.itemTypes.notContains(itemDescription)) {
                        res.itemTypes += itemDescription
                    }
                }
            }
            return res
        }
        else -> {
            jsonParser.skipChildren()
            return Unsupported
        }
    }
}

private fun MutableList<Supported>.notContains(itemDescription: Supported): Boolean {
    when (itemDescription) {
        is Obj -> {
            for (item in this) {
                if (item is Obj && item.fields.size == itemDescription.fields.size) {
                    if (item.toJsonString() == itemDescription.toJsonString()) {
                        return false
                    }
                }
            }
        }
        is Arr -> {
            for (item in this) {
                if (item is Arr && item.itemTypes.size == itemDescription.itemTypes.size) {
                    if (item.toJsonString() == itemDescription.toJsonString()) {
                        return false
                    }
                }
            }
        }
        is Scalar -> {
            if (this.contains(itemDescription)) {
                return false
            }
        }
    }

    return true
}
