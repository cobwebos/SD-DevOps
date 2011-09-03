/**
 * 
 */
package hudson.maven.release;

import java.util.List;
import java.util.Map;

import hudson.maven.Maven3Builder;
import hudson.maven.MavenBuildInformation;
import hudson.maven.MavenReporter;
import hudson.maven.ModuleName;
import hudson.maven.MavenBuild.ProxyImpl2;
import hudson.model.BuildListener;

/**
 * @author domi
 *
 */
public class Mvn3ReleaseBuilder extends Maven3Builder {

	/**
	 * @param listener
	 * @param proxies
	 * @param reporters
	 * @param goals
	 * @param systemProps
	 * @param mavenBuildInformation
	 */
	public Mvn3ReleaseBuilder(BuildListener listener, Map<ModuleName, ProxyImpl2> proxies, Map<ModuleName, List<MavenReporter>> reporters,
			List<String> goals, Map<String, String> systemProps, MavenBuildInformation mavenBuildInformation) {
		super(listener, proxies, reporters, goals, systemProps, mavenBuildInformation);
		// TODO Auto-generated constructor stub
	}

}
