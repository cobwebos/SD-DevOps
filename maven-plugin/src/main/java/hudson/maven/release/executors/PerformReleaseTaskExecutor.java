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

import hudson.maven.release.JenkinsReleaseDescriptor;
import hudson.maven.release.tasks.PerformReleaseProjectTask;
import hudson.maven.release.tasks.ReleaseProjectTask;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;

/**
 * @author domi
 */
public class PerformReleaseTaskExecutor extends AbstractReleaseTaskExecutor {

	public void execute(ReleaseProjectTask task) throws TaskExecutionException {
		PerformReleaseProjectTask performTask = (PerformReleaseProjectTask) task;

		JenkinsReleaseDescriptor descriptor = performTask.getDescriptor();
		descriptor.setUseReleaseProfile(performTask.isUseReleaseProfile());
		descriptor.setPerformGoals(performTask.getGoals());
		descriptor.setCheckoutDirectory(performTask.getBuildDirectory().getAbsolutePath());

		ReleaseEnvironment releaseEnvironment = new DefaultReleaseEnvironment();
		releaseEnvironment.setSettings(settings);
		// TODO implement a default MavenExecutor with Embedded Maven
		// "invoker" there is something wrong here, the invoker gets registered
		// as 'forked-path'???
		releaseEnvironment.setMavenExecutorId("forked-path");
		releaseEnvironment.setMavenHome(descriptor.getMavenEmbedderRequest().getMavenHome());

		List<MavenProject> mavenProjects = this.getMavenProjects(descriptor);

		ReleaseResult result = releaseManager.performWithResult(descriptor, releaseEnvironment, mavenProjects, performTask.getListener());

		// override to show the actual start time
		result.setStartTime(getStartTime());

		if (result.getResultCode() == ReleaseResult.SUCCESS) {
			continuumReleaseManager.getPreparedReleases().remove(performTask.getReleaseId());
		}

		continuumReleaseManager.getReleaseResults().put(performTask.getReleaseId(), result);
	}

}
