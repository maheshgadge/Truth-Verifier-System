package edu.uic.cs.dbis.jtv.misc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

public class Config {
	private static final Properties CONFIG = new Properties();
	static {
		InputStream inStream = ClassLoader
				.getSystemResourceAsStream("config.properties");

		try {
			CONFIG.load(inStream);
		} catch (IOException e) {
			throw new TVerifierException(e);
		}
	}
	// /////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////

	public static final int NUMBER_OF_SRR_TO_EXTRACT = Integer.parseInt(CONFIG
			.getProperty("NUMBER_OF_SRR_TO_EXTRACT"));
	// /////////////////////////////////////////////////////////////////////////

	public static final boolean CACHE_STATEMENT_SEARCH_RESULT = Boolean
			.parseBoolean(CONFIG.getProperty("CACHE_STATEMENT_SEARCH_RESULT"));
	// /////////////////////////////////////////////////////////////////////////

	private static final String _BING_SEARCH_APPLICATION_IDS = CONFIG
			.getProperty("BING_SEARCH_APPLICATION_IDS");
	private static final Set<String> _BING_SEARCH_APPLICATION_IDS_SET = new HashSet<String>();
	static {
		StringTokenizer tokenizer = new StringTokenizer(
				_BING_SEARCH_APPLICATION_IDS, ",; ");
		while (tokenizer.hasMoreTokens()) {
			_BING_SEARCH_APPLICATION_IDS_SET.add(tokenizer.nextToken().trim());
		}
	}
	public static final String[] BING_SEARCH_APPLICATION_IDS = _BING_SEARCH_APPLICATION_IDS_SET
			.toArray(new String[_BING_SEARCH_APPLICATION_IDS_SET.size()]);
	// /////////////////////////////////////////////////////////////////////////

	public static final double SENSE_CLOSENESS_ALPHA = Double
			.parseDouble(CONFIG.getProperty("SENSE_CLOSENESS_ALPHA"));
	public static final double SENSE_CLOSENESS_BETA = Double.parseDouble(CONFIG
			.getProperty("SENSE_CLOSENESS_BETA"));
	// /////////////////////////////////////////////////////////////////////////

	public static final String ALTER_UNITS_FILE = CONFIG
			.getProperty("ALTER_UNITS_FILE");
	public static final String DOUBTFUL_STATEMENTS_FILE = CONFIG
			.getProperty("DOUBTFUL_STATEMENTS_FILE");
	// /////////////////////////////////////////////////////////////////////////

	public static final int NUMBER_OF_ALTER_UNITS_TO_EXTRACT = Integer
			.parseInt(CONFIG.getProperty("NUMBER_OF_ALTER_UNITS_TO_EXTRACT"));
}
