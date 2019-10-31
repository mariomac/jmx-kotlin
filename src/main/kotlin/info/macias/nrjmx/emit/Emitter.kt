/*
 * Fair Source License, version 0.9
 * Copyright (C) 2019 Mario Macias
 * Licensor: Mario Macias
 * Software: JMX metrics fetcher
 * Use Limitation: 25 users
 */
package info.macias.nrjmx.emit

import com.fasterxml.jackson.databind.ObjectMapper
import info.macias.nrjmx.connect.JMXResult
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking

suspend fun StdoutJSONEmitter(receiver: ReceiveChannel<JMXResult>,
                              idAttributes: List<IdAttribute>) = runBlocking {
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
    ObjectMapper().writeValue(System.out, i)
}

