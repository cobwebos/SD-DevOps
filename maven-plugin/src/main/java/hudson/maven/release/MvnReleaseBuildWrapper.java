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
import hudson.maven.MavenModuleSet;
import hudson.maven.Messages;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.security.Permission;
import hudson.security.PermissionScope;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a {@link MavenBuild} to be able to run the <a
 * href="http://maven.apache.org/plugins/maven-release-plugin/">maven release
 * plugin</a> on demand.
 * 
 * Actualy we would like to remove this class at all - but currently we are not
 * able to add new Actions right within {@link MavenModuleSet} :(
 * 
 * @author James Nord
 * @version 0.3
 * @since 0.1
 */
public class MvnReleaseBuildWrapper extends BuildWrapper {

	private transient Logger log = LoggerFactory.getLogger(MvnReleaseBuildWrapper.class);

	private transient boolean doRelease = false;

	private transient String releaseVersion;

	public String releaseGoals = DescriptorImpl.DEFAULT_RELEASE_GOALS;
	public boolean selectCustomScmCommentPrefix = DescriptorImpl.DEFAULT_SELECT_CUSTOM_SCM_COMMENT_PREFIX;
	public boolean selectAppendHudsonUsername = DescriptorImpl.DEFAULT_SELECT_APPEND_HUDSON_USERNAME;

	@DataBoundConstructor
	public MvnReleaseBuildWrapper(String releaseGoals, boolean selectCustomScmCommentPrefix, boolean selectAppendHudsonUsername) {
		super();
		this.releaseGoals = releaseGoals;
		this.selectCustomScmCommentPrefix = selectCustomScmCommentPrefix;
		this.selectAppendHudsonUsername = selectAppendHudsonUsername;
	}

	@Override
	public Environment setUp(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, final BuildListener listener) throws IOException,
			InterruptedException {

		if (!doRelease) {
			// we are not performing a release so don't need a custom
			// tearDown.
			return new Environment() {
				/** intentionally blank */
			};
		}
		// reset for the next build.
		doRelease = false;
		build.addAction(new MvnReleaseBadgeAction("Release - " + releaseVersion));
		System.out.println("badge action added...");

		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
				System.out.println("do we need some tear down actions?");
				return super.tearDown(build, listener);
			}
		};
	}

	void enableRelease() {
		doRelease = true;
	}

	@Override
	public Action getProjectAction(@SuppressWarnings("rawtypes") AbstractProject job) {
		return new MvnReleaseAction((MavenModuleSet) job, selectCustomScmCommentPrefix, selectAppendHudsonUsername);
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

	public boolean isSelectAppendHudsonUsername() {
		return selectAppendHudsonUsername;
	}

	public boolean isSelectCustomScmCommentPrefix() {
		return selectCustomScmCommentPrefix;
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {

		public static final String DEFAULT_RELEASE_GOALS = "-Dresume=false release:prepare release:perform"; //$NON-NLS-1$
		public static final Permission CREATE_RELEASE = new Permission(Item.PERMISSIONS, "Release", //$NON-NLS-1$
				Messages._CreateReleasePermission_Description(), Jenkins.ADMINISTER, PermissionScope.JENKINS);

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

	}

}
