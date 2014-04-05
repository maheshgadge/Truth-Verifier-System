package edu.uic.cs.dbis.jtv.input;

public class DoubtfulStatement extends AbstractStatement {

	private String doubtfulUnit = null;
	private String correctAnswer = null;

	public DoubtfulStatement(int id, String topicUnitLeft, String topicUnitRight) {
		super(id, topicUnitLeft, topicUnitRight);
	}

	public String getDoubtfulUnit() {
		return doubtfulUnit;
	}

	void setDoubtfulUnit(String doubtfulUnit) {
		this.doubtfulUnit = doubtfulUnit;
	}

	public String getCorrectAnswer() {
		return correctAnswer;
	}

	void setCorrectAnswer(String correctAnswer) {
		this.correctAnswer = correctAnswer;
	}

	public String getDoubtfulStatement() {
		String doubtfulStatement = getTopicUnitLeft() + " " + doubtfulUnit
				+ " " + getTopicUnitRight();
		return doubtfulStatement.trim();
	}

	@Override
	public String toString() {
		return getId()
				+ "\t"
				+ getDoubtfulStatement().replace(getDoubtfulUnit(),
						"[" + getDoubtfulUnit() + "]") + "\t{'"
				+ getCorrectAnswer() + "'}";
	}
}
