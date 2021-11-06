package net.khasm.init;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class KhasmInheritanceBuilder {
    public static void main(String[] args) throws Throwable {
        Map<String, String> map = ClasspathInspector.getClasses();
        Map<String, JsonObject> jsonMap = new HashMap<>();
        JsonObject object = new JsonObject();
        object.addProperty("class", "java/lang/Object");
        object.add("children", new JsonArray());
        jsonMap.put("java/lang/Object", object);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String className = entry.getKey();
            String superClassName = entry.getValue();
            JsonObject jsonObject = jsonMap.get(className);
            if (jsonObject == null) {
                jsonObject = new JsonObject();
                jsonObject.addProperty("class", className);
                jsonObject.add("children", new JsonArray());
                jsonMap.put(className, jsonObject);
            }
            if (superClassName != null) {
                JsonObject superJsonObject = jsonMap.computeIfAbsent(superClassName, s -> {
                    JsonObject superJsonObject1 = new JsonObject();
                    superJsonObject1.addProperty("class", s);
                    superJsonObject1.add("children", new JsonArray());
                    return superJsonObject1;
                });
                superJsonObject.getAsJsonArray("children").add(jsonObject);
            }
        }

        // we have the classpath, so now we just need to write the json to a file
        Path path = Path.of(args[0]);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            // Object should be the root for the inheritance tree
            new GsonBuilder().create().toJson(jsonMap.get("java/lang/Object"), writer);
        }
    }
}
