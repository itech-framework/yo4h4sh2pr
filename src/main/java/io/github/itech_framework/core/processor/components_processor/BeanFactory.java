package io.github.itech_framework.core.processor.components_processor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.itech_framework.core.annotations.components.IgnoreInterfaces;
import io.github.itech_framework.core.annotations.constructor.DefaultConstructor;
import io.github.itech_framework.core.annotations.dependency.Use;
import io.github.itech_framework.core.annotations.parameters.DefaultParameter;
import io.github.itech_framework.core.annotations.properties.Property;
import io.github.itech_framework.core.annotations.storage.DataStorage;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.ClassMetadata.FieldPlan;
import io.github.itech_framework.core.resourcecs.CleanupRegistry;
import io.github.itech_framework.core.storage.DataStorageService;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.core.utils.AnnotationUtils;
import io.github.itech_framework.core.utils.ObjectUtils;
import io.github.itech_framework.core.utils.PropertiesLoader;

final class BeanFactory {
	private final Map<String, BeanDefinition> byKey = new LinkedHashMap<>();
	private final Map<String, BeanDefinition> bySimpleName = new HashMap<>();
	private final List<BeanDefinition> definitions = new ArrayList<>();
	private final Map<Object, List<Object>> creationDependencies = new IdentityHashMap<>();
	private final Set<Object> initialized = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<Class<?>, Constructor<?>> constructors = new ConcurrentHashMap<>();

	private final Set<String> ambiguousAliases = new HashSet<>();
	private final ComponentStore store;
	private final CleanupRegistry cleanupRegistry;
	
	private final Set<String> ambiguousSimpleNames = new HashSet<>();
	
	private boolean starting;

	BeanFactory(ComponentStore store, CleanupRegistry cleanupRegistry) {
		if (store == null || cleanupRegistry == null) {
			throw new IllegalArgumentException("Component store and cleanup registry must not be null");
		}
		this.store = store;
		this.cleanupRegistry = cleanupRegistry;
	}

	void define(BeanDefinition definition) {
		String key = definition.key();
		if (byKey.containsKey(key) || store.hasComponent(key)) {
			throw new FrameworkException("Duplicate component key: " + key);
		}
		index(key, definition);
		definitions.add(definition);
		for (String alias : definition.aliases()) {
			if (ambiguousAliases.contains(alias)) {
				continue;
			}
			BeanDefinition existing = byKey.get(alias);
			if (existing == null && !store.hasComponent(alias)) {
				index(alias, definition);
				continue;
			}
			boolean sharedInterface = existing != null && !existing.key().equals(alias)
					&& existing.aliases().contains(alias) && !existing.isFactory() && !definition.isFactory();
			if (sharedInterface) {
				byKey.remove(alias);
				unindexSimpleName(alias, existing);
				ambiguousAliases.add(alias);
			}
		}
	}

	private void unindexSimpleName(String name, BeanDefinition definition) {
		int dot = name.lastIndexOf('.');
		bySimpleName.remove(dot < 0 ? name : name.substring(dot + 1), definition);
	}

	boolean isDefined(String key) {
		return byKey.containsKey(key);
	}

	synchronized void start() {
	    starting = true;
	    try {
	        List<BeanDefinition> sorted = new ArrayList<>(definitions);
	        sorted.sort(Comparator.comparingInt(BeanDefinition::level));
	        for (BeanDefinition definition : sorted) {
	            singleton(definition, new LinkedHashSet<>());
	        }
	        for (Object instance : store.getAllComponents()) {
	            initialize(instance, new LinkedHashSet<>());
	        }
	    } finally {
	        starting = false;
	        creationDependencies.clear();
	        initialized.clear();
	    }
	}

	void injectFields(Class<?> type, Object instance) {
		injectFields(ClassMetadata.of(type), instance, new LinkedHashSet<>());
	}

	void injectMethods(Class<?> type, Object instance, int level) {
		ClassMetadata metadata = ClassMetadata.of(type);
		runInit(metadata, instance);
		registerDestroy(metadata, instance);
	}

	void runInitMethods(Class<?> type, Object instance) {
		runInit(ClassMetadata.of(type), instance);
	}

