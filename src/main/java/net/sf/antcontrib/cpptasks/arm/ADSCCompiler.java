/*
 *
 * Copyright 2003-2004 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.arm;

import net.sf.antcontrib.cpptasks.OptimizationEnum;
import net.sf.antcontrib.cpptasks.compiler.CommandLineCCompiler;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.util.Vector;

import static net.sf.antcontrib.cpptasks.CUtil.getPathFromEnvironment;

/**
 * <p>
 * Adapter for the ARM C Compilers.
 * </p>
 * <p>
 * See Doc No: ARM DUI 0151A, Issued: Nov 2001 at
 * http://www.arm.com/arm/User_Guides?OpenDocument
 * </p>
 *
 * @author Curt Arnold
 */
public class ADSCCompiler extends CommandLineCCompiler {
    /**
     * Header file extensions
     */
    private static final String[] headerExtensions = new String[]{".h", ".hpp", ".inl"};
    /**
     * Source file extensions
     */
    private static final String[] sourceExtensions = new String[]{".c", ".cc", ".cpp", ".cxx", ".c++"};
    /**
     * Singleton for ARM 32-bit C compiler
     */
    private static final ADSCCompiler armcc = new ADSCCompiler("armcc", false, null);
    /**
     * Singleton for ARM 32-bit C++ compiler
     */
    private static final ADSCCompiler armcpp = new ADSCCompiler("armcpp", false, null);
    /**
     * Singleton for ARM 16-bit C compiler
     */
    private static final ADSCCompiler tcc = new ADSCCompiler("tcc", false, null);
    /**
     * Singleton for ARM 16-bit C++ compiler
     */
    private static final ADSCCompiler tcpp = new ADSCCompiler("tcpp", false, null);
    /**
     * Singleton for ARM 32-bit C compiler
     *
     * @return ADSCCompiler
     */
    public static ADSCCompiler getArmCC() {
        return armcc;
    }

    /**
     * Singleton for ARM 32-bit C++ compiler
     *
     * @return ADSCCompiler
     */
    public static ADSCCompiler getArmCpp() {
        return armcpp;
    }

    /**
     * Singleton for ARM 16-bit C compiler
     *
     * @return ADSCCompiler
     */
    public static ADSCCompiler getThumbCC() {
        return tcc;
    }

    /**
     * Singleton for ARM 16-bit C++ compiler
     *
     * @return ADSCCompiler
     */
    public static ADSCCompiler getThumbCpp() {
        return tcpp;
    }

    private static void quoteFile(StringBuilder buf, String outPath) {
        if (outPath.contains(" ")) {
            buf.append('\"');
            buf.append(outPath);
            buf.append('\"');
        } else {
            buf.append(outPath);
        }
    }

    /**
     * Private constructor
     *
     * @param command        executable name
     * @param newEnvironment Change environment
     * @param env            New environment
     */
    private ADSCCompiler(String command, boolean newEnvironment, Environment env) {
        super(command, "-vsn", sourceExtensions, headerExtensions, ".o", false,
                null, newEnvironment, env);
    }

    /**
     * {@inheritDoc}
     */
    protected void addImpliedArgs(Vector<String> args,
                                  final boolean debug,
                                  final boolean multithreaded,
                                  final boolean exceptions,
                                  final LinkType linkType,
                                  final Boolean rtti,
                                  final OptimizationEnum optimization) {
        if (debug) {
            args.addElement("-g");
        }
        //
        //   didn't see anything about producing
        //     anything other than executables in the docs
        if (linkType.isExecutable()) {
        } else if (linkType.isSharedLibrary()) {
        }
    }

    /**
     * <p>
     * Adds flags that customize the warnings reported
     * </p>
     * <p>
     * Compiler does not appear to have warning levels but ability to turn off
     * specific errors by explicit switches, could fabricate levels by
     * prioritizing errors.
     * </p>
     *
     * @param args a vector of String
     * @param warnings int
     * @see net.sf.antcontrib.cpptasks.compiler.CommandLineCompiler#addWarningSwitch(java.util.Vector,
     * int)
     */
    protected void addWarningSwitch(Vector<String> args, int warnings) {
    }

    /**
     * Add command line options for preprocessor macro
     *
     * @param buffer StringBuilder
     * @param define String
     * @param value String
     * @see net.sf.antcontrib.cpptasks.compiler.CommandLineCompiler#getDefineSwitch(java.lang.StringBuilder,
     * java.lang.String, java.lang.String)
     */
    protected void getDefineSwitch(StringBuilder buffer, String define, String value) {
        buffer.append("-D");
        buffer.append(define);
        if (value != null) {
            buffer.append('=');
            buffer.append(value);
        }
    }

    /**
     * ARMINC environment variable contains the default include path
     *
     * @return an array of File
     * @see net.sf.antcontrib.cpptasks.compiler.CommandLineCompiler#getEnvironmentIncludePath()
     */
    protected File[] getEnvironmentIncludePath() {
        return getPathFromEnvironment("ARMINC", ";");
    }

    /**
     * Returns command line option to specify include directory
     *
     * @param source String
     * @return String
     */
    protected String getIncludeDirSwitch(String source) {
        StringBuilder buf = new StringBuilder("-I");
        quoteFile(buf, source);
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.antcontrib.cpptasks.compiler.Processor#getLinker(net.sf.antcontrib.cpptasks.compiler.LinkType)
     */
    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return ADSLibrarian.getInstance();
        }
        if (type.isSharedLibrary()) {
            return ADSLinker.getDllInstance();
        }
        return ADSLinker.getInstance();
    }

    /**
     * Maximum command line length
     *
     * @return int
     * @see net.sf.antcontrib.cpptasks.compiler.CommandLineCompiler#getMaximumCommandLength()
     */
    public int getMaximumCommandLength() {
        return 1000;
    }

    /*
     * Adds command to undefine preprocessor macro
     *
     * @see net.sf.antcontrib.cpptasks.compiler.CommandLineCompiler#getUndefineSwitch(java.lang.StringBuilder,
     *      java.lang.String)
     */
    protected void getUndefineSwitch(StringBuilder buffer, String define) {
        buffer.append("-U");
        buffer.append(define);
    }
}
