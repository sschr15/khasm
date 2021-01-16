package net.khasm.util

import net.fabricmc.loader.api.FabricLoader

private val classCache: HashMap<String, String> = hashMapOf()
private val fieldCache: HashMap<String, String> = hashMapOf()
private val methodCache: HashMap<String, String> = hashMapOf()

fun mapClass(name: String): String {
    return classCache.computeIfAbsent(name) { FabricLoader.getInstance().mappingResolver.mapClassName("intermediary", name) }
}

fun mapField(owner: String, name: String, desc: String): String {
    return fieldCache.computeIfAbsent(owner+name+desc) { FabricLoader.getInstance().mappingResolver.mapFieldName("intermediary", owner, name, desc) }
}

fun mapMethod(owner: String, name: String, desc: String): String {
    return methodCache.computeIfAbsent(owner+name+desc) { FabricLoader.getInstance().mappingResolver.mapMethodName("intermediary", owner, name, desc) }
}
