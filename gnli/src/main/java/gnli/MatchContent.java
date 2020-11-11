package gnli;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sem.graph.EdgeContent;


/**
 * Implementation of {@link EdgeContent} to record match information between two nodes.
 *
 */
public class MatchContent implements EdgeContent, Serializable, Comparable {
	private static final long serialVersionUID = -3687998411657779763L;
	private Specificity specificity;
	private Specificity originalSpecificity;
	private HashMap<String, Double> scores;
	private boolean finalized = false;
	private List<MatchOrigin> matchOrigin;
	private List<HeadModifierPathPair> justification;
	private ArrayList<Float> costList;

	
	public MatchContent() {
		this.specificity = Specificity.NONE;
		this.originalSpecificity = Specificity.NONE;
		this.scores = new HashMap<String, Double>();
		this.scores.put("distance", 0.0);
		this.finalized = false;
		this.matchOrigin = new ArrayList<MatchOrigin>();
		this.justification = new ArrayList<HeadModifierPathPair>();
		this.costList = new ArrayList<Float>();
	}
	
	/**
	 * Create match content for given matchType (stem, surface, sense, concept, embed)
	 * @param matchType
	 */
	public MatchContent(MatchOrigin.MatchType matchType) {
		this();
		this.matchOrigin.add(new MatchOrigin(matchType));
		switch (matchType) {
		case LEMMA:
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
	 * Create match content for given:
	 * @param matchType
	 *   (stem, surface, sense, concept)
	 * @param hSense
	 * 		Sense id on hypothesis/conclusion term
	 * @param tSense
	 * 	    Sense id on text/premise term
	 * @param concept
	 * 	    The concept for which there is a match
	 * @param specificity
	 * 		The specificity of the match (sub, super, equal)
	 * @param score
	 * 		The penalty on the match
	 */
	public MatchContent(MatchOrigin.MatchType matchType, String hSense, String tSense, String concept, Specificity specificity, double positionScore, double depth) {
		this(matchType);
		this.specificity = specificity;
		this.originalSpecificity = specificity;
		this.scores.put("distance", positionScore);
		this.scores.put("depth", depth);
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
	
	/**
	 * Create match content for given:
	 * @param matchType
	 *   (stem, surface, sense, concept)
	 @param specificity
	 * 	The specificity of the match (sub, super, equal)
	 * @param positionScore
	 * 		the distance of the match in terms of JIGSAW disambiguation
	 * @param depth
	 * 		the sense/concept depth of the match
	 */
	public MatchContent(MatchOrigin.MatchType matchType, Specificity specificity,  double positionScore, double depth) {
		this(matchType);
		this.specificity = specificity;
		this.originalSpecificity = specificity;
		this.scores.put("distance", positionScore);
		this.scores.put("depth", depth);

	}
	
	/**
	 * Create match content for blank arguments. 
	 * @param other
	 */
	public MatchContent(MatchContent other) {
		this();
		this.specificity = other.specificity;
		this.originalSpecificity = other.originalSpecificity;
		this.scores = new HashMap<String, Double>(other.scores);
		this.finalized = other.finalized;
		this.matchOrigin = other.matchOrigin;
		this.justification = new ArrayList<HeadModifierPathPair>(other.justification);
	}
	
	/**
	 * The current costs of a match in respect to whether it is mostly associated
	 * with an entailment/contradiction/neutral (approximation of the flags technique
	 * analyzed in the paper)
	 * @return
	 */
	public ArrayList<Float> getCostList(){
		return costList;
	}
	
	public void setCostList(ArrayList<Float> costList){
		this.costList = costList;
	}
	
	/**
	 * The current specificity of the match.
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
	 * The whole penalty/score of the match.
	 * @return
	 */
	public double getScore() {
		double retval = 0.0;
		for (Double score : this.scores.values()) {
			retval += score;
		}
		return retval;
	}
	
	/**
	 * The penalty/score of the match concerning a certain feature (e.g. depth, distance, etc.)
	 * @param feature
	 * @return
	 */
	public double getFeatureScore(String feature) {
		Double retval = this.scores.get(feature);
		return retval == null ? 0.0 : retval;
	}
	
	/** 
	 * Get the available score features.
	 * @return
	 */
	public HashMap<String, Double> getScoreComponents() {
		return this.scores;
	}
	
	/**
	 * Add a score to the general score of the match and record from which feature this
	 * score originates.
	 * @param feature
	 * @param score
	 */
	public void addScore(String feature, double score) {
		Double currentScore = this.scores.get(feature);
		if (currentScore == null) {
			this.scores.put(feature, score);
		} else {
			this.scores.put(feature, currentScore+ score);
		}
	}

	
	/**
	 * Checks whether the specificity of a match has been finalized,
	 * after considering all the restrictions of the match.
	 * @return
	 */
	public boolean isFinalized() {
		return finalized;
	}
	public void setFinalized(boolean finalized) {
		this.finalized = finalized;
	}
	
	/**
	 * Get the origin of the match.
	 * @return
	 */
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
	
	@Override
    public int compareTo(Object o) {
        MatchContent other = (MatchContent) o;
        return (int) (this.getScore() - other.getScore());
    }
	

}
