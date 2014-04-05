package edu.uic.cs.dbis.jtv.web.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import net.billylieurance.azuresearch.AzureSearchResultSet;
import net.billylieurance.azuresearch.AzureSearchWebQuery;
import net.billylieurance.azuresearch.AzureSearchWebResult;
import edu.uic.cs.dbis.jtv.misc.Config;
import edu.uic.cs.dbis.jtv.misc.LogHelper;
import edu.uic.cs.dbis.jtv.web.SearchResult;
import edu.uic.cs.dbis.jtv.web.SearchResultExtractor;

public class BingSearchResultExtractor implements SearchResultExtractor {

	private static final Logger LOGGER = LogHelper
			.getLogger(BingSearchResultExtractor.class);

	private static int nextIDIndex = new Random()
			.nextInt(Config.BING_SEARCH_APPLICATION_IDS.length);

	public static void main(String[] args) {
		for (int index = 0; index < 10; index++) {
			System.out.println(getNextApplicationID());
		}
	}

	private static synchronized String getNextApplicationID() {
		String id = Config.BING_SEARCH_APPLICATION_IDS[nextIDIndex = (nextIDIndex + 1)
				% Config.BING_SEARCH_APPLICATION_IDS.length];

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Using Azure Application ID [" + id + "]");
		}

		return id;
	}

	/**
	 * bing.com's max per-page value is 50 <br>
	 * DO NOT change it if you don't know about it!
	 */
	private static final int RESULT_PER_PAGE = 50;
	private static final int TOTAL_PAGE_NUM = Config.NUMBER_OF_SRR_TO_EXTRACT
			/ RESULT_PER_PAGE;

	@Override
	public List<SearchResult> extractSearchResults(String queryString) {

		AzureSearchWebQuery webQuery = new AzureSearchWebQuery();
		webQuery.setAppid(getNextApplicationID());
		webQuery.setQuery(queryString);
		webQuery.setPerPage(RESULT_PER_PAGE);

		List<SearchResult> result = new ArrayList<SearchResult>(
				Config.NUMBER_OF_SRR_TO_EXTRACT);

		int resultRank = 1;
		int pageNum = 1;
		while (pageNum <= TOTAL_PAGE_NUM) {
			webQuery.doQuery();
			AzureSearchResultSet<AzureSearchWebResult> queryResults = webQuery
					.getQueryResult();
			if (queryResults == null) {
				break;
			}

			for (AzureSearchWebResult queryResult : queryResults) {
				String title = queryResult.getTitle();
				String description = queryResult.getDescription();
				String url = queryResult.getUrl();

				SearchResult searchResult = new SearchResult(resultRank++,
						title, description, url);
				result.add(searchResult);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Retrieved search result for [" + queryString
							+ "]: " + searchResult + " description="
							+ description);
				}
			}

			webQuery.nextPage();
			pageNum++;
		}

		return result;
	}

	// public static void main(String[] args) {
	// SearchResultExtractor searchResultExtractor = new
	// BingSearchResultExtractor();
	// List<SearchResult> searchResults = searchResultExtractor
	// .extractSearchResults("the eiffel tower is feet high");
	// for (SearchResult searchResult : searchResults) {
	// System.out.println(searchResult);
	// System.out.println(searchResult.getDescription());
	// System.out.println(searchResult.getUrl());
	// System.out.println("=============================================");
	// }
	//
	// searchResults = searchResultExtractor
	// .extractSearchResults("uic computer science");
	// for (SearchResult searchResult : searchResults) {
	// System.out.println(searchResult);
	// System.out.println(searchResult.getDescription());
	// System.out.println(searchResult.getUrl());
	// System.out.println("=============================================");
	// }
	// }
}
