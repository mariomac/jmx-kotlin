package info.macias.nrjmx.metrics

private const val nameDelta = "delta"
private const val nameGauge = "gauge"
private const val nameAttr = "attribute"
private const val nameRate = "rate"

class MetricException(msg: String) : Exception(msg)


class Metrics {
    // stores the structures of the metrics for each query type
    private val definitions = HashMap<String, BeanMetricsDefinition>() //key: query
    // stores the metrics for each bean, based on their query type
    private val beans = HashMap<String, BeanMetrics>()

    fun define(query: String, defs: BeanMetricsDefinition) {
        definitions[query] = defs
    }

    // todo: store last bean access and invalidate old metrics
    fun forBean(query: String, bean: String): BeanMetrics {
        var b = beans[bean]
        if (b == null) {
            val q = definitions[query] ?: throw MetricException("non-registered query: $query")
            b = q.instantiate()
            beans[bean] = b
        }
        return b
    }

}

// Uses a cloned registry, with all the metrics set to zero
class BeanMetrics internal constructor(private val registry: BeanMetricsDefinition) {
    fun process(name: String, value: Any): Any {
        // if the metric is not registered, we return it as a simple gauge value
        val m = registry.metrics[name]
                ?: return value // TODO: log warning?

        return m.process(value)
    }
}


class BeanMetricsDefinition {
    internal val metrics = HashMap<String, Metric>()

    fun register(name: String, type: String) {
        metrics[name] = when (name.toLowerCase()) {
            nameRate -> RateMetric()
            nameGauge, nameAttr -> SpotMetric(name)
            nameDelta -> DeltaMetric()
            else -> throw MetricException("invalid metric type: $type")
        }
    }

    // copies all the metrics, with unset values
    internal fun instantiate() = BeanMetrics(
            BeanMetricsDefinition().let {
                metrics.forEach { (name, metric) ->
                    it.register(name, metric.type)
                }
                it
            })
}

internal abstract class Metric(val type: String) {
    abstract fun process(value: Any): Any
}

// gauges or attributes
internal class SpotMetric(type: String) : Metric(type) {
    override fun process(value: Any) = value
}

internal class RateMetric(val now: () -> Long = System::nanoTime) : Metric(nameRate) {
    private var lastValue: Any? = null
    private var lastTime = now()
    override fun process(value: Any): Any {
        val lval = lastValue
        val ltime = lastTime
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

internal class DeltaMetric : Metric(nameDelta) {
    private var lastValue: Any? = null
    override fun process(value: Any): Any {
        val lval = lastValue
        lastValue = value
        if (lval == null) {
            return 0
        }
        return when (value) {
            is Int, Long, Short, Byte -> (value as Number).toLong() - (lval as Number).toLong()
            is Double, Float -> (value as Number).toDouble() - (lval as Number).toFloat()
            else -> throw MetricException("value $value expected to be numeric")
        }
    }
}
