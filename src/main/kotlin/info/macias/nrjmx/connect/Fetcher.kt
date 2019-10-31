/*
 * Fair Source License, version 0.9
 * Copyright (C) 2019 Mario Macias
 * Licensor: Mario Macias
 * Software: JMX metrics fetcher
 * Use Limitation: 25 users
 */
package info.macias.nrjmx.connect

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.slf4j.LoggerFactory
import javax.management.Attribute
import javax.management.ObjectInstance
import javax.management.ObjectName

interface JMXFetcher {
    suspend fun query(q: JMXQuery): ReceiveChannel<JMXResult>
}

data class JMXQuery(val domain: String, val host: String, val eventType: String, val query: String, val attributes: Map<String, String>) // keys: name, val: type
data class JMXResult(val query: JMXQuery, val attributes: List<Attribute>, val bean: ObjectInstance)

class AsyncFetcher(private val connection: ConnectionFactory) : JMXFetcher {
    companion object {
        private val log = LoggerFactory.getLogger(AsyncFetcher::class.java)
        private const val channelBuffer: Int = 10
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, ex ->
        log.error("Can't query MBeans info: {}", ex.message)
        log.debug("queryng mbeans info", ex)
    }

    override suspend fun query(q: JMXQuery): ReceiveChannel<JMXResult> = coroutineScope jmxQuery@{
        var channel = Channel<JMXResult>(channelBuffer)
        val beanName = "${q.domain}:${q.query}"
        val queryObject = ObjectName(beanName)
        log.debug("Querying for {}", beanName)

        launch(exceptionHandler) {
            queryBeans(queryObject).map { bean ->
                async(exceptionHandler) {
                    log.debug("{} result: {}", beanName, bean)
                    val attrs = queryAttributes(bean.objectName, q.attributes.keys.toTypedArray())

                    channel.send(JMXResult(q, attrs, bean))
                }
            }.awaitAll()
            channel.close()
        }
        return@jmxQuery channel
    }

    private fun queryBeans(queryObject: ObjectName): Set<ObjectInstance> =
            connection.get().queryMBeans(queryObject, null)

    private fun queryAttributes(on: ObjectName, attributes: Array<String>): List<Attribute> =
            connection.get().getAttributes(on, attributes).asList()

}