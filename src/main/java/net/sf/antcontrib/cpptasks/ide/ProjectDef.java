/*
 *
 * Copyright 2004-2008 The Ant-Contrib project
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
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.TargetInfo;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * <p>
 * Requests the creation of an IDE project file.  Experimental.
 * </p>
 * <p>
 * Implementation status: msdev5, msdev6 and cbuilderx
 * generate reasonable project files for simple projects,
 * xcode and msdev7 and msdev71 capture source file lists and
 * a few settings.
 * </p>
 *
 * @author Curt Arnold
 */
public final class ProjectDef extends DataType {
    /**
     * Name of property that must be present or definition will be ignored. May
     * be null.
     */
    private String ifProp;

    /**
     * Name of property that must be absent or definition will be ignored. May
     * be null.
     */
    private String unlessProp;

    /**
     * Project file name.
     */
    private File outFile;

    /**
     * Project name.
     */
    private String name;

    /**
     * Fail on error.
     */
    private boolean failOnError = true;

    /**
     * Overwrite existing project file.
     */
    private boolean overwrite = true;

    /**
     * Project writer.
     */
    private ProjectWriter projectWriter;

    /**
     * Object directory.
     */
    private File objDir;

    /**
     * List of dependency definitions.
     */
    private final List<DependencyDef> dependencies = new ArrayList<DependencyDef>();

    /**
     * List of comments.
     */
    private final List<CommentDef> comments = new ArrayList<CommentDef>();

    /**
     * Constructor.
     */
    public ProjectDef() {
    }

    /**
     * <p>
     * Set project type.
     * </p>
     * <table style="width:100%;border-collapse:collapse;border:1px solid black;">
     * <caption></caption>
     * <thead><tr><th>Supported project formats</th></tr></thead>
     * <tbody>
     * <tr>
     * <td>cbuilderx</td>
     * <td>Borland C++BuilderX</td>
     * </tr>
     * <tr>
     * <td>msvc5</td>
     * <td>Microsoft Visual C++ 97</td>
     * </tr>
     * <tr>
     * <td>msvc6</td>
     * <td>Microsoft Visual C++ 6</td>
     * </tr>
     * <tr>
     * <td>msvc7</td>
     * <td>Microsoft Visual C++.NET</td>
     * </tr>
     * <tr>
     * <td>msvc71</td>
     * <td>Microsoft Visual C++.NET 2003</td>
     * </tr>
     * <tr>
     * <td>msvc8</td>
     * <td>Microsoft Visual C++ 2005</td>
     * </tr>
     * <tr>
     * <td>msvc9</td>
     * <td>Microsoft Visual C++ 2008</td>
     * </tr>
     * <tr>
     * <td>xcode</td>
     * <td>Apple Xcode</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param value new value
     */
    public void setType(final ProjectWriterEnum value) {
        projectWriter = value.getProjectWriter();
    }

    /**
     * Sets the name for the generated project file.
     *
     * @param outfile output file name
     */
    public void setOutfile(final File outfile) {
        //
        //   if file name was empty, skip link step
        //
        if (outfile == null || !outfile.toString().isEmpty()) {
            outFile = outfile;
        }
    }

    /**
     * Sets whether a failure to write the project file should cause the
     * task to fail.  Default is true.
     *
     * @param value new value
     */
    public void setFailonerror(final boolean value) {
        failOnError = value;
    }

    /**
     * Sets whether an existing project file should be overwritten,
     * default is true.  If false and the project file exists,
     * the value of failonerror will determine if the task fails.
     *
     * @param value new value
     */
    public void setOverwrite(final boolean value) {
        overwrite = value;
    }

    /**
     * Gets whether an existing project file should be overwritten,
     * default is true.  If false and the project file exists,
     * the value of failonerror will determine if the task fails.
     *
     * @return value
     */
    public boolean getOverwrite() {
        return overwrite;
    }

    /**
     * <p>
     * Determine if this def should be used.
     * </p>
     * <p>
     * Definition will be active if the "if" variable (if specified) is set and
     * the "unless" variable (if specified) is not set and that all reference
     * or extended definitions are active
     * </p>
     *
     * @return true if processor is active
     */
    public boolean isActive() {
        Project project = getProject();
        return CUtil.isActive(project, ifProp, unlessProp);
    }

    /**
     * Class name for a user-supplied project writer.  Use the "type"
     * attribute to specify built-in project writer implementations.
     *
     * @param className full class name
     */
    public void setClassname(final String className) {
        try {
            Class<? extends ProjectWriter> implClass
                    = (Class<? extends ProjectWriter>) ProjectDef.class.getClassLoader().loadClass(className);
            try {
                Method getInstance = implClass.getMethod("getInstance");
                projectWriter = (ProjectWriter) getInstance.invoke(null);
            } catch (Exception ex) {
                projectWriter = implClass.newInstance();
            }
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
    }

    /**
     * <p>
     * Sets the property name for the 'if' condition.
     * </p>
     * <p>
     * The configuration will be ignored unless the property is defined.
     * </p>
     * <p>
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") will throw an exception when
     * evaluated.
     * </p>
     *
     * @param propName name of property
     */
    public void setIf(final String propName) {
        ifProp = propName;
    }

    /**
     * <p>
     * Set the property name for the 'unless' condition.
     * </p>
     * <p>
     * If named property is set, the configuration will be ignored.
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
        unlessProp = propName;
    }

    /**
     * Get name.
     *
     * @return String name
     */
    public String getName() {
        return name;
    }

    /**
     * Set name.
     *
     * @param value String name
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Executes the task. Compiles the given files.
     *
     * @param task       cc task
     * @param sources    source files (includes headers)
     * @param targets    compilation targets
     * @param linkTarget link target
     */
    public void execute(final CCTask task,
                        final List<File> sources,
                        final Hashtable<String, TargetInfo> targets,
                        final TargetInfo linkTarget) {
        try {
            projectWriter.writeProject(outFile, task, this, sources, targets, linkTarget);
        } catch (BuildException ex) {
            if (failOnError) {
                throw ex;
            } else {
                task.log(ex.toString());
            }
        } catch (Exception ex) {
            if (failOnError) {
                throw new BuildException(ex);
            } else {
                task.log(ex.toString());
            }
        }
    }

    /**
     * Gets the object files directory.
     *
     * @return directory, may be null.
     */
    public File getObjdir() {
        return objDir;
    }

    /**
     * Sets the directory used for object files.  If not specified,
     * the object files directory from cc task will be used.
     *
     * @param oDir object file directory.
     */
    public void getObjdir(final File oDir) {
        this.objDir = oDir;
    }

    /**
     * Add a dependency definition to the project.
     *
     * @param dependency dependency.
     */
    public void addDependency(final DependencyDef dependency) {
        dependencies.add(dependency);
    }

    public List<DependencyDef> getDependencies() {
        return new ArrayList<DependencyDef>(dependencies);
    }


    /**
     * Add comment for the generated project file.
     *
     * @param comment comment, may not be null.
     */
    public void addComment(final CommentDef comment) {
        comments.add(comment);
    }

    public List<CommentDef> getComments() {
        return new ArrayList<CommentDef>(comments);
    }

    /**
     * Required by documentation generator.
     */
    public void execute() {
        throw new BuildException(CUtil.STANDARD_EXCUSE);
    }
}
