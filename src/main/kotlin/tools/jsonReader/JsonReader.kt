package tools.jsonReader

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

        fun read(jsonParser: JsonParser, configureBlock: Item.() -> Unit) {
            val reader = Item()
            reader.configureBlock()
            if (null != reader.objBlockReader) {
                if (jsonParser.currentToken != JsonToken.START_OBJECT) {
                    if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
                        throw IOException("Error: START_OBJECT expected.")
                    }
                }
            } else if (null != reader.arrBlockReader) {
                if (jsonParser.currentToken != JsonToken.START_ARRAY) {
                    if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                        throw IOException("Error: START_ARRAY expected.")
                    }
                }
            } else {
                throw IOException("Error: 'objct {...}' or 'array {...}' configure block expected.")
            }
            reader.read(jsonParser)
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
            if (jsonParser.currentToken != JsonToken.START_ARRAY) {
                throw IOException("Error: START_ARRAY expected.")
            }
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
        private var onFinishReadBlock: ((String) -> Unit)? = null

        private fun itemReader(name: String): Item {
            var result = map[name]
            if (result == null) {
                result = Item()
                map[name] = result
            }
            return result
        }

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
            if (jsonParser.currentToken != JsonToken.START_OBJECT) {
                throw IOException("Error: START_OBJECT expected.")
            }
            val thisFieldName = jsonParser.currentName ?: ""
            onStartReadBlock?.invoke(thisFieldName)
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                val fieldName = jsonParser.currentName
                jsonParser.nextToken()
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
