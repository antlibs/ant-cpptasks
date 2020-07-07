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
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.CompilerDef;
import net.sf.antcontrib.cpptasks.OptimizationEnum;
import net.sf.antcontrib.cpptasks.ProcessorDef;
import net.sf.antcontrib.cpptasks.ProcessorParam;
import net.sf.antcontrib.cpptasks.TargetDef;
import net.sf.antcontrib.cpptasks.VersionInfo;
import net.sf.antcontrib.cpptasks.types.CommandLineArgument;
import net.sf.antcontrib.cpptasks.types.UndefineArgument;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;
/**
 * An abstract Compiler implementation which uses an external program to
 * perform the compile.
 *
 * @author Adam Murdoch
 */
public abstract class CommandLineCompiler extends AbstractCompiler {
    private String command;
    private final Environment env;
    private String identifier;
    private String identifierArg;
    private boolean libtool;
    private CommandLineCompiler libtoolCompiler;
    private final boolean newEnvironment;

    protected CommandLineCompiler(String command, String identifierArg,
                                  String[] sourceExtensions, String[] headerExtensions,
                                  String outputSuffix, boolean libtool,
                                  CommandLineCompiler libtoolCompiler, boolean newEnvironment,
                                  Environment env) {
        super(sourceExtensions, headerExtensions, outputSuffix);
        this.command = command;
        if (libtool && libtoolCompiler != null) {
            throw new IllegalArgumentException("libtoolCompiler should be null when libtool is true");
        }
        this.libtool = libtool;
        this.libtoolCompiler = libtoolCompiler;
        this.identifierArg = identifierArg;
        this.newEnvironment = newEnvironment;
        this.env = env;
    }

    abstract protected void addImpliedArgs(Vector<String> args, boolean debug,
                                           boolean multithreaded, boolean exceptions, LinkType linkType,
                                           Boolean rtti, OptimizationEnum optimization);

    /**
     * <p>
     * Adds command-line arguments for include directories.
     * </p>
     * <p>
     * If relativeArgs is not null will add corresponding relative paths
     * include switches to that vector (for use in building a configuration
     * identifier that is consistent between machines).
     * </p>
     *
     * @param baseDirPath  Base directory path.
     * @param includeDirs  Array of include directory paths
     * @param args         Vector of command line arguments used to execute the task
     * @param relativeArgs Vector of command line arguments used to build the
     *                     configuration identifier
     * @param includePathId StringBuffer
     */
    protected void addIncludes(String baseDirPath, File[] includeDirs, Vector<String> args,
                               Vector<String> relativeArgs, StringBuffer includePathId) {
        for (int i = 0; i < includeDirs.length; i++) {
            args.addElement(getIncludeDirSwitch(includeDirs[i].getAbsolutePath()));
            if (relativeArgs != null) {
                String relative = CUtil.getRelativePath(baseDirPath, includeDirs[i]);
                relativeArgs.addElement(getIncludeDirSwitch(relative));
                if (includePathId != null) {
                    if (includePathId.length() == 0) {
                        includePathId.append("/I");
                    } else {
                        includePathId.append(" /I");
                    }
                    includePathId.append(relative);
                }
            }
        }
    }

    abstract protected void addWarningSwitch(Vector<String> args, int warnings);

    protected void buildDefineArguments(CompilerDef[] defs, Vector<String> args) {
        //
        //   assume that we aren't inheriting defines from containing <cc>
        //
        UndefineArgument[] merged = defs[0].getActiveDefines();
        for (int i = 1; i < defs.length; i++) {
            //
            //  if we are inheriting, merge the specific defines with the
            //      containing defines
            merged = UndefineArgument.merge(defs[i].getActiveDefines(), merged);
        }
        StringBuffer buf = new StringBuffer(30);
        for (int i = 0; i < merged.length; i++) {
            buf.setLength(0);
            UndefineArgument current = merged[i];
            if (current.isDefine()) {
                getDefineSwitch(buf, current.getName(), current.getValue());
            } else {
                getUndefineSwitch(buf, current.getName());
            }
            args.addElement(buf.toString());
        }
    }

