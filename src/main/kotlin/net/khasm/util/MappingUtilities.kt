package net.khasm.util

import net.fabricmc.loader.api.FabricLoader

private val classCache = mutableMapOf<String, String>()
private val fieldCache = mutableMapOf<String, String>()
private val methodCache = mutableMapOf<String, String>()

fun mapClass(name: String): String {
    return classCache.getOrPut(name) { FabricLoader.getInstance().mappingResolver.mapClassName("intermediary", name) }
}

fun mapField(owner: String, name: String, desc: String): String {
    return fieldCache.getOrPut(owner+name+desc) { FabricLoader.getInstance().mappingResolver.mapFieldName("intermediary", owner, name, desc) }
}

fun mapMethod(owner: String, name: String, desc: String): String {
    return methodCache.getOrPut(owner+name+desc) { FabricLoader.getInstance().mappingResolver.mapMethodName("intermediary", owner, name, desc) }
}
