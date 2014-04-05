package edu.uic.cs.dbis.jtv.wordnet;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.sussex.nlp.jws.JWS;
import edu.sussex.nlp.jws.Path;
import edu.sussex.nlp.jws.WuAndPalmer;

public class WordNetUtils {

	private static final String WORDNET_PATH = "WordNet";
	private static final JWS WORDNET_SIMILARITY = new JWS(WORDNET_PATH, "3.0");

	private static final IDictionary WORDNET = WORDNET_SIMILARITY
			.getDictionary();

	public static double wupSimilarity(String term1, String term2, POS pos) {
		WuAndPalmer wuAndPalmer = WORDNET_SIMILARITY.getWuAndPalmer();
		return wuAndPalmer.max(term1, term2, Character.toString(pos.getTag()));
	}

	public static double pathSimilarity(String term1, String term2, POS pos) {
		Path path = WORDNET_SIMILARITY.getPath();
		return path.max(term1, term2, Character.toString(pos.getTag()));
	}

	public static Set<String> getDirectHypernyms(String term, POS pos) {
		return getRelatedTerms(term, pos, Pointer.HYPERNYM);
	}

	public static Set<String> getDirectInstanceHypernyms(String term, POS pos) {
		return getRelatedTerms(term, pos, Pointer.HYPERNYM_INSTANCE);
	}

	public static Set<String> getDirectHyponyms(String term, POS pos) {
		return getRelatedTerms(term, pos, Pointer.HYPONYM);
	}

	public static Set<String> getDirectInstanceHyponyms(String term, POS pos) {
		return getRelatedTerms(term, pos, Pointer.HYPONYM_INSTANCE);
	}

	private static Set<String> getRelatedTerms(String term, POS pos,
			Pointer pointer) {
		IIndexWord idxWord = WORDNET.getIndexWord(term, pos);
		if (idxWord == null) {
			return Collections.emptySet();
		}

		Set<String> result = new TreeSet<String>();
		for (IWordID wordID : idxWord.getWordIDs()) {
			IWord word = WORDNET.getWord(wordID);
			ISynset synset = word.getSynset();
			List<ISynsetID> hypernymSynsets = synset.getRelatedSynsets(pointer);
			if (hypernymSynsets == null) {
				continue;
			}

			for (ISynsetID sid : hypernymSynsets) {
				List<IWord> hypernyms = WORDNET.getSynset(sid).getWords();
				for (IWord hypernym : hypernyms) {
					String wordString = hypernym.getLemma();
					wordString = wordString.replace("_", " ");
					result.add(wordString);
				}
			}
		}

		return result;
	}

	public static boolean areSiblings(String term1, String term2, POS pos) {
		// share the same direct hypernym or instance hypernym

		Set<String> hypernyms1 = getDirectHypernyms(term1, pos);
		hypernyms1.addAll(getDirectInstanceHypernyms(term1, pos));

		Set<String> hypernyms2 = getDirectHypernyms(term2, pos);
		hypernyms2.addAll(getDirectInstanceHypernyms(term2, pos));

		return !Collections.disjoint(hypernyms1, hypernyms2);
	}

}
