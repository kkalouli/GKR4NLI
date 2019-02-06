package gnli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import semantic.graph.EdgeContent;


/**
 * Implementation of {@link EdgeContent} to record Ecd match
 * information between two nodes.
 *
 */
public class MatchContent implements EdgeContent {
	private Specificity specificity;
	private Specificity originalSpecificity;
	private Map<String, Float> scores;
	private boolean finalized = false;
	private List<MatchOrigin> matchOrigin;
	private List<HeadModifierPathPair> justification;
	
	public MatchContent() {
		this.specificity = Specificity.NONE;
		this.originalSpecificity = Specificity.NONE;
		this.scores = new HashMap<String, Float>();
		this.scores.put("distance", 0f);
		this.finalized = false;
		this.matchOrigin = new ArrayList<MatchOrigin>();
		this.justification = new ArrayList<HeadModifierPathPair>();
	}
	
	/**
	 * Create match content for given matchType (stem, surface, sense, concept)
	 * @param matchType
	 */
	public MatchContent(MatchOrigin.MatchType matchType) {
		this();
		this.matchOrigin.add(new MatchOrigin(matchType));
		switch (matchType) {
		case STEM:
		case SURFACE:
		case CONCEPT:
		case SENSE:
		case SENSE_CMP:
		case EMBED:
			this.specificity = Specificity.EQUALS;
			this.originalSpecificity = Specificity.EQUALS;
			break;
		default:
			break;		
		}	
	}
	
	/**
	 * 
	 * @param matchType
	 *   (stem, surface, sense, concept)
	 * @param hSense
	 * 		Sense id on hypothesis/conclusion term
	 * @param tSense
	 * 	    Sense id on text/premise term
	 * @param concept
	 * 	    The concept or racId for which there is a match
	 * @param specificity
	 * 		The specificity of the match (sub, super, equal)
	 * @param score
	 * 		The penalty on the match
	 */
	public MatchContent(MatchOrigin.MatchType matchType, String hSense, String tSense, String concept, Specificity specificity, float score) {
		this(matchType);
		this.specificity = specificity;
		this.originalSpecificity = specificity;
		this.scores.put("distance", score);
		MatchOrigin mo = this.matchOrigin.get(0);
		mo.setHSense(hSense); 
		mo.setTSense(tSense);
		switch (matchType) {
		case CONCEPT:
			mo.setConceptOfMatchedSenses(concept);
			break;
		default:
			break;	
		}		
	}
	
	public MatchContent(MatchOrigin.MatchType matchType, Specificity specificity, float score) {
		this(matchType);
		this.specificity = specificity;
		this.originalSpecificity = specificity;
		this.scores.put("distance", score);
	}
	
	public MatchContent(MatchContent other) {
		this();
		this.specificity = other.specificity;
		this.originalSpecificity = other.originalSpecificity;
		this.scores = new HashMap<String, Float>(other.scores);
		this.finalized = other.finalized;
		this.matchOrigin = other.matchOrigin;
		this.justification = new ArrayList<HeadModifierPathPair>(other.justification);
	}
	
	/**
	 * The current specificity of the match
	 * @return
	 */
	public Specificity getSpecificity() {
		return specificity;
	}
	public void setSpecificity(Specificity specificity) {
		this.specificity = specificity;
	}
	
	/**
	 * The original specificity of the match, before any
	 * further restrictions were taken into account
	 * @return
	 */
	public Specificity getOriginalSpecificity() {
		return originalSpecificity;
	}
	
	/**
	 * The origin of the match (stem, sense, concept, etc)
	 * @return
	 */
	public MatchOrigin.MatchType getMatchType() {
		if (this.matchOrigin.size() > 0) {
			return this.matchOrigin.get(0).getMatchType();
		} else {
			return MatchOrigin.MatchType.NONE;
		}
	}
	
	/**
	 * The penalty/score on the match
	 * @return
	 */
	public float getScore() {
		float retval = 0f;
		for (Float score : this.scores.values()) {
			retval =+ score;
		}
		return retval;
	}
	
	public float getFeatureScore(String feature) {
		Float retval = this.scores.get(feature);
		return retval == null ? 0f : retval;
	}
	
	public Map<String, Float> getScoreComponents() {
		return this.scores;
	}
	
	public void addScore(String feature, float score) {
		Float currentScore = this.scores.get(feature);
		if (currentScore == null) {
			currentScore = (Float) 0f;
			this.scores.put(feature, currentScore);
		}
		currentScore =+ score;
	} 
	
	/**
	 * Have all restrictions on the term been considered in 
	 * determining the final specificity
	 * @return
	 */
	public boolean isFinalized() {
		return finalized;
	}
	public void setFinalized(boolean finalized) {
		this.finalized = finalized;
	}
	public List<MatchOrigin> getMatchOrigin() {
		return matchOrigin;
	}
	public void setMatchOrigin(List<MatchOrigin> matchOrigin) {
		this.matchOrigin = matchOrigin;
	}
	public void addMatchOrigin(MatchOrigin matchOrigin) {
		this.matchOrigin.add(matchOrigin);
	}
	
	/** 
	 * The possible path justifications for the match
	 * @return
	 */
	public List<HeadModifierPathPair> getJustification() {
		return justification;
	}
	public void setJustification(List<HeadModifierPathPair> justification) {
		this.justification = justification;
	}
	

}
