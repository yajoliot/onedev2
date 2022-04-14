package io.onedev.server.plugin.report.problem;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.model.ProblemMetric;
import io.onedev.server.web.page.project.stats.buildmetric.BuildMetricStatsPage;

@SuppressWarnings("serial")
public class ProblemStatsPage extends BuildMetricStatsPage<ProblemMetric> {

	public ProblemStatsPage(PageParameters params) {
		super(params);
	}

	@Override
	protected Component newProjectTitle(String componentId) {
		return new Label(componentId, "Code Problem Statistics");
	}

}
