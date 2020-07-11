/*
 *
 * Copyright 2004-2005 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.apple;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.TargetInfo;
import net.sf.antcontrib.cpptasks.compiler.CommandLineCompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinkerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.ProcessorConfiguration;
import net.sf.antcontrib.cpptasks.gcc.GccCCompiler;
import net.sf.antcontrib.cpptasks.ide.CommentDef;
import net.sf.antcontrib.cpptasks.ide.DependencyDef;
import net.sf.antcontrib.cpptasks.ide.ProjectDef;
import net.sf.antcontrib.cpptasks.ide.ProjectWriter;
import org.apache.tools.ant.BuildException;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.sf.antcontrib.cpptasks.CUtil.getRelativePath;
import static net.sf.antcontrib.cpptasks.CUtil.isSystemPath;
import static net.sf.antcontrib.cpptasks.CUtil.toUnixPath;

/**
 * Writes a Apple Xcode 2.1+ project directory.  XCode stores project
 * configuration as a PropertyList.  Though it will always write the project
 * as a Cocoa Old-Style ASCII property list, it will read projects
 * stored using Cocoa's XML Property List format.
 */
public final class XcodeProjectWriter implements ProjectWriter {
    /**
     * Constructor.
     */
    public XcodeProjectWriter() {
    }

    /**
     * Writes a project definition file.
     *
     * @param fileName   File name base, writer may append appropriate extension
     * @param task       cc task for which to write project
     * @param projectDef project element
     * @param sources    list of source files
     * @param targets    compilation targets
     * @param linkTarget link target
     * @throws IOException if error writing project file
     */
    public void writeProject(final File fileName,
                             final CCTask task,
                             final ProjectDef projectDef,
                             final List<File> sources,
                             final Hashtable<String, TargetInfo> targets,
                             final TargetInfo linkTarget) throws IOException {
        File xcodeDir = new File(fileName + ".xcodeproj");
        if (!projectDef.getOverwrite() && xcodeDir.exists()) {
            throw new BuildException("Not allowed to overwrite project file "
                    + xcodeDir.toString());
        }

        CommandLineCompilerConfiguration compilerConfig = getBaseCompilerConfiguration(targets);
        if (compilerConfig == null) {
            throw new BuildException("Unable to find compilation target using GNU C++ compiler");
        }

        CommandLineLinkerConfiguration linkerConfig = null;
        if (linkTarget.getConfiguration() instanceof CommandLineLinkerConfiguration) {
            linkerConfig = (CommandLineLinkerConfiguration) linkTarget.getConfiguration();
        }

        String projectName = projectDef.getName();
        if (projectName == null) {
            projectName = fileName.getName();
        }
        final String basePath = fileName.getAbsoluteFile().getParent();

        xcodeDir.mkdir();

        File xcodeProj = new File(xcodeDir, "project.pbxproj");

        //
        //   create property list
        //
        Map<String, Object> propertyList = new HashMap<String, Object>();
        propertyList.put("archiveVersion", "1");
        propertyList.put("classes", new HashMap<String, Object>());
        propertyList.put("objectVersion", "42");
        Map<String, Object> objects = new HashMap<String, Object>();

        final String sourceTree = "<source>";

        //
        //   add source files and source group to property list
        //
        List<PBXObjectRef> sourceGroupChildren = addSources(objects, "SOURCE_ROOT", basePath, targets);
        PBXObjectRef sourceGroup = createPBXGroup("Source", sourceTree, sourceGroupChildren);
        objects.put(sourceGroup.getID(), sourceGroup.getProperties());

        //
        //    add product to property list
        //
        PBXObjectRef product = addProduct(objects, linkTarget);
        List<PBXObjectRef> productsList = new ArrayList<PBXObjectRef>();
        productsList.add(product);
        PBXObjectRef productsGroup = createPBXGroup("Products", sourceTree, productsList);
        objects.put(productsGroup.getID(), productsGroup.getProperties());

        //
        //    add documentation group to property list
        //
        PBXObjectRef documentationGroup = addDocumentationGroup(objects, sourceTree);

        //
        //    add main group containing source, products and documentation group
        //
        List<PBXObjectRef> groups = new ArrayList<PBXObjectRef>(3);
        groups.add(sourceGroup);
        groups.add(documentationGroup);
        groups.add(productsGroup);
        PBXObjectRef mainGroup = createPBXGroup(projectName, sourceTree, groups);
        StringBuilder comments = new StringBuilder();
        for (CommentDef comment : projectDef.getComments()) {
            comments.append(comment);
        }
        if (comments.length() > 0) {
            mainGroup.getProperties().put("comments", comments.toString());
        }
        objects.put(mainGroup.getID(), mainGroup.getProperties());

        //
        //   add project configurations
        //
        PBXObjectRef compilerConfigurations = addProjectConfigurationList(objects, basePath,
                projectDef.getDependencies(), compilerConfig, linkerConfig);

        String projectDirPath = "";
        List<PBXObjectRef> projectTargets = new ArrayList<PBXObjectRef>();

        //
        //    add project to property list
        //
        //
        //   Calculate path (typically several ../..) of the root directory
        //        (where build.xml lives) relative to the XCode project directory.
        //         XCode 3.0 will now prompt user to supply the value if not specified.
        String projectRoot = toUnixPath(getRelativePath(basePath,
                projectDef.getProject().getBaseDir()));
        PBXObjectRef project = createPBXProject(compilerConfigurations, mainGroup,
                projectDirPath, projectRoot, projectTargets);
        objects.put(project.getID(), project.getProperties());

        List<PBXObjectRef> frameworkBuildFiles = new ArrayList<PBXObjectRef>();
        for (DependencyDef dependency : projectDef.getDependencies()) {
            PBXObjectRef buildFile = addDependency(objects, project, groups, basePath, dependency);
            if (buildFile != null) {
                frameworkBuildFiles.add(buildFile);
            }
        }
        //
        //   add description of native target (that is the executable or
        //      shared library)
        //
        PBXObjectRef nativeTarget = addNativeTarget(objects, linkTarget, product,
                projectName, sourceGroupChildren, frameworkBuildFiles);
        projectTargets.add(nativeTarget);

        //
        //    finish up overall property list
        //
        propertyList.put("objects", objects);
        propertyList.put("rootObject", project.getID());

        //
        //    write property list out to XML file
        //
        try {
            PropertyListSerialization.serialize(propertyList, projectDef.getComments(), xcodeProj);
        } catch (TransformerConfigurationException ex) {
            throw new IOException(ex.toString());
        } catch (SAXException ex) {
            if (ex.getException() instanceof IOException) {
                throw (IOException) ex.getException();
            }
            throw new IOException(ex.toString());
        }
    }

