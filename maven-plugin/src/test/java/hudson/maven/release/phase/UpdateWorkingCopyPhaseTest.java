package hudson.maven.release.phase;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderRequest;
import hudson.maven.MavenUtil;
import hudson.maven.release.core.JenkinsReleaseDescriptor;
import hudson.maven.release.core.phase.UpdateWorkingCopyPhase;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.tasks.Maven.MavenInstallation;

import java.io.File;
import java.util.Properties;

import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.phase.ReleasePhase;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class UpdateWorkingCopyPhaseTest {// extends PlexusInSpringTestCase {
	private UpdateWorkingCopyPhase phase;

	private String getBasedir() {
		return new File(".").getAbsolutePath();
	}

	private File getWorkSpaceDir() {
		return new File(getBasedir(), "/target/dummy-ws/");
	}

	@Before
	public void setUp() throws Exception {

		MavenEmbedderRequest req = getEmbedderRequest();
		MavenEmbedder mvnEmbedder = MavenUtil.createEmbedder(req);

		phase = (UpdateWorkingCopyPhase) mvnEmbedder.lookup(ReleasePhase.ROLE, "update-working-copy");

		File scmPathFile = new File(getBasedir(), "src/test/scm").getAbsoluteFile();
		File scmTargetPathFile = new File(getBasedir(), "/target/scm-test").getAbsoluteFile();
		FileUtils.deleteDirectory(scmTargetPathFile);
		FileUtils.copyDirectoryStructure(scmPathFile, scmTargetPathFile);
	}

	@Test
	public void testWorkingDirDoesNotExist() throws Exception {
		assertNotNull(phase);

		FileUtils.deleteDirectory(getWorkSpaceDir());

		JenkinsReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

		File workingDirectory = new File(releaseDescriptor.getWorkingDirectory());

		// assert no working directory yet
		assertFalse("the working directory already exists", workingDirectory.exists());

		phase.execute(releaseDescriptor, new Settings(), null);

		assertTrue("the wokring directory does not exist after execution", workingDirectory.exists());
	}

	@Test
	public void testWorkingDirAlreadyExistsWithProjectCheckout() throws Exception {
		assertNotNull(phase);

		FileUtils.mkdir(getWorkSpaceDir().getAbsolutePath());
		new File(getWorkSpaceDir(), "touch.txt").getAbsoluteFile().createNewFile(); // simulate
		// an
		// existing
		// checkout

		JenkinsReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

		File workingDirectory = new File(releaseDescriptor.getWorkingDirectory());

		// assert working directory already exists with project checkout
		assertTrue("working dir does not exist befor execution", workingDirectory.exists());
		assertTrue("there are no files in the working directory", workingDirectory.listFiles().length > 0);

		phase.execute(releaseDescriptor, new Settings(), null);

		assertTrue("wokring dir does not exist anymore after execution", workingDirectory.exists());
	}

	@Test
	public void testWorkingDirAlreadyExistsNoProjectCheckout() throws Exception {
		assertNotNull(phase);

		FileUtils.mkdir(getWorkSpaceDir().getAbsolutePath());

		JenkinsReleaseDescriptor releaseDescriptor = createReleaseDescriptor();

		File workingDirectory = new File(releaseDescriptor.getWorkingDirectory());
		FileUtils.deleteDirectory(workingDirectory);
		workingDirectory.mkdirs();

		// assert empty working directory
		assertTrue("working directory does not exist", workingDirectory.exists());
		assertTrue("there are already/still files in the wokring dir", workingDirectory.listFiles().length == 0);

		phase.execute(releaseDescriptor, new Settings(), null);

		assertTrue("the wokring directory does not exist anymore", workingDirectory.exists());
	}

	private JenkinsReleaseDescriptor createReleaseDescriptor() {
		// project source and working directory paths
		String projectUrl = getBasedir() + "/target/scm-test/trunk";
		String workingDirPath = getWorkSpaceDir() + "/updateWorkingCopy_working-directory";

		// create release descriptor
		JenkinsReleaseDescriptor releaseDescriptor = new JenkinsReleaseDescriptor();
		releaseDescriptor.setScmSourceUrl("scm:svn:file://localhost/" + projectUrl);
		releaseDescriptor.setWorkingDirectory(workingDirPath);

		// descriptor.setInteractive(false);
		// descriptor.setMavenEmbedderRequest(getEmbedderRequest());

		return releaseDescriptor;
	}

	private MavenEmbedderRequest getEmbedderRequest() {
		TaskListener listener = new StreamBuildListener(System.out);

		MavenInstallation m = new MavenInstallation("preinstalled", "/usr/share/maven/");
		String profiles = null;
		Properties systemProperties = new Properties();
		String privateRepository = null;
		File settingsLoc = null;

		MavenEmbedderRequest req = new MavenEmbedderRequest(listener, m != null ? m.getHomeDir() : null, profiles, systemProperties, privateRepository,
				settingsLoc);
		return req;
	}

}
