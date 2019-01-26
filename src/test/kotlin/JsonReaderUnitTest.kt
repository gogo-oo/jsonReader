import com.fasterxml.jackson.core.JsonFactory
import org.junit.Test

//    testImplementation 'com.fasterxml.jackson.core:jackson-core:2.9.8'
//
//        https://github.com/FasterXML/jackson-core#general
//        val factory = JsonFactory()
//        // configure, if necessary:
//        factory.enable(JsonParser.Feature.ALLOW_COMMENTS)

class JsonReaderUnitTest {

    @Test
    fun `check Json Reader Object`() {

        println("JsonReaderUnitTest.check Json Reader Object ${KotlinVersion.CURRENT}")

        val str = """
{
"rootObj":{
  "strVal01": "val01",
  "obj01":{
    "f01":"val015",
    "f02":1025
  },
  "obj02":{
    "f01":"val027",
    "f02":2027
  },
  "strArr01": [
    120,
    110,
    130
  ],
  "objArr01": [
      {
        "f01":"oval012",
        "f02":100012
      },
      {
        "f01":"oval022",
        "f02":200022
      }
  ],
  "arrArr01": [
      [
        "oval012",
        "1100012"
      ],
      [
        "oval022",
        "1200022"
      ]
  ],
  "strVal02": "val02",
  "intVal01": 1001
 }
}"""

        data class TempObject(
                var strVal01: String = "",
                var strVal02: String = "",
                var intVal01: Int = -1,
                var f01: String = "",

                val arr01: MutableList<String> = mutableListOf(),
                val arr02: MutableList<Pair<String, Int>> = mutableListOf()
        )

        JsonReader.readObject(JsonFactory().createParser(str)) {
            objct("rootObj") {
                //            value("rootObj") {
//                JsonReader.readObject(this) {

                val to = TempObject()
                var f02: Int = -1
                value("strVal01") { to.strVal01 = getValueAsString("") }
                array("strArr01") { value { to.arr01.add(getValueAsString("")) } }
                array("objArr01") {
                    var f01: String = ""
                    var f02: Int = -1
                    objct {
                        value("f01") { f01 = getValueAsString("") }
                        value("f02") { f02 = getValueAsInt(-1) }
                        onFinishRead { to.arr02.add(f01 to f02) }
                    }
                }
                array("arrArr01") {
                    var f02 = ""
                    array {
                        value { f02 = getValueAsString("") }
                        onFinishRead { println("ExampleUnitTest.onFinishRead arr $f02") }
                    }
                }
                value("strVal02") { to.strVal02 = getValueAsString("") }
                value("intVal01") { to.intVal01 = getValueAsInt(-1) }
////            objct("obj02") {
//            objct("obj01") {
//                value("f01") { to.f01 = getValueAsString("") }
//                value("f02") { f02 = getValueAsInt(-1) }
//                onStartRead { println("ExampleUnitTest.onStartRead $it") }
//                onFinishRead { println("ExampleUnitTest.onFinishRead $it") }
//            }
                value("obj01") {
                    JsonReader.readObject(this) {
                        value("f01") { to.f01 = "jr-" + getValueAsString("") }
                        value("f02") { f02 = 10_000_000 + getValueAsInt(-1) }
                    }
                }
                onFinishRead { println("ExampleUnitTest.onFinishRead $to $f02") }
            }
//            }
        }
    }

    @Test
    fun `check Json Reader Array`() {
        val strArr = """[
      [
        "oval012",
        "1100012"
      ],
      [
        "oval022",
        "1200022"
      ]
  ]"""
        val strObjArr = """[
      [
          {
            "f01":"oval1012",
            "f02":100012
          },
          {
            "f01":"oval2012",
            "f02":100012
          },
          {
            "f01":"oval3022",
            "f02":200022
          }
      ],
      [
          {
            "f01":"oval012",
            "f02":100012
          },
          {
            "f01":"oval022",
            "f02":200022
          }
      ]
  ]"""
        JsonReader.readArray(JsonFactory().createParser(strArr)) {
            val res = mutableListOf<MutableList<String>>()
            array {
                onStartRead { res.add(mutableListOf()) }
                value { res.last().add(getValueAsString("")) }
            }
            onFinishRead { println("ExampleUnitTest.first cool entry point 2 $res") }
        }
        JsonReader.readArray(JsonFactory().createParser(strObjArr)) {
            val res = mutableListOf<MutableList<Pair<String, Int>>>()
//            array {
            value {
                JsonReader.readArray(this) {
                    onStartRead { res.add(mutableListOf()) }
                    var f01 = "";
                    var f02 = -1
                    objct {
                        value("f01") { f01 = getValueAsString("") }
                        value("f02") { f02 = getValueAsInt(-1) }
//                        onStartRead { println("ExampleUnitTest.onStartRead $it") }
                        onFinishRead {
                            //                            println("ExampleUnitTest.onFinishRead $it")
                            res.last().add(f01 to f02)
                        }
                    }
                }
            }
            onFinishRead { println("ExampleUnitTest.first cool entry point 3 $res") }
        }
    }
}
