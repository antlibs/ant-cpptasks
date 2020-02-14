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
package net.sf.antcontrib.cpptasks.devstudio;

import net.sf.antcontrib.cpptasks.compiler.AbstractProcessor;
import net.sf.antcontrib.cpptasks.compiler.TestAbstractLinker;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test for Microsoft Developer Studio linker
 * <p>
 * Override create to test concrete compiler implementations
 * </p>
 */
public class TestDevStudioLinker extends TestAbstractLinker {
    protected AbstractProcessor create() {
        return DevStudioLinker.getInstance();
    }

    @Test
    public void testGetIdentifier() {
        if (!Os.isFamily("windows")) {
            return;
        }
        AbstractProcessor compiler = create();
        String id = compiler.getIdentifier();
        boolean hasMSLinker = ((id.contains("Microsoft")) && (id.contains("Linker")))
                || id.contains("link");
        assertTrue(hasMSLinker);
    }
}
