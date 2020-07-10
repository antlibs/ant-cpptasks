/*
 *
 * Copyright 2002-2005 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.borland;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinker;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinkerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import static net.sf.antcontrib.cpptasks.CUtil.getPathFromEnvironment;

/**
 * Adapter for the Borland(r) tlib Librarian
 *
 * @author Curt Arnold
 */
public class BorlandLibrarian extends CommandLineLinker {
    private static final BorlandLibrarian instance = new BorlandLibrarian();

    public static BorlandLibrarian getInstance() {
        return instance;
    }

    private BorlandLibrarian() {
        super("tlib", "--version", new String[]{".obj"}, new String[0], ".lib", false,
                null);
    }

    protected void addBase(long base, Vector<String> args) {
    }

    protected void addFixed(Boolean fixed, Vector<String> args) {
    }

    protected void addImpliedArgs(boolean debug, LinkType linkType, Vector<String> args) {
    }

    protected void addIncremental(boolean incremental, Vector<String> args) {
    }

    protected void addMap(boolean map, Vector<String> args) {
    }

    protected void addStack(int stack, Vector<String> args) {
    }

    /* (non-Javadoc)
     * @see net.sf.antcontrib.cpptasks.compiler.CommandLineLinker#addEntry(int, java.util.Vector)
     */
    protected void addEntry(String entry, Vector<String> args) {
    }

    protected String getCommandFileSwitch(String cmdFile) {
        //
        //  tlib requires quotes around paths containing -
        //     ilink32 doesn't like them
        StringBuilder buf = new StringBuilder("@");
        BorlandProcessor.quoteFile(buf, cmdFile);
        return buf.toString();
    }

    public File[] getLibraryPath() {
        return getPathFromEnvironment("LIB", ";");
    }

    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        return BorlandProcessor.getLibraryPatterns(libnames, libType);
    }

    public Linker getLinker(LinkType type) {
        return BorlandLinker.getInstance().getLinker(type);
    }

    public int getMaximumCommandLength() {
        return 1024;
    }

    public String[] getOutputFileSwitch(String outFile) {
        return BorlandProcessor.getOutputFileSwitch(outFile);
    }

    public boolean isCaseSensitive() {
        return BorlandProcessor.isCaseSensitive();
    }

    /**
     * <p>
     * Gets identifier for the linker.
     * </p>
     * <p>
     * TLIB will lockup when attempting to get version
     * information.  Since the Librarian version isn't critical
     * just return a stock response.
     * </p>
     */
    public String getIdentifier() {
        return "TLIB 4.5 Copyright (c) 1987, 1999 Inprise Corporation";
    }

    /**
     * Prepares argument list for exec command.
     *
     * @param outputDir   linker output directory
     * @param outputName  linker output name
     * @param sourceFiles linker input files (.obj, .o, .res)
     * @param config      linker configuration
     * @return arguments for runTask
     */
    protected String[] prepareArguments(CCTask task, String outputDir, String outputName,
                                        String[] sourceFiles,
                                        CommandLineLinkerConfiguration config) {
        String[] preargs = config.getPreArguments();
        String[] endargs = config.getEndArguments();
        StringBuilder buf = new StringBuilder();
        Vector<String> execArgs = new Vector<String>();

        execArgs.addElement(this.getCommand());
        String outputFileName = new File(outputDir, outputName).toString();
        execArgs.addElement(quoteFilename(buf, outputFileName));

        for (String prearg : preargs) {
            execArgs.addElement(prearg);
        }

        //
        //   add a place-holder for page size
        //
        int pageSizeIndex = execArgs.size();
        execArgs.addElement(null);

        int objBytes = 0;

        for (String sourceFile : sourceFiles) {
            String last4 = sourceFile.substring(sourceFile.length() - 4).toLowerCase();
            if ((!last4.equals(".def") && !last4.equals(".res")) && !last4.equals(".lib")) {
                execArgs.addElement("+" + quoteFilename(buf, sourceFile));
                objBytes += new File(sourceFile).length();
            }
        }

        for (String endarg : endargs) {
            execArgs.addElement(endarg);
        }

        String[] execArguments = execArgs.toArray(new String[0]);

        int minPageSize = objBytes >> 16;
        int pageSize = 0;
        for (int i = 4; i <= 15; i++) {
            pageSize = 1 << i;
            if (pageSize > minPageSize) break;
        }
        execArguments[pageSizeIndex] = "/P" + pageSize;

        return execArguments;
    }

    /**
     * Prepares argument list to execute the linker using a response file.
     *
     * @param outputFile linker output file
     * @param args       output of prepareArguments
     * @return arguments for runTask
     */
    protected String[] prepareResponseFile(File outputFile, String[] args) throws IOException {
        String[] cmdargs = BorlandProcessor.prepareResponseFile(outputFile, args, " & \n");
        cmdargs[cmdargs.length - 1] = getCommandFileSwitch(cmdargs[cmdargs.length - 1]);
        return cmdargs;
    }

    /**
     * Builds a library
     */
    public void link(CCTask task,
                     File outputFile,
                     String[] sourceFiles,
                     CommandLineLinkerConfiguration config) throws BuildException {
        //
        //  delete any existing library
        outputFile.delete();
        //
        //  build a new library
        super.link(task, outputFile, sourceFiles, config);
    }

    /**
     * Encloses problematic file names within quotes.
     *
     * @param buf      StringBuilder
     * @param filename source file name
     * @return filename potentially enclosed in quotes.
     */
    protected String quoteFilename(StringBuilder buf, String filename) {
        buf.setLength(0);
        BorlandProcessor.quoteFile(buf, filename);
        return buf.toString();
    }

}
