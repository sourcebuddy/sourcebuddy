//snippet OuterClass2
package com.javax0.sourcebuddytest;

import com.javax0.sourcebuddy.DynExt;

import java.lang.invoke.MethodHandles;

public class OuterClass2 implements DynExt {

    private int z = 55;

    private void inc(){
        z++;
    }

    public int getZ() {
        return z;
    }

    public MethodHandles.Lookup getLookup(){
        return MethodHandles.lookup();
    }
    public static MethodHandles.Lookup lookup(){
        return MethodHandles.lookup();
    }
}
//end snippet