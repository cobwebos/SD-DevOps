package hudson.maven.release;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderRequest;
import hudson.maven.MavenInformation;
import hudson.maven.MavenUtil;
import hudson.maven.release.tasks.PerformReleaseProjectTask;
import hudson.maven.release.tasks.PrepareReleaseProjectTask;
import hudson.maven.release.tasks.RollbackReleaseProjectTask;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.tasks.Maven.MavenInstallation;

import java.io.File;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

	@Before
	public void prepareSCM() throws Exception {
		// super.setUp();
		File scmPath = new File(getBasedir(), "src/test/scm").getAbsoluteFile();
		File scmTargetPath = new File(getBasedir(), "target/scm-test").getAbsoluteFile();
		FileUtils.copyDirectory(scmPath, scmTargetPath);

		MavenEmbedderRequest req = getEmbedderRequest();
		mvnEmbedder = MavenUtil.createEmbedder(req);

		scmManager = mvnEmbedder.lookup(ScmManager.class);
		releaseManager = mvnEmbedder.lookup(JenkinsReleaseManager.class);
		System.out.println("->" + releaseManager);
		prepareEx = mvnEmbedder.lookup(TaskExecutor.class, "prepare-release");
		performEx = mvnEmbedder.lookup(TaskExecutor.class, "perform-release");
		rollbackEx = mvnEmbedder.lookup(TaskExecutor.class, "rollback-release");

		Assert.assertNotNull("mvn embedder not available", mvnEmbedder);
		Assert.assertNotNull("ReleaseManager not available", releaseManager);

		Assert.assertNotNull("prepareExec not available", prepareEx);
		Assert.assertNotNull("prepareExec not available", performEx);
		Assert.assertNotNull("prepareExec not available", rollbackEx);

	}

	@Test
	public void testReleases() throws Exception {
		releaseSimpleProject();
		releaseAndRollbackProject();
		releaseSimpleProjectWithNextVersion();
		// releasePerformWithExecutableInDescriptor();
		// releaseProjectWithDependencyOfCustomPackagingType();
		// releaseProjectWithProfile();
	}

	public void releaseSimpleProject() throws Exception {

		String scmPath = new File(getBasedir(), "target/scm-test").getAbsolutePath().replace('\\', '/');
		File workDir = new File(getBasedir(), "target/test-classes/work-dir");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getBasedir(), "target/test-classes/test-dir");
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
		File workDir = new File(getBasedir(), "target/test-classes/work-dir");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getBasedir(), "target/test-classes/test-dir");
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

		// @todo when implemented already, check if tag was also removed
	}

	public void releaseSimpleProjectWithNextVersion() throws Exception {
		String scmPath = new File(getBasedir(), "target/scm-test").getAbsolutePath().replace('\\', '/');
		File workDir = new File(getBasedir(), "target/test-classes/work-dir");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getBasedir(), "target/test-classes/test-dir");
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
