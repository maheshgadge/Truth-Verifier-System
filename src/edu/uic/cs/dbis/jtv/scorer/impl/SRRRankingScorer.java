package edu.uic.cs.dbis.jtv.scorer.impl;

import java.util.HashSet;
import java.util.List;

import edu.uic.cs.dbis.jtv.input.AbstractStatement;
import edu.uic.cs.dbis.jtv.web.SearchResult;

public class SRRRankingScorer extends AbstractTextFeatureScorer {

	@Override
	public double score(String candidateTermsString,
			AbstractStatement statement, String queryString,
			List<SearchResult> srrList) {
		if (srrList.isEmpty()) {
			return 0.0;
		}

		HashSet<String> termsSet = getTermsSet(candidateTermsString);

		double countedRankScore = 0;
		double totalRankScore = 0;
		for (SearchResult srr : srrList) {
			double rankScore = 1 - ((double) srr.getRank()) / srrList.size();
			totalRankScore += rankScore;
			// there is any intersection
			if (doAllTermsInSRR(termsSet, srr)) {
				countedRankScore += rankScore;
			}
		}

		if (totalRankScore == 0) {
			return 0.0;
		}

		return countedRankScore / totalRankScore;
	}

	@Override
	public String getName() {
		return "Rrank";
	}

}
