/*
 *
 * Copyright 2002-2004 The Ant-Contrib project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sf.antcontrib.cpptasks;

import net.sf.antcontrib.cpptasks.compiler.CommandLineCompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.CompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.ProcessorConfiguration;
import net.sf.antcontrib.cpptasks.gcc.GccCCompiler;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for CCTask.
 */
public final class TestCCTask {
    /**
     * Test that a target with no existing object file is
     * returned by getTargetsToBuildByConfiguration.
     */
    @Test
    public void testGetTargetsToBuildByConfiguration1() {
        CompilerConfiguration config1 =
                new CommandLineCompilerConfiguration(GccCCompiler.getInstance(),
                        "dummy", new File[0], new File[0], new File[0], "",
                        new String[0], new ProcessorParam[0], true, new String[0]);
        TargetInfo target1 = new TargetInfo(config1, new File[]{new File("src/foo.bar")},
                null, new File("foo.obj"), true);
        Hashtable<String, TargetInfo> targets = new Hashtable<String, TargetInfo>();
        targets.put(target1.getOutput().getName(), target1);
        Hashtable<ProcessorConfiguration, Vector<TargetInfo>> targetsByConfig =
                CCTask.getTargetsToBuildByConfiguration(targets);
        Vector<TargetInfo> targetsForConfig1 = targetsByConfig.get(config1);
        assertNotNull(targetsForConfig1);
        assertEquals(1, targetsForConfig1.size());
        TargetInfo targetx = targetsForConfig1.elementAt(0);
        assertSame(target1, targetx);
    }

    /**
     * Test that a target that is up to date is not returned by
     * getTargetsToBuildByConfiguration.
     */
    @Test
    public void testGetTargetsToBuildByConfiguration2() {
        CompilerConfiguration config1 =
                new CommandLineCompilerConfiguration(GccCCompiler.getInstance(),
                        "dummy", new File[0], new File[0], new File[0], "",
                        new String[0], new ProcessorParam[0], false, new String[0]);
        //
        //    target doesn't need to be rebuilt
        //
        TargetInfo target1 = new TargetInfo(config1, new File[]{new File("src/foo.bar")},
                null, new File("foo.obj"), false);
        Hashtable<String, TargetInfo> targets = new Hashtable<String, TargetInfo>();
        targets.put(target1.getOutput().getName(), target1);
        //
        //    no targets need to be built, return a zero-length hashtable
        //
        Hashtable<ProcessorConfiguration, Vector<TargetInfo>> targetsByConfig =
                CCTask.getTargetsToBuildByConfiguration(targets);
        assertEquals(0, targetsByConfig.size());
    }

    /**
     * Tests that the default value of failonerror is true.
     */
    @Test
    public void testGetFailOnError() {
        CCTask task = new CCTask();
        boolean failOnError = task.getFailonerror();
        assertTrue(failOnError);
    }

    /**
     * Tests that setting failonerror is effective.
     */
    @Test
    public void testSetFailOnError() {
        CCTask task = new CCTask();
        task.setFailonerror(false);
        boolean failOnError = task.getFailonerror();
        assertFalse(failOnError);
        task.setFailonerror(true);
        failOnError = task.getFailonerror();
        assertTrue(failOnError);
    }

    /**
     * Test checks for the presence of antlib.xml.
     *
     * @throws IOException if stream can't be closed.
     */
    @Test
    public void testAntlibXmlPresent() throws IOException {
        InputStream stream = TestCCTask.class.getClassLoader()
                .getResourceAsStream("net/sf/antcontrib/cpptasks/antlib.xml");
        if (stream != null) {
            stream.close();
        }
        assertNotNull("antlib.xml missing", stream);
    }
}