    /**
     * Compiles a source file.
     *
     * @param task CCTask
     * @param outputDir File
     * @param sourceFiles array of String
     * @param args array of String
     * @param endArgs array of String
     * @param relentless boolean
     * @param config CommandLineCompilerConfiguration
     * @param monitor ProgressMonitor
     * @throws BuildException if something goes wrong
     */
    public void compile(CCTask task, File outputDir, String[] sourceFiles,
                        String[] args, String[] endArgs, boolean relentless,
                        CommandLineCompilerConfiguration config,
                        ProgressMonitor monitor) throws BuildException {
        BuildException exc = null;
        //
        //   determine length of executable name and args
        //
        String command = getCommand();
        int baseLength = command.length() + args.length + endArgs.length;
        if (libtool) {
            baseLength += 8;
        }
        for (int i = 0; i < args.length; i++) {
            baseLength += args[i].length();
        }
        for (int i = 0; i < endArgs.length; i++) {
            baseLength += endArgs[i].length();
        }
        if (baseLength > getMaximumCommandLength()) {
            throw new BuildException("Command line is over maximum length"
                    + " without specifying source file");
        }
        //
        //  typically either 1 or Integer.MAX_VALUE
        //
        int maxInputFilesPerCommand = getMaximumInputFilesPerCommand();
        int argumentCountPerInputFile = getArgumentCountPerInputFile();
        for (int sourceIndex = 0; sourceIndex < sourceFiles.length; ) {
            int cmdLength = baseLength;
            int firstFileNextExec;
            for (firstFileNextExec = sourceIndex; firstFileNextExec < sourceFiles.length
                    && (firstFileNextExec - sourceIndex) < maxInputFilesPerCommand; firstFileNextExec++) {
                cmdLength += getTotalArgumentLengthForInputFile(outputDir,
                        sourceFiles[firstFileNextExec]);
                if (cmdLength >= getMaximumCommandLength()) {
                    break;
                }
            }
            if (firstFileNextExec == sourceIndex) {
                throw new BuildException("Extremely long file name, can't fit on command line");
            }
            int argCount = args.length + 1 + endArgs.length + (firstFileNextExec - sourceIndex)
                    * argumentCountPerInputFile;
            if (libtool) {
                argCount++;
            }
            String[] commandline = new String[argCount];
            int index = 0;
            if (libtool) {
                commandline[index++] = "libtool";
            }
            commandline[index++] = command;
            for (int j = 0; j < args.length; j++) {
                commandline[index++] = args[j];
            }
            for (int j = sourceIndex; j < firstFileNextExec; j++) {
                for (int k = 0; k < argumentCountPerInputFile; k++) {
                    commandline[index++] = getInputFileArgument(outputDir, sourceFiles[j], k);
                }
            }
            for (int j = 0; j < endArgs.length; j++) {
                commandline[index++] = endArgs[j];
            }
            int retval = runCommand(task, outputDir, commandline);
            if (monitor != null) {
                String[] fileNames = new String[firstFileNextExec - sourceIndex];
                for (int j = 0; j < fileNames.length; j++) {
                    fileNames[j] = sourceFiles[sourceIndex + j];
                }
                monitor.progress(fileNames);
            }
            //
            //   if the process returned a failure code and
            //      we aren't holding an exception from an earlier
            //      interaction
            if (retval != 0 && exc == null) {
                //
                //   construct the exception
                //
                exc = new BuildException(this.getCommand()
                        + " failed with return code " + retval, task.getLocation());
                //
                //   and throw it now unless we are relentless
                //
                if (!relentless) {
                    throw exc;
                }
            }
            sourceIndex = firstFileNextExec;
        }
        //
        //   if the compiler returned a failure value earlier
        //      then throw an exception
        if (exc != null) {
            throw exc;
        }
    }

