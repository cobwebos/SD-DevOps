package hudson.maven.release.executors;

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

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenUtil;
import hudson.maven.release.JenkinsReleaseDescriptor;
import hudson.maven.release.JenkinsReleaseException;
import hudson.maven.release.JenkinsReleaseManager;
import hudson.maven.release.tasks.ReleaseProjectTask;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.ReleaseResult;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author domi
 * @author Edwin Punzalan
 * @version $Id: AbstractReleaseTaskExecutor.java 729313 2008-12-24 13:41:11Z
 *          olamy $
 */
public abstract class AbstractReleaseTaskExecutor implements ReleaseTaskExecutor {
	/**
	 * @plexus.requirement
	 */
	protected JenkinsReleaseManager continuumReleaseManager;

	/**
	 * @plexus.requirement
	 */
	protected ReleaseManager releaseManager;

	/**
	 * @plexus.requirement
	 */
	private MavenSettingsBuilder settingsBuilder;

	protected Settings settings;

	private long startTime;

	public void executeTask(Task task) throws TaskExecutionException {
		ReleaseProjectTask releaseTask = (ReleaseProjectTask) task;
		setUp(releaseTask);
		execute(releaseTask);
	}

	protected void setUp(ReleaseProjectTask releaseTask) throws TaskExecutionException {
		// actual release execution start time
		setStartTime(System.currentTimeMillis());

		try {
			// make sure settings is re-read each time
			settings = getSettings();
		} catch (JenkinsReleaseException e) {
			ReleaseResult result = createReleaseResult();

			result.appendError(e);

			continuumReleaseManager.getReleaseResults().put(releaseTask.getReleaseId(), result);

			releaseTask.getListener().error(e.getMessage());

			throw new TaskExecutionException("Failed to build reactor projects.", e);
		}
	}

	protected abstract void execute(ReleaseProjectTask releaseTask) throws TaskExecutionException;

	protected List<MavenProject> getMavenProjects(JenkinsReleaseDescriptor descriptor) {
		List<MavenProject> mavenProjects = Collections.emptyList();
		try {
			MavenEmbedder mvnEmbedder = MavenUtil.createEmbedder(descriptor.getMavenEmbedderRequest());
			mavenProjects = mvnEmbedder.readProjects(descriptor.getProjectDescriptorFile(), true);
		} catch (MavenEmbedderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProjectBuildingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mavenProjects;
	}

	private Settings getSettings() throws JenkinsReleaseException {
		try {
			settings = settingsBuilder.buildSettings(false);
		} catch (IOException e) {
			throw new JenkinsReleaseException("Failed to get Maven Settings.", e);
		} catch (XmlPullParserException e) {
			throw new JenkinsReleaseException("Failed to get Maven Settings.", e);
		}

		return settings;
	}

	protected ReleaseResult createReleaseResult() {
		ReleaseResult result = new ReleaseResult();

		result.setStartTime(getStartTime());

		result.setEndTime(System.currentTimeMillis());

		return result;
	}

	protected long getStartTime() {
		return startTime;
	}

	protected void setStartTime(long startTime) {
		this.startTime = startTime;
	}
}
