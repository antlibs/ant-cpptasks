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
package net.sf.antcontrib.cpptasks.gcc;

import java.io.File;
import java.util.Vector;

import junit.framework.TestCase;
import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.OutputTypeEnum;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;

/**
 * Test ld linker adapter abstract base class
 * <p>
 * Override create to test concrete compiler implementations
 * </p>
 */
public class TestAbstractLdLinker extends TestCase {
    private final String realOSName;

    public TestAbstractLdLinker(String name) {
        super(name);
        realOSName = System.getProperty("os.name");
    }

    protected AbstractLdLinker getLinker() {
        return GccLinker.getInstance();
    }

    protected void tearDown() {
        System.setProperty("os.name", realOSName);
    }

    /**
     * Checks for proper arguments for plugin generation on Darwin
     * <p>
     * See [ 676276 ] Enhanced support for Mac OS X
     * </p>
     */
    public void testAddImpliedArgsDarwinPlugin() {
        System.setProperty("os.name", "Mac OS X");
        AbstractLdLinker linker = getLinker();
        Vector args = new Vector();
        LinkType pluginType = new LinkType();
        OutputTypeEnum pluginOutType = new OutputTypeEnum();
        pluginOutType.setValue("plugin");
        pluginType.setOutputType(pluginOutType);
        linker.addImpliedArgs(false, pluginType, args);
        assertEquals(1, args.size());
        assertEquals("-bundle", args.elementAt(0));
    }

    /**
     * Checks for proper arguments for shared generation on Darwin
     * <p>
     * See [ 676276 ] Enhanced support for Mac OS X
     * </p>
     */
    public void testAddImpliedArgsDarwinShared() {
        System.setProperty("os.name", "Mac OS X");
        AbstractLdLinker linker = getLinker();
        Vector args = new Vector();
        LinkType pluginType = new LinkType();
        OutputTypeEnum pluginOutType = new OutputTypeEnum();
        pluginOutType.setValue("shared");
        pluginType.setOutputType(pluginOutType);
        linker.addImpliedArgs(false, pluginType, args);
        assertEquals(2, args.size());
        assertEquals("-prebind", args.elementAt(0));
        assertEquals("-dynamiclib", args.elementAt(1));
    }

    /**
     * Checks for proper arguments for plugin generation on Darwin
     * <p>
     * See [ 676276 ] Enhanced support for Mac OS X
     * </p>
     */
    public void testAddImpliedArgsNonDarwinPlugin() {
        System.setProperty("os.name", "VAX/VMS");
        AbstractLdLinker linker = getLinker();
        Vector args = new Vector();
        LinkType pluginType = new LinkType();
        OutputTypeEnum pluginOutType = new OutputTypeEnum();
        pluginOutType.setValue("plugin");
        pluginType.setOutputType(pluginOutType);
        linker.addImpliedArgs(false, pluginType, args);
        assertEquals(1, args.size());
        assertEquals("-shared", args.elementAt(0));
    }

    /**
     * Checks for proper arguments for shared generation on Darwin
     * <p>
     * See [ 676276 ] Enhanced support for Mac OS X
     * </p>
     */
    public void testAddImpliedArgsNonDarwinShared() {
        System.setProperty("os.name", "VAX/VMS");
        AbstractLdLinker linker = getLinker();
        Vector args = new Vector();
        LinkType pluginType = new LinkType();
        OutputTypeEnum pluginOutType = new OutputTypeEnum();
        pluginOutType.setValue("shared");
        pluginType.setOutputType(pluginOutType);
        linker.addImpliedArgs(false, pluginType, args);
        assertEquals(1, args.size());
        assertEquals("-shared", args.elementAt(0));
    }

    public void testAddLibrarySetDirSwitch() {
        AbstractLdLinker linker = getLinker();
        CCTask task = new CCTask();
        LibrarySet[] sets = new LibrarySet[]{new LibrarySet()};
        /* throws an Exception in setLibs otherwise */
        sets[0].setProject(new org.apache.tools.ant.Project());
        sets[0].setDir(new File("/foo"));
        sets[0].setLibs(new CUtil.StringArrayBuilder("bart,cart,dart"));
        Vector preargs = new Vector();
        Vector midargs = new Vector();
        Vector endargs = new Vector();
        String[] rc = linker.addLibrarySets(task, sets, preargs, midargs,
                endargs);
        String libdirSwitch = (String) endargs.elementAt(0);
        assertEquals(libdirSwitch.substring(0, 2), "-L");
        //
        //  can't have space after -L or will break Mac OS X
        //
        assertTrue(!libdirSwitch.substring(2, 3).equals(" "));
        assertEquals(libdirSwitch.substring(libdirSwitch.length() - 3), "foo");
    }

    public void testAddLibrarySetLibSwitch() {
        AbstractLdLinker linker = getLinker();
        CCTask task = new CCTask();
        LibrarySet[] sets = new LibrarySet[]{new LibrarySet()};
        /* throws an Exception in setLibs otherwise */
        sets[0].setProject(new org.apache.tools.ant.Project());
        sets[0].setDir(new File("/foo"));
        sets[0].setLibs(new CUtil.StringArrayBuilder("bart,cart,dart"));
        Vector preargs = new Vector();
        Vector midargs = new Vector();
        Vector endargs = new Vector();
        String[] rc = linker.addLibrarySets(task, sets, preargs, midargs,
                endargs);
        assertEquals("-lbart", (String) endargs.elementAt(1));
        assertEquals("-lcart", (String) endargs.elementAt(2));
        assertEquals("-ldart", (String) endargs.elementAt(3));
        assertEquals(endargs.size(), 4);
    }

