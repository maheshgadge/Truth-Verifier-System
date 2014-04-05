package edu.uic.cs.dbis.jtv.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.uic.cs.dbis.jtv.misc.Config;

public class AlternativeStatement extends AbstractStatement {
	private static final int DEFAULT_ALTERNATIVE_UNIT_NUMBER = Config.NUMBER_OF_ALTER_UNITS_TO_EXTRACT;

	private List<String> alternativeUnits = null;
	private List<String> allAlternativeStatements = null;

	public AlternativeStatement(int id, String topicUnitLeft, String topicUnitRight) {
		super(id, topicUnitLeft, topicUnitRight);
		this.alternativeUnits = new ArrayList<String>(
				DEFAULT_ALTERNATIVE_UNIT_NUMBER);
	}

	public void addAlternativeUnit(String alternativeUnit) {
		alternativeUnits.add(alternativeUnit.toLowerCase(Locale.US));
		allAlternativeStatements = null;
	}

	public List<String> getAllAlternativeStatements() {
		if (allAlternativeStatements != null) {
			return allAlternativeStatements;
		}

		allAlternativeStatements = new ArrayList<String>(
				alternativeUnits.size());
		for (String au : alternativeUnits) {
			String alternativeStatement = getTopicUnitLeft() + " " + au + " "
					+ getTopicUnitRight();
			allAlternativeStatements.add(alternativeStatement.trim());
		}

		return allAlternativeStatements;
	}

	public List<String> getAlternativeUnits() {
		return alternativeUnits;
	}
}
