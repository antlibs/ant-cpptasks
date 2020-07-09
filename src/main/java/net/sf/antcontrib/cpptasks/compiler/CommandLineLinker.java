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
import net.sf.antcontrib.cpptasks.LinkerDef;
import net.sf.antcontrib.cpptasks.ProcessorDef;
import net.sf.antcontrib.cpptasks.ProcessorParam;
import net.sf.antcontrib.cpptasks.TargetDef;
import net.sf.antcontrib.cpptasks.VersionInfo;
import net.sf.antcontrib.cpptasks.types.CommandLineArgument;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * An abstract Linker implementation that performs the link via an external
 * command.
 *
 * @author Adam Murdoch
 */
public abstract class CommandLineLinker extends AbstractLinker {
    private String command;
    private Environment env = null;
    private String identifier;
    private String identifierArg;
    private boolean isLibtool;
    private String[] librarySets;
    private CommandLineLinker libtoolLinker;
    private boolean newEnvironment = false;
    private String outputSuffix;


    /**
     * Creates a command line linker invocation
     *
     * @param command String
     * @param identifierArg String
     * @param extensions array of String
     * @param ignoredExtensions array of String
     * @param outputSuffix String
     * @param isLibtool boolean
     * @param libtoolLinker CommandLineLinker
     */
    public CommandLineLinker(String command,
                             String identifierArg,
                             String[] extensions,
                             String[] ignoredExtensions, String outputSuffix,
                             boolean isLibtool, CommandLineLinker libtoolLinker) {
        super(extensions, ignoredExtensions);
        this.command = command;
        this.identifierArg = identifierArg;
        this.outputSuffix = outputSuffix;
        this.isLibtool = isLibtool;
        this.libtoolLinker = libtoolLinker;
    }

    protected abstract void addBase(long base, Vector<String> args);

    protected abstract void addFixed(Boolean fixed, Vector<String> args);

    abstract protected void addImpliedArgs(boolean debug,
                                           LinkType linkType, Vector<String> args);

    protected abstract void addIncremental(boolean incremental, Vector<String> args);

    //
    //  Windows processors handle these through file list
    //
    protected String[] addLibrarySets(CCTask task, LibrarySet[] libsets, Vector<String> preargs,
                                      Vector<String> midargs, Vector<String> endargs) {
        return null;
    }

    protected abstract void addMap(boolean map, Vector<String> args);

    protected abstract void addStack(int stack, Vector<String> args);

    protected abstract void addEntry(String entry, Vector<String> args);

