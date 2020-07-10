/*
 *
 * Copyright 2004 The Ant-Contrib project
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;

/**
 * <p>
 * Information on the execution platforms for the generated code.
 * (Non-functional prototype)
 * </p>
 */
public final class TargetDef extends DataType {
    /**
     * if property.
     */
    private String ifCond;

    /**
     * unless property.
     */
    private String unlessCond;

    /**
     * cpu.
     */
    private CPUEnum cpu;

    /**
     * architecture.
     */
    private ArchEnum arch;

    /**
     * OS Family.
     */
    private OSFamilyEnum osFamily;

    /**
     * Constructor.
     */
    public TargetDef() {
    }

    /**
     * Bogus method required for documentation generation.
     */
    public void execute() {
        throw new BuildException(CUtil.STANDARD_EXCUSE);
    }

    /**
     * Returns true if the define's if and unless conditions (if any) are
     * satisfied.
     *
     * @return true if active
     */
    public boolean isActive() {
        return CUtil.isActive(getProject(), ifCond, unlessCond);
    }

    /**
     * Sets a description of the current data type.
     *
     * @param desc description
     */
    public void setDescription(final String desc) {
        super.setDescription(desc);
    }

    /**
     * Sets an id that can be used to reference this element.
     *
     * @param id id
     */
    public void setId(final String id) {
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
    public void setIf(final String propName) {
        ifCond = propName;
    }

    /**
     * Specifies that this element should behave as if the content of the
     * element with the matching id attribute was inserted at this location. If
     * specified, no other attributes should be specified.
     *
     * @param r id of referenced target
     */
    public void setRefid(final Reference r) {
        super.setRefid(r);
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
    public void setUnless(final String propName) {
        unlessCond = propName;
    }

    /**
     * Gets cpu.
     *
     * @return cpu, may be null.
     */
    public CPUEnum getCpu() {
        if (isReference()) {
            return getRef().getCpu();
        }
        return cpu;
    }

    /**
     * Gets arch.
     *
     * @return arch, may be null.
     */
    public ArchEnum getArch() {
        if (isReference()) {
            return getRef().getArch();
        }
        return arch;
    }

    /**
     * Gets operating system family.
     *
     * @return os family, may be null.
     */
    public OSFamilyEnum getOsfamily() {
        if (isReference()) {
            return getRef().getOsfamily();
        }
        return osFamily;
    }

    /**
     * Sets preferred cpu, but does not use cpu specific instructions.
     *
     * @param value new value
     */
    public void setCpu(final CPUEnum value) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        cpu = value;
    }

    /**
     * Sets cpu architecture, compiler may use cpu specific instructions.
     *
     * @param value new value
     */
    public void setArch(final ArchEnum value) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (cpu != null) {
            throw tooManyAttributes();
        }
        arch = value;
    }

    /**
     * Sets operating system family.
     *
     * @param value new value
     */
    public void setOsfamily(final OSFamilyEnum value) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (cpu != null) {
            throw tooManyAttributes();
        }
        osFamily = value;
    }

    private TargetDef getRef() {
        return getCheckedRef(TargetDef.class, "TargetDef");
    }
}
