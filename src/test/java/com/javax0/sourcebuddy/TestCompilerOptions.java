package com.javax0.sourcebuddy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test that the API works with the compiler options.
 * This test may fail in later versions using different Java version since the compiler options may be different.
 */
public class TestCompilerOptions {


    @Test
    void testOlderTarget() throws Exception {
        final var v = Runtime.version().version().get(0);
        if (v < 20) {
            var byteCode = Compiler.java().from("""
                    package a; class B{}
                    """).options("--source", "7", "--target", "8").compile().get();
            Assertions.assertEquals(52, ByteCodeGouger.major(byteCode));
            Assertions.assertEquals(0, ByteCodeGouger.minor(byteCode));
            byteCode = Compiler.java().from("""
                    package a; class B{}
                    """).options("--source", "7", "--target", "7").compile().get();
            Assertions.assertEquals(51, ByteCodeGouger.major(byteCode));
            Assertions.assertEquals(0, ByteCodeGouger.minor(byteCode));
        }
        var byteCode = Compiler.java().from("""
                    package a; class B{}
                    """).options("--source", "8", "--target", "8").compile().get();
        Assertions.assertEquals(52, ByteCodeGouger.major(byteCode));
        Assertions.assertEquals(0, ByteCodeGouger.minor(byteCode));
    }
}
