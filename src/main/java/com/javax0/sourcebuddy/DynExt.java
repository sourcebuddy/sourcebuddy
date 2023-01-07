package com.javax0.sourcebuddy;

import java.lang.invoke.MethodHandles;

/**
 * This interface can play some role in your code providing lookup objects.
 * <p>
 * There are three different ways to provide a lookup object when creating a nest mate class.
 *
 * <ol>
 *     <li>Proprietary, some way... cook your own food and eat it.</li>
 *     <li>Implement this interface, call {@link #getLookup()} on your instance and pass the returned value to the
 *     method {@link Compiler#nest(MethodHandles.Lookup, MethodHandles.Lookup.ClassOption...)}</li>
 *     <li>Embed a static reference to a lookup object in the nesting host class and let SourceBuddy fetch it
 *     reflectively as documented in {@link LookupFetcher}.</li>
 * </ol>
 * <p>
 * Note<sub>1</sub>: Implementing this interface is not the same as the third approach, because that requires a static
 * field or method. Implementing this interface is technically the same as the first approach. The only difference is
 * that using the interface establishes a convention. When you see that a class implements {@code
 * com.javax0.sourcebuddy.DynExt} you know that it is practically the first approach with the addition that the name of
 * the mthod is {@link #getLookup()}.
 * <p>
 * Note<sub>2</sub>: This interface cannot implement this method as a default method. Doing so would result a lookup
 * object belonging to this package and not the package where the nesting host class is. This is because the call to
 * {@link MethodHandles#lookup()} is caller sensitive and the caller class would be this class if the method is default
 * and not the class that implements the interface.
 */
public interface DynExt {
    /**
     * Create and return a lookup object.
     *
     * The implementation may return a lookup object, which was created before.
     * The returned lookup object may be a value stored in a static field created during class initialization.
     *
     * Lookup objects remember the class that was calling the {@link MethodHandles#lookup()} method.
     * The created lookup object can be used to load hidden classes to be a new nest mate of the calling class.
     * There is no way to create a lookup object for a class from outside the class.
     *
     * @return the lookup object that was created form the implementing class.
     */
    MethodHandles.Lookup getLookup();
}
