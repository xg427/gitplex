package com.gitplex.server.web.component.link;

import javax.annotation.Nullable;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.lib.FileMode;

import com.gitplex.server.git.BlobIdent;
import com.gitplex.server.model.PullRequest;
import com.gitplex.server.model.support.ProjectAndBranch;
import com.gitplex.server.web.page.project.ProjectPage;
import com.gitplex.server.web.page.project.blob.ProjectBlobPage;

@SuppressWarnings("serial")
public class BranchLink extends ViewStateAwarePageLink<Void> {

	private final ProjectAndBranch projectAndBranch;
	
	public BranchLink(String id, ProjectAndBranch projectAndBranch, @Nullable PullRequest request) {
		super(id, ProjectBlobPage.class, paramsOf(projectAndBranch, request));
		this.projectAndBranch = projectAndBranch;
	}
	
	private static PageParameters paramsOf(ProjectAndBranch projectAndBranch, PullRequest request) {
		BlobIdent blobIdent = new BlobIdent(projectAndBranch.getBranch(), null, FileMode.TREE.getBits());
		ProjectBlobPage.State state = new ProjectBlobPage.State(blobIdent);
		state.requestId = PullRequest.idOf(request);
		return ProjectBlobPage.paramsOf(projectAndBranch.getProject(), state);
	}

	@Override
	protected void onConfigure() {
		super.onConfigure();
		setEnabled(projectAndBranch.getObjectName(false) != null);
	}

	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
		configure();
		if (!isEnabled()) {
			tag.setName("span");
		}
	}

	@Override
	public IModel<?> getBody() {
		String label;
		if (getPage() instanceof ProjectPage) {
			ProjectPage page = (ProjectPage) getPage();
			if (page.getProject().equals(projectAndBranch.getProject())) 
				label = projectAndBranch.getBranch();
			else 
				label = projectAndBranch.getFQN();
		} else {
			label = projectAndBranch.getFQN();
		}
		return Model.of(label);
	}

}
