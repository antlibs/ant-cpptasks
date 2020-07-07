/*
 *
 * Copyright 2001-2004 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.ibm;

import net.sf.antcontrib.cpptasks.OptimizationEnum;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.gcc.GccCompatibleCCompiler;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.util.Vector;

/**
 * Adapter for the IBM(r) Visual Age(tm) C++ compiler for AIX(tm)
 *
 * @author Curt Arnold
 */
public final class VisualAgeCCompiler extends GccCompatibleCCompiler {
    private final static String[] headerExtensions = new String[]{".h", ".hpp", ".inl"};
    private final static String[] sourceExtensions = new String[]{".c", ".cc", ".cxx", ".cpp", ".i", ".s"};

    private static final VisualAgeCCompiler instance = new VisualAgeCCompiler("xlC", sourceExtensions,
            headerExtensions, false, null);
    /**
     * Gets singleton instance of this class
     *
     * @return VisualAgeCCompiler
     */
    public static VisualAgeCCompiler getInstance() {
        return instance;
    }

    private String identifier;
    private File[] includePath;

    /**
     * Private constructor. Use getInstance() to get singleton instance of this
     * class.
     *
     * @param command String
     * @param sourceExtensions an array of String
     * @param headerExtensions an array of String
     * @param newEnvironment boolean
     * @param env Environment
     */
    private VisualAgeCCompiler(String command, String[] sourceExtensions, String[] headerExtensions,
                               boolean newEnvironment, Environment env) {
        super(command, "-help", sourceExtensions, headerExtensions, false,
                null, newEnvironment, env);
    }

    public void addImpliedArgs(final Vector<String> args,
                               final boolean debug,
                               final boolean multithreaded,
                               final boolean exceptions,
                               final LinkType linkType,
                               final Boolean rtti,
                               final OptimizationEnum optimization) {
        args.addElement("-c");
        if (debug) {
            args.addElement("-g");
        }
        if (linkType.isSharedLibrary()) {
            args.addElement("-fpic");
        }
        if (rtti != null) {
            if (rtti) {
                args.addElement("-qrtti=all");
            } else {
                args.addElement("-qnortti");
            }
        }
    }

    public void addWarningSwitch(Vector<String> args, int level) {
        switch (level) {
            case 0:
                args.addElement("-w");
                break;
            case 1:
                args.addElement("-qflag=s:s");
                break;
            case 2:
                args.addElement("-qflag=e:e");
                break;
            case 3:
                args.addElement("-qflag=w:w");
                break;
            case 4:
                args.addElement("-qflag=i:i");
                break;
            case 5:
                args.addElement("-qhalt=w:w");
                break;
        }
    }

    public Linker getLinker(LinkType linkType) {
        return VisualAgeLinker.getInstance().getLinker(linkType);
    }

    public int getMaximumCommandLength() {
        return Integer.MAX_VALUE;
    }

    /**
     * <p>
     * Gets identifier for the compiler.
     * </p>
     * <p>
     * Initial attempt at extracting version information
     * would lock up.  Using a stock response.
     * </p>
     *
     * @return String
     */
    public String getIdentifier() {
        return "VisualAge compiler - unidentified version";
    }
}
