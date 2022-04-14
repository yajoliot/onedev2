package io.onedev.server.util.facade;

import io.onedev.server.model.Issue;

public class IssueFacade extends EntityFacade {
	
	private static final long serialVersionUID = 1L;
	
	private final Long projectId;
	
	private final Long number;
	
	public IssueFacade(Long id, Long projectId, Long number) {
		super(id);
		this.projectId = projectId;
		this.number = number;
	}

	public IssueFacade(Issue issue) {
		this(issue.getId(), issue.getProject().getId(), issue.getNumber());
	}
	
	public Long getProjectId() {
		return projectId;
	}

	public Long getNumber() {
		return number;
	}
	
}
