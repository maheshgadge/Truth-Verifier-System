package edu.uic.cs.dbis.jtv.nlp;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.PorterStemmerExporter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import edu.uic.cs.dbis.jtv.misc.TVerifierException;

public class TermUtils {

	private static final PorterStemmerExporter PORTER_STEMMER = new PorterStemmerExporter();

	private static final Set<String> STOP_WORDS = createStopWordsSet();

	private static Set<String> createStopWordsSet() {
		Set<String> stopWords = new HashSet<String>(Arrays.asList(new String[] {
				"i", "a", "about", "an", "are", "as", "at", "be", "by", "com",
				"and", "de", "en", "for", "from", "how", "in", "is", "it",
				"la", "of", "on", "or", "that", "the", "this", "to", "was",
				"what", "when", "where", "who", "will", "with", "und", "www",
				"htm", "html", "php", "pdf", "org", "he", "she", "b", "-",
				"do", "have", "there", "s" }));

		for (Object ob : StopAnalyzer.ENGLISH_STOP_WORDS_SET) {
			char[] charArray = (char[]) ob;
			stopWords.add(new String(charArray));
		}

		return stopWords;
	}

	public static String stem(String word) {
		return PORTER_STEMMER.stem(word);
	}

	public static boolean isStopWord(String term) {
		return STOP_WORDS.contains(term.toLowerCase());
	}

	/**
	 * NO stem! Lower-cased!
	 */
	public static List<String> tokenizeUsingDefaultStopWords(String rawString) {
		return standardAnalyze(rawString, STOP_WORDS);
	}

	/**
	 * NO stem! NO stop-words removing! Lower-cased!
	 */
	public static List<String> tokenize(String rawString) {
		return standardAnalyze(rawString, null);
	}

	/**
	 * NO stem!
	 * 
	 * StandardTokenizer -> StandardFilter -> LowerCaseFilter -> StopFilter
	 */
	private static List<String> standardAnalyze(String rawString,
			Set<String> stopWords) {

		TokenStream tokenStream = constructStandardTokenStream(rawString,
				stopWords);
		CharTermAttribute termAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);

		List<String> result = new ArrayList<String>();
		try {
			while (tokenStream.incrementToken()) {
				// replace ',' for number like '3,000,230'
				String term = termAttribute.toString().replace(",", "");
				if (term.endsWith("'s")) {
					term = term.substring(0, term.length() - 2);
					result.add(term);
					result.add("'s");
				} else if (term.endsWith("s'")) {
					term = term.substring(0, term.length() - 1);
					result.add(term);
				} else {
					result.add(term);
				}
			}
		} catch (IOException e) {
			throw new TVerifierException(e);
		}

		return result;
	}

	private static TokenStream constructStandardTokenStream(String rawString,
			Set<String> stopWords) {

		// Use LUCENE_30 to support "'s"
		StandardAnalyzer standardAnalyzer = new StandardAnalyzer(
				Version.LUCENE_36, stopWords);
		TokenStream tokenStream = standardAnalyzer.tokenStream(null,
				new StringReader(rawString));

		return tokenStream;
	}

	public static String trimStopWordsInBothSides(String rawString) {
		List<String> words = tokenize(rawString);

		int firstNonStopWordIndex = words.size();
		int lastNonStopWordIndex = -1;
		for (int index = 0; index < words.size(); index++) {
			String word = words.get(index);
			if (!isStopWord(word)) {
				firstNonStopWordIndex = index;
				break;
			}
		}

		for (int index = words.size() - 1; index >= 0; index--) {
			String word = words.get(index);
			if (!isStopWord(word)) {
				lastNonStopWordIndex = index;
				break;
			}
		}

		StringBuilder stringBuilder = new StringBuilder();
		if (lastNonStopWordIndex >= 0) {
			for (int index = firstNonStopWordIndex; index < lastNonStopWordIndex; index++) {
				stringBuilder.append(words.get(index));
				stringBuilder.append(' ');
			}
			stringBuilder.append(words.get(lastNonStopWordIndex));
		}

		return stringBuilder.toString().trim();
	}

	public static void main(String[] args) {
		System.out.println(trimStopWordsInBothSides("Pa | Pa"));
	}

}
