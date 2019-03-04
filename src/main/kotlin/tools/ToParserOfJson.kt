package tools

import tools.jsonDescription.*
import tools.jsonDescription.Float
import tools.stringUtil.plusAssign
import tools.stringUtil.times

class ToParserOfJson {

    class OutRes {
        val outFirst = StringBuilder()
        val outVal_ = StringBuilder()
        val outRead = StringBuilder()
        var defaultValue = ""
        var scalarName = ""
    }

    //Satellite
    class Params(
            val resultClass: String = "Res",
            val resultStr: String = "result",
            val assignment: String = "",
            val default: String = "",
            val fieldName: String = "",
            val arrFieldName: String = "",
            var rootClass: String = ""
    ) {
        val jsonFieldName = if (fieldName.isNotEmpty()) """("$fieldName")""" else ""
    }

    companion object {

        const val ObjVal = "ObjVal"

        fun toJsonParserWithCompanion(itemDescription: Supported, className: String): String {
            val resVal = "result"
            val ouRes = toJsonParser(itemDescription, Params(className, resVal))
            val result = StringBuilder()
            result += """package tools

import com.fasterxml.jackson.core.JsonParser
import tools.jsonReader.JsonReader

"""
            result += ouRes.outFirst
            result += ouRes.outVal_

            result += """

    companion object {

        fun read(jsonParser: JsonParser): $className {
            val $resVal = $className()

            JsonReader.read(jsonParser) {
"""

            result += ouRes.outRead
            result += """            }
            return $resVal
        }
    }
}
"""
            return result.toString()
        }

        fun toJsonParser(itemDescription: Supported, params: Params = Params(), recurseCount: Int = 0): OutRes {
            if (recurseCount == 0) {
                params.rootClass = params.resultClass
            }

            val tabString = "    "
            val tabStr = tabString.times(0) + tabString.times(recurseCount)
            val tabStR = tabString.times(4) + tabString.times(recurseCount)
            val result = OutRes()
//            val res = StringBuilder()
            when (itemDescription) {
                is Obj -> {
                    result.outFirst += """${tabStr}class ${params.resultClass} {${'\n'}"""
                    result.outRead += "${tabStR}objct${params.jsonFieldName} {\n"
                    if (params.resultClass == ObjVal) {
                        result.outRead += "$tabString${tabStR}onStartRead { ${params.resultStr}.list += ${params.rootClass}._${params.arrFieldName}.$ObjVal() }\n"
                    }

                    val itemResultOutVal = StringBuilder()
                    for ((name, item) in itemDescription.fields) {
                        val resStr = if (params.resultClass == ObjVal) """${params.resultStr}.list.last().$name""" else """${params.resultStr}.$name"""
                        val defStr = if (params.resultClass == ObjVal) """${params.resultStr}.default.$name""" else """${params.resultStr}.$name"""
                        val itemResult = toJsonParser(item, Params("_$name", resStr, " =", defStr, name, name, rootClass = params.rootClass), recurseCount + 1)
                        result.outFirst += itemResult.outFirst
                        itemResultOutVal += itemResult.outVal_
                        result.outRead += itemResult.outRead
                    }
                    result.outFirst += itemResultOutVal
                    if (params.resultClass != params.rootClass) {
                        result.outFirst += "$tabStr}\n"
                        if (params.resultClass != ObjVal) result.outFirst += "\n"
                        result.outVal_ += "${tabStr}val ${if (params.fieldName.isNotEmpty()) params.fieldName else params.resultStr} = ${params.resultClass}()\n"
                    }
                    result.outRead += "$tabStR}\n"
                }
                is Arr -> {
//                    array {
//                        onStartRead { result.arrArrStrVal.dimension.add(result.arrArrStrVal.list.size) }
//                        value { result.arrArrStrVal.list += getValueAsString(result.arrArrStrVal.default) }
//                    }
                    if (itemDescription.itemTypes.isNotEmpty()) {
                        val item = itemDescription.itemTypes[0]
                        result.outRead += "${tabStR}array${params.jsonFieldName} {\n"
                        //result.outRead += "${tabStr}${tabString}onStartRead { ${params.resultStr}.dimension.add(${params.resultStr}.list.size) }\n"
                        val itemResult = toJsonParser(
                                item,
                                Params(ObjVal, params.resultStr, ".list +=", "${params.resultStr}.default", "", params.arrFieldName, rootClass = params.rootClass),
                                recurseCount + 1
                        )
                        when (item) {
                            is Obj, is Scalar -> {
                                result.outVal_ += "${tabStr}val ${params.arrFieldName} = _${params.arrFieldName}()\n"
                                when (item) {
                                    is Obj -> {
                                        result.outFirst += """${tabStr}class _${params.arrFieldName}(val list: MutableList<$ObjVal> = mutableListOf(), val default: $ObjVal = $ObjVal()) {${'\n'}"""
                                        result.outFirst += itemResult.outFirst
                                        result.outFirst += "$tabStr}\n\n"
                                    }
                                    is Scalar -> {
                                        result.outFirst += """${tabStr}class _${params.arrFieldName}(val list: MutableList<${itemResult.scalarName}> = mutableListOf(), val default: ${itemResult.scalarName} = ${itemResult.defaultValue})${'\n'}"""
                                    }
                                }
                            }
                            is Arr -> {
                                result.outFirst += itemResult.outFirst
                                result.outVal_ += itemResult.outVal_
                            }
                        }
                        result.outRead += itemResult.outRead
                        result.outRead += "$tabStR}\n"
                    }
                }
                is Scalar -> {
                    var getValueAsScalar = ""
                    when (itemDescription) {
                        is Str -> {
                            result.scalarName = "String"
                            result.defaultValue = "\"\""
                            getValueAsScalar = "getValueAsString"
                        }
                        is Int_ -> {
                            result.scalarName = "Int"
                            result.defaultValue = "-1"
                            getValueAsScalar = "getValueAsInt"
                        }
                        is Float -> {
                            result.scalarName = "Double"
                            result.defaultValue = "-1.0"
                            getValueAsScalar = "getValueAsDouble"
                        }
                        is Bool -> {
                            result.scalarName = "Boolean"
                            result.defaultValue = "false"
                            getValueAsScalar = "getValueAsBoolean"
                        }
                    }
                    result.outVal_ += "${tabStr}var ${params.fieldName} = ${result.defaultValue}\n"
                    result.outRead += "${tabStR}value${params.jsonFieldName} { ${params.resultStr}${params.assignment} $getValueAsScalar(${params.default}) }\n"
                }
            }
            return result
        }

    }
}
