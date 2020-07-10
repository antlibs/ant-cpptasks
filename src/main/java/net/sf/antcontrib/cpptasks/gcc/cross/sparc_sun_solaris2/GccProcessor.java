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
package net.sf.antcontrib.cpptasks.gcc.cross.sparc_sun_solaris2;

import net.sf.antcontrib.cpptasks.compiler.CaptureStreamHandler;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static net.sf.antcontrib.cpptasks.CUtil.getExecutableLocation;

/**
 * A add-in class for Gcc processors
 */
public class GccProcessor {
    //   the results from gcc -dumpmachine
    private static String machine;
    private static String[] specs;
    //   the results from gcc -dumpversion
    private static String version;

    private static int addLibraryPatterns(String[] libnames, StringBuilder buf,
                                          String prefix, String extension, String[] patterns, int offset) {
        for (int i = 0; i < libnames.length; i++) {
            buf.setLength(0);
            buf.append(prefix);
            buf.append(libnames[i]);
            buf.append(extension);
            patterns[offset + i] = buf.toString();
        }
        return offset + libnames.length;
    }

    /**
     * Converts absolute Cygwin file or directory names to the corresponding
     * Win32 name.
     *
     * @param names array of names, some elements may be null, will be changed in
     *              place.
     */
    public static void convertCygwinFilenames(String[] names) {
        if (names == null) {
            throw new NullPointerException("names");
        }
        File gccDir = getExecutableLocation(GccCCompiler.CMD_PREFIX
                + "gcc.exe");
        if (gccDir != null) {
            String prefix = gccDir.getAbsolutePath() + "/..";
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                if (name != null && name.length() > 1 && name.charAt(0) == '/') {
                    buf.setLength(0);
                    buf.append(prefix);
                    buf.append(name);
                    names[i] = buf.toString();
                }
            }
        }
    }

    public static String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        StringBuilder buf = new StringBuilder();
        String[] patterns = new String[libnames.length * 2];
        int offset = addLibraryPatterns(libnames, buf, "lib", ".a", patterns, 0);
        if (isHPUX()) {
            offset = addLibraryPatterns(libnames, buf, "lib", ".sl", patterns,
                    offset);
        } else {
            offset = addLibraryPatterns(libnames, buf, "lib", ".so", patterns,
                    offset);
        }
        return patterns;
    }

    public static String getMachine() {
        if (machine == null) {
            String[] args = new String[]{GccCCompiler.CMD_PREFIX + "gcc",
                    "-dumpmachine"};
            String[] cmdout = CaptureStreamHandler.run(args);
            if (cmdout.length == 0) {
                machine = "nomachine";
            } else {
                machine = cmdout[0];
            }
        }
        return machine;
    }

    public static String[] getOutputFileSwitch(String letter, String outputFile) {
        StringBuilder buf = new StringBuilder();
        if (outputFile.contains(" ")) {
            buf.append('"');
            buf.append(outputFile.replace('\\', '/'));
            buf.append('"');
        } else {
            buf.append(outputFile.replace('\\', '/'));
        }
        return new String[]{letter, buf.toString()};
    }

    /**
     * <p>
     * Returns the contents of the gcc specs file.
     * </p>
     * <p>
     * The implementation locates gcc.exe in the executable path and then
     * builds a relative path name from the results of -dumpmachine and
     * -dumpversion. Attempts to use gcc -dumpspecs to provide this information
     * resulted in stalling on the Execute.run
     * </p>
     *
     * @return contents of the specs file
     */
    public static String[] getSpecs() {
        if (specs == null) {
            File gccParent = getExecutableLocation(GccCCompiler.CMD_PREFIX + "gcc.exe");
            if (gccParent != null) {
                //
                //  build a relative path like
                //    ../lib/gcc-lib/i686-pc-cygwin/2.95.3-5/specs
                //  resolve it relative to the location of gcc.exe
                //
                File specsFile = new File(gccParent, String.format("../lib/gcc-lib/%s/%s/specs",
                        getMachine(), getVersion()));
                //
                //  found the specs file
                //
                try {
                    //
                    //  read the lines in the file
                    //
                    BufferedReader reader = new BufferedReader(new FileReader(specsFile));
                    Vector<String> lines = new Vector<String>(100);
                    String line = reader.readLine();
                    while (line != null) {
                        lines.addElement(line);
                        line = reader.readLine();
                    }
                    specs = lines.toArray(new String[0]);
                } catch (IOException ex) {
                }
            }
        }
        if (specs == null) {
            specs = new String[0];
        }
        return specs;
    }

    public static String getVersion() {
        if (version == null) {
            String[] cmdout = CaptureStreamHandler.run(new String[]{GccCCompiler.CMD_PREFIX
                    + "gcc", "-dumpversion"});
            if (cmdout.length == 0) {
                version = "noversion";
            } else {
                version = cmdout[0];
            }
        }
        return version;
    }

    public static boolean isCaseSensitive() {
        return true;
    }

    /**
     * Determines if task is running with cygwin
     *
     * @return true if cygwin was detected
     */
    public static boolean isCygwin() {
        return getMachine().indexOf("cygwin") > 0;
    }

    private static boolean isHPUX() {
        String osname = System.getProperty("os.name").toLowerCase();
        if (osname.contains("hp") && osname.contains("ux")) {
            return true;
        }
        return false;
    }

    /**
     * Parses the results of the specs file for a specific processor and
     * options
     *
     * @param specsContent     Contents of specs file as returned from getSpecs
     * @param specSectionStart start of spec section, for example "*cpp:"
     * @param options          command line switches such as "-istart"
     * @return an array of arrays of String
     */
    public static String[][] parseSpecs(String[] specsContent,
                                        String specSectionStart, String[] options) {
        if (specsContent == null) {
            throw new NullPointerException("specsContent");
        }
        if (specSectionStart == null) {
            throw new NullPointerException("specSectionStart");
        }
        if (options == null) {
            throw new NullPointerException("option");
        }
        String[][] optionValues = new String[options.length][];
        StringBuilder optionValue = new StringBuilder(40);
        for (int i = 0; i < specsContent.length; i++) {
            String specLine = specsContent[i];
            //
            //   if start of section then start paying attention
            //
            if (specLine.startsWith(specSectionStart)) {
                List<Vector<String>> optionVectors = new ArrayList<Vector<String>>();
                //
                //  go to next line and examine contents
                //     and repeat until end of file
                //
                for (i++; i < specsContent.length; i++) {
                    specLine = specsContent[i];
                    for (int j = 0; j < options.length; j++) {
                        if (optionVectors.size() < j + 1) {
                            optionVectors.add(new Vector<String>());
                        }
                        int optionStart = specLine.indexOf(options[j]);
                        while (optionStart >= 0) {
                            optionValue.setLength(0);
                            //
                            //   walk rest of line looking for first non
                            // whitespace
                            //       and then next space
                            boolean hasNonBlank = false;
                            int k = optionStart + options[j].length();
                            for (; k < specLine.length(); k++) {
                                //
                                //  either a blank or a "}" (close of
                                // conditional)
                                //    section will end the path
                                //
                                if (specLine.charAt(k) == ' '
                                        || specLine.charAt(k) == '}') {
                                    if (hasNonBlank) {
                                        break;
                                    }
                                } else {
                                    hasNonBlank = true;
                                    optionValue.append(specLine.charAt(k));
                                }
                            }
                            //
                            //  transition back to whitespace
                            //     value is over, add it to vector
                            if (hasNonBlank) {
                                optionVectors.get(j).addElement(optionValue.toString());
                            }
                            //
                            //  find next occurrence on line
                            //
                            optionStart = specLine.indexOf(options[j], k);
                        }
                    }
                }
                //
                //   copy vectors over to option arrays
                //
                for (int j = 0; j < options.length; j++) {
                    optionValues[j] = optionVectors.get(j).toArray(new String[0]);
                }
            }
        }
        //
        //   fill in any missing option values with
        //      a zero-length string array
        for (int i = 0; i < optionValues.length; i++) {
            String[] zeroLenArray = new String[0];
            if (optionValues[i] == null) {
                optionValues[i] = zeroLenArray;
            }
        }
        return optionValues;
    }

    private GccProcessor() {
    }
}
