/*
 *
 * Copyright 2002-2004 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.types;

import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.MockBuildListener;
import net.sf.antcontrib.cpptasks.MockFileCollector;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.devstudio.DevStudioLibrarian;
import net.sf.antcontrib.cpptasks.devstudio.DevStudioLinker;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the LibrarySet class.
 */
public class TestLibrarySet {

    private LibrarySet libset;
    private Project project;

    @Before
    public void setUp() throws Exception {
        libset = new LibrarySet();
        project = new Project();
    }

    /**
     * Evaluate isActive when "if" specifies a property that is set.
     */
    @Test
    public final void testIsActive1() {
        project.setProperty("windows", "");
        libset.setProject(project);
        libset.setIf("windows");
        CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("kernel32");
        libset.setLibs(libs);
        boolean isActive = libset.isActive(project);
        assertTrue(isActive);
    }

    /**
     * Evaluate isActive when "if" specifies a property whose value suggests the
     * user thinks the value is significant.
     */
    @Test(expected = BuildException.class)
    public final void testIsActive2() {
        //
        // setting the value to false should throw exception to warn user that they are misusing if
        //
        project.setProperty("windows", "false");
        libset.setIf("windows");
        boolean isActive = libset.isActive(project);
    }

    /**
     * Evaluate isActive when "if" specifies a property that is not set.
     */
    @Test
    public final void testIsActive3() {
        libset.setIf("windows");
        boolean isActive = libset.isActive(project);
        assertFalse(isActive);
    }

    /**
     * Evaluate isActive when "unless" specifies a property that is set.
     */
    @Test
    public final void testIsActive4() {
        project.setProperty("windows", "");
        libset.setUnless("windows");
        boolean isActive = libset.isActive(project);
        assertFalse(isActive);
    }

    /**
     * Evaluate isActive when "unless" specifies a property whose value suggests
     * the user thinks the value is significant.
     */
    @Test(expected = BuildException.class)
    public final void testIsActive5() {
        //
        // setting the value to false should throw
        // exception to warn user that they are misusing if
        //
        project.setProperty("windows", "false");
        libset.setUnless("windows");
        boolean isActive = libset.isActive(project);
    }

    /**
     * Evaluate isActive when "unless" specifies a property that is not set.
     */
    @Test
    public final void testIsActive6() {
        libset.setProject(project);
        libset.setUnless("windows");
        CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("kernel32");
        libset.setLibs(libs);
        boolean isActive = libset.isActive(project);
        assertTrue(isActive);
    }

    /**
     * The libs parameter should not end with .lib, .so, .a etc New behavior is
     * to warn if it ends in a suspicious extension.
     */
    @Test
    public final void testLibContainsDot() {
        MockBuildListener listener = new MockBuildListener();
        project.addBuildListener(listener);
        libset.setProject(project);
        CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("mylib1.1");
        libset.setLibs(libs);
        assertEquals(0, listener.getMessageLoggedEvents().size());
    }

    /**
     * The libs parameter should not end with .lib, .so, .a (that is,
     * should be kernel, not kernel.lib).  Previously the libset would
     * warn on configuration, now provides more feedback
     * when library is not found.
     */
    @Test
    public final void testLibContainsDotLib() {
        MockBuildListener listener = new MockBuildListener();
        project.addBuildListener(listener);
        libset.setProject(project);
        CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("mylib1.lib");
        libset.setLibs(libs);
        assertEquals(0, listener.getMessageLoggedEvents().size());
    }

    /**
     * Use of a libset or syslibset without a libs attribute should log a
     * warning message.
     */
    @Test
    public final void testLibNotSpecified() {
        MockBuildListener listener = new MockBuildListener();
        project.addBuildListener(listener);
        libset.setProject(project);
        boolean isActive = libset.isActive(project);
        assertFalse(isActive);
        assertEquals(1, listener.getMessageLoggedEvents().size());
    }

