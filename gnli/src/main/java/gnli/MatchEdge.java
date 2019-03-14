package gnli;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import semantic.graph.vetypes.LinkEdge;


/**
 * Records the match information on the link between a premise and a conclusion
 * node in an {@link EcdGraph}.
 * <p>
 * This includes
 * The original (term only) specificity of the match
 * The revised specificity of the match after considering modifiers
 * The justifications for the revised specificity: A collection of {@link HeadModifierPathPair}s
 * showing the paths by which the premise and conclusion terms connect to matched modifiers
 *
 */
public class MatchEdge extends LinkEdge implements Serializable {

	private static final long serialVersionUID = 6536396542192865602L;

	public MatchEdge(String label, MatchContent content) {
		super(label, content);
	}

	public MatchEdge(MatchEdge match) {
		super(match.label, new MatchContent((MatchContent) match.content));	
		this.sourceVertexId = match.sourceVertexId;
		this.destVertexId = match.destVertexId;
	}

	public void setComplete(boolean b) {
		((MatchContent) this.content).setFinalized(b);
		
	}

	/**
	 * Is the specificity revision on this match complete
	 * @return
	 */
	public boolean isComplete() {
		return ((MatchContent) this.content).isFinalized();
	}

	/**
	 * The specificity of the match
	 * @return
	 */
	public Specificity getSpecificity() {
		return ((MatchContent) this.content).getSpecificity();
	}
	
	/**
	 * The match specificity before any consideration of modifiers
	 * @return
	 */
	public Specificity getOriginalSpecificity() {
		return ((MatchContent) this.content).getOriginalSpecificity();
	}
	public void setSpecificity(Specificity specificity) {
		((MatchContent) this.content).setSpecificity(specificity);		
	}
	
	/**
	 * How the premise and conclusion terms connect to the corresponding modifiers
	 * @return
	 */
	public List<HeadModifierPathPair> getJustification() {
		return ((MatchContent) this.content).getJustification();
	}

	public void addJustification(HeadModifierPathPair justification) {
		((MatchContent) this.content).getJustification().add(justification);
	}

	/** 
	 * The cost of the match
	 * @return
	 */
	public double getScore() {
		return ((MatchContent) this.content).getScore();
	}
	
	public double getFeatureScore(String feature) {
		return ((MatchContent) this.content).getFeatureScore(feature);
	}
	
	public Map<String, Double> getScoreComponents() {
		return ((MatchContent) this.content).getScoreComponents();
	}


	public void incrementScore(String feature, float cost) {
		((MatchContent) this.content).addScore(feature, cost);
		
	}
	
	@Override
	public String toString() {
		return ((MatchContent) this.content).getSpecificity() + "(" + label +")";
	}



}
