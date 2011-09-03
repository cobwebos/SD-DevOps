/*
 * The MIT License
 * 
 * Copyright (c) 2010, NDS Group Ltd., James Nord
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.maven.release;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.AbstractMavenProject;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.Messages;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.security.Permission;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.RunList;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a {@link MavenBuild} to be able to run the <a
 * href="http://maven.apache.org/plugins/maven-release-plugin/">maven release
 * plugin</a> on demand, with the ability to auto close a Nexus Pro Staging Repo
 * 
 * @author domi
 * @author James Nord
 */
public class MvnReleaseBuildWrapper extends BuildWrapper {

	private transient Logger log = LoggerFactory.getLogger(MvnReleaseBuildWrapper.class);

	private transient boolean doRelease = false;
	private transient boolean closeNexusStage = true;

	private transient String releaseVersion;
	private transient String developmentVersion;

	private transient boolean appendHudsonBuildNumber;
	private transient String repoDescription;
	private transient String scmUsername;
	private transient String scmPassword;
	private transient String scmCommentPrefix;
	private transient String scmTag;

	private transient boolean appendHusonUserName;
	private transient String hudsonUserName;

	public String releaseGoals = DescriptorImpl.DEFAULT_RELEASE_GOALS;
	public boolean selectCustomScmCommentPrefix = DescriptorImpl.DEFAULT_SELECT_CUSTOM_SCM_COMMENT_PREFIX;
	public boolean selectAppendHudsonUsername = DescriptorImpl.DEFAULT_SELECT_APPEND_HUDSON_USERNAME;

	@DataBoundConstructor
	public MvnReleaseBuildWrapper(String releaseGoals, boolean selectCustomScmCommentPrefix, boolean selectAppendHudsonUsername) {
		super();
		this.releaseGoals = releaseGoals;
	}

	@Override
	public Action getProjectAction(@SuppressWarnings("rawtypes") AbstractProject job) {
		return new MvnReleaseAction((MavenModuleSet) job, selectCustomScmCommentPrefix, selectAppendHudsonUsername);
	}