    /**
     * Adds a dependency to the object graph.
     *
     * @param objects String - Object map
     * @param project PBXObjectRef
     * @param mainGroupChildren PBXObjectRef list
     * @param baseDir String
     * @param dependency DependencyDef
     * @return PBXBuildFile to add to PBXFrameworksBuildPhase.
     */
    private PBXObjectRef addDependency(final Map<String, Object> objects,
                                       final PBXObjectRef project,
                                       final List<PBXObjectRef> mainGroupChildren,
                                       final String baseDir,
                                       final DependencyDef dependency) {
        if (dependency.getFile() != null) {
            File xcodeDir = new File(dependency.getFile().getAbsolutePath() + ".xcodeproj");
            if (xcodeDir.exists()) {
                PBXObjectRef xcodePrj = createPBXFileReference("SOURCE_ROOT", baseDir, xcodeDir);
                mainGroupChildren.add(xcodePrj);
                objects.put(xcodePrj.getID(), xcodePrj.getProperties());

                int proxyType = 2;
                PBXObjectRef proxy = createPBXContainerItemProxy(xcodePrj, proxyType, dependency.getName());
                objects.put(proxy.getID(), proxy.getProperties());

                PBXObjectRef referenceProxy = createPBXReferenceProxy(proxy, dependency);
                objects.put(referenceProxy.getID(), referenceProxy.getProperties());

                PBXObjectRef buildFile = createPBXBuildFile(referenceProxy, Collections.<String, Object>emptyMap());
                objects.put(buildFile.getID(), buildFile.getProperties());

                List<PBXObjectRef> productsChildren = new ArrayList<PBXObjectRef>();
                productsChildren.add(referenceProxy);
                PBXObjectRef products = createPBXGroup("Products", "<group>", productsChildren);
                objects.put(products.getID(), products.getProperties());

                Map<String, Object> projectReference = new HashMap<String, Object>();
                projectReference.put("ProductGroup", products);
                projectReference.put("ProjectRef", xcodePrj);

                List<Map<String, Object>> projectReferences = (List<Map<String, Object>>) project.getProperties().get("ProjectReferences");
                if (projectReferences == null) {
                    projectReferences = new ArrayList<Map<String, Object>>();
                    project.getProperties().put("ProjectReferences", projectReferences);
                }
                projectReferences.add(projectReference);
                return buildFile;
            }
        }
        return null;
    }

