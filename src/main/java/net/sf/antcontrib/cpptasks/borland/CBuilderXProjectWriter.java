/*
 *
 * Copyright 2004-2005 The Ant-Contrib project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sf.antcontrib.cpptasks.borland;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.TargetInfo;
import net.sf.antcontrib.cpptasks.compiler.CommandLineCompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinkerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.ProcessorConfiguration;
import net.sf.antcontrib.cpptasks.gcc.GccCCompiler;
import net.sf.antcontrib.cpptasks.ide.ProjectDef;
import net.sf.antcontrib.cpptasks.ide.ProjectWriter;
import org.apache.tools.ant.BuildException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import static net.sf.antcontrib.cpptasks.CUtil.getRelativePath;

/**
 * Writes a CBuilderX 1.0 project file.
 *
 * @author Curt Arnold
 */
public final class CBuilderXProjectWriter implements ProjectWriter {
    /**
     * Constructor.
     */
    public CBuilderXProjectWriter() {
    }

    /**
     * Writes a project definition file.
     *
     * @param fileName   project name for file, should has .cbx extension
     * @param task       cc task for which to write project
     * @param projectDef project element
     * @param sources    source files
     * @param targets    compilation targets
     * @param linkTarget link target
     * @throws IOException  if I/O error
     * @throws SAXException if XML serialization error
     * @throws TransformerConfigurationException if XML configuration error
     */
    public void writeProject(final File fileName,
                             final CCTask task,
                             final ProjectDef projectDef,
                             final List<File> sources,
                             final Hashtable<String, TargetInfo> targets,
                             final TargetInfo linkTarget)
            throws IOException, SAXException, TransformerConfigurationException {
        final String basePath = fileName.getAbsoluteFile().getParent();

        File projectFile = new File(fileName + ".cbx");
        if (!projectDef.getOverwrite() && projectFile.exists()) {
            throw new BuildException("Not allowed to overwrite project file "
                    + projectFile.toString());
        }

        CommandLineCompilerConfiguration compilerConfig = getBaseCompilerConfiguration(targets);
        if (compilerConfig == null) {
            throw new BuildException("Unable to generate C++ BuilderX project"
                    + " when gcc or bcc is not used.");
        }

        OutputStream outStream = new FileOutputStream(projectFile);
        StreamResult result = new StreamResult(outStream);

        SAXTransformerFactory sf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler content = sf.newTransformerHandler();
        content.setResult(result);

        content.startDocument();
        Attributes emptyAttrs = new AttributesImpl();
        content.startElement(null, "project", "project", emptyAttrs);
        PropertyWriter propertyWriter = new PropertyWriter(content);
        propertyWriter.write("build.config", "active", "0");
        propertyWriter.write("build.config", "count", "0");
        propertyWriter.write("build.config", "excludedefaultforzero", "0");
        propertyWriter.write("build.config.0", "builddir", "Debug");
        propertyWriter.write("build.config.0", "key", "Debug_Build");
        propertyWriter.write("build.config.0", "linux.builddir", "linux/Debug_Build");
        propertyWriter.write("build.config.0", "settings.MinGW", "default;debug");
        propertyWriter.write("build.config.0", "settings.gnuc++", "default;debug");
        propertyWriter.write("build.config.0", "settings.intellinia32", "default;debug");
        propertyWriter.write("build.config.0", "settings.mswin32", "default;debug");
        propertyWriter.write("build.config.0", "type", "Toolset");
        propertyWriter.write("build.config.0", "win32.builddir", "windows/Debug_Build");
        propertyWriter.write("build.node", "name", projectDef.getName());
        final String buildType = getBuildType(task);
        propertyWriter.write("build.node", "type", buildType);
        propertyWriter.write("build.platform", "active", getActivePlatform(task));
        propertyWriter.write("build.platform", "linux.Debug_Build.toolset", "gnuc++");
        propertyWriter.write("build.platform", "linux.Release_Build.toolset", "gnuc++");
        propertyWriter.write("build.platform", "linux.default", "gnuc++");
        propertyWriter.write("build.platform", "linux.gnuc++.enabled", "1");
        propertyWriter.write("build.platform", "linux.mswin32.enabled", "1");
        propertyWriter.write("build.platform", "linux.win32b.enabled", "1");
        propertyWriter.write("build.platform", "solaris.default", "gnuc++");
        propertyWriter.write("build.platform", "solaris.enabled", "1");
        String toolset = getWin32Toolset(compilerConfig);
        propertyWriter.write("build.platform", "win32.default", toolset);
        propertyWriter.write("build.platform", "win32." + toolset + ".enabled", "1");

        propertyWriter.write("cbproject", "version", "X.1.0");
        if ("dllproject".equals(buildType)) {
            propertyWriter.write("gnuc++.g++compile", "option.fpic_using_GOT.enabled", "1");
            propertyWriter.write("gnuc++.g++link", "option.shared.enabled", "1");
            propertyWriter.write("intellinia32.icc", "option.minus_Kpic.enabled", "1");
            propertyWriter.write("intellinia32.icclink", "option.minus_shared.enabled", "1");
        }
        //
        //   assume the first target is representative of all compilation tasks
        //
        writeCompileOptions(basePath, propertyWriter, compilerConfig);
        writeLinkOptions(basePath, propertyWriter, linkTarget);
        propertyWriter.write("linux.gnuc++.Debug_Build", "saved", "1");
        if ("dllproject".equals(buildType)) {
            propertyWriter.write("runtime", "ExcludeDefaultForZero", "1");
            //propertyWriter.write("unique", "id", "852");
        } else if ("exeproject".equals(buildType)) {
            propertyWriter.write("runtime.0", "BuildTargetOnRun",
                    "com.borland.cbuilder.build.CBProjectBuilder$ProjectBuildAction;make");
            propertyWriter.write("runtime.0", "ConfigurationName", projectDef.getName());
            propertyWriter.write("runtime.0", "RunnableType",
                    "com.borland.cbuilder.runtime.ExecutableRunner");
        }
        AttributesImpl fileAttributes = new AttributesImpl();
        fileAttributes.addAttribute(null, "path", "path", "#PCDATA", "");
        AttributesImpl gccAttributes = null;
        if (!"g++".equals(compilerConfig.getCommand())) {
            gccAttributes = new AttributesImpl();
            gccAttributes.addAttribute(null, "category", "category", "#PCDATA", "build.basecmd");
            gccAttributes.addAttribute(null, "name", "name", "#PCDATA",
                    "linux.gnuc++.Debug_Build.g++_key");
            gccAttributes.addAttribute(null, "value", "value", "#PCDATA",
                    compilerConfig.getCommand());
        }

        for (TargetInfo info : targets.values()) {
            for (File targetsource : info.getSources()) {
                String relativePath = getRelativePath(basePath, targetsource);
                fileAttributes.setValue(0, relativePath);
                content.startElement(null, "file", "file", fileAttributes);

                //
                //  if file ends with .c, use gcc instead of g++
                //
                if (gccAttributes != null) {
                    content.startElement(null, "property", "property", gccAttributes);
                    content.endElement(null, "property", "property");
                }
                content.endElement(null, "file", "file");
            }
        }
        content.endElement(null, "project", "project");
        content.endDocument();
    }

