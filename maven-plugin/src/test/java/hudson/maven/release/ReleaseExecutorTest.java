package hudson.maven.release;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderRequest;
import hudson.maven.MavenUtil;
import hudson.maven.release.tasks.PerformReleaseProjectTask;
import hudson.maven.release.tasks.PrepareReleaseProjectTask;
import hudson.maven.release.tasks.RollbackReleaseProjectTask;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.tasks.Maven.MavenInstallation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseManagerListener;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * These is the test set taken from the current continuum implementation and
 * adopted to the Jenkins implementation.
 * 
 * @author domi
 * @author Edwin Punzalan
 */
public class ReleaseExecutorTest {// extends PlexusTestCase {

	private ScmManager scmManager;

	private MavenEmbedder mvnEmbedder;
	private TaskExecutor prepareEx;
	private TaskExecutor performEx;
	private TaskExecutor rollbackEx;
	private JenkinsReleaseManager releaseManager;

	private String getBasedir() {
		return ".";
	}

	private File getWorkSpaceDir() {
		return new File(getBasedir(), "/target/dummy-ws/");
	}

	/**
	 * Setup the SCM (Subversion) and create a maven embedder.
	 * 
	 * @throws Exception
	 */
	@Before
	public void prepareTest() throws Exception {

		FileUtils.deleteDirectory(new File("target/scm-test"));

		File scmPath = new File(getBasedir(), "src/test/scm").getAbsoluteFile();
		File scmTargetPath = new File(getBasedir(), "target/scm-test").getAbsoluteFile();
		FileUtils.copyDirectory(scmPath, scmTargetPath);

		MavenEmbedderRequest req = getEmbedderRequest();
		mvnEmbedder = MavenUtil.createEmbedder(req);

		scmManager = mvnEmbedder.lookup(ScmManager.class);
		releaseManager = mvnEmbedder.lookup(JenkinsReleaseManager.class);
		prepareEx = mvnEmbedder.lookup(TaskExecutor.class, "prepare-release");
		performEx = mvnEmbedder.lookup(TaskExecutor.class, "perform-release");
		rollbackEx = mvnEmbedder.lookup(TaskExecutor.class, "rollback-release");

		Assert.assertNotNull("mvn embedder not available", mvnEmbedder);
		Assert.assertNotNull("ReleaseManager not available", releaseManager);

		Assert.assertNotNull("prepareExec not available", prepareEx);
		Assert.assertNotNull("prepareExec not available", performEx);
		Assert.assertNotNull("prepareExec not available", rollbackEx);

	}

