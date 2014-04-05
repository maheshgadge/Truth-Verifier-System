package edu.uic.cs.dbis.jtv.scorer.impl;

import java.util.HashSet;
import java.util.List;

import edu.uic.cs.dbis.jtv.input.AbstractStatement;
import edu.uic.cs.dbis.jtv.web.SearchResult;

public class ResultQueryRelevanceScorer extends AbstractTextFeatureScorer {

	@Override
	public double score(String candidateTermsString,
			AbstractStatement statement, String queryString,
			List<SearchResult> srrList) {
		if (srrList.isEmpty()) {
			return 0.0;
		}

		HashSet<String> termsSet = getTermsSet(candidateTermsString);

		double count = 0;
		double queryRelevance = 0;
		for (SearchResult srr : srrList) {
			// there is any intersection
			if (doAllTermsInSRR(termsSet, srr)) {
				queryRelevance += calculateQueryRelevance(queryString, srr);
				count++;
			}
		}

		if (count == 0) {
			return 0.0;
		}

		return queryRelevance / count;
	}

	private double calculateQueryRelevance(String queryTermsString,
			SearchResult srr) {

		HashSet<String> queryTermsSet = getTermsSet(queryTermsString);
		HashSet<String> srrTermsSet = getSRRTermsSet(srr);

		HashSet<String> stemmedQueryTermsSet = stemSet(queryTermsSet);
		HashSet<String> stemmedSrrTermsSet = stemSet(srrTermsSet);

		// intersection
		stemmedSrrTermsSet.retainAll(stemmedQueryTermsSet);

		return ((double) stemmedSrrTermsSet.size())
				/ stemmedQueryTermsSet.size();
	}

	@Override
	public String getName() {
		return "RQR";
	}
}