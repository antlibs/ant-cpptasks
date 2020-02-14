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

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CompilerDef;
import net.sf.antcontrib.cpptasks.ProcessorDef;
import net.sf.antcontrib.cpptasks.TargetDef;
import net.sf.antcontrib.cpptasks.VersionInfo;
import net.sf.antcontrib.cpptasks.parser.CParser;
import net.sf.antcontrib.cpptasks.parser.Parser;
import org.apache.tools.ant.BuildException;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test for abstract compiler class
 * <p>
 * Override create to test concrete compiler implementations
 * </p>
 */
public class TestAbstractCompiler extends TestAbstractProcessor {
    private class DummyAbstractCompiler extends AbstractCompiler {
        public DummyAbstractCompiler() {
            super(new String[]{".cpp", ".c"},
                    new String[]{".hpp", ".h", ".inl"}, ".o");
        }

        public void compile(CCTask task, File[] srcfile, File[] outputfile,
                            CompilerConfiguration config) throws BuildException {
            throw new BuildException("Not implemented");
        }

        public CompilerConfiguration createConfiguration(CCTask task, LinkType linkType,
                                                         ProcessorDef[] def1,
                                                         CompilerDef def2,
                                                         TargetDef targetPlatform,
                                                         VersionInfo versionInfo) {
            return null;
        }

        public Parser createParser(File file) {
            return new CParser();
        }

        public String getIdentifier() {
            return "dummy";
        }

        public Linker getLinker(LinkType type) {
            return null;
        }
    }

    protected AbstractProcessor create() {
        return new DummyAbstractCompiler();
    }

    protected String getObjectExtension() {
        return ".o";
    }

    @Test
    public void testCanParseTlb() {
        AbstractCompiler compiler = (AbstractCompiler) create();
        assertFalse(compiler.canParse(new File("sample.tlb")));
    }

    @Test
    public void testGetOutputFileName1() {
        AbstractProcessor compiler = create();
        String[] output = compiler.getOutputFileNames("c:/foo\\bar\\hello.c", null);
        assertEquals("hello" + getObjectExtension(), output[0]);
        output = compiler.getOutputFileNames("c:/foo\\bar/hello.c", null);
        assertEquals("hello" + getObjectExtension(), output[0]);
        output = compiler.getOutputFileNames("hello.c", null);
        assertEquals("hello" + getObjectExtension(), output[0]);
        output = compiler.getOutputFileNames("c:/foo\\bar\\hello.h", null);
        assertEquals(0, output.length);
        output = compiler.getOutputFileNames("c:/foo\\bar/hello.h", null);
        assertEquals(0, output.length);
    }
}
