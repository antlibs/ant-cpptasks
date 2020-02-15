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
import net.sf.antcontrib.cpptasks.gcc.GccCompatibleCCompiler;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.util.Vector;

import static net.sf.antcontrib.cpptasks.CUtil.getExecutableLocation;

/**
 * Adapter for the HP compiler
 *
 * @author Curt Arnold
 */
public final class HPCompiler extends GccCompatibleCCompiler {

    private String identifier;
    private File[] includePath;
    private static final HPCompiler instance = new HPCompiler("cc", false, null);

    /**
     * Private constructor.  Use GccCCompiler.getInstance() to get
     * singleton instance of this class.
     *
     * @param command String
     * @param newEnvironment boolean
     * @param env Environment
     */
    private HPCompiler(String command, boolean newEnvironment, Environment env) {
        super(command, "-help", false, null, newEnvironment, env);
    }

    public int getMaximumCommandLength() {
        return Integer.MAX_VALUE;
    }

    /**
     * Gets singleton instance of this class
     *
     * @return HPCompiler
     */
    public static HPCompiler getInstance() {
        return instance;
    }

    public File[] getEnvironmentIncludePath() {
        if (includePath == null) {
            File ccLoc = getExecutableLocation("cc");
            if (ccLoc != null) {
                File compilerIncludeDir = new File(new File(ccLoc, "../include").getAbsolutePath());
                if (compilerIncludeDir.exists()) {
                    includePath = new File[2];
                    includePath[0] = compilerIncludeDir;
                }
            }
            if (includePath == null) {
                includePath = new File[1];
            }
            includePath[includePath.length - 1] = new File("/usr/include");
        }
        return includePath;
    }

    public void addImpliedArgs(Vector<String> args,
                               boolean debug,
                               boolean multithreaded,
                               boolean exceptions,
                               LinkType linkType) {
        args.addElement("-c");
        if (debug) {
            args.addElement("-g");
        }
		/*
		if (multithreaded) {
		  args.addElement("-mt");
		}
		*/
        if (linkType.isSharedLibrary()) {
            args.addElement("+z");
        }
    }

    public void addWarningSwitch(Vector<String> args, int level) {
        switch (level) {
            case 0:
                args.addElement("-w");
                break;
            case 1:
            case 2:
                args.addElement("+w");
                break;
				/*
				        case 3:
				        case 4:
				        case 5:
				        args.addElement("+w2");
				        break;
				*/
        }
    }

    public Linker getLinker(LinkType linkType) {
        return HPLinker.getInstance().getLinker(linkType);
    }
}
