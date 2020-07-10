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


import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Processor;
import net.sf.antcontrib.cpptasks.compiler.ProcessorConfiguration;
import net.sf.antcontrib.cpptasks.types.CommandLineArgument;
import net.sf.antcontrib.cpptasks.types.ConditionalFileSet;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Reference;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Vector;

/**
 * An abstract compiler/linker definition.
 *
 * @author Curt Arnold
 */
public abstract class ProcessorDef extends DataType {
    /**
     * <p>
     * Returns the equivalent Boolean object for the specified value.
     * </p>
     * <p>
     * Equivalent to Boolean.valueOf in JDK 1.4
     * </p>
     *
     * @param val boolean value
     * @return Boolean.TRUE or Boolean.FALSE
     */
    protected static Boolean booleanValueOf(boolean val) {
        if (val) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * if true, targets will be built for debugging
     */
    private Boolean debug;
    private Environment env = null;
    /**
     * Reference for "extends" processor definition
     */
    private Reference extendsRef = null;
    /**
     * Name of property that must be present or definition will be ignored. May
     * be null.
     */
    private String ifProp;
    /**
     * if true, processor definition inherits values from containing cc
     * element
     */
    private boolean inherit;
    private Boolean libtool = null;
    protected boolean newEnvironment = false;
    /**
     * Processor.
     */
    private Processor processor;
    /**
     * Collection of &lt;compilerarg&gt; or &lt;linkerarg&gt; contained by definition
     */
    private final Vector<CommandLineArgument> processorArgs = new Vector<CommandLineArgument>();
    /**
     * Collection of &lt;compilerparam&gt; or &lt;linkerparam&gt; contained by definition
     */
    private final Vector<ProcessorParam> processorParams = new Vector<ProcessorParam>();
    /**
     * if true, all targets will be unconditionally rebuilt
     */
    private Boolean rebuild;
    /**
     * Collection of &lt;fileset&gt; contained by definition
     */
    private final Vector<ConditionalFileSet> srcSets = new Vector<ConditionalFileSet>();
    /**
     * Name of property that if present will cause definition to be ignored.
     * May be null.
     */
    private String unlessProp;

    /**
     * Constructor
     *
     * @throws NullPointerException if something goes wrong
     */
    protected ProcessorDef() throws NullPointerException {
        inherit = true;
    }

    /**
     * Adds a &lt;compilerarg&gt; or &lt;linkerarg&gt;
     *
     * @param arg command line argument, must not be null
     * @throws NullPointerException if arg is null
     * @throws BuildException       if this definition is a reference
     */
    protected void addConfiguredProcessorArg(CommandLineArgument arg)
            throws NullPointerException, BuildException {
        if (arg == null) {
            throw new NullPointerException("arg");
        }
        if (isReference()) {
            throw noChildrenAllowed();
        }
        processorArgs.addElement(arg);
    }

    /**
     * Adds a &lt;compilerarg&gt; or &lt;linkerarg&gt;
     *
     * @param param command line argument, must not be null
     * @throws NullPointerException if arg is null
     * @throws BuildException       if this definition is a reference
     */
    protected void addConfiguredProcessorParam(ProcessorParam param)
            throws NullPointerException, BuildException {
        if (param == null) {
            throw new NullPointerException("param");
        }
        if (isReference()) {
            throw noChildrenAllowed();
        }
        processorParams.addElement(param);
    }

    /**
     * Add an environment variable to the launched process.
     *
     * @param var Environment.Variable
     */
    public void addEnv(Environment.Variable var) {
        if (env == null) {
            env = new Environment();
        }
        env.addVariable(var);
    }

    /**
     * <p>
     * Adds a source file set.
     * </p>
     * <p>
     * Files in these sets will be processed by this configuration and will not
     * participate in the auction.
     * </p>
     *
     * @param srcSet Fileset identifying files that should be processed by this
     *               processor
     * @throws BuildException if processor definition is a reference
     */
    public void addFileset(ConditionalFileSet srcSet) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        srcSet.setProject(getProject());
        srcSets.addElement(srcSet);
    }

