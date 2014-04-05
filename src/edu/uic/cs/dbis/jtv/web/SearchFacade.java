package edu.uic.cs.dbis.jtv.web;

import java.io.File;
import java.util.List;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Predicate;

import edu.uic.cs.dbis.jtv.misc.Assert;
import edu.uic.cs.dbis.jtv.misc.Config;
import edu.uic.cs.dbis.jtv.web.SearchResult;
import edu.uic.cs.dbis.jtv.web.SearchResultExtractor;
import edu.uic.cs.dbis.jtv.web.impl.BingSearchResultExtractor;

public class SearchFacade {

	private static final String DB4OFILENAME = "cache/search_result_cache.db4o";
	private static final SearchFacade INSTANCE = new SearchFacade();

	private final ObjectContainer db;

	private SearchFacade() {
		if (!Config.CACHE_STATEMENT_SEARCH_RESULT) {
			File dbFile = new File(DB4OFILENAME);
			if (dbFile.exists()) {
				Assert.isTrue(dbFile.renameTo(new File(dbFile.getAbsolutePath()
						+ ".bak")));
			}
		}

		db = Db4oEmbedded.openFile(DB4OFILENAME);
	}

	public static SearchFacade getInstance() {
		return INSTANCE;
	}

	private static class InnerStoreStructure {
		private String queryString;
		private List<SearchResult> searchResult;

		public InnerStoreStructure(String queryString,
				List<SearchResult> searchResult) {
			this.queryString = queryString;
			this.searchResult = searchResult;
		}

		public String getQueryString() {
			return queryString;
		}

		public List<SearchResult> getSearchResult() {
			return searchResult;
		}

	}

	private SearchResultExtractor searchResultExtractor = new BingSearchResultExtractor();

	public List<SearchResult> search(final String queryString) {
		if (Config.CACHE_STATEMENT_SEARCH_RESULT) {
			ObjectSet<InnerStoreStructure> resultInDB = db
					.query(new Predicate<InnerStoreStructure>() {
						private static final long serialVersionUID = 1L;

						@Override
						public boolean match(InnerStoreStructure candidate) {
							return queryString.equals(candidate
									.getQueryString());
						}
					});

			if (resultInDB != null && !resultInDB.isEmpty()) {
				return resultInDB.get(0).getSearchResult();
			}
		}

		List<SearchResult> result = searchResultExtractor
				.extractSearchResults(queryString);

		if (Config.CACHE_STATEMENT_SEARCH_RESULT) {
			db.store(new InnerStoreStructure(queryString, result));
			db.commit();
		}

		return result;
	}

	// private void printAllCachedSearchResults() {
	// ObjectSet<InnerStoreStructure> resultInDB = db
	// .query(InnerStoreStructure.class);
	//
	// for (InnerStoreStructure structure : resultInDB) {
	// System.out.println(structure.getQueryString());
	// Assert.isTrue(structure.getSearchResult().size() ==
	// Config.NUMBER_OF_SRR_TO_EXTRACT);
	// // for (SearchResult result : structure.getSearchResult()) {
	// // System.out.println(result);
	// // }
	// // System.out.println("===========================================");
	// }
	//
	// System.out.println("Total number: " + resultInDB.size());
	// }
	//
	// public static void main(String[] args) {
	// SearchFacade.getInstance().printAllCachedSearchResults();
	// }

}
