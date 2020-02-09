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
package net.sf.antcontrib.cpptasks.types;

/**
 * <p>
 * Preprocessor macro definition.
 * </p>
 *
 * @author Mark A Russell {@literal <mark_russell@csgsystems.com>}
 */
public class DefineArgument extends UndefineArgument {
    private String value;

    public DefineArgument() {
        super(true);
    }

    /**
     * Returns the value of the define
     *
     * @return String
     */
    public final String getValue() {
        return value;
    }

    /**
     * Set the value attribute
     *
     * @param value String
     */
    public final void setValue(String value) {
        this.value = value;
    }
}
