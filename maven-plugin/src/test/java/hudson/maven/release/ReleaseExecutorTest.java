package hudson.maven.release;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderRequest;
import hudson.maven.MavenUtil;
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
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReleaseExecutorTest {

	private ScmManager scmManager;

	private MavenEmbedder mvnEmbedder;
	private Object prepareEx;
	private Object performEx;
	private Object rollbackEx;
	private Object relMgr;

	private String getBasedir() {
		return ".";
	}

	@Before
	public void prepareSCM() throws Exception {
		File scmPath = new File(getBasedir(), "src/test/scm").getAbsoluteFile();
		File scmTargetPath = new File(getBasedir(), "target/scm-test").getAbsoluteFile();
		FileUtils.copyDirectory(scmPath, scmTargetPath);
	}

	@Before
	public void prepareMVNContext() throws Exception {
		TaskListener listener = new StreamBuildListener(System.out);

		MavenInstallation m = null;
		String profiles = null;
		Properties systemProperties = new Properties();
		String privateRepository = null;
		File settingsLoc = null;

		MavenEmbedderRequest req = new MavenEmbedderRequest(listener, m != null ? m.getHomeDir() : null, profiles, systemProperties, privateRepository,
				settingsLoc);
		mvnEmbedder = MavenUtil.createEmbedder(req);

		scmManager = mvnEmbedder.lookup(ScmManager.class);
		prepareEx = mvnEmbedder.lookup(TaskExecutor.class.getName(), "prepare-release");
		performEx = mvnEmbedder.lookup(TaskExecutor.class.getName(), "perform-release");
		rollbackEx = mvnEmbedder.lookup(TaskExecutor.class.getName(), "rollback-release");
		relMgr = mvnEmbedder.lookup("ReleaseManager");

		Assert.assertNotNull("mvn embedder not available", mvnEmbedder);
		Assert.assertNotNull("prepareExec not available", prepareEx);
		Assert.assertNotNull("prepareExec not available", performEx);
		Assert.assertNotNull("prepareExec not available", rollbackEx);
		Assert.assertNotNull("prepareExec not available", relMgr);

	}

	@Test
	public void testSimpleRelease() throws Exception {

		String scmPath = new File(getBasedir(), "target/scm-test").getAbsolutePath().replace('\\', '/');
		File workDir = new File(getBasedir(), "target/test-classes/work-dir");
		FileUtils.deleteDirectory(workDir);
		File testDir = new File(getBasedir(), "target/test-classes/test-dir");
		FileUtils.deleteDirectory(testDir);

		JenkinsReleaseDescriptor descriptor = new JenkinsReleaseDescriptor();
		descriptor.setInteractive(false);
		descriptor.setScmSourceUrl("scm:svn:file://localhost/" + scmPath + "/trunk");
		descriptor.setWorkingDirectory(workDir.getAbsolutePath());

		ScmRepository repository = getScmRepositorty(descriptor.getScmSourceUrl());
		ScmFileSet fileSet = new ScmFileSet(workDir);
		scmManager.getProviderByRepository(repository).checkOut(repository, fileSet, (ScmVersion) null);

		String pom = FileUtils.readFileToString(new File(workDir, "pom.xml"));
		Assert.assertTrue("Test dev version", pom.indexOf("<version>1.0-SNAPSHOT</version>") > 0);

	}

	private ScmRepository getScmRepositorty(String scmUrl) throws ScmRepositoryException, NoSuchScmProviderException {
		ScmRepository repository = scmManager.makeScmRepository(scmUrl.trim());
		repository.getProviderRepository().setPersistCheckout(true);
		return repository;
	}

}
