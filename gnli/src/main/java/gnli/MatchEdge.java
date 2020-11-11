package gnli;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sem.graph.vetypes.LinkEdge;


/**
 * Records the match information between a premise and a hypothesis node.
 * This includes:
 * 	- the original specificity of the match
 * 	- the updated specificity of the match after considering modifiers
 * 	- the justifications for the updated specificity: A collection of HeadModifierPathPairs
 * showing the paths by which the premise and hypothesis terms connect to matched modifiers
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

	/**
	 * Set that an edge has been completely updated and is finalized.
	 * @param b
	 */
	public void setComplete(boolean b) {
		((MatchContent) this.content).setFinalized(b);
		
	}

	/**
	 * Check whether a match update is complete.
	 * @return
	 */
	public boolean isComplete() {
		return ((MatchContent) this.content).isFinalized();
	}

	/**
	 * Get the current specificity of the match.
	 * @return
	 */
	public Specificity getSpecificity() {
		return ((MatchContent) this.content).getSpecificity();
	}
	
	public void setSpecificity(Specificity specificity) {
		((MatchContent) this.content).setSpecificity(specificity);		
	}
	
	/**
	 *  Get the original specificity of the match before any updates were done.
	 * @return
	 */
	public Specificity getOriginalSpecificity() {
		return ((MatchContent) this.content).getOriginalSpecificity();
	}
	
	/**
	 * Get how the premise and hypothesis terms connect to their corresponding modifiers.
	 * @return
	 */
	public List<HeadModifierPathPair> getJustification() {
		return ((MatchContent) this.content).getJustification();
	}

	/**
	 * Add how the premise and hypothesis terms connect to their corresponding modifiers.
	 * @return
	 */
	public void addJustification(HeadModifierPathPair justification) {
		((MatchContent) this.content).getJustification().add(justification);
	}

	/** 
	 * Get the entire cost of the match.
	 * @return
	 */
	public double getScore() {
		return ((MatchContent) this.content).getScore();
	}
	
	/** 
	 * Get the cost of the match correspnding to a specific feature, e.g. depth, distance.
	 * @return
	 */
	public double getFeatureScore(String feature) {
		return ((MatchContent) this.content).getFeatureScore(feature);
	}
	
	/**
	 * Get the different components that make up the score.
	 * @return
	 */
	public HashMap<String, Double> getScoreComponents() {
		return ((MatchContent) this.content).getScoreComponents();
	}

	/**
	 * Increment the score corresponding to a specific feature.
	 * @param feature
	 * @param cost
	 */
	public void incrementScore(String feature, float cost) {
		((MatchContent) this.content).addScore(feature, cost);
		
	}
	
	public void incrementScore(HashMap<String, Double> map) {
		for (String key :map.keySet()){
			((MatchContent) this.content).addScore(key, map.get(key));
		}		
	}
	
	/**
	 * Add a cost to the costList, i.e. add a "flag" for whether the match is mostly
	 * associated with an entailment/contradiciton/neutral.
	 * @param cost
	 */
	public void addCost(float cost) {
		((MatchContent) this.content).getCostList().add(cost);
		
	}
	
	public ArrayList<Float> getCostList() {
		return	((MatchContent) this.content).getCostList();		
	}
	
	@Override
	public String toString() {
		return ((MatchContent) this.content).getSpecificity() + "(" + label +")";
	}
	


}
