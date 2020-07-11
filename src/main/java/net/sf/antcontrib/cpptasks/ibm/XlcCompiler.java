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
 *        Ant-Contrib project (http://sourceforge.net/projects/ant-contrib)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "Ant-Contrib"
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Ant-Contrib"
 *    nor may "Ant-Contrib" appear in their names without prior written
 *    permission of the Ant-Contrib project.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE ANT-CONTRIB PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */
package net.sf.antcontrib.cpptasks.ibm;

import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.gcc.GccCompatibleCCompiler;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.util.Vector;


/**
 * Adapter for the IBM(r) Visual Age(tm) C++ compiler for AIX(tm)
 *
 * @author Curt Arnold
 */
public final class XlcCompiler extends GccCompatibleCCompiler {
    private String identifier;
    private File[] includePath;
    private static final XlcCompiler instance = new XlcCompiler("xlc_r", false, null);

    /**
     * Private constructor.  Use getInstance() to get
     * singleton instance of this class.
     *
     * @param command String
     * @param newEnvironment boolean
     * @param env Environment
     */
    private XlcCompiler(String command, boolean newEnvironment, Environment env) {
        super(command, "-qversion", false, null, newEnvironment, env);
    }

    public int getMaximumCommandLength() {
        return Integer.MAX_VALUE;
    }

    /**
     * Gets singleton instance of this class
     *
     * @return XlcCompiler
     */
    public static XlcCompiler getInstance() {
        return instance;
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
        if (linkType.isSharedLibrary()) {
            args.addElement("-fpic");
            args.addElement("-qmkshrobj");

        }
    }

    public void addWarningSwitch(Vector<String> args, int level) {
        switch (level) {
            case 0:
                args.addElement("-w");
                break;
            case 1:
                args.addElement("-qflag=s:s");
                break;
            case 2:
                args.addElement("-qflag=e:e");
                break;
            case 3:
                args.addElement("-qflag=w:w");
                break;
            case 4:
                args.addElement("-qflag=i:i");
                break;
            case 5:
                args.addElement("-qhalt=w:w");
                break;
        }
    }

    public Linker getLinker(LinkType linkType) {
        return VisualAgeLinker.getInstance().getLinker(linkType);
    }
}
