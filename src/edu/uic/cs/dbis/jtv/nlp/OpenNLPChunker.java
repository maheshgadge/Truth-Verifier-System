package edu.uic.cs.dbis.jtv.nlp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.util.Span;
import edu.stanford.nlp.ling.CoreLabel;
import edu.uic.cs.dbis.jtv.misc.TVerifierException;

public class OpenNLPChunker {

	public static enum ChunkType {
		NP, VP, PP, ADJP, ADVP, O;
	}

	private static final String CHUNKER_MODEL_NAME = "models/en-chunker.bin";
	private static ChunkerModel MODEL = getChunkerModel();

	private static ChunkerModel getChunkerModel() {
		try {
			InputStream modelIn = ClassLoader
					.getSystemResourceAsStream(CHUNKER_MODEL_NAME);
			return new ChunkerModel(modelIn);
		} catch (IOException ioe) {
			throw new TVerifierException(ioe);
		}
	}

	List<Span> getChunkSpans(List<CoreLabel> taggedTerms, ChunkType chunkType) {
		String[] terms = new String[taggedTerms.size()];
		String[] tags = new String[taggedTerms.size()];
		int index = 0;
		for (CoreLabel taggedTerm : taggedTerms) {
			terms[index] = taggedTerm.originalText();
			tags[index] = taggedTerm.tag();
			index++;
		}

		ChunkerME chunker = new ChunkerME(MODEL);
		Span[] spans = chunker.chunkAsSpans(terms, tags);

		List<Span> result = new ArrayList<Span>();
		for (Span span : spans) {
			if (chunkType.name().equals(span.getType())) {
				result.add(span);
			}
		}

		return result;
	}

	List<List<CoreLabel>> getChunks(List<CoreLabel> taggedTerms,
			ChunkType chunkType) {
		List<Span> spans = getChunkSpans(taggedTerms, chunkType);

		List<List<CoreLabel>> result = new ArrayList<List<CoreLabel>>();

		for (Span span : spans) {
			List<CoreLabel> phrase = taggedTerms.subList(span.getStart(),
					span.getEnd());
			result.add(phrase);
		}

		return result;
	}

	// public static void main(String[] args) throws IOException, IOException {
	// InputStream modelIn = ClassLoader
	// .getSystemResourceAsStream("models/en-pos-maxent.bin");
	// POSModel model = new POSModel(modelIn);
	// POSTaggerME tagger = new POSTaggerME(model);
	//
	// String context =
	// "I go to school at Stanford University at 8:00 ( Dec. 12th, 2012 ), which is located in California.";
	// String result = tagger.tag(context);
	// System.out.println(result);
	// }

}