    /**
     * Add documentation group to map of objects.
     *
     * @param objects    object map.
     * @param sourceTree source tree description.
     * @return documentation group.
     */
    private PBXObjectRef addDocumentationGroup(final Map<String, Object> objects,
                                               final String sourceTree) {
        List<PBXObjectRef> productsList = new ArrayList<PBXObjectRef>();
        PBXObjectRef products = createPBXGroup("Documentation", sourceTree, productsList);
        objects.put(products.getID(), products.getProperties());
        return products;
    }

    /**
     * Add file reference of product to map of objects.
     *
     * @param objects    object map.
     * @param linkTarget build description for executable or shared library.
     * @return file reference to generated executable or shared library.
     */
    private PBXObjectRef addProduct(final Map<String, Object> objects,
                                    final TargetInfo linkTarget) {
        //
        //   create file reference for executable file
        //     forget Ant's location, just place in XCode's default location
        PBXObjectRef executable = createPBXFileReference("BUILD_PRODUCTS_DIR",
                linkTarget.getOutput().getParent(),
                linkTarget.getOutput());
        Map<String, Object> executableProperties = executable.getProperties();

        String fileType = getFileType(linkTarget);
        executableProperties.put("explicitFileType", fileType);
        executableProperties.put("includeInIndex", "0");
        objects.put(executable.getID(), executableProperties);

        return executable;
    }

