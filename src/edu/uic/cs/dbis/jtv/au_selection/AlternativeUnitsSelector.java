package edu.uic.cs.dbis.jtv.au_selection;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import edu.stanford.nlp.util.StringUtils;
import edu.uic.cs.dbis.jtv.feature.Feature;
import edu.uic.cs.dbis.jtv.feature.impl.DataTypeMatcher;
import edu.uic.cs.dbis.jtv.feature.impl.ResultCoverage;
import edu.uic.cs.dbis.jtv.feature.impl.ResultQueryRelevance;
import edu.uic.cs.dbis.jtv.feature.impl.SRRRanking;
import edu.uic.cs.dbis.jtv.feature.impl.SenseCloseness;
import edu.uic.cs.dbis.jtv.feature.impl.TermDistance;
import edu.uic.cs.dbis.jtv.feature.impl.TermLocalCorrelation;
import edu.uic.cs.dbis.jtv.input.DoubtfulStatement;
import edu.uic.cs.dbis.jtv.input.DoubtfulStatementsReader;
import edu.uic.cs.dbis.jtv.input.AlternativeStatement;
import edu.uic.cs.dbis.jtv.misc.Assert;
import edu.uic.cs.dbis.jtv.misc.Config;
import edu.uic.cs.dbis.jtv.misc.LogHelper;
import edu.uic.cs.dbis.jtv.misc.SerializationHelper;
import edu.uic.cs.dbis.jtv.misc.TVerifierException;
import edu.uic.cs.dbis.jtv.nlp.NamedEntityUtils;
import edu.uic.cs.dbis.jtv.nlp.PersonNameUtils;
import edu.uic.cs.dbis.jtv.nlp.NamedEntityUtils.NEClass;
import edu.uic.cs.dbis.jtv.nlp.TermUtils;
import edu.uic.cs.dbis.jtv.web.SearchFacade;
import edu.uic.cs.dbis.jtv.web.SearchResult;

public class AlternativeUnitsSelector {

	private static final Logger LOGGER = LogHelper
			.getLogger(AlternativeUnitsSelector.class);

	private static final String INSTANCE_CACHE_FILE = "cache/au_selection_training_instances.cache";

	private static class InstanceWithAU implements Serializable {
		private static final long serialVersionUID = 8014031383665089068L;

		private Instance instance;
		private String alternativeUnit;

		private InstanceWithAU(Instance instance, String alternativeUnit) {
			this.instance = instance;
			this.alternativeUnit = alternativeUnit;
		}
	}

	private static Map<Integer, List<InstanceWithAU>> CACHED_INSTANCE_BY_ID = null;

	private static final String DATASET_NAME = "AU_SELECTION";

	private Classifier classifier = new LinearRegression();

	private List<Feature> features;
	private FastVector featureDefs;

	private Instances trainingDataset;

	public AlternativeUnitsSelector() {
		features = new ArrayList<Feature>(Arrays.asList(new Feature[] {
				new ResultCoverage(), //
				new ResultQueryRelevance(), //
				new SRRRanking(), //
				new TermDistance(),//
				new SenseCloseness(), //
				new TermLocalCorrelation() //
				}));

		featureDefs = defineFeatures(features);
	}

	public void learn(List<DoubtfulStatement> doubtfulStatements) {
		trainingDataset = createDataset(doubtfulStatements);
		try {
			classifier.buildClassifier(trainingDataset);
		} catch (Exception e) {
			throw new TVerifierException(e);
		}
	}

	public Map<String, Double> predict(DoubtfulStatement doubtfulStatement) {

		Assert.notNull(trainingDataset, "You must learn the model first!");

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Predicting: " + doubtfulStatement);
		}

		TreeMap<Double, String> alternativeUnitsByScore = new TreeMap<Double, String>(
				Collections.reverseOrder());

		List<InstanceWithAU> instances = getInstanceFromCachedFile(doubtfulStatement);
		if (instances == null) {
			instances = createInstances(doubtfulStatement);
			addInstancesIntoCache(doubtfulStatement, instances);
		}

