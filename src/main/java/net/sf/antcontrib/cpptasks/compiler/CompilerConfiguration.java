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
import net.sf.antcontrib.cpptasks.CompilerParam;
import net.sf.antcontrib.cpptasks.DependencyInfo;
import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * A configuration for a compiler
 *
 * @author Curt Arnold
 */
public interface CompilerConfiguration extends ProcessorConfiguration {
    void compile(CCTask task, File outputDir, String[] sourceFiles,
                 boolean relentless, ProgressMonitor monitor) throws BuildException;

    /**
     * <p>
     * This method may be used to get two distinct compiler configurations, one
     * for compiling the specified file and producing a precompiled header
     * file, and a second for compiling other files using the precompiled
     * header file.
     * </p>
     * <p>
     * The last (preferably only) include directive in the prototype file will
     * be used to mark the boundary between pre-compiled and normally compiled
     * headers.
     * </p>
     *
     * @param prototype A source file (for example, stdafx.cpp) that is used to build
     *                  the precompiled header file. @returns null if precompiled
     *                  headers are not supported or a two element array containing
     *                  the precompiled header generation configuration and the
     *                  consuming configuration
     * @param nonPrecompiledFiles an array of nonprecompiled file names
     * @return an array of CompilerConfiguration
     */
    CompilerConfiguration[] createPrecompileConfigurations(File prototype,
                                                           String[] nonPrecompiledFiles);

    /**
     * <p>
     * Returns an digest for the include path for the configuration.
     * </p>
     * <p>
     * This is used to determine if cached dependency information is invalid
     * because the include paths have changed.
     * </p>
     *
     * @return String
     */
    String getIncludePathIdentifier();

    CompilerParam getParam(String name);

    boolean isPrecompileGeneration();

    DependencyInfo parseIncludes(CCTask task, File baseDir, File source);
}
