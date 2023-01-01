//snippet OuterClass
package com.javax0.sourcebuddytest;

import com.javax0.sourcebuddy.DynExt;

import java.lang.invoke.MethodHandles;

public class OuterClass implements DynExt {

    private int z = 55;

    public int getZ() {
        return z;
    }

    public MethodHandles.Lookup getLookup(){
        return MethodHandles.lookup();
    }
}
//end snippet