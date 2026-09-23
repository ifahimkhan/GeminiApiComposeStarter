package com.fahim.geminiApiComposeStarter.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MathEngineToolTest {

    private lateinit var tool: MathEngineTool

    @Before
    fun setup() {
        tool = MathEngineTool()
    }

    @Test
    fun `evaluates basic arithmetic and order of operations`() {
        val res1 = tool.execute("2 + 3 * 4")
        assertTrue(res1.isSuccess)
        assertEquals("14", res1.result)

        val res2 = tool.execute("(2 + 3) * 4")
        assertTrue(res2.isSuccess)
        assertEquals("20", res2.result)

        val res3 = tool.execute("2^3 + 10")
        assertTrue(res3.isSuccess)
        assertEquals("18", res3.result)
    }

    @Test
    fun `evaluates mathematical functions`() {
        val res1 = tool.execute("sqrt(144)")
        assertTrue(res1.isSuccess)
        assertEquals("12", res1.result)

        val res2 = tool.execute("abs(-45.5)")
        assertTrue(res2.isSuccess)
        assertEquals("45.5", res2.result)

        val res3 = tool.execute("log(1000)")
        assertTrue(res3.isSuccess)
        assertEquals("3", res3.result)
    }

    @Test
    fun `evaluates percentages correctly`() {
        val res = tool.execute("15% of 200")
        assertTrue(res.isSuccess)
        assertEquals("30", res.result)
    }

    @Test
    fun `evaluates statistical calculations`() {
        val meanRes = tool.execute("mean of 10, 20, 30, 40, 50")
        assertTrue(meanRes.isSuccess)
        assertEquals("30", meanRes.result)

        val medianRes = tool.execute("median of 12, 5, 20, 8, 3")
        assertTrue(medianRes.isSuccess)
        assertEquals("8", medianRes.result)

        val stdRes = tool.execute("standard deviation of 10, 10, 10")
        assertTrue(stdRes.isSuccess)
        assertEquals("0", stdRes.result)
    }

    @Test
    fun `evaluates unit conversions`() {
        val tempRes = tool.execute("convert 100 celsius to fahrenheit")
        assertTrue(tempRes.isSuccess)
        assertEquals("212 fahrenheit", tempRes.result)

        val distRes = tool.execute("convert 5 km to miles")
        assertTrue(distRes.isSuccess)
        assertTrue(distRes.result.startsWith("3.1069"))

        val dataRes = tool.execute("convert 2 gb to mb")
        assertTrue(dataRes.isSuccess)
        assertEquals("2048 mb", dataRes.result)
    }
}
