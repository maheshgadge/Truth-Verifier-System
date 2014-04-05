package edu.uic.cs.dbis.jtv.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.uic.cs.dbis.jtv.nlp.OpenNLPChunker.ChunkType;

public class NamedEntityUtils {

	public static enum NEClass {
		PERSON, LOCATION, ORGANIZATION, NUMBER, DATE, TIME, MONEY, PERCENT, OTHER;

		public static NEClass parse(String string) {
			if ("DURATION".equals(string)) {
				return NUMBER;
			} else if ("ORDINAL".equals(string) || "MISC".equals(string)
					|| "SET".equals(string) || "O".equals(string)) {
				return OTHER;
			}

			return NEClass.valueOf(string);
		}
	}

	private static final String POSTAG_POSSESSIVE = "POS";
	private static final List<String> POSTAG_PUNCTUATIONS = Arrays
			.asList(new String[] { ".", ",", ":" });
	private static final List<String> POSTAG_PARENTHESES = Arrays.asList(
			"-LRB-", "-RRB-");

	private static final OpenNLPChunker CHUNKER = new OpenNLPChunker();
	private static StanfordCoreNLP STANFORD_NLP = null;

	private static final String NAMED_ENTITIES_CACHE_FILE = "cache/named_entities_cache.db4o";
	private static final ObjectContainer CACHE_DB = Db4oEmbedded
			.openFile(NAMED_ENTITIES_CACHE_FILE);

	private static StanfordCoreNLP initializeStanfordCoreNLP() {

		Properties properties = new Properties();
		properties.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		properties
				.put("pos.model",
						"edu/stanford/nlp/models/pos-tagger/english-caseless-left3words-distsim.tagger");
		properties
				.put("parse.model",
						"edu/stanford/nlp/models/lexparser/englishPCFG.caseless.ser.gz");
		properties
				.put("ner.model",
						"edu/stanford/nlp/models/ner/english.all.3class.caseless.distsim.crf.ser.gz,edu/stanford/nlp/models/ner/english.muc.7class.caseless.distsim.crf.ser.gz,edu/stanford/nlp/models/ner/english.conll.4class.caseless.distsim.crf.ser.gz");

		return new StanfordCoreNLP(properties);
	}

	public static NEClass getNEClass(String phrase, String context) {
		phrase = phrase.toLowerCase();
		Map<NEClass, LinkedHashSet<String>> phraseByNEClass = getPhrasesByNEClass(context);

		int maxScore = 0;
		NEClass result = NEClass.OTHER;
		for (Entry<NEClass, LinkedHashSet<String>> entry : phraseByNEClass
				.entrySet()) {
			NEClass neClass = entry.getKey();
			if (neClass.equals(NEClass.OTHER)) {
				continue;
			}

			LinkedHashSet<String> phrasesIdentified = entry.getValue();
			for (String phraseIdentified : phrasesIdentified) {
				int score = longestCommonTermsCount(phraseIdentified, phrase);
				if (score > maxScore) {
					result = neClass;
					maxScore = score;
				}
			}
		}

		return result;
	}

	private static class InnerStoreStructure {
		private String context;
		private Map<NEClass, LinkedHashSet<String>> phrasesByNEClass;

		private InnerStoreStructure(String context,
				Map<NEClass, LinkedHashSet<String>> phrasesByNEClass) {
			this.context = context;
			this.phrasesByNEClass = phrasesByNEClass;
		}
	}

	private static volatile Map<String, Map<NEClass, LinkedHashSet<String>>> cachedDataInDB = null;

	public static Map<NEClass, LinkedHashSet<String>> getPhrasesByNEClass(
			final String context) {

		if (cachedDataInDB == null) {
			synchronized (NamedEntityUtils.class) {
				if (cachedDataInDB == null) {
					cachedDataInDB = readCachedDataFromDB();
				}
			}
		}

		Map<NEClass, LinkedHashSet<String>> result = cachedDataInDB
				.get(context);
		if (result != null) {
			return result;
		}

		result = getPhrasesByNEClassNoCache(context);
		cachedDataInDB.put(context, result);

		CACHE_DB.store(new InnerStoreStructure(context, result));
		CACHE_DB.commit();

		return result;
	}

	private static Map<String, Map<NEClass, LinkedHashSet<String>>> readCachedDataFromDB() {

		ObjectSet<InnerStoreStructure> cachedData = CACHE_DB
				.query(InnerStoreStructure.class);

		Map<String, Map<NEClass, LinkedHashSet<String>>> result = new HashMap<String, Map<NEClass, LinkedHashSet<String>>>(
				cachedData.size());
		for (InnerStoreStructure structure : cachedData) {
			result.put(structure.context, structure.phrasesByNEClass);
		}

		return result;
	}

