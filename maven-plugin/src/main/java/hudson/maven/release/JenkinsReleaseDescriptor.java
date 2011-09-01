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
import hudson.maven.MavenEmbedderRequest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.shared.release.config.ReleaseDescriptor;

/**
 * @author domi
 * @author <a href="mailto:ctan@apache.org">Maria Catherine Tan</a>
 */
public class JenkinsReleaseDescriptor extends ReleaseDescriptor {
	private static final long serialVersionUID = 1L;

	private String releaseBy;

	private MavenEmbedderRequest mavenEmbedderRequest;

	private Map<String, String> environments;

	public void addEnvironment(String name, String value) {
		getEnvironments().put(name, value);
	}

	public Map<String, String> getEnvironments() {
		if (environments == null) {
			environments = new HashMap<String, String>();
		}

		return environments;
	}

	public void mapEnvironments(String name, String value) {
		if (environments == null) {
			environments = new HashMap<String, String>();
		} else {
			assert !environments.containsKey(name);
		}

		environments.put(name, value);
	}

	public void setEnvironments(Map<String, String> environments) {
		this.environments = environments;
	}

	public MavenEmbedderRequest getMavenEmbedderRequest() {
		return mavenEmbedderRequest;
	}

	public void setMavenEmbedderRequest(MavenEmbedderRequest mavenEmbedderRequest) {
		this.mavenEmbedderRequest = mavenEmbedderRequest;
	}

	public String getReleaseBy() {
		return releaseBy;
	}

	public void setReleaseBy(String releaseBy) {
		this.releaseBy = releaseBy;
	}

	public File getProjectDescriptorFile() {
		String parentPath = this.getWorkingDirectory();

		String pomFilename = this.getPomFileName();
		if (pomFilename == null) {
			pomFilename = "pom.xml";
		}

		return new File(parentPath, pomFilename);
	}

}
