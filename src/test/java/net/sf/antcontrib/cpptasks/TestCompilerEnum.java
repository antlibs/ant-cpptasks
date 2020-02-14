/*
 *
 * Copyright 2002-2005 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks;

import org.apache.tools.ant.BuildException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for CompilerEnum.
 */
public class TestCompilerEnum {
   /**
    * Test that "gcc" is recognized as a compiler enum.
    */
   @Test
    public void testCompilerEnum1() {
        CompilerEnum compilerEnum = new CompilerEnum();
        compilerEnum.setValue("gcc");
        assertTrue(compilerEnum.getIndex() >= 0);
    }

    /**
     * Test that "bogus" is not recognized as a compiler enum.
     */
    @Test(expected = BuildException.class)
    public void testCompilerEnum2() {
        CompilerEnum compilerEnum = new CompilerEnum();
        compilerEnum.setValue("bogus");
    }
}
