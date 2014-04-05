package edu.uic.cs.dbis.jtv.input;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import edu.uic.cs.dbis.jtv.misc.Assert;
import edu.uic.cs.dbis.jtv.misc.Config;
import edu.uic.cs.dbis.jtv.misc.TVerifierException;

public class DoubtfulStatementsReader {

	private static final String AU_LEFT_SYMBOL = "['";
	private static final int AU_LEFT_SYMBOL_LENGTH = AU_LEFT_SYMBOL.length();
	private static final String AU_RIGHT_SYMBOL = "']";

	private static final String ANSWER_LEFT_SYMBOL = "{'";
	private static final int ANSWER_LEFT_SYMBOL_LENGTH = ANSWER_LEFT_SYMBOL
			.length();
	private static final String ANSWER_RIGHT_SYMBOL = "'}";

	private static final char[] DELIMITERS = " \t\n\r\f".toCharArray();

	private static final List<DoubtfulStatement> STATEMENTS = new ArrayList<DoubtfulStatement>();

	public static List<DoubtfulStatement> retrieveAllStatements() {
		if (STATEMENTS.isEmpty()) {
			return parseAllStatementsFromInputFiles();
		}

		return Collections.unmodifiableList(STATEMENTS);
	}

	private static List<DoubtfulStatement> parseAllStatementsFromInputFiles() {
		STATEMENTS.clear();

		String filePath = ClassLoader.getSystemResource(
				Config.DOUBTFUL_STATEMENTS_FILE).getPath();
		File file = new File(filePath);
		Collection<DoubtfulStatement> statements = parseStatementsFromOneInputFile(file);
		STATEMENTS.addAll(statements);

		return Collections.unmodifiableList(STATEMENTS);
	}

	private static Collection<DoubtfulStatement> parseStatementsFromOneInputFile(
			File file) {
		TreeMap<Integer, DoubtfulStatement> statementsById = new TreeMap<Integer, DoubtfulStatement>();
		try {
			@SuppressWarnings("unchecked")
			List<String> lines = FileUtils.readLines(file);

			for (String line : lines) {
				int idIndexEnd = StringUtils.indexOfAny(line, DELIMITERS);
				Assert.isTrue(idIndexEnd > 0);
				int id = Integer.parseInt(line.substring(0, idIndexEnd).trim());
				DoubtfulStatement statement = parseStatement(id, line,
						idIndexEnd);
				statementsById.put(id, statement);

			}
		} catch (IOException e) {
			throw new TVerifierException(e);
		}

		return statementsById.values();
	}

	private static DoubtfulStatement parseStatement(int id, String line,
			int idIndexEnd) {
		int begin = idIndexEnd;
		int end = line.lastIndexOf(AU_LEFT_SYMBOL);

		String statementString = line.substring(begin, end).trim();
		String doubtfulUnit = parseDoubtfulUnit(line, end);
		String correctAnswer = parseCorrectAnswer(line);

		int auIndex = statementString.indexOf(doubtfulUnit);
		Assert.isTrue(auIndex >= 0, "Can not mathch DU[" + doubtfulUnit
				+ "] in [" + statementString + "]. ");

		String left = statementString.substring(0, auIndex).trim();
		String right = statementString.substring(
				auIndex + doubtfulUnit.length()).trim();

		DoubtfulStatement result = new DoubtfulStatement(id, left, right);
		result.setDoubtfulUnit(doubtfulUnit);
		result.setCorrectAnswer(correctAnswer);

		return result;
	}

	private static String parseCorrectAnswer(String line) {
		int begin = line.lastIndexOf(ANSWER_LEFT_SYMBOL);
		int end = line.lastIndexOf(ANSWER_RIGHT_SYMBOL);
		return line.substring(begin + ANSWER_LEFT_SYMBOL_LENGTH, end);
	}

	private static String parseDoubtfulUnit(String line, int begin) {
		int end = line.lastIndexOf(AU_RIGHT_SYMBOL);
		return line.substring(begin + AU_LEFT_SYMBOL_LENGTH, end);
	}

	public static void main(String[] args) {
		List<DoubtfulStatement> statements = DoubtfulStatementsReader
				.retrieveAllStatements();
		for (DoubtfulStatement statement : statements) {
			String doubtfulUnit = statement.getDoubtfulUnit();
			String sentence = statement.getDoubtfulStatement();

			String originalSentence = statement.getId() + "\t"
					+ sentence.replace(doubtfulUnit, "[" + doubtfulUnit + "]");
			System.out.println(originalSentence + "\t"
					+ statement.getCorrectAnswer());
		}
	}
}
