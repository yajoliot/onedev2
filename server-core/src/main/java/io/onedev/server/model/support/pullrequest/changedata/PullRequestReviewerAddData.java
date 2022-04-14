package io.onedev.server.model.support.pullrequest.changedata;

import javax.annotation.Nullable;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.model.User;

public class PullRequestReviewerAddData extends PullRequestChangeData {

	private static final long serialVersionUID = 1L;

	private final Long reviewerId;
	
	public PullRequestReviewerAddData(User reviewer) {
		this.reviewerId = reviewer.getId();
	}
	
	@Override
	public String getActivity() {
		User user = getUser();
		if (user != null)
			return "added reviewer \"" + user.getDisplayName() + "\"";
		else
			return "added reviewer unknown";
	}

	@Nullable
	public User getUser() {
		return OneDev.getInstance(UserManager.class).get(reviewerId);
	}
	
}
