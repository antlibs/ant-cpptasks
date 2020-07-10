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
package net.sf.antcontrib.cpptasks;

import net.sf.antcontrib.cpptasks.compiler.CommandLineCompiler;
import net.sf.antcontrib.cpptasks.compiler.Compiler;
import net.sf.antcontrib.cpptasks.compiler.Processor;
import net.sf.antcontrib.cpptasks.gcc.GccCCompiler;
import net.sf.antcontrib.cpptasks.types.CompilerArgument;
import net.sf.antcontrib.cpptasks.types.ConditionalPath;
import net.sf.antcontrib.cpptasks.types.DefineSet;
import net.sf.antcontrib.cpptasks.types.IncludePath;
import net.sf.antcontrib.cpptasks.types.SystemIncludePath;
import net.sf.antcontrib.cpptasks.types.UndefineArgument;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.util.Vector;

/**
 * <p>
 * A compiler definition. compiler elements may be placed either as children of
 * a cc element or the project element. A compiler element with an id attribute
 * may be referenced from compiler elements with refid or extends attributes.
 * </p>
 *
 * @author Adam Murdoch
 */
public final class CompilerDef extends ProcessorDef {
    /**
     * The source file sets.
     */
    private final Vector<DefineSet> defineSets = new Vector<DefineSet>();
    private Boolean exceptions;
    private Boolean rtti;
    private final Vector<ConditionalPath> includePaths = new Vector<ConditionalPath>();
    private Boolean multithreaded;
    private final Vector<PrecompileDef> precompileDefs = new Vector<PrecompileDef>();
    private final Vector<ConditionalPath> sysIncludePaths = new Vector<ConditionalPath>();
    private OptimizationEnum optimization;
    private int warnings = -1;

    public CompilerDef() {
    }

