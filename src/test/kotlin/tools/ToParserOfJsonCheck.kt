import com.fasterxml.jackson.core.JsonFactory
import org.junit.Test
import tools.JsonUtil
import tools.ToParserOfJson
import tools.ToParserOfJsonCheckRes
import tools.jsonDescription.Supported
import tools.jsonDescription.readJsonItemDescription
import java.io.File

class ToParserOfJsonCheck {

    val jsonStr00 = """
{

  "strVal01": "val01",
  "strVal02": "val02",
  "intVal01": 1001,
  "strVal01": "val01b",
  "obj01":{
    "f01":"val015",
    "f02":1025
  },
    "obj02":{
    "f21":"val027",
    "f22":{
        "f211":"val015",
        "f221":1025
    }
  },
  "arrObj01": [{
    "f31":"val015",
    "f32":1025
  }],
  "arrVal01": [1001]
  ,
  "arrArrVal01": [
  [1001,1002,1003,1004],
  [2001,2002,2003,2004],
  [3001,3002,3003,3004]
  ]  ,
  "arrArrObj01": [
  [{
    "f41":"val015",
    "f42":{
        "f411":"val015",
        "f421":1025,
        "FarrArrVal01": [
  [1001,1002,1003,1004],
  [2001,2002,2003,2004],
  [3001,3002,3003,3004]
  ]
    }
  },
  {
    "f41":"val015",
    "f42":{

    }
  }],
    [{
    "f41":"val115"

  },
  {

  }],
    [{
    "f41":"val215",
    "f42":{
        "f411":"val215"

    }
  },
  {
    "f42":{
        "f421":1225
    }
  }]


  ]

}"""

    val jsonStr01 = """
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

  "strVal02": "val02",
  "intVal01": 1001
 }
}"""

    @Test
    fun `check 01 ToJsonParser`() {
        println("ToParserOfJsonCheck.check 01 ToParserOfJson ${KotlinVersion.CURRENT}")

        val descJsonStr01 = readJsonItemDescription(JsonFactory().createParser(jsonStr00))
        println(descJsonStr01)
        println(JsonUtil.toJsonPrettyStr(descJsonStr01.toString()))

        if (descJsonStr01 is Supported) {
            val outRes = ToParserOfJson.toJsonParser(descJsonStr01)
            println(outRes.outFirst)
            println(outRes.outVal__)
            println(outRes.outRead_)

            val className = "ToParserOfJsonCheckRes"
            val outFile = File("tmp/$className.kt").apply { parentFile.mkdirs() }
            val outStr = ToParserOfJson.toJsonParserWithCompanion(descJsonStr01, className)
            outFile.writeText(outStr)
        }

        val res = ToParserOfJsonCheckRes.read(JsonFactory().createParser(jsonStr00))
        println("ToParserOfJsonCheck.check 01 ToJsonParse ")
    }

    @Test
    fun `check 02 ToJsonParser`() {
        println("JsonDescriptionCheck.`check 02 Json Description` ${KotlinVersion.CURRENT}")
        var i = 0
        for (inFile in File("../tmp/in01").listFiles() ?: emptyArray()) {
            println(inFile)
            val outParentFile = File("tmp/out01kt").apply { mkdirs() }
            val description = readJsonItemDescription(JsonFactory().createParser(inFile))
            if (description is Supported) {
                val className = "ToParserOfJsonCheckRes$i"
                val outFile = File(outParentFile, "/$className.kt")
                val outStr = """//${inFile.absolutePath}
${ToParserOfJson.toJsonParserWithCompanion(description, className)}
"""
                outFile.writeText(outStr)
                i++
            }

        }
    }
}
