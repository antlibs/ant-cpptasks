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
import net.sf.antcontrib.cpptasks.compiler.Compiler;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.devstudio.DevStudioCCompiler;
import net.sf.antcontrib.cpptasks.gcc.GccCCompiler;
import net.sf.antcontrib.cpptasks.types.CompilerArgument;
import net.sf.antcontrib.cpptasks.types.ConditionalPath;
import net.sf.antcontrib.cpptasks.types.DefineArgument;
import net.sf.antcontrib.cpptasks.types.DefineSet;
import net.sf.antcontrib.cpptasks.types.IncludePath;
import net.sf.antcontrib.cpptasks.types.SystemIncludePath;
import net.sf.antcontrib.cpptasks.types.UndefineArgument;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Tests for CompilerDef.
 */
public final class TestCompilerDef extends TestProcessorDef {
    /**
     * Creates a new processor.
     *
     * @return new processor
     */
    protected ProcessorDef create() {
        return new CompilerDef();
    }

    /**
     * <p>
     * This method tests CompilerDef.getActiveDefines.
     * </p>
     * <p>
     * A CompilerDef is created similar to what would be created for
     * </p>
     * <pre>
     * &lt;cc&gt;
     *   &lt;defineset&gt;
     *     &lt;define name=&quot;DEBUG&quot; if=&quot;debug&quot;/&gt;
     *     &lt;define name=&quot;NDEBUG&quot; unless=&quot;debug&quot;/&gt;
     *   &lt;/defineset&gt;
     * &lt;/cc&gt;
     * </pre>
     * <p>
     * Then getActiveDefines is called for a project without and with the
     * "debug" property defined. Return value from getActiveDefines should
     * contain one member
     * </p>
     */
    @Test
    public void testGetActiveDefines() {
        Project project = new Project();
        CompilerDef def = new CompilerDef();
        def.setProject(project);
        DefineSet defset = new DefineSet();
        DefineArgument arg1 = new DefineArgument();
        arg1.setName("DEBUG");
        arg1.setIf("debug");
        defset.addDefine(arg1);
        DefineArgument arg2 = new DefineArgument();
        arg2.setName("NDEBUG");
        arg2.setUnless("debug");
        defset.addDefine(arg2);
        def.addConfiguredDefineset(defset);
        //
        //  Evaluate without "debug" set
        //
        UndefineArgument[] activeArgs = def.getActiveDefines();
        assertEquals(1, activeArgs.length);
        assertEquals("NDEBUG", activeArgs[0].getName());
        //
        //  Set the "debug" property
        //
        project.setProperty("debug", "");
        activeArgs = def.getActiveDefines();
        assertEquals(1, activeArgs.length);
        assertEquals("DEBUG", activeArgs[0].getName());
    }

    /**
     * <p>
     * This method tests CompilerDef.getActiveIncludePath.
     * <p>
     * A CompilerDef is created similar to what would be created for
     * </p>
     * <pre>
     * &lt;cc&gt;
     *   &lt;includepath location=&quot;..&quot; if=&quot;debug&quot;/&gt;
     * &lt;/cc&gt;
     * </pre>
     * <p>
     * and is evaluate for a project without and without "debug" set
     * </p>
     */
    @Test
    public void testGetActiveIncludePaths() {
        Project project = new Project();
        CompilerDef def = new CompilerDef();
        def.setProject(project);
        ConditionalPath path = def.createIncludePath();
        path.setLocation(new File(".."));
        path.setIf("debug");
        //
        //  Evaluate without "debug" set
        //
        String[] includePaths = def.getActiveIncludePaths();
        assertEquals(0, includePaths.length);
        //
        //  Set the "debug" property
        //
        project.setProperty("debug", "");
        includePaths = def.getActiveIncludePaths();
        assertEquals(1, includePaths.length);
    }

    /**
     * Tests that setting classname to the Gcc compiler is effective.
     */
    @Test
    public void testGetGcc() {
        CompilerDef compilerDef = (CompilerDef) create();
        compilerDef.setClassname("net.sf.antcontrib.cpptasks.gcc.GccCCompiler");
        Compiler comp = (Compiler) compilerDef.getProcessor();
        assertNotNull(comp);
        assertSame(GccCCompiler.getInstance(), comp);
    }

    /**
     * Tests that setting classname to the MSVC compiler is effective.
     */
    @Test
    public void testGetMSVC() {
        CompilerDef compilerDef = (CompilerDef) create();
        compilerDef.setClassname("net.sf.antcontrib.cpptasks.devstudio.DevStudioCCompiler");
        Compiler comp = (Compiler) compilerDef.getProcessor();
        assertNotNull(comp);
        assertSame(DevStudioCCompiler.getInstance(), comp);
    }

    /**
     * Tests that setting classname to an bogus class name results in a
     * BuildException.
     */
    @Test(expected = BuildException.class)
    public void testUnknownClass() {
        CompilerDef compilerDef = (CompilerDef) create();
        compilerDef.setClassname("net.sf.antcontrib.cpptasks.bogus.BogusCompiler");
    }

    /**
     * Test that setting classname to a class that doesn't support Compiler
     * throws a BuildException.
     */
    @Test(expected = BuildException.class)
    public void testWrongType() {
        CompilerDef compilerDef = (CompilerDef) create();
        compilerDef.setClassname("net.sf.antcontrib.cpptasks.devstudio.DevStudioLinker");
    }

    /**
     * Gets the command line arguments that precede filenames.
     *
     * @param processor processor under test
     * @return command line arguments
     */
    protected String[] getPreArguments(final ProcessorDef processor) {
        return ((CommandLineCompilerConfiguration) getConfiguration(processor)).getPreArguments();
    }

