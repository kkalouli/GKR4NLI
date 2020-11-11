package gnli;

import java.io.Serializable;
import java.util.List;

import sem.graph.SemanticEdge;


/**
 * Represents a pair of role paths that have been matched with each other.
 * The first path is from a hypothesis term to its modifier.
 * The second path is from a (matched) text/premise term to its (matched) modifier.
 * A path is an ordered sequence of {@link SemanticEdge}s.
 * @author Katerina Kalouli, 2019
 */
public class HeadModifierPathPair implements Serializable {
	private static final long serialVersionUID = 7683317319590861558L;
	private SemanticEdge basePair;
	private SemanticEdge modifiersPair;
	private List<SemanticEdge> hypothesisPath;
	private List<SemanticEdge> premisePath;
	private float cost;
	
	/**
	 * The {@link MatchEdge} linking the premise and  terms
	 * @return
	 */
	public SemanticEdge getBasePair() {
		return basePair;
	}
	public void setBasePair(SemanticEdge basePair) {
		this.basePair = basePair;
	}
	
	
	/**
	 * The {@link MatchEdge} linking the premise and hypothesis modifiers
	 * @return
	 */
	public SemanticEdge getModifiersPair() {
		return modifiersPair;
	}
	public void setModifiersPair(SemanticEdge modifiersPair) {
		this.modifiersPair = modifiersPair;
	}
	
	/**
	 * The path from the hypothesis term to its (matched) modifier
	 * @return
	 */
	public List<SemanticEdge> getHypothesisPath() {
		return hypothesisPath;
	}
	public void setHypothesisPath(List<SemanticEdge> hPath) {
		this.hypothesisPath = hPath;
	}
	
	
	/**
	 * The path from the hypothesis term to its (matched) modifier
	 * @return
	 */
	public List<SemanticEdge> getPremisePath() {
		return premisePath;
	}
	public void setPremisePath(List<SemanticEdge> tPath) {
		this.premisePath = tPath;
	}
	
	/**
	 * The cost of the path, as computed by the {@link PathScorer}
	 * @return
	 */
	public float getCost() {
		return cost;
	}
	public void setCost(float cost) {
		this.cost = cost;
	}
	

}
