package tools

import com.fasterxml.jackson.core.JsonParser
import tools.jsonReader.JsonReader

class ToParserOfJsonCheckRes {
    class _obj01 {
        var f01 = ""
        var f02 = -1
    }

    class _obj02 {
        class _f22 {
            var f211 = ""
            var f221 = -1
        }

        var f21 = ""
        val f22 = _f22()
    }

    class _arrObj01(val list: MutableList<ObjVal> = mutableListOf(), val default: ObjVal = ObjVal()) {
        class ObjVal {
            var f31 = ""
            var f32 = -1
        }
    }

    class _arrVal01(val list: MutableList<Int> = mutableListOf(), val default: Int = -1)
    class _arrArrVal01(val list: MutableList<Int> = mutableListOf(), val rowCounts: MutableList<Int> = mutableListOf(), val default: Int = -1)
    class _arrArrObj01(val list: MutableList<ObjVal> = mutableListOf(), val rowCounts: MutableList<Int> = mutableListOf(), val default: ObjVal = ObjVal()) {
        class ObjVal {
            class _f42 {
                class _FarrArrVal01(val list: MutableList<Int> = mutableListOf(), val rowCounts: MutableList<Int> = mutableListOf(), val default: Int = -1)

                var f411 = "--"
                var f421 = -12
                val FarrArrVal01 = _FarrArrVal01()
            }

            var f41 = ""
            val f42 = _f42()
        }
    }

    var strVal01 = ""
    var strVal02 = ""
    var intVal01 = -1
    val obj01 = _obj01()
    val obj02 = _obj02()
    val arrObj01 = _arrObj01()
    val arrVal01 = _arrVal01()
    val arrArrVal01 = _arrArrVal01()
    val arrArrObj01 = _arrArrObj01()


    companion object {

        fun read(jsonParser: JsonParser): ToParserOfJsonCheckRes {
            val result = ToParserOfJsonCheckRes()

            JsonReader.read(jsonParser) {
                objct {
                    value("strVal01") { result.strVal01 = getValueAsString(result.strVal01) }
                    value("strVal02") { result.strVal02 = getValueAsString(result.strVal02) }
                    value("intVal01") { result.intVal01 = getValueAsInt(result.intVal01) }
                    objct("obj01") {
                        value("f01") { result.obj01.f01 = getValueAsString(result.obj01.f01) }
                        value("f02") { result.obj01.f02 = getValueAsInt(result.obj01.f02) }
                    }
                    objct("obj02") {
                        value("f21") { result.obj02.f21 = getValueAsString(result.obj02.f21) }
                        objct("f22") {
                            value("f211") { result.obj02.f22.f211 = getValueAsString(result.obj02.f22.f211) }
                            value("f221") { result.obj02.f22.f221 = getValueAsInt(result.obj02.f22.f221) }
                        }
                    }
                    array("arrObj01") {
                        objct {
                            onStartRead { result.arrObj01.list += _arrObj01.ObjVal() }
                            value("f31") { result.arrObj01.list.last().f31 = getValueAsString(result.arrObj01.default.f31) }
                            value("f32") { result.arrObj01.list.last().f32 = getValueAsInt(result.arrObj01.default.f32) }
                        }
                    }
                    array("arrVal01") {
                        value { result.arrVal01.list += getValueAsInt(result.arrVal01.default) }
                    }
                    array("arrArrVal01") {
                        array {
                            value { result.arrArrVal01.list += getValueAsInt(result.arrArrVal01.default) }
                            onFinishRead { result.arrArrVal01.rowCounts.add(result.arrArrVal01.list.size) }
                        }
                    }
                    array("arrArrObj01") {
                        array {
                            objct {
                                onStartRead { result.arrArrObj01.list += _arrArrObj01.ObjVal() }
                                value("f41") { result.arrArrObj01.list.last().f41 = getValueAsString(result.arrArrObj01.default.f41) }
                                objct("f42") {
                                    value("f411") { result.arrArrObj01.list.last().f42.f411 = getValueAsString(result.arrArrObj01.default.f42.f411) }
                                    value("f421") { result.arrArrObj01.list.last().f42.f421 = getValueAsInt(result.arrArrObj01.default.f42.f421) }
                                    array("FarrArrVal01") {
                                        array {
                                            value { result.arrArrObj01.list.last().f42.FarrArrVal01.list += getValueAsInt(result.arrArrObj01.list.last().f42.FarrArrVal01.default) }
                                            onFinishRead { result.arrArrObj01.list.last().f42.FarrArrVal01.rowCounts.add(result.arrArrObj01.list.last().f42.FarrArrVal01.list.size) }
                                        }
                                    }
                                }
                            }
                            onFinishRead { result.arrArrObj01.rowCounts.add(result.arrArrObj01.list.size) }
                        }
                    }
                }
            }
            return result
        }
    }
}