    public void testAddLibrarySetLibFrameworkNonDarwin() {
        System.setProperty("os.name", "VAX/VMS");
        AbstractLdLinker linker = getLinker();
        CCTask task = new CCTask();
        LibrarySet[] sets = new LibrarySet[]{new LibrarySet()};
        /* throws an Exception in setLibs otherwise */
        sets[0].setProject(new org.apache.tools.ant.Project());
        sets[0].setDir(new File("/foo"));
        LibraryTypeEnum libType = new LibraryTypeEnum();
        libType.setValue("framework");
        sets[0].setType(libType);
        sets[0].setLibs(new CUtil.StringArrayBuilder("bart,cart,dart"));
        Vector preargs = new Vector();
        Vector midargs = new Vector();
        Vector endargs = new Vector();
        String[] rc = linker.addLibrarySets(task, sets, preargs, midargs,
                endargs);
        assertEquals("-L", ((String) endargs.elementAt(0)).substring(0, 2));
        assertEquals("-Bdynamic", (String) endargs.elementAt(1));
        assertEquals("-lbart", (String) endargs.elementAt(2));
        assertEquals("-lcart", (String) endargs.elementAt(3));
        assertEquals("-ldart", (String) endargs.elementAt(4));
        assertEquals(endargs.size(), 5);
    }

    public void testAddLibrarySetLibFrameworkDarwin() {
        System.setProperty("os.name", "Mac OS X");
        AbstractLdLinker linker = getLinker();
        CCTask task = new CCTask();
        LibrarySet[] sets = new LibrarySet[]{new LibrarySet()};
        /* throws an Exception in setLibs otherwise */
        sets[0].setProject(new org.apache.tools.ant.Project());
        sets[0].setDir(new File("/foo"));
        LibraryTypeEnum libType = new LibraryTypeEnum();
        libType.setValue("framework");
        sets[0].setType(libType);
        sets[0].setLibs(new CUtil.StringArrayBuilder("bart,cart,dart"));
        Vector preargs = new Vector();
        Vector midargs = new Vector();
        Vector endargs = new Vector();
        String[] rc = linker.addLibrarySets(task, sets, preargs, midargs,
                endargs);
        assertEquals("-F", ((String) endargs.elementAt(0)).substring(0, 2));
        assertEquals("-framework bart", (String) endargs.elementAt(1));
        assertEquals("-framework cart", (String) endargs.elementAt(2));
        assertEquals("-framework dart", (String) endargs.elementAt(3));
        assertEquals(endargs.size(), 4);
    }

    public void testAddLibraryStatic() {
        AbstractLdLinker linker = getLinker();
        CCTask task = new CCTask();
        LibrarySet[] sets = new LibrarySet[]{
                new LibrarySet(),
                new LibrarySet(),
                new LibrarySet()};
        /* throws an Exception in setLibs otherwise */
        sets[0].setProject(new org.apache.tools.ant.Project());
        sets[0].setLibs(new CUtil.StringArrayBuilder("bart"));
        sets[1].setProject(new org.apache.tools.ant.Project());
        sets[1].setLibs(new CUtil.StringArrayBuilder("cart"));
        LibraryTypeEnum libType = new LibraryTypeEnum();
        libType.setValue("static");
        sets[1].setType(libType);
        sets[2].setProject(new org.apache.tools.ant.Project());
        sets[2].setLibs(new CUtil.StringArrayBuilder("dart"));
        Vector preargs = new Vector();
        Vector midargs = new Vector();
        Vector endargs = new Vector();
        String[] rc = linker.addLibrarySets(task, sets, preargs, midargs,
                endargs);
        assertEquals("-lbart", (String) endargs.elementAt(0));
        assertEquals("-Bstatic", (String) endargs.elementAt(1));
        assertEquals("-lcart", (String) endargs.elementAt(2));
        assertEquals("-Bdynamic", (String) endargs.elementAt(3));
        assertEquals("-ldart", (String) endargs.elementAt(4));
        assertEquals(endargs.size(), 5);
    }

    public void testLibReturnValue() {
        AbstractLdLinker linker = getLinker();
        CCTask task = new CCTask();
        LibrarySet[] sets = new LibrarySet[]{new LibrarySet()};
        /* throws an Exception in setLibs otherwise */
        sets[0].setProject(new org.apache.tools.ant.Project());
        sets[0].setDir(new File("/foo"));
        sets[0].setLibs(new CUtil.StringArrayBuilder("bart,cart,dart"));
        Vector preargs = new Vector();
        Vector midargs = new Vector();
        Vector endargs = new Vector();
        String[] rc = linker.addLibrarySets(task, sets, preargs, midargs,
                endargs);
        assertEquals(3, rc.length);
        assertEquals("bart", rc[0]);
        assertEquals("cart", rc[1]);
        assertEquals("dart", rc[2]);
    }
}
