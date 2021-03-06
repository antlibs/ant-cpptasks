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
package net.sf.antcontrib.cpptasks.compiler;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.LinkerParam;
import net.sf.antcontrib.cpptasks.ProcessorParam;
import net.sf.antcontrib.cpptasks.TargetInfo;
import net.sf.antcontrib.cpptasks.VersionInfo;
import org.apache.tools.ant.BuildException;

/**
 * A configuration for a command line linker
 *
 * @author Curt Arnold
 */
public final class CommandLineLinkerConfiguration implements LinkerConfiguration {
    private final String[][] args;
    private final String identifier;
    private final String[] libraryNames;
    private final CommandLineLinker linker;
    private final boolean map;
    private final ProcessorParam[] params;
    private final boolean rebuild;
    private final boolean debug;
    private final String startupObject;

    public CommandLineLinkerConfiguration(CommandLineLinker linker,
                                          String identifier, String[][] args, ProcessorParam[] params,
                                          boolean rebuild, boolean map, boolean debug, String[] libraryNames,
                                          String startupObject) {
        if (linker == null) {
            throw new NullPointerException("linker");
        }
        if (args == null) {
            throw new NullPointerException("args");
        } else {
            this.args = args.clone();
        }
        this.linker = linker;
        this.params = params.clone();
        this.rebuild = rebuild;
        this.identifier = identifier;
        this.map = map;
        this.debug = debug;
        if (libraryNames == null) {
            this.libraryNames = new String[0];
        } else {
            this.libraryNames = libraryNames.clone();
        }
        this.startupObject = startupObject;
    }

    public int bid(String filename) {
        return linker.bid(filename);
    }

    public String[] getEndArguments() {
        return args[1].clone();
    }

    /**
     * Returns a string representation of this configuration. Should be
     * canonical so that equivalent configurations will have equivalent string
     * representations
     */
    public String getIdentifier() {
        return identifier;
    }

    public String[] getLibraryNames() {
        return libraryNames.clone();
    }

    public boolean getMap() {
        return map;
    }

    public String[] getOutputFileNames(String inputFile, VersionInfo versionInfo) {
        return linker.getOutputFileNames(inputFile, versionInfo);
    }

    public LinkerParam getParam(String name) {
        for (ProcessorParam param : params) {
            if (name.equals(param.getName())) {
                return (LinkerParam) param;
            }
        }
        return null;
    }

    public ProcessorParam[] getParams() {
        return params;
    }

    public String[] getPreArguments() {
        return args[0].clone();
    }

    public boolean getRebuild() {
        return rebuild;
    }

    public String getStartupObject() {
        return startupObject;
    }

    public void link(CCTask task, TargetInfo linkTarget) throws BuildException {
        //
        //  AllSourcePath's include any syslibsets
        //
        String[] sourcePaths = linkTarget.getAllSourcePaths();
        linker.link(task, linkTarget.getOutput(), sourcePaths, this);
    }

    public String toString() {
        return identifier;
    }

    public Linker getLinker() {
        return linker;
    }

    public boolean isDebug() {
        return debug;
    }
}
