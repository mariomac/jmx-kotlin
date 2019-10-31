/*
 * Fair Source License, version 0.9
 * Copyright (C) 2019 Mario Macias
 * Licensor: Mario Macias
 * Software: JMX metrics fetcher
 * Use Limitation: 25 users
 */
package info.macias.nrjmx.cfg

import com.fasterxml.jackson.annotation.JsonProperty

class Collection {
    var collect: Array<Collect>? = null
}

class Collect {
    var domain: String? = null

    @JsonProperty("event_type")
    var eventType: String? = null

    var beans: List<Bean>? = null
}

class Bean {
    companion object {
        private val defaultAttributeType = "gauge"
        private val attributeName = "attr"
        private val attributeType = "metric_type"
    }

    var query: String? = null
    var attributes: List<Any>? = null

    // key: name, value: type (rate, gauge, etc...)
    fun attributesMap(): Map<String, String> {
        val map = HashMap<String, String>()
        for (a in attributes ?: emptyList()) {
            when (a) {
                is String -> map[a] = defaultAttributeType
                is Map<*, *> -> {
                    val name = a[attributeName]
                    val type = a[attributeType]
                    if (name == null || type == null) {
                        throw Exception("invalid attribute definition ${a.keys.toTypedArray()}")
                    }
                    map[name.toString()] = type.toString()
                }
            }
        }
        return map
    }
}