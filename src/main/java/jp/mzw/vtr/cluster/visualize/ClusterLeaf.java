package jp.mzw.vtr.cluster.visualize;

public class ClusterLeaf {
	int hashcode;
	String projectId;
	String commitId;
	String className;
	String methodName;

	public ClusterLeaf(int hashcode, String projectId, String commitId, String className, String methodName) {
		this.hashcode = hashcode;
		this.projectId = projectId;
		this.commitId = commitId;
		this.className = className;
		this.methodName = methodName;
	}

	public int getHashcode() {
		return this.hashcode;
	}

	public String getProjectId() {
		return this.projectId;
	}

	public String getCommitId() {
		return this.commitId;
	}

	public String getClassName() {
		return this.className;
	}

	public String getMethodName() {
		return this.methodName;
	}
}
