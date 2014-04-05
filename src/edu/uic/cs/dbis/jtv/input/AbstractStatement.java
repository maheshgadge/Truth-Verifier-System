package edu.uic.cs.dbis.jtv.input;

public abstract class AbstractStatement {

	private int id = -1;
	private String topicUnitLeft = "";
	private String topicUnitRight = "";

	public AbstractStatement(int id, String topicUnitLeft, String topicUnitRight) {
		this.id = id;
		this.topicUnitLeft = topicUnitLeft != null ? topicUnitLeft.trim() : "";
		this.topicUnitRight = topicUnitRight != null ? topicUnitRight.trim()
				: "";
	}

	public String getTopicUnitLeft() {
		return topicUnitLeft;
	}

	public String getTopicUnitRight() {
		return topicUnitRight;
	}

	/**
	 * Since the DU may appear in the middle of a sentence, like: 'the life
	 * expectancy of an elephant is 60 years [60]' There may be at most two TUs,
	 * 'the life expectancy of an elephant is' and 'years'
	 * 
	 * @return all TUs of the statement
	 */
	public String getTopicUnit() {
		return (topicUnitLeft + " " + topicUnitRight).trim();
	}

	public int getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractStatement other = (AbstractStatement) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
