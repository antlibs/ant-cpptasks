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
package net.sf.antcontrib.cpptasks.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the DefineArgument class
 */
public class TestDefineArgument {
    @Test(expected = BuildException.class)
    public void testIsActive1() {
        DefineArgument arg = new DefineArgument();
        Project project = new Project();
        boolean isActive = arg.isActive(project);
    }

    @Test
    public void testIsActive2() {
        DefineArgument arg = new DefineArgument();
        arg.setName("TEST");
        Project project = new Project();
        project.setProperty("cond", "");
        arg.setIf("cond");
        assertTrue(arg.isActive(project));
    }

    @Test
    public void testIsActive3() {
        DefineArgument arg = new DefineArgument();
        arg.setName("TEST");
        Project project = new Project();
        arg.setIf("cond");
        assertFalse(arg.isActive(project));
    }

    @Test(expected = BuildException.class)
    public void testIsActive4() {
        DefineArgument arg = new DefineArgument();
        arg.setName("TEST");
        Project project = new Project();
        project.setProperty("cond", "false");
        arg.setIf("cond");
        boolean isActive = arg.isActive(project);
    }

    @Test
    public void testIsActive5() {
        DefineArgument arg = new DefineArgument();
        arg.setName("TEST");
        Project project = new Project();
        project.setProperty("cond", "");
        arg.setUnless("cond");
        assertFalse(arg.isActive(project));
    }

    @Test
    public void testIsActive6() {
        DefineArgument arg = new DefineArgument();
        arg.setName("TEST");
        Project project = new Project();
        arg.setUnless("cond");
        assertTrue(arg.isActive(project));
    }

    @Test(expected = BuildException.class)
    public void testIsActive7() {
        DefineArgument arg = new DefineArgument();
        arg.setName("TEST");
        Project project = new Project();
        project.setProperty("cond", "false");
        arg.setUnless("cond");
        boolean isActive = arg.isActive(project);
    }

    @Test
    public void testIsActive8() {
        DefineArgument arg = new DefineArgument();
        arg.setName("TEST");
        Project project = new Project();
        project.setProperty("cond", "");
        arg.setIf("cond");
        arg.setUnless("cond");
        assertFalse(arg.isActive(project));
    }

    @Test
    public void testMerge() {
        UndefineArgument[] base = new UndefineArgument[2];
        UndefineArgument[] specific = new UndefineArgument[2];
        base[0] = new DefineArgument();
        base[0].setName("foo");
        base[1] = new UndefineArgument();
        base[1].setName("hello");
        specific[0] = new DefineArgument();
        specific[0].setName("hello");
        specific[1] = new UndefineArgument();
        specific[1].setName("world");
        UndefineArgument[] merged = UndefineArgument.merge(base, specific);
        assertEquals(3, merged.length);
        assertEquals("foo", merged[0].getName());
        assertTrue(merged[0].isDefine());
        assertEquals("hello", merged[1].getName());
        assertTrue(merged[1].isDefine());
        assertEquals("world", merged[2].getName());
        assertFalse(merged[2].isDefine());
    }
}