    /**
     * Gets build type from link target.
     *
     * @param task CCTask current task
     * @return String build type
     */
    private String getBuildType(final CCTask task) {
        String outType = task.getOuttype();
        if ("executable".equals(outType)) {
            return "exeproject";
        } else if ("static".equals(outType)) {
            return "libraryproject";
        }
        return "dllproject";
    }

    /**
     * Gets active platform.
     *
     * @param task CCTask cc task
     * @return String platform identifier
     */
    private String getActivePlatform(final CCTask task) {
        String osName = System.getProperty("os.name").toLowerCase(Locale.US);
        if (osName.contains("windows")) {
            return "win32";
        }
        return "linux";
    }

    private String getWin32Toolset(final CommandLineCompilerConfiguration compilerConfig) {
        if (compilerConfig != null && compilerConfig.getCompiler() instanceof BorlandCCompiler) {
            return "win32b";
        }
        return "MinGW";
    }

    /**
     * Utility class to generate property elements.
     */
    private static class PropertyWriter {
        /**
         * Content handler.
         */
        private ContentHandler content;

        /**
         * Attributes list.
         */
        private AttributesImpl propertyAttributes;

        /**
         * Constructor.
         *
         * @param contentHandler ContentHandler content handler
         */
        public PropertyWriter(final ContentHandler contentHandler) {
            content = contentHandler;
            propertyAttributes = new AttributesImpl();
            propertyAttributes.addAttribute(null, "category", "category", "#PCDATA", "");
            propertyAttributes.addAttribute(null, "name", "name", "#PCDATA", "");
            propertyAttributes.addAttribute(null, "value", "value", "#PCDATA", "");
        }

