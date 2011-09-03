package hudson.maven.release.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import hudson.maven.release.core.tasks.PerformReleaseProjectTask;
import hudson.maven.release.core.tasks.PrepareReleaseProjectTask;
import hudson.maven.release.core.tasks.RollbackReleaseProjectTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.BooleanUtils;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseManagerListener;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseDescriptorStore;
import org.apache.maven.shared.release.config.ReleaseDescriptorStoreException;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.repository.LocalRepository;

/**
 * 
 * @author Jason van Zyl
 * @author Edwin Punzalan
 * @version $Id: DefaultContinuumReleaseManager.java 1063962 2011-01-27
 *          02:28:22Z ctan $
 */
public class DefaultJenkinsReleaseManager implements JenkinsReleaseManager {
	/**
	 * @plexus.requirement
	 */
	private ReleaseDescriptorStore releaseStore;

	/**
	 * @plexus.requirement
	 */
	private TaskQueue prepareReleaseQueue;

	/**
	 * @plexus.requirement
	 */
	private TaskQueue performReleaseQueue;

	/**
	 * @plexus.requirement
	 */
	private TaskQueue rollbackReleaseQueue;

	/**
	 * @plexus.requirement
	 */
	private ScmManager scmManager;

	private Map<String, ReleaseManagerListener> listeners;

	/**
	 * contains previous release:prepare descriptors; one per project
	 * 
	 * @todo remove static when singleton strategy is working
	 */
	private static Map preparedReleases;

	/**
	 * contains results
	 * 
	 * @todo remove static when singleton strategy is working
	 */
	private static Map releaseResults;

	@Override
	public String prepare(ProjectDescriptor project, Properties releaseProperties, Map<String, String> relVersions, Map<String, String> devVersions,
			ReleaseManagerListener listener, String workingDirectory) throws JenkinsReleaseException {
		return prepare(project, releaseProperties, relVersions, devVersions, listener, workingDirectory, null, null);
	}

	@Override
	public String prepare(ProjectDescriptor project, Properties releaseProperties, Map<String, String> relVersions, Map<String, String> devVersions,
			ReleaseManagerListener listener, String workingDirectory, Map<String, String> environments, String executable) throws JenkinsReleaseException {
		String releaseId = project.getGroupId() + ":" + project.getArtifactId();

		// TODO get JenkinsReleaseDescriptor
		ReleaseDescriptor descriptor = getReleaseDescriptor(project, releaseProperties, relVersions, devVersions, environments, workingDirectory, executable);

		if (listener == null) {
			listener = new DefaultReleaseManagerListener();
			// listener.setUsername(releaseProperties.getProperty("release-by"));
		}

		getListeners().put(releaseId, listener);

		try {
			// TODO pas agregated modules instead of null
			prepareReleaseQueue.put(new PrepareReleaseProjectTask(releaseId, descriptor, (ReleaseManagerListener) listener));
		} catch (TaskQueueException e) {
			throw new JenkinsReleaseException("Failed to add prepare release task in queue.", e);
		}

		return releaseId;
	}

	@Override
	public void perform(String releaseId, File buildDirectory, String goals, String arguments, boolean useReleaseProfile, ReleaseManagerListener listener)
			throws JenkinsReleaseException {
		perform(releaseId, buildDirectory, goals, arguments, useReleaseProfile, listener, null);
	}

	@Override
	public void perform(String releaseId, File buildDirectory, String goals, String arguments, boolean useReleaseProfile, ReleaseManagerListener listener,
			LocalRepository repository) throws JenkinsReleaseException {
		ReleaseDescriptor descriptor = (ReleaseDescriptor) getPreparedReleases().get(releaseId);
		if (descriptor != null) {
			perform(releaseId, descriptor, buildDirectory, goals, arguments, useReleaseProfile, listener, repository);
		}
	}

	@Override
	public void perform(String releaseId, String workingDirectory, File buildDirectory, String goals, String arguments, boolean useReleaseProfile,
			ReleaseManagerListener listener) throws JenkinsReleaseException {
		ReleaseDescriptor descriptor = readReleaseDescriptor(workingDirectory);
		perform(releaseId, descriptor, buildDirectory, goals, arguments, useReleaseProfile, listener, null);
	}

	private void perform(String releaseId, ReleaseDescriptor descriptor, File buildDirectory, String goals, String arguments, boolean useReleaseProfile,
			ReleaseManagerListener listener, LocalRepository repository) throws JenkinsReleaseException {
		if (descriptor != null) {
			descriptor.setAdditionalArguments(arguments);
		}

		if (listener == null) {
			listener = new DefaultReleaseManagerListener();
			if (descriptor instanceof JenkinsReleaseDescriptor) {
				// listener.setUsername(((JenkinsReleaseDescriptor)
				// descriptor).getReleaseBy());
			}
		}

		getListeners().put(releaseId, listener);
		try {
			PerformReleaseProjectTask performReleaseProjectTask = new PerformReleaseProjectTask(releaseId, descriptor, buildDirectory,
					(ReleaseManagerListener) listener, "package", true);
			performReleaseQueue.put(performReleaseProjectTask);
		} catch (TaskQueueException e) {
			throw new JenkinsReleaseException("Failed to add perform release task in queue.", e);
		}

	}

	@Override
	public void rollback(String releaseId, String workingDirectory, ReleaseManagerListener listener) throws JenkinsReleaseException {
		ReleaseDescriptor descriptor = readReleaseDescriptor(workingDirectory);

		if (listener == null) {
			listener = new DefaultReleaseManagerListener();
			if (descriptor instanceof JenkinsReleaseDescriptor) {
				// listener.setUsername(((JenkinsReleaseDescriptor)
				// descriptor).getReleaseBy());
			}
		}

		rollback(releaseId, descriptor, listener);
	}

