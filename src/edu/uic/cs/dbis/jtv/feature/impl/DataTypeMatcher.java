package edu.uic.cs.dbis.jtv.feature.impl;

import org.apache.commons.lang3.StringUtils;

import edu.uic.cs.dbis.jtv.input.DoubtfulStatement;
import edu.uic.cs.dbis.jtv.nlp.NamedEntityUtils;
import edu.uic.cs.dbis.jtv.nlp.NamedEntityUtils.NEClass;

public class DataTypeMatcher {

	public static NEClass[] matchNamedEntityClass(
			DoubtfulStatement doubtfulStatement) {

		String doubtfulStatementString = doubtfulStatement
				.getDoubtfulStatement();
		String doubtfulUnit = doubtfulStatement.getDoubtfulUnit();
		NEClass doubtfulUnitNEClass = NamedEntityUtils.getNEClass(doubtfulUnit,
				doubtfulStatementString);

		// special cases
		if (doubtfulUnitNEClass.equals(NEClass.DATE)
				&& StringUtils.isNumeric(doubtfulUnit)) {
			return new NEClass[] { NEClass.DATE, NEClass.NUMBER };
		}

		return new NEClass[] { doubtfulUnitNEClass };

	}
}
