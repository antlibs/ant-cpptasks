/*
 *
 * Copyright 2001-2008 The Ant-Contrib project
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

import net.sf.antcontrib.cpptasks.compiler.CompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.compiler.LinkerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.Processor;
import net.sf.antcontrib.cpptasks.compiler.ProcessorConfiguration;
import net.sf.antcontrib.cpptasks.ide.ProjectDef;
import net.sf.antcontrib.cpptasks.types.CompilerArgument;
import net.sf.antcontrib.cpptasks.types.ConditionalFileSet;
import net.sf.antcontrib.cpptasks.types.DefineSet;
import net.sf.antcontrib.cpptasks.types.IncludePath;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LinkerArgument;
import net.sf.antcontrib.cpptasks.types.SystemIncludePath;
import net.sf.antcontrib.cpptasks.types.SystemLibrarySet;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * <p>
 * Compile and link task.
 * </p>
 *
 * <p>
 * This task can compile various source languages and produce executables,
 * shared libraries (aka DLL's) and static libraries. Compiler adaptors are
 * currently available for several C/C++ compilers, FORTRAN, MIDL and Windows
 * Resource files.
 * </p>
 *
 * <p>
 * Copyright (c) 2001-2008, The Ant-Contrib project.
 * </p>
 *
 * <p>
 * Licensed under the Apache Software License 2.0,
 * http://www.apache.org/licenses/LICENSE-2.0.
 * </p>
 *
 * <p>
 * For use with Apache Ant 1.5 or later. This software is not a product of the
 * of the Apache Software Foundation and no endorsement is implied.
 * </p>
 *
 * <p>
 * THIS SOFTWARE IS PROVIDED 'AS-IS', See
 * http://www.apache.org/licenses/LICENSE-2.0 for additional disclaimers.
 * </p>
 * <p>
 * To use:
 * </p>
 * <ol>
 * <li>
 * Place cpptasks.jar into Ant's classpath by placing it in Ant's lib
 * directory, adding it to the CLASSPATH environment variable or by using the
 * -lib command line option.
 * </li>
 * <li>
 * Add type and task definitions to the build file:
 * <ul>
 * <li>
 * Ant 1.6 or later:
 * <ul>
 * <li>Add xmlns:cpptasks="antlib:net.sf.antcontrib.cpptasks" to
 * &lt;project&gt; element.
 * </li>
 * <li>
 * Add &lt;cpptasks:cc/&gt;, &lt;cpptasks:compiler/&gt; and
 * &lt;cpptasks:linker/&gt; elements to the project.
 * </li>
 * </ul>
 * </li>
 * <li>
 * Ant 1.5 or later:
 * <ul>
 * <li>Add &lt;taskdef resource="cpptasks.tasks"/&gt; and
 * &lt;typedef resource="cpptasks.types"/&gt; to body of &lt;project&gt;
 * element.
 * </li>
 * <li>
 * Add &lt;cc/&gt;, &lt;compiler/&gt; and &lt;linker/&gt; elements to the
 * project.
 * </li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li>
 * Set the path and environment variables to be able to run compiler from
 * command line.
 * </li>
 * <li>
 * Build the project.
 * </li>
 * </ol>
 *
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public class CCTask extends Task {
    private static class SystemLibraryCollector implements FileVisitor {
        private final Hashtable<String, File> libraries;
        private final Linker linker;

        public SystemLibraryCollector(Linker linker, Hashtable<String, File> libraries) {
            this.linker = linker;
            this.libraries = libraries;
        }

        public void visit(File basedir, String filename) {
            if (linker.bid(filename) > 0) {
                File libfile = new File(basedir, filename);
                String key = linker.getLibraryKey(libfile);
                libraries.put(key, libfile);
            }
        }
    }

    private static class ProjectFileCollector implements FileVisitor {
        private final List<File> files;

        /**
         * Creates a new ProjectFileCollector.
         *
         * @param files vector for collected files.
         */
        public ProjectFileCollector(List<File> files) {
            this.files = files;
        }

        /**
         * Called for each file to be considered for collection.
         *
         * @param parentDir parent directory
         * @param filename  filename within directory
         */
        public void visit(File parentDir, String filename) {
            files.add(new File(parentDir, filename));
        }
    }

    private static final ProcessorConfiguration[] EMPTY_CONFIG_ARRAY = new ProcessorConfiguration[0];

    /**
     * Builds a Hashtable to targets needing to be rebuilt keyed by compiler
     * configuration
     *
     * @param targets Hashtable of String, TargetInfo
     * @return Hashtable of ProcessorConfiguration, Vector of TargetInfo
     */
    public static Hashtable<ProcessorConfiguration, Vector<TargetInfo>> getTargetsToBuildByConfiguration(Hashtable<String, TargetInfo> targets) {
        Hashtable<ProcessorConfiguration, Vector<TargetInfo>> targetsByConfig = new Hashtable<ProcessorConfiguration, Vector<TargetInfo>>();
        for (Map.Entry<String, TargetInfo> entry : targets.entrySet()) {
            TargetInfo target = entry.getValue();
            if (target.getRebuild()) {
                Vector<TargetInfo> targetsForSameConfig = targetsByConfig.get(target.getConfiguration());
                if (targetsForSameConfig != null) {
                    targetsForSameConfig.addElement(target);
                } else {
                    targetsForSameConfig = new Vector<TargetInfo>();
                    targetsForSameConfig.addElement(target);
                    targetsByConfig.put(target.getConfiguration(), targetsForSameConfig);
                }
            }
        }
        return targetsByConfig;
    }

    /**
     * The compiler definitions.
     */
    private final Vector<CompilerDef> mCompilers = new Vector<CompilerDef>();
    /**
     *
     * The library sets.
     */
    private final Vector<LibrarySet> mLibsets = new Vector<LibrarySet>();
    /**
     * The linker definitions.
     */
    private final Vector<LinkerDef> mLinkers = new Vector<LinkerDef>();
    /**
     * The object directory.
     */
    private File mObjdir;
    /**
     * The output file.
     */
    private File mOutfile;
    /**
     * The linker definitions.
     */
    private final Vector<TargetDef> targetPlatforms = new Vector<TargetDef>();
    /**
     * The distributer definitions.
     */
    private final Vector<DistributerDef> distributers = new Vector<DistributerDef>();
    private final Vector<VersionInfo> versionInfos = new Vector<VersionInfo>();
    private final Vector<ProjectDef> projects = new Vector<ProjectDef>();
    private boolean projectsOnly = false;


    /**
     * If true, stop build on compile failure.
     */
    protected boolean failOnError = true;

    /**
     * Content that appears in &lt;cc&gt; as well as in &lt;compiler&gt; is maintained by a
     * captive CompilerDef instance
     */
    private final CompilerDef compilerDef = new CompilerDef();
    /**
     * The OS390 dataset to build to object to
     */
    private String dataset;
    /**
     * <p>
     * Depth of dependency checking
     * </p>
     * <p>
     * Values &lt; 0 indicate full dependency checking Values &gt;= 0 indicate
     * partial dependency checking and for superficial compilation checks. Will
     * throw BuildException before attempting link
     * </p>
     */
    private int dependencyDepth = -1;
    /**
     * Content that appears in &lt;cc&gt; as well as in &lt;linker&gt; is maintained by a
     * captive CompilerDef instance
     */
    private final LinkerDef linkerDef = new LinkerDef();
    /**
     * contains the subsystem, output type and
     */
    private final LinkType linkType = new LinkType();
    /**
     * The property name which will be set with the physical filename of the
     * file that is generated by the linker
     */
    private String outputFileProperty;
    /**
     * if relentless = true, compilations should attempt to compile as many
     * files as possible before throwing a BuildException
     */
    private boolean relentless;

    public CCTask() {
    }

    /**
     * Adds a compiler definition or reference.
     *
     * @param compiler compiler
     * @throws NullPointerException if compiler is null
     */
    public void addConfiguredCompiler(CompilerDef compiler) {
        if (compiler == null) {
            throw new NullPointerException("compiler");
        }
        compiler.setProject(getProject());
        mCompilers.addElement(compiler);
    }

    /**
     * Adds a compiler command-line arg. Argument will be inherited by all
     * nested compiler elements that do not have inherit="false".
     *
     * @param arg CompilerArgument
     */
    public void addConfiguredCompilerArg(CompilerArgument arg) {
        compilerDef.addConfiguredCompilerArg(arg);
    }

    /**
     * Adds a defineset. Will be inherited by all compiler elements that do not
     * have inherit="false".
     *
     * @param defs Define set
     */
    public void addConfiguredDefineset(DefineSet defs) {
        compilerDef.addConfiguredDefineset(defs);
    }

    /**
     * Adds a linker definition. The first linker that is not disqualified by
     * its "if" and "unless" attributes will perform the link. If no child
     * linker element is active, the linker implied by the name of cc element or
     * classname attribute will be used.
     *
     * @param linker linker
     * @throws NullPointerException if linker is null
     */
    public void addConfiguredLinker(LinkerDef linker) {
        if (linker == null) {
            throw new NullPointerException("linker");
        }
        linker.setProject(getProject());
        mLinkers.addElement(linker);
    }

    /**
     * Adds a linker command-line arg. Argument will be inherited by all nested
     * linker elements that do not have inherit="false".
     *
     * @param arg LinkerArgument
     */
    public void addConfiguredLinkerArg(LinkerArgument arg) {
        linkerDef.addConfiguredLinkerArg(arg);
    }

    /**
     * Adds an environment variable to the launched process.
     *
     * @param var Environment.Variable
     */
    public void addEnv(Environment.Variable var) {
        compilerDef.addEnv(var);
        linkerDef.addEnv(var);
    }

    /**
     * <p>
     * Adds a source file set.
     * </p>
     * <p>
     * Files in these filesets will be auctioned to the available compiler
     * configurations, with the default compiler implied by the cc element
     * bidding last. If no compiler is interested in the file, it will be
     * passed to the linker.
     * </p>
     * <p>
     * To have a file be processed by a particular compiler configuration, add
     * a fileset to the corresponding compiler element.
     * </p>
     *
     * @param srcSet ConditionalFileSet
     */
    public void addFileset(ConditionalFileSet srcSet) {
        compilerDef.addFileset(srcSet);
    }

    /**
     * <p>
     * Adds a library set.
     * </p>
     * <p>
     * Library sets will be inherited by all linker elements that do not have
     * inherit="false".
     * </p>
     *
     * @param libset library set
     * @throws NullPointerException if libset is null.
     */
    public void addLibset(LibrarySet libset) {
        if (libset == null) {
            throw new NullPointerException("libset");
        }
        linkerDef.addLibset(libset);
    }

    /**
     * <p>
     * Adds a system library set. Timestamps and locations of system library
     * sets are not used in dependency analysis.
     * </p>
     * <p>
     * Essential libraries (such as C Runtime libraries) should not be
     * specified since the task will attempt to identify the correct libraries
     * based on the multithread, debug and runtime attributes.
     * </p>
     * <p>
     * System library sets will be inherited by all linker elements that do not
     * have inherit="false".
     * </p>
     *
     * @param libset library set
     * @throws NullPointerException if libset is null.
     */
    public void addSyslibset(SystemLibrarySet libset) {
        if (libset == null) {
            throw new NullPointerException("libset");
        }
        linkerDef.addSyslibset(libset);
    }

    /**
     * Specifies the generation of IDE project file.  Experimental.
     *
     * @param projectDef project file generation specification
     */
    public void addProject(final ProjectDef projectDef) {
        if (projectDef == null) {
            throw new NullPointerException("projectDef");
        }
        projects.addElement(projectDef);
    }

    public void setProjectsOnly(final boolean value) {
        projectsOnly = value;
    }

    /**
     * Checks all targets that are not forced to be rebuilt or are missing
     * object files to be checked for modified include files
     *
     * @param targets Hashtable of String, TargetInfo
     * @return total number of targets to be rebuilt
     */
    protected int checkForChangedIncludeFiles(Hashtable<String, TargetInfo> targets) {
        int potentialTargets = 0;
        int definiteTargets = 0;
        for (Map.Entry<String, TargetInfo> entry : targets.entrySet()) {
            if (!entry.getValue().getRebuild()) {
                potentialTargets++;
            } else {
                definiteTargets++;
            }
        }
        //
        //     If there were remaining targets that
        //        might be out of date
        //
        if (potentialTargets > 0) {
            log("Starting dependency analysis for " + potentialTargets + " files.");
            DependencyTable dependencyTable = new DependencyTable(mObjdir);
            try {
                dependencyTable.load();
            } catch (Exception ex) {
                log("Problem reading dependencies.xml: " + ex.toString());
            }
            for (Map.Entry<String, TargetInfo> entry : targets.entrySet()) {
                TargetInfo target = entry.getValue();
                if (!target.getRebuild()
                        && dependencyTable.needsRebuild(this, target, dependencyDepth)) {
                    target.mustRebuild();
                }
            }
            dependencyTable.commit(this);
        }
        //
        //   count files being rebuilt now
        //
        int currentTargets = 0;
        for (Map.Entry<String, TargetInfo> entry : targets.entrySet()) {
            if (entry.getValue().getRebuild()) {
                currentTargets++;
            }
        }
        if (potentialTargets > 0) {
            log((potentialTargets - currentTargets + definiteTargets) + " files are up to date.");
            log((currentTargets - definiteTargets)
                    + " files to be recompiled from dependency analysis.");
        }
        log(currentTargets + " total files to be compiled.");
        return currentTargets;
    }

    protected LinkerConfiguration collectExplicitObjectFiles(Vector<File> objectFiles,
                                                             Vector<File> sysObjectFiles,
                                                             VersionInfo versionInfo) {
        //
        //    find the first eligible linker
        //
        //
        ProcessorConfiguration linkerConfig = null;
        LinkerDef selectedLinkerDef = null;
        Linker selectedLinker = null;
        Hashtable<String, File> sysLibraries = new Hashtable<String, File>();
        TargetDef targetPlatform = getTargetPlatform();
        FileVisitor objCollector = null;
        FileVisitor sysLibraryCollector = null;
        for (LinkerDef currentLinkerDef : mLinkers) {
             if (currentLinkerDef.isActive()) {
                selectedLinkerDef = currentLinkerDef;
                selectedLinker = currentLinkerDef.getProcessor().getLinker(linkType);
                //
                //   skip the linker if it doesn't know how to
                //      produce the specified link type
                if (selectedLinker != null) {
                    linkerConfig = currentLinkerDef.createConfiguration(this,
                            linkType, linkerDef, targetPlatform, versionInfo);
                    if (linkerConfig != null) {
                        //
                        //   create collectors for object files
                        //      and system libraries
                        objCollector = new ObjectFileCollector(selectedLinker, objectFiles);
                        sysLibraryCollector = new SystemLibraryCollector(selectedLinker,
                                sysLibraries);
                        //
                        //    if the <linker> has embedded <fileset>'s
                        //       (such as linker specific libraries)
                        //       add them as object files.
                        //
                        if (currentLinkerDef.hasFileSets()) {
                            currentLinkerDef.visitFiles(objCollector);
                        }
                        //
                        //    user libraries are just a specialized form
                        //       of an object fileset
                        selectedLinkerDef.visitUserLibraries(selectedLinker, objCollector);
                    }
                    break;
                }
            }
        }
        if (linkerConfig == null) {
            linkerConfig = linkerDef.createConfiguration(this, linkType, null, targetPlatform,
                    versionInfo);
            selectedLinker = linkerDef.getProcessor().getLinker(linkType);
            objCollector = new ObjectFileCollector(selectedLinker, objectFiles);
            sysLibraryCollector = new SystemLibraryCollector(selectedLinker, sysLibraries);
        }
        //
        //  unless there was a <linker> element that
        //     explicitly did not inherit files from
        //        containing <cc> element
        if (selectedLinkerDef == null || selectedLinkerDef.getInherit()) {
            linkerDef.visitUserLibraries(selectedLinker, objCollector);
            linkerDef.visitSystemLibraries(selectedLinker, sysLibraryCollector);
        }
        //
        //   if there was a <syslibset> in a nested <linker>
        //       evaluate it last so it takes priority over
        //       identically named libs from <cc> element
        //
        if (selectedLinkerDef != null) {
            //
            //    add any system libraries to the hashtable
            //       done in reverse order so the earliest
            //        on the classpath takes priority
            selectedLinkerDef.visitSystemLibraries(selectedLinker, sysLibraryCollector);
        }
        //
        //   copy over any system libraries to the
        //      object files vector
        //
        for (Map.Entry<String, File> entry : sysLibraries.entrySet()) {
            sysObjectFiles.addElement(entry.getValue());
        }
        return (LinkerConfiguration) linkerConfig;
    }

    /**
     * <p>
     * Adds an include path.
     * </p>
     * <p>
     * Include paths will be inherited by nested compiler elements that do not
     * have inherit="false".
     * </p>
     *
     * @return IncludePath
     */
    public IncludePath createIncludePath() {
        return compilerDef.createIncludePath();
    }

    /**
     * Specifies precompilation prototype file and exclusions. Inherited by all
     * compilers that do not have inherit="false".
     *
     * @return PrecompiledDef
     */
    public PrecompileDef createPrecompile() throws BuildException {
        return compilerDef.createPrecompile();
    }

    /**
     * <p>
     * Adds a system include path. Locations and timestamps of files located
     * using the system include paths are not used in dependency analysis.
     * </p>
     * <p>
     * Standard include locations should not be specified. The compiler
     * adapters should recognized the settings from the appropriate environment
     * variables or configuration files.
     * </p>
     * <p>
     * System include paths will be inherited by nested compiler elements that
     * do not have inherit="false".
     * </p>
     *
     * @return SystemIncludePath
     */
    public SystemIncludePath createSysIncludePath() {
        return compilerDef.createSysIncludePath();
    }

    /**
     * Executes the task. Compiles the given files.
     *
     * @throws BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {
        //
        //   if link type allowed objdir to be defaulted
        //      provide it from outfile
        if (mObjdir == null) {
            if (mOutfile != null) {
                mObjdir = new File(mOutfile.getParent());
            } else {
                mObjdir = new File(".");
            }
        }

        //
        //   if the object directory does not exist
        //
        if (!mObjdir.exists()) {
            throw new BuildException("Object directory does not exist");
        }
        TargetHistoryTable objHistory = new TargetHistoryTable(this, mObjdir);

        //
        //   get the first active version info
        //
        VersionInfo versionInfo = null;
        for (VersionInfo vInfo : versionInfos) {
            if (vInfo.merge().isActive()) {
                versionInfo = vInfo;
                break;
            }
        }
        //
        //  determine the eventual linker configuration
        //      (may be null) and collect any explicit
        //          object files or libraries
        Vector<File> objectFiles = new Vector<File>();
        Vector<File> sysObjectFiles = new Vector<File>();
        LinkerConfiguration linkerConfig = collectExplicitObjectFiles(objectFiles, sysObjectFiles,
                versionInfo);

        //
        //   Assemble hashtable of all files
        //       that we know how to compile (keyed by output file name)
        //
        Hashtable<String, TargetInfo> targets = getTargets(linkerConfig, objectFiles, versionInfo,
                mOutfile);
        TargetInfo linkTarget = null;
        //
        //   if output file is not specified,
        //      then skip link step
        //
        if (mOutfile != null) {
            linkTarget = getLinkTarget(linkerConfig, objectFiles, sysObjectFiles, targets,
                    versionInfo);
        }

        if (projects.size() > 0) {
            ArrayList<File> files = new ArrayList<File>();
            ProjectFileCollector matcher = new ProjectFileCollector(files);
            for (CompilerDef currentCompilerDef : mCompilers) {
                 if (currentCompilerDef.isActive() && currentCompilerDef.hasFileSets()) {
                    currentCompilerDef.visitFiles(matcher);
                }
            }
            compilerDef.visitFiles(matcher);

            for (ProjectDef projectDef : projects) {
                if (projectDef.isActive()) {
                    projectDef.execute(this, files, targets, linkTarget);
                }
            }
        }
        if (projectsOnly) {
            return;
        }
        //
        //     mark targets that don't have a history record or
        //        whose source last modification time is not
        //        the same as the history to be rebuilt
        //
        objHistory.markForRebuild(targets);
        CCTaskProgressMonitor monitor = new CCTaskProgressMonitor(objHistory, versionInfo);
        //
        //      check for changed include files
        //
        int rebuildCount = checkForChangedIncludeFiles(targets);
        if (rebuildCount > 0) {
            BuildException compileException = null;
            //
            //    compile all targets with getRebuild() == true
            //
            Hashtable<ProcessorConfiguration, Vector<TargetInfo>> targetsByConfig
                    = getTargetsToBuildByConfiguration(targets);
            //
            //    build array containing Vectors with precompiled generation
            //       steps going first
            //
            List<Vector<TargetInfo>> targetVectorsList = new ArrayList<Vector<TargetInfo>>();
            for (Map.Entry<ProcessorConfiguration, Vector<TargetInfo>> entry : targetsByConfig.entrySet()) {
                Vector<TargetInfo> targetsForConfig = entry.getValue();
                //
                //    get the configuration from the first entry
                //
                CompilerConfiguration config = (CompilerConfiguration)
                        (targetsForConfig.elementAt(0)).getConfiguration();
                if (config.isPrecompileGeneration()) {
                    targetVectorsList.add(targetsForConfig);
                }
            }
            for (Map.Entry<ProcessorConfiguration, Vector<TargetInfo>> entry : targetsByConfig.entrySet()) {
                Vector<TargetInfo> targetsForConfig = entry.getValue();
                boolean found = false;
                for (Vector<TargetInfo> targetVectors : targetVectorsList) {
                    if (targetVectors == targetsForConfig) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    targetVectorsList.add(targetsForConfig);
                }
            }
            for (Vector<TargetInfo> targetsForConfig : targetVectorsList) {
                //
                //    get the targets for this configuration
                //    get the configuration from the first entry
                //
                CompilerConfiguration config = (CompilerConfiguration)
                        (targetsForConfig.elementAt(0)).getConfiguration();
                //
                //    prepare the list of source files
                //
                String[] sourceFiles = new String[targetsForConfig.size()];
                int index = 0;
                for (TargetInfo targetInfo : targetsForConfig) {
                    sourceFiles[index++] = targetInfo.getSources()[0].toString();
                }
                try {
                    config.compile(this, mObjdir, sourceFiles, relentless, monitor);
                } catch (BuildException ex) {
                    if (compileException == null) {
                        compileException = ex;
                    }
                    if (!relentless) {
                        break;
                    }
                }
            }
            //
            //   save the details of the object file compilation
            //     settings to disk for dependency analysis
            //
            try {
                objHistory.commit();
            } catch (IOException ex) {
                this.log("Error writing history.xml: " + ex.toString());
            }
            //
            //  if we threw a compile exception and
            //     didn't throw it at the time because
            //     we were relentless then
            //        save the history and
            //           throw the exception
            //
            if (compileException != null) {
                if (failOnError) {
                    throw compileException;
                } else {
                    log(compileException.getMessage(), Project.MSG_ERR);
                    return;
                }
            }
        }
        //
        //    if the dependency tree was not fully
        //      evaluated, then throw an exception
        //      since we really didn't do what we
        //      should have done
        //
        if (dependencyDepth >= 0) {
            throw new BuildException("All files at depth " + dependencyDepth
                    + " from changes successfully compiled.\n"
                    + "Remove or change dependencyDepth to -1 to perform full compilation.");
        }
        //
        //   if no link target then
        //       commit the history for the object files
        //           and leave the task
        if (linkTarget != null) {
            //
            //    get the history for the link target (may be the same
            //        as the object history)
            TargetHistoryTable linkHistory = getLinkHistory(objHistory);
            //
            //    see if it needs to be rebuilt
            //
            linkHistory.markForRebuild(linkTarget);
            //
            //    if it needs to be rebuilt, rebuild it
            //
            File output = linkTarget.getOutput();
            if (linkTarget.getRebuild()) {
                log("Starting link");
                LinkerConfiguration linkConfig =
                        (LinkerConfiguration) linkTarget.getConfiguration();
                if (failOnError) {
                    linkConfig.link(this, linkTarget);
                } else {
                    try {
                        linkConfig.link(this, linkTarget);
                    } catch (BuildException ex) {
                        log(ex.getMessage(), Project.MSG_ERR);
                        return;
                    }
                }
                if (outputFileProperty != null) {
                    getProject().setProperty(outputFileProperty, output.getAbsolutePath());
                }
                linkHistory.update(linkTarget);
                try {
                    linkHistory.commit();
                } catch (IOException ex) {
                    log("Error writing link history.xml: " + ex.toString());
                }
            } else if (outputFileProperty != null) {
                getProject().setProperty(outputFileProperty, output.getAbsolutePath());
            }
        }
    }

    /**
     * Gets the dataset.
     *
     * @return Returns a String
     */
    public String getDataset() {
        return dataset;
    }

    protected TargetHistoryTable getLinkHistory(TargetHistoryTable objHistory) {
        File outputFileDir = new File(mOutfile.getParent());
        //
        //   if the output file is being produced in the link
        //        directory, then we can use the same history file
        //
        if (mObjdir.equals(outputFileDir)) {
            return objHistory;
        }
        return new TargetHistoryTable(this, outputFileDir);
    }

    protected TargetInfo getLinkTarget(LinkerConfiguration linkerConfig,
                                       Vector<File> objectFiles, Vector<File> sysObjectFiles,
                                       Hashtable<String, TargetInfo> compileTargets,
                                       VersionInfo versionInfo) {
        //
        //  walk the compile phase targets and
        //     add those sources that have already been
        //     assigned to the linker or
        //     our output files the linker knows how to consume
        //     files the linker knows how to consume
        //
        for (Map.Entry<String, TargetInfo> compileTargetEntry : compileTargets.entrySet()) {
            File file = compileTargetEntry.getValue().getOutput();
            //
            //   output of compile tasks
            //
            int bid = linkerConfig.bid(file.toString());
            if (bid > 0) {
                objectFiles.addElement(file);
            }
        }
        String[] fullNames = linkerConfig.getOutputFileNames(mOutfile.getName(), versionInfo);
        return new TargetInfo(linkerConfig, objectFiles.toArray(new File[0]),
                sysObjectFiles.toArray(new File[0]), new File(mOutfile.getParent(), fullNames[0]),
                linkerConfig.getRebuild());
    }

    public File getObjdir() {
        return mObjdir;
    }

    public File getOutfile() {
        return mOutfile;
    }

    public TargetDef getTargetPlatform() {
        return null;
    }

    /**
     * This method collects a Hashtable, keyed by output file name, of
     * TargetInfo's for every source file that is specified in the filesets of
     * the &lt;cc&gt; and nested &lt;compiler&gt; elements. The TargetInfo's contain the
     * appropriate compiler configurations for their possible compilation
     */
    private Hashtable<String, TargetInfo> getTargets(LinkerConfiguration linkerConfig,
                                                     Vector<File> objectFiles,
                                                     VersionInfo versionInfo, File outputFile) {
        Hashtable<String, TargetInfo> targets = new Hashtable<String, TargetInfo>(1000);
        TargetDef targetPlatform = getTargetPlatform();
        //
        //   find active (specialized) compilers
        //
        Vector<ProcessorConfiguration> biddingProcessors = new Vector<ProcessorConfiguration>();
        for (CompilerDef currentCompilerDef : mCompilers) {
            if (currentCompilerDef.isActive()) {
                ProcessorConfiguration config = currentCompilerDef.createConfiguration(this,
                        linkType, compilerDef, targetPlatform, versionInfo);
                //
                //   see if this processor had a precompile child element
                //
                PrecompileDef precompileDef = currentCompilerDef.getActivePrecompile(compilerDef);
                ProcessorConfiguration[] localConfigs = new ProcessorConfiguration[]{config};
                //
                //    if it does then
                //
                if (precompileDef != null) {
                    File prototype = precompileDef.getPrototype();
                    //
                    //  will throw exceptions if prototype doesn't exist, etc
                    //
                    if (!prototype.exists()) {
                        throw new BuildException("prototype (" + prototype.toString()
                                + ") does not exist.");
                    }
                    if (prototype.isDirectory()) {
                        throw new BuildException("prototype (" + prototype.toString()
                                + ") is a directory.");
                    }
                    String[] exceptFiles = precompileDef.getExceptFiles();
                    //
                    //  create a precompile building and precompile using
                    //      variants of the configuration
                    //      or return null if compiler doesn't support
                    //      precompilation
                    CompilerConfiguration[] configs = ((CompilerConfiguration) config)
                            .createPrecompileConfigurations(prototype, exceptFiles);
                    if (configs != null && configs.length == 2) {
                        //
                        //   visit the precompiled file to add it into the
                        //      targets list (just like any other file if
                        //      compiler doesn't support precompilation)
                        TargetMatcher matcher = new TargetMatcher(this, mObjdir,
                                new ProcessorConfiguration[]{configs[0]},
                                linkerConfig, objectFiles, targets, versionInfo);

                        matcher.visit(new File(prototype.getParent()),
                                prototype.getName());
                        //
                        //   only the configuration that uses the
                        //      precompiled header gets added to the bidding list
                        biddingProcessors.addElement(configs[1]);
                        localConfigs = new ProcessorConfiguration[2];
                        localConfigs[0] = configs[1];
                        localConfigs[1] = config;
                    }
                }
                //
                //   if the compiler has a fileset
                //       then allow it to add its files
                //       to the set of potential targets
                if (currentCompilerDef.hasFileSets()) {
                    TargetMatcher matcher = new TargetMatcher(this, mObjdir, localConfigs,
                            linkerConfig, objectFiles, targets, versionInfo);
                    currentCompilerDef.visitFiles(matcher);
                }
                biddingProcessors.addElement(config);
            }
        }
        //
        //    add fallback compiler at the end
        //
        ProcessorConfiguration config = compilerDef.createConfiguration(this,
                linkType, null, targetPlatform, versionInfo);
        biddingProcessors.addElement(config);
        ProcessorConfiguration[] bidders = biddingProcessors.toArray(new ProcessorConfiguration[0]);
        //
        //   bid out the <fileset>'s in the cctask
        //
        TargetMatcher matcher = new TargetMatcher(this, mObjdir, bidders, linkerConfig,
                objectFiles, targets, versionInfo);
        compilerDef.visitFiles(matcher);

        if (outputFile != null && versionInfo != null) {
            boolean isDebug = linkerConfig.isDebug();
            try {
                linkerConfig.getLinker().addVersionFiles(versionInfo, linkType, outputFile,
                        isDebug, mObjdir, matcher);
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
        }
        return targets;
    }

    /**
     * Sets the default compiler adapter. Use the "name" attribute when the
     * compiler is a supported compiler.
     *
     * @param classname fully qualified classname which implements CompilerAdapter
     */
    public void setClassname(String classname) {
        compilerDef.setClassname(classname);
        linkerDef.setClassname(classname);
    }

    /**
     * Sets the dataset for OS/390 builds.
     *
     * @param dataset The dataset to set
     */
    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    /**
     * Enables or disables generation of debug info.
     *
     * @param debug boolean
     */
    public void setDebug(boolean debug) {
        compilerDef.setDebug(debug);
        linkerDef.setDebug(debug);
    }

    /**
     * Gets debug state.
     *
     * @return true if building for debugging
     */
    public boolean getDebug() {
        return compilerDef.getDebug(null, 0);
    }

    /**
     * Deprecated.
     * <p>
     * Controls the depth of the dependency evaluation. Used to do a quick
     * check of changes before a full build.
     * </p>
     * <p>
     * Any negative value which will perform full dependency checking. Positive
     * values will truncate dependency checking. A value of 0 will cause only
     * those files that changed to be recompiled, a value of 1 which cause
     * files that changed or that explicitly include a file that changed to be
     * recompiled.
     * </p>
     * <p>
     * Any non-negative value will cause a BuildException to be thrown before
     * attempting a link or completing the task.
     * </p>
     *
     * @param depth int
     */
    public void setDependencyDepth(int depth) {
        dependencyDepth = depth;
    }

    /**
     * Enables generation of exception handling code
     *
     * @param exceptions boolean
     */
    public void setExceptions(boolean exceptions) {
        compilerDef.setExceptions(exceptions);
    }

    /**
     * Enables run-time type information.
     *
     * @param rtti boolean
     */
    public void setRtti(boolean rtti) {
        compilerDef.setRtti(rtti);
    }
    //    public LinkType getLinkType() {
    //      return linkType;
    //    }

    /**
     * Enables or disables incremental linking.
     *
     * @param incremental new state
     */
    public void setIncremental(boolean incremental) {
        linkerDef.setIncremental(incremental);
    }

    /**
     * <p>
     * Set use of libtool.
     * </p>
     * <p>
     * If set to true, the "libtool " will be prepended to the command line for
     * compatible processors
     * </p>
     *
     * @param libtool If true, use libtool.
     */
    public void setLibtool(boolean libtool) {
        compilerDef.setLibtool(libtool);
        linkerDef.setLibtool(libtool);
    }

    /**
     * Sets the output file type. Supported values "executable", "shared", and
     * "static".  Deprecated, specify outtype instead.
     *
     * @param outputType OutputTypeEnum
     * @deprecated specify outtype instead.
     */
    public void setLink(OutputTypeEnum outputType) {
        linkType.setOutputType(outputType);
    }

    /**
     * Enables or disables generation of multithreaded code
     *
     * @param multi If true, generated code may be multithreaded.
     */
    public void setMultithreaded(boolean multi) {
        compilerDef.setMultithreaded(multi);
    }
    //
    //  keep near duplicate comment at CompilerDef.setName in sync
    //

    /**
     * <p>
     * Sets type of the default compiler and linker.
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
     * <td>GNU FORTRAN compiler</td>
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
     * <td>Qt user interface compiler (creates .h, .cpp and moc_*.cpp files).</td>
     * </tr>
     * <tr>
     * <td>moc</td>
     * <td>Qt meta-object compiler</td>
     * </tr>
     * <tr>
     * <td>xpidl</td>
     * <td>Mozilla xpidl compiler (creates .h and .xpt files).</td>
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
    public void setName(CompilerEnum name) {
        compilerDef.setName(name);
        Processor compiler = compilerDef.getProcessor();
        Linker linker = compiler.getLinker(linkType);
        linkerDef.setProcessor(linker);
    }

    /**
     * Do not propagate old environment when new environment variables are
     * specified.
     *
     * @param newenv boolean
     */
    public void setNewenvironment(boolean newenv) {
        compilerDef.setNewenvironment(newenv);
        linkerDef.setNewenvironment(newenv);
    }

    /**
     * <p>
     * Sets the destination directory for object files.
     * </p>
     * <p>
     * Generally this should be a property expression that evaluates to
     * distinct debug and release object file directories.
     * </p>
     *
     * @param dir object directory
     */
    public void setObjdir(File dir) {
        if (dir == null) {
            throw new NullPointerException("dir");
        }
        mObjdir = dir;
    }

    /**
     * Sets the output file name. If not specified, the task will only compile
     * files and not attempt to link. If an extension is not specified, the
     * task may use a system appropriate extension and prefix, for example,
     * outfile="example" may result in "libexample.so" being created.
     *
     * @param outfile output file name
     */
    public void setOutfile(File outfile) {
        //
        //   if file name was empty, skip link step
        //
        if (outfile == null || outfile.toString().length() > 0) {
            mOutfile = outfile;
        }
    }

    /**
     * Specifies the name of a property to set with the physical filename that
     * is produced by the linker
     *
     * @param outputFileProperty String
     */
    public void setOutputFileProperty(String outputFileProperty) {
        this.outputFileProperty = outputFileProperty;
    }

    /**
     * Sets the output file type. Supported values "executable", "shared", and
     * "static".
     *
     * @param outputType OutputTypeEnum
     */
    public void setOuttype(OutputTypeEnum outputType) {
        linkType.setOutputType(outputType);
    }

    /**
     * Gets output type.
     *
     * @return output type
     */
    public String getOuttype() {
        return linkType.getOutputType();
    }

    /**
     * Sets the project.
     *
     * @param project Project
     */
    public void setProject(Project project) {
        super.setProject(project);
        compilerDef.setProject(project);
        linkerDef.setProject(project);
    }

    /**
     * If set to true, all files will be rebuilt.
     *
     * @param rebuildAll If true, all files will be rebuilt. If false, up to
     *                   date files will not be rebuilt.
     */
    public void setRebuild(boolean rebuildAll) {
        compilerDef.setRebuild(rebuildAll);
        linkerDef.setRebuild(rebuildAll);
    }

    /**
     * If set to true, compilation errors will not stop the task until all
     * files have been attempted.
     *
     * @param relentless If true, don't stop on the first compilation error
     */
    public void setRelentless(boolean relentless) {
        this.relentless = relentless;
    }

    /**
     * Sets the type of runtime library, possible values "dynamic", "static".
     *
     * @param rtlType RuntimeType
     */
    public void setRuntime(RuntimeType rtlType) {
        linkType.setStaticRuntime((rtlType.getIndex() == 1));
    }

    /**
     * <p>
     * Sets the nature of the subsystem under which that the program will
     * execute.
     * </p>
     * <table style="width:100%;border-collapse:collapse;border:1px solid black;">
     * <caption></caption>
     * <thead><tr><th>Supported subsystems</th></tr></thead>
     * <tbody>
     * <tr>
     * <td>gui</td>
     * <td>Graphical User Interface</td>
     * </tr>
     * <tr>
     * <td>console</td>
     * <td>Command Line Console</td>
     * </tr>
     * <tr>
     * <td>other</td>
     * <td>Other</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param subsystem subsystem
     * @throws NullPointerException if subsystem is null
     */
    public void setSubsystem(SubsystemEnum subsystem) {
        if (subsystem == null) {
            throw new NullPointerException("subsystem");
        }
        linkType.setSubsystem(subsystem);
    }

    /**
     * Gets subsystem name.
     *
     * @return Subsystem name
     */
    public String getSubsystem() {
        return linkType.getSubsystem();
    }

    /**
     * Enumerated attribute with the values "none", "severe", "default",
     * "production", "diagnostic", and "aserror".
     *
     * @param level WarningLevelEnum
     */
    public void setWarnings(WarningLevelEnum level) {
        compilerDef.setWarnings(level);
    }

    /**
     * Indicates whether the build will continue
     * even if there are compilation errors; defaults to true.
     *
     * @param fail if true halt the build on failure
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    /**
     * Gets the failonerror flag.
     *
     * @return the failonerror flag
     */
    public boolean getFailonerror() {
        return failOnError;
    }

    /**
     * Adds a target definition or reference (Non-functional prototype).
     *
     * @param target target
     * @throws NullPointerException if compiler is null
     */
    public void addConfiguredTarget(TargetDef target) {
        if (target == null) {
            throw new NullPointerException("target");
        }
        target.setProject(getProject());
        targetPlatforms.addElement(target);
    }

    /**
     * Adds a distributer definition or reference (Non-functional prototype).
     *
     * @param distributer distributer
     * @throws NullPointerException if compiler is null
     */
    public void addConfiguredDistributer(DistributerDef distributer) {
        if (distributer == null) {
            throw new NullPointerException("distributer");
        }
        distributer.setProject(getProject());
        distributers.addElement(distributer);
    }

    /**
     * Sets optimization.
     *
     * @param optimization OptimizationEnum
     */
    public void setOptimize(OptimizationEnum optimization) {
        compilerDef.setOptimize(optimization);
    }

    /**
     * Adds descriptive version information to be included in the
     * generated file.  The first active version info block will
     * be used.
     *
     * @param newVersionInfo VersionInfo
     */
    public void addConfiguredVersioninfo(VersionInfo newVersionInfo) {
        newVersionInfo.setProject(this.getProject());
        versionInfos.addElement(newVersionInfo);
    }
}
