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

import net.sf.antcontrib.cpptasks.parser.Parser;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A command line C compiler that can utilize precompilation of header files
 *
 * @author Curt Arnold
 */
public abstract class PrecompilingCommandLineCompiler extends CommandLineCompiler
        implements PrecompilingCompiler {
    protected PrecompilingCommandLineCompiler(String command,
                                              String identifierArg, String[] sourceExtensions,
                                              String[] headerExtensions, String outputSuffix,
                                              boolean libtool,
                                              PrecompilingCommandLineCompiler libtoolCompiler,
                                              boolean newEnvironment, Environment env) {
        super(command, identifierArg, sourceExtensions, headerExtensions, outputSuffix, libtool,
                libtoolCompiler, newEnvironment, env);
    }

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
     * @param config    base configuration
     * @param prototype A source file (for example, stdafx.cpp) that is used to build
     *                  the precompiled header file. @returns null if precompiled
     *                  headers are not supported or a two element array containing
     *                  the precompiled header generation configuration and the
     *                  consuming configuration
     * @param exceptFiles An array of file names
     * @return an array of CompilerConfiguration
     */
    public CompilerConfiguration[] createPrecompileConfigurations(
            CompilerConfiguration config, File prototype, String[] exceptFiles) {
        //
        //   cast should success or someone is passing us a configuration
        //      that was prepared by another processor
        //
        CommandLineCompilerConfiguration cmdLineConfig = (CommandLineCompilerConfiguration) config;
        //
        //   parse prototype file to determine last header
        //
        Parser parser = createParser(prototype);
        String[] includes;
        try {
            Reader reader = new BufferedReader(new FileReader(prototype));
            parser.parse(reader);
            includes = parser.getIncludes();
        } catch (IOException ex) {
            throw new BuildException("Error parsing precompiled header prototype: "
                    + prototype.toString() + ":" + ex.toString());
        }
        if (includes.length == 0) {
            throw new BuildException("Precompiled header prototype: "
                    + prototype.toString() + " does not contain any include directives.");
        }
        CompilerConfiguration[] configs = new CompilerConfiguration[2];
        configs[0] = createPrecompileGeneratingConfig(cmdLineConfig, prototype, includes[0]);
        configs[1] = createPrecompileUsingConfig(cmdLineConfig, prototype, includes[0],
                exceptFiles);
        return configs;
    }

    protected abstract CompilerConfiguration createPrecompileGeneratingConfig(
            CommandLineCompilerConfiguration baseConfig, File prototype,
            String lastInclude);

    protected abstract CompilerConfiguration createPrecompileUsingConfig(
            CommandLineCompilerConfiguration baseConfig, File prototype,
            String lastInclude, String[] exceptFiles);
}
