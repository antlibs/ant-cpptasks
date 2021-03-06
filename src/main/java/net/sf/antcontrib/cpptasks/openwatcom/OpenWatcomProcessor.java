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
package net.sf.antcontrib.cpptasks.openwatcom;

import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;

import java.util.Vector;

/**
 * A add-in class for OpenWatcom processors.
 */
public final class OpenWatcomProcessor {
    /**
     * Adds warning command line options.
     *
     * @param args  Vector list of options
     * @param level int value of WarningLevelEnum
     */
    public static void addWarningSwitch(final Vector<String> args, final int level) {
        switch (level) {
            case 0:
                args.addElement("/w0");
                break;
            case 2:
                break;
            case 3:
                args.addElement("/w2");
                break;
            case 4:
                args.addElement("/w3");
                break;
            case 5:
                args.addElement("/we");
                break;
            default:
                args.addElement("/w1");
                break;
        }
    }

    /**
     * Gets command line option to read from an option file.
     *
     * @param cmdFile String file name for option file
     * @return String Command line option
     */
    public static String getCommandFileSwitch(final String cmdFile) {
        StringBuilder buf = new StringBuilder("@");
        if (cmdFile.contains(" ")) {
            buf.append('\"');
            buf.append(cmdFile.replace('/', '\\'));
            buf.append('\"');
        } else {
            buf.append(cmdFile);
        }
        return buf.toString();
    }

    /**
     * Creates a command line option to define a preprocessor macro.
     *
     * @param buffer StringBuilder destination buffer
     * @param define String parameter to define
     * @param value  String value, may be null
     */
    public static void getDefineSwitch(final StringBuilder buffer,
                                       final String define,
                                       final String value) {
        buffer.append("/d");
        buffer.append(define);
        if (value != null && !value.isEmpty()) {
            buffer.append('=');
            buffer.append(value);
        }
    }

    /**
     * Create a command line option to add a directory to the include path.
     *
     * @param includeDir String directory
     * @return String command line option
     */
    public static String getIncludeDirSwitch(final String includeDir) {
        return "/i=" + includeDir.replace('/', '\\');
    }

    /**
     * Builds command line options to specify the output file names.
     *
     * @param outPath String path to output file
     * @return String[] command line options
     */
    public static String[] getOutputFileSwitch(final String outPath) {
        StringBuilder buf = new StringBuilder("/fo=");
        if (outPath.contains(" ")) {
            buf.append('\"');
            buf.append(outPath);
            buf.append('\"');
        } else {
            buf.append(outPath);
        }
        return new String[]{buf.toString()};
    }

    /**
     * Get file selectors for specified libraries.
     *
     * @param libnames library names
     * @param libType  library type
     * @return file selectors
     */
    public static String[] getLibraryPatterns(final String[] libnames,
                                              final LibraryTypeEnum libType) {
        StringBuilder buf = new StringBuilder();
        String[] patterns = new String[libnames.length];
        for (int i = 0; i < libnames.length; i++) {
            buf.setLength(0);
            buf.append(libnames[i]);
            buf.append(".lib");
            patterns[i] = buf.toString();
        }
        return patterns;
    }

    /**
     * Builds a command line option to undefine a preprocessor macro.
     *
     * @param buffer StringBuilder destination
     * @param define String macro to be undefined
     */
    public static void getUndefineSwitch(final StringBuilder buffer,
                                         final String define) {
        buffer.append("/u");
        buffer.append(define);
    }

    /**
     * Gets whether processor treatment of file names is case-sensitive.
     *
     * @return boolean true if case sensitive
     */
    public static boolean isCaseSensitive() {
        return false;
    }

    /**
     * Private constructor.
     */
    private OpenWatcomProcessor() {
    }
}