    /**
     * Creates a configuration
     *
     * @param task CCTask
     * @param linkType LinkType
     * @param baseDef reference to def from containing cc element, may be null
     * @param targetPlatform TargetDef
     * @param versionInfo VersionInfo
     * @return configuration
     */
    public ProcessorConfiguration createConfiguration(CCTask task, LinkType linkType,
                                                      ProcessorDef baseDef,
                                                      TargetDef targetPlatform,
                                                      VersionInfo versionInfo) {
        if (isReference()) {
            return getRef().createConfiguration(task, linkType,
                    baseDef, targetPlatform, versionInfo);
        }
        return getProcessor(linkType).createConfiguration(task, linkType,
                getDefaultProviders(baseDef), this, targetPlatform, versionInfo);
    }

    /**
     * Prepares list of processor arguments ( compilerarg, linkerarg ) that
     * are active for the current project settings.
     *
     * @return active compiler arguments
     */
    public CommandLineArgument[] getActiveProcessorArgs() {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("project must be set");
        }
        if (isReference()) {
            return getRef().getActiveProcessorArgs();
        }
        Vector<CommandLineArgument> activeArgs = new Vector<CommandLineArgument>();
        for (CommandLineArgument arg : processorArgs) {
            if (arg.isActive(p)) {
                activeArgs.addElement(arg);
            }
        }
        return activeArgs.toArray(new CommandLineArgument[0]);
    }

    /**
     * Prepares list of processor arguments (compilerarg, linkerarg) that
     * are active for the current project settings.
     *
     * @return active compiler arguments
     */
    public ProcessorParam[] getActiveProcessorParams() {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("project must be set");
        }
        if (isReference()) {
            return getRef().getActiveProcessorParams();
        }
        Vector<ProcessorParam> activeParams = new Vector<ProcessorParam>();
        for (ProcessorParam param : processorParams) {
            if (param.isActive(p)) {
                activeParams.addElement(param);
            }
        }
        return activeParams.toArray(new ProcessorParam[0]);
    }

    /**
     * Gets boolean indicating debug build.
     *
     * @param defaultProviders array of ProcessorDef's in descending priority
     * @param index            index to first element in array that should be considered
     * @return if true, built targets for debugging
     */
    public boolean getDebug(ProcessorDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getDebug(defaultProviders, index);
        }
        if (debug != null) {
            return debug;
        } else {
            if (defaultProviders != null && index < defaultProviders.length) {
                return defaultProviders[index].getDebug(defaultProviders,
                        index + 1);
            }
        }
        return false;
    }

    /**
     * Creates an chain of objects which provide default values in descending
     * order of significance.
     *
     * @param baseDef corresponding ProcessorDef from CCTask, will be last element
     *                in array unless inherit = false
     * @return default provider array
     */
    protected final ProcessorDef[] getDefaultProviders(ProcessorDef baseDef) {
        ProcessorDef extendsDef = getExtends();
        Vector<ProcessorDef> chain = new Vector<ProcessorDef>();
        while (extendsDef != null && !chain.contains(extendsDef)) {
            chain.addElement(extendsDef);
            extendsDef = extendsDef.getExtends();
        }
        if (baseDef != null && getInherit()) {
            chain.addElement(baseDef);
        }
        return chain.toArray(new ProcessorDef[0]);
    }

    /**
     * Gets the ProcessorDef specified by the extends attribute.
     *
     * @return Base ProcessorDef, null if extends is not specified
     * @throws BuildException if reference is not same type object
     */
    public ProcessorDef getExtends() throws BuildException {
        if (extendsRef != null) {
            Object obj = extendsRef.getReferencedObject(getProject());
            if (!getClass().isInstance(obj)) {
                throw new BuildException("Referenced object "
                        + extendsRef.getRefId() + " not correct type, is "
                        + obj.getClass().getName() + " should be "
                        + getClass().getName());
            }
            return (ProcessorDef) obj;
        }
        return null;
    }

    /**
     * Gets the inherit attribute. If the inherit value is true, this processor
     * definition will inherit default values from the containing cc element.
     *
     * @return if true then properties from the containing &lt;cc&gt; element are
     * used.
     */
    public final boolean getInherit() {
        return inherit;
    }

    public boolean getLibtool() {
        if (libtool != null) {
            return libtool;
        }
        if (isReference()) {
            return getRef().getLibtool();
        }
        ProcessorDef extendsDef = getExtends();
        if (extendsDef != null) {
            return extendsDef.getLibtool();
        }
        return false;
    }

    /**
     * Obtains the appropriate processor (compiler, linker)
     *
     * @return processor
     */
    protected Processor getProcessor() {
        if (isReference()) {
            return getRef().getProcessor();
        }
        //
        //   if a processor has not been explicitly set
        //      then may be set by an extended definition
        if (processor == null) {
            ProcessorDef extendsDef = getExtends();
            if (extendsDef != null) {
                return extendsDef.getProcessor();
            }
        }
        return processor;
    }

    /**
     * Obtains the appropriate processor (compiler, linker) based on the
     * LinkType.
     *
     * @param linkType LinkType
     * @return processor
     */
    protected Processor getProcessor(LinkType linkType) {
        // by default ignore the linkType.
        return getProcessor();
    }

    /**
     * Gets a boolean value indicating whether all targets must be rebuilt
     * regardless of dependency analysis.
     *
     * @param defaultProviders array of ProcessorDef's in descending priority
     * @param index            index to first element in array that should be considered
     * @return true if all targets should be rebuilt.
     */
    public boolean getRebuild(ProcessorDef[] defaultProviders, int index) {
        if (isReference()) {
            return getRef().getRebuild(defaultProviders, index);
        }
        if (rebuild != null) {
            return rebuild;
        } else {
            if (defaultProviders != null && index < defaultProviders.length) {
                return defaultProviders[index].getRebuild(defaultProviders,
                        index + 1);
            }
        }
        return false;
    }

    /**
     * Returns true if the processor definition contains embedded file set
     * definitions
     *
     * @return true if processor definition contains embedded filesets
     */
    public boolean hasFileSets() {
        if (isReference()) {
            return getRef().hasFileSets();
        }
        return srcSets.size() > 0;
    }

    /**
     * <p>
     * Determine if this def should be used.
     * </p>
     * <p>
     * Definition will be active if the "if" variable (if specified) is set and
     * the "unless" variable (if specified) is not set and that all reference
     * or extended definitions are active.
     * </p>
     *
     * @return true if processor is active
     * @throws IllegalStateException if not properly initialized
     * @throws BuildException        if "if" or "unless" variable contains suspicious values
     *                               "false" or "no" which indicates possible confusion
     */
    public boolean isActive() throws BuildException, IllegalStateException {
        Project project = getProject();
        if (!CUtil.isActive(project, ifProp, unlessProp)
                || isReference() && !getRef().isActive()) {
            return false;
        }
        //
        //  walk through any extended definitions
        //
        ProcessorDef[] defaultProviders = getDefaultProviders(null);
        for (ProcessorDef defaultProvider : defaultProviders) {
            if (!defaultProvider.isActive()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the class name for the adapter. Use the "name" attribute when the
     * tool is supported.
     *
     * @param className full class name
     * @throws BuildException if ProcessorDef cannot be instantiated
     */
    public void setClassname(String className) throws BuildException {
        Processor proc;
        try {
            Class<? extends Processor> implClass
                    = (Class<? extends Processor>) ProcessorDef.class.getClassLoader().loadClass(className);
            try {
                Method getInstance = implClass.getMethod("getInstance");
                proc = (Processor) getInstance.invoke(null);
            } catch (Exception ex) {
                proc = implClass.newInstance();
            }
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
        setProcessor(proc);
    }

    /**
     * If set true, all targets will be built for debugging.
     *
     * @param debug true if targets should be built for debugging
     * @throws BuildException if processor definition is a reference
     */
    public void setDebug(boolean debug) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.debug = booleanValueOf(debug);
    }

    /**
     * Sets a description of the current data type.
     *
     * @param desc String
     */
    public void setDescription(String desc) {
        super.setDescription(desc);
    }

    /**
     * Specifies that this element extends the element with id attribute with a
     * matching value. The configuration will be constructed from the settings
     * of this element, element referenced by extends, and the containing cc
     * element.
     *
     * @param extendsRef Reference to the extended processor definition.
     * @throws BuildException if this processor definition is a reference
     */
    public void setExtends(Reference extendsRef) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.extendsRef = extendsRef;
    }

    /**
     * Sets an id that can be used to reference this element.
     *
     * @param id id
     */
    public void setId(String id) {
        //
        //  this is actually accomplished by a different
        //     mechanism, but we can document it
        //
    }

    /**
     * <p>
     * Sets the property name for the 'if' condition.
     * </p>
     * <p>
     * The configuration will be ignored unless the property is defined.
     * </p>
     * <p>
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") will throw an exception when
     * evaluated.
     * </p>
     *
     * @param propName name of property
     */
    public void setIf(String propName) {
        ifProp = propName;
    }

    /**
     * If inherit has the default value of true, defines, includes and other
     * settings from the containing cc element will be inherited.
     *
     * @param inherit new value
     * @throws BuildException if processor definition is a reference
     */
    public void setInherit(boolean inherit) throws BuildException {
        if (isReference()) {
            throw super.tooManyAttributes();
        }
        this.inherit = inherit;
    }

    /**
     * <p>
     * Set use of libtool.
     * </p>
     * <p>
     * If set to true, the "libtool " will be prepended to the command line
     * </p>
     *
     * @param libtool If true, use libtool.
     */
    public void setLibtool(boolean libtool) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.libtool = booleanValueOf(libtool);
    }

    /**
     * Do not propagate old environment when new environment variables are
     * specified.
     *
     * @param newenv boolean
     */
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * Sets the processor
     *
     * @param processor processor, may not be null.
     * @throws BuildException       if ProcessorDef is a reference
     * @throws NullPointerException if processor is null
     */
    protected void setProcessor(Processor processor) throws BuildException, NullPointerException {
        if (processor == null) {
            throw new NullPointerException("processor");
        }
        if (isReference()) {
            throw super.tooManyAttributes();
        }
        if (env == null && !newEnvironment) {
            this.processor = processor;
        } else {
            this.processor = processor.changeEnvironment(newEnvironment, env);
        }
    }

    /**
     * If set true, all targets will be unconditionally rebuilt.
     *
     * @param rebuild if true, rebuild all targets.
     * @throws BuildException if processor definition is a reference
     */
    public void setRebuild(boolean rebuild) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.rebuild = booleanValueOf(rebuild);
    }

    /**
     * Specifies that this element should behave as if the content of the
     * element with the matching id attribute was inserted at this location. If
     * specified, no other attributes or child content should be specified,
     * other than "if", "unless" and "description".
     *
     * @param ref Reference to other element
     */
    public void setRefid(Reference ref) {
        super.setRefid(ref);
    }

    /**
     * <p>
     * Set the property name for the 'unless' condition.
     * </p>
     * <p>
     * If named property is set, the configuration will be ignored.
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
        unlessProp = propName;
    }

    /**
     * This method calls the FileVisitor's visit function for every file in the
     * processors definition
     *
     * @param visitor object whose visit method is called for every file
     */
    public void visitFiles(FileVisitor visitor) {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("project must be set before this call");
        }
        if (isReference()) {
            getRef().visitFiles(visitor);
        }
        //
        //   if this processor extends another,
        //      visit its files first
        //
        ProcessorDef extendsDef = getExtends();
        if (extendsDef != null) {
            extendsDef.visitFiles(visitor);
        }

        for (int i = 0; i < srcSets.size(); i++) {
            ConditionalFileSet srcSet = srcSets.elementAt(i);
            if (srcSet.isActive()) {
                // Find matching source files
                DirectoryScanner scanner = srcSet.getDirectoryScanner(p);
                // Check each source file - see if it needs compilation
                File parentDir = scanner.getBasedir();
                for (String currentFile : scanner.getIncludedFiles()) {
                    visitor.visit(parentDir, currentFile);
                }
            }
        }
    }

    private ProcessorDef getRef() {
        return getCheckedRef(ProcessorDef.class, "ProcessorDef");
    }
}
