package edu.uic.cs.dbis.jtv.web;

import java.util.List;

public interface SearchResultExtractor {

	List<SearchResult> extractSearchResults(String queryString);

}
