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
package net.sf.antcontrib.cpptasks.ibm;

import net.sf.antcontrib.cpptasks.compiler.AbstractProcessor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test IBM Visual Age compiler adapter
 */
// TODO Since VisualAgeCCompiler extends GccCompatibleCCompiler, this test
// should probably extend TestGccCompatibleCCompiler.
public class TestVisualAgeCCompiler {

    private VisualAgeCCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = VisualAgeCCompiler.getInstance();
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
    public void testBidPreprocessed() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.i"));
    }

    @Test
    public void testBidAssembly() {
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.s"));
    }
}
