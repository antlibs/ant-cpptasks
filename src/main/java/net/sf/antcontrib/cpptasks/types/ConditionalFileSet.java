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
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.FileSet;

/**
 * <p>
 * An Ant FileSet object augmented with if and unless conditions.
 * </p>
 *
 * @author Curt Arnold
 */
public class ConditionalFileSet extends FileSet {
    private String ifCond;
    private String unlessCond;

    public ConditionalFileSet() {
    }

    public void execute() throws BuildException {
        throw new BuildException(CUtil.STANDARD_EXCUSE);
    }

    /**
     * overrides FileSet's implementation which would throw an exception since
     * the referenced object isn't this type.
     *
     * @param p Project
     */
    protected AbstractFileSet getRef(Project p) {
        return (AbstractFileSet) getRefid().getReferencedObject(p);
    }

    /**
     * Returns true if the Path's if and unless conditions (if any) are
     * satisfied.
     *
     * @return boolean
     */
    public boolean isActive() throws BuildException {
        Project p = getProject();
        if (p == null) {
            throw new IllegalStateException("setProject() should have been called");
        }
        return CUtil.isActive(p, ifCond, unlessCond);
    }

    /**
     * <p>
     * Sets the property name for the 'if' condition.
     * </p>
     * <p>
     * The fileset will be ignored unless the property is defined.
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
     * <p>
     * Set the property name for the 'unless' condition.
     * </p>
     * <p>
     * If named property is set, the fileset will be ignored.
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
}