	private void rollback(String releaseId, ReleaseDescriptor descriptor, ReleaseManagerListener listener) throws JenkinsReleaseException {
		Task releaseTask = new RollbackReleaseProjectTask(releaseId, descriptor, (ReleaseManagerListener) listener);

		try {
			rollbackReleaseQueue.put(releaseTask);
		} catch (TaskQueueException e) {
			throw new JenkinsReleaseException("Failed to rollback release.", e);
		}
	}

	/**
	 * @see hudson.maven.release.core.JenkinsReleaseManager#getPreparedReleases()
	 */
	@Override
	public Map getPreparedReleases() {
		if (preparedReleases == null) {
			preparedReleases = new HashMap();
		}

		return preparedReleases;
	}

	/**
	 * @see hudson.maven.release.core.JenkinsReleaseManager#getReleaseResults()
	 */
	@Override
	public Map getReleaseResults() {
		if (releaseResults == null) {
			releaseResults = new HashMap();
		}

		return releaseResults;
	}

	private ReleaseDescriptor getReleaseDescriptor(ProjectDescriptor project, Properties releaseProperties, Map<String, String> relVersions,
			Map<String, String> devVersions, Map<String, String> environments, String workingDirectory, String executable) {
		JenkinsReleaseDescriptor descriptor = new JenkinsReleaseDescriptor();

		// release properties from the project
		descriptor.setWorkingDirectory(workingDirectory);
		descriptor.setScmSourceUrl(project.getScmUrl());

		// required properties
		descriptor.setScmReleaseLabel(releaseProperties.getProperty("scm-tag"));
		descriptor.setScmTagBase(releaseProperties.getProperty("scm-tagbase"));
		descriptor.setReleaseVersions(relVersions);
		descriptor.setDevelopmentVersions(devVersions);
		descriptor.setPreparationGoals(releaseProperties.getProperty("preparation-goals"));
		descriptor.setAdditionalArguments(releaseProperties.getProperty("arguments"));
		descriptor.setAddSchema(Boolean.valueOf(releaseProperties.getProperty("add-schema")));
		descriptor.setAutoVersionSubmodules(Boolean.valueOf(releaseProperties.getProperty("auto-version-submodules")));

		String useEditMode = releaseProperties.getProperty("use-edit-mode");
		if (BooleanUtils.toBoolean(useEditMode)) {
			descriptor.setScmUseEditMode(Boolean.valueOf(useEditMode));
		}

		String localRepository = project.getLocalRepository();

		if (localRepository != null) {
			String args = descriptor.getAdditionalArguments();

			if (StringUtils.isNotEmpty(args)) {
				descriptor.setAdditionalArguments(args + " \"-Dmaven.repo.local=" + localRepository + "\"");
			} else {
				descriptor.setAdditionalArguments("\"-Dmaven.repo.local=" + localRepository + "\"");
			}
		}

		// other properties
		if (releaseProperties.containsKey("scm-username")) {
			descriptor.setScmUsername(releaseProperties.getProperty("scm-username"));
		}
		if (releaseProperties.containsKey("scm-password")) {
			descriptor.setScmPassword(releaseProperties.getProperty("scm-password"));
		}
		if (releaseProperties.containsKey("scm-comment-prefix")) {
			descriptor.setScmCommentPrefix(releaseProperties.getProperty("scm-comment-prefix"));
		}
		if (releaseProperties.containsKey("use-release-profile")) {
			descriptor.setUseReleaseProfile(Boolean.valueOf(releaseProperties.getProperty("use-release-profile")));
		}

		// forced properties
		descriptor.setInteractive(false);

		// set environments
		descriptor.setEnvironments(environments);

		// release by
		descriptor.setReleaseBy(releaseProperties.getProperty("release-by"));

		return descriptor;
	}

	private ReleaseDescriptor readReleaseDescriptor(String workingDirectory) throws JenkinsReleaseException {
		ReleaseDescriptor descriptor = new JenkinsReleaseDescriptor();
		descriptor.setWorkingDirectory(workingDirectory);

		try {
			descriptor = releaseStore.read(descriptor);
		} catch (ReleaseDescriptorStoreException e) {
			throw new JenkinsReleaseException("Failed to parse descriptor file.", e);
		}

		return descriptor;
	}

	@Override
	public Map<String, ReleaseManagerListener> getListeners() {
		if (listeners == null) {
			listeners = new HashMap<String, ReleaseManagerListener>();
		}

		return listeners;
	}

	/**
	 * @see hudson.maven.release.core.JenkinsReleaseManager#sanitizeTagName(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public String sanitizeTagName(String scmUrl, String tagName) throws Exception {
		ScmRepository scmRepo = scmManager.makeScmRepository(scmUrl);
		ScmProvider scmProvider = scmManager.getProviderByRepository(scmRepo);
		return scmProvider.sanitizeTagName(tagName);
	}

	// @Override
	// public ReleaseListenerSummary getListener(String releaseId) {
	// ContinuumReleaseManagerListener listener =
	// (ContinuumReleaseManagerListener) getListeners().get(releaseId);
	//
	// if (listener != null) {
	// ReleaseListenerSummary listenerSummary = new ReleaseListenerSummary();
	// listenerSummary.setGoalName(listener.getGoalName());
	// listenerSummary.setError(listener.getError());
	// listenerSummary.setInProgress(listener.getInProgress());
	// listenerSummary.setState(listener.getState());
	// listenerSummary.setPhases(listener.getPhases());
	// listenerSummary.setCompletedPhases(listener.getCompletedPhases());
	// listenerSummary.setUsername(listener.getUsername());
	//
	// return listenerSummary;
	// }
	//
	// return null;
	// }
}
