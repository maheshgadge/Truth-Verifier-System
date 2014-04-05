package edu.uic.cs.dbis.jtv.web;

import java.io.Serializable;

public class SearchResult implements Serializable {
	private static final long serialVersionUID = -4609806086455856496L;

	private int rank;
	private String title;
	private String description;
	private String url;

	public SearchResult(int rank, String title, String description, String url) {
		this.rank = rank;
		this.title = title;
		this.description = description;
		this.url = url;
	}

	public int getRank() {
		return rank;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return "SearchResult [rank=" + rank + ", title=" + title + "]";
	}
}
