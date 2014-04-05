package edu.uic.cs.dbis.jtv.feature.impl;

import java.util.List;

import edu.uic.cs.dbis.jtv.feature.Feature;
import edu.uic.cs.dbis.jtv.input.AbstractStatement;
import edu.uic.cs.dbis.jtv.input.DoubtfulStatement;
import edu.uic.cs.dbis.jtv.misc.Assert;
import edu.uic.cs.dbis.jtv.scorer.impl.AbstractTextFeatureScorer;
import edu.uic.cs.dbis.jtv.web.SearchResult;

public class TermLocalCorrelation extends AbstractTextFeatureScorer implements
		Feature {

	// private ResultCoverage rc = new ResultCoverage();

	@Override
	public double score(String candidateTermsString,
			AbstractStatement statement, String queryString,
			List<SearchResult> srrList) {
		if (srrList.isEmpty()) {
			return 0.0;
		}

		String doubtUnit = ((DoubtfulStatement) statement).getDoubtfulUnit();
		if (candidateTermsString.equals(doubtUnit)) {
			return 0.5;
		}

		// ////////////////////////////////////////////////////////////
		int candidateTermRecordCount = 0;
		int doubtUnitRecordCount = 0;
		int commonCount = 0;
		for (SearchResult srr : srrList) {
			List<String> termsInSRR = getSRRTermsList(srr);

			int candidateCount = termFrequency(candidateTermsString, termsInSRR);
			int doubtCount = termFrequency(doubtUnit, termsInSRR);

			candidateTermRecordCount += (candidateCount * candidateCount);
			doubtUnitRecordCount += (doubtCount * doubtCount);
			commonCount += candidateCount * doubtCount;
		}

		int x = candidateTermRecordCount + doubtUnitRecordCount - commonCount;
		if (x == 0) {
			return 0;
		}

		double result = ((double) commonCount) / ((double) x);
		Assert.isTrue(result >= 0);

		return result;
	}

	private int termFrequency(String term, List<String> termList) {
		int count = 0;
		for (String termInList : termList) {
			if (termInList.equals(term)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public String getName() {
		return "TLC";
	}

}