    /**
     * Add file references for all source files to map of objects.
     *
     * @param objects    map of objects.
     * @param sourceTree source tree.
     * @param basePath   parent of XCode project dir
     * @param targets    build targets.
     * @return list containing file references of source files.
     */
    private List<PBXObjectRef> addSources(final Map<String, Object> objects,
                                          final String sourceTree,
                                          final String basePath,
                                          final Hashtable<String, TargetInfo> targets) {
        List<PBXObjectRef> sourceGroupChildren = new ArrayList<PBXObjectRef>();

        List<File> sourceList = new ArrayList<File>();
        for (TargetInfo info : targets.values()) {
            Collections.addAll(sourceList, info.getSources());
        }
        File[] sortedSources = sourceList.toArray(new File[]{});
        Arrays.sort(sortedSources, new Comparator<File>() {
            public int compare(final File o1, final File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (File sortedSource : sortedSources) {
            PBXObjectRef fileRef = createPBXFileReference(sourceTree,
                    basePath, sortedSource);
            sourceGroupChildren.add(fileRef);
            objects.put(fileRef.getID(), fileRef.getProperties());
        }

        return sourceGroupChildren;
    }

    /**
     * Add native target configuration list.
     *
     * @param objects     map of objects.
     * @param projectName project name.
     * @return build configurations for native target.
     */
    private PBXObjectRef addNativeTargetConfigurationList(final Map<String, Object> objects,
                                                          final String projectName) {
        //
        //   Create a configuration list with
        //     two stock configurations: Debug and Release
        //
        List<PBXObjectRef> configurations = new ArrayList<PBXObjectRef>();
        Map<String, Object> debugSettings = new HashMap<String, Object>();
        debugSettings.put("COPY_PHASE_STRIP", "NO");
        debugSettings.put("GCC_DYNAMIC_NO_PIC", "NO");
        debugSettings.put("GCC_ENABLE_FIX_AND_CONTINUE", "YES");
        debugSettings.put("GCC_MODEL_TUNING", "G5");
        debugSettings.put("GCC_OPTIMIZATION_LEVEL", "0");
        debugSettings.put("INSTALL_PATH", "$(HOME)/bin");
        debugSettings.put("PRODUCT_NAME", projectName);
        debugSettings.put("ZERO_LINK", "YES");
        PBXObjectRef debugConfig = createXCBuildConfiguration("Debug",
                debugSettings);
        objects.put(debugConfig.getID(), debugConfig.getProperties());
        configurations.add(debugConfig);

        Map<String, Object> releaseSettings = new HashMap<String, Object>();
        List<String> archs = new ArrayList<String>();
        archs.add("ppc");
        archs.add("i386");
        releaseSettings.put("ARCHS", archs);
        releaseSettings.put("GCC_GENERATE_DEBUGGING_SYMBOLS", "NO");
        releaseSettings.put("GCC_MODEL_TUNING", "G5");
        releaseSettings.put("INSTALL_PATH", "$(HOME)/bin");
        releaseSettings.put("PRODUCT_NAME", projectName);
        PBXObjectRef releaseConfig = createXCBuildConfiguration("Release",
                releaseSettings);
        objects.put(releaseConfig.getID(), releaseConfig.getProperties());
        configurations.add(releaseConfig);

        PBXObjectRef configurationList = createXCConfigurationList(configurations);
        objects.put(configurationList.getID(), configurationList.getProperties());
        return configurationList;
    }

    /**
     * Add project configuration list.
     *
     * @param objects        map of objects.
     * @param baseDir        base directory.
     * @param dependencies   a list of dependencies.
     * @param compilerConfig compiler configuration.
     * @param linkerConfig   linker configuration.
     * @return project configuration object.
     */
    private PBXObjectRef addProjectConfigurationList(final Map<String, Object> objects,
                                                     final String baseDir,
                                                     final List<DependencyDef> dependencies,
                                                     final CommandLineCompilerConfiguration compilerConfig,
                                                     final CommandLineLinkerConfiguration linkerConfig) {
        //
        //   Create a configuration list with
        //     two stock configurations: Debug and Release
        //
        List<PBXObjectRef> configurations = new ArrayList<PBXObjectRef>();
        Map<String, Object> debugSettings = new HashMap<String, Object>();
        debugSettings.put("GCC_WARN_ABOUT_RETURN_TYPE", "YES");
        debugSettings.put("GCC_WARN_UNUSED_VARIABLE", "YES");
        debugSettings.put("PREBINDING", "NO");
        debugSettings.put("SDKROOT", "/Developer/SDKs/MacOSX10.4u.sdk");

        PBXObjectRef debugConfig = createXCBuildConfiguration("Debug", debugSettings);
        objects.put(debugConfig.getID(), debugConfig.getProperties());
        configurations.add(debugConfig);

        Map<String, Object> releaseSettings = new HashMap<String, Object>();
        releaseSettings.put("GCC_WARN_ABOUT_RETURN_TYPE", "YES");
        releaseSettings.put("GCC_WARN_UNUSED_VARIABLE", "YES");
        releaseSettings.put("PREBINDING", "NO");
        releaseSettings.put("SDKROOT", "/Developer/SDKs/MacOSX10.4u.sdk");
        PBXObjectRef releaseConfig = createXCBuildConfiguration("Release", releaseSettings);
        objects.put(releaseConfig.getID(), releaseConfig.getProperties());
        configurations.add(releaseConfig);
        PBXObjectRef configurationList = createXCConfigurationList(configurations);
        Map<String, Object> projectConfigurationListProperties = configurationList.getProperties();
        projectConfigurationListProperties.put("defaultConfigurationIsVisible", "0");
        projectConfigurationListProperties.put("defaultConfigurationName", "Debug");
        objects.put(configurationList.getID(), configurationList.getProperties());

        //
        //    add include paths to both configurations
        //
        File[] includeDirs = compilerConfig.getIncludePath();
        if (includeDirs.length > 0) {
            List<String> includePaths = new ArrayList<String>();
            Map<String, String> includePathMap = new HashMap<String, String>();
            for (File includeDir : includeDirs) {
                if (!isSystemPath(includeDir)) {
                    String absPath = includeDir.getAbsolutePath();
                    if (!includePathMap.containsKey(absPath)) {
                        if (absPath.startsWith("/usr/")) {
                            includePaths.add(toUnixPath(absPath));
                        } else {
                            String relPath = toUnixPath(getRelativePath(baseDir,
                                    includeDir));
                            includePaths.add(relPath);
                        }
                        includePathMap.put(absPath, absPath);
                    }
                }
            }
            includePaths.add("${inherited)");
            debugSettings.put("HEADER_SEARCH_PATHS", includePaths);
            releaseSettings.put("HEADER_SEARCH_PATHS", includePaths);
        }

        //
        //   add preprocessor definitions to both configurations
        //
        //
        String[] preArgs = compilerConfig.getPreArguments();
        List<String> defines = new ArrayList<String>();
        for (String preArg : preArgs) {
            if (preArg.startsWith("-D")) {
                defines.add(preArg.substring(2));
            }
        }
        if (defines.size() > 0) {
            defines.add("$(inherited)");
            debugSettings.put("GCC_PREPROCESSOR_DEFINITIONS", defines);
            releaseSettings.put("GCC_PREPROCESSOR_DEFINITIONS", defines);
        }

        if (linkerConfig != null) {
            Map<String, String> librarySearchMap = new HashMap<String, String>();
            List<String> librarySearchPaths = new ArrayList<String>();
            List<String> otherLdFlags = new ArrayList<String>();
            String[] linkerArgs = linkerConfig.getEndArguments();
            for (String linkerArg : linkerArgs) {
                if (linkerArg.startsWith("-L")) {
                    String libDir = linkerArg.substring(2);
                    if (!librarySearchMap.containsKey(libDir)) {
                        if (!libDir.equals("/usr/lib")) {
                            librarySearchPaths.add(toUnixPath(getRelativePath(baseDir,
                                    new File(libDir))));
                        }
                        librarySearchMap.put(libDir, libDir);

                    }
                } else if (linkerArg.startsWith("-l")) {
                    //
                    //  check if library is in dependencies list
                    //
                    String libName = linkerArg.substring(2);
                    boolean found = false;
                    for (DependencyDef dependency : dependencies) {
                        if (libName.startsWith(dependency.getName())) {
                            File dependencyFile = dependency.getFile();
                            if (dependencyFile != null
                                    && new File(dependencyFile.getAbsolutePath()
                                    + ".xcodeproj").exists()) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        otherLdFlags.add(linkerArg);
                    }
                }
            }

            debugSettings.put("LIBRARY_SEARCH_PATHS", librarySearchPaths);
            debugSettings.put("OTHER_LDFLAGS", otherLdFlags);
            releaseSettings.put("LIBRARY_SEARCH_PATHS", librarySearchPaths);
            releaseSettings.put("OTHER_LDFLAGS", otherLdFlags);
        }
        return configurationList;
    }

    /**
     * Add native target to map of objects.
     *
     * @param objects             map of objects.
     * @param linkTarget          description of executable or shared library.
     * @param product             product.
     * @param projectName         project name.
     * @param sourceGroupChildren source files needed to build product.
     * @param frameworkBuildFiles framework files needed to build product.
     * @return native target.
     */
    private PBXObjectRef addNativeTarget(final Map<String, Object> objects,
                                         final TargetInfo linkTarget,
                                         final PBXObjectRef product,
                                         final String projectName,
                                         final List<PBXObjectRef> sourceGroupChildren,
                                         final List<PBXObjectRef> frameworkBuildFiles) {
        int buildActionMask = 2147483647;
        List<PBXObjectRef> buildPhases = new ArrayList<PBXObjectRef>();
        PBXObjectRef buildConfigurations = addNativeTargetConfigurationList(objects, projectName);

        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("ATTRIBUTES", new ArrayList<Object>());
        List<PBXObjectRef> buildFiles = new ArrayList<PBXObjectRef>();
        for (PBXObjectRef sourceFile : sourceGroupChildren) {
            PBXObjectRef buildFile = createPBXBuildFile(sourceFile, settings);
            buildFiles.add(buildFile);
            objects.put(buildFile.getID(), buildFile.getProperties());
        }

        PBXObjectRef sourcesBuildPhase = createPBXSourcesBuildPhase(buildActionMask,
                buildFiles, false);
        objects.put(sourcesBuildPhase.getID(), sourcesBuildPhase.getProperties());
        buildPhases.add(sourcesBuildPhase);

        buildActionMask = 8;
        PBXObjectRef frameworksBuildPhase = createPBXFrameworksBuildPhase(buildActionMask,
                        frameworkBuildFiles, false);
        objects.put(frameworksBuildPhase.getID(), frameworksBuildPhase.getProperties());
        buildPhases.add(frameworksBuildPhase);

        PBXObjectRef copyFilesBuildPhase = createPBXCopyFilesBuildPhase(8,
                "/usr/share/man/man1", "0", new ArrayList<File>(), true);
        objects.put(copyFilesBuildPhase.getID(), copyFilesBuildPhase.getProperties());
        buildPhases.add(copyFilesBuildPhase);

        List<Object> buildRules = new ArrayList<Object>();

        List<Object> dependencies = new ArrayList<Object>();

        String productInstallPath = "$(HOME)/bin";

        String productType = getProductType(linkTarget);

        PBXObjectRef nativeTarget = createPBXNativeTarget(projectName,
                buildConfigurations, buildPhases, buildRules, dependencies,
                productInstallPath, projectName, product, productType);
        objects.put(nativeTarget.getID(), nativeTarget.getProperties());

        return nativeTarget;
    }

    private int getProductTypeIndex(final TargetInfo linkTarget) {
        String outPath = linkTarget.getOutput().getPath();
        String outExtension = null;
        int lastDot = outPath.lastIndexOf('.');
        if (lastDot != -1) {
            outExtension = outPath.substring(lastDot);
        }
        if (".a".equalsIgnoreCase(outExtension) || ".lib".equalsIgnoreCase(outExtension)) {
            return 1;
        } else if (".dylib".equalsIgnoreCase(outExtension) || ".so".equalsIgnoreCase(outExtension)
                || ".dll".equalsIgnoreCase(outExtension)) {
            return 2;
        }
        return 0;
    }

    private String getProductType(final TargetInfo linkTarget) {
        switch (getProductTypeIndex(linkTarget)) {
            case 1:
                return "com.apple.product-type.library.static";
            case 2:
                return "com.apple.product-type.library.dynamic";
            default:
                return "com.apple.product-type.tool";
        }
    }

    private String getFileType(final TargetInfo linkTarget) {
        switch (getProductTypeIndex(linkTarget)) {
            case 1:
                return "archive.ar";
            case 2:
                return "compiled.mach-o.dylib";
            default:
                return "compiled.mach-o.executable";
        }
    }

    /**
     * Create PBXFileReference.
     *
     * @param sourceTree source tree.
     * @param baseDir    base directory.
     * @param file       file.
     * @return PBXFileReference object.
     */
    private static PBXObjectRef createPBXFileReference(final String sourceTree,
                                                       final String baseDir,
                                                       final File file) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "PBXFileReference");

        String relPath = toUnixPath(getRelativePath(baseDir, file));
        map.put("path", relPath);
        map.put("name", file.getName());
        map.put("sourceTree", sourceTree);
        return new PBXObjectRef(map);
    }

    /**
     * Create PBXGroup.
     *
     * @param name       group name.
     * @param sourceTree source tree.
     * @param children   list of PBXFileReferences.
     * @return group.
     */
    private static PBXObjectRef createPBXGroup(final String name,
                                               final String sourceTree,
                                               final List<PBXObjectRef> children) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "PBXGroup");
        map.put("name", name);
        map.put("sourceTree", sourceTree);
        map.put("children", children);
        return new PBXObjectRef(map);
    }

