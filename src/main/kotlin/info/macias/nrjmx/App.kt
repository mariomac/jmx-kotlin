package info.macias.nrjmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import info.macias.kaconf.ConfiguratorBuilder
import info.macias.kaconf.Property
import info.macias.nrjmx.cfg.Collect
import info.macias.nrjmx.cfg.Collection
import info.macias.nrjmx.connect.AsyncFetcher
import info.macias.nrjmx.connect.JMXConnectionFactory
import info.macias.nrjmx.connect.JMXQuery
import info.macias.nrjmx.connect.JMXResult
import info.macias.nrjmx.emit.IdAttribute
import info.macias.nrjmx.emit.StdoutJSONEmitter
import info.macias.nrjmx.metrics.BeanMetricsDefinition
import info.macias.nrjmx.metrics.Metrics
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import java.io.File
import javax.management.Attribute
import kotlin.system.exitProcess

class InputConfig {
    @Property("JMX_HOST")
    var host: String = "localhost"

    @Property("JMX_PORT")
    var port: Int = 7199

    @Property("JMX_URL")
    var url: String = "service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi"

    @Property("COLLECT_FILES")
    var collectFile: String = "tomcat.yaml"

    var timeoutSeconds: Long = 15
}

private val log = LoggerFactory.getLogger("info.macias.nrjmx")

class App(private val config: InputConfig) {
    private val fetcher = AsyncFetcher(JMXConnectionFactory(
            String.format(config.url, config.host, config.port)))

    fun run() = runBlocking {
        // Load collect file
        log.debug("load metrics definition files")
        val collectFile = ObjectMapper(YAMLFactory()).findAndRegisterModules()
                .readValue(File(config.collectFile), Collection::class.java)

        if (collectFile.collect == null) {
            log.error("empty collect file")
            exitProcess(-1)
        }

        val collects = collectFile.collect!! // todo: log exceptions
                // filters all the invalid domains
                .filter { it.domain != null && it.beans != null && it.eventType != null } // todo: log message

        log.debug("registering metrics")
        val metrics = Metrics()
        collects.forEach {
            it.beans!!.forEach { bean ->
                metrics.define(bean.query!!, BeanMetricsDefinition().apply {
                    bean.attributesMap().forEach(::register)
                })
            }
        }

        while (true) {
            fetchLoop(collects, metrics)
            Thread.sleep(5000) // todo: do it well
        }
    }

    private suspend fun CoroutineScope.fetchLoop(collects: List<Collect>, metrics: Metrics) {
        log.debug("starting fetching")

        val (jmxResults, timeout) = queryAllDomains(collects)

        log.debug("starting collection")
        val emitterChannel = Channel<JMXResult>()
        async {
            // processing metrics from the results
            for (r in jmxResults) {
                val processed: List<Attribute>
                metrics.forBean(query = r.query.query,
                        bean = r.bean.objectName.keyPropertyListString).let { processor ->
                    processed = r.attributes.map {
                        Attribute(it.name, processor.process(it.name, it.value))
                    }
                }
                emitterChannel.send(JMXResult(r.query, processed, r.bean))
            }
        }.invokeOnCompletion {
            emitterChannel.close()
        }

        StdoutJSONEmitter(emitterChannel, listOf(
                IdAttribute("host", config.host),
                IdAttribute("port", config.port.toString())))

        timeout.cancel("task finished. No timeout")
    }


    // Collect beans and attributes and asynchronously processes them to send them to the
    // emitter channel
    // implements a timeout
    private fun CoroutineScope.queryAllDomains(collects: Iterable<Collect>): Pair<Channel<JMXResult>, Job> {
        val results = Channel<JMXResult>()
        val queryJob = async {
            collects.map { dom ->
                dom.beans!!.map { bean ->
                    fetcher.query(JMXQuery(domain = dom.domain!!,
                            eventType = dom.eventType!!,
                            query = bean.query!!,
                            host = config.host,
                            attributes = bean.attributesMap()))
                }.forEach { resultChannel ->
                    for (jmxResult in resultChannel) {
                        results.send(jmxResult)
                    }
                }
            }
        }
        queryJob.invokeOnCompletion { results.close() }

        // implement timeout
        val timeout = launch {
            delay(config.timeoutSeconds * 1000)
            log.warn("queries did not complete after {} seconds." +
                    " Canceling and sending only the received data", config.timeoutSeconds)
            queryJob.cancel("timeout")
            results.close()
        }
        return Pair(results, timeout)
    }
}

fun main() {
    // Load config
    val config = InputConfig()
    ConfiguratorBuilder()
            .addSource(System.getenv()).build()
            .configure(config)

    App(config).run()
}

