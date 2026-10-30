package io.github.itech_framework.core.utils;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PackageClassesLoader {
    private static final Logger logger = LogManager.getLogger(PackageClassesLoader.class);

    private static final String CLASS_SUFFIX = ".class";

    public static List<Class<?>> findAllClasses(String basePackage, Class<?> clazz) throws IOException {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalArgumentException("Base package must not be null or blank");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("Class used to resolve the class loader must not be null");
        }

        ClassLoader classLoader = clazz.getClassLoader();
        if (classLoader == null) {
            throw new IOException("Class " + clazz.getName() + " has no class loader; cannot scan package " + basePackage);
        }

        String path = basePackage.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<Class<?>> classes = new ArrayList<>();

        logger.debug("Scanning package: {}", basePackage);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            switch (protocol) {
                case "file", "jrt" -> classes.addAll(findClassesWithNio(resource, basePackage, classLoader));
                case "jar" -> classes.addAll(findClassesInJar(resource, path, classLoader));
                default -> throw new IOException("Unsupported resource protocol '" + protocol
                        + "' while scanning package " + basePackage + ": " + resource);
            }
        }

        logger.info("Found {} classes in package {}", classes.size(), basePackage);
        return classes;
    }

    private static boolean isScannable(String relativeClassFile) {
        if (!relativeClassFile.endsWith(CLASS_SUFFIX)) return false;

        String simple = relativeClassFile.substring(relativeClassFile.lastIndexOf('/') + 1);
        if (simple.equals("module-info.class") || simple.equals("package-info.class")) return false;

        int dollar = simple.indexOf('$');
        while (dollar >= 0) {
            if (dollar + 1 < simple.length() && Character.isDigit(simple.charAt(dollar + 1))) {
                return false;
            }
            dollar = simple.indexOf('$', dollar + 1);
        }
        return true;
    }

    private static String toClassName(String classFilePath) {
        return classFilePath
                .substring(0, classFilePath.length() - CLASS_SUFFIX.length())
                .replace('\\', '.')
                .replace('/', '.');
    }

    private static List<Class<?>> findClassesWithNio(URL resource, String basePackage, ClassLoader classLoader) throws IOException {
        List<Class<?>> classes = new ArrayList<>();

        Path root;
        try {
            root = Paths.get(resource.toURI());
        } catch (URISyntaxException | RuntimeException e) {
            throw new IOException("Invalid resource location while scanning package " + basePackage + ": " + resource, e);
        }

        if (!Files.isDirectory(root)) {
            throw new IOException("Resource for package " + basePackage + " is not a directory: " + resource);
        }

        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(Files::isRegularFile).toList();
        } catch (IOException | RuntimeException e) {
            throw new IOException("Failed to walk " + resource + " while scanning package " + basePackage, e);
        }

        for (Path file : files) {
            String relative = root.relativize(file).toString().replace('\\', '/');
            if (!isScannable(relative)) continue;

            classes.add(loadClass(basePackage + "." + toClassName(relative), classLoader));
        }

        logger.debug("Found {} classes via {} in {}", classes.size(), resource.getProtocol(), basePackage);
        return classes;
    }

    private static List<Class<?>> findClassesInJar(URL resource, String path, ClassLoader classLoader) throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        String prefix = path.endsWith("/") ? path : path + "/";

        JarURLConnection connection;
        try {
            connection = (JarURLConnection) resource.openConnection();
            connection.setUseCaches(false);
        } catch (IOException | ClassCastException e) {
            throw new IOException("Could not open JAR resource: " + resource, e);
        }

        try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(prefix)) continue;
                if (!isScannable(name)) continue;

                classes.add(loadClass(toClassName(name), classLoader));
            }
        } catch (IOException e) {
            throw new IOException("Failed to read JAR resource: " + resource, e);
        }

        logger.debug("Found {} classes in JAR resource {}", classes.size(), resource);
        return classes;
    }

    private static Class<?> loadClass(String className, ClassLoader classLoader) throws IOException {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IOException("Could not load class " + className, e);
        }
    }
}