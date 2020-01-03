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

package pl.project13.maven.git;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Arrays.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

@RunWith(JUnitParamsRunner.class)
public class GitCommitIdMojoIntegrationTest extends GitIntegrationTest {
	
	@Rule
	public final EnvironmentVariables environmentVariablesMock = new EnvironmentVariables();
	
	static final boolean UseJGit = false;
	static final boolean UseNativeGit = true;
	
	public static Collection<?> useNativeGit() {
		return asList(UseJGit, UseNativeGit);
	}
	
	public static Collection<?> useDirty() {
		return asList(true, false);
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldIncludeExpectedProperties(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		
		assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.dirty"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldExcludeAsConfiguredProperties(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setUseNativeGit(useNativeGit);
		mojo.setExcludeProperties(ImmutableList.of("git.remote.origin.url", ".*.user.*"));
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		
		// explicitly excluded
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.remote.origin.url"));
		
		// glob excluded
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.build.user.name"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.build.user.email"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.user.name"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.user.email"));
		
		// these stay
		assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldIncludeOnlyAsConfiguredProperties(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setUseNativeGit(useNativeGit);
		mojo.setIncludeOnlyProperties(ImmutableList.of("git.remote.origin.url", ".*.user.*", "^git.commit.id.full$"));
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		
		// explicitly included
		assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));
		
		// glob included
		assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
		
		// these excluded
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.branch"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.id.abbrev"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.message.full"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.message.short"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.time"));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldExcludeAndIncludeAsConfiguredProperties(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setUseNativeGit(useNativeGit);
		mojo.setIncludeOnlyProperties(ImmutableList.of("git.remote.origin.url", ".*.user.*"));
		mojo.setExcludeProperties(ImmutableList.of("git.build.user.email"));
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		
		// explicitly included
		assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));
		
		// explicitly excluded -> overrules include only properties
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.build.user.email"));
		
		// glob included
		assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
		
		// these excluded
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.branch"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.id.full"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.id.abbrev"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.message.full"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.message.short"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.commit.time"));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldHaveNoPrefixWhenConfiguredPrefixIsEmptyStringAsConfiguredProperties(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setUseNativeGit(useNativeGit);
		mojo.setPrefix("");
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		
		// explicitly excluded
		assertThat(properties).satisfies(new DoesNotContainKeyCondition("git.remote.origin.url"));
		assertThat(properties).satisfies(new DoesNotContainKeyCondition(".remote.origin.url"));
		assertThat(properties).satisfies(new ContainsKeyCondition("remote.origin.url"));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldSkipDescribeWhenConfiguredToDoSo(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig config = new GitDescribeConfig();
		config.setSkip(true);
		
		// when
		mojo.setGitDescribe(config);
		mojo.execute();
		
		// then
		assertThat(targetProject.getProperties()).satisfies(new DoesNotContainKeyCondition("git.commit.id.describe"));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldUseJenkinsBranchInfoWhenAvailable(boolean useNativeGit) throws Exception {
		// given
		Map<String, String> env = Maps.newHashMap();
		
		String detachedHeadSha1 = "b6a73ed747dd8dc98642d731ddbf09824efb9d48";
		String ciUrl = "http://myciserver.com";
		
		// when
		// in a detached head state, getBranch() will return the SHA1...standard behavior
		shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, detachedHeadSha1);
		
		// again, SHA1 will be returned if we're in jenkins, but GIT_BRANCH is not set
		env.put("JENKINS_URL", ciUrl);
		shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, detachedHeadSha1);
		
		// now set GIT_BRANCH too and see that the branch name from env var is returned
		env.clear();
		env.put("JENKINS_URL", ciUrl);
		env.put("GIT_BRANCH", "mybranch");
		shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mybranch");
		
		// same, but for hudson
		env.clear();
		env.put("HUDSON_URL", ciUrl);
		env.put("GIT_BRANCH", "mybranch");
		shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mybranch");
		
		// now set GIT_LOCAL_BRANCH too and see that the branch name from env var is returned
		env.clear();
		env.put("JENKINS_URL", ciUrl);
		env.put("GIT_BRANCH", "mybranch");
		env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
		shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mylocalbranch");
		
		// same, but for hudson
		env.clear();
		env.put("HUDSON_URL", ciUrl);
		env.put("GIT_BRANCH", "mybranch");
		env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
		shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, "mylocalbranch");
		
		// GIT_BRANCH but no HUDSON_URL or JENKINS_URL
		env.clear();
		env.put("GIT_BRANCH", "mybranch");
		env.put("GIT_LOCAL_BRANCH", "mylocalbranch");
		shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(useNativeGit, env, detachedHeadSha1);
	}
	
	private void shouldUseJenkinsBranchInfoWhenAvailableHelperAndAssertBranch(boolean useNativeGit, Map<String, String> env, String expectedBranchName) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		// remove all keys from System.getenv()
		List<String> keySet = new ArrayList<>(System.getenv().keySet());
		for (String key : keySet) {
			environmentVariablesMock.set(key, null);
		}
		// set System.getenv() to be equal to given parameter env
		for (Map.Entry<String, String> entry : env.entrySet()) {
			environmentVariablesMock.set(entry.getKey(), entry.getValue());
		}
		
		// verify that System.getenv() is actually equal
		Assert.assertEquals(env, System.getenv());
		
		// reset repo and force detached HEAD
		try (final Git git = git("my-jar-project")) {
			git.reset().setMode(ResetCommand.ResetType.HARD).setRef("b6a73ed").call();
			git.checkout().setName("b6a73ed").setForce(true).call();
		}
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.branch", expectedBranchName);
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldResolvePropertiesOnDefaultSettingsForNonPomProject(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldNotRunWhenSkipIsSet(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-skip-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setSkip(true);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertThat(targetProject.getProperties()).isEmpty();
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldNotRunWhenPackagingPomAndDefaultSettingsApply(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertThat(targetProject.getProperties()).isEmpty();
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldRunWhenPackagingPomAndSkipPomsFalse(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setSkipPoms(false);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertThat(targetProject.getProperties()).isNotEmpty();
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldUseParentProjectRepoWhenInvokedFromChild(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setSkipPoms(false);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldUseChildProjectRepoIfInvokedFromChild(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setSkipPoms(false);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
	}
	
	@Test(expected = MojoExecutionException.class)
	@Parameters(method = "useNativeGit")
	public void shouldFailWithExceptionWhenNoGitRepoFound(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withNoGitRepoAvailable()
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setSkipPoms(false);
		mojo.setUseNativeGit(useNativeGit);
		
		mojo.execute();
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateCustomPropertiesFileProperties(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		String targetFilePath = "target/classes/custom-git.properties";
		File expectedFile = new File(targetProject.getBasedir(), targetFilePath);
		
		setProjectToExecuteMojoIn(targetProject);
		mojo.setGenerateGitPropertiesFile(true);
		mojo.setGenerateGitPropertiesFilename(targetFilePath);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		try {
			mojo.execute();
			
			// then
			assertThat(expectedFile).exists();
		} finally {
			FileUtils.forceDelete(expectedFile);
		}
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateCustomPropertiesFileJson(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT_WITH_SPECIAL_CHARACTERS)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		String targetFilePath = "target/classes/custom-git.properties";
		File expectedFile = new File(targetProject.getBasedir(), targetFilePath);
		
		setProjectToExecuteMojoIn(targetProject);
		mojo.setGenerateGitPropertiesFile(true);
		mojo.setGenerateGitPropertiesFilename(targetFilePath);
		mojo.setFormat("json");
		mojo.setUseNativeGit(useNativeGit);
		// when
		try {
			mojo.execute();
			
			// then
			assertThat(expectedFile).exists();
			String json = Files.toString(expectedFile, Charset.forName("UTF-8"));
			ObjectMapper om = new ObjectMapper();
			Map<?, ?> map = new HashMap<>();
			map = om.readValue(json, map.getClass());
			assertThat(map.size() > 10);
		} finally {
			FileUtils.forceDelete(expectedFile);
		}
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldSkipWithoutFailOnNoGitDirectoryWhenNoGitRepoFound(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withNoGitRepoAvailable()
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setFailOnNoGitDirectory(false);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertThat(targetProject.getProperties()).isEmpty();
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldNotSkipWithoutFailOnNoGitDirectoryWhenNoGitRepoIsPresent(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		mojo.setFailOnNoGitDirectory(false);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalse(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
		gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected
		
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "v1.0.0-dirty");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateDescribeWithTagOnlyWhenForceLongFormatIsFalseAndAbbrevLengthIsNonDefault(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 10);
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "v1.0.0");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe-short", "v1.0.0");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrue(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "v1.0.0-0-gde4db35");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateDescribeWithTagAndZeroAndCommitIdWhenForceLongFormatIsTrueAndAbbrevLengthIsNonDefault(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 10);
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "v1.0.0-0-gde4db35917");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe-short", "v1.0.0-0");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateCommitIdAbbrevWithDefaultLength(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		mojo.setAbbrevLength(7);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "de4db35");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateCommitIdAbbrevWithNonDefaultLength(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		mojo.setAbbrevLength(10);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertThat(targetProject.getProperties()).includes(entry("git.commit.id.abbrev", "de4db35917"));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldFormatDate(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		String dateFormat = "MM/dd/yyyy";
		mojo.setDateFormat(dateFormat);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
		
		SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
		String expectedDate = smf.format(new Date());
		assertThat(targetProject.getProperties()).includes(entry("git.build.time", expectedDate));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldSkipGitDescribe(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
		gitDescribeConfig.setSkip(true);
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertThat(targetProject.getProperties()).satisfies(new DoesNotContainKeyCondition("git.commit.id.describe"));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldMarkGitDescribeAsDirty(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
		String dirtySuffix = "-dirtyTest";
		gitDescribeConfig.setDirty(dirtySuffix);
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertThat(targetProject.getProperties()).includes(entry("git.commit.id.describe", "v1.0.0-0-gde4db35" + dirtySuffix));
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldAlwaysPrintGitDescribe(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
		gitDescribeConfig.setAlways(true);
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "0b0181b");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldWorkWithEmptyGitDescribe(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldWorkWithNullGitDescribe(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		mojo.setGitDescribe(null);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertGitPropertiesPresentInProject(targetProject.getProperties());
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldExtractTagsOnGivenCommit(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
				.create();
		
		try (final Git git = git("my-jar-project")) {
			git.reset().setMode(ResetCommand.ResetType.HARD).setRef("d37a598").call();
		}
		
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		mojo.setGitDescribe(null);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		assertGitPropertiesPresentInProject(properties);
		
		assertThat(properties).satisfies(new ContainsKeyCondition("git.tags"));
		assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");
		
		assertThat(Splitter.on(",").split(properties.get("git.tags").toString()))
				.containsOnly("lightweight-tag", "newest-tag");
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "2");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldExtractTagsOnGivenCommitWithOldestCommit(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
				.create();
		
		try (final Git git = git("my-jar-project")) {
			git.reset().setMode(ResetCommand.ResetType.HARD).setRef("9597545").call();
		}
		
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		mojo.setGitDescribe(null);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		assertGitPropertiesPresentInProject(properties);
		
		assertThat(properties).satisfies(new ContainsKeyCondition("git.tags"));
		assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");
		
		assertThat(Splitter.on(",").split(properties.get("git.tags").toString()))
				.containsOnly("annotated-tag", "lightweight-tag", "newest-tag");
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "1");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldExtractTagsOnHead(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.ON_A_TAG)
				.create();
		
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		mojo.setGitDescribe(null);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		assertGitPropertiesPresentInProject(properties);
		
		assertThat(properties).satisfies(new ContainsKeyCondition("git.tags"));
		assertThat(properties.get("git.tags").toString()).doesNotContain("refs/tags/");
		
		assertThat(Splitter.on(",").split(properties.get("git.tags").toString()))
				.containsOnly("v1.0.0");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void runGitDescribeWithMatchOption(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-plugin-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_THREE_COMMITS_AND_TWO_TAGS_CURRENTLY_ON_COMMIT_WITHOUT_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		String headCommitId = "b0c6d28b3b83bf7b905321bae67d9ca4c75a203f";
		Map<String, String> gitTagMap = new HashMap<>();
		gitTagMap.put("v1.0", "f830b5f85cad3d33ba50d04c3d1454e1ae469057");
		gitTagMap.put("v2.0", "0e3495783c56589213ee5f2ae8900e2dc1b776c4");
		
		for (Map.Entry<String, String> entry : gitTagMap.entrySet()) {
			String gitDescribeMatchNeedle = entry.getKey();
			String commitIdOfMatchNeedle = entry.getValue();
			
			GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
			gitDescribeConfig.setMatch(gitDescribeMatchNeedle);
			gitDescribeConfig.setAlways(false);
			
			mojo.setGitDescribe(gitDescribeConfig);
			mojo.setUseNativeGit(useNativeGit);
			
			// when
			mojo.execute();
			
			// then
			assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.commit.id.describe");
			assertThat(targetProject.getProperties().getProperty("git.commit.id.describe")).startsWith(gitDescribeMatchNeedle);
			
			assertThat(targetProject.getProperties().stringPropertyNames()).contains("git.commit.id.full");
			assertThat(targetProject.getProperties().get("git.commit.id.full")).isNotEqualTo(commitIdOfMatchNeedle);
			assertThat(targetProject.getProperties().get("git.commit.id.full")).isEqualTo(headCommitId);
			assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "3");
		}
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateClosestTagInformationWhenOnATag(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(false, 7);
		gitDescribeConfig.setDirty("-dirty"); // checking if dirty works as expected
		
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.name", "v1.0.0");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.commit.count", "0");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateClosestTagInformationWhenOnATagAndDirty(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
		String dirtySuffix = "-dirtyTest";
		gitDescribeConfig.setDirty(dirtySuffix);
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.name", "v1.0.0");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.commit.count", "0");
	}
	
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateClosestTagInformationWhenCommitHasTwoTags(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
				.create();
		
		try (final Git git = git("my-jar-project")) {
			git.reset().setMode(ResetCommand.ResetType.HARD).setRef("d37a598").call();
		}
		
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		mojo.setGitDescribe(null);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		// AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS ==> Where the newest-tag was created latest
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.name", "newest-tag");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.commit.count", "0");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldUseDateFormatTimeZone(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		// RFC 822 time zone: Sign TwoDigitHours Minutes
		String dateFormat = "Z"; // we want only the timezone (formated in RFC 822) out of the dateformat (easier for asserts)
		String expectedTimeZoneOffset = "+0200";
		String executionTimeZoneOffset = "-0800";
		TimeZone expectedTimeZone = TimeZone.getTimeZone("GMT" + expectedTimeZoneOffset);
		TimeZone executionTimeZone = TimeZone.getTimeZone("GMT" + executionTimeZoneOffset);
		
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
		mojo.setGitDescribe(gitDescribeConfig);
		mojo.setUseNativeGit(useNativeGit);
		mojo.setDateFormat(dateFormat);
		mojo.setDateFormatTimeZone(expectedTimeZone.getID());
		
		// override the default timezone for execution and testing
		TimeZone currentDefaultTimeZone = TimeZone.getDefault();
		TimeZone.setDefault(executionTimeZone);
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		assertPropertyPresentAndEqual(properties, "git.commit.time", expectedTimeZoneOffset);
		
		assertPropertyPresentAndEqual(properties, "git.build.time", expectedTimeZoneOffset);
		
		// set the timezone back
		TimeZone.setDefault(currentDefaultTimeZone);
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateCommitIdOldFashioned(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.ON_A_TAG_DIRTY)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		mojo.setUseNativeGit(useNativeGit);
		mojo.setCommitIdGenerationMode("flat");
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		assertThat(properties.stringPropertyNames()).contains("git.commit.id");
		assertThat(properties.stringPropertyNames()).excludes("git.commit.id.full");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void testDetectCleanWorkingDirectory(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.GIT_WITH_NO_CHANGES)
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
		String dirtySuffix = "-dirtyTest";
		gitDescribeConfig.setDirty(dirtySuffix);
		mojo.setGitDescribe(gitDescribeConfig);
		
		mojo.setUseNativeGit(useNativeGit);
		mojo.setCommitIdGenerationMode("flat");
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		assertThat(properties.get("git.dirty")).isEqualTo("false");
		assertThat(properties).includes(entry("git.commit.id.describe", "85c2888")); // assert no dirtySuffix at the end!
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void testDetectDirtyWorkingDirectory(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-pom-project", "pom")
				.withChildProject("my-jar-module", "jar")
				.withGitRepoInChild(AvailableGitTestRepo.WITH_ONE_COMMIT) // GIT_WITH_CHANGES
				.create();
		MavenProject targetProject = mavenSandbox.getChildProject();
		
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribeConfig = createGitDescribeConfig(true, 7);
		String dirtySuffix = "-dirtyTest";
		gitDescribeConfig.setDirty(dirtySuffix);
		mojo.setGitDescribe(gitDescribeConfig);
		
		mojo.setUseNativeGit(useNativeGit);
		mojo.setCommitIdGenerationMode("flat");
		
		// when
		mojo.execute();
		
		// then
		Properties properties = targetProject.getProperties();
		assertThat(properties.get("git.dirty")).isEqualTo("true");
		assertThat(properties).includes(entry("git.commit.id.describe", "0b0181b" + dirtySuffix)); // assert dirtySuffix at the end!
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateClosestTagInformationWithExcludeLightweightTagsForClosestTag(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
		gitDescribe.setDirty("-customDirtyMark");
		gitDescribe.setTags(false); // exclude lightweight tags
		
		mojo.setGitDescribe(gitDescribe);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "b6a73ed");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "annotated-tag-2-gb6a73ed74-customDirtyMark");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.name", "annotated-tag");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.commit.count", "2");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "3");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateClosestTagInformationWithIncludeLightweightTagsForClosestTag(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
		gitDescribe.setDirty("-customDirtyMark");
		gitDescribe.setTags(true); // include lightweight tags
		
		mojo.setGitDescribe(gitDescribe);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "b6a73ed");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "lightweight-tag-1-gb6a73ed74-customDirtyMark");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.name", "lightweight-tag");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.commit.count", "1");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateClosestTagInformationWithIncludeLightweightTagsForClosestTagAndPreferAnnotatedTags(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
		gitDescribe.setDirty("-customDirtyMark");
		gitDescribe.setTags(true); // include lightweight tags
		
		mojo.setGitDescribe(gitDescribe);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "b6a73ed");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "newest-tag-1-gb6a73ed74-customDirtyMark");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.name", "newest-tag");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.commit.count", "1");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void shouldGenerateClosestTagInformationWithIncludeLightweightTagsForClosestTagAndFilter(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_COMMIT_THAT_HAS_TWO_TAGS)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
		gitDescribe.setDirty("-customDirtyMark");
		gitDescribe.setTags(true); // include lightweight tags
		gitDescribe.setMatch("light*");
		
		mojo.setGitDescribe(gitDescribe);
		mojo.setUseNativeGit(useNativeGit);
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "b6a73ed");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "lightweight-tag-1-gb6a73ed74-customDirtyMark");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.name", "lightweight-tag");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.closest.tag.commit.count", "1");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void verifyEvalOnDifferentCommitWithParentOfHead(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
		gitDescribe.setDirty(null);
		
		mojo.setGitDescribe(gitDescribe);
		mojo.setUseNativeGit(useNativeGit);
		mojo.setEvaluateOnCommit("HEAD^1");
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "e3d159d");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "e3d159dd7");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.tags", "test_tag");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.dirty", "true");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void verifyEvalOnDifferentCommitWithBranchName(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
		gitDescribe.setDirty(null);
		
		mojo.setGitDescribe(gitDescribe);
		mojo.setUseNativeGit(useNativeGit);
		mojo.setEvaluateOnCommit("test");
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "9cb810e");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "test_tag-0-g9cb810e57");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.tags", "test_tag");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.dirty", "true");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "2");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void verifyEvalOnDifferentCommitWithTagName(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
		gitDescribe.setDirty(null);
		
		mojo.setGitDescribe(gitDescribe);
		mojo.setUseNativeGit(useNativeGit);
		mojo.setEvaluateOnCommit("test_tag");
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "9cb810e");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "test_tag-0-g9cb810e57");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.tags", "test_tag");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.dirty", "true");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "2");
	}
	
	@Test
	@Parameters(method = "useNativeGit")
	public void verifyEvalOnDifferentCommitWithCommitHash(boolean useNativeGit) throws Exception {
		// given
		mavenSandbox.withParentProject("my-jar-project", "jar")
				.withNoChildProject()
				.withGitRepoInParent(AvailableGitTestRepo.WITH_TAG_ON_DIFFERENT_BRANCH)
				.create();
		MavenProject targetProject = mavenSandbox.getParentProject();
		setProjectToExecuteMojoIn(targetProject);
		
		GitDescribeConfig gitDescribe = createGitDescribeConfig(true, 9);
		gitDescribe.setDirty(null);
		
		mojo.setGitDescribe(gitDescribe);
		mojo.setUseNativeGit(useNativeGit);
		mojo.setEvaluateOnCommit("9cb810e");
		
		// when
		mojo.execute();
		
		// then
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.abbrev", "9cb810e");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.commit.id.describe", "test_tag-0-g9cb810e57");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.tags", "test_tag");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.dirty", "true");
		
		assertPropertyPresentAndEqual(targetProject.getProperties(), "git.total.commit.count", "2");
	}
	
	// TODO: Test that fails when trying to pass invalid data to evaluateOnCommit
	
	private GitDescribeConfig createGitDescribeConfig(boolean forceLongFormat, int abbrev) {
		GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
		gitDescribeConfig.setTags(true);
		gitDescribeConfig.setForceLongFormat(forceLongFormat);
		gitDescribeConfig.setAbbrev(abbrev);
		gitDescribeConfig.setDirty("");
		return gitDescribeConfig;
	}
	
	private void assertPropertyPresentAndEqual(Properties properties, String key, String expected) {
		assertThat(properties.stringPropertyNames()).contains(key);
		assertThat(properties.getProperty(key)).isEqualTo(expected);
	}
	
	private void assertGitPropertiesPresentInProject(Properties properties) {
		assertThat(properties).satisfies(new ContainsKeyCondition("git.build.time"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.build.host"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.branch"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.full"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.abbrev"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.id.describe"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.name"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.build.user.email"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.name"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.user.email"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.full"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.message.short"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.commit.time"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.remote.origin.url"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.closest.tag.name"));
		assertThat(properties).satisfies(new ContainsKeyCondition("git.closest.tag.commit.count"));
	}
}
