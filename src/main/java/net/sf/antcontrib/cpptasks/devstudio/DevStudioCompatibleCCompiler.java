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
package net.sf.antcontrib.cpptasks.devstudio;

import net.sf.antcontrib.cpptasks.OptimizationEnum;
import net.sf.antcontrib.cpptasks.compiler.CommandLineCompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.CompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.PrecompilingCommandLineCCompiler;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.util.Vector;

import static net.sf.antcontrib.cpptasks.CUtil.getBasename;
import static net.sf.antcontrib.cpptasks.CUtil.getPathFromEnvironment;

/**
 * An abstract base class for compilers that are basically command line
 * compatible with Microsoft(r) C/C++ Optimizing Compiler
 *
 * @author Curt Arnold
 */
public abstract class DevStudioCompatibleCCompiler extends PrecompilingCommandLineCCompiler {
    private static final String[] mflags = new String[]{
            //
            //   first four are single-threaded
            //      (runtime=static,debug=false), (..,debug=true),
            //      (runtime=dynamic,debug=true), (..,debug=false), (not supported)
            //    next four are multi-threaded, same sequence
            "/ML", "/MLd", null, null, "/MT", "/MTd", "/MD", "/MDd"};

    protected DevStudioCompatibleCCompiler(String command, String identifierArg,
                                           boolean newEnvironment, Environment env) {
        super(command, identifierArg, new String[]{".c", ".cc", ".cpp", ".cxx", ".c++"},
                new String[]{".h", ".hpp", ".inl"}, ".obj", false, null, newEnvironment, env);
    }

    protected void addImpliedArgs(final Vector<String> args,
                                  final boolean debug,
                                  final boolean multithreaded,
                                  final boolean exceptions,
                                  final LinkType linkType,
                                  final Boolean rtti,
                                  final OptimizationEnum optimization) {
        args.addElement("/c");
        args.addElement("/nologo");
        if (exceptions) {
            //   changed to eliminate warning on VC 2005, should support VC 6 and later
            //   use /GX to support VC5 - 2005 (with warning)
            args.addElement("/EHsc");
        }
        int mindex = 0;
        if (multithreaded) {
            mindex += 4;
        }
        boolean staticRuntime = linkType.isStaticRuntime();
        if (!staticRuntime) {
            mindex += 2;
        }
        if (debug) {
            mindex += 1;
            addDebugSwitch(args);
        } else {
            if (optimization != null) {
                if (optimization.isSize()) {
                    args.addElement("/O1");
                }
                if (optimization.isSpeed()) {
                    args.addElement("/O2");
                }
            }
            args.addElement("/DNDEBUG");
        }
        String mflag = mflags[mindex];
        if (mflag == null) {
            throw new BuildException("multithread='false' and runtime='dynamic' not supported");
        }
        args.addElement(mflag);
        if (rtti != null && rtti) {
            args.addElement("/GR");
        }
    }

    protected void addDebugSwitch(Vector<String> args) {
        args.addElement("/Zi");
        args.addElement("/Od");
        args.addElement("/GZ");
        args.addElement("/D_DEBUG");
    }

    protected void addWarningSwitch(Vector<String> args, int level) {
        DevStudioProcessor.addWarningSwitch(args, level);
    }

    protected CompilerConfiguration createPrecompileGeneratingConfig(
            CommandLineCompilerConfiguration baseConfig, File prototype,
            String lastInclude) {
        String[] additionalArgs = new String[]{"/Fp" + getBasename(prototype) + ".pch", "/Yc"};
        return new CommandLineCompilerConfiguration(baseConfig, additionalArgs,
                null, true);
    }

    protected CompilerConfiguration createPrecompileUsingConfig(
            CommandLineCompilerConfiguration baseConfig, File prototype,
            String lastInclude, String[] exceptFiles) {
        String[] additionalArgs = new String[]{"/Fp" + getBasename(prototype) + ".pch",
                "/Yu" + lastInclude};
        return new CommandLineCompilerConfiguration(baseConfig, additionalArgs,
                exceptFiles, false);
    }

    protected void getDefineSwitch(StringBuilder buffer, String define, String value) {
        DevStudioProcessor.getDefineSwitch(buffer, define, value);
    }

    protected File[] getEnvironmentIncludePath() {
        return getPathFromEnvironment("INCLUDE", ";");
    }

    protected String getIncludeDirSwitch(String includeDir) {
        return DevStudioProcessor.getIncludeDirSwitch(includeDir);
    }

    protected void getUndefineSwitch(StringBuilder buffer, String define) {
        DevStudioProcessor.getUndefineSwitch(buffer, define);
    }
}
