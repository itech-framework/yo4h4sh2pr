package io.github.itech_framework.core.processor.components_processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.annotations.ComponentScan;
import io.github.itech_framework.core.annotations.api_client.EnableApiClient;
import io.github.itech_framework.core.annotations.components.Component;
import io.github.itech_framework.core.annotations.components.IgnoreInterfaces;
import io.github.itech_framework.core.annotations.components.levels.BusinessLogic;
import io.github.itech_framework.core.annotations.components.levels.DataAccess;
import io.github.itech_framework.core.annotations.components.levels.Presentation;
import io.github.itech_framework.core.annotations.components.levels.Service;
import io.github.itech_framework.core.annotations.jfx.EnableJavaFx;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.module.ComponentInitializer;
import io.github.itech_framework.core.module.ComponentRegistry;
import io.github.itech_framework.core.module.ModuleInitializer;
import io.github.itech_framework.core.resourcecs.CleanupRegistry;
import io.github.itech_framework.core.storage.DataStorageService;
import io.github.itech_framework.core.storage.DefaultDataStorageService;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.core.utils.AnnotationUtils;
import io.github.itech_framework.core.utils.PackageClassesLoader;
import io.github.itech_framework.core.utils.PropertiesLoader;

public class ComponentProcessor {
    public static final int DATA_ACCESS_LEVEL = 0;
    public static final int BUSINESS_LOGIC_LEVEL = 1;
    public static final int PRESENTATION_LEVEL = 2;
    public static final int DEFAULT_LEVEL = 3;
    public static final Set<String> JPA_ENTITY_ANNOTATIONS = Set.of("javax.persistence.Entity",
            "jakarta.persistence.Entity");

    private static final Logger logger = LogManager.getLogger(ComponentProcessor.class);
    private static final AtomicBoolean SHUTDOWN_HOOK = new AtomicBoolean();
    private static volatile BeanFactory beanFactory = new BeanFactory();

