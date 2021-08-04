package io.holunda.camunda.variable.serializer

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.camunda.bpm.engine.variable.VariableMap
import org.camunda.bpm.engine.variable.Variables
import java.time.Instant
import java.util.*

/**
 * Serialize variables into a map using provided object mapper.
 * @param payload serialization content
 * @param mapper optional mapper, if not provided, Jackson default mapper will be used.
 */
fun serialize(payload: Any, mapper: ObjectMapper): VariableMap {
  return Variables.createVariables().apply {
    this.putAll(mapper.convertValue(payload, object : TypeReference<Map<String, Any>>() {}))
  }
}

/**
 * Deserializes JSON back into variable map.
 */
fun String?.toPayloadVariableMap(objectMapper: ObjectMapper): VariableMap = Variables.createVariables().apply {
  if (this@toPayloadVariableMap != null) {
    putAll(objectMapper.readValue(this@toPayloadVariableMap, object : TypeReference<Map<String, Any>>() {}))
  }
}

/**
 * Serializes payload as JSON.
 */
fun VariableMap.toPayloadJson(objectMapper: ObjectMapper): String =
  objectMapper.writeValueAsString(this)



/**
 * Converts a deep map structure representing the payload into a map of one level keyed by the JSON path and valued by the value.
 * The map might contain primitive types or maps as value.
 * @param limit of levels to convert. Defaults to -1 meaning there is no limit.
 */
fun VariableMap.toJsonPathsWithValues(limit: Int = -1): Map<String, Any> {
  val pathsWithValues: List<MutableMap<String, Any>> = this.entries.map {
    it.toJsonPathWithValue(prefix = "", limit = limit).toMap().toMutableMap()
  }
  return pathsWithValues.reduce { result, memberList -> result.apply { putAll(memberList) } }.toMap()
}


internal fun MutableMap.MutableEntry<String, Any>.toJsonPathWithValue(prefix: String = "", limit: Int = -1): List<Pair<String, Any>> {
  // level limit check
  val currentLevel = prefix.count { ".".contains(it) }
  if (limit != -1 && currentLevel >= limit) {
    return listOf()
  }
  // compose the path key
  val key = if (prefix == "") {
    this.key
  } else {
    "$prefix.${this.key}"
  }
  return if (this.value.isPrimitiveType()) {
    listOf(key to this.value)
  } else if (this.value is Map<*, *>) {
    @Suppress("UNCHECKED_CAST")
    (this.value as Map<String, Any>).toMutableMap().entries.map { it.toJsonPathWithValue(key, limit) }.flatten()
  } else {
    // ignore complex objects
    listOf()
  }
}

internal fun Any.isPrimitiveType(): Boolean {
  return when (this) {
    // TODO: ask Jackson for the supported list of types
    is String, is Boolean, is Number, is Int, is Long, is Float, is Date, is Instant -> true
    else -> false
  }
}


