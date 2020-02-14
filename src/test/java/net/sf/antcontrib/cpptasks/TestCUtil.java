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
package net.sf.antcontrib.cpptasks;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
/**
 * Tests for CUtil class
 */
public class TestCUtil {
    @Test
    public void testGetPathFromEnvironment() {
        File[] files = CUtil.getPathFromEnvironment("LIB", ";");
        assertNotNull(files);
    }

    @Test
    public void testGetRelativePath1() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase, new File("/foo/bar/baz"));
        assertEquals("baz", rel);
    }

    @Test
    public void testGetRelativePath2() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase, new File("/foo/bar/"));
        assertEquals(".", rel);
    }

    @Test
    public void testGetRelativePath3() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase,
                new File("/foo/bar/a"));
        assertEquals("a", rel);
    }

    @Test
    public void testGetRelativePath4() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase, new File("/foo/"));
        assertEquals("..", rel);
    }

    @Test
    public void testGetRelativePath5() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase, new File("/a"));
        String expected = ".." + File.separator + ".." + File.separator + "a";
        assertEquals(expected, rel);
    }

    @Test
    public void testGetRelativePath6() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase, new File("/foo/baz/bar"));
        String expected = ".." + File.separator + "baz" + File.separator + "bar";
        assertEquals(expected, rel);
    }

    @Test
    public void testGetRelativePath7() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        //
        //  skip the UNC test unless running on Windows
        //
        String osName = System.getProperty("os.name");
        if (osName.indexOf("Windows") >= 0) {
            File uncFile = new File("\\\\fred\\foo.bar");
            String uncPath;
            try {
                uncPath = uncFile.getCanonicalPath();
            } catch (IOException ex) {
                uncPath = uncFile.toString();
            }
            String rel = CUtil.getRelativePath(canonicalBase, uncFile);
            assertEquals(uncPath, rel);
        }
    }

    @Test
    public void testGetRelativePath8() throws IOException {
        String canonicalBase = new File("/foo/bar/something").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase,
                new File("/foo/bar/something.extension"));
        String expected = ".." + File.separator + "something.extension";
        assertEquals(expected, rel);
    }

    @Test
    public void testGetRelativePath9() throws IOException {
        String canonicalBase = new
                File("/foo/bar/something").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase,
                new File("/foo/bar/somethingElse"));
        String expected = ".." + File.separator + "somethingElse";
        assertEquals(expected, rel);
    }

    @Test
    public void testGetRelativePath10() throws IOException {
        String canonicalBase = new
                File("/foo/bar/something").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase,
                new File("/foo/bar/something else"));
        String expected = ".." + File.separator + "something else";
        assertEquals(expected, rel);
    }

    @Test
    public void testParsePath1() {
        File[] files = CUtil.parsePath("", ";");
        assertEquals(0, files.length);
    }

    @Test
    public void testParsePath2() {
        String workingDir = System.getProperty("user.dir");
        File[] files = CUtil.parsePath(workingDir, ";");
        assertEquals(1, files.length);
        File workingDirFile = new File(workingDir);
        assertEquals(workingDirFile, files[0]);
    }

    @Test
    public void testParsePath3() {
        String workingDir = System.getProperty("user.dir");
        File[] files = CUtil.parsePath(workingDir + ";", ";");
        assertEquals(1, files.length);
        assertEquals(new File(workingDir), files[0]);
    }

    @Test
    public void testParsePath4() {
        String workingDir = System.getProperty("user.dir");
        String javaHome = System.getProperty("java.home");
        File[] files = CUtil.parsePath(workingDir + ";" + javaHome, ";");
        assertEquals(2, files.length);
        assertEquals(new File(workingDir), files[0]);
        assertEquals(new File(javaHome), files[1]);
    }

    @Test
    public void testParsePath5() {
        String workingDir = System.getProperty("user.dir");
        String javaHome = System.getProperty("java.home");
        File[] files = CUtil.parsePath(workingDir + ";" + javaHome + ";", ";");
        assertEquals(2, files.length);
        assertEquals(new File(workingDir), files[0]);
        assertEquals(new File(javaHome), files[1]);
    }

    /**
     * Test of xmlAttributeEncode.
     * <p>
     * See patch 1267472 and bug 1032302.
     * </p>
     */
    @Test
    public void testXmlEncode() {
        assertEquals("&lt;&quot;boo&quot;&gt;", CUtil.xmlAttribEncode("<\"boo\">"));
    }
}
