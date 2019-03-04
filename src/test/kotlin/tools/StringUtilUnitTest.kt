package tools

import org.junit.Test
import tools.stringUtil.times


class StringUtilUnitTest {

    @Test
    fun `test String times`() {
        println("StringUtilUnitTest.test String times ${KotlinVersion.CURRENT}")
        assertStrTimes(" ", (-1), "")
        assertStrTimes(" ", (0), "")
        assertStrTimes(" ", (1), " ")
        assertStrTimes(" ", (2), "  ")
        assertStrTimes(" ", (3), "   ")

        assertStrTimes("-", (-3), "")
        assertStrTimes("-", (0), "")
        assertStrTimes("-", (3), "---")
        assertStrTimes("-", (4), "----")

        assert(" ".times(-1) == "")
        assert(" ".times(0) == "")
        assert(" ".times(1) == " ")
        assert(" ".times(2) == "  ")
        assert(" ".times(3) == "   ")
    }

    private fun assertStrTimes(template: String, count: Int, expected: String) {
        val resultStr = template.times(count)
        assert(resultStr == expected) { "expected=`$expected` got=`$resultStr`" }
    }

}
