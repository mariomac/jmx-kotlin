package info.macias.nrjmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import info.macias.kaconf.ConfiguratorBuilder
import info.macias.kaconf.Property
import info.macias.nrjmx.cfg.Collection
import info.macias.nrjmx.connect.AsyncFetcher
import info.macias.nrjmx.connect.JMXConnectionFactory
import info.macias.nrjmx.connect.JMXQuery
import info.macias.nrjmx.connect.JMXResult
import info.macias.nrjmx.emit.IdAttribute
import info.macias.nrjmx.emit.StdoutJSONEmitter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import java.io.File
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
        val collectFile = ObjectMapper(YAMLFactory()).findAndRegisterModules()
                .readValue(File(config.collectFile), Collection::class.java)

        if (collectFile.collect == null) {
            log.error("empty collect file")
            exitProcess(-1)
        }

        log.debug("starting fetching")

        val (emitterChannel, timeout) = queryAllDomains(collectFile)

        log.debug("starting collection")

        StdoutJSONEmitter(emitterChannel, listOf(
                IdAttribute("host", config.host),
                IdAttribute("port", config.port.toString())))

        timeout.cancel("task finished. No timeout")
    }

    // Collect beans and attributes and asynchronously processes them to send them to the
    // emitter channel
    // implements a timeout
    private fun CoroutineScope.queryAllDomains(collectFile: Collection): Pair<Channel<JMXResult>, Job> {
        val emitterChannel = Channel<JMXResult>()

        val queryJob = async {
            collectFile.collect!! // todo: log exceptions
                    // filters all the invalid domains
                    .filter { it.domain != null && it.beans != null && it.eventType != null } // todo: log message
                    // queries all the attributes for each domain, and emits them
                    .map { dom ->
                        dom.beans!!.map { bean ->
                            fetcher.query(JMXQuery(domain = dom.domain!!,
                                    eventType = dom.eventType!!,
                                    query = bean.query!!,
                                    host = config.host,
                                    attributes = bean.attributesMap()))
                        }.forEach { resultChannel ->
                            for (jmxResult in resultChannel) {
                                emitterChannel.send(jmxResult)
                            }
                        }
                    }
        }
        queryJob.invokeOnCompletion { emitterChannel.close() }

        // implement timeout
        val timeout = launch {
            delay(config.timeoutSeconds * 1000)
            log.warn("queries did not complete after {} seconds." +
                    " Canceling and sending only the received data", config.timeoutSeconds)
            queryJob.cancel("timeout")
            emitterChannel.close()
        }
        return Pair(emitterChannel, timeout)
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

