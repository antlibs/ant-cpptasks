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
package net.sf.antcontrib.cpptasks.ide;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.TargetInfo;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

/**
 * Project writer interface.
 *
 * @author Curt Arnold
 */
public interface ProjectWriter {
    /**
     * Write  project definition file.
     *
     * @param baseName   File name base, writer may append appropriate extension
     * @param task       task
     * @param projectDef project element
     * @param files      source and header files
     * @param targets    compilation targets
     * @param linkTarget link target
     * @throws IOException  if I/O error is encountered
     * @throws SAXException if I/O error during XML serialization
     * @throws TransformerConfigurationException if XML transformer is misconfigured
     */
    void writeProject(final File baseName,
                      final CCTask task,
                      final ProjectDef projectDef,
                      final List<File> files,
                      final Hashtable<String, TargetInfo> targets,
                      final TargetInfo linkTarget) throws IOException, SAXException, TransformerConfigurationException;
}
