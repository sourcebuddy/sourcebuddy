package com.javax0.sourcebuddy;

import java.lang.invoke.MethodHandles;

/**
 * There are three different ways to provide a lookup object when creating a nest mate class.
 *
 * <ol>
 *     <li>Proprietary, some way... cook your own food and eat it.</li>
 *     <li>Implement this interface, call {@link #getLookup()} on your instance and pass the returned value to the
 *     method {@link Compiler#nest(MethodHandles.Lookup, MethodHandles.Lookup.ClassOption...)}</li>
 *     <li>Embed a static lookup value in the nesting host class and let SourceBuddy fetch it reflectively as
 *     documented in {@link LookupFetcher}.</li>
 * </ol>
 * <p>
 * Note<sub>1</sub>: Implementing this interface is not the same as the third approach, because that requires a static
 * field or method.
 * <p>
 * Note<sub>2</sub>: This interface cannot implement this method as a default method. Doing so would result a lookup
 * object belonging to this package and not the package where the nesting host class is. This is because the call to
 * {@link MethodHandles#lookup()} is caller sensitive and the caller class would be this class if the method is default
 * and not the class that implements the interface.
 */
public interface DynExt {
    MethodHandles.Lookup getLookup();
}
