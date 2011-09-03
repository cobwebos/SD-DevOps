package hudson.maven.release.core;

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


import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.shared.release.ReleaseManagerListener;
import org.sonatype.aether.repository.LocalRepository;

/**
 * @author domi
 * @author Jason van Zyl
 * @author Edwin Punzalan
 * @version $Id: DefaultContinuumReleaseManager.java 1063962 2011-01-27
 *          02:28:22Z ctan $
 */
public interface JenkinsReleaseManager {

	public String prepare(ProjectDescriptor project, Properties releaseProperties, Map<String, String> relVersions, Map<String, String> devVersions,
			ReleaseManagerListener listener, String workingDirectory) throws JenkinsReleaseException;

	public String prepare(ProjectDescriptor project, Properties releaseProperties, Map<String, String> relVersions, Map<String, String> devVersions,
			ReleaseManagerListener listener, String workingDirectory, Map<String, String> environments, String executable) throws JenkinsReleaseException;

	public void perform(String releaseId, File buildDirectory, String goals, String arguments, boolean useReleaseProfile, ReleaseManagerListener listener)
			throws JenkinsReleaseException;

	public void perform(String releaseId, File buildDirectory, String goals, String arguments, boolean useReleaseProfile, ReleaseManagerListener listener,
			LocalRepository repository) throws JenkinsReleaseException;

	public void perform(String releaseId, String workingDirectory, File buildDirectory, String goals, String arguments, boolean useReleaseProfile,
			ReleaseManagerListener listener) throws JenkinsReleaseException;

	public void rollback(String releaseId, String workingDirectory, ReleaseManagerListener listener) throws JenkinsReleaseException;

	public Map getPreparedReleases();

	public Map getReleaseResults();

	public Map<String, ReleaseManagerListener> getListeners();

	public String sanitizeTagName(String scmUrl, String tagName) throws Exception;

	// public ReleaseListenerSummary getListener(String releaseId);

}