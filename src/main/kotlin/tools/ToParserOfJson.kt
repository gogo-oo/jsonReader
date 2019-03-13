package tools

import tools.jsonDescription.*
import tools.stringUtil.plusAssign
import tools.stringUtil.times

class ToParserOfJson {
    class JPath(val params: Params = Params(), val items: Array<Item> = emptyArray()) {
        class Item(val name: String, val type: Supported, val parent: Item? = null) {
            companion object {
                val empty = Item("", Str_)
            }

            val className = "_$name"
            val isParentArray = parent is Item && parent.type is Arr
        }

        val lastItem: Item = if (items.isNotEmpty()) items.last() else Item.empty
        inline val size get() = items.size
        inline val fieldName get() = if (lastItem.name.isNotEmpty()) lastItem.name else params.resultProp
        val jsonFieldName = if (lastItem.name.isNotEmpty()) """("${lastItem.name}")""" else ""
        val resultStr = "${params.resultProp}.${items.joinToString(".") { if (it.isParentArray) "list.last()" else it.name }}".replace("${params.resultProp}..", "${params.resultProp}.")
        val resultStrOnStartReadObj = "${params.resultProp}.${items.joinToString(".") { it.name }}".replace("${params.resultProp}..", "${params.resultProp}.").replace("..", ".list.last().")
        val default = "${params.resultProp}.${items.joinToString(".") { if (it.isParentArray) "defaultVal" else it.name }}".replace("${params.resultProp}..", "${params.resultProp}.").replace("..", ".list.last().")
        val pathStr
            get() = StringBuilder().apply {
                for (item in items) {
                    if (item.name.isNotEmpty()) {
                        append('.')
                        append(item.name)
                    }
                }
            }.toString()
        //                val className = if (lastItem != Item.empty) lastItem.className else params.resultClass
        val className = if (lastItem == Item.empty) params.resultClass else if (items.size >= 2 && items[items.size - 2].type is Arr) ObjVal else lastItem.className
        val classNameFull: String

        init {
            val _classNameFull = StringBuilder()
            var divider = ""
            for (item in items) {
//                if (null == item.parent && item.name == "") continue
                _classNameFull += divider
                _classNameFull += if (item.isParentArray) ObjVal else item.className
                divider = "."
            }
            classNameFull = _classNameFull.toString().replace("_._", "_")
        }

        fun and(name: String, item: Supported) = JPath(params, arrayOf(*items, JPath.Item(name, item, lastItem)))

        fun and(item: Supported) = JPath(params, arrayOf(*items, JPath.Item("", item, lastItem)))

    }

    class OutRes {
        val outFirst = StringBuilder()
        val outVal__ = StringBuilder()
        val outRead_ = StringBuilder()
    }

    class Params(val resultClass: String = "Res", val resultProp: String = "result") {
        val hasPathStr = false

        companion object {
            val empty = Params()
        }
    }

    class ScalarDescription(val scalarName: String, val defaultValue: String, val getValueAsScalar: String)

