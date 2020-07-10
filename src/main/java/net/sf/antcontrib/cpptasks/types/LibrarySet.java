/*
 *
 * Copyright 2001-2006 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.types;

import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.FileVisitor;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;

import java.io.File;

/**
 * <p>
 * A set of library names. Libraries can also be added to a link by specifying
 * them in a fileset.
 * </p>
 * <p>
 * For most Unix-like compilers, libset will result in a series of -l and -L
 * linker arguments. For Windows compilers, the library names will be used to
 * locate the appropriate library files which will be added to the linkers
 * input file list as if they had been specified in a fileset.
 * </p>
 *
 * @author Mark A Russell {@literal <mark_russell@csgsystems.com>}
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public class LibrarySet extends DataType {
    private String dataset;
    private boolean explicitCaseSensitive;
    private String ifCond;
    private String[] libnames;
    private final FileSet set = new FileSet();
    private String unlessCond;
    private LibraryTypeEnum libraryType;

    public LibrarySet() {
        libnames = new String[0];
    }

    public void execute() throws BuildException {
        throw new BuildException(CUtil.STANDARD_EXCUSE);
    }

    /**
     * Gets the dataset. Used on OS390 if the libs are in a dataset.
     *
     * @return Returns a String
     */
    public String getDataset() {
        if (isReference()) {
            return getRef().getDataset();
        }
        return dataset;
    }

    public File getDir(final Project project) {
        if (isReference()) {
            return getRef().getDir(project);
        }
        return set.getDir(project);
    }

    protected FileSet getFileSet() {
        if (isReference()) {
            return getRef().getFileSet();
        }
        return set;
    }

    public String[] getLibs() {
        if (isReference()) {
            return getRef().getLibs();
        }
        return libnames.clone();
    }

    /**
     * Gets preferred library type
     *
     * @return library type, may be null.
     */
    public LibraryTypeEnum getType() {
        if (isReference()) {
            return getRef().getType();
        }
        return libraryType;
    }

    /**
     * Returns true if the define's if and unless conditions (if any) are
     * satisfied.
     *
     * @param p Project
     * @return boolean
     */
    public boolean isActive(final Project p) {
        if (p == null) {
            throw new NullPointerException("p");
        }
        if (ifCond != null) {
            String ifValue = p.getProperty(ifCond);
            if (ifValue != null) {
                if (ifValue.equals("no") || ifValue.equals("false")) {
                    throw new BuildException("property "
                            + ifCond
                            + " used as if condition has value "
                            + ifValue
                            + " which suggests a misunderstanding of if attributes");
                }
            } else {
                return false;
            }
        }
        if (unlessCond != null) {
            String unlessValue = p.getProperty(unlessCond);
            if (unlessValue != null) {
                if (unlessValue.equals("no") || unlessValue.equals("false")) {
                    throw new BuildException("property "
                            + unlessCond
                            + " used as unless condition has value "
                            + unlessValue
                            + " which suggests a misunderstanding of unless attributes");
                }
                return false;
            }
        }
        if (isReference()) {
            return getRef().isActive(getProject());
        }
        if (libnames.length == 0) {
            p.log("libnames not specified or empty.", Project.MSG_WARN);
            return false;
        }
        return true;
    }

    /**
     * Sets case sensitivity of the file system. If not set, will default to
     * the linker's case sensitivity.
     *
     * @param isCaseSensitive "true"|"on"|"yes" if file system is case sensitive,
     *                        "false"|"off"|"no" when not.
     */
    public void setCaseSensitive(final boolean isCaseSensitive) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        explicitCaseSensitive = true;
        set.setCaseSensitive(isCaseSensitive);
    }

    /**
     * Sets the dataset. Used on OS390 if the libs are in a dataset.
     *
     * @param dataset The dataset to set
     */
    public void setDataset(final String dataset) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.dataset = dataset;
    }

    /**
     * Library directory.
     *
     * @param dir library directory
     * @throws BuildException if something goes wrong
     */
    public void setDir(final File dir) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        set.setDir(dir);
    }

    /**
     * <p>
     * Sets the property name for the 'if' condition.
     * </p>
     * <p>
     * The library set will be ignored unless the property is defined.
     * </p>
     * <p>
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") will throw an exception when
     * evaluated.
     * </p>
     *
     * @param propName property name
     */
    public void setIf(String propName) {
        ifCond = propName;
    }

    /**
     * Comma-separated list of library names without leading prefixes, such as
     * "lib", or extensions, such as ".so" or ".a".
     *
     * @param libs StringArrayBuilder
     * @throws BuildException if something goes wrong
     */
    public void setLibs(final CUtil.StringArrayBuilder libs) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        libnames = libs.getValue();
        //
        //   earlier implementations would warn of suspicious library names
        //    (like libpthread for pthread or kernel.lib for kernel).
        //    visitLibraries now provides better feedback and ld type linkers
        //    should provide adequate feedback so the check here is not necessary.
    }

    public void setProject(final Project project) {
        set.setProject(project);
        super.setProject(project);
    }

    /**
     * <p>
     * Set the property name for the 'unless' condition.
     * </p>
     * <p>
     * If named property is set, the library set will be ignored.
     * </p>
     * <p>
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") of the behavior will throw an
     * exception when evaluated.
     * </p>
     *
     * @param propName name of property
     */
    public void setUnless(String propName) {
        unlessCond = propName;
    }

    /**
     * Sets the preferred library type. Supported values "shared", "static", and
     * "framework".  "framework" is equivalent to "shared" on non-Darwin platforms.
     *
     * @param type LibraryTypeEnum
     */
    public void setType(final LibraryTypeEnum type) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.libraryType = type;
    }

    public void visitLibraries(final Project project, final Linker linker, final File[] libpath,
                               final FileVisitor visitor) throws BuildException {
        if (isReference()) {
            getRef().visitLibraries(project, linker, libpath, visitor);
        }
        //
        //  if there was a libs attribute then
        //     add the corresponding patterns to the FileSet
        //
        if (libnames != null) {
            for (String libname : libnames) {
                String[] patterns = linker.getLibraryPatterns(new String[]{libname}, libraryType);
                if (patterns.length > 0) {
                    FileSet localSet = (FileSet) set.clone();
                    //
                    //   unless explicitly set
                    //      will default to the linker case sensitivity
                    //
                    if (!explicitCaseSensitive) {
                        boolean linkerCaseSensitive = linker.isCaseSensitive();
                        localSet.setCaseSensitive(linkerCaseSensitive);
                    }
                    //
                    //   add all the patterns for this libname
                    //
                    for (String pattern : patterns) {
                        PatternSet.NameEntry entry = localSet.createInclude();
                        entry.setName(pattern);
                    }
                    int matches = 0;
                    //
                    //  if there was no specified directory then
                    //     run through the libpath backwards
                    //
                    if (localSet.getDir(project) == null) {
                        //
                        //  scan libpath in reverse order
                        //     to give earlier entries priority
                        //
                        int j = libpath.length - 1;
                        while (j >= 0) {
                            FileSet clone = (FileSet) localSet.clone();
                            clone.setDir(libpath[j]);
                            DirectoryScanner scanner = clone.getDirectoryScanner(project);
                            File basedir = scanner.getBasedir();
                            String[] files = scanner.getIncludedFiles();
                            matches += files.length;
                            for (String file : files) {
                                visitor.visit(basedir, file);
                            }
                            j--;
                        }
                    } else {
                        DirectoryScanner scanner = localSet.getDirectoryScanner(project);
                        File basedir = scanner.getBasedir();
                        String[] files = scanner.getIncludedFiles();
                        matches += files.length;
                        for (String file : files) {
                            visitor.visit(basedir, file);
                        }
                    }
                    //
                    //  TODO: following section works well for Windows
                    //      style linkers but unnecessary fails
                    //     Unix style linkers.  Will need to revisit.
                    //
                    if (matches == 0 && false) {
                        StringBuilder msg = new StringBuilder("No file matching ");
                        if (patterns.length == 1) {
                            msg.append("pattern (");
                            msg.append(patterns[0]);
                            msg.append(")");
                        } else {
                            msg.append("patterns (\"");
                            msg.append(patterns[0]);
                            for (int k = 1; k < patterns.length; k++) {
                                msg.append(", ");
                                msg.append(patterns[k]);
                            }
                            msg.append(")");
                        }
                        msg.append(" for library name \"");
                        msg.append(libname);
                        msg.append("\" was found.");
                        throw new BuildException(msg.toString());
                    }
                }
            }
        }
    }

    private LibrarySet getRef() {
        return getCheckedRef(LibrarySet.class, "LibrarySet");
    }
}
