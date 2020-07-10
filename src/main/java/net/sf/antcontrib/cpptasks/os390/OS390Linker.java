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
package net.sf.antcontrib.cpptasks.os390;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.VersionInfo;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinker;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinkerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import static net.sf.antcontrib.cpptasks.CUtil.getPathFromEnvironment;

/**
 * Adapter for the IBM (R) OS/390 (tm) Linker.
 *
 * @author Hiram Chirino {@literal <cojonudo14@hotmail.com>}
 */
public final class OS390Linker extends CommandLineLinker {
    private static final OS390Linker datasetLinker = new OS390Linker();
    private static final OS390Linker dllLinker = new OS390Linker("", ".dll");
    private static final OS390Linker instance = new OS390Linker("", "");

    public static OS390Linker getDataSetInstance() {
        return datasetLinker;
    }

    public static OS390Linker getInstance() {
        return instance;
    }

    private final boolean isADatasetLinker;
    File outputFile;
    private final String outputPrefix;
    CCTask task;

    private OS390Linker() {
        super("cxx", "/bogus", new String[]{".o", ".a", ".lib", ".xds"},
                new String[]{".dll", ".x"}, ".xds", false, null);
        this.outputPrefix = "";
        this.isADatasetLinker = true;
    }

    private OS390Linker(String outputPrefix, String outputSuffix) {
        super("cxx", "/bogus", new String[]{".o", ".a", ".lib", ".x"},
                new String[]{".dll"}, outputSuffix, false, null);
        this.outputPrefix = outputPrefix;
        this.isADatasetLinker = false;
    }

    protected void addBase(long base, Vector<String> args) {
    }

    protected void addFixed(Boolean fixed, Vector<String> args) {
    }

    protected void addImpliedArgs(boolean debug, LinkType linkType, Vector<String> args) {
        if (linkType.isSharedLibrary()) {
            args.addElement("-W");
            args.addElement("l,DLL");
        }
    }

    protected void addIncremental(boolean incremental, Vector<String> args) {
    }

    /*
     * @see CommandLineLinker#addLibrarySets(LibrarySet[], Vector, Vector,
     *      Vector)
     */
    protected String[] addLibrarySets(CCTask task, LibrarySet[] libsets,
                                      Vector<String> preargs, Vector<String> midargs, Vector<String> endargs) {
        // If yo want to link against a library sitting in a dataset and
        // not in the HFS, you can just use the //'dataset' notation
        // to specify it. e.g:
        // <libset dir="." libs="//'MQM.V5R2M0.SCSQLOAD'"/>
        //
        // We have to have special handling here because the file is not
        // on the normal filesystem so the task will not normally include it
        // as part of the link command.
        if (libsets != null) {
            for (LibrarySet libset : libsets) {
                for (String lib : libset.getLibs()) {
                    if (lib.startsWith("//")) {
                        endargs.addElement("-l");
                        endargs.addElement(lib);
                        continue;
                    }
                    String ds = libset.getDataset();
                    if (ds != null) {
                        endargs.addElement("//'" + ds + "(" + lib + ")'");
                    }
                }
            }
        }
        return super.addLibrarySets(task, libsets, preargs, midargs, endargs);
    }

    protected void addMap(boolean map, Vector<String> args) {
    }

    protected void addStack(int stack, Vector<String> args) {
    }

    protected void addEntry(String entry, Vector<String> args) {
    }

    public String getCommandFileSwitch(String commandFile) {
        return "@" + commandFile;
    }

    public File[] getLibraryPath() {
        return getPathFromEnvironment("LIB", ";");
    }

    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        StringBuilder buf = new StringBuilder();
        String[] patterns = new String[libnames.length * 3];
        int offset = addLibraryPatterns(libnames, buf, "lib", ".a", patterns, 0);
        offset = addLibraryPatterns(libnames, buf, "", ".x", patterns, offset);
        offset = addLibraryPatterns(libnames, buf, "", ".o", patterns, offset);
        return patterns;
    }

    private static int addLibraryPatterns(String[] libnames, StringBuilder buf,
                                          String prefix, String extension, String[] patterns, int offset) {
        for (int i = 0; i < libnames.length; i++) {
            buf.setLength(0);
            buf.append(prefix);
            buf.append(libnames[i]);
            buf.append(extension);
            patterns[offset + i] = buf.toString();
        }
        return offset + libnames.length;
    }

    public Linker getLinker(LinkType linkType) {
        if (this == datasetLinker) {
            return datasetLinker;
        }
        if (linkType.isSharedLibrary()) {
            return dllLinker;
        }
        return instance;
    }

    public int getMaximumCommandLength() {
        return Integer.MAX_VALUE;
    }

    public String[] getOutputFileNames(String baseName, VersionInfo versionInfo) {
        String[] baseNames = super.getOutputFileNames(baseName, versionInfo);
        if (outputPrefix.length() > 0) {
            for (int i = 0; i < baseNames.length; i++) {
                baseNames[i] = outputPrefix + baseNames[i];
            }
        }
        return baseNames;
    }

    protected String[] getOutputFileSwitch(CCTask task, String outputFile) {
        if (isADatasetLinker && task.getDataset() != null) {
            String ds = task.getDataset();
            outputFile = "//'" + ds + "(" + outputFile + ")'";
        }
        return getOutputFileSwitch(outputFile);
    }

    public String[] getOutputFileSwitch(String outputFile) {
        return new String[]{"-o", outputFile};
    }

    public boolean isCaseSensitive() {
        return OS390Processor.isCaseSensitive();
    }

    /*
     * @see CommandLineLinker#link(Task, File, String[],
     *      CommandLineLinkerConfiguration)
     */
    public void link(CCTask task, File outputFile, String[] sourceFiles,
                     CommandLineLinkerConfiguration config) throws BuildException {
        this.task = task;
        this.outputFile = outputFile;
        if (isADatasetLinker) {
            int dotPos = outputFile.getName().indexOf(".");
            if (dotPos >= 0) {
                String newname = outputFile.getName().substring(0, dotPos);
                outputFile = new File(outputFile.getParent(), newname);
            }
        }
        super.link(task, outputFile, sourceFiles, config);
    }

    /*
     * @see CommandLineLinker#runCommand(Task, File, String[])
     */
    protected int runCommand(CCTask task, File workingDir,
                             String[] cmdline) throws BuildException {
        int rc = super.runCommand(task, workingDir, cmdline);
        // create the .xds file if everything was ok.
        if (rc == 0) {
            try {
                outputFile.delete();
                new FileOutputStream(outputFile).close();
            } catch (IOException e) {
                throw new BuildException(e.getMessage());
            }
        }
        return rc;
    }
}
