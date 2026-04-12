package io.github.itech_framework.core.processor.components_processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.annotations.ComponentScan;
import io.github.itech_framework.core.annotations.api_client.EnableApiClient;
import io.github.itech_framework.core.annotations.components.Component;
import io.github.itech_framework.core.annotations.components.IgnoreInterfaces;
import io.github.itech_framework.core.annotations.components.levels.BusinessLogic;
import io.github.itech_framework.core.annotations.components.levels.Configuration;
import io.github.itech_framework.core.annotations.components.levels.DataAccess;
import io.github.itech_framework.core.annotations.components.levels.Presentation;
import io.github.itech_framework.core.annotations.components.levels.Service;
import io.github.itech_framework.core.annotations.components.policy.DisableLoaded;
import io.github.itech_framework.core.annotations.constructor.DefaultConstructor;
import io.github.itech_framework.core.annotations.dependency.Use;
import io.github.itech_framework.core.annotations.jfx.EnableJavaFx;
import io.github.itech_framework.core.annotations.methods.OnInit;
import io.github.itech_framework.core.annotations.methods.PreDestroy;
import io.github.itech_framework.core.annotations.parameters.DefaultParameter;
import io.github.itech_framework.core.annotations.properties.Property;
import io.github.itech_framework.core.annotations.reactives.Rx;
import io.github.itech_framework.core.annotations.storage.DataStorage;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.module.ComponentInitializer;
import io.github.itech_framework.core.module.ComponentRegistry;
import io.github.itech_framework.core.module.ModuleInitializer;
import io.github.itech_framework.core.resourcecs.CleanupRegistry;
import io.github.itech_framework.core.storage.DataStorageService;
import io.github.itech_framework.core.storage.DefaultDataStorageService;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.core.utils.AnnotationUtils;
import io.github.itech_framework.core.utils.ObjectUtils;
import io.github.itech_framework.core.utils.PackageClassesLoader;
import io.github.itech_framework.core.utils.PropertiesLoader;

public class ComponentProcessor {
	public static final int DATA_ACCESS_LEVEL = 0;
	public static final int BUSINESS_LOGIC_LEVEL = 1;
	public static final int PRESENTATION_LEVEL = 2;
	public static final int DEFAULT_LEVEL = 3;
	public static final Set<String> JPA_ENTITY_ANNOTATIONS = Set.of("javax.persistence.Entity",
			"jakarta.persistence.Entity");
	private static boolean apiClientsEnabled = false;

	private static boolean javaFxEnabled = false;

	private static final Logger logger = LogManager.getLogger(ComponentProcessor.class);
	private static final ThreadLocal<Set<Class<?>>> CREATING_INSTANCES = ThreadLocal.withInitial(HashSet::new);

	public static void initialize(Class<?> clazz) throws Exception {
		logger.debug("Initializing component...");
		logger.debug("Component scan found? -> {}", clazz.isAnnotationPresent(ComponentScan.class));
		if (clazz.isAnnotationPresent(ComponentScan.class)) {
			ComponentScan componentScan = clazz.getAnnotation(ComponentScan.class);

			// Load properties first
			PropertiesLoader.load(componentScan.properties(), clazz);

			// Load module initializers
			ServiceLoader<ModuleInitializer> loader = ServiceLoader.load(ModuleInitializer.class);
			ComponentRegistry registry = new ComponentRegistry();
			for (ModuleInitializer initializer : loader) {
				initializer.initialize(registry);
			}

			// registering data storage service
			if (ComponentStore.getComponent(DataStorageService.class.getName()) == null) {
				logger.debug("Registering default StorageService");
				DataStorageService defaultStorage = new DefaultDataStorageService();
				ComponentStore.registerComponent(DataStorageService.class.getName(), defaultStorage, DEFAULT_LEVEL);
			}

			// check for api client component is enabled or not
			if (clazz.isAnnotationPresent(EnableApiClient.class)) {
				apiClientsEnabled = true;
			}

			// check for javafx
			if (clazz.isAnnotationPresent(EnableJavaFx.class)) {
				javaFxEnabled = true;
				validateForJavaFx();
			}

			String basePackage = componentScan.basePackage();
			List<Class<?>> classes = PackageClassesLoader.findAllClasses(basePackage, clazz);
			for (Class<?> componentClass : classes) {
				logger.debug("Scanning class: {}", componentClass.getName());
				processComponents(componentClass);
			}

			processTierLevel(DATA_ACCESS_LEVEL);
			processTierLevel(BUSINESS_LOGIC_LEVEL);
			processTierLevel(PRESENTATION_LEVEL);
			processTierLevel(DEFAULT_LEVEL);

			// clean up resources registry
			Runtime.getRuntime().addShutdownHook(new Thread(CleanupRegistry::cleanup));
		}
	}

