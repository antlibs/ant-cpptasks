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
package net.sf.antcontrib.cpptasks.gcc;

import net.sf.antcontrib.cpptasks.compiler.AbstractProcessor;
import net.sf.antcontrib.cpptasks.parser.CParser;
import net.sf.antcontrib.cpptasks.parser.FortranParser;
import net.sf.antcontrib.cpptasks.parser.Parser;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test gcc compiler adapter
 */
public class TestGccCCompiler extends TestGccCompatibleCCompiler {
    protected GccCompatibleCCompiler create() {
        return GccCCompiler.getInstance();
    }

    @Test
    public void testBidObjectiveAssembly() {
        GccCCompiler compiler = GccCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.s"));
    }

    @Test
    public void testBidObjectiveC() {
        GccCCompiler compiler = GccCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.m"));
    }

    @Test
    public void testBidObjectiveCpp() {
        GccCCompiler compiler = GccCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.mm"));
    }

    @Test
    public void testBidPreprocessedCpp() {
        GccCCompiler compiler = GccCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.ii"));
    }

    @Test
    public void testCreateCParser1() {
        Parser parser = GccCCompiler.getInstance().createParser(new File("foo.c"));
        assertTrue(parser instanceof CParser);
    }

    @Test
    public void testCreateCParser2() {
        Parser parser = GccCCompiler.getInstance().createParser(new File("foo."));
        assertTrue(parser instanceof CParser);
    }

    @Test
    public void testCreateCParser3() {
        Parser parser = GccCCompiler.getInstance().createParser(new File("foo"));
        assertTrue(parser instanceof CParser);
    }

    @Test
    public void testCreateFortranParser1() {
        Parser parser = GccCCompiler.getInstance().createParser(new File("foo.f"));
        assertTrue(parser instanceof FortranParser);
    }

    @Test
    public void testCreateFortranParser2() {
        Parser parser = GccCCompiler.getInstance().createParser(new File("foo.FoR"));
        assertTrue(parser instanceof FortranParser);
    }
}
