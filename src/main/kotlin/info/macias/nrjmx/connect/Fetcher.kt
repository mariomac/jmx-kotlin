package info.macias.nrjmx.connect

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import javax.management.Attribute
import javax.management.ObjectInstance
import javax.management.ObjectName

data class JMXQuery(val domain: String, val host: String, val eventType: String, val query: String, val attributes: Map<String, String>) // keys: name, val: type
data class JMXResult(val query: JMXQuery, val attributes: List<Attribute>, val bean: ObjectInstance)

class AsyncFetcher(private val connection: ConnectionFactory) {
    companion object {
        private val log = LoggerFactory.getLogger(AsyncFetcher::class.java)
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, ex ->
        log.error("Can't query MBeans info: {}", ex.message)
        log.debug("queryng mbeans info", ex)
    }


    suspend fun query(q: JMXQuery): List<Deferred<JMXResult>> = coroutineScope {
        val beanName = "${q.domain}:${q.query}"
        val queryObject = ObjectName(beanName)
        log.debug("Querying for {}", beanName)

        queryBeans(queryObject).map { bean ->
            async(exceptionHandler) {
                val conn = connection.get()
                log.debug("{} result: {}", beanName, bean)
                val attrs = conn.getAttributes(bean.objectName, q.attributes.keys.toTypedArray())
                        .asList()

                JMXResult(q, attrs, bean)
            }
        }.toList()
    }

    private fun queryBeans(queryObject: ObjectName): Set<ObjectInstance> =
            connection.get().queryMBeans(queryObject, null)

}