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
package net.sf.antcontrib.cpptasks.devstudio;

import java.util.Vector;

/**
 * A add-in class for Microsoft Developer Studio processors
 */
public class DevStudioProcessor {
    public static void addWarningSwitch(Vector<String> args, int level) {
        switch (level) {
            case 0:
                args.addElement("/W0");
                break;
            case 1:
                args.addElement("/W1");
                break;
            case 2:
                break;
            case 3:
                args.addElement("/W3");
                break;
            case 4:
                args.addElement("/W4");
                break;
            case 5:
                args.addElement("/WX");
                break;
        }
    }

    public static String getCommandFileSwitch(String cmdFile) {
        StringBuilder buf = new StringBuilder("@");
        if (cmdFile.contains(" ")) {
            buf.append('\"');
            buf.append(cmdFile.replace('/', '\\'));
            buf.append('\"');
        } else {
            buf.append(cmdFile);
        }
        return buf.toString();
    }

    public static void getDefineSwitch(StringBuilder buffer, String define, String value) {
        buffer.append("/D");
        buffer.append(define);
        if (value != null && !value.isEmpty()) {
            buffer.append('=');
            buffer.append(value);
        }
    }

    public static String getIncludeDirSwitch(String includeDir) {
        return "/I" + includeDir.replace('/', '\\');
    }

    public static String[] getOutputFileSwitch(String outPath) {
        StringBuilder buf = new StringBuilder("/Fo");
        if (outPath.contains(" ")) {
            buf.append('\"');
            buf.append(outPath);
            buf.append('\"');
        } else {
            buf.append(outPath);
        }
        return new String[]{buf.toString()};
    }

    public static void getUndefineSwitch(StringBuilder buffer, String define) {
        buffer.append("/U");
        buffer.append(define);
    }

    public static boolean isCaseSensitive() {
        return false;
    }

    private DevStudioProcessor() {
    }
}
