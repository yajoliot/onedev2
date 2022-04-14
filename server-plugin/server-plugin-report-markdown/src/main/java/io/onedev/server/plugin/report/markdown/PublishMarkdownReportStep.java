package io.onedev.server.plugin.report.markdown;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.utils.FileUtils;
import io.onedev.commons.utils.LockUtils;
import io.onedev.commons.utils.TaskLogger;
import io.onedev.server.buildspec.BuildSpec;
import io.onedev.server.buildspec.step.PublishReportStep;
import io.onedev.server.buildspec.step.StepGroup;
import io.onedev.server.model.Build;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Interpolative;

@Editable(order=1100, group=StepGroup.PUBLISH_REPORTS, name="Markdown")
public class PublishMarkdownReportStep extends PublishReportStep {

	private static final long serialVersionUID = 1L;
	
	public static final String CATEGORY = "markdown";
	
	public static final String START_PAGE = "$onedev-startpage$";
	
	private String startPage;
	
	@Editable(order=1100, description="Specify start page of the report relative to <a href='$docRoot/pages/concepts.md#job-workspace'>job workspace</a>, for instance: <tt>manual/index.md</tt>")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getStartPage() {
		return startPage;
	}

	public void setStartPage(String startPage) {
		this.startPage = startPage;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestVariables(String matchWith) {
		return BuildSpec.suggestVariables(matchWith, true, true);
	}

	@Override
	public Map<String, byte[]> run(Build build, File inputDir, TaskLogger logger) {
		LockUtils.write(getReportLockKey(build), new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				File startPage = new File(inputDir, getStartPage()); 
				if (startPage.exists()) {
					File reportDir = new File(build.getPublishDir(), CATEGORY + "/" + getReportName());

					FileUtils.createDir(reportDir);
					File startPageFile = new File(reportDir, START_PAGE);
					FileUtils.writeFile(startPageFile, getStartPage());
					
					int baseLen = inputDir.getAbsolutePath().length() + 1;
					for (File file: getPatternSet().listFiles(inputDir)) {
						try {
							FileUtils.copyFile(file, new File(reportDir, file.getAbsolutePath().substring(baseLen)));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					
				} else {
					logger.log("WARNING: Markdown report start page not found: " + startPage.getAbsolutePath());
				}
				return null;
			}
			
		});
		
		return null;
	}

	public static String getReportLockKey(Build build) {
		return PublishMarkdownReportStep.class.getName() + ":" + build.getId();
	}
	
}
