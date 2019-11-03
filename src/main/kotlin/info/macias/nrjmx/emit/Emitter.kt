package info.macias.nrjmx.emit

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import info.macias.nrjmx.connect.JMXResult
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

internal val log = LoggerFactory.getLogger("info.macias.nrjmx.emit")
internal val objectMapper = jacksonObjectMapper()

suspend fun StdoutJSONEmitter(receiver: ReceiveChannel<JMXResult>,
                              idAttributes: List<IdAttribute>) = coroutineScope {
    var i = IntegrationV3()
    for (r in receiver) {
        val ms = i.entity(r.query.domain, idAttributes).addMetrics(r.query.eventType)
        ms["bean"] = r.bean.objectName.keyPropertyListString
        ms["domain"] = r.query.domain
        ms["query"] = r.query.query
        ms["displayName"] = r.query.domain
        ms["host"] = r.query.host
        for (attr in r.attributes) {
            ms[attr.name] = attr.value
        }
        for ((key, value) in r.bean.objectName.keyPropertyList) {
            // remove trailing and ceiling quotes, if any
            ms["key:$key"] = value.trim('"', ' ')
        }
    }
    println(objectMapper.writeValueAsString(i))
}

