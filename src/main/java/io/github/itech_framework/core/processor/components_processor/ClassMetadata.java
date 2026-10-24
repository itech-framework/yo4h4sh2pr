package io.github.itech_framework.core.processor.components_processor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.itech_framework.core.annotations.components.Component;
import io.github.itech_framework.core.annotations.components.levels.Configuration;
import io.github.itech_framework.core.annotations.components.policy.DisableLoaded;
import io.github.itech_framework.core.annotations.dependency.Use;
import io.github.itech_framework.core.annotations.methods.OnInit;
import io.github.itech_framework.core.annotations.methods.PreDestroy;
import io.github.itech_framework.core.annotations.properties.Property;
import io.github.itech_framework.core.annotations.storage.DataStorage;
import io.github.itech_framework.core.utils.AnnotationUtils;

record ClassMetadata(Class<?> type, boolean disabled, List<FieldPlan> fields, List<Method> initMethods,
                     List<Method> destroyMethods, List<Method> factoryMethods) {

    record FieldPlan(Field field, Property property, Use use, DataStorage storage) {}

    private static final Map<Class<?>, ClassMetadata> CACHE = new ConcurrentHashMap<>();

    static ClassMetadata of(Class<?> type) {
        return CACHE.computeIfAbsent(type, ClassMetadata::build);
    }

    private static ClassMetadata build(Class<?> type) {
        List<FieldPlan> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                Property property = field.getAnnotation(Property.class);
                Use use = field.getAnnotation(Use.class);
                DataStorage storage = field.getAnnotation(DataStorage.class);
                if (property == null && use == null && storage == null) {
                    continue;
                }
                field.setAccessible(true);
                fields.add(new FieldPlan(field, property, use, storage));
            }
        }

        boolean configuration = type.isAnnotationPresent(Configuration.class);
        List<Method> init = new ArrayList<>();
        List<Method> destroy = new ArrayList<>();
        List<Method> factories = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(OnInit.class)) {
                method.setAccessible(true);
                init.add(method);
            }
            if (method.isAnnotationPresent(PreDestroy.class)) {
                method.setAccessible(true);
                destroy.add(method);
            }
            if (configuration && method.isAnnotationPresent(Component.class) && method.getReturnType() != void.class) {
                method.setAccessible(true);
                factories.add(method);
            }
        }
        init.sort(Comparator.comparingInt(m -> m.getAnnotation(OnInit.class).order()));
        factories.sort(Comparator.comparing(Method::getName).thenComparingInt(Method::getParameterCount));

        return new ClassMetadata(type, AnnotationUtils.hasAnnotation(type, DisableLoaded.class),
                List.copyOf(fields), List.copyOf(init), List.copyOf(destroy), List.copyOf(factories));
    }
}