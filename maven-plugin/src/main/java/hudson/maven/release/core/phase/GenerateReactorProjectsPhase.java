package hudson.maven.release.core.phase;

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
import hudson.maven.release.core.JenkinsReleaseDescriptor;

import java.io.IOException;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.phase.AbstractReleasePhase;

/**
 * @author domi
 */
public class GenerateReactorProjectsPhase extends AbstractReleasePhase {

	@Override
	public ReleaseResult execute(ReleaseDescriptor releaseDescriptor, Settings settings, List<MavenProject> reactorProjects) throws ReleaseExecutionException,
			ReleaseFailureException {

		JenkinsReleaseDescriptor jenkinsReleaseDescriptor = (JenkinsReleaseDescriptor) releaseDescriptor;
		ReleaseResult result = new ReleaseResult();

		try {
			MavenEmbedder mvnEmbedder = MavenUtil.createEmbedder(jenkinsReleaseDescriptor.getMavenEmbedderRequest());
			List<MavenProject> mavenProjects = mvnEmbedder.readProjects(jenkinsReleaseDescriptor.getProjectDescriptorFile(), true);
			reactorProjects.addAll(mavenProjects);
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

		result.setResultCode(ReleaseResult.SUCCESS);

		return result;
	}

	@Override
	public ReleaseResult simulate(ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects) throws ReleaseExecutionException,
			ReleaseFailureException {
		return execute(releaseDescriptor, settings, reactorProjects);
	}

	@Override
	public ReleaseResult execute(ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List reactorProjects)
			throws ReleaseExecutionException, ReleaseFailureException {
		return execute(releaseDescriptor, releaseEnvironment.getSettings(), reactorProjects);
	}

	@Override
	public ReleaseResult simulate(ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List reactorProjects)
			throws ReleaseExecutionException, ReleaseFailureException {
		return execute(releaseDescriptor, releaseEnvironment.getSettings(), reactorProjects);
	}

}