    companion object {

        const val ObjVal = "ObjVal"

        fun Scalar.description() = when (this) {
            Str_ -> ScalarDescription("String", "\"\"", "getValueAsString")
            Int_ -> ScalarDescription("Int", "-1", "getValueAsInt")
            Flt_ -> ScalarDescription("Double", "-1.0", "getValueAsDouble")
            Bool -> ScalarDescription("Boolean", "false", "getValueAsBoolean")
        }

        fun toJsonParserWithCompanion(itemDescription: Supported, className: String): String {
            val resVal = "result"
            val ouRes = toJsonParser(itemDescription, Params(className, resVal))
            val result = StringBuilder()
            result += """package tools

import com.fasterxml.jackson.core.JsonParser
import tools.jsonReader.JsonReader

"""
            result += ouRes.outFirst
            result += ouRes.outVal__

            result += """

    companion object {

        fun read(jsonParser: JsonParser): $className {
            val $resVal = $className()

            JsonReader.read(jsonParser) {
"""

            result += ouRes.outRead_
            result += """            }
            return $resVal
        }
    }
}
"""
            return result.toString()
        }

        fun toJsonParser(itemDescription: Supported, params: Params = Params.empty, path: JPath = JPath(params), innerArray: Boolean = false): OutRes {
            val pathStr = if (params.hasPathStr) " // ${path.pathStr}" else ""
            val tabString = "    "
            val tabStr = tabString.times(0) + tabString.times(path.size)
            val tabStR = tabString.times(4) + tabString.times(path.size)
            val result = OutRes()
            when (itemDescription) {
                is Obj -> {
                    result.outFirst += "${tabStr}class ${path.className} {\n"
                    result.outRead_ += "${tabStR}objct${path.jsonFieldName} {\n"
                    if (path.lastItem.isParentArray) {
                        result.outRead_ += "$tabString${tabStR}onStartRead { ${path.resultStrOnStartReadObj}list += ${path.classNameFull}() }\n"
                    }
                    val itemResultOutVal = StringBuilder()
                    for ((name, item) in itemDescription.fields) {
                        val itemResult = toJsonParser(item, path = path.and(name, item))
                        result.outFirst += itemResult.outFirst
                        itemResultOutVal += itemResult.outVal__
                        result.outRead_ += itemResult.outRead_
                    }
                    result.outFirst += itemResultOutVal
                    if (path.size != 0) {
                        result.outFirst += "$tabStr}\n"
                        if (!path.lastItem.isParentArray) result.outFirst += "\n"
                        result.outVal__ += "${tabStr}val ${path.fieldName} = ${path.className}()${pathStr}\n"
                    }
                    result.outRead_ += "$tabStR}\n"
                }
                is Arr -> {
                    //?? result.outRead_ += "${tabStr}${tabString}onStartRead { ${params.resultStr}.dimension.add(${params.resultStr}.list.size) }\n"
                    //                    array {
                    //                        onStartRead { result.arrArrStrVal.dimension.add(result.arrArrStrVal.list.size) }
                    //                        value { result.arrArrStrVal.list += getValueAsString(result.arrArrStrVal.default) }
                    //                    }
                    if (itemDescription.itemTypes.isNotEmpty()) {
                        val item = itemDescription.itemTypes[0]
                        val rowCounts = StringBuilder()
                        if (innerArray) {
                            rowCounts += ", val rowCounts: MutableList<Int> = mutableListOf()"
                            result.outRead_ += "${tabStR}array {\n"
                        } else {
                            result.outRead_ += "${tabStR}array${path.jsonFieldName} {\n"
                        }
                        when (item) {
                            is Obj, is Scalar -> {
                                val newPath = path.and(item)
                                when (item) {
                                    is Obj -> {
                                        val itemResult = toJsonParser(item, path = newPath)
                                        result.outFirst += "${tabStr}class ${path.className}(val list: MutableList<$ObjVal> = mutableListOf()$rowCounts, val defaultVal: $ObjVal = $ObjVal()) {\n"
                                        result.outFirst += itemResult.outFirst
                                        if (path.size != 0) {
                                            result.outFirst += "$tabStr}\n\n"
                                        }
                                        result.outRead_ += itemResult.outRead_
                                    }
                                    is Scalar -> {
                                        val desc = item.description()
                                        result.outFirst += "${tabStr}class ${path.className}(val list: MutableList<${desc.scalarName}> = mutableListOf()$rowCounts, val defaultVal: ${desc.scalarName} = ${desc.defaultValue})\n"
                                        result.outRead_ += "$tabString${tabStR}value${newPath.jsonFieldName} { ${path.resultStr}.list += ${desc.getValueAsScalar}(${path.resultStr}.defaultVal) }\n"
                                    }
                                }
                                if (path.size != 0) {
                                    result.outVal__ += "${tabStr}val ${path.fieldName} = ${path.className}()${pathStr}\n"
                                }
                            }
                            is Arr -> {
                                val itemResult = toJsonParser(item, path = path, innerArray = true)
                                result.outFirst += itemResult.outFirst
                                result.outVal__ += itemResult.outVal__
                                result.outRead_ += itemResult.outRead_
                            }
                        }
                        if (innerArray) {
                            result.outRead_ += "$tabString${tabStR}onFinishRead { ${path.resultStrOnStartReadObj}.rowCounts.add(${path.resultStrOnStartReadObj}.list.size) }\n"
//
                        }
                        result.outRead_ += "$tabStR}\n"
                    }
                }
                is Scalar -> {
                    val desc = itemDescription.description()
                    result.outVal__ += "${tabStr}var ${path.fieldName} = ${desc.defaultValue}${pathStr}\n"
                    result.outRead_ += "${tabStR}value${path.jsonFieldName} { ${path.resultStr} = ${desc.getValueAsScalar}(${path.default}) }\n"
                }
            }
            return result
        }

    }
}