    /**
     * Adds a compiler command-line arg.
     *
     * @param arg CompilerArgument
     */
    public void addConfiguredCompilerArg(CompilerArgument arg) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        addConfiguredProcessorArg(arg);
    }

    /**
     * Adds a compiler command-line arg.
     *
     * @param param CompilerParam
     */
    public void addConfiguredCompilerParam(CompilerParam param) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        addConfiguredProcessorParam(param);
    }

    /**
     * Adds a defineset.
     *
     * @param defs DefineSet
     */
    public void addConfiguredDefineset(DefineSet defs) {
        if (defs == null) {
            throw new NullPointerException("defs");
        }
        if (isReference()) {
            throw noChildrenAllowed();
        }
        defineSets.addElement(defs);
    }

    /**
     * Creates an include path.
     *
     * @return IncludePath
     */
    public IncludePath createIncludePath() {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("project must be set");
        }
        if (isReference()) {
            throw noChildrenAllowed();
        }
        IncludePath path = new IncludePath(p);
        includePaths.addElement(path);
        return path;
    }

    /**
     * Specifies precompilation prototype file and exclusions.
     *
     * @return PrecompileDef
     */
    public PrecompileDef createPrecompile() throws BuildException {
        Project p = getProject();
        if (isReference()) {
            throw noChildrenAllowed();
        }
        PrecompileDef precomp = new PrecompileDef();
        precomp.setProject(p);
        precompileDefs.addElement(precomp);
        return precomp;
    }

    /**
     * <p>
     * Creates a system include path. Locations and timestamps of files located
     * using the system include paths are not used in dependency analysis.
     * </p>
     * <p>
     * Standard include locations should not be specified. The compiler
     * adapters should recognized the settings from the appropriate environment
     * variables or configuration files.
     * </p>
     *
     * @return SystemIncludePath
     */
    public SystemIncludePath createSysIncludePath() {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("project must be set");
        }
        if (isReference()) {
            throw noChildrenAllowed();
        }
        SystemIncludePath path = new SystemIncludePath(p);
        sysIncludePaths.addElement(path);
        return path;
    }

    public void execute() throws BuildException {
        throw new BuildException(CUtil.STANDARD_EXCUSE);
    }

    public UndefineArgument[] getActiveDefines() {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("project must be set before this call");
        }
        if (isReference()) {
            return getRef().getActiveDefines();
        }
        Vector<UndefineArgument> actives = new Vector<UndefineArgument>();
        for (DefineSet currentSet : defineSets) {
            for (UndefineArgument define : currentSet.getDefines()) {
                if (define.isActive(p)) {
                    actives.addElement(define);
                }
            }
        }
        return actives.toArray(new UndefineArgument[0]);
    }

    /**
     * Returns the compiler-specific include path.
     *
     * @return array of String
     */
    public String[] getActiveIncludePaths() {
        if (isReference()) {
            return getRef().getActiveIncludePaths();
        }
        return getActivePaths(includePaths);
    }

    private String[] getActivePaths(Vector<ConditionalPath> paths) {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("project not set");
        }
        Vector<String> activePaths = new Vector<String>();
        for (ConditionalPath path : paths) {
            if (path.isActive(p)) {
                for (String pathEntry : path.list()) {
                    activePaths.addElement(pathEntry);
                }
            }
        }
        return activePaths.toArray(new String[0]);
    }

    public PrecompileDef getActivePrecompile(CompilerDef ccElement) {
        if (isReference()) {
            return getRef().getActivePrecompile(ccElement);
        }
        for (PrecompileDef current : precompileDefs) {
            if (current.isActive()) {
                return current;
            }
        }
        CompilerDef extendedDef = (CompilerDef) getExtends();
        if (extendedDef != null) {
            PrecompileDef current = extendedDef.getActivePrecompile(null);
            if (current != null) {
                return current;
            }
        }
        if (ccElement != null && getInherit()) {
            return ccElement.getActivePrecompile(null);
        }
        return null;
    }

    public String[] getActiveSysIncludePaths() {
        if (isReference()) {
            return getRef().getActiveSysIncludePaths();
        }
        return getActivePaths(sysIncludePaths);
    }

    public final boolean getExceptions(CompilerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getExceptions(defaultProviders, index);
        }
        if (exceptions != null) {
            return exceptions;
        }
        if (defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getExceptions(defaultProviders, index + 1);
        }
        return false;
    }

    public final Boolean getRtti(CompilerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getRtti(defaultProviders, index);
        }
        if (rtti != null) {
            return rtti;
        }
        if (defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getRtti(defaultProviders, index + 1);
        }
        return null;
    }

    public final OptimizationEnum getOptimization(CompilerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getOptimization(defaultProviders, index);
        }
        if (optimization != null) {
            return optimization;
        }
        if (defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getOptimization(defaultProviders, index + 1);
        }
        return null;
    }

    public boolean getMultithreaded(CompilerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getMultithreaded(defaultProviders, index);
        }
        if (multithreaded != null) {
            return multithreaded;
        }
        if (defaultProviders != null && index < defaultProviders.length) {
            return defaultProviders[index].getMultithreaded(defaultProviders, index + 1);
        }
        return true;
    }

    public Processor getProcessor() {
        Processor processor = super.getProcessor();
        if (processor == null) {
            processor = GccCCompiler.getInstance();
        }
        if (getLibtool() && processor instanceof CommandLineCompiler) {
            processor = ((CommandLineCompiler) processor).getLibtoolCompiler();
        }
        return processor;
    }

    public int getWarnings(CompilerDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getWarnings(defaultProviders, index);
        }
        if (warnings == -1) {
            if (defaultProviders != null && index < defaultProviders.length) {
                return defaultProviders[index].getWarnings(defaultProviders, index + 1);
            }
        }
        return warnings;
    }

    /**
     * Sets the default compiler adapter. Use the "name" attribute when the
     * compiler is a supported compiler.
     *
     * @param classname fully qualified classname which implements CompilerAdapter
     */
    public void setClassname(String classname) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        super.setClassname(classname);
        Processor proc = getProcessor();
        if (!(proc instanceof Compiler)) {
            throw new BuildException(classname + " does not implement Compiler");
        }
    }

    /**
     * Enables or disables exception support.
     *
     * @param exceptions if true, exceptions are supported.
     */
    public void setExceptions(boolean exceptions) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.exceptions = booleanValueOf(exceptions);
    }

    /**
     * Enables or disables run-time type information.
     *
     * @param rtti if true, run-time type information is supported.
     */
    public void setRtti(boolean rtti) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.rtti = booleanValueOf(rtti);
    }

    /**
     * Enables or disables generation of multithreaded code. Unless specified,
     * multithreaded code generation is enabled.
     *
     * @param multithreaded If true, generated code may be multithreaded.
     */
    public void setMultithreaded(boolean multithreaded) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.multithreaded = booleanValueOf(multithreaded);
    }

    /**
     * <p>
     * Sets compiler type.
     * </p>
     * <table style="width:100%;border-collapse:collapse;border:1px solid black;">
     * <caption></caption>
     * <thead><tr><th>Supported compilers</th></tr></thead>
     * <tbody>
     * <tr>
     * <td>gcc (default)</td>
     * <td>GCC C++ compiler</td>
     * </tr>
     * <tr>
     * <td>g++</td>
     * <td>GCC C++ compiler</td>
     * </tr>
     * <tr>
     * <td>c++</td>
     * <td>GCC C++ compiler</td>
     * </tr>
     * <tr>
     * <td>g77</td>
     * <td>GNU Fortran compiler</td>
     * </tr>
     * <tr>
     * <td>msvc</td>
     * <td>Microsoft Visual C++</td>
     * </tr>
     * <tr>
     * <td>bcc</td>
     * <td>Borland C++ Compiler</td>
     * </tr>
     * <tr>
     * <td>msrc</td>
     * <td>Microsoft Resource Compiler</td>
     * </tr>
     * <tr>
     * <td>brc</td>
     * <td>Borland Resource Compiler</td>
     * </tr>
     * <tr>
     * <td>df</td>
     * <td>Compaq Visual Fortran Compiler</td>
     * </tr>
     * <tr>
     * <td>midl</td>
     * <td>Microsoft MIDL Compiler</td>
     * </tr>
     * <tr>
     * <td>icl</td>
     * <td>Intel C++ compiler for Windows (IA-32)</td>
     * </tr>
     * <tr>
     * <td>ecl</td>
     * <td>Intel C++ compiler for Windows (IA-64)</td>
     * </tr>
     * <tr>
     * <td>icc</td>
     * <td>Intel C++ compiler for Linux (IA-32)</td>
     * </tr>
     * <tr>
     * <td>ecc</td>
     * <td>Intel C++ compiler for Linux (IA-64)</td>
     * </tr>
     * <tr>
     * <td>CC</td>
     * <td>Sun ONE C++ compiler</td>
     * </tr>
     * <tr>
     * <td>aCC</td>
     * <td>HP aC++ C++ Compiler</td>
     * </tr>
     * <tr>
     * <td>os390</td>
     * <td>OS390 C Compiler</td>
     * </tr>
     * <tr>
     * <td>os400</td>
     * <td>Icc Compiler</td>
     * </tr>
     * <tr>
     * <td>sunc89</td>
     * <td>Sun C89 C Compiler</td>
     * </tr>
     * <tr>
     * <td>xlC</td>
     * <td>VisualAge C Compiler</td>
     * </tr>
     * <tr>
     * <td>uic</td>
     * <td>Qt user interface compiler</td>
     * </tr>
     * <tr>
     * <td>moc</td>
     * <td>Qt meta-object compiler</td>
     * </tr>
     * <tr>
     * <td>wcl</td>
     * <td>OpenWatcom C/C++ compiler</td>
     * </tr>
     * <tr>
     * <td>wfl</td>
     * <td>OpenWatcom FORTRAN compiler</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param name CompilerEnum
     */
    public void setName(CompilerEnum name) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        setProcessor(name.getCompiler());
    }

    protected void setProcessor(Processor proc) throws BuildException {
        try {
            super.setProcessor(proc);
        } catch (ClassCastException ex) {
            throw new BuildException(ex);
        }
    }

    /**
     * Enumerated attribute with the values "none", "severe", "default",
     * "production", "diagnostic", and "aserror".
     *
     * @param level WarningLevelEnum
     */
    public void setWarnings(WarningLevelEnum level) {
        warnings = level.getIndex();
    }

    /**
     * Sets optimization level.
     *
     * @param value optimization level
     */
    public void setOptimize(OptimizationEnum value) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.optimization = value;
    }

    private CompilerDef getRef() {
        return getCheckedRef(CompilerDef.class, "CompilerDef");
    }
}