	private static int longestCommonTermsCount(String firstTerm,
			String secondTerm) {
		if (firstTerm == null || secondTerm == null || firstTerm.length() == 0
				|| secondTerm.length() == 0) {
			return 0;
		}

		List<String> first = TermUtils.tokenize(firstTerm);
		List<String> second = TermUtils.tokenize(secondTerm);

		int maxLen = 0;
		int fl = first.size();
		int sl = second.size();
		int[][] table = new int[fl][sl];

		for (int i = 0; i < fl; i++) {
			for (int j = 0; j < sl; j++) {
				if (first.get(i).equals(second.get(j))) {
					if (i == 0 || j == 0) {
						table[i][j] = 1;
					} else {
						table[i][j] = table[i - 1][j - 1] + 1;
					}
					if (table[i][j] > maxLen) {
						maxLen = table[i][j];
					}
				}
			}
		}

		return maxLen;
	}

	private static Map<NEClass, LinkedHashSet<String>> getPhrasesByNEClassNoCache(
			String text) {

		Map<NEClass, LinkedHashSet<String>> result = new HashMap<NEClass, LinkedHashSet<String>>();

		Annotation document = new Annotation(text);
		if (STANFORD_NLP == null) {
			STANFORD_NLP = initializeStanfordCoreNLP();
		}
		STANFORD_NLP.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {

			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			List<CoreLabel> identifiedNonOtherTokens = new ArrayList<CoreLabel>();

			int phraseStartIndex = -1;
			NEClass phraseNEClass = null;

			for (int index = 0; index < tokens.size(); index++) {
				CoreLabel token = tokens.get(index);
				NEClass neClass = NEClass.parse(token.ner());

				// new type comes in, or not non-other yet
				if (neClass != phraseNEClass || phraseStartIndex == -1) {
					// finish previous one
					if (phraseStartIndex > -1) {
						String phrase = concatenate(tokens, phraseStartIndex,
								index);
						storePhrase(result, phraseNEClass, phrase);
					}

					// record the new start index
					if (neClass != NEClass.OTHER) {
						phraseStartIndex = index;
						identifiedNonOtherTokens.add(token);
					} else {
						phraseStartIndex = -1;
					}

					phraseNEClass = neClass;
				} else { // continue the same type
					identifiedNonOtherTokens.add(token);
				}
			}

			if (phraseStartIndex > -1) {
				String phrase = concatenate(tokens, phraseStartIndex,
						tokens.size());
				storePhrase(result, phraseNEClass, phrase);
			}

			// for all the noun phrases, if not been identified as any class
			// then it is a OTHER
			List<List<CoreLabel>> nounPhraseLables = CHUNKER.getChunks(tokens,
					ChunkType.NP);
			next_np: for (List<CoreLabel> nounPhrase : nounPhraseLables) {
				for (CoreLabel term : nounPhrase) {
					if (identifiedNonOtherTokens.contains(term)) {
						continue next_np;
					}
				}

				String phrase = concatenate(nounPhrase, 0, nounPhrase.size());
				phrase = TermUtils.trimStopWordsInBothSides(phrase);
				storePhrase(result, NEClass.OTHER, phrase);
			}
		}

		return result;
	}

	private static void storePhrase(Map<NEClass, LinkedHashSet<String>> result,
			NEClass neClass, String phrase) {
		if (phrase.isEmpty()) {
			return;
		}

		LinkedHashSet<String> phrases = result.get(neClass);
		if (phrases == null) {
			phrases = new LinkedHashSet<String>();
			result.put(neClass, phrases);
		}
		phrases.add(phrase.toLowerCase());
	}

	private static String concatenate(List<CoreLabel> tokens, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int index = start; index < end; index++) {
			CoreLabel token = tokens.get(index);

			String tag = token.tag();
			// ignore punctuation or parentheses
			if (POSTAG_PUNCTUATIONS.contains(tag)
					|| POSTAG_PARENTHESES.contains(tag)) {
				continue;
			}

			if (!POSTAG_POSSESSIVE.equals(tag) || index > start) {
				sb.append(" ");
			}
			sb.append(token.originalText());
		}

		return sb.toString().trim();
	}

	// public static void main(String[] args) {
	// // String context =
	// //
	// "I go to school at Stanford University at 8:00 (Dec. 12th, 2012), which is located in California.";
	// String context =
	// "richard nixon is the first u.s. president ever to resign";
	// Map<NEClass, LinkedHashSet<String>> result =
	// getPhrasesByNEClass(context);
	// for (Entry<NEClass, LinkedHashSet<String>> entry : result.entrySet()) {
	// System.out.println(entry);
	// }
	//
	// System.out.println();
	// // System.out.println(getNEClass("Dec. 12th", context));
	// // System.out.println(getNEClass("in California", context));
	// // System.out.println(getNEClass("Stanford", context));
	// System.out.println(getNEClass("richard nixon", context));
	// }
}
