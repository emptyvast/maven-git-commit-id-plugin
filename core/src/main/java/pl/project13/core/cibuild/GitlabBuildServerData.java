/*
 * This file is part of git-commit-id-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.core.cibuild;

import pl.project13.core.GitCommitPropertyConstant;
import pl.project13.core.log.LoggerBridge;


import javax.annotation.Nonnull;

import java.util.Map;
import java.util.Properties;

public class GitlabBuildServerData extends BuildServerDataProvider {
	
	GitlabBuildServerData(LoggerBridge log, @Nonnull Map<String, String> env) {
		super(log, env);
	}
	
	/**
	 * @see <a href="https://docs.gitlab.com/ce/ci/variables/predefined_variables.html">GitlabCIVariables</a>
	 */
	public static boolean isActiveServer(Map<String, String> env) {
		// CI is not unique to Gitlab CI (e.g. CircleCI). Use GITLAB_CI instead.
		return env.containsKey("GITLAB_CI");
	}
	
	@Override
	void loadBuildNumber(@Nonnull Properties properties) {
		// GITLAB CI
		// CI_PIPELINE_ID will be present if in a Gitlab CI environment (Gitlab >8.10 & Gitlab CI >0.5)  and contains a server wide unique ID for a pipeline run
		String uniqueBuildNumber = env.getOrDefault("CI_PIPELINE_ID", "");
		// CI_PIPELINE_IID will be present if in a Gitlab CI environment (Gitlab >11.0) and contains the project specific build number
		String buildNumber = env.getOrDefault("CI_PIPELINE_IID", "");
		
		put(properties, GitCommitPropertyConstant.BUILD_NUMBER, buildNumber);
		put(properties, GitCommitPropertyConstant.BUILD_NUMBER_UNIQUE, uniqueBuildNumber);
	}
	
	@Override
	public String getBuildBranch() {
		String environmentBasedBranch = env.get("CI_COMMIT_REF_NAME");
		log.info("Using environment variable based branch name. CI_COMMIT_REF_NAME = {}", environmentBasedBranch);
		return environmentBasedBranch;
	}
}