		for (InstanceWithAU instance : instances) {
			Instance classMissing = (Instance) instance.instance.copy();
			classMissing.setDataset(trainingDataset);
			classMissing.setClassMissing();

			try {
				double score = classifier.classifyInstance(classMissing);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(score + "|\t" + classMissing);
				}

				alternativeUnitsByScore.put(score, instance.alternativeUnit);

			} catch (Exception e) {
				throw new TVerifierException(e);
			}
		}

		Map<String, Double> result = new LinkedHashMap<String, Double>(
				Config.NUMBER_OF_ALTER_UNITS_TO_EXTRACT);
		int index = 0;
		for (Entry<Double, String> candidateAUByScore : alternativeUnitsByScore
				.entrySet()) {
			if (index++ >= Config.NUMBER_OF_ALTER_UNITS_TO_EXTRACT) {
				break;
			}

			result.put(candidateAUByScore.getValue(),
					candidateAUByScore.getKey());
		}

		return result;
	}

	private Instances createDataset(List<DoubtfulStatement> doubtfulStatements) {

		Instances dataSet = new Instances(DATASET_NAME, featureDefs, 0);
		// Set class index
		dataSet.setClassIndex(featureDefs.size() - 1);

		// add into dataset
		for (DoubtfulStatement doubtfulStatement : doubtfulStatements) {

			LOGGER.debug("Training model using: " + doubtfulStatement);

			List<InstanceWithAU> instances = getInstanceFromCachedFile(doubtfulStatement);
			if (instances == null) {
				instances = createInstances(doubtfulStatement);
				addInstancesIntoCache(doubtfulStatement, instances);
			}

			for (InstanceWithAU instance : instances) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(instance.instance);
				}

				dataSet.add(instance.instance);
			}
		}

		// // normalize [0, 1]
		// Normalize normalize = new Normalize();
		// // un-comment below for normalize [-1, 1]
		// // normalize.setScale(2.0);
		// // normalize.setTranslation(-1.0);
		// try {
		// normalize.setInputFormat(dataSet);
		// dataSet = Filter.useFilter(dataSet, normalize);
		// } catch (Exception e) {
		// throw new TVerifierException(e);
		// }

		return dataSet;
	}

	private void addInstancesIntoCache(DoubtfulStatement doubtfulStatement,
			List<InstanceWithAU> instances) {
		Assert.notNull(CACHED_INSTANCE_BY_ID);

		CACHED_INSTANCE_BY_ID.put(doubtfulStatement.getId(), instances);

		// remove old one
		FileUtils.deleteQuietly(new File(INSTANCE_CACHE_FILE));

		// create new one
		SerializationHelper.serialize(CACHED_INSTANCE_BY_ID,
				INSTANCE_CACHE_FILE);
	}

	@SuppressWarnings("unchecked")
	private List<InstanceWithAU> getInstanceFromCachedFile(
			DoubtfulStatement doubtfulStatement) {
		if (CACHED_INSTANCE_BY_ID == null) {
			CACHED_INSTANCE_BY_ID = (Map<Integer, List<InstanceWithAU>>) SerializationHelper
					.deserialize(INSTANCE_CACHE_FILE);

			if (CACHED_INSTANCE_BY_ID == null) {
				CACHED_INSTANCE_BY_ID = new HashMap<Integer, List<InstanceWithAU>>();
			}
		}

		return CACHED_INSTANCE_BY_ID.get(doubtfulStatement.getId());
	}

	private List<InstanceWithAU> createInstances(
			DoubtfulStatement doubtfulStatement) {

		String queryString = doubtfulStatement.getTopicUnit();
		List<SearchResult> srrList = SearchFacade.getInstance().search(
				queryString);

		String correctAnswer = doubtfulStatement.getCorrectAnswer();

		LinkedHashSet<String> candidateAUs = gatherCandidateAlternativeUnits(
				doubtfulStatement, srrList);

		List<InstanceWithAU> result = new ArrayList<InstanceWithAU>(
				candidateAUs.size());
		for (String candidateAU : candidateAUs) {

			Instance instance = createTrainingInstance(doubtfulStatement,
					queryString, srrList, correctAnswer, candidateAU);

			result.add(new InstanceWithAU(instance, candidateAU));
		}

		return result;
	}

	private Instance createTrainingInstance(
			DoubtfulStatement doubtfulStatement, String queryString,
			List<SearchResult> srrList, String correctAnswer, String candidateAU) {

		Instance instance = new Instance(featureDefs.size());

		int index = 0;
		for (Feature feature : features) {
			double value = feature.score(candidateAU, doubtfulStatement,
					queryString, srrList);
			instance.setValue((Attribute) featureDefs.elementAt(index++), value);
		}

		double accuracy = measureCandidateAUAccuracy(candidateAU, correctAnswer);
		// the final one is the target
		instance.setValue((Attribute) featureDefs.elementAt(index++), accuracy);
		Assert.isTrue(index == featureDefs.size());
		return instance;
	}

	private double measureCandidateAUAccuracy(String candidateAU,
			String correctAnswer) {

		List<String> correctTerms = TermUtils.tokenize(correctAnswer);
		List<String> candidateAUTerms = TermUtils.tokenize(candidateAU);

		double tp = 0;
		for (String candidateTerm : candidateAUTerms) {
			if (correctTerms.contains(candidateTerm)) {
				tp++;
			}
		}

		double recall = tp / correctTerms.size();
		double precision = tp / candidateAUTerms.size();

		if ((precision + recall) == 0) {
			return 0;
		}
		return 2 * precision * recall / (precision + recall);
	}

	private LinkedHashSet<String> gatherCandidateAlternativeUnits(
			DoubtfulStatement doubtfulStatement, List<SearchResult> srrList) {

		NEClass[] matchedClasses = DataTypeMatcher
				.matchNamedEntityClass(doubtfulStatement);

		LinkedHashSet<String> candidateAUs = new LinkedHashSet<String>();

		for (SearchResult srr : srrList) {
			// construct a whole text string
			String text = srr.getTitle() + ". " + srr.getDescription();
			Map<NEClass, LinkedHashSet<String>> phrasesByNEClassInSRR = NamedEntityUtils
					.getPhrasesByNEClass(text);

			for (NEClass neClassMatched : matchedClasses) {
				Set<String> phrases = phrasesByNEClassInSRR.get(neClassMatched);
				if (phrases == null) {
					continue;
				}

				if (neClassMatched == NEClass.OTHER) {
					// for OTHER type, removes all stop-words (true)
					candidateAUs.addAll(trimStopWordsInBothSides(phrases));
				} else if (neClassMatched == NEClass.DATE) {
					for (String phrase : phrases) {
						// TODO currently, we only accept numeric date
						if (StringUtils.isNumeric(phrase)) {
							candidateAUs.add(phrase);
						}
					}
				} else if (neClassMatched == NEClass.NUMBER) {
					for (String phrase : phrases) {
						phrase = phrase.replace(",", "");
						// TODO we don't accept numbers like "one", "two"... now
						if (StringUtils.isNumeric(phrase)) {
							candidateAUs.add(phrase);
						}
					}
				} else if (neClassMatched == NEClass.PERSON) {
					for (String phrase : phrases) {
						if (PersonNameUtils.isPossiblePersonName(phrase)) {
							candidateAUs.add(phrase);
						}
					}
				} else {
					candidateAUs.addAll(phrases);
				}
			}
		}

		return filterOutTopicUnitTerms(candidateAUs,
				doubtfulStatement.getTopicUnit());
		// return candidateAUs;
	}

	private LinkedHashSet<String> filterOutTopicUnitTerms(
			LinkedHashSet<String> candidateAUs, String topicUnits) {
		List<String> topicTerms = TermUtils
				.tokenizeUsingDefaultStopWords(topicUnits);

		LinkedHashSet<String> reult = new LinkedHashSet<String>();
		for (String candidateAU : candidateAUs) {
			List<String> auTerms = TermUtils
					.tokenizeUsingDefaultStopWords(candidateAU);
			if (Collections.disjoint(topicTerms, auTerms)) {
				reult.add(candidateAU);
			}
		}

		return reult;
	}

	private Collection<String> trimStopWordsInBothSides(Set<String> phrases) {
		Set<String> result = new LinkedHashSet<String>(phrases.size());
		for (String phrase : phrases) {
			String trimmedPhrase = TermUtils.trimStopWordsInBothSides(phrase);
			if (trimmedPhrase.length() == 0) {
				continue;
			}
			result.add(trimmedPhrase);
		}
		return result;
	}

	private static FastVector defineFeatures(List<Feature> featuresToUse) {

		FastVector result = new FastVector();

		for (Feature definition : featuresToUse) {
			result.addElement(new Attribute(definition.getName()));
		}

		// Declare the target attribute
		result.addElement(new Attribute("TARGET"));

		return result;
	}

	public AlternativeStatement getAternativeStatement(
			DoubtfulStatement doubtfulStatement) {
		AlternativeStatement result = new AlternativeStatement(
				doubtfulStatement.getId(),
				doubtfulStatement.getTopicUnitLeft(),
				doubtfulStatement.getTopicUnitRight());

		List<String> alternativeUnits = getAlternativeUnits(doubtfulStatement);
		for (String alternativeUnit : alternativeUnits) {
			result.addAlternativeUnit(alternativeUnit);
		}

		return result;
	}

	private List<String> getAlternativeUnits(DoubtfulStatement doubtfulStatement) {
		List<DoubtfulStatement> doubtfulStatements = DoubtfulStatementsReader
				.retrieveAllStatements();
		List<DoubtfulStatement> trainingData = new ArrayList<DoubtfulStatement>(
				doubtfulStatements);
		Assert.isTrue(trainingData.remove(doubtfulStatement));

		AlternativeUnitsSelector selector = new AlternativeUnitsSelector();
		selector.learn(trainingData);
		Map<String, Double> result = selector.predict(doubtfulStatement);

		return new ArrayList<String>(result.keySet());
	}

	public static void main(String[] args) {

		AlternativeUnitsSelector selector = new AlternativeUnitsSelector();

		List<DoubtfulStatement> doubtfulStatements = DoubtfulStatementsReader
				.retrieveAllStatements();
		for (DoubtfulStatement doubtfulStatement : doubtfulStatements) {
			System.out.println(doubtfulStatement);
			System.out.println("\t"
					+ selector.getAlternativeUnits(doubtfulStatement));
			System.out.println("=============================================");
		}
	}

}
