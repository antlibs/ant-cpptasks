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
package net.sf.antcontrib.cpptasks;

import net.sf.antcontrib.cpptasks.compiler.CompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.ProgressMonitor;
import org.apache.tools.ant.BuildException;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * A description of a file built or to be built
 */
public class TestTargetInfo {
    private class DummyConfiguration implements CompilerConfiguration {
        public int bid(String filename) {
            return 1;
        }

        public void close() {
        }

        public void compile(CCTask task, File workingDir, String[] source, boolean relentless,
                            ProgressMonitor monitor) throws BuildException {
            throw new BuildException("Not implemented");
        }

        public CompilerConfiguration[] createPrecompileConfigurations(File file,
                                                                      String[] exceptFiles) {
            return null;
        }

        public String getIdentifier() {
            return "dummy";
        }

        public String[] getIncludeDirectories() {
            return new String[0];
        }

        public String getIncludePathIdentifier() {
            return "dummyIncludePath";
        }

        public String[] getOutputFileNames(String inputFile, VersionInfo versionInfo) {
            return new String[0];
        }

        public CompilerParam getParam(String name) {
            return null;
        }

        public ProcessorParam[] getParams() {
            return new ProcessorParam[0];
        }

        public boolean getRebuild() {
            return false;
        }

        public boolean isPrecompileGeneration() {
            return true;
        }

        public DependencyInfo parseIncludes(CCTask task, File baseDir, File file) {
            return null;
        }
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullConfig() {
        new TargetInfo(null, new File[]{new File("")}, null, new File(""), false);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullOutput() {
        CompilerConfiguration config = new DummyConfiguration();
        new TargetInfo(config, new File[]{new File("")}, null, null, false);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullSource() {
        CompilerConfiguration config = new DummyConfiguration();
        new TargetInfo(config, null, null, new File(""), false);
    }

    @Test
    public void testGetRebuild() {
        CompilerConfiguration config = new DummyConfiguration();
        TargetInfo targetInfo = new TargetInfo(config, new File[]{new File("FoO.BaR")},
                null, new File("foo.o"), false);
        assertFalse(targetInfo.getRebuild());
        targetInfo = new TargetInfo(config, new File[]{new File("FoO.BaR")},
                null, new File("foo.o"), true);
        assertTrue(targetInfo.getRebuild());
    }

   @Test
    public void testGetSource() {
        CompilerConfiguration config = new DummyConfiguration();
        TargetInfo targetInfo = new TargetInfo(config, new File[]{new File("FoO.BaR")},
                null, new File("foo.o"), false);
        String source = targetInfo.getSources()[0].getName();
        assertEquals(source, "FoO.BaR");
    }

    @Test
    public void testHasSameSource() {
        CompilerConfiguration config = new DummyConfiguration();
        TargetInfo targetInfo = new TargetInfo(config, new File[]{new File("foo.bar")},
                null, new File("foo.o"), false);
        assertEquals(targetInfo.getSources()[0], new File("foo.bar"));
        assertNotEquals(targetInfo.getSources()[0], new File("boo.far"));
    }

    @Test
    public void testMustRebuild() {
        CompilerConfiguration config = new DummyConfiguration();
        TargetInfo targetInfo = new TargetInfo(config, new File[]{new File("FoO.BaR")},
                null, new File("foo.o"), false);
        assertFalse(targetInfo.getRebuild());
        targetInfo.mustRebuild();
        assertTrue(targetInfo.getRebuild());
    }
}