	private static void validateForJavaFx() {
		if (javaFxEnabled) {
			try {
				Class.forName("io.github.itech_framework.java_fx.ITechJavaFxApplication");
			} catch (ClassNotFoundException e) {
				throw new FrameworkException(
						"""
								    JavaFx is enable but no java fx module found! please add this in your pom or download jar for javafx component.
								    <groupId>io.github.itech-framework</groupId>
								    <artifactId>java-fx</artifactId>
								""");
			}
		}
	}

	private static void processTierLevel(int level) throws Exception {
		List<Object> components = ComponentStore.getComponentsByLevel(level);
		for (Object instance : components) {
			if (!AnnotationUtils.hasAnnotation(instance.getClass(), DisableLoaded.class)) {
				injectFields(instance.getClass(), instance);
				injectMethods(instance.getClass(), instance, level);
			}
		}
	}

	private static void processComponents(Class<?> clazz) {
		if (isJpaEntity(clazz)) {
			if (!isJpaModuleAvailable()) {
				StringBuilder error = new StringBuilder("JPA required but not available.\n");
				try {
					Class.forName("io.github.itech_framework.jpa.config.FlexiJpaConfig");
					error.append("- JPA module found but initialization failed\n");
					error.append("- Check your jpa.* properties configuration");
				} catch (ClassNotFoundException e) {
					error.append("- Add JPA dependency to pom.xml\n");
					error.append("- Required for entities: ").append(clazz.getName());
				}
				throw new FrameworkException(error.toString());
			}
		}
		if (apiClientsEnabled) {
			// process for api client
			// load component initializer
			ServiceLoader<ComponentInitializer> componentInitializerServiceLoader = ServiceLoader
					.load(ComponentInitializer.class);

			for (ComponentInitializer initializer : componentInitializerServiceLoader) {
				initializer.initializeComponent(clazz);
			}
		}
		if (isComponentClass(clazz)) {
			try {
				Component componentAnnotation = getComponentAnnotation(clazz);
				String key = getComponentKey(componentAnnotation, clazz);
				int level = determineComponentLevel(clazz);

				if (ComponentStore.components.containsKey(key)) {
					throw new IllegalArgumentException("Duplicate component key: " + key);
				}

				Constructor<?> constructor = findSuitableConstructor(clazz);
				Object instance = createInstance(constructor);

				ComponentStore.registerComponent(key, instance, level);

				if (!AnnotationUtils.hasAnnotation(clazz, IgnoreInterfaces.class)) {
					for (Class<?> iface : clazz.getInterfaces()) {
						String interfaceKey = iface.getName();
						if (ComponentStore.components.containsKey(interfaceKey)) {
							throw new IllegalArgumentException(
									"Duplicate component key for interface: " + interfaceKey);
						}
						ComponentStore.registerComponent(interfaceKey, instance, level);
					}
				}

			} catch (Exception e) {
				logger.error("Component processing failed", e);
				throw new RuntimeException(e);
			}
		}

	}

