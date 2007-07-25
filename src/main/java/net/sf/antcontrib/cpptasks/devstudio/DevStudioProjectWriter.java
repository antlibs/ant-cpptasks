/*
 *
 * Copyright 2004 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.devstudio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.TargetInfo;
import net.sf.antcontrib.cpptasks.compiler.CommandLineCompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinkerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.ProcessorConfiguration;
import net.sf.antcontrib.cpptasks.ide.ProjectDef;
import net.sf.antcontrib.cpptasks.ide.ProjectWriter;
import org.apache.tools.ant.BuildException;

/**
 * Writes a Microsoft Visual Studio 97 or Visual Studio 6 project file.
 *
 * Status: Collects file list but does not pick
 * up libraries and settings from project.
 *
 * @author curta
 */
public final class DevStudioProjectWriter
    implements ProjectWriter {
  /**
   * Visual Studio version.
   */
  private String version;

  /**
   * Constructor.
   * @param versionArg String Visual Studio version.
   */
  public DevStudioProjectWriter(final String versionArg) {
    this.version = versionArg;
  }

  /**
   *  Writes a project definition file.
   * @param fileName File name base, writer may append appropriate extension
   * @param task cc task for which to write project
   * @param projectDef project element
   * @param files source files
   * @param targets compilation targets
   * @param linkTarget link target
   * @throws IOException if error writing project file
   */
  public void writeProject(final File fileName,
                           final CCTask task,
                           final ProjectDef projectDef,
                           final List files,
                           final Hashtable targets,
                           final TargetInfo linkTarget) throws IOException {

    //
    //    some characters are apparently not allowed in VS project names
    //       but have not been able to find them documented
    //       limiting characters to alphas, numerics and hyphens
    StringBuffer projectNameBuf;
    String projectName = projectDef.getName();
    if (projectName != null) {
      projectNameBuf = new StringBuffer(projectName);
    } else {
      projectNameBuf = new StringBuffer(fileName.getName());
    }
    for (int i = 0; i < projectNameBuf.length(); i++) {
      final char ch = projectNameBuf.charAt(i);
      if (!((ch >= 'a' && ch <= 'z')
             || (ch >= 'A' && ch <= 'Z')
             || (ch >= '0' && ch <= '9'))) {
        projectNameBuf.setCharAt(i, '_');
      }
    }
    projectName = projectNameBuf.toString();

    final String basePath = fileName.getAbsoluteFile().getParent();

    File dspFile = new File(fileName + ".dsp");
    if (!projectDef.getOverwrite() && dspFile.exists()) {
      throw new BuildException("Not allowed to overwrite project file "
                               + dspFile.toString());
    }
    File dswFile = new File(fileName + ".dsw");
    if (!projectDef.getOverwrite() && dswFile.exists()) {
        throw new BuildException("Not allowed to overwrite project file "
                                 + dswFile.toString());
      }

    CommandLineCompilerConfiguration compilerConfig =
        getBaseCompilerConfiguration(targets);
    if (compilerConfig == null) {
      throw new BuildException(
          "Unable to generate Visual Studio project "
          + "when Microsoft C++ is not used.");
    }

    Writer writer = new BufferedWriter(new FileWriter(dspFile));
    writer.write("# Microsoft Developer Studio Project File - Name=\"");
    writer.write(projectName);
    writer.write("\" - Package Owner=<4>\r\n");
    writer.write(
        "# Microsoft Developer Studio Generated Build File, Format Version ");
    writer.write(this.version);
    writer.write("\r\n");
    writer.write("# ** DO NOT EDIT **\r\n\r\n");
    String outputType = task.getOuttype();
    String subsystem = task.getSubsystem();
    String configName = projectName;
    final boolean isDebug = task.getDebug();
    if (isDebug) {
      configName += " - Win32 Debug";
    } else {
      configName += " - Win32 Release";
    }
    String targtype = "Win32 (x86) Dynamic-Link Library";
    String targid = "0x0102";
    if ("executable".equals(outputType)) {
      if ("console".equals(subsystem)) {
        targtype = "Win32 (x86) Console Application";
        targid = "0x0103";
      } else {
        targtype = "Win32 (x86) Application";
        targid = "0x0101";
      }
    } else if ("static".equals(outputType)) {
      targtype = "Win32 (x86) Static Library";
      targid = "0x0104";
    }
    writer.write("# TARGTYPE \"");
    writer.write(targtype);
    writer.write("\" ");
    writer.write(targid);
    writer.write("\r\n\r\nCFG=");
    writer.write(configName);
    writer.write("\r\n");

    writeMessage(writer, projectName, configName, targtype);

    writer.write("# Begin Project\r\n");
    if (version.equals("6.00")) {
      writer.write("# PROP AllowPerConfigDependencies 0\r\n");
    }
    writer.write("# PROP Scc_ProjName \"\"\r\n");
    writer.write("# PROP Scc_LocalPath \"\"\r\n");
    writer.write("CPP=cl.exe\r\n");
    writer.write("MTL=midl.exe\r\n");
    writer.write("RSC=rc.exe\r\n");
    writer.write("# PROP BASE Use_MFC 0\r\n");

    writer.write("# PROP BASE Use_Debug_Libraries ");
    if (isDebug) {
      writer.write("1\r\n");
    } else {
      writer.write("0\r\n");
    }

    File objDir = task.getObjdir();
    String objDirPath = CUtil.getRelativePath(basePath, objDir);

    File outFile = task.getOutfile();
    File buildDir = outFile.getParentFile();
    String buildDirPath = CUtil.getRelativePath(basePath, buildDir);

    writer.write("# PROP BASE Output_Dir \"");
    writer.write(buildDirPath);
    writer.write("\"\r\n");
    writer.write("# PROP BASE Intermediate_Dir \"");
    writer.write(objDirPath);
    writer.write("\"\r\n");
    writer.write("# PROP BASE Target_Dir \"\"\r\n");
    writer.write("# PROP Use_MFC 0\r\n");
    writer.write("# PROP Use_Debug_Libraries ");
    if (isDebug) {
      writer.write("1\r\n");
    } else {
      writer.write("0\r\n");
    }
    writer.write("# PROP Output_Dir \"");
    writer.write(buildDirPath);
    writer.write("\"\r\n");
    writer.write("# PROP Intermediate_Dir \"");
    writer.write(objDirPath);
    writer.write("\"\r\n");
    writer.write("# PROP Target_Dir \"\"\r\n");
    writeCompileOptions(writer, basePath, compilerConfig);
    writer.write(
        "# ADD BASE MTL /nologo /D \"_DEBUG\" /mktyplib203 /o NUL /win32\r\n");
    writer.write(
        "# ADD MTL /nologo /D \"_DEBUG\" /mktyplib203 /o NUL /win32\r\n");
    writer.write("# ADD BASE RSC /l 0x409 /d \"_DEBUG\"\r\n");
    writer.write("# ADD RSC /l 0x409 /d \"_DEBUG\"\r\n");
    writer.write("BSC32=bscmake.exe\r\n");
    writer.write("# ADD BASE BSC32 /nologo\r\n");
    writer.write("# ADD BSC32 /nologo\r\n");
    writer.write("LINK32=link.exe\r\n");
    writeLinkOptions(writer, basePath, linkTarget, targets);
    writer.write("# Begin Target\r\n\r\n");
    writer.write("# Name \"");
    writer.write(configName);
    writer.write("\"\r\n");

    File[] sortedSources = getSources(files);

    if (version.equals("6.00")) {
      final String sourceFilter = "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat";
      final String headerFilter = "h;hpp;hxx;hm;inl";
      final String resourceFilter =
          "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe";

      writer.write("# Begin Group \"Source Files\"\r\n\r\n");
      writer.write("# PROP Default_Filter \"" + sourceFilter + "\"\r\n");

      for (int i = 0; i < sortedSources.length; i++) {
        if (!isGroupMember(headerFilter, sortedSources[i])
            && !isGroupMember(resourceFilter, sortedSources[i])) {
          writeSource(writer, basePath, sortedSources[i]);
        }
      }
      writer.write("# End Group\r\n");

      writer.write("# Begin Group \"Header Files\"\r\n\r\n");
      writer.write("# PROP Default_Filter \"" + headerFilter + "\"\r\n");

      for (int i = 0; i < sortedSources.length; i++) {
        if (isGroupMember(headerFilter, sortedSources[i])) {
          writeSource(writer, basePath, sortedSources[i]);
        }
      }
      writer.write("# End Group\r\n");

      writer.write("# Begin Group \"Resource Files\"\r\n\r\n");
      writer.write("# PROP Default_Filter \"" + resourceFilter + "\"\r\n");

      for (int i = 0; i < sortedSources.length; i++) {
        if (isGroupMember(resourceFilter, sortedSources[i])) {
          writeSource(writer, basePath, sortedSources[i]);
        }
      }
      writer.write("# End Group\r\n");

    } else {
      for (int i = 0; i < sortedSources.length; i++) {
        writeSource(writer, basePath, sortedSources[i]);
      }
    }

    writer.write("# End Target\r\n");
    writer.write("# End Project\r\n");
    writer.close();

    //
    //    write workspace file
    //
    writer = new BufferedWriter(new FileWriter(dswFile));

    writer.write("Microsoft Developer Studio Workspace File, Format Version ");
    writer.write(version);
    writer.write("\r\n");
    writer.write("# WARNING: DO NOT EDIT OR DELETE");
    writer.write(" THIS WORKSPACE FILE!\r\n\r\n");

    writer.write("############################################");
    writer.write("###################################\r\n\r\n");
    writer.write("Project: \"" + projectName + "\"=.\\"
                 + dspFile.getName()
                 + " - Package Owner=<4>\r\n\r\n");

    writer.write("Package=<5>\r\n{{{\r\n}}}\r\n\r\n");
    writer.write("Package=<4>\r\n{{{\r\n}}}\r\n\r\n");
    writer.write("######################################");
    writer.write("#########################################\r\n\r\n");

    writer.write("Global:\r\n\r\nPackage=<5>\r\n{{{\r\n}}}");
    writer.write("\r\n\r\nPackage=<3>\r\n{{{\r\n}}}\r\n\r\n");

    writer.write("########################################");
    writer.write("#######################################\r\n\r\n");
    writer.close();

  }

  /**
   * Returns true if the file has an extension that appears in the group filter.
   * @param filter String group filter
   * @param candidate File file
   * @return boolean true if member of group
   */
  private boolean isGroupMember(final String filter, final File candidate) {
    String fileName = candidate.getName();
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot >= 0 && lastDot < fileName.length() - 1) {
      String extension =
           ";" + fileName.substring(lastDot + 1).toLowerCase() + ";";
      String semiFilter = ";" + filter + ";";
      return semiFilter.indexOf(extension) >= 0;
    }
    return false;
  }

  /**
   * Writes the entry for one source file in the project.
   * @param writer Writer writer
   * @param basePath String base path for project
   * @param groupMember File project source file
   * @throws IOException if error writing project file
   */
  private void writeSource(final Writer writer,
                           final String basePath,
                           final File groupMember)
      throws IOException {
    writer.write("# Begin Source File\r\n\r\nSOURCE=");
    String relativePath = CUtil.getRelativePath(basePath,
                                                groupMember);
    //
    //  if relative path is just a name (hello.c) then
    //    make it .\hello.c
    if (!relativePath.startsWith(".")
        && relativePath.indexOf(":") < 0
        && !relativePath.startsWith("\\")) {
      relativePath = ".\\" + relativePath;
    }
    writer.write(relativePath);
    writer.write("\r\n# End Source File\r\n");
  }

  /**
   * Get alphabetized array of source files.
   * @param sourceList list of source files
   * @return File[] source files
   */
  private File[] getSources(final List sourceList) {
    File[] sortedSources = new File[sourceList.size()];
    sourceList.toArray(sortedSources);
    Arrays.sort(sortedSources, new Comparator() {
      public int compare(final Object o1, final Object o2) {
        return ((File) o1).getName().compareTo(((File) o2).getName());
      }
    });
    return sortedSources;
  }

  /**
   * Writes "This is not a makefile" warning.
   * @param writer Writer writer
   * @param projectName String project name
   * @param configName String configuration name
   * @param targtype String target type
   * @throws IOException if error writing project
   */

  private void writeMessage(final Writer writer,
                            final String projectName,
                            final String configName,
                            final String targtype) throws IOException {
    writer.write(
        "!MESSAGE This is not a valid makefile. ");
    writer.write("To build this project using NMAKE,\r\n");
    writer.write("!MESSAGE use the Export Makefile command and run\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write("!MESSAGE NMAKE /f \"");
    writer.write(projectName);
    writer.write(".mak\".\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write(
        "!MESSAGE You can specify a configuration when running NMAKE\r\n");
    writer.write(
        "!MESSAGE by defining the macro CFG on the command line. ");
    writer.write("For example:\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write("!MESSAGE NMAKE /f \"");
    writer.write(projectName);
    writer.write(".mak\" CFG=\"");
    writer.write(configName);
    writer.write("\"\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write("!MESSAGE Possible choices for configuration are:\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write("!MESSAGE \"");
    writer.write(configName);
    writer.write("\" (based on \"");
    writer.write(targtype);
    writer.write("\")\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write("\r\n");

  }

  /**
   * Gets the first recognized compiler from the
   * compilation targets.
   * @param targets compilation targets
   * @return representative (hopefully) compiler configuration
   */
  private CommandLineCompilerConfiguration
      getBaseCompilerConfiguration(final Hashtable targets) {
    //
    //   find first target with an DevStudio C compilation
    //
    CommandLineCompilerConfiguration compilerConfig = null;
    //
    //   get the first target and assume that it is representative
    //
    Iterator targetIter = targets.values().iterator();
    while (targetIter.hasNext()) {
      TargetInfo targetInfo = (TargetInfo) targetIter.next();
      ProcessorConfiguration config = targetInfo.getConfiguration();
      String identifier = config.getIdentifier();
      //
      //   for the first cl compiler
      //
      if (config instanceof CommandLineCompilerConfiguration) {
        compilerConfig = (CommandLineCompilerConfiguration) config;
        if (compilerConfig.getCompiler() instanceof DevStudioCCompiler) {
          return compilerConfig;
        }
      }
    }
    return null;
  }

  /**
   * Writes compiler options.
   * @param writer Writer writer
   * @param baseDir String base directory
   * @param compilerConfig compiler configuration
   * @throws IOException if error on writing project
   */
  private void writeCompileOptions(final Writer writer,
                                   final String baseDir,
                                   final CommandLineCompilerConfiguration
                                   compilerConfig) throws IOException {
    StringBuffer baseOptions = new StringBuffer(50);
    baseOptions.append("# ADD BASE CPP");
    StringBuffer options = new StringBuffer(50);
    options.append("# ADD CPP");
    File[] includePath = compilerConfig.getIncludePath();
    for (int i = 0; i < includePath.length; i++) {
      options.append(" /I \"");
      String relPath = CUtil.getRelativePath(baseDir, includePath[i]);
      options.append(relPath);
      options.append('"');
    }

    String[] preArgs = compilerConfig.getPreArguments();
    for (int i = 0; i < preArgs.length; i++) {
      if (preArgs[i].startsWith("/D")) {
        options.append(" /D ");
        baseOptions.append(" /D ");
        String body = preArgs[i].substring(2);
        if (preArgs[i].indexOf('=') >= 0) {
          options.append(body);
          baseOptions.append(body);
        } else {
          options.append('"');
          options.append(body);
          options.append('"');
        }
      } else if (!preArgs[i].startsWith("/I")) {
        options.append(" ");
        options.append(preArgs[i]);
        baseOptions.append(" ");
        baseOptions.append(preArgs[i]);
      }
    }
    baseOptions.append("\r\n");
    options.append("\r\n");
    writer.write(baseOptions.toString());
    writer.write(options.toString());

  }

  /**
   * Writes link options.
   * @param writer Writer writer
   * @param basePath String base path
   * @param linkTarget TargetInfo link target
   * @param targets Hashtable all targets
   * @throws IOException if unable to write to project file
   */
  private void writeLinkOptions(final Writer writer,
                                final String basePath,
                                final TargetInfo linkTarget,
                                final Hashtable targets) throws IOException {

    StringBuffer baseOptions = new StringBuffer(100);
    StringBuffer options = new StringBuffer(100);
    baseOptions.append("# ADD BASE LINK32");
    options.append("# ADD LINK32");

    ProcessorConfiguration config = linkTarget.getConfiguration();
    if (config instanceof CommandLineLinkerConfiguration) {
      CommandLineLinkerConfiguration linkConfig =
          (CommandLineLinkerConfiguration) config;

      File[] linkSources = linkTarget.getAllSources();
      for (int i = 0; i < linkSources.length; i++) {
        //
        //   if file was not compiled or otherwise generated
        //
        if (targets.get(linkSources[i].getName()) == null) {
          String relPath = CUtil.getRelativePath(basePath, linkSources[i]);
          //
          //   if path has an embedded space then
          //      must quote
          if (relPath.indexOf(' ') > 0) {
            options.append(" \"");
            options.append(relPath);
            options.append("\"");
          } else {
            options.append(' ');
            options.append(relPath);
          }
        }
      }
      String[] preArgs = linkConfig.getPreArguments();
      for (int i = 0; i < preArgs.length; i++) {
        options.append(' ');
        options.append(preArgs[i]);
        baseOptions.append(' ');
        baseOptions.append(preArgs[i]);
      }
      String[] endArgs = linkConfig.getEndArguments();
      for (int i = 0; i < endArgs.length; i++) {
        options.append(' ');
        options.append(endArgs[i]);
        baseOptions.append(' ');
        baseOptions.append(endArgs[i]);
      }
    }
    baseOptions.append("\r\n");
    options.append("\r\n");
    writer.write(baseOptions.toString());
    writer.write(options.toString());
  }
}