    protected LinkerConfiguration createConfiguration(CCTask task, LinkType linkType,
                                                      ProcessorDef[] baseDefs,
                                                      LinkerDef specificDef,
                                                      TargetDef targetPlatform,
                                                      VersionInfo versionInfo) {
        Vector<String> preargs = new Vector<String>();
        Vector<String> midargs = new Vector<String>();
        Vector<String> endargs = new Vector<String>();
        List<Vector<String>> args = new ArrayList<Vector<String>>();
        args.add(preargs);
        args.add(midargs);
        args.add(endargs);

        LinkerDef[] defaultProviders = new LinkerDef[baseDefs.length + 1];
        defaultProviders[0] = specificDef;
        for (int i = 0; i < baseDefs.length; i++) {
            defaultProviders[i + 1] = (LinkerDef) baseDefs[i];
        }
        //
        //   add command line arguments inherited from <cc> element
        //     any "extends" and finally the specific CompilerDef
        CommandLineArgument[] commandArgs;
        for (int i = defaultProviders.length - 1; i >= 0; i--) {
            commandArgs = defaultProviders[i].getActiveProcessorArgs();
            for (CommandLineArgument commandArg : commandArgs) {
                args.get(commandArg.getLocation()).addElement(commandArg.getValue());
            }
        }

        Vector<ProcessorParam> params = new Vector<ProcessorParam>();
        //
        //   add command line arguments inherited from <cc> element
        //     any "extends" and finally the specific CompilerDef
        ProcessorParam[] paramArray;
        for (int i = defaultProviders.length - 1; i >= 0; i--) {
            Collections.addAll(params, defaultProviders[i].getActiveProcessorParams());
        }

        paramArray = params.toArray(new ProcessorParam[0]);

        boolean debug = specificDef.getDebug(baseDefs, 0);


        String startupObject = getStartupObject(linkType);

        addImpliedArgs(debug, linkType, preargs);
        addIncremental(specificDef.getIncremental(defaultProviders, 1), preargs);
        addFixed(specificDef.getFixed(defaultProviders, 1), preargs);
        addMap(specificDef.getMap(defaultProviders, 1), preargs);
        addBase(specificDef.getBase(defaultProviders, 1), preargs);
        addStack(specificDef.getStack(defaultProviders, 1), preargs);
        addEntry(specificDef.getEntry(defaultProviders, 1), preargs);

        String[] libnames = null;
        LibrarySet[] libsets = specificDef.getActiveLibrarySets(defaultProviders, 1);
        if (libsets.length > 0) {
            libnames = addLibrarySets(task, libsets, preargs, midargs, endargs);
        }

        StringBuilder buf = new StringBuilder(getIdentifier());
        for (Vector<String> v : args) {
            for (String arg : v) {
                buf.append(' ').append(arg);
            }
        }
        String configId = buf.toString();

        String[][] options = new String[][]{
                new String[args.get(0).size() + args.get(1).size()],
                new String[args.get(2).size()]};
        options[0] = args.get(0).toArray(new String[0]);
        int offset = args.get(0).size();
        for (int i = 0; i < args.get(1).size(); i++) {
            options[0][i + offset] = args.get(1).elementAt(i);
        }
        options[1] = args.get(2).toArray(new String[0]);


        boolean rebuild = specificDef.getRebuild(baseDefs, 0);
        boolean map = specificDef.getMap(defaultProviders, 1);

        //task.log("libnames:"+libnames.length, Project.MSG_VERBOSE);
        return new CommandLineLinkerConfiguration(this, configId, options,
                paramArray, rebuild, map, debug, libnames, startupObject);
    }

    /**
     * Allows derived linker to decorate linker option.
     * Override by GccLinker to prepend a "-Wl," to
     * pass option to through gcc to linker.
     *
     * @param buf buffer that may be used and abused in the decoration process,
     *            must not be null.
     * @param arg linker argument
     * @return String
     */
    protected String decorateLinkerOption(StringBuilder buf, String arg) {
        return arg;
    }

    protected final String getCommand() {
        return command;
    }

    protected abstract String getCommandFileSwitch(String commandFile);


    public String getIdentifier() {
        if (identifier == null) {
            if (identifierArg == null) {
                identifier = getIdentifier(new String[]{command}, command);
            } else {
                identifier = getIdentifier(new String[]{command, identifierArg},
                        command);
            }
        }
        return identifier;
    }

    public final CommandLineLinker getLibtoolLinker() {
        if (libtoolLinker != null) {
            return libtoolLinker;
        }
        return this;
    }

    protected abstract int getMaximumCommandLength();

    public String[] getOutputFileNames(String baseName, VersionInfo versionInfo) {
        return new String[]{baseName + outputSuffix};
    }

    protected String[] getOutputFileSwitch(CCTask task, String outputFile) {
        return getOutputFileSwitch(outputFile);
    }

    protected abstract String[] getOutputFileSwitch(String outputFile);

    protected String getStartupObject(LinkType linkType) {
        return null;
    }

