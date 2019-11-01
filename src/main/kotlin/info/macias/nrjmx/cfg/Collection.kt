package info.macias.nrjmx.cfg

import com.fasterxml.jackson.annotation.JsonProperty

class Collection {
    var collect: Array<Collect>? = null
}

class Collect {
    var domain: String? = null

    @JsonProperty("event_type")
    var eventType: String? = null

    // Key: bean query
    var beans: Map<String, Bean>? = null


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
    fun attributesMap() : Map<String,String> {
        val map = HashMap<String, String>()
        for (a in attributes?: emptyList()) {
            when(a) {
                is String -> map[a] = defaultAttributeType
                is Map<*,*> -> {
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