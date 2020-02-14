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

import net.sf.antcontrib.cpptasks.OutputTypeEnum;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Curt Arnold
 */
public class TestGccLinker {
    private final String realOSName = System.getProperty("os.name");

    @After
    public void tearDown() {
        System.setProperty("os.name", realOSName);
    }

    @Test
    public void testGetLinkerDarwinPlugin() {
        System.setProperty("os.name", "Mac OS X");
        GccLinker linker = GccLinker.getInstance();
        OutputTypeEnum outputType = new OutputTypeEnum();
        outputType.setValue("plugin");
        LinkType linkType = new LinkType();
        linkType.setOutputType(outputType);
        Linker pluginLinker = linker.getLinker(linkType);
        assertEquals("libfoo.bundle", pluginLinker.getOutputFileNames("foo", null)[0]);
    }

    @Test
    public void testGetLinkerDarwinShared() {
        System.setProperty("os.name", "Mac OS X");
        GccLinker linker = GccLinker.getInstance();
        OutputTypeEnum outputType = new OutputTypeEnum();
        outputType.setValue("shared");
        LinkType linkType = new LinkType();
        linkType.setOutputType(outputType);
        Linker sharedLinker = linker.getLinker(linkType);
        assertEquals("libfoo.dylib", sharedLinker.getOutputFileNames("foo", null)[0]);
    }

    @Test
    public void testGetLinkerNonDarwinPlugin() {
        System.setProperty("os.name", "Microsoft Windows");
        GccLinker linker = GccLinker.getInstance();
        OutputTypeEnum outputType = new OutputTypeEnum();
        outputType.setValue("plugin");
        LinkType linkType = new LinkType();
        linkType.setOutputType(outputType);
        Linker pluginLinker = linker.getLinker(linkType);
        assertEquals("libfoo.so", pluginLinker.getOutputFileNames("foo", null)[0]);
    }

    @Test
    public void testGetLinkerNonDarwinShared() {
        System.setProperty("os.name", "Microsoft Windows");
        GccLinker linker = GccLinker.getInstance();
        OutputTypeEnum outputType = new OutputTypeEnum();
        outputType.setValue("shared");
        LinkType linkType = new LinkType();
        linkType.setOutputType(outputType);
        Linker sharedLinker = linker.getLinker(linkType);
        assertEquals("libfoo.so", sharedLinker.getOutputFileNames("foo", null)[0]);
    }
}
