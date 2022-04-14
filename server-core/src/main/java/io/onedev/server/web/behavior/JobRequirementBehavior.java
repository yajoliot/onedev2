package io.onedev.server.web.behavior;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.onedev.commons.codeassist.AntlrUtils;
import io.onedev.commons.codeassist.FenceAware;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.commons.codeassist.parser.Element;
import io.onedev.commons.codeassist.parser.ParseExpect;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.server.OneDev;
import io.onedev.server.job.requirement.JobRequirementLexer;
import io.onedev.server.job.requirement.JobRequirementParser;
import io.onedev.server.job.requirement.JobRequirement;
import io.onedev.server.model.Build;
import io.onedev.server.search.entity.project.ProjectQuery;
import io.onedev.server.web.behavior.inputassist.ANTLRAssistBehavior;
import io.onedev.server.web.util.SuggestionUtils;

@SuppressWarnings("serial")
public class JobRequirementBehavior extends ANTLRAssistBehavior {

	public JobRequirementBehavior() {
		super(JobRequirementParser.class, "jobRequirement", false);
	}

	@Override
	protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
		if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
			LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
			if (spec.getRuleName().equals("Quoted")) {
				return new FenceAware(codeAssist.getGrammar(), '"', '"') {

					@Override
					protected List<InputSuggestion> match(String matchWith) {
						if ("criteriaField".equals(spec.getLabel())) {
							List<String> fields = Lists.newArrayList(Build.NAME_PROJECT);
							return SuggestionUtils.suggest(fields, matchWith);
						} else if ("criteriaValue".equals(spec.getLabel())) {
							List<Element> operatorElements = terminalExpect.getState().findMatchedElementsByLabel("operator", true);
							Preconditions.checkState(operatorElements.size() == 1);
							String operatorName = StringUtils.normalizeSpace(operatorElements.get(0).getMatchedText());
							int operator = AntlrUtils.getLexerRule(JobRequirementLexer.ruleNames, operatorName);							
							if (operator == JobRequirementLexer.Is) {
								List<Element> fieldElements = terminalExpect.getState().findMatchedElementsByLabel("criteriaField", true);
								Preconditions.checkState(fieldElements.size() == 1);
								String fieldName = JobRequirement.getValue(fieldElements.get(0).getMatchedText());
								if (fieldName.equals(Build.NAME_PROJECT)) {
									if (!matchWith.contains("*") && !matchWith.contains("?"))
										return SuggestionUtils.suggestProjects(matchWith);
									else
										return null;
								}
							}
						} 
						return null;
					}
					
					@Override
					protected String getFencingDescription() {
						return "value should be quoted";
					}
					
				}.suggest(terminalExpect);
			}
		} 
		return null;
	}
	
	@Override
	protected Optional<String> describe(ParseExpect parseExpect, String suggestedLiteral) {
		parseExpect = parseExpect.findExpectByLabel("operator");
		if (parseExpect != null) {
			List<Element> fieldElements = parseExpect.getState().findMatchedElementsByLabel("criteriaField", false);
			if (!fieldElements.isEmpty()) {
				String fieldName = JobRequirement.getValue(fieldElements.iterator().next().getMatchedText());
				try {
					JobRequirement.checkField(fieldName, AntlrUtils.getLexerRule(JobRequirementLexer.ruleNames, suggestedLiteral));
				} catch (ExplicitException e) {
					return null;
				}
			}
		}
		return super.describe(parseExpect, suggestedLiteral);
	}
	
	@Override
	protected List<String> getHints(TerminalExpect terminalExpect) {
		List<String> hints = new ArrayList<>();
		if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
			LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
			if ("criteriaValue".equals(spec.getLabel())) {
				List<Element> fieldElements = terminalExpect.getState().findMatchedElementsByLabel("criteriaField", true);
				if (!fieldElements.isEmpty()) {
					String fieldName = ProjectQuery.getValue(fieldElements.get(0).getMatchedText());
					if (fieldName.equals(Build.NAME_PROJECT))
						hints.add("Use '**', '*' or '?' for <a href='" + OneDev.getInstance().getDocRoot() + "/pages/path-wildcard.md' target='_blank'>path wildcard match</a>");
					else 
						hints.add("Use '*' for wildcard match");
				}
			}
		} 
		return hints;
	}
	
}
