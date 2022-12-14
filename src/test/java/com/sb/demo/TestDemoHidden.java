package com.sb.demo;

import com.javax0.sourcebuddy.Compiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;

public class TestDemoHidden {

    @DisplayName("Load a hidden class without lookup object")
    @Test
    void loadHiddenClass() throws Exception {
        // snipline loadHidden_noLookup
        Compiler.java().from("Z", "class Z{}").hidden().compile().load();
    }

    @DisplayName("Load a hidden class with lookup object")
    @Test
    void loadHiddenClassWithLookup() throws Exception {
        // snipline loadHidden_Lookup
        Compiler.java().from( "package com.sb.demo;class Z{}").hidden(MethodHandles.lookup()).compile().load();
    }

}