    /**
     * this threw an exception prior to 2002-09-05 and started to throw one
     * again 2002-11-19 up to 2002-12-11.
     */
    @Test
    public final void testShortLibName() {
        CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("li");
        libset.setProject(project);
        libset.setLibs(libs);
    }

    /**
     * The libs parameter should contain not a lib prefix (that is,
     * pthread not libpthread).  Previously the libset would
     * warn on configuration, now provides more feedback
     * when library is not found.
     */
    @Test
    public final void testStartsWithLib() {
        MockBuildListener listener = new MockBuildListener();
        project.addBuildListener(listener);
        libset.setProject(project);
        CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("libmylib1");
        libset.setLibs(libs);
        assertEquals(0, listener.getMessageLoggedEvents().size());
    }

    /**
     * This test creates two "fake" libraries in the temporary directory and
     * check how many are visited.
     *
     * @param linker   linker
     * @param expected expected number of visited files
     * @throws IOException if unable to write to temporary directory or delete temporary
     *                     files
     */
    private void testVisitFiles(final Linker linker, final int expected) throws IOException {
        MockBuildListener listener = new MockBuildListener();
        project.addBuildListener(listener);
        libset.setProject(project);

        // create temporary files named cpptasksXXXXX.lib
        File lib1 = File.createTempFile("cpptasks", ".lib");
        String lib1Name = lib1.getName();
        lib1Name = lib1Name.substring(0, lib1Name.indexOf(".lib"));
        File lib2 = File.createTempFile("cpptasks", ".lib");
        File baseDir = lib1.getParentFile();

        // set the dir attribute to the temporary directory
        libset.setDir(baseDir);
        // set libs to the file name without the suffix
        CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder(lib1Name);
        libset.setLibs(libs);

        // collect all files visited
        MockFileCollector collector = new MockFileCollector();
        libset.visitLibraries(project, linker, new File[0], collector);

        // get the canonical paths for the initial and visited libraries
        String expectedCanonicalPath = lib1.getCanonicalPath();
        String actualCanonicalPath = null;
        if (collector.size() == 1) {
            actualCanonicalPath = new File(collector.getBaseDir(0),
                    collector.getFileName(0)).getCanonicalPath();
        }
        // delete the temporary files
        lib1.delete();
        lib2.delete();
        //   was there only one match
        assertEquals(expected, collector.size());
        if (expected == 1) {
            // is its canonical path as expected
            assertEquals(expectedCanonicalPath, actualCanonicalPath);
        }
    }

    /**
     * Run testVisitFiles with the MSVC Linker
     * expect one matching file.
     *
     * @throws IOException if unable to create or delete temporary file
     */
    @Test
    public final void testLinkerVisitFiles() throws IOException {
        Linker linker = DevStudioLinker.getInstance();
        testVisitFiles(linker, 1);
    }

    /**
     * Run testVisitFiles with the MSVC Librarian
     * expect one matching file.
     *
     * @throws IOException if unable to create or delete temporary file
     */
    @Test
    public final void testLibrarianVisitFiles() throws IOException {
        Linker linker = DevStudioLibrarian.getInstance();
        testVisitFiles(linker, 0);
    }


    /**
     * This test specifies a library pattern that should
     * not match any available libraries and expects that
     * a build exception will be raised.
     * <p>
     * See bug 1380366
     * </p>
     */
    // code around line 320 in LibrarySet that would throw BuildException
    // (and prevent reaching this line) is disabled since logic for identifying
    // missing libraries does not work reliably on non-Windows platforms
    // @Test(expected = BuildException.class)
    @Test
    public final void testBadLibname() {
        MockBuildListener listener = new MockBuildListener();
        project.addBuildListener(listener);
        libset.setProject(project);
        // set libs to the file name without the suffix
        CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("badlibname");
        libset.setLibs(libs);

        // collect all files visited
        MockFileCollector collector = new MockFileCollector();
        libset.visitLibraries(project, DevStudioLinker.getInstance(), new File[0], collector);
    }
}
