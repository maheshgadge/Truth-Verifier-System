package edu.uic.cs.dbis.jtv.scorer.impl;

import java.util.HashSet;
import java.util.List;

import edu.uic.cs.dbis.jtv.input.AbstractStatement;
import edu.uic.cs.dbis.jtv.web.SearchResult;

public class ResultCoverageScorer extends AbstractTextFeatureScorer {

	@Override
	public double score(String candidateTermsString,
			AbstractStatement statement, String queryString,
			List<SearchResult> srrList/*, NEClass neClass*/) {
		if (srrList.isEmpty()) {
			return 0.0;
		}

		HashSet<String> termsSet = getTermsSet(candidateTermsString);

		double count = 0;
		for (SearchResult srr : srrList) {
			// there is any intersection
			if (doAllTermsInSRR(termsSet, srr)) {
				count++;
			}
		}

		return count / srrList.size();
	}

	@Override
	public String getName() {
		return "RC";
	}
}
