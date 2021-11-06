package net.khasm.transform

import com.google.gson.*
import net.khasm.util.classInheritanceTree
import net.khasm.util.gson
import org.objectweb.asm.ClassWriter
import java.lang.reflect.Type

/**
 * A modified version of [ClassWriter] that changes the class loader used for finding common superclasses.
 */
class KhasmClassWriter(flags: Int, private val modifiedClassLoader: ClassLoader) : ClassWriter(flags) {
    override fun getClassLoader(): ClassLoader {
        return modifiedClassLoader
    }

    override fun getCommonSuperClass(type1: String, type2: String): String {
        // classInheritanceTree is a ClassLoadingTree object representing java.lang.Object itself
        val class1 = classInheritanceTree.findClass(type1) ?: classInheritanceTree
        val class2 = classInheritanceTree.findClass(type2) ?: classInheritanceTree
        return getCommonSuperClass(class1, class2).className
    }

    private fun getCommonSuperClass(type1: ClassLoadingTree, type2: ClassLoadingTree): ClassLoadingTree {
        if (type1 == type2) return type1
        if (type1.superClass == type2) return type2
        if (type2.superClass == type1) return type1
        var commonSuperClass = type1
        while (commonSuperClass !in type2.allSuperClasses) {
            commonSuperClass = commonSuperClass.superClass ?: classInheritanceTree // java.lang.Object
        }
        return commonSuperClass
    }
}

class ClassLoadingTree(val className: String, val subclasses: List<ClassLoadingTree>) {
    var superClass: ClassLoadingTree? = null
        private set

    init {
        for (subclass in subclasses) {
            subclass.superClass = this
        }
    }

    val allSuperClasses: List<ClassLoadingTree> by lazy {
            val superClasses = mutableListOf<ClassLoadingTree>()
            var current: ClassLoadingTree? = this
            while (current != null) {
                superClasses.add(current)
                current = current.superClass
            }
            superClasses
        }

    override fun toString(): String {
        return "$className (${subclasses.size} children)"
    }

    fun findClass(className: String): ClassLoadingTree? {
        if (this.className == className) return this
        for (subclass in subclasses) {
            val found = subclass.findClass(className)
            if (found != null) return found
        }
        return null
    }

    companion object Serializer : JsonSerializer<ClassLoadingTree>, JsonDeserializer<ClassLoadingTree> {
        override fun serialize(src: ClassLoadingTree, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
            val json = JsonObject()
            json.addProperty("class", src.className)
            json.add("children", gson.toJsonTree(src.subclasses))
            return json
        }

        override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): ClassLoadingTree {
            val jsonObject = json.asJsonObject
            val className = jsonObject.get("class").asString
            val children = mutableListOf<ClassLoadingTree>()
            jsonObject.get("children").asJsonArray.forEach {
                children.add(gson.fromJson(it, ClassLoadingTree::class.java))
            }
            return ClassLoadingTree(className, children)
        }
    }
}
