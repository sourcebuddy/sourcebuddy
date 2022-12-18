package com.javax0.sourcebuddy;

import javax0.jamal.DocumentConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestCreateAdocs {

    /**
     * Although the IntelliJ plugin updates the README.adoc file all the time we open the README.adoc.jam file
     * some content comes from external files, like the version of the project, and it does not get updates
     * unless we explicitly update the readme during the build.
     *
     * Having this "test" ensures that the README.adoc reflects the latest build and not the latest edit.
     *
     * @throws Exception if there is an error in the documentation that prevents the compilation
     */
    @DisplayName("Convert the README.adoc.jam to README.adoc")
    @Test
    void convertReadme()throws Exception{
        DocumentConverter.convert("README.adoc.jam");
    }
}
