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

import net.sf.antcontrib.cpptasks.ProcessorParam;
import net.sf.antcontrib.cpptasks.gcc.GccCCompiler;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestCommandLineCompilerConfiguration
        extends
        TestCompilerConfiguration {
    private CommandLineCompiler compiler;
    private String compilerId;

    @Before
    public void startUp() {
        compiler = GccCCompiler.getInstance();
        compilerId = compiler.getIdentifier();
    }

    protected CompilerConfiguration create() {
        return new CommandLineCompilerConfiguration(compiler, "dummy",
                new File[0], new File[0], new File[0], "",
                new String[]{"/Id:/gcc"}, new ProcessorParam[0], false,
                new String[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullCompiler() {
        new CommandLineCompilerConfiguration(null, "dummy", new File[0],
                new File[0], new File[0], "", new String[0],
                new ProcessorParam[0], false, new String[0]);
    }

    @Test
    public void testGetIdentifier() {
        CompilerConfiguration config = create();
        String id = config.getIdentifier();
        assertEquals("dummy", id);
    }

    @Test
    public void testToString() {
        CompilerConfiguration config = create();
        String toString = config.toString();
        assertEquals("dummy", toString);
    }
}
