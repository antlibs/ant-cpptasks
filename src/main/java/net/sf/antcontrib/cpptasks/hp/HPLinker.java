/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Ant-Contrib project.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package net.sf.antcontrib.cpptasks.hp;

import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.gcc.AbstractLdLinker;

import java.io.File;
import java.util.Vector;

import static net.sf.antcontrib.cpptasks.CUtil.getExecutableLocation;

/**
 * Adapter for HP linker
 *
 * @author Curt Arnold
 */
public final class HPLinker extends AbstractLdLinker {
    private static final String[] objFiles = new String[]{".o", ".a", ".lib", ".dll", ".so", ".sl"};
    private static final String[] discardFiles = new String[0];

    private static final HPLinker instance =
            new HPLinker("ld", objFiles, discardFiles, "", "");
    private static final HPLinker dllLinker =
            new HPLinker("ld", objFiles, discardFiles, "lib", ".sl");
    private static final HPLinker arLinker =
            new HPLinker("ld", objFiles, discardFiles, "", ".a");

    private File[] libDirs;

    private HPLinker(String command, String[] extensions,
                     String[] ignoredExtensions, String outputPrefix,
                     String outputSuffix) {
        super(command, "-help", extensions, ignoredExtensions,
                outputPrefix, outputSuffix, false, null);
    }

    public static HPLinker getInstance() {
        return instance;
    }

    /**
     * Returns library path.
     */
    public File[] getLibraryPath() {
        if (libDirs == null) {
            File CCloc = getExecutableLocation("ld");
            if (CCloc != null) {
                File compilerLib = new File(new File(CCloc, "../lib").getAbsolutePath());
                if (compilerLib.exists()) {
                    libDirs = new File[2];
                    libDirs[0] = compilerLib;
                }
            }
            if (libDirs == null) {
                libDirs = new File[1];
            }
        }
        libDirs[libDirs.length - 1] = new File("/usr/lib");
        return libDirs;
    }

    public void addImpliedArgs(boolean debug, LinkType linkType, Vector<String> args) {
/*      if(linkType.isStaticRuntime()) {
        args.addElement("-static");
      }
*/
        if (linkType.isSharedLibrary()) {
            args.addElement("-b");
        }
/*      
      if (linkType.isStaticLibrary()) {
        args.addElement("-Wl,-noshared");
      }
*/
    }


    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return arLinker;
        }
        if (type.isSharedLibrary()) {
            return dllLinker;
        }
        return instance;
    }

    public void addIncremental(boolean incremental, Vector<String> args) {
    /*
      if (incremental) {
        args.addElement("-xidlon");
      } else {
        args.addElement("-xidloff");
      }
     */
    }
}
