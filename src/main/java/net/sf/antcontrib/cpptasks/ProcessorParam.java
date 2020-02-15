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

import org.apache.tools.ant.Project;

/*******************************************************************************
 * <p>
 * A processor parameter.
 * </p>
 *
 * @author inger
 *
 ******************************************************************************/
public class ProcessorParam {
    private String ifCond;
    private String name;
    private String unlessCond;
    private String value;

    public ProcessorParam() {
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns true if the define's if and unless conditions (if any) are
     * satisfied.
     *
     * @param p Project
     * @return boolean
     */
    public boolean isActive(Project p) {
        if (value == null) {
            return false;
        }
        if (ifCond != null && p.getProperty(ifCond) == null) {
            return false;
        } else if (unlessCond != null && p.getProperty(unlessCond) != null) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * Sets the property name for the 'if' condition.
     * </p>
     * <p>
     * The argument will be ignored unless the property is defined.
     * </p>
     * <p>
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") will throw an exception when
     * evaluated.
     * </p>
     *
     * @param propName String
     */
    public void setIf(String propName) {
        ifCond = propName;
    }

    /**
     * Specifies relative location of argument on command line. "start" will
     * place argument at start of command line, "mid" will place argument after
     * all "start" arguments but before filenames, "end" will place argument
     * after filenames.
     *
     * @param name String
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>
     * Set the property name for the 'unless' condition.
     * </p>
     * <p>
     * If named property is set, the argument will be ignored.
     * </p>
     * <p>
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") of the behavior will throw an
     * exception when evaluated.
     * </p>
     *
     * @param propName name of property
     */
    public void setUnless(String propName) {
        unlessCond = propName;
    }

    /**
     * Specifies the string that should appear on the command line. The
     * argument will be quoted if it contains embedded blanks. Use multiple
     * arguments to avoid quoting.
     *
     * @param value String
     */
    public void setValue(String value) {
        this.value = value;
    }
}
