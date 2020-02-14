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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;

import static org.junit.Assert.assertNotNull;
/**
 * Base class for tests on classes that consume or public XML documents.
 *
 * @author Curt Arnold
 */
public abstract class TestXMLConsumer {
    /**
     * copies a resource to a temporary directory.
     *
     * @param source resource name, such as "files/openshore/history.xml".
     * @param dest   temporary file created by TesmporaryFoolder rule.
     * @throws IOException if something goes wrong
     */
    public static void copyResource(String source, File dest) throws IOException, URISyntaxException {
        URL url = TestAllClasses.class.getClassLoader().getResource(source);
        FileInputStream src = url == null ? new FileInputStream(source)
                : new FileInputStream(new File(url.toURI()));
        FileChannel sourceChannel = src.getChannel();

        FileChannel destChannel = null;
        try {
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            sourceChannel.close();
            destChannel.close();
        }
    }
}
