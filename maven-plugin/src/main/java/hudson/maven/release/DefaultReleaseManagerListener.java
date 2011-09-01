package hudson.maven.release;

import java.util.List;

import org.apache.maven.shared.release.ReleaseManagerListener;

public class DefaultReleaseManagerListener implements ReleaseManagerListener {

	@Override
	public void goalStart(String goal, List<String> phases) {
		// TODO Auto-generated method stub

	}

	@Override
	public void phaseStart(String name) {
		System.out.println("DefaultReleaseManagerListener.phaseStart(): " + name);
	}

	@Override
	public void phaseEnd() {
		System.out.println("DefaultReleaseManagerListener.phaseEnd()");
	}

	@Override
	public void phaseSkip(String name) {
		System.out.println("DefaultReleaseManagerListener.phaseSkip(): " + name);
	}

	@Override
	public void goalEnd() {
		System.out.println("DefaultReleaseManagerListener.goalEnd()");
	}

	@Override
	public void error(String reason) {
		System.out.println("DefaultReleaseManagerListener.error():" + reason);
	}

}
