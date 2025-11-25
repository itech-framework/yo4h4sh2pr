package io.github.itech_framework.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;

/**
 * Main application class for ITech Annotation Framework.
 * <p>
 * Provides the entry point for initializing and running applications built 
 * with the ITech component-based framework. Handles component scanning, 
 * dependency injection, and application lifecycle management through 
 * annotation processing.
 * </p>
 * 
 * @author Sai Zaw Myint
 */
public class ITechApplication {
    private static final Logger logger = LogManager.getLogger(ITechApplication.class);
    
    /**
     * Initializes and starts the ITech framework application
     * @param clazz The main application class used as the starting point 
     *              for component scanning and dependency injection setup
     * @throws Exception 
     * @throws RuntimeException if the application fails to initialize 
     *         components properly. The exception will contain the root 
     *         cause of the failure
     * @implNote This method will:
     * <ul>
     *   <li>Initialize component processing through {@link ComponentProcessor}</li>
     *   <li>Scan for annotated components in the classpath</li>
     *   <li>Set up dependency injection context</li>
     *   <li>Handle any initialization errors by logging and rethrowing</li>
     * </ul>
     */
    public static void run(Class<?> clazz) throws Exception {
    	ComponentProcessor.initialize(clazz);
        logger.debug("Components initialized!");
    }
}
