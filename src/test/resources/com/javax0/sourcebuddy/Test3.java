package com.javax0.sourcebuddy;

/**
 * This is the same sample test class as Test1, but during some tests the compiled version of
 * Test1 (Test1.class file) is saved into ./target/test-classes and because of that the class loader loads
 * it before the {@link ByteClassLoader} could load it hidden.
 */
public class Test3 {
	public String a() {
		return "x";
	}
}
