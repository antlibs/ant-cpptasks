/*
 *
 * Copyright 2002-2007 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.sun;

import net.sf.antcontrib.cpptasks.compiler.AbstractProcessor;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.assertEquals;

/**
 * Test Sun Forte compiler adapter
 */
// TODO Since ForteCCCompiler extends GccCompatibleCCompiler, this test
// should probably extend TestGccCompatibleCCompiler.
public class TestForteCCCompiler {

    private ForteCCCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = ForteCCCompiler.getInstance();
    }

    @Test
    public void testBidC() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.c"));
    }

    @Test
    public void testBidCpp() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.C"));
    }

    @Test
    public void testBidCpp2() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.cc"));
    }

    @Test
    public void testBidCpp3() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.cxx"));
    }

    @Test
    public void testBidCpp4() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.cpp"));
    }

    @Test
    public void testBidCpp5() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.c++"));
    }

    @Test
    public void testBidPreprocessed() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.i"));
    }

    @Test
    public void testBidAssembly() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.s"));
    }

    /**
     * Tests command line switches for warning = 0
     */
    @Test
    public void testWarningLevel0() {
        Vector<String> args = new Vector<String>();
        compiler.addWarningSwitch(args, 0);
        assertEquals(1, args.size());
        assertEquals("-w", args.elementAt(0));
    }

    /**
     * Tests command line switches for warning = 1
     */
    @Test
    public void testWarningLevel1() {
        Vector<String> args = new Vector<String>();
        compiler.addWarningSwitch(args, 1);
        assertEquals(0, args.size());
    }

    /**
     * Tests command line switches for warning = 2
     */
    @Test
    public void testWarningLevel2() {
        Vector<String> args = new Vector<String>();
        compiler.addWarningSwitch(args, 2);
        assertEquals(0, args.size());
    }

    /**
     * Tests command line switches for warning = 3
     */
    @Test
    public void testWarningLevel3() {
        Vector<String> args = new Vector<String>();
        compiler.addWarningSwitch(args, 3);
        assertEquals(1, args.size());
        assertEquals("+w", args.elementAt(0));
    }

    /**
     * Tests command line switches for warning = 4
     */
    @Test
    public void testWarningLevel4() {
        Vector<String> args = new Vector<String>();
        compiler.addWarningSwitch(args, 4);
        assertEquals(1, args.size());
        assertEquals("+w2", args.elementAt(0));
    }

    /**
     * Tests command line switches for warning = 5
     */
    @Test
    public void testWarningLevel5() {
        Vector<String> args = new Vector<String>();
        compiler.addWarningSwitch(args, 5);
        assertEquals(2, args.size());
        assertEquals("+w2", args.elementAt(0));
        assertEquals("-xwe", args.elementAt(1));
    }
}
