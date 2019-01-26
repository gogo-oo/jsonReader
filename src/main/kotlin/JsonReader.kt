//
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import java.io.IOException

class JsonReader {
    companion object {
        @DslMarker
        annotation class JsonReaderObjMarker

        @DslMarker
        annotation class JsonReaderArrMarker

        fun readObject(jsonParser: JsonParser, configureBlock: JsonReader.Obj.() -> Unit) {
            val reader = JsonReader.Obj()
            reader.configureBlock()
            if (jsonParser.currentToken() != JsonToken.START_OBJECT) {
                if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
                    throw IOException("Error: START_OBJECT expected.")
                }
            }
            reader.read(jsonParser)
        }

        fun readArray(jsonParser: JsonParser, configureBlock: JsonReader.Arr.() -> Unit) {
            val reader = JsonReader.Arr()
            reader.configureBlock()
            if (jsonParser.currentToken() != JsonToken.START_ARRAY) {
                if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                    throw IOException("Error: START_ARRAY expected.")
                }
            }
            reader.read(jsonParser)
        }

        private fun log(str: String) {
//                println(str)
        }
    }


    @JsonReaderArrMarker
    class Arr {
        private var valueBlockReader: (JsonParser.() -> Unit)? = null
        private var objBlockReader: Obj? = null
        private var arrBlockReader: Arr? = null

        private var onStartReadBlock: (() -> Unit)? = null
        private var onFinishReadBlock: (() -> Unit)? = null

        fun onStartRead(function: () -> Unit) {
            onStartReadBlock = function
        }

        fun onFinishRead(function: () -> Unit) {
            onFinishReadBlock = function
        }

        fun value(readerBlock: JsonParser.() -> Unit) {
            valueBlockReader = readerBlock
        }

        fun objct(configureBlock: Obj.() -> Unit) {
            objBlockReader = Obj().apply { configureBlock() }
        }

        fun array(configureBlock: Arr.() -> Unit) {
            arrBlockReader = Arr().apply { configureBlock() }
        }

        fun read(jsonParser: JsonParser) {
            if (jsonParser.currentToken() != JsonToken.START_ARRAY) {
                throw IOException("Error: START_ARRAY expected.")
            }
            log("read a0 ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
            onStartReadBlock?.invoke()
            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                log("read av ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
                if (null != valueBlockReader) {
                    valueBlockReader?.invoke(jsonParser)
                } else {
                    if (null != objBlockReader) {
                        objBlockReader?.read(jsonParser)
                    } else {
                        if (null != arrBlockReader) {
                            arrBlockReader?.read(jsonParser)
                        } else {
                            jsonParser.skipChildren()
                        }
                    }
                }
            }
            onFinishReadBlock?.invoke()
        }
    }

    @JsonReaderObjMarker
    class Obj {
        private val map = hashMapOf<String, JsonParser.() -> Unit>()
        private val mapObj = hashMapOf<String, Obj>()
        private val mapArr = hashMapOf<String, Arr>()

        private var onStartReadBlock: ((String) -> Unit)? = null
        private var onFinishReadBlock: ((String) -> Unit)? = null

        fun onStartRead(function: (String) -> Unit) {
            onStartReadBlock = function
        }

        fun onFinishRead(function: (String) -> Unit) {
            onFinishReadBlock = function
        }

        fun value(name: String, readerBlock: JsonParser.() -> Unit) {
            map[name] = readerBlock
        }

        fun objct(name: String, configureBlock: Obj.() -> Unit) {
            mapObj[name] = Obj().apply { configureBlock() }
        }

        fun array(name: String, configureBlock: Arr.() -> Unit) {
            mapArr[name] = Arr().apply { configureBlock() }
        }

        fun read(jsonParser: JsonParser) {
            if (jsonParser.currentToken() != JsonToken.START_OBJECT) {
                throw IOException("Error: START_OBJECT expected.")
            }
            log("read o0 ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
            val thisFieldName = jsonParser.getCurrentName() ?: ""
            onStartReadBlock?.invoke(thisFieldName)
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                val fieldName = jsonParser.getCurrentName()
                log("read  of ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
                jsonParser.nextToken()
                log("read ov ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
                val valueReader = map[fieldName]
                if (null != valueReader) {
                    jsonParser.valueReader()
                } else {
                    val objectReader = mapObj[fieldName]
                    if (null != objectReader) {
                        objectReader.read(jsonParser)
                    } else {
                        val arrReader = mapArr[fieldName]
                        if (null != arrReader) {
                            arrReader.read(jsonParser)
                        } else {
                            jsonParser.skipChildren()
                        }
                    }
                }
            }
            onFinishReadBlock?.invoke(thisFieldName)
        }
    }
}
