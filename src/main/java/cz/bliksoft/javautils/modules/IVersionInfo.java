package cz.bliksoft.javautils.modules;

import cz.bliksoft.javautils.StringUtils;

public interface IVersionInfo {

	String getArtifactId();

	String getGroupId();

	String getVersion();

	String getBranch();

	String getCommitIdAbbrev();

	String getTags();

	String getClosestTag();

	String getClosestTagCommitCount();

	/**
	 * Human-readable version string like: my-plugin 0.0.1 [master:9359bd8]
	 */
	default String getDisplayVersion() {
		StringBuilder sb = new StringBuilder();

		sb.append(getArtifactId()).append(" ").append(getVersion());

		if (getBranch() != null && StringUtils.hasText(getBranch()) && getCommitIdAbbrev() != null
				&& StringUtils.hasText(getCommitIdAbbrev())) {
			sb.append(" [").append(getBranch()).append(":").append(getCommitIdAbbrev()).append("]");
		}

		if (getTags() != null && StringUtils.hasText(getTags())) {
			sb.append(" ").append(getTags());
		}

		return sb.toString();
	}
}
