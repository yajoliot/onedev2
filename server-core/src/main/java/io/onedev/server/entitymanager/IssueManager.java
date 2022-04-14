package io.onedev.server.entitymanager;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.onedev.server.model.Issue;
import io.onedev.server.model.Milestone;
import io.onedev.server.model.Project;
import io.onedev.server.persistence.dao.EntityManager;
import io.onedev.server.search.entity.EntityQuery;
import io.onedev.server.util.MilestoneAndIssueState;
import io.onedev.server.util.ProjectIssueStats;
import io.onedev.server.util.ProjectScopedNumber;
import io.onedev.server.util.criteria.Criteria;
import io.onedev.server.web.component.issue.workflowreconcile.UndefinedFieldResolution;
import io.onedev.server.web.component.issue.workflowreconcile.UndefinedFieldValue;
import io.onedev.server.web.component.issue.workflowreconcile.UndefinedFieldValuesResolution;
import io.onedev.server.web.component.issue.workflowreconcile.UndefinedStateResolution;

public interface IssueManager extends EntityManager<Issue> {
	
    @Nullable
    Issue find(Project project, long number);
    
    @Nullable
    Issue find(ProjectScopedNumber fqn);
    
    @Nullable
    Issue findByFQN(String fqn);

    @Nullable
    Issue findByUUID(String uuid);
    
	void open(Issue issue);
	
	Long getNextNumber(Project numberScope);
	
	List<Issue> query(EntityQuery<Issue> issueQuery, int firstResult, int maxResults, 
			boolean loadFieldsAndLinks);
	
	int count(@Nullable Criteria<Issue> issueCriteria);
	
	List<Issue> query(Project project, boolean inTree, EntityQuery<Issue> issueQuery, 
			int firstResult, int maxResults, boolean loadFieldsAndLinks);
	
	int count(Project project, boolean inTree, @Nullable Criteria<Issue> issueCriteria);
	
	List<Issue> query(@Nullable EntityQuery<Issue> scope, Project project, String term, int count);

	Collection<String> getUndefinedStates();
	
	void fixUndefinedStates(Map<String, UndefinedStateResolution> resolutions);
	
	Collection<String> getUndefinedFields();
	
	void fixUndefinedFields(Map<String, UndefinedFieldResolution> resolutions);
	
	Collection<UndefinedFieldValue> getUndefinedFieldValues();
	
	void fixUndefinedFieldValues(Map<String, UndefinedFieldValuesResolution> resolutions);
	
	void fixStateAndFieldOrdinals();
	
	void saveDescription(Issue issue, @Nullable String description);
	
	@Override
	void delete(Issue issue);
	
	Collection<Long> getIssueNumbers(Long projectId);
	
	void move(Project targetProject, Collection<Issue> issues);
	
	void delete(Collection<Issue> issues);
	
	Collection<MilestoneAndIssueState> queryMilestoneAndIssueStates(Project project, Collection<Milestone> milestones);
	
	List<ProjectIssueStats> queryStats(Collection<Project> projects);
	
	Collection<Milestone> queryUsedMilestones(Project project);

	void clearSchedules(Project project, Collection<Milestone> milestones);
	
	List<Issue> queryAfter(@Nullable Long afterIssueId, int count);

}
