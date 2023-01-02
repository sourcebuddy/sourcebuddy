package com.javax0.sourcebuddy;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

/**
 * Fetch a lookup object from a class.
 * <p>
 * When the SourceBuddy library loads hidden classes it needs a lookup object.
 * The library can create lookup objects with certain tricks, but these are in a different module.
 * Having a lookup object from the same package, but a different module may not work properly.
 * This is especially true when the new hidden class is going to be a nest mate of an already existing class.
 * In this case the lookup object has to come from the class that we add to (nest host class).
 * <p>
 * The application can get an instance of a lookup object from the nest host some way and pass it to the method
 * {@link Compiler#nest(MethodHandles.Lookup, MethodHandles.Lookup.ClassOption...) nest()}.
 * <p>
 * The application can also use the version of the method not needing a lookup argument.
 * In this case the library will use this class to find a lookup object calling {@link #nestLookup(StringJavaSource)}.
 */
public class LookupFetcher {

    /**
     * Get a lookup object reflectively from the nesting host class.
     * <p>
     * The method will use the standard class loader to get access to the class, which is already loaded and has the
     * same name as the dummy nesting host provided in the source object. If there is no such class the method returns
     * {@code null}.
     * <p>
     * Using the class the method tries to find field that is
     * <p>
     * FIELD
     * <ul>
     *     <li>{@code static}</li>
     *     <li>has a type compatible with {@link MethodHandles.Lookup}</li>
     * </ul>
     * <p>
     * If there is at least one it uses the one it finds first reading the value.
     * <p>
     * If there is no field matching the criteria then it tries to find a method, which is
     * <p>
     * METHOD
     * <ul>
     *     <li>{@code static}</li>
     *     <li>has a return type compatible with {@link MethodHandles.Lookup}</li>
     *     <li>has no argument</li>
     * </ul>
     * <p>
     * If there is at least one it uses the one it finds first invoking it and use the return value.
     * <p>
     * If all these fails, the return value is {@code null}.
     * <p>
     * If the source object already has a lookup object then all the calculation is skipped and the already provided
     * object is returned.
     *
     * @param source the source object that is the dummy for the nesting host
     * @return the lookup object or {@code null}
     */
    static MethodHandles.Lookup nestLookup(final StringJavaSource source) {
        if (source.lookup != null) {
            return source.lookup;
        }

        final Class<?> host;
        try {
            host = Class.forName(source.binaryName);
        } catch (ClassNotFoundException e) {
            return null;
        }
        return getLookupField(host).map(f -> tryGet(f, LookupFetcher::get))
                .orElseGet(() -> getLookupMethod(host).map(m -> tryGet(m, LookupFetcher::invoke)).orElse(null));
    }

    @FunctionalInterface
    private interface LookupGetter {
        MethodHandles.Lookup apply(AccessibleObject o) throws Exception;
    }

    private static MethodHandles.Lookup get(AccessibleObject field) {
        try {
            return (MethodHandles.Lookup) ((Field) field).get(null);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static MethodHandles.Lookup invoke(AccessibleObject method) {
        try {
            return (MethodHandles.Lookup) ((Method) method).invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static MethodHandles.Lookup tryGet(AccessibleObject lookup, LookupGetter getter) {
        try {
            lookup.setAccessible(true);
            return getter.apply(lookup);
        } catch (Exception ignored) {
            return null;
        }

    }

    private static Optional<Method> getLookupMethod(final Class<?> host) {
        return Arrays.stream(host.getDeclaredMethods())
                .filter(m -> (m.getModifiers() & Modifier.STATIC) > 0)
                .filter(m -> MethodHandles.Lookup.class.isAssignableFrom(m.getReturnType()))
                .filter(m -> m.getGenericParameterTypes().length == 0)
                .findFirst();
    }

    private static Optional<Field> getLookupField(final Class<?> host) {
        return Arrays.stream(host.getDeclaredFields())
                .filter(f -> (f.getModifiers() & Modifier.STATIC) > 0)
                .filter(f -> MethodHandles.Lookup.class.isAssignableFrom(f.getType()))
                .findFirst();
    }

}