	private static boolean isJpaModuleAvailable() {
		try {
			Class<?> configClass = Class.forName("io.github.itech_framework.jpa.config.FlexiJpaConfig");
			Object config = ComponentStore.getComponent(configClass.getName());

			logger.debug("Is JPA config is NULL : {}", config == null);

			if (config == null)
				return false;

			Method isInitialized = configClass.getMethod("isInitialized");

			logger.debug("Is JPA initialized correctly: {}", isInitialized.invoke(config));
			return (boolean) isInitialized.invoke(config);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isJpaEntity(Class<?> clazz) {
		return Arrays.stream(clazz.getAnnotations())
				.anyMatch(annotation -> JPA_ENTITY_ANNOTATIONS.contains(annotation.annotationType().getName()));
	}

	private static Component getComponentAnnotation(Class<?> clazz) {
		if (clazz.isAnnotationPresent(Component.class)) {
			return clazz.getAnnotation(Component.class);
		}

		for (Annotation annotation : clazz.getAnnotations()) {
			Component component = annotation.annotationType().getAnnotation(Component.class);
			if (component != null) {
				return component;
			}
		}
		throw new IllegalStateException("No component annotation found on " + clazz.getName());
	}

	private static boolean isComponentClass(Class<?> clazz) {
		boolean isComponent = clazz.isAnnotationPresent(Component.class)
				|| Arrays.stream(clazz.getAnnotations()).anyMatch(annotation -> {
					boolean hasComponent = annotation.annotationType().isAnnotationPresent(Component.class);
					logger.debug("Checking annotation {} on {}: {}", annotation.annotationType().getSimpleName(),
							clazz.getSimpleName(), hasComponent);
					return hasComponent;
				});

		logger.debug("Class {} is component: {}", clazz.getSimpleName(), isComponent);
		return isComponent;
	}

	@SuppressWarnings("removal")
	private static int determineComponentLevel(Class<?> clazz) {
		if (clazz.isAnnotationPresent(DataAccess.class))
			return DATA_ACCESS_LEVEL;
		if (clazz.isAnnotationPresent(BusinessLogic.class))
			return BUSINESS_LOGIC_LEVEL;
		if (clazz.isAnnotationPresent(Service.class))
			return BUSINESS_LOGIC_LEVEL;
		if (clazz.isAnnotationPresent(Presentation.class))
			return PRESENTATION_LEVEL;
		return DEFAULT_LEVEL;
	}

	private static String getComponentKey(Component component, Class<?> clazz) {
		return component.name().isEmpty() ? clazz.getName() : component.name();
	}

	private static Constructor<?> findSuitableConstructor(Class<?> clazz) throws NoSuchMethodException {
		List<Constructor<?>> constructors = Arrays.stream(clazz.getDeclaredConstructors())
				.sorted((c1, c2) -> Integer.compare(c2.getParameterCount(), c1.getParameterCount())).toList();

		Optional<Constructor<?>> defaultConstructor = constructors.stream()
				.filter(c -> c.isAnnotationPresent(DefaultConstructor.class)).findFirst();

		if (defaultConstructor.isPresent()) {
			Constructor<?> cons = defaultConstructor.get();
			cons.setAccessible(true);
			return cons;
		}

		try {
			return clazz.getDeclaredConstructor();
		} catch (NoSuchMethodException ignored) {
		}

		for (Constructor<?> constructor : constructors) {
			if (canResolveConstructorParameters(constructor)) {
				constructor.setAccessible(true);
				return constructor;
			}
		}

		throw new NoSuchMethodException("No resolvable constructor found for " + clazz.getName());
	}

	private static boolean canResolveConstructorParameters(Constructor<?> constructor) {
		return Arrays.stream(constructor.getParameters())
				.allMatch(param -> ComponentStore.components.containsKey(param.getType().getName())
						|| param.isAnnotationPresent(DefaultParameter.class));
	}

	private static Object createInstance(Constructor<?> constructor) throws Exception {
		Object[] args = Arrays.stream(constructor.getParameters()).map(ComponentProcessor::resolveParameter).toArray();
		return constructor.newInstance(args);
	}

	private static Object resolveParameter(Parameter parameter) {
		Object component = ComponentStore.components.get(parameter.getType().getName());
		if (component != null)
			return component;

		DefaultParameter defaultParam = parameter.getAnnotation(DefaultParameter.class);
		if (defaultParam != null) {
			return ObjectUtils.convertValue(defaultParam.value(), parameter.getType());
		}

		return getDefaultValueForType(parameter.getType());
	}

	public static void injectFields(Class<?> clazz, Object instance) {
		Class<?> currentClass = clazz;
		while (currentClass != null && currentClass != Object.class) {
			processClassFields(currentClass, instance);
			currentClass = currentClass.getSuperclass();
		}
	}

	@SuppressWarnings("removal")
	private static void processClassFields(Class<?> clazz, Object instance) {
		for (Field field : clazz.getDeclaredFields()) {
			try {
				if (field.isAnnotationPresent(Property.class)) {
					processPropertyField(instance, field);
				}
				if (field.isAnnotationPresent(Rx.class) || field.isAnnotationPresent(Use.class)) {
					processDependencyField(instance, field);
				}
				if (field.isAnnotationPresent(DataStorage.class)) {
					processDataStorageField(instance, field);
				}
			} catch (IllegalAccessException e) {
				logger.error("Field injection failed for {} in {}", field.getName(), clazz.getName(), e);
				throw new RuntimeException("Field injection failed", e);
			}
		}
	}

	private static void processPropertyField(Object instance, Field field) throws IllegalAccessException {
		Property property = field.getAnnotation(Property.class);
		String key = property.key();
		String defaultValue = property.defaultValue();

		String value = PropertiesLoader.getProperty(key, defaultValue);

		if (value == null || value.isEmpty()) {
			if (defaultValue.isEmpty()) {
				throw new IllegalStateException("Property '" + key + "' not found and no default value specified");
			}
			value = defaultValue;
		}

		Object convertedValue = ObjectUtils.convertValue(value, field.getType());
		field.setAccessible(true);
		field.set(instance, convertedValue);
	}

	@SuppressWarnings("removal")
	private static void processDependencyField(Object instance, Field field) throws IllegalAccessException {
		String componentName = "";
		boolean share = true;

		// Check which annotation is present
		if (field.isAnnotationPresent(Rx.class)) {
			Rx rxAnnotation = field.getAnnotation(Rx.class);
			componentName = rxAnnotation.name();
			logger.warn("@Rx annotation is deprecated and will be removed in future versions. "
					+ "Please use @Use instead in class: {}", field.getDeclaringClass().getName());
		} else if (field.isAnnotationPresent(Use.class)) {
			Use componentRefAnnotation = field.getAnnotation(Use.class);
			componentName = componentRefAnnotation.name();
			share = componentRefAnnotation.share();
		}

		// Get the component
		Object component = resolveComponentForField(field, componentName, share);

		if (component == null) {
			throw new IllegalStateException("Missing component for field: " + field.getName() + " in class: "
					+ field.getDeclaringClass().getName());
		}

		if (!field.getType().isAssignableFrom(component.getClass())) {
			throw new ClassCastException("Component type mismatch for field " + field.getName() + ". Expected: "
					+ field.getType().getName() + ", Got: " + component.getClass().getName());
		}

		// Inject the component
		field.setAccessible(true);
		field.set(instance, component);
	}

	private static Object resolveComponentForField(Field field, String componentName, boolean share) {
		Class<?> fieldType = field.getType();
		String key = componentName.isEmpty() ? fieldType.getName() : componentName;

		if (share) {
			// Get from ComponentStore
			return ComponentStore.components.get(key);
		} else {
			// Create new instance with cycle detection
			Class<?> componentClass = determineComponentClass(fieldType, componentName);
			return createNewInstance(componentClass);
		}
	}

	private static Class<?> determineComponentClass(Class<?> fieldType, String componentName) {
		if (!componentName.isEmpty()) {
			Object registeredComponent = findRegisteredComponentByName(componentName);
			if (registeredComponent != null) {
				return registeredComponent.getClass();
			}
		}

		if (fieldType.isInterface()) {
			for (Map.Entry<String, Object> entry : ComponentStore.components.entrySet()) {
				if (fieldType.isAssignableFrom(entry.getValue().getClass())) {
					return entry.getValue().getClass();
				}
			}

			throw new IllegalStateException("Cannot determine concrete implementation for interface: "
					+ fieldType.getName() + ". Please specify a component name or register a concrete implementation.");
		}

		return fieldType;
	}

	private static Object findRegisteredComponentByName(String name) {
		Object component = ComponentStore.components.get(name);
		if (component != null) {
			return component;
		}

		for (Map.Entry<String, Object> entry : ComponentStore.components.entrySet()) {
			String simpleName = entry.getKey();
			if (simpleName.contains(".")) {
				simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1);
			}
			if (simpleName.equals(name)) {
				return entry.getValue();
			}
		}

		return null;
	}

	private static Object createNewInstance(Class<?> clazz) {
		Set<Class<?>> creating = CREATING_INSTANCES.get();

		if (creating.contains(clazz)) {
			throw new FrameworkException("Circular dependency detected while creating instance of " + clazz.getName()
					+ ". Current creation chain: "
					+ creating.stream().map(Class::getSimpleName).collect(Collectors.joining(" -> ")) + " -> "
					+ clazz.getSimpleName());
		}

		creating.add(clazz);
		try {
			Constructor<?> constructor = findSuitableConstructor(clazz);

			Object[] args = Arrays.stream(constructor.getParameters())
					.map(param -> resolveConstructorParameterForNewInstance(param, creating)).toArray();

			Object instance = constructor.newInstance(args);

			injectFields(clazz, instance);

			processInitMethod(clazz, instance);

			return instance;
		} catch (Exception e) {
			throw new FrameworkException("Failed to create new instance of " + clazz.getName(), e);
		} finally {
			creating.remove(clazz);
			if (creating.isEmpty()) {
				CREATING_INSTANCES.remove();
			}
		}
	}

	private static Object resolveConstructorParameterForNewInstance(Parameter parameter, Set<Class<?>> creating) {
		Class<?> paramType = parameter.getType();

		// Check for circular dependency at constructor level
		if (creating.contains(paramType)) {
			throw new FrameworkException("Circular dependency in constructor parameter. Cannot create "
					+ paramType.getName() + " while creating " + creating.iterator().next().getName());
		}

		// Try to get from ComponentStore first
		Object component = ComponentStore.components.get(paramType.getName());
		if (component != null) {
			return component;
		}

		// Check for DefaultParameter annotation
		DefaultParameter defaultParam = parameter.getAnnotation(DefaultParameter.class);
		if (defaultParam != null) {
			return ObjectUtils.convertValue(defaultParam.value(), paramType);
		}

		// If it's a component type, create new instance (with cycle detection)
		if (isComponentClass(paramType)) {
			// Check if we're already creating this type
			if (creating.contains(paramType)) {
				throw new FrameworkException("Circular dependency: Trying to create " + paramType.getName()
						+ " while already in creation chain");
			}

			// Create new instance with the current creation set
			return createNewInstance(paramType);
		}

		// Try to find by interface in ComponentStore
		for (Map.Entry<String, Object> entry : ComponentStore.components.entrySet()) {
			if (paramType.isAssignableFrom(entry.getValue().getClass())) {
				return entry.getValue();
			}
		}

		// Return default value
		return getDefaultValueForType(paramType);
	}

	private static void processDataStorageField(Object instance, Field field) throws IllegalAccessException {
		DataStorage dataStorage = field.getAnnotation(DataStorage.class);
		String key = dataStorage.key().isEmpty() ? field.getName() : dataStorage.key();

		DataStorageService storageService = (DataStorageService) ComponentStore
				.getComponent(DataStorageService.class.getName());
		if (storageService == null) {
			throw new IllegalStateException("No StorageService available for @DataStorage");
		}

		String storedValue = storageService.load(key);
		Object valueToSet;

		if (storedValue != null) {
			valueToSet = ObjectUtils.convertValue(storedValue, field.getType());
		} else {
			String defaultValue = dataStorage.defaultValue();
			if (!defaultValue.isEmpty()) {
				valueToSet = ObjectUtils.convertValue(defaultValue, field.getType());
			} else {
				field.setAccessible(true);
				valueToSet = field.get(instance);
			}
		}

		field.setAccessible(true);
		field.set(instance, valueToSet);

		/*
		 * CleanupRegistry.addTask(() -> { try { field.setAccessible(true); Object
		 * currentValue = field.get(instance); String valueToStore =
		 * convertToString(currentValue, field.getType()); storageService.save(key,
		 * valueToStore); } catch (IllegalAccessException e) {
		 * logger.error("Failed to save DataStorage field {}", key, e); } },
		 * DEFAULT_LEVEL);
		 */
	}

	/*
	 * private static String convertToString(Object value, Class<?> type) { if
	 * (value == null) return null; if (type.isEnum()) { return ((Enum<?>)
	 * value).name(); } return value.toString(); }
	 */

	public static void injectMethods(Class<?> clazz, Object instance, int level) throws Exception {
	    processInitMethod(clazz, instance);
	    processPreDestroyMethod(clazz, instance, level);
	    if(clazz.isAnnotationPresent(Configuration.class)) {
	        processConfigurationMethod(clazz, instance);
	    }
	}

	private static void processConfigurationMethod(Class<?> clazz, Object configInstance) throws Exception {
	    List<Method> componentMethods = Arrays.stream(clazz.getDeclaredMethods())
	            .filter(m -> m.isAnnotationPresent(Component.class) && m.getReturnType() != void.class)
	            .collect(Collectors.toList());
	    
	    logger.debug("Processing configuration class: {}, found {} component methods", 
	                 clazz.getSimpleName(), componentMethods.size());
	    
	    // Track processed methods to detect circular dependencies
	    ThreadLocal<Set<Method>> methodsInProgress = ThreadLocal.withInitial(HashSet::new);
	    
	    for (Method method : componentMethods) {
	        processConfigurationBeanMethod(method, configInstance, methodsInProgress);
	    }
	}

	private static void processConfigurationBeanMethod(Method method, Object configInstance, 
	                                                   ThreadLocal<Set<Method>> methodsInProgress) throws Exception {
	    
	    Set<Method> processing = methodsInProgress.get();
	    
	    // Check for circular dependency
	    if (processing.contains(method)) {
	        throw new FrameworkException("Circular dependency detected in configuration method: " + 
	                                     method.getName() + " in class " + method.getDeclaringClass().getName());
	    }
	    
	    processing.add(method);
	    try {
	        Component componentAnnotation = method.getAnnotation(Component.class);
	        String key = getComponentKeyFromMethod(componentAnnotation, method);
	        
	        // Check if already registered
	        if (ComponentStore.components.containsKey(key)) {
	            logger.debug("Bean already registered with key: {}, skipping method: {}", 
	                         key, method.getName());
	            return;
	        }
	        
	        // Resolve method parameters (may include other configuration beans)
	        Object[] params = resolveConfigurationMethodParameters(method, configInstance, methodsInProgress);
	        
	        // Invoke method to create bean
	        method.setAccessible(true);
	        Object bean = method.invoke(configInstance, params);
	        
	        if (bean == null) {
	            throw new FrameworkException("Configuration method " + method.getName() + 
	                                         " returned null. @Component methods must return non-null objects.");
	        }
	        
	        // Determine component level
	        int level = determineComponentLevel(method.getReturnType());
	        
	        // Register the bean
	        ComponentStore.registerComponent(key, bean, level);
	        
	        // Also register under return type
	        ComponentStore.registerComponent(method.getReturnType().getName(), bean, level);
	        
	        // Register under interfaces if not ignored
	        Class<?> beanClass = bean.getClass();
	        if (!AnnotationUtils.hasAnnotation(beanClass, IgnoreInterfaces.class)) {
	            for (Class<?> iface : beanClass.getInterfaces()) {
	                String interfaceKey = iface.getName();
	                if (!ComponentStore.components.containsKey(interfaceKey)) {
	                    ComponentStore.registerComponent(interfaceKey, bean, level);
	                }
	            }
	        }
	        
	        // Perform field injection on the bean
	        injectFields(beanClass, bean);
	        
	        // Call @OnInit methods on the bean
	        processInitMethod(beanClass, bean);
	        
	        // Register @PreDestroy methods
	        processPreDestroyMethod(beanClass, bean, level);
	        
	        logger.debug("Registered bean from configuration method: {} -> {}", 
	                     method.getName(), key);
	        
	    } finally {
	        processing.remove(method);
	        if (processing.isEmpty()) {
	            methodsInProgress.remove();
	        }
	    }
	}

	private static String getComponentKeyFromMethod(Component componentAnnotation, Method method) {
	    if (componentAnnotation != null && !componentAnnotation.name().isEmpty()) {
	        return componentAnnotation.name();
	    }
	    return method.getDeclaringClass().getName() + "." + method.getName();
	}

	private static Object[] resolveConfigurationMethodParameters(Method method, Object configInstance,
	                                                            ThreadLocal<Set<Method>> methodsInProgress) throws Exception {
	    Parameter[] parameters = method.getParameters();
	    Object[] args = new Object[parameters.length];
	    
	    for (int i = 0; i < parameters.length; i++) {
	        Parameter param = parameters[i];
	        Class<?> paramType = param.getType();
	        
	        Object component = ComponentStore.components.get(paramType.getName());
	        if (component != null) {
	            args[i] = component;
	            continue;
	        }
	        
	        component = findConfigurationBean(paramType, method.getDeclaringClass(), 
	                                         configInstance, methodsInProgress);
	        if (component != null) {
	            args[i] = component;
	            continue;
	        }
	        
	        DefaultParameter defaultParam = param.getAnnotation(DefaultParameter.class);
	        if (defaultParam != null) {
	            args[i] = ObjectUtils.convertValue(defaultParam.value(), paramType);
	            continue;
	        }
	        
	        if (isComponentClass(paramType)) {
	            try {
	                args[i] = createNewInstance(paramType);
	            } catch (Exception e) {
	                throw new FrameworkException("Failed to create dependency for parameter '" + 
	                                           param.getName() + "' in method " + method.getName(), e);
	            }
	            continue;
	        }
	        
	        for (Map.Entry<String, Object> entry : ComponentStore.components.entrySet()) {
	            if (paramType.isAssignableFrom(entry.getValue().getClass())) {
	                args[i] = entry.getValue();
	                break;
	            }
	        }
	        
	        if (args[i] == null) {
	            throw new FrameworkException("Cannot resolve parameter '" + param.getName() + 
	                                       "' of type " + paramType.getName() + 
	                                       " for configuration method " + method.getName());
	        }
	    }
	    
	    return args;
	}

	private static Object findConfigurationBean(Class<?> type, Class<?> configClass, 
	                                           Object configInstance,
	                                           ThreadLocal<Set<Method>> methodsInProgress) throws Exception {
	    List<Method> matchingMethods = Arrays.stream(configClass.getDeclaredMethods())
	            .filter(m -> m.isAnnotationPresent(Component.class) && 
	                         m.getReturnType() != void.class &&
	                         type.isAssignableFrom(m.getReturnType()))
	            .collect(Collectors.toList());
	    
	    if (matchingMethods.isEmpty()) {
	        return null;
	    }
	    
	    if (matchingMethods.size() > 1) {
	        for (Method m : matchingMethods) {
	            Component comp = m.getAnnotation(Component.class);
	            String key = getComponentKeyFromMethod(comp, m);
	            if (ComponentStore.components.containsKey(key)) {
	                return ComponentStore.components.get(key);
	            }
	        }
	        throw new FrameworkException("Multiple @Component methods return " + type.getName() + 
	                                   " in configuration class " + configClass.getName() + 
	                                   ". Use @Component(name='...') to specify unique names.");
	    }
	    
	    Method beanMethod = matchingMethods.get(0);
	    
	    Component compAnnotation = beanMethod.getAnnotation(Component.class);
	    String key = getComponentKeyFromMethod(compAnnotation, beanMethod);
	    
	    if (ComponentStore.components.containsKey(key)) {
	        return ComponentStore.components.get(key);
	    }
	    
	    processConfigurationBeanMethod(beanMethod, configInstance, methodsInProgress);
	    
	    return ComponentStore.components.get(key);
	}

	private static void processInitMethod(Class<?> clazz, Object instance) throws Exception {
		List<Method> initMethods = Arrays.stream(clazz.getDeclaredMethods())
				.filter(m -> m.isAnnotationPresent(OnInit.class))
				.sorted(Comparator.comparingInt(m -> m.getAnnotation(OnInit.class).order())).toList();

		for (Method method : initMethods) {
			validateInitMethod(method);
			Object[] params = resolveMethodParameters(method);
			method.setAccessible(true);
			try {
				method.invoke(instance, params);
			} catch (InvocationTargetException e) {
				Throwable targetException = e.getTargetException();
				if (targetException instanceof Exception) {
					throw (Exception) targetException;
				} else {
					throw new RuntimeException("Error in init method", targetException);
				}
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw e;
			}
		}
	}

	private static void processPreDestroyMethod(Class<?> clazz, Object instance, int level) {
		Arrays.stream(clazz.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(PreDestroy.class))
				.forEach(method -> {
					CleanupRegistry.register(() -> {
						try {
							method.setAccessible(true);
							method.invoke(instance);
						} catch (Exception e) {
							throw new FrameworkException(e.getMessage());
						}
					}, level);
				});
	}

	private static void validateInitMethod(Method method) {
		if (method.getParameterCount() > 0 && !canResolveMethodParameters(method)) {
			throw new IllegalStateException("Unresolvable parameters for method: " + method.getName());
		}
	}

	private static boolean canResolveMethodParameters(Method method) {
		return Arrays.stream(method.getParameters())
				.allMatch(param -> ComponentStore.components.containsKey(param.getType().getName())
						|| param.isAnnotationPresent(DefaultParameter.class));
	}

	private static Object[] resolveMethodParameters(Method method) {
		return Arrays.stream(method.getParameters()).map(param -> {
			Object component = ComponentStore.components.get(param.getType().getName());
			if (component != null)
				return component;
			throw new IllegalStateException("Cannot resolve parameter: " + param.getName());
		}).toArray();
	}

	private static Object getDefaultValueForType(Class<?> type) {
		if (type == int.class)
			return 0;
		if (type == double.class)
			return 0.0;
		if (type == long.class)
			return 0L;
		if (type == float.class)
			return 0.0f;
		if (type == short.class)
			return (short) 0;
		if (type == byte.class)
			return (byte) 0;
		if (type == boolean.class)
			return false;
		if (type == char.class)
			return '\0';
		if (type == Integer.class || type == Double.class || type == Long.class || type == Float.class
				|| type == Short.class || type == Byte.class || type == Boolean.class || type == Character.class
				|| type == String.class)
			return null;

		throw new IllegalArgumentException("Unsupported parameter type: " + type.getName());
	}
}