    /**
     * Create PBXProject.
     *
     * @param buildConfigurationList build configuration list.
     * @param mainGroup              main group.
     * @param projectDirPath         project directory path.
     * @param targets                targets.
     * @param projectRoot            projectRoot directory relative to
     * @return project.
     */
    private static PBXObjectRef createPBXProject(final PBXObjectRef buildConfigurationList,
                                                 final PBXObjectRef mainGroup,
                                                 final String projectDirPath,
                                                 final String projectRoot,
                                                 final List<PBXObjectRef> targets) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "PBXProject");
        map.put("buildConfigurationList", buildConfigurationList.getID());
        map.put("hasScannedForEncodings", "0");
        map.put("mainGroup", mainGroup.getID());
        map.put("projectDirPath", projectDirPath);
        map.put("targets", targets);
        map.put("projectRoot", projectRoot);
        return new PBXObjectRef(map);
    }

    /**
     * Create XCConfigurationList.
     *
     * @param buildConfigurations build configurations.
     * @return configuration list.
     */
    private static PBXObjectRef createXCConfigurationList(final List<PBXObjectRef> buildConfigurations) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "XCConfigurationList");
        map.put("buildConfigurations", buildConfigurations);
        return new PBXObjectRef(map);
    }

    /**
     * Create XCBuildConfiguration.
     *
     * @param name          name.
     * @param buildSettings build settings.
     * @return build configuration.
     */
    private static PBXObjectRef createXCBuildConfiguration(final String name,
                                                           final Map<String, Object> buildSettings) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "XCBuildConfiguration");
        map.put("buildSettings", buildSettings);
        map.put("name", name);
        return new PBXObjectRef(map);
    }

    /**
     * Create PBXNativeTarget.
     *
     * @param name                   name.
     * @param buildConfigurationList build configuration list.
     * @param buildPhases            build phases.
     * @param buildRules             build rules.
     * @param dependencies           dependencies.
     * @param productInstallPath     product install path.
     * @param productName            product name.
     * @param productReference       file reference for product.
     * @param productType            product type.
     * @return native target.
     */
    private static PBXObjectRef createPBXNativeTarget(final String name,
                                                      final PBXObjectRef buildConfigurationList,
                                                      final List<PBXObjectRef> buildPhases,
                                                      final List<Object> buildRules,
                                                      final List<Object> dependencies,
                                                      final String productInstallPath,
                                                      final String productName,
                                                      final PBXObjectRef productReference,
                                                      final String productType) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "PBXNativeTarget");
        map.put("buildConfigurationList", buildConfigurationList);
        map.put("buildPhases", buildPhases);
        map.put("buildRules", buildRules);
        map.put("dependencies", dependencies);
        map.put("name", name);
        map.put("productInstallPath", productInstallPath);
        map.put("productName", productName);
        map.put("productReference", productReference);
        map.put("productType", productType);
        return new PBXObjectRef(map);
    }

    /**
     * Create PBXSourcesBuildPhase.
     *
     * @param buildActionMask build action mask.
     * @param files           source files.
     * @param runOnly         if true, phase should only be run on deployment.
     * @return PBXSourcesBuildPhase.
     */
    private static PBXObjectRef createPBXSourcesBuildPhase(int buildActionMask,
                                                           List<PBXObjectRef> files,
                                                           boolean runOnly) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("buildActionMask",
                String.valueOf(buildActionMask));
        map.put("files", files);
        map.put("isa", "PBXSourcesBuildPhase");
        map.put("runOnlyForDeploymentPostprocessing", toString(runOnly));
        return new PBXObjectRef(map);
    }

    /**
     * Create PBXBuildFile.
     *
     * @param fileRef  source file.
     * @param settings build settings.
     * @return PBXBuildFile.
     */
    private static PBXObjectRef createPBXBuildFile(PBXObjectRef fileRef,
                                                   Map<String, Object> settings) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("fileRef", fileRef);
        map.put("isa", "PBXBuildFile");
        if (settings != null) {
            map.put("settings", settings);
        }
        return new PBXObjectRef(map);
    }

    /**
     * Create PBXFrameworksBuildPhase.
     *
     * @param buildActionMask build action mask.
     * @param files           files.
     * @param runOnly         if true, phase should only be run on deployment.
     * @return PBXFrameworkBuildPhase.
     */
    private static PBXObjectRef createPBXFrameworksBuildPhase(final int buildActionMask,
                                                              final List<PBXObjectRef> files,
                                                              final boolean runOnly) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "PBXFrameworksBuildPhase");
        map.put("buildActionMask", NumberFormat.getIntegerInstance(Locale.US).format(buildActionMask));
        map.put("files", files);
        map.put("runOnlyForDeploymentPostprocessing", toString(runOnly));
        return new PBXObjectRef(map);
    }

    /**
     * Create a build phase that copies files to a destination.
     *
     * @param buildActionMask  build action mask.
     * @param dstPath          destination path.
     * @param dstSubfolderSpec subfolder spec.
     * @param files            files.
     * @param runOnly          if true, phase should only be run on deployment.
     * @return PBXCopyFileBuildPhase.
     */
    private static PBXObjectRef createPBXCopyFilesBuildPhase(final int buildActionMask,
                                                             final String dstPath,
                                                             final String dstSubfolderSpec,
                                                             final List<File> files,
                                                             final boolean runOnly) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "PBXCopyFilesBuildPhase");
        map.put("buildActionMask", NumberFormat.getIntegerInstance(Locale.US).format(buildActionMask));
        map.put("dstPath", dstPath);
        map.put("dstSubfolderSpec", dstSubfolderSpec);
        map.put("files", files);
        map.put("runOnlyForDeploymentPostprocessing", toString(runOnly));
        return new PBXObjectRef(map);
    }

    /**
     * Create a proxy for a file in a different project.
     *
     * @param containerPortal XcodeProject containing file.
     * @param proxyType       proxy type.
     * @return PBXContainerItemProxy.
     */
    private static PBXObjectRef createPBXContainerItemProxy(final PBXObjectRef containerPortal,
                                                            final int proxyType,
                                                            final String remoteInfo) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "PBXContainerItemProxy");
        map.put("containerPortal", containerPortal);
        map.put("proxyType", NumberFormat.getIntegerInstance(Locale.US).format(proxyType));
        map.put("remoteInfo", remoteInfo);
        return new PBXObjectRef(map);
    }

    /**
     * Create a proxy for a file in a different project.
     *
     * @param remoteRef  PBXContainerItemProxy for reference.
     * @param dependency dependency.
     * @return PBXContainerItemProxy.
     */
    private static PBXObjectRef createPBXReferenceProxy(final PBXObjectRef remoteRef,
                                                        final DependencyDef dependency) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("isa", "PBXReferenceProxy");
        String fileType = "compiled.mach-o.dylib";
        map.put("fileType", fileType);
        map.put("remoteRef", remoteRef);
        map.put("path", dependency.getFile().getName() + ".dylib");
        map.put("sourceTree", "BUILT_PRODUCTS_DIR");
        return new PBXObjectRef(map);
    }

    /**
     * Method returns "1" for true, "0" for false.
     *
     * @param b boolean value.
     * @return "1" for true, "0" for false.
     */
    private static String toString(boolean b) {
        if (b) {
            return "1";
        } else {
            return "0";
        }
    }

    /**
     * Represents a property map with an 96 bit identity.
     * When placed in a property list, this object will
     * output the string representation of the identity
     * which XCode uses to find the corresponding property
     * bag in the "objects" property of the top-level property list.
     */
    private static final class PBXObjectRef {
        /**
         * Identifier.
         */
        private final String id;
        /**
         * Properties.
         */
        private final Map<String, Object> properties;
        /**
         * Next available identifier.
         */
        private static int nextID = 0;

        /**
         * Create reference.
         *
         * @param props properties.
         */
        public PBXObjectRef(final Map<String, Object> props) {
            if (props == null) {
                throw new NullPointerException("props");
            }
            StringBuilder buf = new StringBuilder("000000000000000000000000");
            String idStr = Integer.toHexString(nextID++);
            buf.replace(buf.length() - idStr.length(), buf.length(), idStr);
            id = buf.toString();
            properties = props;
        }

        /**
         * Get object identifier.
         *
         * @return identifier.
         */
        public String toString() {
            return id;
        }

        /**
         * Get object identifier.
         *
         * @return object identifier.
         */
        public String getID() {
            return id;
        }

        /**
         * Get properties.
         *
         * @return properties.
         */
        public Map<String, Object> getProperties() {
            return properties;
        }
    }

    /**
     * Gets the first recognized compiler from the
     * compilation targets.
     *
     * @param targets compilation targets
     * @return representative (hopefully) compiler configuration
     */
    private CommandLineCompilerConfiguration getBaseCompilerConfiguration(
            Hashtable<String, TargetInfo> targets) {
        //
        //   find first target with an GNU C++ compilation
        //
        CommandLineCompilerConfiguration compilerConfig;
        //
        //   get the first target and assume that it is representative
        //
        for (TargetInfo targetInfo : targets.values()) {
            ProcessorConfiguration config = targetInfo.getConfiguration();
            //
            //   for the first cl compiler
            //
            if (config instanceof CommandLineCompilerConfiguration) {
                compilerConfig = (CommandLineCompilerConfiguration) config;
                if (compilerConfig.getCompiler() instanceof GccCCompiler) {
                    return compilerConfig;
                }
            }
        }
        return null;
    }
}
