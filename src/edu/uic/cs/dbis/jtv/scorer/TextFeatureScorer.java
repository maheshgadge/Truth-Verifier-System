package edu.uic.cs.dbis.jtv.scorer;

import java.util.List;

import edu.uic.cs.dbis.jtv.input.AbstractStatement;
import edu.uic.cs.dbis.jtv.web.SearchResult;

public interface TextFeatureScorer {

	double score(String candidateTermsString, AbstractStatement statement,
			String queryString, List<SearchResult> srrList/*, NEClass neClass*/);

	String getName();
}
