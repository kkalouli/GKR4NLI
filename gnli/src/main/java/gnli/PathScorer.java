package gnli;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import sem.graph.SemanticEdge;
import sem.graph.vetypes.SenseNode;
import sem.graph.vetypes.SkolemNode;



/**
 * Compute cost of equating text and hypothesis role paths.
 * Currently a stub.
 * 
 *
 */
public class PathScorer implements Serializable {
	private static final long serialVersionUID = 7142446450358190985L;
	private GNLIGraph gnliGraph;
	private float maxCost;
	private boolean learning;
	private InferenceComputer infComputer;
	
	
	public PathScorer(GNLIGraph gnliGraph, float maxCost, boolean learning, InferenceComputer infComputer) {
		super();
		this.gnliGraph = gnliGraph;
		this.maxCost = maxCost;
		this.infComputer = infComputer;
		this.learning = learning;
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
	

	public void addEntailRolePath(HeadModifierPathPair pathToAdd){
		if (pathsAreIdentical(pathToAdd) == true)
			return;
		if (pathToAdd.getConclusionPath() != null && pathToAdd.getPremisePath() != null){
			String key = pathToAdd.getConclusionPath().toString()+"/"+pathToAdd.getPremisePath().toString();		
			if (!infComputer.getEntailRolePaths().containsKey(key)){
				infComputer.getEntailRolePaths().put(key, new ArrayList<HeadModifierPathPair>());
			}
			this.infComputer.getEntailRolePaths().get(key).add(pathToAdd);
			// if a path is added to the entailPaths, it should be removed from the neutralPaths in case it is there. 
			if (infComputer.getNeutralRolePaths().keySet().contains(key))
				removeNeutralRolePath(key); 
		}
	}
	
	
	public void removeEntailRolePath(String key){
		if (infComputer.getEntailRolePaths().keySet().contains(key)){
			infComputer.getEntailRolePaths().remove(key);
		}
	}
	
	private boolean pathsAreIdentical(HeadModifierPathPair path){
		if (path.getConclusionPath() != null && path.getPremisePath() != null){
			String hypPath = path.getConclusionPath().toString();
			String txtPath = path.getPremisePath().toString();
			if (hypPath.equals(txtPath))
				return true;
			
		}
		return false;
	}
	
	
	public void addNeutralRolePath(HeadModifierPathPair pathToAdd){
		if (pathsAreIdentical(pathToAdd) == true)
			return;
		if (pathToAdd.getConclusionPath() != null && pathToAdd.getPremisePath() != null){
			String key = pathToAdd.getConclusionPath().toString()+"/"+pathToAdd.getPremisePath().toString();
			// only add it to the neutral paths if it is not already contained in the entailPaths (entailPaths are more dominant) 
			if (infComputer.getEntailRolePaths().containsKey(key))
				return;
			if (!infComputer.getNeutralRolePaths().containsKey(key)){
				infComputer.getNeutralRolePaths().put(key, new ArrayList<HeadModifierPathPair>());
			}
			infComputer.getNeutralRolePaths().get(key).add(pathToAdd);
		}
	}
	
	public void removeNeutralRolePath(String key){
		if (infComputer.getNeutralRolePaths().keySet().contains(key)){
			infComputer.getNeutralRolePaths().remove(key);
		}
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
		if (pathsAreIdentical(hMPath) == false && learning == false){
			String key = hPath.toString()+"/"+tPath.toString();
			if (infComputer.getNeutralRolePaths().containsKey(key))
				cost += maxCost;
			else if (infComputer.getEntailRolePaths().containsKey(key))
				cost -= 10;
		}
		if (tPath.size() == hPath.size() && hPath.size() == 1){
			// if the match is based on opposing roles, it should be neglected
			if ( (tPath.get(0).getLabel().equals("sem_subj") && hPath.get(0).getLabel().equals("sem_obj")) ||
				 (tPath.get(0).getLabel().equals("sem_obj") && hPath.get(0).getLabel().equals("sem_subj")) )
				cost += maxCost;
			else if ( (tPath.get(0).getLabel().equals("amod") && hPath.get(0).getLabel().equals("rstr")) ||
					  (tPath.get(0).getLabel().equals("rstr") && hPath.get(0).getLabel().equals("amod"))   )
				cost -= 2;
			else if (tPath.equals(hPath))
				cost -= 2; 
		} else if (tPath.size() == 2 && hPath.size() == 1){				
			if (hPath.get(0).getLabel().equals("amod") && tPath.get(0).getLabel().equals("nmod") && tPath.get(1).getLabel().equals("amod"))
				cost -= 2;
		} else if (hPath.size() == 2 && tPath.size() == 1){	
			if (tPath.get(0).getLabel().equals("amod") && hPath.get(0).getLabel().equals("nmod") && hPath.get(1).getLabel().equals("amod"))
				cost -= 2;
		}
		//cost += ((MatchContent) hMPath.getModifiersPair().getContent()).getScore();
		
		// if the match is based on different senses of the same word, the match should be neglected
		if ( gnliGraph.getStartNode(hMPath.getModifiersPair()) instanceof SkolemNode && !hMPath.getModifiersPair().getLabel().equals("sense")
				&& !hMPath.getModifiersPair().getLabel().equals("concept") && !hMPath.getModifiersPair().getLabel().equals("embed")){
			SkolemNode startNode = (SkolemNode) gnliGraph.getStartNode(hMPath.getModifiersPair());
			SkolemNode finishNode = (SkolemNode) gnliGraph.getFinishNode(hMPath.getModifiersPair());
			List<SenseNode> startSenses = gnliGraph.getHypothesisGraph().getSenses(startNode);
			List<SenseNode> finishSenses = gnliGraph.getTextGraph().getSenses(finishNode);
			boolean same = false;
			for (SenseNode sSense : startSenses){
				for (SenseNode fSense : finishSenses){
					if (fSense.getContent().getConcepts().equals(sSense.getContent().getConcepts())){
						same = true;
					}
				}
			}
			if (same == false && !startSenses.isEmpty() && !finishSenses.isEmpty()){
				cost += maxCost;
			}
			
		}		
		return cost;
	}
	

	public boolean pathBelowThreshold(float cost) {
		return cost < maxCost;
	}


}
