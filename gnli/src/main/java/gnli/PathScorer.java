package gnli;

import java.util.List;

import semantic.graph.SemanticEdge;
import semantic.graph.vetypes.SenseNode;
import semantic.graph.vetypes.SkolemNode;
import semantic.graph.vetypes.TermNode;



/**
 * Compute cost of equating text and hypothesis role paths.
 * Currently a stub.
 * 
 *
 */
public class PathScorer {
	private GNLIGraph gnliGraph;
	private float maxCost;
	
	public PathScorer(GNLIGraph gnliGraph, float maxCost) {
		super();
		this.gnliGraph = gnliGraph;
		this.maxCost = maxCost;
	}
	
	
	public PathScorer(float maxCost) {
		this.gnliGraph = null;
		this.maxCost = maxCost;
	}


	public float getMaxCost() {
		return maxCost;
	}


	public void setMaxCost(float maxCost) {
		this.maxCost = maxCost;
	}


	public GNLIGraph getGNLIGraph() {
		return gnliGraph;
	}

	public void setGNLIGraph(GNLIGraph gnliGraph) {
		this.gnliGraph = gnliGraph;
	}
	
	/**
	 * Cost is difference in the path lengths plus any further
	 * penalties on the premise path. The conclusion path will typically
	 * be length 1
	 * @param mcp
	 * @return
	 */
	public float pathCost(HeadModifierPathPair hMPath) {
		List<SemanticEdge> tPath = hMPath.getPremisePath();
		List<SemanticEdge> hPath = hMPath.getConclusionPath();
		int tLen = 0;
		int hLen = 0;
		if (tPath != null && !tPath.isEmpty())
			tLen = tPath.size();
		if (hPath != null && !hPath.isEmpty())
			hLen = hPath.size();
		
		float cost = 0 ;
		if (tLen > hLen)
			cost = tLen - hLen;
		else 
			cost = hLen-tLen;
		
		if (tPath != null && hPath != null) {
			cost += pathPenalty(hMPath, tPath, hPath);
		}
		return cost;
	}
	
	/**
	* 3 penalties:
	* kind of path (roles)
	* type of match: embed is not as good
	* score of match (embed is already penalized in the match score)
	* not the same sense across matches
	 */
	public float pathPenalty(HeadModifierPathPair hMPath, List<SemanticEdge> tPath, List<SemanticEdge> hPath) {
		float cost = 0;
		if (tPath.size() == hPath.size()){
			// if the match is based on opposing roles, it should be neglected
			if ( (tPath.get(0).getLabel().equals("sem_subj") && hPath.get(0).getLabel().equals("sem_obj")) ||
				 (tPath.get(0).getLabel().equals("sem_obj") && hPath.get(0).getLabel().equals("sem_subj")) )
				cost += 30;
			else if ( (tPath.get(0).getLabel().equals("amod") && hPath.get(0).getLabel().equals("rstr")) ||
					  (tPath.get(0).getLabel().equals("rstr") && hPath.get(0).getLabel().equals("amod"))   )
				cost -= 2;
			else if (tPath.equals(hPath))
				cost -= 2; 
		} else if (tPath.size() == 2){				
			if (hPath.get(0).getLabel().equals("amod") && tPath.get(0).getLabel().equals("nmod") && tPath.get(1).getLabel().equals("amod"))
				cost -= 2;
		}
		cost += ((MatchContent) hMPath.getModifiersPair().getContent()).getScore();
		
		// if the match is based on different senses, the match should be neglected
		if ( gnliGraph.getStartNode(hMPath.getModifiersPair()) instanceof SkolemNode){
			SkolemNode startNode = (SkolemNode) gnliGraph.getStartNode(hMPath.getModifiersPair());
			SkolemNode finishNode = (SkolemNode) gnliGraph.getFinishNode(hMPath.getModifiersPair());
			List<SenseNode> startSenses = gnliGraph.getHypothesisGraph().getSenses(startNode);
			List<SenseNode> finishSenses = gnliGraph.getTextGraph().getSenses(finishNode);
			boolean same = true;
			for (SenseNode sSense : startSenses){
				for (SenseNode fSense : finishSenses){
					if (!sSense.getContent().getConcepts().equals(fSense.getContent().getConcepts())){
						same = false;
					}
				}
			}
			if (same == false){
				cost += 30;
			}
			
		}		
		return cost;
	}
	

	public boolean pathBelowThreshold(float cost) {
		return cost < maxCost;
	}


}