	void destroyInstance(Class<?> type, Object instance) {
		ClassMetadata metadata = ClassMetadata.of(type);
		for (Method method : metadata.destroyMethods()) {
			invokeDestroy(metadata, method, instance);
		}
	}

	private void index(String name, BeanDefinition definition) {
	    byKey.put(name, definition);
	    int dot = name.lastIndexOf('.');
	    String simple = dot < 0 ? name : name.substring(dot + 1);
	    BeanDefinition existing = bySimpleName.putIfAbsent(simple, definition);
	    if (existing != null && existing != definition) {
	        ambiguousSimpleNames.add(simple);
	    }
	}

	private BeanDefinition bySimple(String name) {
	    if (ambiguousSimpleNames.contains(name)) {
	        throw new FrameworkException("Name '" + name + "' matches more than one component. Use the fully qualified name.");
	    }
	    return bySimpleName.get(name);
	}

	private Object singleton(BeanDefinition definition, Set<String> path) {
		Object existing = store.getComponent(definition.key());
		if (existing != null) {
			return existing;
		}
		if (!path.add(definition.key())) {
			throw circular(path, definition.key());
		}
		try {
			Object instance = definition.isFactory() ? invokeFactory(definition, path) : construct(definition, path);
			register(definition, instance);
			return instance;
		} finally {
			path.remove(definition.key());
		}
	}

	private Object construct(BeanDefinition definition, Set<String> path) {
		Class<?> type = definition.type();
		Constructor<?> constructor = selectConstructor(type);
		List<Object> dependencies = new ArrayList<>();
		Object[] args = resolveArguments(constructor.getParameters(), path, dependencies);
		Object instance;
		try {
			instance = constructor.newInstance(args);
		} catch (ReflectiveOperationException e) {
			throw failure("Failed to create " + type.getName(), e);
		}
		creationDependencies.put(instance, dependencies);
		return instance;
	}

	private Object invokeFactory(BeanDefinition definition, Set<String> path) {
		BeanDefinition ownerDefinition = byKey.get(definition.factoryOwnerKey());
		Object owner = singleton(ownerDefinition, path);
		initialize(owner, path);

		Method method = definition.factoryMethod();
		List<Object> dependencies = new ArrayList<>();
		dependencies.add(owner);
		Object[] args = resolveArguments(method.getParameters(), path, dependencies);
		Object bean;
		try {
			bean = method.invoke(owner, args);
		} catch (ReflectiveOperationException e) {
			throw failure(
					"Configuration method failed: " + method.getDeclaringClass().getName() + "." + method.getName(), e);
		}
		if (bean == null) {
			throw new FrameworkException("Configuration method " + method.getName()
					+ " returned null. @Component methods must return non-null objects.");
		}
		creationDependencies.put(bean, dependencies);
		return bean;
	}

	private void register(BeanDefinition definition, Object instance) {
		store.registerComponent(definition.key(), instance, definition.level());
		for (String alias : definition.aliases()) {
			if (byKey.get(alias) == definition) {
				store.registerComponent(alias, instance, definition.level());
			}
		}
		if (definition.isFactory() && !AnnotationUtils.hasAnnotation(instance.getClass(), IgnoreInterfaces.class)) {
			for (Class<?> iface : instance.getClass().getInterfaces()) {
				String name = iface.getName();
				if (!byKey.containsKey(name) && !store.hasComponent(name)) {
					store.registerComponent(name, instance, definition.level());
				}
			}
		}
	}

	private void initialize(Object instance, Set<String> path) {
		if (!initialized.add(instance)) {
			return;
		}
		ClassMetadata metadata = ClassMetadata.of(instance.getClass());
		if (metadata.disabled()) {
			return;
		}
		for (Object dependency : creationDependencies.getOrDefault(instance, List.of())) {
			initialize(dependency, path);
		}
		for (FieldPlan plan : metadata.fields()) {
			if (plan.use() != null && plan.use().share()) {
				Object dependency = resolveShared(plan.field().getType(), plan.use().name(), path);
				if (dependency != null) {
					initialize(dependency, path);
				}
			}
		}
		injectFields(metadata, instance, path);
		runInit(metadata, instance);
		registerDestroy(metadata, instance);
	}