        /**
         * Write property element.
         *
         * @param category String category
         * @param name     String property name
         * @param value    String property value
         * @throws SAXException if I/O error or illegal content
         */
        public final void write(final String category,
                                final String name,
                                final String value) throws SAXException {
            propertyAttributes.setValue(0, category);
            propertyAttributes.setValue(1, name);
            propertyAttributes.setValue(2, value);
            content.startElement(null, "property", "property",
                    propertyAttributes);
            content.endElement(null, "property", "property");
        }
    }

    /**
     * Gets the first recognized compiler from the
     * compilation targets.
     *
     * @param targets compilation targets
     * @return representative (hopefully) compiler configuration
     */
    private CommandLineCompilerConfiguration
    getBaseCompilerConfiguration(final Hashtable<String, TargetInfo> targets) {
        //
        //   find first target with an gcc or bcc compilation
        //
        CommandLineCompilerConfiguration compilerConfig;
        //
        //   get the first target and assume that it is representative
        //
        for (TargetInfo targetInfo : targets.values()) {
            ProcessorConfiguration config = targetInfo.getConfiguration();
            String identifier = config.getIdentifier();
            //
            //   for the first gcc or bcc compiler
            //
            if (config instanceof CommandLineCompilerConfiguration) {
                compilerConfig = (CommandLineCompilerConfiguration) config;
                if (compilerConfig.getCompiler() instanceof GccCCompiler
                        || compilerConfig.getCompiler() instanceof BorlandCCompiler) {
                    return compilerConfig;
                }
            }
        }
        return null;
    }

    /**
     * Writes elements corresponding to compilation options.
     *
     * @param baseDir        String base directory
     * @param writer         PropertyWriter property writer
     * @param compilerConfig representative configuration
     * @throws SAXException if I/O error or illegal content
     */
    private void writeCompileOptions(final String baseDir,
                                     final PropertyWriter writer,
                                     final CommandLineCompilerConfiguration
                                             compilerConfig) throws SAXException {
        boolean isBcc = false;
        boolean isUnix = true;
        String compileID = "linux.Debug_Build.gnuc++.g++compile";
        if (compilerConfig.getCompiler() instanceof BorlandCCompiler) {
            compileID = "win32.Debug_Build.win32b.bcc32";
            isUnix = false;
            isBcc = true;
        }

        File[] includePath = compilerConfig.getIncludePath();
        int includeIndex = 1;
        if (isUnix) {
            writer.write(compileID, "option.I.arg." + (includeIndex++), "/usr/include");
            writer.write(compileID, "option.I.arg." + (includeIndex++), "/usr/include/g++-3");
        }
        for (File file : includePath) {
            String relPath = getRelativePath(baseDir, file);
            writer.write(compileID, "option.I.arg." + (includeIndex++), relPath);
        }
        if (includePath.length > 0) {
            writer.write(compileID, "option.I.enabled", "1");
        }

        String defineBase = "option.D_MACRO_VALUE";
        if (isBcc) {
            defineBase = "option.D";
        }
        String defineOption = defineBase + ".arg.";
        int defineIndex = 1;
        int undefineIndex = 1;
        String[] preArgs = compilerConfig.getPreArguments();
        for (String preArg : preArgs) {
            if (preArg.startsWith("-D")) {
                writer.write(compileID, defineOption + (defineIndex++), preArg.substring(2));
            } else if (preArg.startsWith("-U")) {
                writer.write(compileID, "option.U.arg." + (undefineIndex++), preArg.substring(2));
            } else if (!(preArg.startsWith("-I") || preArg.startsWith("-o"))) {
                //
                //  any others (-g, -fno-rtti, -w, -Wall, etc)
                //
                writer.write(compileID, "option." + preArg.substring(1) + ".enabled", "1");
            }
        }
        if (defineIndex > 1) {
            writer.write(compileID, defineBase + ".enabled", "1");
        }
        if (undefineIndex > 1) {
            writer.write(compileID, "option.U.enabled", "1");
        }
    }

