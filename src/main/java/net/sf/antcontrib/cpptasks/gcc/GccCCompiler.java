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
package net.sf.antcontrib.cpptasks.gcc;

import net.sf.antcontrib.cpptasks.OptimizationEnum;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.compiler.Processor;
import net.sf.antcontrib.cpptasks.parser.CParser;
import net.sf.antcontrib.cpptasks.parser.FortranParser;
import net.sf.antcontrib.cpptasks.parser.Parser;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.util.Vector;

import static net.sf.antcontrib.cpptasks.CUtil.checkDirectoryArray;

/**
 * Adapter for the GCC C/C++ compiler
 *
 * @author Adam Murdoch
 */
public final class GccCCompiler extends GccCompatibleCCompiler {
    private final static String[] sourceExtensions = new String[]{".c", /* C */
            ".cc", /* C++ */
            ".cpp", /* C++ */
            ".cxx", /* C++ */
            ".c++", /* C++ */
            ".i", /* preprocessed C */
            ".ii", /* preprocessed C++ */
            ".f", /* FORTRAN */
            ".for", /* FORTRAN */
            ".m", /* Objective-C */
            ".mm", /* Objected-C++ */
            ".s" /* Assembly */
    };
    private final static String[] headerExtensions = new String[]{".h", ".hpp", ".inl"};
    private static final GccCCompiler cppInstance = new GccCCompiler("c++",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("c++", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
    private static final GccCCompiler g77Instance = new GccCCompiler("g77",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("g77", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
    private static final GccCCompiler gppInstance = new GccCCompiler("g++",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("g++", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
    private static final GccCCompiler instance = new GccCCompiler("gcc",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("gcc", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);

    /**
     * Gets c++ adapter
     *
     * @return GccCCompiler
     */
    public static GccCCompiler getCppInstance() {
        return cppInstance;
    }

    /**
     * Gets g77 adapter
     *
     * @return GccCCompiler
     */
    public static GccCCompiler getG77Instance() {
        return g77Instance;
    }

    /**
     * Gets gpp adapter
     *
     * @return GccCCompiler
     */
    public static GccCCompiler getGppInstance() {
        return gppInstance;
    }

    /**
     * Gets gcc adapter
     *
     * @return GccCCompiler
     */
    public static GccCCompiler getInstance() {
        return instance;
    }

    private String identifier;
    private File[] includePath;
    private final boolean isPICMeaningful;

    /**
     * Private constructor. Use GccCCompiler.getInstance() to get singleton
     * instance of this class.
     *
     * @param command String
     * @param sourceExtensions an array of String
     * @param headerExtensions an array of String
     * @param isLibtool boolean
     * @param libtoolCompiler GccCCompiler
     * @param newEnvironment boolean
     * @param env Environment
     */
    private GccCCompiler(String command, String[] sourceExtensions,
                         String[] headerExtensions, boolean isLibtool,
                         GccCCompiler libtoolCompiler, boolean newEnvironment,
                         Environment env) {
        super(command, null, sourceExtensions, headerExtensions, isLibtool,
                libtoolCompiler, newEnvironment, env);
        isPICMeaningful = !System.getProperty("os.name").contains("Windows");
    }

    public void addImpliedArgs(final Vector<String> args,
                               final boolean debug,
                               final boolean multithreaded,
                               final boolean exceptions,
                               final LinkType linkType,
                               final Boolean rtti,
                               final OptimizationEnum optimization) {
        super.addImpliedArgs(args, debug, multithreaded, exceptions, linkType, rtti, optimization);
        if (isPICMeaningful && linkType.isSharedLibrary()) {
            args.addElement("-fPIC");
        }
    }

    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new GccCCompiler(getCommand(), this.getSourceExtensions(),
                    this.getHeaderExtensions(), this.getLibtool(),
                    (GccCCompiler) this.getLibtoolCompiler(), newEnvironment,
                    env);
        }
        return this;
    }

    /**
     * <p>
     * Create parser to determine dependencies.
     * </p>
     * <p>
     * Will create appropriate parser (C++, FORTRAN) based on file extension.
     * </p>
     *
     * @param source File
     * @return Parser
     */
    protected Parser createParser(File source) {
        if (source != null) {
            String sourceName = source.getName();
            int lastDot = sourceName.lastIndexOf('.');
            if (lastDot >= 0 && lastDot + 1 < sourceName.length()) {
                char afterDot = sourceName.charAt(lastDot + 1);
                if (afterDot == 'f' || afterDot == 'F') {
                    return new FortranParser();
                }
            }
        }
        return new CParser();
    }

    public File[] getEnvironmentIncludePath() {
        if (includePath == null) {
            //
            //   construct default include path from machine id and version id
            //
            String[] defaultInclude = new String[]{String.format("/lib/%s/%s/include",
                    GccProcessor.getMachine(), GccProcessor.getVersion())};
            //
            //   read specs file and look for -istart and -idirafter
            //
            String[] specs = GccProcessor.getSpecs();
            String[][] optionValues = GccProcessor.parseSpecs(specs, "*cpp:",
                    new String[]{"-isystem ", "-idirafter "});
            //
            //   if no entries were found, then use a default path
            //
            if (optionValues[0].length == 0 && optionValues[1].length == 0) {
                optionValues[0] = new String[]{"/usr/local/include",
                        "/usr/include", "/usr/include/win32api"};
            }
            //
            //  remove mingw entries.
            //    For MinGW compiles this will mean the
            //      location of the sys includes will be
            //      wrong in dependencies.xml
            //      but that should have no significant effect
            for (int i = 0; i < optionValues.length; i++) {
                for (int j = 0; j < optionValues[i].length; j++) {
                    if (optionValues[i][j].indexOf("mingw") > 0) {
                        optionValues[i][j] = null;
                    }
                }
            }
            //
            //   if cygwin then
            //     we have to prepend location of gcc32
            //       and .. to start of absolute filenames to
            //       have something that will exist in the
            //       windows filesystem
            if (GccProcessor.isCygwin()) {
                GccProcessor.convertCygwinFilenames(optionValues[0]);
                GccProcessor.convertCygwinFilenames(optionValues[1]);
                GccProcessor.convertCygwinFilenames(defaultInclude);
            }
            int count = checkDirectoryArray(optionValues[0]);
            count += checkDirectoryArray(optionValues[1]);
            count += checkDirectoryArray(defaultInclude);
            includePath = new File[count];
            int index = 0;
            for (String[] optionValue : optionValues) {
                for (String s : optionValue) {
                    if (s != null) {
                        includePath[index++] = new File(s);
                    }
                }
            }
            for (String s : defaultInclude) {
                if (s != null) {
                    includePath[index++] = new File(s);
                }
            }
        }
        return includePath.clone();
    }

    public String getIdentifier() throws BuildException {
        if (identifier == null) {
            identifier = String.format("%s %s %s %s", getLibtool() ? "libtool" : "", getCommand(),
                    GccProcessor.getVersion(), GccProcessor.getMachine());
        }
        return identifier;
    }

    public Linker getLinker(LinkType linkType) {
        return GccLinker.getInstance().getLinker(linkType);
    }

    public int getMaximumCommandLength() {
        /*
         * Window-specific compilers use a limit of 2048 to prevent over
         * running the available command line length, while the Unix compilers
         * assume that they have an unlimited command line.
         */
        if (System.getProperty("os.name").contains("Windows")) {
            return 2048;
        } else {
            return Integer.MAX_VALUE;
        }
    }
}
