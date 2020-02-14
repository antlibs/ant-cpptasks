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
package net.sf.antcontrib.cpptasks.compiler;

import net.sf.antcontrib.cpptasks.OutputTypeEnum;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for LinkType
 *
 * @author Curt Arnold
 */
public class TestLinkType {
    /**
     * Tests if isPluginModule returns true when set to plugin output type
     * <p>
     * See patch [ 676276 ] Enhanced support for Mac OS X
     * </p>
     */
    @Test
    public void testIsPluginFalse() {
        LinkType type = new LinkType();
        OutputTypeEnum pluginType = new OutputTypeEnum();
        pluginType.setValue("executable");
        type.setOutputType(pluginType);
        assertFalse(type.isPluginModule());
    }

    /**
     * Tests if isPluginModule returns true when set to plugin output type
     * <p>
     * See patch [ 676276 ] Enhanced support for Mac OS X
     * </p>
     */
    @Test
    public void testIsPluginTrue() {
        LinkType type = new LinkType();
        OutputTypeEnum pluginType = new OutputTypeEnum();
        pluginType.setValue("plugin");
        type.setOutputType(pluginType);
        assertTrue(type.isPluginModule());
    }
}
