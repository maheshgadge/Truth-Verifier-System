package edu.uic.cs.dbis.jtv.scorer.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import edu.uic.cs.dbis.jtv.misc.LogHelper;
import edu.uic.cs.dbis.jtv.nlp.TermUtils;
import edu.uic.cs.dbis.jtv.scorer.TextFeatureScorer;
import edu.uic.cs.dbis.jtv.web.SearchResult;

public abstract class AbstractTextFeatureScorer implements TextFeatureScorer {

	private static final Logger LOGGER = LogHelper
			.getLogger(AbstractTextFeatureScorer.class);

	protected boolean doAllTermsInSRR(HashSet<String> termsSet, SearchResult srr) {
		HashSet<String> srrTerms = getSRRTermsSet(srr);
		return srrTerms.containsAll(termsSet);
	}

	// protected boolean doesAnyTermInSRR(HashSet<String> termsSet,
	// SearchResult srr) {
	// HashSet<String> srrTerms = getSRRTermsSet(srr);
	// return !Collections.disjoint(termsSet, srrTerms);
	// }

	protected HashSet<String> getTermsSet(String termsString) {
		List<String> terms = TermUtils.tokenize(termsString);
		// Assert.isTrue(terms.size() > 0);
		if (terms.isEmpty()) {
			LOGGER.warn("No terms in String [" + termsString + "]. ");
		}
		HashSet<String> termsSet = new HashSet<String>(terms);
		return termsSet;
	}

	protected List<String> getSRRTermsList(SearchResult srr) {
		List<String> titleTerms = TermUtils.tokenize(srr.getTitle());
		List<String> descriptionTerms = TermUtils
				.tokenize(srr.getDescription());

		List<String> srrTerms = new ArrayList<String>();
		srrTerms.addAll(titleTerms);
		srrTerms.addAll(descriptionTerms);

		return srrTerms;
	}

	protected HashSet<String> getSRRTermsSet(SearchResult srr) {
		HashSet<String> srrTerms = new HashSet<String>();
		srrTerms.addAll(getSRRTermsList(srr));
		return srrTerms;
	}

	protected HashSet<String> stemSet(HashSet<String> queryTermsSet) {
		HashSet<String> result = new HashSet<String>(queryTermsSet.size());
		for (String term : queryTermsSet) {
			result.add(TermUtils.stem(term));
		}

		return result;
	}
}
