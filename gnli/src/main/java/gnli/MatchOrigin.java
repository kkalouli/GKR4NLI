package gnli;

import java.io.Serializable;

/**
 * Get the origin of the match between a premise and a hypothesis. 
 * @author Katerina Kalouli, 2019
 *
 */
public class MatchOrigin implements Serializable { 
	private static final long serialVersionUID = 5464834181048581205L;
	public static enum MatchType {LEMMA, SURFACE, SENSE, EMBED, CONCEPT, SENSE_CMP}
	private MatchType matchType;
	private String hSense = null;
	private String tSense = null;
	private String concept = null;
	
	
	public MatchOrigin(MatchType matchType) {
		this.matchType = matchType;
	}

	public MatchOrigin(MatchType matchType, String hSense, String tSense) {
		this.matchType = matchType;
		this.hSense = hSense;
		this.tSense = tSense;
	}
	
	public MatchType getMatchType() {
		return matchType;
	}
	protected void setMatchType(MatchType matchType) {
		this.matchType = matchType;
	}
	public String getHSense() {
		return hSense;
	}
	protected void setHSense(String hSense) {
		this.hSense = hSense;
	}
	public String getTSense() {
		return tSense;
	}
	protected void setTSense(String tSense) {
		this.tSense = tSense;
	}
	
	public String getConceptOfMatchedSenses() {
		return concept;
	}
	protected void setConceptOfMatchedSenses(String concept) {
		this.concept = concept;
	}

}
