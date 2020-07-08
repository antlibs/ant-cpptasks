/*
 *
 * Copyright 2003-2004 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.gcc.cross;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.LinkerParam;
import net.sf.antcontrib.cpptasks.compiler.CaptureStreamHandler;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinkerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.gcc.AbstractLdLinker;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.util.Vector;

import static net.sf.antcontrib.cpptasks.CUtil.checkDirectoryArray;

/**
 * Adapter for the g++ variant of the GCC linker
 *
 * @author Stephen M. Webb {@literal <stephen.webb@bregmasoft.com>}
 */
public class GppLinker extends AbstractLdLinker {
    protected static final String[] discardFiles = new String[0];
    protected static final String[] objFiles = new String[]{".o", ".a", ".lib",
            ".dll", ".so", ".sl"};
    private static final GppLinker dllLinker = new GppLinker("gcc", objFiles,
            discardFiles, "lib", ".so", false, new GppLinker("gcc", objFiles,
            discardFiles, "lib", ".so", true, null));
    private final static String libPrefix = "libraries: =";
    protected static final String[] libtoolObjFiles = new String[]{".fo", ".a",
            ".lib", ".dll", ".so", ".sl"};
    private static String[] linkerOptions = new String[]{"-bundle", "-dylib",
            "-dynamic", "-dynamiclib", "-nostartfiles", "-nostdlib",
            "-prebind", "-s", "-static", "-shared", "-symbolic", "-Xlinker"};
    private static final GppLinker instance = new GppLinker("gcc", objFiles,
            discardFiles, "", "", false, null);
    private static final GppLinker machDllLinker = new GppLinker("gcc",
            objFiles, discardFiles, "lib", ".dylib", false, null);
    private static final GppLinker machPluginLinker = new GppLinker("gcc",
            objFiles, discardFiles, "lib", ".bundle", false, null);

    public static GppLinker getInstance() {
        return instance;
    }

    private File[] libDirs;
    private String runtimeLibrary;

    protected GppLinker(String command, String[] extensions,
                        String[] ignoredExtensions, String outputPrefix,
                        String outputSuffix, boolean isLibtool, GppLinker libtoolLinker) {
        super(command, "-dumpversion", extensions, ignoredExtensions,
                outputPrefix, outputSuffix, isLibtool, libtoolLinker);
    }

    protected void addImpliedArgs(boolean debug, LinkType linkType, Vector<String> args) {
        super.addImpliedArgs(debug, linkType, args);
        if (getIdentifier().contains("mingw")) {
            if (linkType.isSubsystemConsole()) {
                args.addElement("-mconsole");
            }
            if (linkType.isSubsystemGUI()) {
                args.addElement("-mwindows");
            }
        }
        if (linkType.isStaticRuntime()) {
            String[] cmdin = new String[]{"g++", "-print-file-name=libstdc++.a"};
            String[] cmdout = CaptureStreamHandler.run(cmdin);
            if (cmdout.length > 0) {
                runtimeLibrary = cmdout[0];
            } else {
                runtimeLibrary = null;
            }
        } else {
            runtimeLibrary = "-lstdc++";
        }
    }

    public String[] addLibrarySets(CCTask task, LibrarySet[] libsets,
                                   Vector<String> preargs, Vector<String> midargs, Vector<String> endargs) {
        String[] rs = super.addLibrarySets(task, libsets, preargs, midargs,
                endargs);
        if (runtimeLibrary != null) {
            endargs.addElement(runtimeLibrary);
        }
        return rs;
    }

    protected Object clone() throws CloneNotSupportedException {
        GppLinker clone = (GppLinker) super.clone();
        return clone;
    }

    /**
     * Allows derived linker to decorate linker option. Override by GppLinker to
     * prepend a "-Wl," to pass option to through gcc to linker.
     *
     * @param buf buffer that may be used and abused in the decoration process,
     *            must not be null.
     * @param arg linker argument
     */
    public String decorateLinkerOption(StringBuilder buf, String arg) {
        String decoratedArg = arg;
        if (arg.length() > 1 && arg.charAt(0) == '-') {
            switch (arg.charAt(1)) {
                //
                //   passed automatically by GCC
                //
                case 'g':
                case 'f':
                case 'F':
                    /* Darwin */
                case 'm':
                case 'O':
                case 'W':
                case 'l':
                case 'L':
                case 'u':
                    break;
                default:
                    boolean known = false;
                    for (String linkerOption : linkerOptions) {
                        if (linkerOption.equals(arg)) {
                            known = true;
                            break;
                        }
                    }
                    if (!known) {
                        buf.setLength(0);
                        buf.append("-Wl,");
                        buf.append(arg);
                        decoratedArg = buf.toString();
                    }
                    break;
            }
        }
        return decoratedArg;
    }

    /**
     * Returns library path.
     */
    public File[] getLibraryPath() {
        if (libDirs == null) {
            Vector<String> dirs = new Vector<String>();
            // Ask GCC where it will look for its libraries.
            String[] args = new String[]{"g++", "-print-search-dirs"};
            String[] cmdout = CaptureStreamHandler.run(args);
            for (int i = 0; i < cmdout.length; ++i) {
                int prefixIndex = cmdout[i].indexOf(libPrefix);
                if (prefixIndex >= 0) {
                    // Special case DOS-type GCCs like MinGW or Cygwin
                    int s = prefixIndex + libPrefix.length();
                    int t = cmdout[i].indexOf(';', s);
                    while (t > 0) {
                        dirs.addElement(cmdout[i].substring(s, t));
                        s = t + 1;
                        t = cmdout[i].indexOf(';', s);
                    }
                    dirs.addElement(cmdout[i].substring(s));
                    ++i;
                    for (; i < cmdout.length; ++i) {
                        dirs.addElement(cmdout[i]);
                    }
                }
            }
            // Eliminate all but actual directories.
            String[] libpath = dirs.toArray(new String[0]);
            int count = checkDirectoryArray(libpath);
            // Build return array.
            libDirs = new File[count];
            int index = 0;
            for (String s : libpath) {
                if (s != null) {
                    libDirs[index++] = new File(s);
                }
            }
        }
        return libDirs;
    }

    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return GccLibrarian.getInstance();
        }
        if (type.isPluginModule()) {
            if (GccProcessor.getMachine().contains("darwin")) {
                return machPluginLinker;
            } else {
                return dllLinker;
            }
        }
        if (type.isSharedLibrary()) {
            if (GccProcessor.getMachine().contains("darwin")) {
                return machDllLinker;
            } else {
                return dllLinker;
            }
        }
        return instance;
    }

    public void link(CCTask task, File outputFile, String[] sourceFiles,
                     CommandLineLinkerConfiguration config) throws BuildException {
        try {
            GppLinker clone = (GppLinker) this.clone();
            LinkerParam param = config.getParam("target");
            if (param != null) {
                clone.setCommand(param.getValue() + "-" + this.getCommand());
            }
            clone.superlink(task, outputFile, sourceFiles, config);
        } catch (CloneNotSupportedException e) {
            superlink(task, outputFile, sourceFiles, config);
        }
    }

    private void superlink(CCTask task, File outputFile, String[] sourceFiles,
                           CommandLineLinkerConfiguration config) throws BuildException {
        super.link(task, outputFile, sourceFiles, config);
    }
}
