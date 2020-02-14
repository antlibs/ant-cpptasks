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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;

/**
 * DependencyTable tests
 *
 * @author Curt Arnold
 */
public class TestDependencyTable extends TestXMLConsumer {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File dependencyFile;

    @Before
    public void setUp() throws IOException {
        dependencyFile = temporaryFolder.newFile("dependencies.xml");
    }

    /**
     * Loads a dependency file from OpenSHORE (http://www.openshore.org)
     *
     * @throws IOException if something goes wrong
     * @throws ParserConfigurationException if parser configuration is wrong
     * @throws SAXException if parser input is incorrect
     */
    @Test
    public void testLoadOpenshore() throws IOException, ParserConfigurationException, SAXException, URISyntaxException {
        copyResource("openshore/dependencies.xml", dependencyFile);
        DependencyTable dependencies = new DependencyTable(temporaryFolder.getRoot());
        dependencies.load();
    }

    /**
     * Loads a dependency file from Xerces-C (http://xml.apache.org)
     *
     * @throws IOException if something goes wrong
     * @throws ParserConfigurationException if parser configuration is wrong
     * @throws SAXException if parser input is incorrect
     */
    @Test
    public void testLoadXerces() throws IOException, ParserConfigurationException, SAXException, URISyntaxException {
        copyResource("xerces-c/dependencies.xml", dependencyFile);
        DependencyTable dependencies = new DependencyTable(temporaryFolder.getRoot());
        dependencies.load();
    }
}
