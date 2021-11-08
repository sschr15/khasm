package net.khasm.init;

import com.google.gson.JsonArray;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class KhasmInitializerFinder {
    public static void main(String[] args) throws Throwable {
        Map<String, String> classToSuperClassMap = ClasspathInspector.getClasses();
        Map<String, List<String>> classToSubclassesMap = new HashMap<>(new HashSet<>(classToSuperClassMap.values()).size());
        JsonArray initializers = new JsonArray();
        for (Map.Entry<String, String> entry : classToSuperClassMap.entrySet()) {
            String className = entry.getKey();
            String superClassName = entry.getValue();
            if (superClassName != null) {
                List<String> subclasses = classToSubclassesMap.computeIfAbsent(superClassName, k -> new ArrayList<>());
                subclasses.add(className);
            }
        }

        List<String> initializerClasses = classToSubclassesMap.get("net/khasm/KhasmInitializer");
        // if initializerClasses is null, it means that problems occurred and exceptions should be thrown
        addInitializers(initializers, initializerClasses, classToSubclassesMap);

        Path outputPath = Path.of(args[0]);
        Files.write(outputPath, initializers.toString().getBytes());
    }

    private static void addInitializers(JsonArray initializers, List<String> initializerClasses, Map<String, List<String>> classToSubclassesMap) {
        for (String initializerClass : initializerClasses) {
            initializers.add(initializerClass);
            if (classToSubclassesMap.containsKey(initializerClass)) {
                addInitializers(initializers, classToSubclassesMap.get(initializerClass), classToSubclassesMap);
            }
        }
    }
}
