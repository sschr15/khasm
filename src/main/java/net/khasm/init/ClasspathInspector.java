package net.khasm.init;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Find classes in the classpath (reads JARs and classpath folders).
 *
 * This has been modified by sschr15 to use Paths instead of File objects for easier jar-in-jar support.
 *
 * @author P&aring;l Brattberg, brattberg@gmail.com
 * @see <a href=https://gist.github.com/pal/110024/8a845866d3aad6865a4d2cad2b3eff112b61b1d5>Source</a>
 */
public class ClasspathInspector {
    private static final Map<String, String> classToSuperClass = new HashMap<>();

    public static Map<String, String> getClasses() throws Throwable {
        classToSuperClass.clear();
        loadJavaClasses();
        List<Path> classLocations = getClassLocationsForCurrentClasspath();
        for (Path file : classLocations) {
            getClassesFromPath(file);
        }
        return classToSuperClass;
    }

    private static void getClassesFromPath(Path path) throws Throwable {
        if (Files.isDirectory(path)) {
            getClassesFromDirectory(path);
        } else {
            getClassesFromJarFile(path);
        }
    }

    private static void getClassesFromJarFile(Path path) throws Throwable {
        if (Files.isReadable(path)) {
            Path rootOfJar = FileSystems.newFileSystem(path).getPath("/");
            for (Path filePath1 : Files.walk(rootOfJar).toList()) {
                if (!Files.isDirectory(filePath1) && filePath1.getFileName().toString().endsWith(".class")) {
                    ClassNode classNode = new ClassNode();
                    ClassReader classReader;
                    try {
                        classReader = new ClassReader(Files.readAllBytes(filePath1));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    classReader.accept(classNode, 0);
                    classToSuperClass.put(classNode.name, classNode.superName);
                }
            }
            // Because Fabric adds jar files in META-INF/jars to the classpath at runtime, we need to check there as well.
            if (Files.exists(rootOfJar.resolve("META-INF")) && Files.exists(rootOfJar.resolve("META-INF").resolve("jars"))) {
                for (Path filePath : Files.newDirectoryStream(rootOfJar.resolve("META-INF/jars"))) {
                    if (!Files.isDirectory(filePath) && filePath.getFileName().toString().endsWith("jar")) {
                        getClassesFromJarFile(filePath);
                    }
                }
            }
        }
    }

    private static void getClassesFromDirectory(Path path) throws Throwable {
        // get jar files from top-level directory
        List<Path> jarFiles = listFiles(path, entry -> entry.endsWith(".jar"), false);
        for (Path file : jarFiles) {
            getClassesFromJarFile(file);
        }

        // get all class-files
        List<Path> classFiles = listFiles(path, entry -> entry.endsWith(".class"), true);
        for (Path classfile : classFiles) {
            ClassNode classNode = new ClassNode();
            byte[] classBytes = Files.readAllBytes(classfile);
            ClassReader classReader = new ClassReader(classBytes);
            classReader.accept(classNode, 0);
            classToSuperClass.put(classNode.name, classNode.superName);
        }
    }

    private static List<Path> listFiles(Path directory, DirectoryStream.Filter<Path> filter, boolean recurse) throws IOException {
        List<Path> files = new ArrayList<>();
        // Go over entries
        for (Path entry : Files.newDirectoryStream(directory, filter)) {
            // If there is no filter or the filter accepts the
            // file / directory, add it to the list
            if (filter.accept(entry)) {
                files.add(entry);
            }

            // If the file is a directory and the recurse flag
            // is set, recurse into the directory
            if (recurse && Files.isDirectory(entry)) {
                files.addAll(listFiles(entry, filter, true));
            }
        }

        // Return collection of files
        return files;
    }

    public static List<Path> getClassLocationsForCurrentClasspath() {
        List<Path> urls = new ArrayList<>();
        String javaClassPath = System.getProperty("java.class.path");
        if (javaClassPath != null) {
            for (String path : javaClassPath.split(File.pathSeparator)) {
                urls.add(Path.of(path));
            }
        }
        return urls;
    }

    private static void loadJavaClasses() throws Throwable {
        List<String> missingClasses = new ArrayList<>();

        // Use Java src.zip file to find what Java classes exist
        Path javaSrcZipRoot = FileSystems.newFileSystem(findJavaSourceZip()).getPath("/");
        for (Path filePath : Files.walk(javaSrcZipRoot).toList()) {
            if (!Files.isDirectory(filePath) && filePath.getFileName().toString().endsWith(".java")) {
                String classFileName = filePath.toAbsolutePath().toString().replace(".java", ".class");
                if (classFileName.contains("module-info") || classFileName.contains("package-info")) continue;
                classFileName = classFileName.substring(classFileName.indexOf("/", 1) + 1); // remove module name
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classFileName);
                if (is == null) {
                    // I don't know why this happens, but it does
                    missingClasses.add(filePath.toAbsolutePath().toString());
                    continue;
                }
                byte[] classBytes = Objects.requireNonNull(is).readAllBytes();
                ClassNode classNode = new ClassNode();
                ClassReader classReader = new ClassReader(classBytes);
                classReader.accept(classNode, 0);
                classToSuperClass.put(classNode.name, classNode.superName);
            }
        }
        System.out.println("Missing classes: " + (missingClasses.size() < 50 ? missingClasses : missingClasses.size()));
    }

    private static Path findJavaSourceZip() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        // jdk 8 and similar
        Path jdk8Zip = javaHome.resolve("src.zip");
        // more modern jdk
        Path modernZip = javaHome.resolve("lib").resolve("src.zip");

        if (Files.exists(jdk8Zip)) {
            return jdk8Zip;
        } else if (Files.exists(modernZip)) {
            return modernZip;
        }
        throw new RuntimeException("Could not find Java source zip file");
    }
}
