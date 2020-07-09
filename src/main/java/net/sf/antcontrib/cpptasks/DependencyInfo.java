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
package net.sf.antcontrib.cpptasks;

import java.util.Vector;

/**
 * @author Curt Arnold
 */
public final class DependencyInfo {
    /**
     * <p>
     * Last modified time of this file or anything that it depends on.
     * </p>
     * <p>
     * Not persisted since almost any change could invalidate it. Initialized
     * to long.MIN_VALUE on construction.
     * </p>
     */
    private long compositeLastModified;
    private final String includePathIdentifier;
    private final String[] includes;
    private final String source;
    private final long sourceLastModified;
    private final String[] sysIncludes;

    public DependencyInfo(String includePathIdentifier, String source, long sourceLastModified,
                          Vector<String> includes, Vector<String> sysIncludes) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (includePathIdentifier == null) {
            throw new NullPointerException("includePathIdentifier");
        }
        this.source = source;
        this.sourceLastModified = sourceLastModified;
        this.includePathIdentifier = includePathIdentifier;
        if (includes.size() == 0) {
            this.includes = new String[0];
            compositeLastModified = sourceLastModified;
        } else {
            this.includes = includes.toArray(new String[0]);
            compositeLastModified = Long.MIN_VALUE;
        }
        this.sysIncludes = sysIncludes.toArray(new String[0]);
    }

    /**
     * Returns the latest modification date of the source or anything that it
     * depends on.
     *
     * @return the composite lastModified time, returns Long.MIN_VALUE if not
     * set
     */
    public long getCompositeLastModified() {
        return compositeLastModified;
    }

    public String getIncludePathIdentifier() {
        return includePathIdentifier;
    }

    public String[] getIncludes() {
        String[] includesClone = includes.clone();
        return includesClone;
    }

    public String getSource() {
        return source;
    }

    public long getSourceLastModified() {
        return sourceLastModified;
    }

    public String[] getSysIncludes() {
        String[] sysIncludesClone = sysIncludes.clone();
        return sysIncludesClone;
    }

    public void setCompositeLastModified(long lastMod) {
        compositeLastModified = lastMod;
    }
}