	private Constructor<?> selectConstructor(Class<?> type) {
		Constructor<?> cached = constructors.get(type);
		if (cached != null) {
			return cached;
		}
		Constructor<?> selected = chooseConstructor(type);
		selected.setAccessible(true);
		constructors.put(type, selected);
		return selected;
	}

	private Constructor<?> chooseConstructor(Class<?> type) {
		Constructor<?>[] declared = type.getDeclaredConstructors();
		for (Constructor<?> constructor : declared) {
			if (constructor.isAnnotationPresent(DefaultConstructor.class)) {
				return constructor;
			}
		}
		try {
			return type.getDeclaredConstructor();
		} catch (NoSuchMethodException ignored) {
		}
		List<Constructor<?>> candidates = new ArrayList<>(List.of(declared));
		candidates.sort((a, b) -> Integer.compare(b.getParameterCount(), a.getParameterCount()));
		Constructor<?> best = null;
		for (Constructor<?> constructor : candidates) {
			if (!canResolve(constructor.getParameters())) {
				continue;
			}
			if (best == null) {
				best = constructor;
				continue;
			}
			if (constructor.getParameterCount() == best.getParameterCount()) {
				throw new FrameworkException(
						"Ambiguous constructors in " + type.getName() + ". Mark one with @DefaultConstructor.");
			}
			break;
		}
		if (best == null) {
			throw new FrameworkException("No resolvable constructor found for " + type.getName());
		}
		return best;
	}

	private boolean canResolve(Parameter[] parameters) {
		for (Parameter parameter : parameters) {
			if (!parameter.isAnnotationPresent(DefaultParameter.class) && !isResolvable(parameter)) {
				return false;
			}
		}
		return true;
	}

	private boolean isResolvable(Parameter parameter) {
		Use use = parameter.getAnnotation(Use.class);
		if (use != null && !use.name().isEmpty()) {
			String name = use.name();
			return byKey.containsKey(name) || bySimpleName.containsKey(name) || store.hasComponent(name);
		}
		Class<?> type = parameter.getType();
		return !isValueType(type) && (byKey.containsKey(type.getName()) || store.hasComponent(type.getName())
				|| countAssignable(type) == 1);
	}

