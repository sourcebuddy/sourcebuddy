package com.javax0.sourcebuddy;

import org.junit.jupiter.api.Test;

public class TestClassPathCollector {

    @Test
    void testClassPathCollector() throws Exception {
        // there is not much to test here, but at least we can run the code, and it does not throw an exception
        final var classpath = ClasspathCollector.getEntries();
    }


}
