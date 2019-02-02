//
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import java.io.IOException

class JsonReader {
    companion object {
        @DslMarker
        annotation class JsonReaderItemMarker

        @DslMarker
        annotation class JsonReaderObjMarker

        @DslMarker
        annotation class JsonReaderArrMarker

        fun read(jsonParser: JsonParser, configureBlock: JsonReader.Item.() -> Unit) {
            val reader = JsonReader.Item()
            reader.configureBlock()
            if (null != reader.objBlockReader) {
                if (jsonParser.currentToken() != JsonToken.START_OBJECT) {
                    if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
                        throw IOException("Error: START_OBJECT expected.")
                    }
                }
            } else if (null != reader.arrBlockReader) {
                if (jsonParser.currentToken() != JsonToken.START_ARRAY) {
                    if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                        throw IOException("Error: START_ARRAY expected.")
                    }
                }
            } else {
                throw IOException("Error: 'objct {...}' or 'array {...}' configure block expected.")
            }
            reader.read(jsonParser)
        }

        private fun log(str: String) {
//                println(str)
        }
    }

    @JsonReaderItemMarker
    class Item {
        private var valueBlockReader: (JsonParser.() -> Unit)? = null
        internal var objBlockReader: Obj? = null
        internal var arrBlockReader: Arr? = null

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
            log("read i0 ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
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
    }

    @JsonReaderArrMarker
    class Arr {
        private var onStartReadBlock: (() -> Unit)? = null
        private var onFinishReadBlock: (() -> Unit)? = null

        private var itemReader = Item()

        fun onStartRead(function: () -> Unit) {
            onStartReadBlock = function
        }

        fun onFinishRead(function: () -> Unit) {
            onFinishReadBlock = function
        }

        fun value(readerBlock: JsonParser.() -> Unit) {
            itemReader.value(readerBlock)
        }

        fun objct(configureBlock: Obj.() -> Unit) {
            itemReader.objct(configureBlock)
        }

        fun array(configureBlock: Arr.() -> Unit) {
            itemReader.array(configureBlock)
        }

        fun read(jsonParser: JsonParser) {
            if (jsonParser.currentToken() != JsonToken.START_ARRAY) {
                throw IOException("Error: START_ARRAY expected.")
            }
            log("read a0 ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
            onStartReadBlock?.invoke()
            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                itemReader.read(jsonParser)
            }
            onFinishReadBlock?.invoke()
        }
    }

    @JsonReaderObjMarker
    class Obj {
        private val map = hashMapOf<String, Item>()
        private var onStartReadBlock: ((String) -> Unit)? = null

        private fun itemReader(name: String): Item {
            var result = map[name]
            if (result == null) {
                result = Item()
                map[name] = result
            }
            return result
        }

        private var onFinishReadBlock: ((String) -> Unit)? = null

        fun onStartRead(function: (String) -> Unit) {
            onStartReadBlock = function
        }

        fun onFinishRead(function: (String) -> Unit) {
            onFinishReadBlock = function
        }

        fun value(name: String, readerBlock: JsonParser.() -> Unit) {
            itemReader(name).value(readerBlock)
        }

        fun objct(name: String, configureBlock: Obj.() -> Unit) {
            itemReader(name).objct(configureBlock)
        }

        fun array(name: String, configureBlock: Arr.() -> Unit) {
            itemReader(name).array(configureBlock)
        }

        fun read(jsonParser: JsonParser) {
            if (jsonParser.currentToken() != JsonToken.START_OBJECT) {
                throw IOException("Error: START_OBJECT expected.")
            }
            log("read o0 ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
            val thisFieldName = jsonParser.currentName ?: ""
            onStartReadBlock?.invoke(thisFieldName)
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                val fieldName = jsonParser.currentName
                log("read  of ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
                jsonParser.nextToken()
                log("read ov ${jsonParser.currentToken()} ${jsonParser.currentName} ${jsonParser.getValueAsString("")}")
                val itemReader = map[fieldName]
                if (null != itemReader) {
                    itemReader.read(jsonParser)
                } else {
                    jsonParser.skipChildren()
                }
            }
            onFinishReadBlock?.invoke(thisFieldName)
        }
    }
}
