/*
 *
 * Copyright 2002-2007 The Ant-Contrib project
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

import net.sf.antcontrib.cpptasks.borland.TestBorlandCCompiler;
import net.sf.antcontrib.cpptasks.compiler.TestAbstractCompiler;
import net.sf.antcontrib.cpptasks.compiler.TestAbstractLinker;
import net.sf.antcontrib.cpptasks.compiler.TestAbstractProcessor;
import net.sf.antcontrib.cpptasks.compiler.TestCommandLineCompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.TestLinkType;
import net.sf.antcontrib.cpptasks.devstudio.TestDevStudio2005CCompiler;
import net.sf.antcontrib.cpptasks.devstudio.TestDevStudioCCompiler;
import net.sf.antcontrib.cpptasks.devstudio.TestDevStudioLinker;
import net.sf.antcontrib.cpptasks.gcc.TestAbstractArLibrarian;
import net.sf.antcontrib.cpptasks.gcc.TestAbstractLdLinker;
import net.sf.antcontrib.cpptasks.gcc.TestGccCCompiler;
import net.sf.antcontrib.cpptasks.gcc.TestGccLinker;
import net.sf.antcontrib.cpptasks.hp.TestaCCCompiler;
import net.sf.antcontrib.cpptasks.ibm.TestVisualAgeCCompiler;
import net.sf.antcontrib.cpptasks.parser.TestCParser;
import net.sf.antcontrib.cpptasks.sun.TestForteCCCompiler;
import net.sf.antcontrib.cpptasks.types.TestDefineArgument;
import net.sf.antcontrib.cpptasks.types.TestLibrarySet;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(value = {
        TestCUtil.class,
        TestBorlandCCompiler.class,
        TestAbstractCompiler.class,
        TestAbstractLinker.class,
        TestAbstractProcessor.class,
        TestCCTask.class,
        TestCompilerEnum.class,
        TestCommandLineCompilerConfiguration.class,
        TestDependencyTable.class,
        TestDefineArgument.class,
        TestDevStudio2005CCompiler.class,
        TestDevStudioCCompiler.class,
        TestDevStudioLinker.class,
        TestLinkerDef.class,
        TestTargetInfo.class,
        TestLibrarySet.class,
        TestCompilerDef.class,
        TestCParser.class,
        TestGccCCompiler.class,
        TestAbstractLdLinker.class,
        TestAbstractArLibrarian.class,
        TestTargetHistoryTable.class,
        TestOutputTypeEnum.class,
        TestLinkType.class,
        TestLinkerEnum.class,
        TestAbstractLdLinker.class,
        TestAbstractArLibrarian.class,
        TestGccLinker.class,
        TestGccLinker.class,
        TestForteCCCompiler.class,
        TestaCCCompiler.class,
        TestVisualAgeCCompiler.class
})
public class TestAllClasses {
}
