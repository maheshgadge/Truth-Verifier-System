package edu.uic.cs.dbis.jtv.feature.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.mit.jwi.item.POS;
import edu.uic.cs.dbis.jtv.feature.Feature;
import edu.uic.cs.dbis.jtv.input.AbstractStatement;
import edu.uic.cs.dbis.jtv.input.DoubtfulStatement;
import edu.uic.cs.dbis.jtv.misc.Config;
import edu.uic.cs.dbis.jtv.nlp.NamedEntityUtils;
import edu.uic.cs.dbis.jtv.nlp.NamedEntityUtils.NEClass;
import edu.uic.cs.dbis.jtv.nlp.TermUtils;
import edu.uic.cs.dbis.jtv.web.SearchResult;
import edu.uic.cs.dbis.jtv.wordnet.WordNetUtils;

public class SenseCloseness implements Feature {

	@Override
	public double score(String candidateTermsString,
			AbstractStatement statement, String queryString,
			List<SearchResult> srrList) {
		DoubtfulStatement doubtfulStatement = (DoubtfulStatement) statement;

		String doubtfulStatementString = doubtfulStatement
				.getDoubtfulStatement();
		String doubtUnit = doubtfulStatement.getDoubtfulUnit();
		NEClass doubtfulUnitNEClass = NamedEntityUtils.getNEClass(doubtUnit,
				doubtfulStatementString);
		if (doubtfulUnitNEClass != NEClass.OTHER) {
			return 1.0;
		}

		// /////////////////////////////////////////////////////////////////////
		candidateTermsString = candidateTermsString.toLowerCase().trim();
		doubtUnit = doubtUnit.toLowerCase().trim();

		if (candidateTermsString.equals(doubtUnit)) {
			return 0.5;
		}

		double maxScore = 0;

		maxScore = Math.max(
				maxScore,
				computeCloseness(
						Collections.singletonList(candidateTermsString),
						Collections.singletonList(doubtUnit)));

		List<String> candidateTerms = TermUtils
				.tokenizeUsingDefaultStopWords(candidateTermsString);
		List<String> doubtUnitTerms = TermUtils
				.tokenizeUsingDefaultStopWords(doubtUnit);

		if (doubtUnitTerms.size() > 1) {
			maxScore = Math.max(
					maxScore,
					computeCloseness(candidateTerms,
							Collections.singletonList(doubtUnit)));
			maxScore = Math.max(
					maxScore,
					computeCloseness(
							Collections.singletonList(candidateTermsString),
							doubtUnitTerms));
			maxScore = Math.max(maxScore,
					computeCloseness(candidateTerms, doubtUnitTerms));
		}

		return maxScore;
	}

	private double computeCloseness(List<String> terms1, List<String> terms2) {

		double maxScore = 0.0;
		for (String term1 : terms1) {
			for (String term2 : terms2) {
				double score = computeCloseness(term1, term2);
				maxScore = Math.max(maxScore, score);
			}
		}

		return maxScore;
	}

	private double computeCloseness(String term1, String term2) {

		double maxScore = 0.0;

		if (isHypernym(term1, term2) || isHypernym(term2, term1)) {
			maxScore = Config.SENSE_CLOSENESS_ALPHA;
		} else if (WordNetUtils.areSiblings(term1, term2, POS.NOUN)) {
			maxScore = Config.SENSE_CLOSENESS_BETA;
		}

		maxScore = Math.max(maxScore,
				WordNetUtils.wupSimilarity(term1, term2, POS.NOUN));

		return maxScore;
	}

	/**
	 * If term2 is term1's hypernym
	 * 
	 * @param term1
	 * @param term2
	 * @return
	 */
	private boolean isHypernym(String term1, String term2) {
		Set<String> hypernyms1 = WordNetUtils.getDirectHypernyms(term1,
				POS.NOUN);
		hypernyms1.addAll(WordNetUtils.getDirectInstanceHypernyms(term1,
				POS.NOUN));

		return hypernyms1.contains(term2);
	}

	@Override
	public String getName() {
		return "SC";
	}

}