    /**
     * Performs a link using a command line linker
     *
     * @param task CCTask
     * @param outputFile File
     * @param sourceFiles an array of String
     * @param config CommandLineLinkerConfiguration
     * @throws BuildException if something goes wrong
     */
    public void link(CCTask task,
                     File outputFile,
                     String[] sourceFiles,
                     CommandLineLinkerConfiguration config) throws BuildException {
        File parentDir = new File(outputFile.getParent());
        String parentPath;
        try {
            parentPath = parentDir.getCanonicalPath();
        } catch (IOException ex) {
            parentPath = parentDir.getAbsolutePath();
        }
        String[] execArgs = prepareArguments(task, parentPath, outputFile.getName(),
                sourceFiles, config);
        int commandLength = 0;
        for (String execArg : execArgs) {
            commandLength += execArg.length() + 1;
        }

        //
        //   if command length exceeds maximum
        //       then create a temporary
        //       file containing everything but the command name
        if (commandLength >= this.getMaximumCommandLength()) {
            try {
                execArgs = prepareResponseFile(outputFile, execArgs);
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
        }

        int retval = runCommand(task, parentDir, execArgs);
        //
        //   if the process returned a failure code then
        //       throw an BuildException
        //
        if (retval != 0) {
            //
            //   construct the exception
            //
            throw new BuildException(this.getCommand() + " failed with return code " + retval, task.getLocation());
        }

    }


    /**
     * Prepares argument list for exec command.  Will return null
     * if command line would exceed allowable command line buffer.
     *
     * @param task        compilation task
     * @param outputDir   linker output directory
     * @param outputFile  linker output file
     * @param sourceFiles linker input files (.obj, .o, .res)
     * @param config      linker configuration
     * @return arguments for runTask
     */
    protected String[] prepareArguments(CCTask task, String outputDir, String outputFile,
                                        String[] sourceFiles,
                                        CommandLineLinkerConfiguration config) {
        String[] preargs = config.getPreArguments();
        String[] endargs = config.getEndArguments();
        String[] outputSwitch = getOutputFileSwitch(task, outputFile);
        int allArgsCount = preargs.length + 1 + outputSwitch.length + sourceFiles.length
                + endargs.length;
        if (isLibtool) {
            allArgsCount++;
        }
        String[] allArgs = new String[allArgsCount];
        int index = 0;
        if (isLibtool) {
            allArgs[index++] = "libtool";
        }
        allArgs[index++] = this.getCommand();
        StringBuilder buf = new StringBuilder();
        for (String prearg : preargs) {
            allArgs[index++] = decorateLinkerOption(buf, prearg);
        }
        for (String aSwitch : outputSwitch) {
            allArgs[index++] = aSwitch;
        }
        for (String sourceFile : sourceFiles) {
            allArgs[index++] = prepareFilename(buf, outputDir, sourceFile);
        }
        for (String endarg : endargs) {
            allArgs[index++] = decorateLinkerOption(buf, endarg);
        }
        return allArgs;
    }

    /**
     * Processes filename into argument form
     *
     * @param buf StringBuilder
     * @param outputDir String
     * @param sourceFile String
     * @return String
     */
    protected String prepareFilename(StringBuilder buf, String outputDir, String sourceFile) {
        String relativePath = CUtil.getRelativePath(outputDir, new File(sourceFile));
        return quoteFilename(buf, relativePath);
    }

    /**
     * Prepares argument list to execute the linker using a
     * response file.
     *
     * @param outputFile linker output file
     * @param args       output of prepareArguments
     * @return arguments for runTask
     * @throws IOException if something goes wrong
     */
    protected String[] prepareResponseFile(File outputFile, String[] args) throws IOException {
        String baseName = outputFile.getName();
        File commandFile = new File(outputFile.getParent(), baseName + ".rsp");
        FileWriter writer = new FileWriter(commandFile);
        int execArgCount = 1;
        if (isLibtool) {
            execArgCount++;
        }
        String[] execArgs = new String[execArgCount + 1];
        System.arraycopy(args, 0, execArgs, 0, execArgCount);
        execArgs[execArgCount] = getCommandFileSwitch(commandFile.toString());
        for (int i = execArgCount; i < args.length; i++) {
            //
            //   if embedded space and not quoted then
            //       quote argument
            if (args[i].contains(" ") && args[i].charAt(0) != '\"') {
                writer.write('\"');
                writer.write(args[i]);
                writer.write("\"\n");
            } else {
                writer.write(args[i]);
                writer.write('\n');
            }
        }
        writer.close();
        return execArgs;
    }


    protected String quoteFilename(StringBuilder buf, String filename) {
        if (filename.contains(" ")) {
            buf.setLength(0);
            buf.append('\"');
            buf.append(filename);
            buf.append('\"');
            return buf.toString();
        }
        return filename;
    }

    /**
     * This method is exposed so test classes can overload
     * and test the arguments without actually spawning the
     * compiler
     *
     * @param task CCTask
     * @param workingDir File
     * @param cmdline an array of String
     * @return int
     * @throws BuildException idf something goes wrong
     */
    protected int runCommand(CCTask task, File workingDir,
                             String[] cmdline) throws BuildException {
        return CUtil.runCommand(task, workingDir, cmdline, newEnvironment, env);
    }

    protected final void setCommand(String command) {
        this.command = command;
    }

}