    public static void initialize(Class<?> clazz) throws Exception {
        ComponentScan componentScan = clazz.getAnnotation(ComponentScan.class);
        if (componentScan == null) {
            logger.debug("No @ComponentScan on {}", clazz.getName());
            return;
        }

        PropertiesLoader.load(componentScan.properties(), clazz);

        ComponentRegistry registry = new ComponentRegistry();
        for (ModuleInitializer initializer : ServiceLoader.load(ModuleInitializer.class)) {
            initializer.initialize(registry);
        }

        if (clazz.isAnnotationPresent(EnableJavaFx.class)) {
            validateForJavaFx();
        }

        List<ComponentInitializer> componentInitializers = new ArrayList<>();
        if (clazz.isAnnotationPresent(EnableApiClient.class)) {
            ServiceLoader.load(ComponentInitializer.class).forEach(componentInitializers::add);
        }

        BeanFactory factory = new BeanFactory();
        List<BeanDefinition> scanned = new ArrayList<>();
        Boolean jpaAvailable = null;
        for (Class<?> candidate : PackageClassesLoader.findAllClasses(componentScan.basePackage(), clazz)) {
            if (isJpaEntity(candidate)) {
                if (jpaAvailable == null) {
                    jpaAvailable = isJpaModuleAvailable();
                }
                if (!jpaAvailable) {
                    throw jpaUnavailable(candidate);
                }
            }
            for (ComponentInitializer initializer : componentInitializers) {
                initializer.initializeComponent(candidate);
            }
            BeanDefinition definition = describe(candidate);
            if (definition != null) {
                factory.define(definition);
                scanned.add(definition);
            }
        }
        for (BeanDefinition owner : scanned) {
            defineFactoryMethods(factory, owner);
        }

        String storageKey = DataStorageService.class.getName();
        if (!factory.isDefined(storageKey) && ComponentStore.getComponent(storageKey) == null) {
            ComponentStore.registerComponent(storageKey, new DefaultDataStorageService(), DEFAULT_LEVEL);
        }

        beanFactory = factory;
        factory.start();

        if (SHUTDOWN_HOOK.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(CleanupRegistry::cleanup));
        }
    }

    public static void injectFields(Class<?> clazz, Object instance) {
        beanFactory.injectFields(clazz, instance);
    }

    public static void injectMethods(Class<?> clazz, Object instance, int level) throws Exception {
        beanFactory.injectMethods(clazz, instance, level);
    }
    
    public static void runInitMethods(Class<?> clazz, Object instance) {
        beanFactory.runInitMethods(clazz, instance);
    }

    public static void destroyInstance(Class<?> clazz, Object instance) {
        beanFactory.destroyInstance(clazz, instance);
    }

    private static BeanDefinition describe(Class<?> type) {
        if (Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        Component component = componentAnnotation(type);
        if (component == null) {
            return null;
        }
        String key = component.name().isEmpty() ? type.getName() : component.name();
        List<String> aliases = AnnotationUtils.hasAnnotation(type, IgnoreInterfaces.class)
                ? List.of()
                : Arrays.stream(type.getInterfaces()).map(Class::getName).toList();
        return BeanDefinition.ofClass(key, type, levelOf(type), aliases);
    }

    private static void defineFactoryMethods(BeanFactory factory, BeanDefinition owner) {
        for (Method method : ClassMetadata.of(owner.type()).factoryMethods()) {
            Component annotation = method.getAnnotation(Component.class);
            String key = annotation.name().isEmpty()
                    ? method.getDeclaringClass().getName() + "." + method.getName()
                    : annotation.name();
            Class<?> returnType = method.getReturnType();
            List<String> aliases = returnType.getName().equals(key) ? List.of() : List.of(returnType.getName());
            factory.define(BeanDefinition.ofFactory(key, returnType, levelOf(returnType), aliases, method, owner.key()));
        }
    }

    private static Component componentAnnotation(Class<?> type) {
        Component direct = type.getAnnotation(Component.class);
        if (direct != null) {
            return direct;
        }
        for (Annotation annotation : type.getAnnotations()) {
            Component meta = annotation.annotationType().getAnnotation(Component.class);
            if (meta != null) {
                return meta;
            }
        }
        return null;
    }

    @SuppressWarnings("removal")
    private static int levelOf(Class<?> type) {
        if (type.isAnnotationPresent(DataAccess.class)) return DATA_ACCESS_LEVEL;
        if (type.isAnnotationPresent(BusinessLogic.class)) return BUSINESS_LOGIC_LEVEL;
        if (type.isAnnotationPresent(Service.class)) return BUSINESS_LOGIC_LEVEL;
        if (type.isAnnotationPresent(Presentation.class)) return PRESENTATION_LEVEL;
        return DEFAULT_LEVEL;
    }

    private static boolean isJpaEntity(Class<?> type) {
        for (Annotation annotation : type.getAnnotations()) {
            if (JPA_ENTITY_ANNOTATIONS.contains(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJpaModuleAvailable() {
        try {
            Class<?> configClass = Class.forName("io.github.itech_framework.jpa.config.FlexiJpaConfig");
            Object config = ComponentStore.getComponent(configClass.getName());
            if (config == null) {
                return false;
            }
            return (boolean) configClass.getMethod("isInitialized").invoke(config);
        } catch (Exception e) {
            return false;
        }
    }

    private static FrameworkException jpaUnavailable(Class<?> entity) {
        StringBuilder error = new StringBuilder("JPA required but not available.\n");
        try {
            Class.forName("io.github.itech_framework.jpa.config.FlexiJpaConfig");
            error.append("- JPA module found but initialization failed\n");
            error.append("- Check your jpa.* properties configuration");
        } catch (ClassNotFoundException e) {
            error.append("- Add JPA dependency to pom.xml\n");
            error.append("- Required for entities: ").append(entity.getName());
        }
        return new FrameworkException(error.toString());
    }

    private static void validateForJavaFx() {
        try {
            Class.forName("io.github.itech_framework.java_fx.ITechJavaFxApplication");
        } catch (ClassNotFoundException e) {
            throw new FrameworkException("""
                    JavaFx is enable but no java fx module found! please add this in your pom or download jar for javafx component.
                    <groupId>io.github.itech-framework</groupId>
                    <artifactId>java-fx</artifactId>
                    """);
        }
    }
}