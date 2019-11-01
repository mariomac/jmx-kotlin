package info.macias.nrjmx.metrics

class BeanMetrics(val bean: String) {
    private val metrics = HashMap<String, Metric>()



}

private abstract class Metric() {
    abstract fun reportedValue(value: Any): Any
}

// gauges or attributes
private class SpotMetric() : Metric() {
    override fun reportedValue(value: Any) = value
}

private class RateMetric(, val now: () -> Long = System::nanoTime) : Metric() {
    private var lastValue: Any? = null
    private var lastTime = now()
    override fun reportedValue(value: Any): Any {
        var lval = lastValue
        var ltime = lastTime
        lastValue = value
        lastTime = now()
        if (lval == null) {
            return 0
        }
        val lvalN = (lval as Number).toDouble()
        val valN = (value as Number).toDouble()
        val secondsEllapsed = (lastTime - ltime).toDouble() / 1_000_000_000.0
        return (valN - lvalN) / secondsEllapsed
    }
}

private class DeltaMetric() : Metric() {
    private var lastValue: Any? = null
    override fun reportedValue(value: Any): Any {
        var lval = lastValue
        lastValue = value
        if (lval == null) {
            return 0
        }
        return when (value) {
            is Int, Long, Short, Byte -> (value as Number).toLong() - (lval as Number).toLong()
            is Double, Float -> (value as Number).toDouble() - (lval as Number).toFloat()
            else -> throw RuntimeException("value $value of metric $name expected to be numeric")
        }
    }
}
