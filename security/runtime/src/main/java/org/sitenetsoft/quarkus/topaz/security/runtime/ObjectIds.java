package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.sitenetsoft.quarkus.topaz.security.TopazCheck;
import org.sitenetsoft.quarkus.topaz.security.TopazObjectId;

/**
 * Resolves the object id of a {@link TopazCheck}.
 */
final class ObjectIds {

    // the @TopazObjectId getter (preferred) or field of a class, searched up its superclasses
    private static final ClassValue<Optional<AccessibleObject>> MEMBERS = new ClassValue<>() {
        @Override
        protected Optional<AccessibleObject> computeValue(Class<?> type) {
            return Optional.ofNullable(findMember(type));
        }
    };

    private ObjectIds() {
    }

    /**
     * @return the object id: {@link TopazCheck#objectId()} when not empty, else the {@link TopazObjectId} argument,
     *         else the {@link TopazObjectId} field or getter of the first argument whose class declares one
     * @throws IllegalStateException when no object id can be resolved, it is null or blank, or two parameters are
     *         annotated
     */
    static String resolve(TopazCheck binding, Method method, Object[] args) {
        if (!binding.objectId().isEmpty()) {
            return binding.objectId();
        }
        return require(fromArguments(method, args), method);
    }

    private static Object fromArguments(Method method, Object[] args) {
        Annotation[][] annotations = method.getParameterAnnotations();
        int annotated = -1;
        for (int i = 0; i < annotations.length; i++) {
            for (Annotation a : annotations[i]) {
                if (a instanceof TopazObjectId) {
                    if (annotated >= 0) {
                        throw new IllegalStateException("@TopazObjectId on more than one parameter of " + name(method));
                    }
                    annotated = i;
                }
            }
        }
        if (annotated >= 0) {
            return args[annotated];
        }
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            Optional<AccessibleObject> member = MEMBERS.get(types[i]);
            if (member.isPresent()) {
                return args[i] == null ? null : read(member.get(), args[i], method);
            }
        }
        throw new IllegalStateException("@TopazCheck on " + name(method)
                + " has no objectId, no @TopazObjectId parameter and no argument with a @TopazObjectId field or getter");
    }

    private static String require(Object value, Method method) {
        String id = value == null ? null : value.toString();
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("@TopazCheck object id of " + name(method) + " is null or blank");
        }
        return id;
    }

    private static Object read(AccessibleObject member, Object target, Method method) {
        try {
            if (member instanceof Method getter) {
                return getter.invoke(target);
            }
            return ((Field) member).get(target);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot read the @TopazObjectId of " + name(method), e);
        }
    }

    private static AccessibleObject findMember(Class<?> type) {
        if (type.isPrimitive() || type.isArray() || type.isInterface()) {
            return null;
        }
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(TopazObjectId.class) && m.getParameterCount() == 0 && !m.isSynthetic()) {
                    m.setAccessible(true);
                    return m;
                }
            }
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(TopazObjectId.class)) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return null;
    }

    private static String name(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }
}
