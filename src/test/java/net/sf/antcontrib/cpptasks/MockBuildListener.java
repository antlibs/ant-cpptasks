/*
 *
 * Copyright 2003-2004 The Ant-Contrib project
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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

import java.util.Vector;

/**
 * Captures build events
 */
public class MockBuildListener implements BuildListener {
    private final Vector<BuildEvent> buildFinishedEvents = new Vector<BuildEvent>();
    private final Vector<BuildEvent> buildStartedEvents = new Vector<BuildEvent>();
    private final Vector<BuildEvent> messageLoggedEvents = new Vector<BuildEvent>();
    private final Vector<BuildEvent> targetFinishedEvents = new Vector<BuildEvent>();
    private final Vector<BuildEvent> targetStartedEvents = new Vector<BuildEvent>();
    private final Vector<BuildEvent> taskFinishedEvents = new Vector<BuildEvent>();
    private final Vector<BuildEvent> taskStartedEvents = new Vector<BuildEvent>();
    /**
     * Signals that the last target has finished. This event will still be
     * fired if an error occurred during the build.
     *
     * @param event An event with any relevant extra information. Must not be
     *              <code>null</code>.
     * @see BuildEvent#getException()
     */
    public void buildFinished(BuildEvent event) {
        buildFinishedEvents.addElement(event);
    }

    /**
     * Signals that a build has started. This event is fired before any targets
     * have started.
     *
     * @param event An event with any relevant extra information. Must not be
     *              <code>null</code>.
     */
    public void buildStarted(BuildEvent event) {
        buildStartedEvents.addElement(event);
    }

    public Vector<BuildEvent> getBuildFinishedEvents() {
        return new Vector<BuildEvent>(buildFinishedEvents);
    }

    /**
     * Gets a list of buildStarted events
     *
     * @return list of build started events
     */
    public Vector<BuildEvent> getBuildStartedEvents() {
        return new Vector<BuildEvent>(buildStartedEvents);
    }

    /**
     * Gets message logged events
     *
     * @return vector of "MessageLogged" events.
     */
    public Vector<BuildEvent> getMessageLoggedEvents() {
        return new Vector<BuildEvent>(messageLoggedEvents);
    }

    /**
     * Gets target finished events
     *
     * @return vector of "TargetFinished" events.
     */
    public Vector<BuildEvent> getTargetFinishedEvents() {
        return new Vector<BuildEvent>(targetFinishedEvents);
    }

    /**
     * Gets target started events
     *
     * @return vector of "TargetStarted" events.
     */
    public Vector<BuildEvent> getTargetStartedEvents() {
        return new Vector<BuildEvent>(targetStartedEvents);
    }

    /**
     * Gets task finished events
     *
     * @return vector of "TaskFinished" events.
     */
    public Vector<BuildEvent> getTaskFinishedEvents() {
        return new Vector<BuildEvent>(taskFinishedEvents);
    }

    /**
     * Gets task started events
     *
     * @return vector of "TaskStarted" events.
     */
    public Vector<BuildEvent> getTaskStartedEvents() {
        return new Vector<BuildEvent>(taskStartedEvents);
    }

    /**
     * Signals a message logging event.
     *
     * @param event An event with any relevant extra information. Must not be
     *              <code>null</code>.
     * @see BuildEvent#getMessage()
     * @see BuildEvent#getPriority()
     */
    public void messageLogged(BuildEvent event) {
        messageLoggedEvents.addElement(event);
    }

    /**
     * Signals that a target has finished. This event will still be fired if an
     * error occurred during the build.
     *
     * @param event An event with any relevant extra information. Must not be
     *              <code>null</code>.
     * @see BuildEvent#getException()
     */
    public void targetFinished(BuildEvent event) {
        targetFinishedEvents.addElement(event);
    }

    /**
     * Signals that a target is starting.
     *
     * @param event An event with any relevant extra information. Must not be
     *              <code>null</code>.
     * @see BuildEvent#getTarget()
     */
    public void targetStarted(BuildEvent event) {
        targetStartedEvents.addElement(event);
    }

    /**
     * Signals that a task has finished. This event will still be fired if an
     * error occurred during the build.
     *
     * @param event An event with any relevant extra information. Must not be
     *              <code>null</code>.
     * @see BuildEvent#getException()
     */
    public void taskFinished(BuildEvent event) {
        taskFinishedEvents.addElement(event);
    }

    /**
     * Signals that a task is starting.
     *
     * @param event An event with any relevant extra information. Must not be
     *              <code>null</code>.
     * @see BuildEvent#getTask()
     */
    public void taskStarted(BuildEvent event) {
        taskStartedEvents.addElement(event);
    }
}
