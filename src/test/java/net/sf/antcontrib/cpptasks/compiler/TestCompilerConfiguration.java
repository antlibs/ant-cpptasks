/*
 *
 * Copyright 2002-2004 The Ant-Contrib project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.antcontrib.cpptasks.compiler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
/**
 *
 */
public abstract class TestCompilerConfiguration {
   protected abstract CompilerConfiguration create();
    public String getObjectFileExtension() {
        return ".o";
    }

    @Test
    public void testBid() {
        CompilerConfiguration compiler = create();
        int bid = compiler.bid("c:/foo\\bar\\hello.c");
        assertEquals(100, bid);
        bid = compiler.bid("c:/foo\\bar/hello.c");
        assertEquals(100, bid);
        bid = compiler.bid("c:/foo\\bar\\hello.h");
        assertEquals(1, bid);
        bid = compiler.bid("c:/foo\\bar/hello.h");
        assertEquals(1, bid);
        bid = compiler.bid("c:/foo\\bar/hello.pas");
        assertEquals(0, bid);
        bid = compiler.bid("c:/foo\\bar/hello.java");
        assertEquals(0, bid);
    }

    @Test
    public void testGetOutputFileName1() {
        CompilerConfiguration compiler = create();
        String input = "c:/foo\\bar\\hello.c";
        //
        //  may cause IllegalStateException since
        //     setPlatformInfo has not been called
        try {
            String[] output = compiler.getOutputFileNames(input, null);
        } catch (java.lang.IllegalStateException ex) {
        }
    }

    @Test
    public void testGetOutputFileName2() {
        CompilerConfiguration compiler = create();
        String[] output = compiler.getOutputFileNames("c:/foo\\bar\\hello.c", null);
        assertEquals("hello" + getObjectFileExtension(), output[0]);
        output = compiler.getOutputFileNames("c:/foo\\bar/hello.c", null);
        assertEquals("hello" + getObjectFileExtension(), output[0]);
        output = compiler.getOutputFileNames("hello.c", null);
        assertEquals("hello" + getObjectFileExtension(), output[0]);
        output = compiler.getOutputFileNames("c:/foo\\bar\\hello.h", null);
        assertEquals(0, output.length);
        output = compiler.getOutputFileNames("c:/foo\\bar/hello.h", null);
        assertEquals(0, output.length);
    }
}
