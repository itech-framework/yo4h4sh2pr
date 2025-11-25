package io.github.itech_framework.core.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PackageClassesLoader {
    private static final Logger logger = LogManager.getLogger(PackageClassesLoader.class);

    public static List<Class<?>> findAllClasses(String basePackage, Class<?> clazz) throws IOException {
        ClassLoader classLoader = clazz.getClassLoader();
        String path = basePackage.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<Class<?>> classes = new ArrayList<>();

        logger.debug("Scanning package: {}", basePackage);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();
            
            if ("file".equals(protocol)) {
                // Development mode - running from file system
                classes.addAll(findClassesInFileSystem(resource, basePackage, classLoader));
            } else if ("jar".equals(protocol)) {
                // Production mode - running from JAR
                classes.addAll(findClassesInJar(resource, basePackage, path, classLoader));
            } else {
                logger.warn("Unsupported resource protocol: {}", protocol);
            }
        }

        logger.info("Found {} classes in package {}", classes.size(), basePackage);
        return classes;
    }

    private static List<Class<?>> findClassesInFileSystem(URL resource, String packageName, ClassLoader classLoader) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            String decodedPath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
            File directory = new File(decodedPath);
            if (directory.exists() && directory.isDirectory()) {
                classes.addAll(findClassesInDirectory(directory, packageName, classLoader));
                logger.debug("Found {} classes in file system", classes.size());
            }
        } catch (Exception e) {
            logger.error("Error processing file system resource: {}", resource, e);
        }
        return classes;
    }

    private static List<Class<?>> findClassesInJar(URL resource, String packageName, String path, ClassLoader classLoader) {
        List<Class<?>> classes = new ArrayList<>();
        
        try {
            JarURLConnection jarConnection = (JarURLConnection) resource.openConnection();
            try (JarFile jar = jarConnection.getJarFile()) {
                Enumeration<JarEntry> entries = jar.entries();
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    
                    // Check if this entry is in our package and is a class file
                    if (entryName.startsWith(path) && entryName.endsWith(".class") && !entryName.contains("$")) {
                        String className = entryName.replace('/', '.')
                                                  .substring(0, entryName.length() - 6); // Remove .class
                        try {
                            Class<?> clazz = classLoader.loadClass(className);
                            classes.add(clazz);
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            logger.warn("Could not load class: {}", className, e);
                        }
                    }
                }
                logger.debug("Found {} classes in JAR", classes.size());
            }
        } catch (IOException e) {
            logger.error("Error processing JAR resource: {}", resource, e);
        }
        
        return classes;
    }

    private static List<Class<?>> findClassesInDirectory(File directory, String packageName, ClassLoader classLoader) {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            logger.warn("Directory does not exist: {}", directory.getAbsolutePath());
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) return classes;

        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    classes.addAll(findClassesInDirectory(file, 
                            packageName + "." + file.getName(), 
                            classLoader));
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' +
                            file.getName().substring(0, file.getName().length() - 6);
                    classes.add(loadClass(className, classLoader));
                }
            } catch (Exception e) {
                logger.warn("Skipping file {}: {}", file.getName(), e.getMessage());
            }
        }
        return classes;
    }

    private static Class<?> loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        try {
            return classLoader.loadClass(className);
        } catch (NoClassDefFoundError e) {
            throw new ClassNotFoundException("Dependency missing for class: " + className, e);
        }
    }
}