	@Override
	public Environment setUp(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException,
			InterruptedException {

		if (!doRelease) {
			// we are not performing a release so don't need a custom tearDown.
			return new Environment() {
				/** intentionally blank */
			};
		}

		// TODO how to pass the release information to Mvn3ReleaseBuiler?
		
		build.addAction(new MvnReleaseBadgeAction("Release - " + releaseVersion));

		return new Environment() {

			@Override
			public boolean tearDown(@SuppressWarnings("rawtypes") AbstractBuild bld, BuildListener lstnr) throws IOException, InterruptedException {
				boolean retVal = true;

				if (bld.getResult().isBetterOrEqualTo(Result.SUCCESS)) {
					// keep this build.
					lstnr.getLogger().println("[MvnRelease] marking build to keep unti the next release build");
					bld.keepLog();

					for (Run run : (RunList<? extends Run>) (bld.getProject().getBuilds())) {
						MvnReleaseBadgeAction a = run.getAction(MvnReleaseBadgeAction.class);
						if (a != null && run.getResult() == Result.SUCCESS) {
							if (bld.getNumber() != run.getNumber()) {
								lstnr.getLogger().println("[MvnRelease] removing keep build from build " + run.getNumber());
								run.keepLog(false);
								break;
							}
						}
					}

				}

				return retVal;
			}
		};
	}
	
	public String getReleaseVersion(){
		return releaseVersion;
	}

	void enableRelease() {
		doRelease = true;
	}

	public void setReleaseVersion(String releaseVersion) {
		this.releaseVersion = releaseVersion;
	}

	public void setDevelopmentVersion(String developmentVersion) {
		this.developmentVersion = developmentVersion;
	}

	public void setAppendHudsonBuildNumber(boolean appendHudsonBuildNumber) {
		this.appendHudsonBuildNumber = appendHudsonBuildNumber;
	}

	public void setCloseNexusStage(boolean closeNexusStage) {
		this.closeNexusStage = closeNexusStage;
	}

	public void setRepoDescription(String repoDescription) {
		this.repoDescription = repoDescription;
	}

	public void setScmUsername(String scmUsername) {
		this.scmUsername = scmUsername;
	}

	public void setScmPassword(String scmPassword) {
		this.scmPassword = scmPassword;
	}

	public void setScmCommentPrefix(String scmCommentPrefix) {
		this.scmCommentPrefix = scmCommentPrefix;
	}

	/**
	 * @param scmTag
	 *            the scmTag to set
	 */
	public void setScmTag(String scmTag) {
		this.scmTag = scmTag;
	}

	public void setAppendHusonUserName(boolean appendHusonUserName) {
		this.appendHusonUserName = appendHusonUserName;
	}

	public boolean isSelectCustomScmCommentPrefix() {
		return selectCustomScmCommentPrefix;
	}

	public void setSelectCustomScmCommentPrefix(boolean selectCustomScmCommentPrefix) {
		this.selectCustomScmCommentPrefix = selectCustomScmCommentPrefix;
	}

	public boolean isSelectAppendHudsonUsername() {
		return selectAppendHudsonUsername;
	}

	public void setSelectAppendHudsonUsername(boolean selectAppendHudsonUsername) {
		this.selectAppendHudsonUsername = selectAppendHudsonUsername;
	}

	public void setHudsonUserName(String hudsonUserName) {
		this.hudsonUserName = hudsonUserName;
	}

	private MavenModuleSet getModuleSet(AbstractBuild<?, ?> build) {
		if (build instanceof MavenBuild) {
			MavenBuild m2Build = (MavenBuild) build;
			MavenModule mm = m2Build.getProject();
			MavenModuleSet mmSet = mm.getParent();
			return mmSet;
		} else if (build instanceof MavenModuleSetBuild) {
			MavenModuleSetBuild m2moduleSetBuild = (MavenModuleSetBuild) build;
			MavenModuleSet mmSet = m2moduleSetBuild.getProject();
			return mmSet;
		} else {
			return null;
		}
	}

	public static boolean hasReleasePermission(@SuppressWarnings("rawtypes") AbstractProject job) {
		return job.hasPermission(DescriptorImpl.CREATE_RELEASE);
	}

	public static void checkReleasePermission(@SuppressWarnings("rawtypes") AbstractProject job) {
		job.checkPermission(DescriptorImpl.CREATE_RELEASE);
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {

		public static final String DEFAULT_RELEASE_GOALS = "-Dresume=false release:prepare release:perform"; //$NON-NLS-1$
		public static final Permission CREATE_RELEASE = new Permission(Item.PERMISSIONS, "Release", //$NON-NLS-1$
				Messages._CreateReleasePermission_Description(), Hudson.ADMINISTER);

		public static final boolean DEFAULT_SELECT_CUSTOM_SCM_COMMENT_PREFIX = false;
		public static final boolean DEFAULT_SELECT_APPEND_HUDSON_USERNAME = false;

		public DescriptorImpl() {
			super(MvnReleaseBuildWrapper.class);
			load();
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return (item instanceof AbstractMavenProject);
		}

		@Override
		public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
			save();
			return true; // indicate that everything is good so far
		}

		@Override
		public String getDisplayName() {
			return Messages.MvnReleaseWrapper_DisplayName();
		}

		/**
		 * Checks if the Nexus URL exists and we can authenticate against it.
		 * TODO add support for nexus
		 */
		/*
		 * public FormValidation doUrlCheck(@QueryParameter String urlValue,
		 * final @QueryParameter String usernameValue, final @QueryParameter
		 * String passwordValue) throws IOException, ServletException { // this
		 * method can be used to check if a file exists anywhere in the file
		 * system, // so it should be protected. if
		 * (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) { return
		 * FormValidation.ok(); }
		 * 
		 * urlValue = Util.fixEmptyAndTrim(urlValue); if (urlValue == null) {
		 * return FormValidation.ok(); } final String testURL; if
		 * (urlValue.endsWith("/")) { testURL = urlValue.substring(0,
		 * urlValue.length() - 1); } else { testURL = urlValue; } URL url =
		 * null; try { url = new URL(testURL); if
		 * (!(url.getProtocol().equals("http") ||
		 * url.getProtocol().equals("https"))) { return
		 * FormValidation.error("protocol must be http or https"); } StageClient
		 * client = new StageClient(new URL(testURL), usernameValue,
		 * passwordValue); client.checkAuthentication(); } catch
		 * (MalformedURLException ex) { return FormValidation.error(url +
		 * " is not a valid URL"); } catch (StageException ex) { FormValidation
		 * stageError = FormValidation.error(ex.getMessage());
		 * stageError.initCause(ex); return stageError; } return
		 * FormValidation.ok(); }
		 */
	}

}
