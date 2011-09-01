/**
 * 
 */
package hudson.maven.release.phase;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.PomFinder;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author domi
 * 
 */
public class RunPerformGoalsPhase extends org.apache.maven.shared.release.phase.RunPerformGoalsPhase {

	@Override
	public ReleaseResult execute(ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List<MavenProject> reactorProjects)
			throws ReleaseExecutionException {
		String additionalArguments = releaseDescriptor.getAdditionalArguments();

		if (releaseDescriptor.isUseReleaseProfile()) {
			if (!StringUtils.isEmpty(additionalArguments)) {
				additionalArguments = additionalArguments + " -DperformRelease=true";
			} else {
				additionalArguments = "-DperformRelease=true";
			}
		}

		// ensure we don't use the release pom for the perform goals
		// if (!StringUtils.isEmpty(additionalArguments)) {
		// additionalArguments = additionalArguments + " -f pom.xml";
		// } else {
		// additionalArguments = "-f pom.xml";
		// }

		String workDir = releaseDescriptor.getWorkingDirectory();
		if (workDir == null) {
			workDir = System.getProperty("user.dir");
		}

		String pomFileName = releaseDescriptor.getPomFileName();
		if (pomFileName == null) {
			pomFileName = "pom.xml";
		}

		File pomFile = new File(workDir, pomFileName);
		PomFinder pomFinder = new PomFinder(getLogger());
		boolean foundPom = pomFinder.parsePom(pomFile);

		if (foundPom) {
			File matchingPom = pomFinder.findMatchingPom(new File(releaseDescriptor.getCheckoutDirectory()));
			if (matchingPom != null) {
				getLogger().info("Invoking perform goals in directory " + matchingPom.getParent());
				releaseDescriptor.setCheckoutDirectory(matchingPom.getParent());
			}

		}

		return execute(releaseDescriptor, releaseEnvironment, new File(releaseDescriptor.getCheckoutDirectory()), additionalArguments);
	}
}
