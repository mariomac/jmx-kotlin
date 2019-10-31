/*
 * Fair Source License, version 0.9
 * Copyright (C) 2019 Mario Macias
 * Licensor: Mario Macias
 * Software: JMX metrics fetcher
 * Use Limitation: 25 users
 */
package info.macias.nrjmx.emit

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

private const val entityType = "jmx-domain"

class IntegrationV3 {
    val name = "com.newrelic.jmx"
    @JsonProperty("protocol_version")
    val protocolVersion = "3"
    @JsonProperty("integration_version")
    val integrationVersion = "0.1-kotlin"
    val data = ArrayList<Entity>()

    @JsonIgnore
    val entitiesCache = HashMap<String, Entity>()

    fun entity(name: String, idAttributes: List<IdAttribute>): Entity {
        val ed = EntityData(name, entityType, idAttributes)
        return entitiesCache.getOrPut(ed.compoundName, {
            val entity = Entity(ed)
            data.add(entity)
            entity
        })
    }
}

@JsonIgnoreProperties("compoundName")
class EntityData {
    // defined this way because jackson otherwise ignores the naming override
    constructor(name: String, type: String, idAttributes: List<IdAttribute>) {
        this.name = name
        this.type = type
        this.idAttributes = idAttributes
    }

    val name: String
    val type: String
    @JsonProperty("id_attributes")
    val idAttributes: List<IdAttribute>
    val compoundName: String
        get() = "$type:$name"
}

class IdAttribute {
    // defined this way because jackson otherwise ignores the naming override
    constructor(key: String, value: String) {
        this.key = key
        this.value = value
    }

    @JsonProperty("Key")
    val key: String
    @JsonProperty("Value")
    val value: String
}


class Entity(val entity: EntityData) {

    var metrics = ArrayList<Map<String, Any>>()

    fun addMetrics(eventType: String): MutableMap<String, Any> {
        val metricsSet = HashMap<String, Any>()
        metrics.add(metricsSet)
        metricsSet["event_type"] = eventType

        metricsSet["entityName"] = "${entity.type}:${entity.name}"
        return metricsSet
    }
}