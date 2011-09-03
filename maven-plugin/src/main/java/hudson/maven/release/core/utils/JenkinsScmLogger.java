package hudson.maven.release.core.utils;

import org.codehaus.plexus.logging.Logger;

public class JenkinsScmLogger implements org.apache.maven.scm.log.ScmLogger {

	private Logger logger;

	public JenkinsScmLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	/**
	 * {@inheritDoc}
	 */
	public void debug(String content) {
		logger.debug(content);
	}

	/**
	 * {@inheritDoc}
	 */
	public void debug(String content, Throwable error) {
		logger.debug(content, error);
	}

	/**
	 * {@inheritDoc}
	 */
	public void debug(Throwable error) {
		logger.debug("", error);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	/**
	 * {@inheritDoc}
	 */
	public void info(String content) {
		logger.info(content);
	}

	/**
	 * {@inheritDoc}
	 */
	public void info(String content, Throwable error) {
		logger.info(content, error);
	}

	/**
	 * {@inheritDoc}
	 */
	public void info(Throwable error) {
		logger.info("", error);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	/**
	 * {@inheritDoc}
	 */
	public void warn(String content) {
		logger.warn(content);
	}

	/**
	 * {@inheritDoc}
	 */
	public void warn(String content, Throwable error) {
		logger.warn(content, error);
	}

	/**
	 * {@inheritDoc}
	 */
	public void warn(Throwable error) {
		logger.warn("", error);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	/**
	 * {@inheritDoc}
	 */
	public void error(String content) {
		logger.error(content);
	}

	/**
	 * {@inheritDoc}
	 */
	public void error(String content, Throwable error) {
		logger.error(content, error);
	}

	/**
	 * {@inheritDoc}
	 */
	public void error(Throwable error) {
		logger.error("", error);
	}
}
