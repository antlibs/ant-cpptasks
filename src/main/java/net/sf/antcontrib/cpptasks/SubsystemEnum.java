/*
 *
 * Copyright 2001-2004 The Ant-Contrib project
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

import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Enumeration of supported subsystems
 *
 * @author Curt Arnold
 */
public final class SubsystemEnum extends EnumeratedAttribute {
    private static final String[] values = new String[]{"gui", "console", "other"};

    public SubsystemEnum() {
        setValue("gui");
    }

    public String[] getValues() {
        return values;
    }
}