    /**
     * Tests if a fileset enclosed in the base compiler definition is effective.
     *
     * @throws IOException if unable to create or delete a temporary file
     */
    @Test
    public void testExtendsFileSet() throws IOException {
        super.testExtendsFileSet(File.createTempFile("cpptaskstest", ".cpp"));
    }

    /**
     * Tests if the rebuild attribute of the base compiler definition is
     * effective.
     */
    @Test
    public void testExtendsRebuild() {
        testExtendsRebuild(new CompilerDef());
    }

    /**
     * Tests that compilerarg's contained in the base compiler definition are
     * effective.
     */
    @Test
    public void testExtendsCompilerArgs() {
        CompilerDef baseLinker = new CompilerDef();
        CompilerArgument linkerArg = new CompilerArgument();
        linkerArg.setValue("/base");
        baseLinker.addConfiguredCompilerArg(linkerArg);
        CompilerDef extendedLinker = (CompilerDef) createExtendedProcessorDef(baseLinker);
        String[] preArgs = getPreArguments(extendedLinker);
        assertEquals(2, preArgs.length);
        assertEquals("/base", preArgs[0]);
    }

    /**
     * Tests that defineset's contained in the base compiler definition are
     * effective.
     */
    @Test
    public void testExtendsDefineSet() {
        CompilerDef baseCompiler = new CompilerDef();
        DefineSet defSet = new DefineSet();
        DefineArgument define = new DefineArgument();
        define.setName("foo");
        define.setValue("bar");
        defSet.addDefine(define);
        baseCompiler.addConfiguredDefineset(defSet);
        CompilerDef extendedCompiler = (CompilerDef) createExtendedProcessorDef(baseCompiler);
        String[] preArgs = getPreArguments(extendedCompiler);
        assertEquals(2, preArgs.length);
        assertEquals("-Dfoo=bar", preArgs[1]);
    }

    /**
     * Tests that includepath's contained in the base compiler definition are
     * effective.
     */
    @Test
    public void testExtendsIncludePath() {
        CompilerDef baseCompiler = new CompilerDef();
        CompilerDef extendedCompiler = (CompilerDef) createExtendedProcessorDef(baseCompiler);
        IncludePath path = baseCompiler.createIncludePath();
        path.setPath("/tmp");
        String[] preArgs = getPreArguments(extendedCompiler);
        assertEquals(2, preArgs.length);
        assertEquals("-I", preArgs[1].substring(0, 2));
    }

    /**
     * Tests that sysincludepath's contained in the base compiler definition are
     * effective.
     */
    @Test
    public void testExtendsSysIncludePath() {
        CompilerDef baseCompiler = new CompilerDef();
        CompilerDef extendedCompiler = (CompilerDef) createExtendedProcessorDef(baseCompiler);
        SystemIncludePath path = baseCompiler.createSysIncludePath();
        path.setPath("/tmp");
        String[] preArgs = getPreArguments(extendedCompiler);
        assertEquals(2, preArgs.length);
        assertEquals("-I", preArgs[1].substring(0, 2));
    }

    /**
     * Sets the name attribute.
     *
     * @param compiler compiler under test
     * @param name     compiler name
     */
    private static void setCompilerName(final CompilerDef compiler, final String name) {
        CompilerEnum compilerName = new CompilerEnum();
        compilerName.setValue(name);
        compiler.setName(compilerName);
    }

    /**
     * Tests that the extend attribute of the base compiler definition is
     * effective.
     */
    @Test
    public void testExtendsExceptions() {
        CompilerDef baseCompiler = new CompilerDef();
        baseCompiler.setExceptions(true);
        CompilerDef extendedCompiler = (CompilerDef) createExtendedProcessorDef(baseCompiler);
        setCompilerName(extendedCompiler, "msvc");
        String[] preArgs = getPreArguments(extendedCompiler);
        assertEquals("/EHsc", preArgs[2]);
    }

    /**
     * Tests that the multithread attribute of the base compiler definition is
     * effective.
     */
    @Test
    public void testExtendsMultithreaded() {
        CompilerDef baseCompiler = new CompilerDef();
        baseCompiler.setMultithreaded(false);
        CompilerDef extendedCompiler = (CompilerDef) createExtendedProcessorDef(baseCompiler);
        setCompilerName(extendedCompiler, "msvc");
        CCTask cctask = new CCTask();
        LinkType linkType = new LinkType();
        linkType.setStaticRuntime(true);
        CommandLineCompilerConfiguration config = (CommandLineCompilerConfiguration)
                extendedCompiler.createConfiguration(cctask, linkType, null, null, null);
        String[] preArgs = config.getPreArguments();
        assertEquals("/ML", preArgs[3]);
    }

    /**
     * Tests that the name attribute in the base compiler is effective.
     */
    @Test
    public void testExtendsName() {
        CompilerDef baseCompiler = new CompilerDef();
        setCompilerName(baseCompiler, "msvc");
        CompilerDef extendedCompiler = (CompilerDef) createExtendedProcessorDef(baseCompiler);
        extendedCompiler.setExceptions(true);
        String[] preArgs = getPreArguments(extendedCompiler);
        assertEquals("/EHsc", preArgs[2]);
    }

    /**
     * Tests that the classname attribute in the base compiler is effective.
     */
    @Test
    public void testExtendsClassname() {
        CompilerDef baseCompiler = new CompilerDef();
        baseCompiler.setClassname("net.sf.antcontrib.cpptasks.devstudio.DevStudioCCompiler");
        CompilerDef extendedCompiler = (CompilerDef) createExtendedProcessorDef(baseCompiler);
        extendedCompiler.setExceptions(true);
        String[] preArgs = getPreArguments(extendedCompiler);
        assertEquals("/EHsc", preArgs[2]);
    }
}