    protected CompilerConfiguration createConfiguration(final CCTask task,
                                                        final LinkType linkType,
                                                        final ProcessorDef[] baseDefs,
                                                        final CompilerDef specificDef,
                                                        final TargetDef targetPlatform,
                                                        final VersionInfo versionInfo) {
        Vector<String> args = new Vector<String>();
        CompilerDef[] defaultProviders = new CompilerDef[baseDefs.length + 1];
        for (int i = 0; i < baseDefs.length; i++) {
            defaultProviders[i + 1] = (CompilerDef) baseDefs[i];
        }
        defaultProviders[0] = specificDef;
        Vector<CommandLineArgument> cmdArgs = new Vector<CommandLineArgument>();
        //
        //   add command line arguments inherited from <cc> element
        //     any "extends" and finally the specific CompilerDef
        CommandLineArgument[] commandArgs;
        for (int i = defaultProviders.length - 1; i >= 0; i--) {
            commandArgs = defaultProviders[i].getActiveProcessorArgs();
            for (int j = 0; j < commandArgs.length; j++) {
                if (commandArgs[j].getLocation() == 0) {
                    args.addElement(commandArgs[j].getValue());
                } else {
                    cmdArgs.addElement(commandArgs[j]);
                }
            }
        }
        Vector<ProcessorParam> params = new Vector<ProcessorParam>();
        //
        //   add command line arguments inherited from <cc> element
        //     any "extends" and finally the specific CompilerDef
        ProcessorParam[] paramArray;
        for (int i = defaultProviders.length - 1; i >= 0; i--) {
            paramArray = defaultProviders[i].getActiveProcessorParams();
            Collections.addAll(params, paramArray);
        }
        paramArray = params.toArray(new ProcessorParam[0]);
        boolean multithreaded = specificDef.getMultithreaded(defaultProviders, 1);
        boolean debug = specificDef.getDebug(baseDefs, 0);
        boolean exceptions = specificDef.getExceptions(defaultProviders, 1);
        Boolean rtti = specificDef.getRtti(defaultProviders, 1);
        OptimizationEnum optimization = specificDef.getOptimization(defaultProviders, 1);
        this.addImpliedArgs(args, debug, multithreaded, exceptions, linkType, rtti, optimization);
        //
        //    add all appropriate defines and undefines
        //
        buildDefineArguments(defaultProviders, args);
        int warnings = specificDef.getWarnings(defaultProviders, 0);
        addWarningSwitch(args, warnings);
        int endCount = 0;
        for (CommandLineArgument arg : cmdArgs) {
            if (arg.getLocation() == 1) {
                args.addElement(arg.getValue());
            } else if (arg.getLocation() == 2) {
                endCount++;
            }
        }
        String[] endArgs = new String[endCount];
        int index = 0;
        for (CommandLineArgument arg : cmdArgs) {
            if (arg.getLocation() == 2) {
                endArgs[index++] = arg.getValue();
            }
        }
        //
        //   Want to have distinct set of arguments with relative
        //      path names for includes that are used to build
        //      the configuration identifier
        //
        Vector<String> relativeArgs = (Vector<String>) args.clone();
        //
        //    add all active include and sysincludes
        //
        StringBuffer includePathIdentifier = new StringBuffer();
        File baseDir = specificDef.getProject().getBaseDir();
        String baseDirPath;
        try {
            baseDirPath = baseDir.getCanonicalPath();
        } catch (IOException ex) {
            baseDirPath = baseDir.toString();
        }
        Vector<String> includePath = new Vector<String>();
        Vector<String> sysIncludePath = new Vector<String>();
        for (int i = defaultProviders.length - 1; i >= 0; i--) {
            String[] incPath = defaultProviders[i].getActiveIncludePaths();
            for (int j = 0; j < incPath.length; j++) {
                includePath.addElement(incPath[j]);
            }
            incPath = defaultProviders[i].getActiveSysIncludePaths();
            for (int j = 0; j < incPath.length; j++) {
                sysIncludePath.addElement(incPath[j]);
            }
        }
        File[] incPath = new File[includePath.size()];
        for (int i = 0; i < includePath.size(); i++) {
            incPath[i] = new File(includePath.elementAt(i));
        }
        File[] sysIncPath = new File[sysIncludePath.size()];
        for (int i = 0; i < sysIncludePath.size(); i++) {
            sysIncPath[i] = new File(sysIncludePath.elementAt(i));
        }
        addIncludes(baseDirPath, incPath, args, relativeArgs,
                includePathIdentifier);
        addIncludes(baseDirPath, sysIncPath, args, null, null);
        StringBuilder buf = new StringBuilder(getIdentifier());
        for (int i = 0; i < relativeArgs.size(); i++) {
            buf.append(' ');
            buf.append(relativeArgs.elementAt(i));
        }
        for (int i = 0; i < endArgs.length; i++) {
            buf.append(' ');
            buf.append(endArgs[i]);
        }
        String configId = buf.toString();
        boolean rebuild = specificDef.getRebuild(baseDefs, 0);
        File[] envIncludePath = getEnvironmentIncludePath();
        return new CommandLineCompilerConfiguration(this, configId, incPath,
                sysIncPath, envIncludePath, includePathIdentifier.toString(),
                args.toArray(new String[0]), paramArray, rebuild, endArgs);
    }

    protected int getArgumentCountPerInputFile() {
        return 1;
    }

    protected final String getCommand() {
        return command;
    }

    abstract protected void getDefineSwitch(StringBuffer buffer, String define,
                                            String value);

    protected abstract File[] getEnvironmentIncludePath();

    public String getIdentifier() {
        if (identifier == null) {
            if (identifierArg == null) {
                identifier = getIdentifier(new String[]{command}, command);
            } else {
                identifier = getIdentifier(new String[]{command, identifierArg}, command);
            }
        }
        return identifier;
    }

    abstract protected String getIncludeDirSwitch(String source);

    protected String getInputFileArgument(File outputDir, String filename, int index) {
        //
        //   if there is an embedded space,
        //      must enclose in quotes
        if (filename.contains(" ")) {
            return "\"" + filename + "\"";
        }
        return filename;
    }

    protected final boolean getLibtool() {
        return libtool;
    }

    /**
     * <p>
     * Obtains the same compiler, but with libtool set
     * </p>
     * <p>
     * Default behavior is to ignore libtool
     * </p>
     *
     * @return CommandLineCompiler
     */
    public final CommandLineCompiler getLibtoolCompiler() {
        if (libtoolCompiler != null) {
            return libtoolCompiler;
        }
        return this;
    }

    abstract public int getMaximumCommandLength();

    protected int getMaximumInputFilesPerCommand() {
        return Integer.MAX_VALUE;
    }

    protected int getTotalArgumentLengthForInputFile(File outputDir,
                                                     String inputFile) {
        return inputFile.length() + 1;
    }

    abstract protected void getUndefineSwitch(StringBuffer buffer, String define);

    /**
     * This method is exposed so test classes can overload and test the
     * arguments without actually spawning the compiler
     *
     * @param task CCTask
     * @param workingDir File
     * @param cmdLine array of String
     * @return int
     * @throws BuildException if something goes wrong
     */
    protected int runCommand(CCTask task, File workingDir,
                             String[] cmdLine) throws BuildException {
        return CUtil.runCommand(task, workingDir, cmdLine, newEnvironment, env);
    }

    protected final void setCommand(String command) {
        this.command = command;
    }
}
