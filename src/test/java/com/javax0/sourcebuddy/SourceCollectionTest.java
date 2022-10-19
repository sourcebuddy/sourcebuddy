package com.javax0.sourcebuddy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Perform the tests to see that the files  are collected from a directory properly
 */
public class SourceCollectionTest {

    @Test
    void collectFromResources() throws Exception {
        final var sut = Compiler.java().from(Paths.get("./src/test/resources/file_collect_test"));
        final var sourcesField = Compiler.class.getDeclaredField("sources");
        sourcesField.setAccessible(true);
        final var sources = (List<StringJavaSource>)sourcesField.get(sut);
        Assertions.assertEquals(List.of("DefaultPackage",
                "com.verhas.Another",
                "com.javax0.sourcebuddy.Main",
                "com.javax0.Another"),
                sources.stream().map(sjs->sjs.binaryName).collect(Collectors.toList()));
    }
}