	private Object[] resolveArguments(Parameter[] parameters, Set<String> path, List<Object> dependencies) {
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			args[i] = resolveArgument(parameters[i], path, dependencies);
		}
		return args;
	}

	private Object resolveArgument(Parameter parameter, Set<String> path, List<Object> dependencies) {
		Class<?> type = parameter.getType();
		Use use = parameter.getAnnotation(Use.class);
		Object shared = resolveShared(type, use == null ? "" : use.name(), path);
		if (shared != null) {
			if (dependencies != null) {
				dependencies.add(shared);
			}
			return shared;
		}
		DefaultParameter defaultParameter = parameter.getAnnotation(DefaultParameter.class);
		if (defaultParameter != null) {
			return ObjectUtils.convertValue(defaultParameter.value(), type);
		}
		return missingArgument(parameter);
	}

	private static Object missingArgument(Parameter parameter) {
		throw new FrameworkException("Cannot resolve parameter '" + parameter.getName() + "' of type "
				+ parameter.getType().getName() + " in " + parameter.getDeclaringExecutable()
				+ ": no component is registered for it and it has no @DefaultParameter");
	}

	private Object resolveShared(Class<?> type, String name, Set<String> path) {
		if (name.isEmpty() && isValueType(type)) {
			return null;
		}
		String key = name.isEmpty() ? type.getName() : name;
		Object existing = store.getComponent(key);
		if (existing != null) {
			return existing;
		}
		BeanDefinition definition = byKey.get(key);
		if (definition == null && !name.isEmpty()) {
			definition = bySimple(name);
		}
		if (definition == null && name.isEmpty()) {
			definition = uniqueDefinition(type);
		}
		if (definition != null) {
			return singleton(definition, path);
		}
		return name.isEmpty() ? uniqueInstance(type) : null;
	}

	private BeanDefinition uniqueDefinition(Class<?> type) {
		BeanDefinition match = null;
		for (BeanDefinition candidate : definitions) {
			if (type.isAssignableFrom(candidate.type())) {
				if (match != null) {
					throw new FrameworkException("Multiple components match type " + type.getName() + ": " + match.key()
							+ ", " + candidate.key() + ". Use @Use(name = \"...\") to choose one.");
				}
				match = candidate;
			}
		}
		return match;
	}

	private int countAssignable(Class<?> type) {
		int count = 0;
		for (BeanDefinition candidate : definitions) {
			if (type.isAssignableFrom(candidate.type())) {
				count++;
			}
		}
		return count;
	}

	private Object uniqueInstance(Class<?> type) {
		List<?> found = store.findByType(type);
		if (found.size() > 1) {
			throw new FrameworkException(
					"Multiple components match type " + type.getName() + ". Use @Use(name = \"...\") to choose one.");
		}
		return found.isEmpty() ? null : found.get(0);
	}

	void injectFields(ClassMetadata metadata, Object instance, Set<String> path) {
		for (FieldPlan plan : metadata.fields()) {
			try {
				if (plan.property() != null) {
					injectProperty(plan, instance);
				}
				if (plan.use() != null) {
					injectDependency(plan, instance, path);
				}
				if (plan.storage() != null) {
					injectStorage(plan, instance, path);
				}
			} catch (IllegalAccessException e) {
				throw new FrameworkException("Field injection failed for " + plan.field().getName() + " in "
						+ plan.field().getDeclaringClass().getName(), e);
			}
		}
	}

	private void injectProperty(FieldPlan plan, Object instance) throws IllegalAccessException {
		Property property = plan.property();
		String value = PropertiesLoader.getProperty(property.key(), null);
		if (value == null) {
			if (property.defaultValue().isEmpty()) {
				throw new FrameworkException(
						"Property '" + property.key() + "' not found and no default value specified (field "
								+ plan.field().getDeclaringClass().getName() + "." + plan.field().getName() + ")");
			}
			value = property.defaultValue();
		}
		plan.field().set(instance, ObjectUtils.convertValue(value, plan.field().getType()));
	}

	private void injectDependency(FieldPlan plan, Object instance, Set<String> path) throws IllegalAccessException {
		Use use = plan.use();
		Field field = plan.field();
		Object component = use.share()
		        ? resolveShared(field.getType(), use.name(), path)
		        : prototype(prototypeType(field.getType(), use.name()), path);
		if (use.share() && starting && component != null) {
		    initialize(component, path);
		}
		
		if (component == null) {
			throw new IllegalStateException("Missing component for field: " + field.getName() + " in class: "
					+ field.getDeclaringClass().getName());
		}
		if (!field.getType().isInstance(component)) {
			throw new ClassCastException("Component type mismatch for field " + field.getName() + ". Expected: "
					+ field.getType().getName() + ", Got: " + component.getClass().getName());
		}
		field.set(instance, component);
	}

	private void injectStorage(FieldPlan plan, Object instance, Set<String> path) throws IllegalAccessException {
	    DataStorage dataStorage = plan.storage();
	    Field field = plan.field();
	    String key = dataStorage.key().isEmpty() ? field.getName() : dataStorage.key();
	    Object resolved = resolveShared(DataStorageService.class, "", path);
	    if (resolved == null) {
	        throw new FrameworkException("No DataStorageService is available for @DataStorage field "
	                + field.getDeclaringClass().getName() + "." + field.getName());
	    }
	    if (starting) {
	        initialize(resolved, path);
	    }
	    DataStorageService service = (DataStorageService) resolved;
	    Object value = service.load(key, field.getType());
	    if (value == null) {
	        value = !dataStorage.defaultValue().isEmpty()
	                ? ObjectUtils.convertValue(dataStorage.defaultValue(), field.getType())
	                : field.get(instance);
	    }
	    field.set(instance, value);
	}

	private Class<?> prototypeType(Class<?> fieldType, String name) {
		if (!name.isEmpty()) {
			Class<?> named = concreteType(name);
			if (named != null) {
				return named;
			}
		}
		if (fieldType.isInterface() || Modifier.isAbstract(fieldType.getModifiers())) {
			Class<?> registered = concreteType(fieldType.getName());
			if (registered != null) {
				return registered;
			}
			BeanDefinition definition = uniqueDefinition(fieldType);
			if (definition != null) {
				return concreteType(definition.key());
			}
			List<?> found = store.findByType(fieldType);
			if (found.size() == 1) {
				return found.get(0).getClass();
			}
			throw new IllegalStateException("Cannot determine concrete implementation for interface: "
					+ fieldType.getName() + ". Please specify a component name or register a concrete implementation.");
		}
		return fieldType;
	}

	private Class<?> concreteType(String name) {
		Object shared = store.getComponent(name);
		if (shared != null) {
			return shared.getClass();
		}
		BeanDefinition definition = byKey.get(name);
		if (definition == null) {
			definition = bySimple(name);
		}
		if (definition == null) {
			return null;
		}
		Object created = store.getComponent(definition.key());
		return created != null ? created.getClass() : definition.type();
	}

	private Object prototype(Class<?> type, Set<String> path) {
		String marker = "new " + type.getName();
		if (!path.add(marker)) {
			throw circular(path, marker);
		}
		try {
			Constructor<?> constructor = selectConstructor(type);
			List<Object> dependencies = new ArrayList<>();
			Object[] args = resolveArguments(constructor.getParameters(), path, dependencies);
			if (starting) {
			    for (Object dependency : dependencies) {
			        initialize(dependency, path);
			    }
			}
			Object instance = constructor.newInstance(args);
			
			ClassMetadata metadata = ClassMetadata.of(type);
			injectFields(metadata, instance, path);
			runInit(metadata, instance);
			return instance;
		} catch (ReflectiveOperationException e) {
			throw failure("Failed to create new instance of " + type.getName(), e);
		} finally {
			path.remove(marker);
		}
	}

	private void runInit(ClassMetadata metadata, Object instance) {
		for (Method method : metadata.initMethods()) {
			Object[] args = initArguments(method);
			try {
				method.invoke(instance, args);
			} catch (ReflectiveOperationException e) {
				throw failure("Init method failed: " + metadata.type().getName() + "." + method.getName(), e);
			}
		}
	}

	private Object[] initArguments(Method method) {
		Parameter[] parameters = method.getParameters();
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			Use use = parameter.getAnnotation(Use.class);
			Object component = resolveShared(parameter.getType(), use == null ? "" : use.name(), new LinkedHashSet<>());

			if (component != null) {
				args[i] = component;
				continue;
			}
			DefaultParameter defaultParameter = parameter.getAnnotation(DefaultParameter.class);
			if (defaultParameter == null) {
				throw new IllegalStateException(
						"Cannot resolve parameter: " + parameter.getName() + " of init method " + method.getName());
			}
			args[i] = ObjectUtils.convertValue(defaultParameter.value(), parameter.getType());
		}
		return args;
	}

	private void registerDestroy(ClassMetadata metadata, Object instance) {
	    for (Method method : metadata.destroyMethods()) {
	        cleanupRegistry.register(() -> invokeDestroy(metadata, method, instance), CleanupRegistry.BEAN_PRIORITY);
	    }
	}

	private static void invokeDestroy(ClassMetadata metadata, Method method, Object instance) {
		try {
			method.invoke(instance);
		} catch (ReflectiveOperationException e) {
			throw failure("PreDestroy failed: " + metadata.type().getName() + "." + method.getName(), e);
		}
	}

	private static boolean isValueType(Class<?> type) {
		return type.isPrimitive() || type.getName().startsWith("java.lang.");
	}

	private static FrameworkException circular(Set<String> path, String key) {
		return new FrameworkException("Circular dependency detected: " + String.join(" -> ", path) + " -> " + key);
	}

	private static FrameworkException failure(String message, Throwable cause) {
		Throwable root = cause instanceof InvocationTargetException ite && ite.getTargetException() != null
				? ite.getTargetException()
				: cause;
		if (root instanceof FrameworkException framework) {
			return framework;
		}
		if (root instanceof Exception exception) {
			return new FrameworkException(message, exception);
		}
		if (root instanceof Error error) {
			throw error;
		}
		return new FrameworkException(message + ": " + root);
	}

	public boolean isStarting() {
		return starting;
	}

	public void setStarting(boolean starting) {
		this.starting = starting;
	}
	
}