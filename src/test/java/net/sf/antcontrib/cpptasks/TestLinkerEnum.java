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
package net.sf.antcontrib.cpptasks;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
/**
 * Tests for LinkerEnum
 *
 * @author Curt Arnold
 */
public class TestLinkerEnum {
    /**
     * Test checks that enumeration contains value g++
     * <p>
     * See patch [ 676276 ] Enhanced support for Mac OS X
     * </p>
     */
    @Test
    public void testContainsValueGpp() {
        assertTrue(new LinkerEnum().containsValue("g++"));
    }
}
