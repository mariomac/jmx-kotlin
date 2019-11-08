package info.macias.nrjmx.metrics

import org.junit.Assert.assertEquals
import kotlin.test.Test

class MetricsTest {
    @Test
    fun testMetrics() {

        val defs = BeanMetricsDefinition()
        defs.register("theGauge", "gauge")
        defs.register("theRate", "rate")
        defs.register("theDelta", "delta")

        val metrics = Metrics()
        metrics.define("query-1", defs)

        val bms = metrics.forBean("query-1", "myBean")
        bms.process("theGauge", 0)
        bms.process("theRate", 0)
        bms.process("theDelta", 0)

        val bms2 = metrics.forBean("query-1", "myBean2")
        bms2.process("theGauge", 0)
        bms2.process("theRate", 0)
        bms2.process("theDelta", 0)

        Thread.sleep(500)

        assertEquals(200.0, bms.process("theRate", 100) as Double, 10.0)
        assertEquals(600.0, bms2.process("theRate", 300) as Double, 10.0)
        assertEquals(100, bms.process("theGauge", 100))
        assertEquals(100L, bms.process("theDelta", 100))
        assertEquals(123, bms2.process("theGauge", 123))
        assertEquals(432L, bms2.process("theDelta", 432))
    }
}