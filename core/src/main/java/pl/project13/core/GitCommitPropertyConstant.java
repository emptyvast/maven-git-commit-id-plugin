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

package pl.project13.core;

public class GitCommitPropertyConstant {
	// these properties will be exposed to maven
	public static final String BRANCH = "branch";
	public static final String LOCAL_BRANCH_AHEAD = "local.branch.ahead";
	public static final String LOCAL_BRANCH_BEHIND = "local.branch.behind";
	public static final String DIRTY = "dirty";
	// only one of the following two will be exposed, depending on the commitIdGenerationMode
	public static final String COMMIT_ID_FLAT = "commit.id";
	public static final String COMMIT_ID_FULL = "commit.id.full";
	public static final String COMMIT_ID_ABBREV = "commit.id.abbrev";
	public static final String COMMIT_DESCRIBE = "commit.id.describe";
	public static final String COMMIT_SHORT_DESCRIBE = "commit.id.describe-short";
	public static final String BUILD_AUTHOR_NAME = "build.user.name";
	public static final String BUILD_AUTHOR_EMAIL = "build.user.email";
	public static final String BUILD_TIME = "build.time";
	public static final String BUILD_VERSION = "build.version";
	public static final String BUILD_HOST = "build.host";
	public static final String BUILD_NUMBER = "build.number";
	public static final String BUILD_NUMBER_UNIQUE = "build.number.unique";
	public static final String COMMIT_AUTHOR_NAME = "commit.user.name";
	public static final String COMMIT_AUTHOR_EMAIL = "commit.user.email";
	public static final String COMMIT_MESSAGE_FULL = "commit.message.full";
	public static final String COMMIT_MESSAGE_SHORT = "commit.message.short";
	public static final String COMMIT_TIME = "commit.time";
	public static final String REMOTE_ORIGIN_URL = "remote.origin.url";
	public static final String TAGS = "tags";
	public static final String CLOSEST_TAG_NAME = "closest.tag.name";
	public static final String CLOSEST_TAG_COMMIT_COUNT = "closest.tag.commit.count";
	public static final String TOTAL_COMMIT_COUNT = "total.commit.count";
	
}
