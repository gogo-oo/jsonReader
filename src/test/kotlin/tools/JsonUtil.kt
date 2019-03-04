package tools

import com.fasterxml.jackson.databind.ObjectMapper


class JsonUtil {

    companion object {
        fun toJsonPrettyStr(str: String): String {
            val mapper = ObjectMapper()
            val result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(str, Any::class.java))
            return result ?: ""
        }
    }
}
