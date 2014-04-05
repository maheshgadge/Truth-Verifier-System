package edu.uic.cs.dbis.jtv.scorer.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import edu.uic.cs.dbis.jtv.input.AbstractStatement;
import edu.uic.cs.dbis.jtv.nlp.TermUtils;
import edu.uic.cs.dbis.jtv.scorer.third_part.SmallestBestSnippet;
import edu.uic.cs.dbis.jtv.scorer.third_part.SmallestBestSnippet.TokenInfo;
import edu.uic.cs.dbis.jtv.web.SearchResult;

public class TermDistanceScorer extends AbstractTextFeatureScorer {

	@Override
	public double score(String candidateTermsString,
			AbstractStatement statement, String queryString,
			List<SearchResult> srrList) {
		if (srrList.isEmpty()) {
			return 0.0;
		}

		HashSet<String> termsSet = getTermsSet(candidateTermsString);
		HashSet<String> stemmedTermsSet = stemSet(termsSet);
		String stemmedCandidateTermsString = concatenate(stemmedTermsSet);

		double distanceScore = 0;
		for (SearchResult srr : srrList) {
			// there is any intersection

			if (doAllTermsInSRR(termsSet, srr)) {
				String description = srr.getDescription();
				List<SmallestBestSnippet.TokenInfo> descriptionTokens = SmallestBestSnippet
						.tokenize(description);
				int snipptLength = descriptionTokens.size();
				String stemmedDescription = stemDescription(descriptionTokens,
						description);

				int minWindow = calculateSmallestWindowSize(
						stemmedCandidateTermsString, stemmedDescription);
				int distance = snipptLength - minWindow;
				if (distance <= 0) {
					continue;
				}

				distanceScore += (distance * calculateQueryRelevance(
						queryString, srr));
			}
		}

		return distanceScore / srrList.size();
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

	private String stemDescription(List<TokenInfo> descriptionTokens,
			String description) {
		StringBuilder result = new StringBuilder();
		for (TokenInfo ti : descriptionTokens) {
			String token = description.substring(ti.offset, ti.len + ti.offset);

			result.append(TermUtils.stem(token)).append(" ");
		}

		return result.toString().trim();
	}

	private int calculateSmallestWindowSize(String candidateTermsString,
			String contentTermsString) {
		return SmallestBestSnippet.smallestWindowSize(candidateTermsString,
				contentTermsString);
	}

	private String concatenate(Collection<String> terms) {
		StringBuilder result = new StringBuilder();
		for (String term : terms) {
			result.append(term).append(" ");
		}

		return result.toString().trim();
	}

	@Override
	public String getName() {
		return "TD";
	}
}
