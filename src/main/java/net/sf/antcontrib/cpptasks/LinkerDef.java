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
package net.sf.antcontrib.cpptasks;

import net.sf.antcontrib.cpptasks.compiler.CommandLineLinker;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.compiler.Processor;
import net.sf.antcontrib.cpptasks.gcc.GccLinker;
import net.sf.antcontrib.cpptasks.types.FlexLong;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LinkerArgument;
import net.sf.antcontrib.cpptasks.types.SystemLibrarySet;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FlexInteger;

import java.io.File;
import java.util.Vector;

/**
 * <p>
 * A linker definition. linker elements may be placed either as children of a
 * cc element or the project element. A linker element with an id attribute may
 * be referenced by linker elements with refid or extends attributes.
 * </p>
 *
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public class LinkerDef extends ProcessorDef {
    private long base;
    private String entry;
    private Boolean fixed;
    private Boolean incremental;
    private final Vector<LibrarySet> librarySets = new Vector<LibrarySet>();
    private Boolean map;
    private int stack;
    private final Vector<LibrarySet> sysLibrarySets = new Vector<LibrarySet>();
    /**
     * Default constructor
     *
     * @see java.lang.Object#Object()
     */
    public LinkerDef() {
        base = -1;
        stack = -1;
    }

    private void addActiveLibrarySet(Project project, Vector<LibrarySet> libsets,
                                     Vector<LibrarySet> srcSets) {
        for (LibrarySet set : srcSets) {
            if (set.isActive(project)) {
                libsets.addElement(set);
            }
        }
    }

    private void addActiveSystemLibrarySets(Project project, Vector<LibrarySet> libsets) {
        addActiveLibrarySet(project, libsets, sysLibrarySets);
    }

    private void addActiveUserLibrarySets(Project project, Vector<LibrarySet> libsets) {
        addActiveLibrarySet(project, libsets, librarySets);
    }

    /**
     * Adds a linker command-line arg.
     *
     * @param arg LinkerArgument
     */
    public void addConfiguredLinkerArg(LinkerArgument arg) {
        addConfiguredProcessorArg(arg);
    }

    /**
     * Adds a compiler command-line arg.
     *
     * @param param LinkerParam
     */
    public void addConfiguredLinkerParam(LinkerParam param) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        addConfiguredProcessorParam(param);
    }

    /**
     * Adds a system library set.
     *
     * @param libset LibrarySet
     */
    public void addLibset(LibrarySet libset) {
        if (isReference()) {
            throw super.noChildrenAllowed();
        }
        if (libset == null) {
            throw new NullPointerException("libset");
        }
        librarySets.addElement(libset);
    }

    /**
     * Adds a system library set.
     *
     * @param libset SystemLibrarySet
     */
    public void addSyslibset(SystemLibrarySet libset) {
        if (isReference()) {
            throw super.noChildrenAllowed();
        }
        if (libset == null) {
            throw new NullPointerException("libset");
        }
        sysLibrarySets.addElement(libset);
    }

    public void execute() throws BuildException {
        throw new BuildException(CUtil.STANDARD_EXCUSE);
    }

    /**
     * Returns an array of active library sets for this linker definition.
     *
     * @param defaultProviders an array of LinkerDef
     * @param index int
     * @return an array of LibrarySet
     */
    public LibrarySet[] getActiveLibrarySets(LinkerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getActiveUserLibrarySets(defaultProviders, index);
        }
        Project p = getProject();
        Vector<LibrarySet> libsets = new Vector<LibrarySet>();
        for (int i = index; i < defaultProviders.length; i++) {
            defaultProviders[i].addActiveUserLibrarySets(p, libsets);
        }
        addActiveUserLibrarySets(p, libsets);
        for (int i = index; i < defaultProviders.length; i++) {
            defaultProviders[i].addActiveSystemLibrarySets(p, libsets);
        }
        addActiveSystemLibrarySets(p, libsets);
        return libsets.toArray(new LibrarySet[0]);
    }

    /**
     * Returns an array of active library sets for this linker definition.
     *
     * @param defaultProviders an array of LinkerDef
     * @param index int
     * @return an array of LibrarySet
     */
    public LibrarySet[] getActiveSystemLibrarySets(LinkerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getActiveUserLibrarySets(defaultProviders, index);
        }
        Project p = getProject();
        Vector<LibrarySet> libsets = new Vector<LibrarySet>();
        for (int i = index; i < defaultProviders.length; i++) {
            defaultProviders[i].addActiveSystemLibrarySets(p, libsets);
        }
        addActiveSystemLibrarySets(p, libsets);
        return libsets.toArray(new LibrarySet[0]);
    }

    /**
     * Returns an array of active library sets for this linker definition.
     *
     * @param defaultProviders an array of LinkerDef
     * @param index int
     * @return an array of LibrarySet
     */
    public LibrarySet[] getActiveUserLibrarySets(LinkerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getActiveUserLibrarySets(defaultProviders, index);
        }
        Project p = getProject();
        Vector<LibrarySet> libsets = new Vector<LibrarySet>();
        for (int i = index; i < defaultProviders.length; i++) {
            defaultProviders[i].addActiveUserLibrarySets(p, libsets);
        }
        addActiveUserLibrarySets(p, libsets);
        return libsets.toArray(new LibrarySet[0]);
    }

    public long getBase(LinkerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getBase(defaultProviders, index);
        }
        if (base <= 0 && defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getBase(defaultProviders, index + 1);
        }
        return base;
    }

    public Boolean getFixed(LinkerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getFixed(defaultProviders, index);
        }
        if (fixed == null && defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getFixed(defaultProviders, index + 1);
        }
        return fixed;
    }

    public boolean getIncremental(LinkerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getIncremental(defaultProviders, index);
        }
        if (incremental != null) {
            return incremental;
        }
        if (defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getIncremental(defaultProviders, index + 1);
        }
        return false;
    }

    public boolean getMap(LinkerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getMap(defaultProviders, index);
        }
        if (map != null) {
            return map;
        }
        if (defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getMap(defaultProviders, index + 1);
        }
        return false;
    }

    public String getEntry(LinkerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getEntry(defaultProviders, index);
        }
        if (entry != null) {
            return entry;
        }
        if (defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getEntry(defaultProviders, index + 1);
        }
        return null;
    }

    public Processor getProcessor() {
        Linker linker = (Linker) super.getProcessor();
        if (linker == null) {
            linker = GccLinker.getInstance();
        }
        if (getLibtool() && linker instanceof CommandLineLinker) {
            CommandLineLinker cmdLineLinker = (CommandLineLinker) linker;
            linker = cmdLineLinker.getLibtoolLinker();
        }
        return linker;
    }

    public Processor getProcessor(LinkType linkType) {
        Processor proc = getProcessor();
        return proc.getLinker(linkType);
    }

    public int getStack(LinkerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getStack(defaultProviders, index);
        }
        if (stack < 0 && defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getStack(defaultProviders, index + 1);
        }
        return stack;
    }

    /**
     * Sets the base address. May be specified in either decimal or hex.
     *
     * @param base base address
     */
    public void setBase(FlexLong base) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.base = base.longValue();
    }

    /**
     * Sets the starting address.
     *
     * @param entry function name
     */
    public void setEntry(String entry) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.entry = entry;
    }

    /**
     * If true, marks the file to be loaded only at its preferred address.
     *
     * @param fixed boolean
     */
    public void setFixed(boolean fixed) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.fixed = booleanValueOf(fixed);
    }

    /**
     * If true, allows incremental linking.
     *
     * @param incremental boolean
     */
    public void setIncremental(boolean incremental) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.incremental = booleanValueOf(incremental);
    }

    /**
     * If set to true, a map file will be produced.
     *
     * @param map boolean
     */
    public void setMap(boolean map) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.map = booleanValueOf(map);
    }

    /**
     * <p>
     * Sets linker type.
     * </p>
     * <table style="width:100%;border-collapse:collapse;border:1px solid black;">
     * <caption></caption>
     * <thead><tr><th>Supported linkers</th></tr></thead>
     * <tbody>
     * <tr>
     * <td>gcc</td>
     * <td>Gcc Linker</td>
     * </tr>
     * <tr>
     * <td>g++</td>
     * <td>G++ Linker</td>
     * </tr>
     * <tr>
     * <td>ld</td>
     * <td>Ld Linker</td>
     * </tr>
     * <tr>
     * <td>ar</td>
     * <td>Gcc Librarian</td>
     * </tr>
     * <tr>
     * <td>msvc</td>
     * <td>Microsoft Linker</td>
     * </tr>
     * <tr>
     * <td>bcc</td>
     * <td>Borland Linker</td>
     * </tr>
     * <tr>
     * <td>df</td>
     * <td>Compaq Visual Fortran Linker</td>
     * </tr>
     * <tr>
     * <td>icl</td>
     * <td>Intel Linker for Windows (IA-32)</td>
     * </tr>
     * <tr>
     * <td>ecl</td>
     * <td>Intel Linker for Windows (IA-64)</td>
     * </tr>
     * <tr>
     * <td>icc</td>
     * <td>Intel Linker for Linux (IA-32)</td>
     * </tr>
     * <tr>
     * <td>ecc</td>
     * <td>Intel Linker for Linux (IA-64)</td>
     * </tr>
     * <tr>
     * <td>CC</td>
     * <td>Sun ONE Linker</td>
     * </tr>
     * <tr>
     * <td>aCC</td>
     * <td>HP aC++ Linker</td>
     * </tr>
     * <tr>
     * <td>os390</td>
     * <td>OS390 Linker</td>
     * </tr>
     * <tr>
     * <td>os390batch</td>
     * <td>OS390 Linker</td>
     * </tr>
     * <tr>
     * <td>os400</td>
     * <td>IccLinker</td>
     * </tr>
     * <tr>
     * <td>sunc89</td>
     * <td>C89 Linker</td>
     * </tr>
     * <tr>
     * <td>xlC</td>
     * <td>VisualAge Linker</td>
     * </tr>
     * <tr>
     * <td>wcl</td>
     * <td>OpenWatcom C/C++ linker</td>
     * </tr>
     * <tr>
     * <td>wfl</td>
     * <td>OpenWatcom FORTRAN linker</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param name LinkerEnum
     * @throws BuildException if something goes wrong
     */
    public void setName(LinkerEnum name) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        super.setProcessor(name.getLinker());
    }

    protected void setProcessor(Processor proc) throws BuildException {
        Linker linker;
        if (proc instanceof Linker) {
            linker = (Linker) proc;
        } else {
            linker = proc.getLinker(new LinkType());
        }
        super.setProcessor(linker);
    }

    /**
     * Sets stack size in bytes.
     *
     * @param stack FlexInteger
     */
    public void setStack(FlexInteger stack) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.stack = stack.intValue();
    }

    public void visitSystemLibraries(Linker linker, FileVisitor libraryVisitor) {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("project must be set");
        }
        if (isReference()) {
            getRef().visitSystemLibraries(linker, libraryVisitor);
        } else {
            //
            //   if this linker extends another,
            //      visit its libraries first
            //
            LinkerDef extendsDef = (LinkerDef) getExtends();
            if (extendsDef != null) {
                extendsDef.visitSystemLibraries(linker, libraryVisitor);
            }
            if (sysLibrarySets.size() > 0) {
                File[] libpath = linker.getLibraryPath();
                for (LibrarySet set : sysLibrarySets) {
                    if (set.isActive(p)) {
                        set.visitLibraries(p, linker, libpath, libraryVisitor);
                    }
                }
            }
        }
    }

    public void visitUserLibraries(Linker linker, FileVisitor libraryVisitor) {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("project must be set");
        }
        if (isReference()) {
            getRef().visitUserLibraries(linker, libraryVisitor);
        } else {
            //
            //   if this linker extends another,
            //      visit its libraries first
            //
            LinkerDef extendsDef = (LinkerDef) getExtends();
            if (extendsDef != null) {
                extendsDef.visitUserLibraries(linker, libraryVisitor);
            }
            //
            //   visit the user libraries
            //
            if (librarySets.size() > 0) {
                File[] libpath = linker.getLibraryPath();
                for (LibrarySet set : librarySets) {
                    if (set.isActive(p)) {
                        set.visitLibraries(p, linker, libpath, libraryVisitor);
                    }
                }
            }
        }
    }

    private LinkerDef getRef() {
        return getCheckedRef(LinkerDef.class, "LinkerDef");
    }
}
