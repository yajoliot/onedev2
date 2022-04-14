package io.onedev.server.plugin.report.clover;

import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.server.buildspec.step.StepGroup;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.Patterns;

@Editable(order=9900, group=StepGroup.PUBLISH_REPORTS, name="Jest Coverage")
public class PublishJestCoverageReportStep extends PublishCloverReportStep {

	private static final long serialVersionUID = 1L;
	
	@Editable(order=100, description="Specify Jest coverage report file in clover format relative to <a href='$docRoot/pages/concepts.md#job-workspace'>job workspace</a>, "
			+ "for instance <tt>coverage/clover.xml</tt>. This file can be generated with Jest option <tt>'--coverage'</tt>. "
			+ "Use * or ? for pattern match")
	@Interpolative(variableSuggester="suggestVariables")
	@Patterns(path=true)
	@NotEmpty
	@Override
	public String getFilePatterns() {
		return super.getFilePatterns();
	}

	@Override
	public void setFilePatterns(String filePatterns) {
		super.setFilePatterns(filePatterns);
	}
	
}