	/**
	 * These tests build on top of each other (mainly the release number).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReleases() throws Exception {
		releaseSimpleProject();
		releaseAndRollbackProject();
		releaseSimpleProjectWithNextVersion();
		releasePerformWithExecutableInDescriptor();
		// TODO fix this test - currently failing
		releaseProjectWithDependencyOfCustomPackagingType();
	}

	public void releaseSimpleProject() throws Exception {

		String scmPath = new File(getBasedir(), "target/scm-test").getAbsolutePath().replace('\\', '/');
		File workDir = new File(getWorkSpaceDir(), "work-dir");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getWorkSpaceDir(), "test-dir");
		FileUtils.deleteDirectory(testDir);

		JenkinsReleaseDescriptor descriptor = new JenkinsReleaseDescriptor();
		descriptor.setInteractive(false);
		descriptor.setScmSourceUrl("scm:svn:file://localhost/" + scmPath + "/trunk");
		descriptor.setWorkingDirectory(workDir.getAbsolutePath());
		descriptor.setMavenEmbedderRequest(getEmbedderRequest());

		ScmRepository repository = getScmRepositorty(descriptor.getScmSourceUrl());
		ScmFileSet fileSet = new ScmFileSet(workDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		File pomFile = new File(workDir, "pom.xml");
		String pom = FileUtils.readFileToString(pomFile);
		Assert.assertTrue("Test dev version", pom.indexOf("<version>1.0-SNAPSHOT</version>") > 0);

		doPrepareWithNoError(descriptor);

		pom = FileUtils.readFileToString(pomFile);
		Assert.assertTrue("Test version increment", pom.indexOf("<version>1.1-SNAPSHOT</version>") > 0);

		repository = getScmRepositorty("scm:svn:file://localhost/" + scmPath + "/tags/test-artifact-1.0");
		fileSet = new ScmFileSet(testDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		pom = FileUtils.readFileToString(new File(testDir, "pom.xml"));
		Assert.assertTrue("Test released version", pom.indexOf("<version>1.0</version>") > 0);
	}

	public void releaseAndRollbackProject() throws Exception {
		String scmPath = new File(getBasedir(), "target/scm-test").getAbsolutePath().replace('\\', '/');
		File workDir = new File(getWorkSpaceDir(), "work-dir");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getWorkSpaceDir(), "test-dir");
		FileUtils.deleteDirectory(testDir);

		JenkinsReleaseDescriptor descriptor = new JenkinsReleaseDescriptor();
		descriptor.setInteractive(false);
		descriptor.setScmSourceUrl("scm:svn:file://localhost/" + scmPath + "/trunk");
		descriptor.setWorkingDirectory(workDir.getAbsolutePath());
		descriptor.setMavenEmbedderRequest(getEmbedderRequest());
		descriptor.setPreparationGoals("test");

		ScmRepository repository = getScmRepositorty(descriptor.getScmSourceUrl());
		ScmFileSet fileSet = new ScmFileSet(workDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		String pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		Assert.assertTrue("Test dev version", pom.indexOf("<version>1.1-SNAPSHOT</version>") > 0);

		doPrepareWithNoError(descriptor);

		pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		Assert.assertTrue("Test version increment", pom.indexOf("<version>1.2-SNAPSHOT</version>") > 0);

		repository = getScmRepositorty("scm:svn:file://localhost/" + scmPath + "/tags/test-artifact-1.1");
		fileSet = new ScmFileSet(testDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		pom = FileUtils.readFileToString(new File(testDir, "pom.xml"));
		Assert.assertTrue("Test released version", pom.indexOf("<version>1.1</version>") > 0);

		rollbackEx.executeTask(new RollbackReleaseProjectTask("testRelease", descriptor, null));

		pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		Assert.assertTrue("Test rollback version", pom.indexOf("<version>1.1-SNAPSHOT</version>") > 0);

		Assert.assertFalse("Test that release.properties has been cleaned", new File(workDir, "release.properties").exists());
		Assert.assertFalse("Test that backup file has been cleaned", new File(workDir, "pom.xml.releaseBackup").exists());

		// TODO when implemented already, check if tag was also removed
	}

	public void releaseSimpleProjectWithNextVersion() throws Exception {
		String scmPath = new File(getBasedir(), "target/scm-test").getAbsolutePath().replace('\\', '/');
		File workDir = new File(getWorkSpaceDir(), "work-dir");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getWorkSpaceDir(), "test-dir");
		FileUtils.deleteDirectory(testDir);

		JenkinsReleaseDescriptor descriptor = new JenkinsReleaseDescriptor();
		descriptor.setInteractive(false);
		descriptor.setScmSourceUrl("scm:svn:file://localhost/" + scmPath + "/trunk");
		descriptor.setWorkingDirectory(workDir.getAbsolutePath());
		descriptor.mapReleaseVersion("test-group:test-artifact", "2.0");
		descriptor.mapDevelopmentVersion("test-group:test-artifact", "2.1-SNAPSHOT");
		descriptor.setMavenEmbedderRequest(getEmbedderRequest());

		ScmRepository repository = getScmRepositorty(descriptor.getScmSourceUrl());
		ScmFileSet fileSet = new ScmFileSet(workDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		String pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		Assert.assertTrue("Test dev version", pom.indexOf("<version>1.1-SNAPSHOT</version>") > 0);

		doPrepareWithNoError(descriptor);

		pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		Assert.assertTrue("Test version increment", pom.indexOf("<version>2.1-SNAPSHOT</version>") > 0);

		repository = getScmRepositorty("scm:svn:file://localhost/" + scmPath + "/tags/test-artifact-2.0");
		fileSet = new ScmFileSet(testDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		pom = FileUtils.readFileToString(new File(testDir, "pom.xml"));
		Assert.assertTrue("Test released version", pom.indexOf("<version>2.0</version>") > 0);

		// * CONTINUUM-2559
		performEx.executeTask(getPerformTask("testRelease", descriptor, new File(getBasedir(), "target/test-classes/build-dir")));

		ReleaseResult result = (ReleaseResult) releaseManager.getReleaseResults().get("testRelease");
		if (result.getResultCode() != ReleaseResult.SUCCESS) {
			Assert.fail("Error in release:perform. Release output follows:\n" + result.getOutput());
		}

	}

	public void releasePerformWithExecutableInDescriptor() throws Exception {
		String scmPath = new File(getBasedir(), "target/scm-test").getAbsolutePath().replace('\\', '/');
		File workDir = new File(getWorkSpaceDir(), "work-dir");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getWorkSpaceDir(), "test-dir");
		FileUtils.deleteDirectory(testDir);

		JenkinsReleaseDescriptor descriptor = new JenkinsReleaseDescriptor();
		descriptor.setInteractive(false);
		descriptor.setScmSourceUrl("scm:svn:file://localhost/" + scmPath + "/trunk");
		descriptor.setWorkingDirectory(workDir.getAbsolutePath());
		descriptor.setMavenEmbedderRequest(getEmbedderRequest());

		ScmRepository repository = getScmRepositorty(descriptor.getScmSourceUrl());
		ScmFileSet fileSet = new ScmFileSet(workDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		String pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		assertTrue("Test dev version", pom.indexOf("<version>2.1-SNAPSHOT</version>") > 0);

		doPrepareWithNoError(descriptor);

		pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		assertTrue("Test version increment", pom.indexOf("<version>2.2-SNAPSHOT</version>") > 0);

		repository = getScmRepositorty("scm:svn:file://localhost/" + scmPath + "/tags/test-artifact-2.1");
		fileSet = new ScmFileSet(testDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		pom = FileUtils.readFileToString(new File(testDir, "pom.xml"));
		assertTrue("Test released version", pom.indexOf("<version>2.1</version>") > 0);

		File file = new File(descriptor.getWorkingDirectory(), "release.properties");
		assertTrue("release.properties file does not exist", file.exists());

		Properties properties = new Properties();

		InputStream inStream = null;
		OutputStream outStream = null;

		try {
			inStream = new FileInputStream(file);

			properties.load(inStream);
			// TODO how to define a different release executable (mvn) via
			// Jenkins - needed?
			properties.setProperty("build.executable", "test/executable/mvn");

			outStream = new FileOutputStream(file);

			properties.store(outStream, "release configuration");
		} finally {
			IOUtil.close(inStream);
		}

		performEx.executeTask(getPerformTask("testRelease", descriptor, new File(getWorkSpaceDir(), "build-dir")));

		ReleaseResult result = (ReleaseResult) releaseManager.getReleaseResults().get("testRelease");

		assertTrue("start time not set!", result.getStartTime() > 0);
		assertTrue("end time not set!", result.getEndTime() > 0);
		assertEquals("release not success", ReleaseResult.SUCCESS, result.getResultCode());

		// TODO enable when external executable is implemented.
		if (!result.getOutput().replace("\\", "/").contains("test/executable/mvn")) {
			System.out.println("##########################################################################################");
			System.out.println("Test not fully implemented yet! (enable when defining external executable is implemented)!");
			System.out.println("##########################################################################################");
			// fail("Error in release:perform. Missing executable");
		}
	}

	public void releaseProjectWithDependencyOfCustomPackagingType() throws Exception {
		String scmPath = new File(getBasedir(), "target/scm-test/continuum-1814").getAbsolutePath().replace('\\', '/');
		File workDir = new File(getWorkSpaceDir(), "continuum-1814");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getWorkSpaceDir(), "test-dir");
		FileUtils.deleteDirectory(testDir);

		JenkinsReleaseDescriptor descriptor = new JenkinsReleaseDescriptor();
		descriptor.setInteractive(false);
		descriptor.setScmSourceUrl("scm:svn:file://localhost/" + scmPath + "/trunk");
		descriptor.setWorkingDirectory(workDir.getAbsolutePath());
		descriptor.setMavenEmbedderRequest(getEmbedderRequest());

		ScmRepository repository = getScmRepositorty(descriptor.getScmSourceUrl());
		ScmFileSet fileSet = new ScmFileSet(workDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		String pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		assertTrue("Test dev version", pom.indexOf("<version>1.6-SNAPSHOT</version>") > 0);

		doPrepareWithNoError(descriptor);

		pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		assertTrue("Test version increment", pom.indexOf("<version>1.7-SNAPSHOT</version>") > 0);

		repository = getScmRepositorty("scm:svn:file://localhost/" + scmPath + "/tags/continuum-1814-1.6");
		fileSet = new ScmFileSet(testDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		pom = FileUtils.readFileToString(new File(testDir, "pom.xml"));
		assertTrue("Test released version", pom.indexOf("<version>1.6</version>") > 0);

		performEx.executeTask(getPerformTask("testRelease", descriptor, new File(getWorkSpaceDir(), "build-dir")));

		ReleaseResult result = (ReleaseResult) releaseManager.getReleaseResults().get("testRelease");
		if (result.getResultCode() != ReleaseResult.SUCCESS) {
			fail("Error in release:perform. Release output follows:\n" + result.getOutput());
		}

	}

	/**
	 * The project tried to release in this test has two modules, but one if it
	 * is only references in a profile called 'all'. After execution, all
	 * modules and parent pom must have the same version.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReleaseProjectWithProfile() throws Exception {
		String scmPath = new File(getBasedir(), "target/scm-test/continuum-2610").getAbsolutePath().replace('\\', '/');
		// checkout to this directory
		File workDir = new File(getWorkSpaceDir(), "continuum-2610");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getWorkSpaceDir(), "test-dir");
		FileUtils.deleteDirectory(testDir);

		JenkinsReleaseDescriptor descriptor = new JenkinsReleaseDescriptor();
		descriptor.setInteractive(false);
		descriptor.setScmSourceUrl("scm:svn:file://localhost/" + scmPath + "/trunk");
		descriptor.setWorkingDirectory(workDir.getAbsolutePath());

		// TODO get a better way to define the profiles (only one location!)
		descriptor.setAdditionalArguments("-Pall");
		MavenEmbedderRequest embedderRequest = getEmbedderRequest().setProfiles("all");
		descriptor.setMavenEmbedderRequest(embedderRequest);

		ScmRepository repository = getScmRepositorty(descriptor.getScmSourceUrl());
		ScmFileSet fileSet = new ScmFileSet(workDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		String pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		final String versionStrBefor = "<version>1.0-SNAPSHOT</version>";
		assertTrue("Test root dev version", pom.indexOf(versionStrBefor) > 0);
		String moduleAPom = FileUtils.readFileToString(new File(workDir, "module-A/pom.xml"));
		assertTrue("Test module A dev version", moduleAPom.indexOf(versionStrBefor) > 0);
		String moduleBPom = FileUtils.readFileToString(new File(workDir, "module-B/pom.xml"));
		assertTrue("Test module B dev version", moduleBPom.indexOf(versionStrBefor) > 0);

		doPrepareWithNoError(descriptor);

		pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		final String newSnapshotVersionStr = "<version>1.1-SNAPSHOT</version>";
		assertTrue("Test root version increment", pom.indexOf(newSnapshotVersionStr) > 0);
		moduleAPom = FileUtils.readFileToString(new File(workDir, "module-A/pom.xml"));
		assertTrue("Test module A version increment", moduleAPom.indexOf(newSnapshotVersionStr) > 0);
		moduleBPom = FileUtils.readFileToString(new File(workDir, "module-B/pom.xml"));
		assertTrue("Test module B version increment", moduleBPom.indexOf(newSnapshotVersionStr) > 0);

		repository = getScmRepositorty("scm:svn:file://localhost/" + scmPath + "/tags/continuum-2610-1.0");
		fileSet = new ScmFileSet(testDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		pom = FileUtils.readFileToString(new File(testDir, "pom.xml"));
		final String versionStrAfter = "<version>1.0</version>";
		assertTrue("Test root released version", pom.indexOf(versionStrAfter) > 0);
		moduleAPom = FileUtils.readFileToString(new File(testDir, "module-A/pom.xml"));
		assertTrue("Test module A released version", moduleAPom.indexOf(versionStrAfter) > 0);
		moduleBPom = FileUtils.readFileToString(new File(testDir, "module-B/pom.xml"));
		assertTrue("Test module B released version", moduleBPom.indexOf(versionStrAfter) > 0);
	}

	private void doPrepareWithNoError(ReleaseDescriptor descriptor) throws TaskExecutionException {
		prepareEx.executeTask(getPrepareTask("testRelease", descriptor));

		ReleaseResult result = (ReleaseResult) releaseManager.getReleaseResults().get("testRelease");
		if (result.getResultCode() != ReleaseResult.SUCCESS) {
			Assert.fail("Error in release:prepare. Release output follows:\n" + result.getOutput());
		}
	}

	private Task getPrepareTask(String releaseId, ReleaseDescriptor descriptor) {
		return new PrepareReleaseProjectTask(releaseId, descriptor, null);
	}

	private Task getPerformTask(String releaseId, ReleaseDescriptor descriptor, File buildDir) {
		return new PerformReleaseProjectTask(releaseId, descriptor, buildDir, (ReleaseManagerListener) null, "package", true);
	}

	private ScmRepository getScmRepositorty(String scmUrl) throws ScmRepositoryException, NoSuchScmProviderException {
		ScmRepository repository = scmManager.makeScmRepository(scmUrl.trim());
		repository.getProviderRepository().setPersistCheckout(true);
		return repository;
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
