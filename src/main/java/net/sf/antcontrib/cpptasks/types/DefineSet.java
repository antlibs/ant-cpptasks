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
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;

import java.util.Vector;

/**
 * <p>
 * Set of preprocessor macro defines and undefines.
 * </p>
 *
 * @author Mark A Russell {@literal <mark_russell@csgsystems.com>}
 * @author Adam Murdoch
 */
public class DefineSet extends DataType {
    private final Vector<UndefineArgument> defineList = new Vector<UndefineArgument>();
    private String ifCond = null;
    private String unlessCond = null;

    /**
     * Adds a define element.
     *
     * @param arg DefineArgument
     * @throws BuildException if reference
     */
    public void addDefine(DefineArgument arg) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        defineList.addElement(arg);
    }

    /**
     * Adds defines/undefines.
     *
     * @param defs an array of String
     * @param isDefine boolean
     */
    private void addDefines(String[] defs, boolean isDefine) {
        for (String s : defs) {
            UndefineArgument def;
            if (isDefine) {
                def = new DefineArgument();
            } else {
                def = new UndefineArgument();
            }
            def.setName(s);
            defineList.addElement(def);
        }
    }

    /**
     * Adds an undefine element.
     *
     * @param arg UndefineArgument
     * @throws BuildException if reference
     */
    public void addUndefine(UndefineArgument arg) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        defineList.addElement(arg);
    }

    public void execute() throws BuildException {
        throw new BuildException(CUtil.STANDARD_EXCUSE);
    }

    /**
     * Returns the defines and undefines in this set.
     *
     * @return an array of UndefineArgument
     */
    public UndefineArgument[] getDefines() throws BuildException {
        if (isReference()) {
            return getRef().getDefines();
        }
        if (isActive()) {
            return defineList.toArray(new UndefineArgument[0]);
        }
        return new UndefineArgument[0];
    }

    /**
     * Returns true if the define's if and unless conditions (if any) are
     * satisfied.
     *
     * @return boolean
     * @throws BuildException throws build exception if name is not set
     */
    public final boolean isActive() throws BuildException {
        return CUtil.isActive(getProject(), ifCond, unlessCond);
    }

    /**
     * A comma-separated list of preprocessor macros to define. Use nested
     * define elements to define macro values.
     *
     * @param defList comma-separated list of preprocessor macros
     * @throws BuildException throw if defineset is a reference
     */
    public void setDefine(CUtil.StringArrayBuilder defList) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        addDefines(defList.getValue(), true);
    }

    /**
     * Sets a description of the current data type.
     *
     * @param desc String
     */
    public void setDescription(String desc) {
        super.setDescription(desc);
    }

    /**
     * Sets an id that can be used to reference this element.
     *
     * @param id id
     */
    public void setId(String id) {
        //
        //  this is actually accomplished by a different
        //     mechanism, but we can document it
        //
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
     * Specifies that this element should behave as if the content of the
     * element with the matching id attribute was inserted at this location. If
     * specified, no other attributes or child content should be specified,
     * other than "description".
     *
     * @param r Reference
     */
    public void setRefid(Reference r) throws BuildException {
        if (!defineList.isEmpty()) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * A comma-separated list of preprocessor macros to undefine.
     *
     * @param undefList comma-separated list of preprocessor macros
     * @throws BuildException throw if defineset is a reference
     */
    public void setUndefine(CUtil.StringArrayBuilder undefList) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        addDefines(undefList.getValue(), false);
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

    private DefineSet getRef() {
        return getCheckedRef(DefineSet.class, "DefineSet");
    }
}
