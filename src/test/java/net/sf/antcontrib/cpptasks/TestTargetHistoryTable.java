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

import net.sf.antcontrib.cpptasks.compiler.ProcessorConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertTrue;

/**
 * Tests for TargetHistoryTable
 *
 * @author Curt Arnold
 */
public class TestTargetHistoryTable extends TestXMLConsumer {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static class MockProcessorConfiguration implements ProcessorConfiguration {
        public MockProcessorConfiguration() {
        }

        public int bid(String fileName) {
            return 100;
        }

        public String getIdentifier() {
            return "Mock Configuration";
        }

        public String[] getOutputFileNames(String baseName, VersionInfo versionInfo) {
            return new String[]{baseName};
        }

        public ProcessorParam[] getParams() {
            return new ProcessorParam[0];
        }

        public boolean getRebuild() {
            return false;
        }
    }

    private File historyFile;

    @Before
    public void setUp() throws IOException {
        historyFile = temporaryFolder.newFile("history.xml");
    }

    /**
     * Tests loading a stock history file
     *
     * @throws IOException if something goes wrong
     * @throws URISyntaxException if resource URI is incorrect
     */
    @Test
    public void testLoadOpenshore() throws IOException, URISyntaxException {
        copyResource("openshore/history.xml", historyFile);
        CCTask task = new CCTask();
        TargetHistoryTable history = new TargetHistoryTable(task, temporaryFolder.getRoot());
    }

    /**
     * Tests loading a stock history file
     *
     * @throws IOException if something goes wrong
     * @throws URISyntaxException if resource URI is incorrect
     */
    @Test
    public void testLoadXerces() throws IOException, URISyntaxException {
        copyResource("xerces-c/history.xml", historyFile);
        CCTask task = new CCTask();
        TargetHistoryTable history = new TargetHistoryTable(task, temporaryFolder.getRoot());
    }

    /**
     * Tests for bug fixed by patch [ 650397 ] Fix: Needless rebuilds on Unix
     *
     * @throws IOException if something goes wrong
     */
    @Test
    public void testUpdateTimeResolution() throws IOException {
        assertTrue(historyFile.exists());
        historyFile.delete();

        TargetHistoryTable table = new TargetHistoryTable(null, temporaryFolder.getRoot());
        //
        //  create a dummy compiled unit
        //
        File compiledFile = temporaryFolder.newFile("dummy.o");
        FileOutputStream compiledStream = new FileOutputStream(compiledFile);
        compiledStream.close();
        //
        //   lastModified times can be slightly less than
        //      task start time due to file system resolution.
        //      Mimic this by slightly incrementing the last modification time.
        //
        long startTime = compiledFile.lastModified() + 1;
        //
        //   update the table
        //
        table.update(new MockProcessorConfiguration(), new String[]{"dummy.o"}, null);
        //
        //   commit. If "compiled" file was judged to be
        //   valid we should have a history file.
        //
        table.commit();
        assertTrue("History file was not created", historyFile.exists());
        assertTrue("History file was empty", historyFile.length() > 10);
    }
}
