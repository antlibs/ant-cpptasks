/*
 *
 * Copyright 2002-2005 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.parser;

import org.junit.Test;

import java.io.CharArrayReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the CParser class.
 */
public final class TestCParser {
    /**
     * Checks parsing of #include &lt;foo.h&gt;.
     *
     * @throws IOException test fails on IOException
     */
    @Test
    public void testImmediateImportBracket() throws IOException {
        CharArrayReader reader = new CharArrayReader("#import <foo.h> nowhatever  ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 1);
        assertEquals("foo.h", includes[0]);
    }

    /**
     * Checks parsing of #import "foo.h".
     *
     * @throws IOException test fails on IOException
     */
    @Test
    public void testImmediateImportQuote() throws IOException {
        CharArrayReader reader = new CharArrayReader("#import \"foo.h\"   ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 1);
        assertEquals("foo.h", includes[0]);
    }

    /**
     * Checks parsing of #include &lt;foo.h&gt;.
     *
     * @throws IOException test fails on IOException
     */
    @Test
    public void testImmediateIncludeBracket() throws IOException {
        CharArrayReader reader = new CharArrayReader("#include      <foo.h>   ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 1);
        assertEquals("foo.h", includes[0]);
    }

    /**
     * Checks parsing of #include "foo.h".
     *
     * @throws IOException test fails on IOException.
     */
    @Test
    public void testImmediateIncludeQuote() throws IOException {
        CharArrayReader reader = new CharArrayReader("#include     \"foo.h\"   ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 1);
        assertEquals("foo.h", includes[0]);
    }

    /**
     * Checks parsing of #import &lt;foo.h.
     *
     *
     * @throws IOException test fails on IOException
     */
    @Test
    public void testIncompleteImmediateImportBracket() throws IOException {
        CharArrayReader reader = new CharArrayReader("#import <foo.h   ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 0);
    }

    /**
     * Checks parsing of #import &quot;foo.h.
     *
     *
     * @throws IOException test fails on IOException
     */
    @Test
    public void testIncompleteImmediateImportQuote() throws IOException {
        CharArrayReader reader = new CharArrayReader("#import \"foo.h   ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 0);
    }

    /**
     * Checks parsing of #include &lt;foo.h.
     *
     *
     * @throws IOException test fails on IOException
     */
    @Test
    public void testIncompleteImmediateIncludeBracket() throws IOException {
        CharArrayReader reader = new CharArrayReader("#include <foo.h   ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 0);
    }

    /**
     * Checks parsing of #include &quot;foo.h.
     *
     *
     * @throws IOException test fails on IOException
     */
    @Test
    public void testIncompleteImmediateIncludeQuote() throws IOException {
        CharArrayReader reader = new CharArrayReader("#include     \"foo.h    ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 0);
    }

    /**
     * Checks parsing of #include foo.h.
     *
     * @throws IOException test fails on IOException
     */
    @Test
    public void testNoQuoteOrBracket() throws IOException {
        CharArrayReader reader = new CharArrayReader("#include foo.h  ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 0);
    }

    /**
     * Checks parsing of //#include "foo.h".
     *
     * @throws IOException test fails on IOException
     */
    @Test
    public void testNotFirstWhitespace() throws IOException {
        CharArrayReader reader = new CharArrayReader("//#include \"foo.h\"".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 0);
    }

    /**
     * Checks parsing when line contains leading whitespace.
     *
     * @throws IOException test fails on IOException.
     */
    @Test
    public void testLeadingSpace() throws IOException {
        CharArrayReader reader = new CharArrayReader(" #include     \"foo.h\"   ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 1);
        assertEquals("foo.h", includes[0]);
    }

    /**
     * Checks parsing when line contains a leading tab.
     *
     * @throws IOException test fails on IOException.
     */
    @Test
    public void testLeadingTab() throws IOException {
        CharArrayReader reader = new CharArrayReader("\t#include     \"foo.h\"   ".toCharArray());
        CParser parser = new CParser();
        parser.parse(reader);
        String[] includes = parser.getIncludes();
        assertEquals(includes.length, 1);
        assertEquals("foo.h", includes[0]);
    }
}
