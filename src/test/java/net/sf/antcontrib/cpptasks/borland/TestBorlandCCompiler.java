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
package net.sf.antcontrib.cpptasks.borland;

import net.sf.antcontrib.cpptasks.compiler.AbstractProcessor;
import net.sf.antcontrib.cpptasks.compiler.TestAbstractCompiler;
import org.junit.Test;

/**
 * Borland C++ Compiler adapter tests
 * <p>
 * Override create to test concrete compiler implementations
 * </p>
 */
public class TestBorlandCCompiler extends TestAbstractCompiler {
    protected AbstractProcessor create() {
        return BorlandCCompiler.getInstance();
    }

    protected String getObjectExtension() {
        return ".obj";
    }

    @Test
    public void testGetIdentifier() {
    }
}
