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

import net.sf.antcontrib.cpptasks.CUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.util.Vector;

/**
 * <p>
 * Preprocessor macro undefinition.
 * </p>
 *
 * @author Mark A Russell {@literal <mark_russell@csgsystems.com>}
 */
public class UndefineArgument {
    /**
     * <p>
     * This method returns an array of UndefineArgument and DefineArgument's by
     * merging a base list with an override list.
     * </p>
     * <p>
     * Any define in the base list with a name that appears in the override
     * list is suppressed. All entries in the override list are preserved.
     * </p>
     *
     * @param base an array of UndefineArgument
     * @param override an array of UndefineArgument
     * @return an array of UndefineArgument
     */
    public static UndefineArgument[] merge(UndefineArgument[] base,
                                           UndefineArgument[] override) {
        if (base.length == 0) {
            return override.clone();
        }
        if (override.length == 0) {
            return base.clone();
        }
        Vector<UndefineArgument> unduplicated = new Vector<UndefineArgument>();
        for (UndefineArgument current : base) {
            String currentName = current.getName();
            boolean match = false;
            if (currentName == null) {
                match = true;
            } else {
                for (UndefineArgument over : override) {
                    String overName = over.getName();
                    if (overName != null && overName.equals(currentName)) {
                        match = true;
                        break;
                    }
                }
            }
            if (!match) {
                unduplicated.addElement(current);
            }
        }
        UndefineArgument[] combined = unduplicated.toArray(new UndefineArgument[unduplicated.size()
                + override.length]);
        System.arraycopy(override, 0, combined, unduplicated.size(), override.length);
        return combined;
    }

    private boolean define = false;
    private String ifCond;
    private String name;
    private String unlessCond;

    public UndefineArgument() {
    }

    protected UndefineArgument(boolean isDefine) {
        this.define = isDefine;
    }

    public void execute() throws BuildException {
        throw new BuildException(CUtil.STANDARD_EXCUSE);
    }

    /**
     * Returns the name of the define
     *
     * @return String
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the value of the define
     *
     * @return String
     */
    public String getValue() {
        return null;
    }

    /**
     * Returns true if the define's if and unless conditions (if any) are
     * satisfied.
     *
     * @param p Project
     * @return boolean
     * @throws BuildException throws build exception if name is not set
     */
    public final boolean isActive(Project p) throws BuildException {
        if (name == null) {
            throw new BuildException("<define> is missing name attribute");
        }
        return CUtil.isActive(p, ifCond, unlessCond);
    }

    /**
     * Returns true if this is a define, false if an undefine.
     *
     * @return boolean
     */
    public final boolean isDefine() {
        return define;
    }

    /**
     * <p>
     * Sets the property name for the 'if' condition.
     * </p>
     * <p>
     * The define will be ignored unless the property is defined.
     * </p>
     * <p>
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") will throw an exception when
     * evaluated.
     * </p>
     *
     * @param propName property name
     */
    public final void setIf(String propName) {
        ifCond = propName;
    }

    /**
     * Set the name attribute
     *
     * @param name String
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * <p>
     * Set the property name for the 'unless' condition.
     * </p>
     * <p>
     * If named property is set, the define will be ignored.
     * </p>
     * <p>
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") of the behavior will throw an
     * exception when evaluated.
     * </p>
     *
     * @param propName name of property
     */
    public final void setUnless(String propName) {
        unlessCond = propName;
    }
}