    /**
     * Writes elements corresponding to link options.
     *
     * @param baseDir    String base directory
     * @param writer     PropertyWriter property writer
     * @param linkTarget TargetInfo link target
     * @throws SAXException if I/O error or illegal content
     */
    private void
    writeLinkOptions(final String baseDir,
                     final PropertyWriter writer,
                     final TargetInfo linkTarget) throws SAXException {
        if (linkTarget != null) {
            ProcessorConfiguration config = linkTarget.getConfiguration();
            if (config instanceof CommandLineLinkerConfiguration) {
                CommandLineLinkerConfiguration linkConfig =
                        (CommandLineLinkerConfiguration) config;

                if (linkConfig.getLinker() instanceof BorlandLinker) {
                    String linkID = "win32.Debug_Build.win32b.ilink32";
                    writeIlinkArgs(writer, linkID, linkConfig.getPreArguments());
                    writeIlinkArgs(writer, linkID, linkConfig.getEndArguments());
                    writer.write(linkID, "param.libfiles.1", "cw32mt.lib");
                    writer.write(linkID, "param.libfiles.2", "import32.lib");
                    int libIndex = 3;
                    String[] libNames = linkConfig.getLibraryNames();
                    for (String libName : libNames) {
                        writer.write(linkID, "param.libfiles." + (libIndex++), libName);
                    }
                    String startup = linkConfig.getStartupObject();
                    if (startup != null) {
                        writer.write(linkID, "param.objfiles.1", startup);
                    }
                } else {
                    String linkID = "linux.Debug_Build.gnuc++.g++link";
                    writeLdArgs(writer, linkID, linkConfig.getPreArguments());
                    writeLdArgs(writer, linkID, linkConfig.getEndArguments());
                }
            }
        }
    }

    /**
     * Writes ld linker options to project file.
     *
     * @param writer  PropertyWriter property writer
     * @param linkID  String linker identifier
     * @param preArgs String[] linker arguments
     * @throws SAXException thrown if unable to write option
     */
    private void writeLdArgs(final PropertyWriter writer,
                             final String linkID,
                             final String[] preArgs) throws SAXException {
        int objnameIndex = 1;
        int libnameIndex = 1;
        int libpathIndex = 1;
        for (String preArg : preArgs) {
            if (preArg.startsWith("-o")) {
                writer.write(linkID, "option.o.arg." + (objnameIndex++), preArg.substring(2));
            } else if (preArg.startsWith("-l")) {
                writer.write(linkID, "option.l.arg." + (libnameIndex++), preArg.substring(2));
            } else if (preArg.startsWith("-L")) {
                writer.write(linkID, "option.L.arg." + (libpathIndex++), preArg.substring(2));
            } else {
                //
                //  any others
                //
                writer.write(linkID, "option." + preArg.substring(1) + ".enabled", "1");
            }
        }
        if (objnameIndex > 1) {
            writer.write(linkID, "option.o.enabled", "1");
        }
        if (libnameIndex > 1) {
            writer.write(linkID, "option.l.enabled", "1");
        }
        if (libpathIndex > 1) {
            writer.write(linkID, "option.L.enabled", "1");
        }
    }

    /**
     * Writes ilink32 linker options to project file.
     *
     * @param writer PropertyWriter property writer
     * @param linkID String linker identifier
     * @param args   String[] linker arguments
     * @throws SAXException thrown if unable to write option
     */
    private void writeIlinkArgs(final PropertyWriter writer,
                                final String linkID,
                                final String[] args) throws SAXException {
        for (String arg : args) {
            if (arg.charAt(0) == '/' || arg.charAt(0) == '-') {
                int equalsPos = arg.indexOf('=');
                if (equalsPos > 0) {
                    String option = "option." + arg.substring(0, equalsPos - 1);
                    writer.write(linkID, option + ".enabled", "1");
                    writer.write(linkID, option + ".value", arg.substring(equalsPos + 1));
                } else {
                    writer.write(linkID, "option." + arg.substring(1) + ".enabled", "1");
                }
            }
        }
    }

}
