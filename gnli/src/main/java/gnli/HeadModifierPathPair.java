package gnli;

import java.io.Serializable;
import java.util.List;

import semantic.graph.SemanticEdge;


/**
 * Represents a pair of role chains / paths.
 * The first path is from a hypothesis/conclusion term to its modifier.
 * The second path is from a (matched) text/premise  term to its (matched) modifier.
 * A path is an ordered sequence of {@link SemanticEdge}s.
 *
 */
public class HeadModifierPathPair implements Serializable {
	private static final long serialVersionUID = 7683317319590861558L;
	private SemanticEdge basePair;
	private SemanticEdge modifiersPair;
	private List<SemanticEdge> conclusionPath;
	private List<SemanticEdge> premisePath;
	private float cost;
	
	/**
	 * The {@link MatchEdge} linking the premise and conclusion terms
	 * @return
	 */
	public SemanticEdge getBasePair() {
		return basePair;
	}
	public void setBasePair(SemanticEdge basePair) {
		this.basePair = basePair;
	}
	
	
	/**
	 * The {@link MatchEdge} linking the premise and conclusion modifiers
	 * @return
	 */
	public SemanticEdge getModifiersPair() {
		return modifiersPair;
	}
	public void setModifiersPair(SemanticEdge modifiersPair) {
		this.modifiersPair = modifiersPair;
	}
	
	/**
	 * The path from the conclusion term to its (matched) modifier
	 * @return
	 */
	public List<SemanticEdge> getConclusionPath() {
		return conclusionPath;
	}
	public void setConclusionPath(List<SemanticEdge> hPath) {
		this.conclusionPath = hPath;
	}
	
	
	/**
	 * The path from the conclusion term to its (matched) modifier
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
