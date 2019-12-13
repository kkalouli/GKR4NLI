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
	private float neuCost;
	private float contraCost;
	private boolean learning;
	private InferenceComputer infComputer;
	
	
	
	
	public PathScorer(GNLIGraph gnliGraph, float neuCost, float contraCost, boolean learning, InferenceComputer infComputer) {
		super();
		this.gnliGraph = gnliGraph;
		this.neuCost = neuCost;
		this.contraCost = contraCost;
		this.infComputer = infComputer;
		this.learning = learning;
	}
	
	
	
	public PathScorer(float neuCost, float contraCost) {
		this.gnliGraph = null;
		this.neuCost = neuCost;
		this.contraCost = contraCost;
	}
	

	public float getNeuCost() {
		return neuCost;
	}


	public void setNeuCost(float neuCost) {
		this.neuCost = neuCost;
	}
	
	public float getContraCost() {
		return contraCost;
	}


	public void setContraCost(float contraCost) {
		this.contraCost = contraCost;
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
			// 1st learning: only add it to the entail paths if it is not already contained in the contraPaths (contraPaths are more dominant) 
			/*if (infComputer.getContraRolePaths().containsKey(key) )
				return;*/
			// 2nd learning: add it anyway
			if (!infComputer.getEntailRolePaths().containsKey(key)){
				infComputer.getEntailRolePaths().put(key, new ArrayList<HeadModifierPathPair>());
			}
			this.infComputer.getEntailRolePaths().get(key).add(pathToAdd);
			// 1st learning: if a path is added to the entailPaths, it should be removed from the neutralPaths in case it is there. 
			/*if (infComputer.getNeutralRolePaths().keySet().contains(key))
				removeNeutralRolePath(key);*/ 
		}
	}
	
	
	public void removeEntailRolePath(String key){
		if (infComputer.getEntailRolePaths().keySet().contains(key)){
			infComputer.getEntailRolePaths().remove(key);
		}
	}
	
	public void addContraRolePath(HeadModifierPathPair pathToAdd){
		if (pathsAreIdentical(pathToAdd) == true)
			return;
		if (pathToAdd.getConclusionPath() != null && pathToAdd.getPremisePath() != null){
			String key = pathToAdd.getConclusionPath().toString()+"/"+pathToAdd.getPremisePath().toString();		
			if (!infComputer.getContraRolePaths().containsKey(key)){
				infComputer.getContraRolePaths().put(key, new ArrayList<HeadModifierPathPair>());
			}
			this.infComputer.getContraRolePaths().get(key).add(pathToAdd);
			// 1st learning: if a path is added to the contraPaths, it should be removed from the neutralPaths and the entailPaths in case it is there. 
			/*if (infComputer.getNeutralRolePaths().keySet().contains(key))
				removeNeutralRolePath(key); 
			if (infComputer.getEntailRolePaths().keySet().contains(key))
				removeEntailRolePath(key); */
		}
	}
	
	
	public void removeContraRolePath(String key){
		if (infComputer.getContraRolePaths().keySet().contains(key)){
			infComputer.getContraRolePaths().remove(key);
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
			// 1st learning: only add it to the neutral paths if it is not already contained in the entailPaths (entailPaths are more dominant) 
			//if (infComputer.getEntailRolePaths().containsKey(key) || infComputer.getContraRolePaths().containsKey(key) )
			//	return;
			// 2nd learning: add it anyway
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
		float cost = 0 ;
		/*if (tPath != null && !tPath.isEmpty())
			tLen = tPath.size();
		if (hPath != null && !hPath.isEmpty())
			hLen = hPath.size();
		
		
		if (tLen > hLen)
			cost = tLen - hLen;
		else 
			cost = hLen-tLen;
		*/
		if (tPath != null && hPath != null) {
			cost += pathPenalty(hMPath, tPath, hPath);
		}
		return cost;
	}
	
	/**
	* 3 penalties:
	* kind of path (roles)
	* score of match (embed is already penalized in the match score)
	* not the same sense across matches
	 */
	public float pathPenalty(HeadModifierPathPair hMPath, List<SemanticEdge> tPath, List<SemanticEdge> hPath) {
		float cost = 0;	
		/*
		if (pathsAreIdentical(hMPath) == false && learning == false){
			String key = hPath.toString()+"/"+tPath.toString();
			// following costs after 1st learning
			/*if (infComputer.getNeutralRolePaths().containsKey(key))
				if (infComputer.getNeutralRolePaths().get(key).size() == 1)
					cost += maxCost/2;
				else 
					cost += maxCost;
			else if (infComputer.getContraRolePaths().containsKey(key))
				cost -= 10;	
			else if (infComputer.getEntailRolePaths().containsKey(key))
				cost -= 10;		
				*/
			// following costs after 2nd learning
			// path exists in only one of the lists
		/*
			if (infComputer.getNeutralRolePaths().containsKey(key) && 
					!infComputer.getEntailRolePaths().containsKey(key) &&
					!infComputer.getContraRolePaths().containsKey(key)){
				if (infComputer.getNeutralRolePaths().get(key).size() > 1)
					cost += neuCost;
				else
					cost += neuCost*0.75;
			}
			else if (!infComputer.getNeutralRolePaths().containsKey(key) && 
					!infComputer.getEntailRolePaths().containsKey(key) &&
					infComputer.getContraRolePaths().containsKey(key)){
				if (infComputer.getContraRolePaths().get(key).size() > 1)
					cost += contraCost;
				else
					cost += contraCost;
			}
			else if (!infComputer.getNeutralRolePaths().containsKey(key) && 
					infComputer.getEntailRolePaths().containsKey(key) &&
					!infComputer.getContraRolePaths().containsKey(key)){
				if (infComputer.getEntailRolePaths().get(key).size() > 1)
					cost += 0;
				else
					cost += neuCost*0.25;
			}
			// path exists in two of the lists
			if (infComputer.getNeutralRolePaths().containsKey(key) && 
					infComputer.getEntailRolePaths().containsKey(key) &&
					!infComputer.getContraRolePaths().containsKey(key)){
				if (infComputer.getNeutralRolePaths().get(key).size() == infComputer.getEntailRolePaths().get(key).size())
					cost += neuCost*0.5;
				else if (infComputer.getNeutralRolePaths().get(key).size() > infComputer.getEntailRolePaths().get(key).size()){
					float prob = (float) infComputer.getNeutralRolePaths().get(key).size()/(infComputer.getNeutralRolePaths().get(key).size() + infComputer.getEntailRolePaths().get(key).size()); 
					cost += neuCost*prob;
				}
				else if (infComputer.getNeutralRolePaths().get(key).size() < infComputer.getEntailRolePaths().get(key).size()){
					float prob = (float) infComputer.getNeutralRolePaths().get(key).size() / (infComputer.getNeutralRolePaths().get(key).size() + infComputer.getEntailRolePaths().get(key).size()); 
					cost += neuCost*prob;
				}
			} 
			else if (!infComputer.getNeutralRolePaths().containsKey(key) && 
					infComputer.getEntailRolePaths().containsKey(key) &&
					infComputer.getContraRolePaths().containsKey(key)){
				if (infComputer.getContraRolePaths().get(key).size() == infComputer.getEntailRolePaths().get(key).size())
					cost += contraCost+contraCost*0.25;
				else if (infComputer.getContraRolePaths().get(key).size() > infComputer.getEntailRolePaths().get(key).size()){
					float prob = (float) infComputer.getEntailRolePaths().get(key).size() / (infComputer.getContraRolePaths().get(key).size() + infComputer.getEntailRolePaths().get(key).size()); 
					cost += contraCost+prob*100;
					//cost += contraCost+contraCost/8;
				}			
				else if (infComputer.getContraRolePaths().get(key).size() < infComputer.getEntailRolePaths().get(key).size()){
					float prob = (float) infComputer.getContraRolePaths().get(key).size() / (infComputer.getContraRolePaths().get(key).size() + infComputer.getEntailRolePaths().get(key).size()); 
					cost += contraCost+prob*100;
					//cost += contraCost+contraCost*0.25+contraCost/8;
				}					
			} 
			else if (infComputer.getNeutralRolePaths().containsKey(key) && 
					!infComputer.getEntailRolePaths().containsKey(key) &&
					infComputer.getContraRolePaths().containsKey(key)){
				if (infComputer.getContraRolePaths().get(key).size() == infComputer.getNeutralRolePaths().get(key).size())
					cost += contraCost- contraCost*0.25;
				else if (infComputer.getContraRolePaths().get(key).size() > infComputer.getNeutralRolePaths().get(key).size()){
					float prob = (float) infComputer.getNeutralRolePaths().get(key).size() / (infComputer.getContraRolePaths().get(key).size() + infComputer.getNeutralRolePaths().get(key).size()); 
					cost += contraCost-prob*100;
					//cost += contraCost*0.75;
				}		
				else if (infComputer.getContraRolePaths().get(key).size() < infComputer.getNeutralRolePaths().get(key).size()){
					float prob = (float) infComputer.getContraRolePaths().get(key).size() / (infComputer.getContraRolePaths().get(key).size() + infComputer.getNeutralRolePaths().get(key).size()); 
					cost += contraCost-prob*100;
					//cost += contraCost*0.25;
				}
					
			} 
			//path exists in all 3 lists
			if (infComputer.getNeutralRolePaths().containsKey(key) && 
					infComputer.getEntailRolePaths().containsKey(key) &&
					infComputer.getContraRolePaths().containsKey(key)){
				if (infComputer.getNeutralRolePaths().get(key).size() == infComputer.getEntailRolePaths().get(key).size() && infComputer.getEntailRolePaths().get(key).size()  == infComputer.getContraRolePaths().get(key).size() )
					cost += 0;
				float probEntail = (float) infComputer.getEntailRolePaths().get(key).size() / (infComputer.getEntailRolePaths().get(key).size()+ infComputer.getContraRolePaths().get(key).size() + infComputer.getNeutralRolePaths().get(key).size()) ;
				float probContra = (float) infComputer.getContraRolePaths().get(key).size() / (infComputer.getEntailRolePaths().get(key).size()+ infComputer.getContraRolePaths().get(key).size() + infComputer.getNeutralRolePaths().get(key).size()) ;
				float probNeutral = (float) infComputer.getNeutralRolePaths().get(key).size() / (infComputer.getEntailRolePaths().get(key).size()+ infComputer.getContraRolePaths().get(key).size() + infComputer.getNeutralRolePaths().get(key).size()) ;

				if (probEntail > probContra && probEntail > probNeutral){
					if (probNeutral > probContra)
						cost += neuCost-probEntail*100;
					else if (probNeutral < probContra)
						cost += contraCost+probEntail*100;
					else
						cost += 0;
				}
				
				if (probContra > probEntail && probContra > probNeutral){
					if (probEntail > probNeutral)
						cost += contraCost-probEntail*100;
					else if (probEntail < probNeutral)
						cost += neuCost+probContra*100;
					else
						cost += 0;
				}
				
				if (probNeutral > probEntail && probNeutral > probContra){
					if (probEntail > probContra)
						cost += neuCost-probEntail*100;
					else if (probEntail < probContra)
						cost += contraCost-probNeutral*100;
				}
				
				/*if (infComputer.getNeutralRolePaths().get(key).size() > infComputer.getEntailRolePaths().get(key).size() && infComputer.getEntailRolePaths().get(key).size()  > infComputer.getContraRolePaths().get(key).size())
						cost += contraCost*0.75;			
				else if (infComputer.getNeutralRolePaths().get(key).size() > infComputer.getContraRolePaths().get(key).size() && infComputer.getContraRolePaths().get(key).size()  > infComputer.getEntailRolePaths().get(key).size() )
					cost += contraCost*0.25;
				else if (infComputer.getEntailRolePaths().get(key).size() > infComputer.getNeutralRolePaths().get(key).size() && infComputer.getNeutralRolePaths().get(key).size()  > infComputer.getContraRolePaths().get(key).size() )
					cost += neuCost*0.25;
				else if (infComputer.getEntailRolePaths().get(key).size() > infComputer.getContraRolePaths().get(key).size() && infComputer.getContraRolePaths().get(key).size()  > infComputer.getNeutralRolePaths().get(key).size() )
					cost += contraCost-contraCost*0.75;
				else if (infComputer.getContraRolePaths().get(key).size() > infComputer.getNeutralRolePaths().get(key).size() && infComputer.getNeutralRolePaths().get(key).size()  > infComputer.getContraRolePaths().get(key).size() )
					cost += contraCost*0.75;
				else if (infComputer.getContraRolePaths().get(key).size() > infComputer.getEntailRolePaths().get(key).size() && infComputer.getEntailRolePaths().get(key).size()  > infComputer.getNeutralRolePaths().get(key).size() )
					cost += contraCost-contraCost*0.25;
				if (infComputer.getNeutralRolePaths().get(key).size() == infComputer.getEntailRolePaths().get(key).size() && infComputer.getEntailRolePaths().get(key).size()  == infComputer.getContraRolePaths().get(key).size() )
					cost += 0;*/
				
			//} 
				
		//}
		if (tPath.size() == hPath.size() && hPath.size() == 1){
			// if the match is based on opposing roles, it should be neglected
			if ( (tPath.get(0).getLabel().equals("sem_subj") && hPath.get(0).getLabel().equals("sem_obj")) ||
				 (tPath.get(0).getLabel().equals("sem_obj") && hPath.get(0).getLabel().equals("sem_subj")) )
				cost += contraCost;
			else if ( (tPath.get(0).getLabel().equals("sem_subj") && hPath.get(0).getLabel().equals("nmod_comp")) ||
				 (tPath.get(0).getLabel().equals("nmod_comp") && hPath.get(0).getLabel().equals("sem_subj")) )
				cost += contraCost;
			else if ( (tPath.get(0).getLabel().equals("amod") && hPath.get(0).getLabel().equals("rstr")) ||
					  (tPath.get(0).getLabel().equals("rstr") && hPath.get(0).getLabel().equals("amod"))   )
				cost -= 10;
			else if (tPath.equals(hPath))
				cost -= 10; 
		} else if (tPath.size() == 2 && hPath.size() == 1){				
			if (hPath.get(0).getLabel().equals("amod") && tPath.get(0).getLabel().equals("nmod") && tPath.get(1).getLabel().equals("amod"))
				cost -= 10;
		} else if (hPath.size() == 2 && tPath.size() == 1){	
			if (tPath.get(0).getLabel().equals("amod") && hPath.get(0).getLabel().equals("nmod") && hPath.get(1).getLabel().equals("amod"))
				cost -= 10;
		}
		if (tPath.size() == 2 && hPath.size() == 1){
			if ( tPath.get(0).getLabel().equals("sem_subj") && tPath.get(1).getLabel().equals("is_element") && hPath.get(0).getLabel().equals("sem_obj"))
				cost += contraCost;
			if ( tPath.get(0).getLabel().equals("sem_obj") && tPath.get(1).getLabel().equals("is_element") && hPath.get(0).getLabel().equals("sem_subj"))
				cost += contraCost;
			if ( tPath.get(0).getLabel().equals("sem_comp") && tPath.get(1).getLabel().equals("sem_subj") && hPath.get(0).getLabel().equals("nmod_comp"))
				cost += contraCost;
			if ( tPath.get(0).getLabel().equals("sem_comp") && tPath.get(1).getLabel().equals("nmod_comp") && hPath.get(0).getLabel().equals("sem_subj"))
				cost += contraCost;
				// added on 19.11 for embedded ctxs: he knew the authors, he knew that the authors talked.
			if (tPath.get(0).getLabel().equals("sem_comp") && tPath.get(1).getLabel().equals("sem_subj") && hPath.get(0).getLabel().equals("sem_obj"))
				cost += contraCost;
		}
		if (tPath.size() == 1 && hPath.size() == 2){
			if ( hPath.get(0).getLabel().equals("sem_subj") && hPath.get(1).getLabel().equals("is_element") && tPath.get(0).getLabel().equals("sem_obj"))
				cost += contraCost;
			if ( hPath.get(0).getLabel().equals("sem_obj") && hPath.get(1).getLabel().equals("is_element") && tPath.get(0).getLabel().equals("sem_subj"))
				cost += contraCost;
			if ( hPath.get(0).getLabel().equals("sem_comp") && hPath.get(1).getLabel().equals("sem_subj") && tPath.get(0).getLabel().equals("nmod_comp"))
				cost += contraCost;
			if ( hPath.get(0).getLabel().equals("sem_comp") && hPath.get(1).getLabel().equals("nmod_comp") && tPath.get(0).getLabel().equals("sem_subj"))
				cost += contraCost;
			// added on 19.11
			if ( hPath.get(0).getLabel().equals("sem_comp") && hPath.get(1).getLabel().equals("sem_subj") && tPath.get(0).getLabel().equals("sem_obj"))
				cost += contraCost;
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
				boolean hasSense = true;
				for (SenseNode sense : startSenses){
					if (sense.getLabel().equals("00000000"))
						hasSense = false;
					break;
				}
				for (SenseNode sense : finishSenses){
					if (sense.getLabel().equals("00000000"))
						hasSense = false;
					break;
				}
				if (hasSense == true)
					cost += neuCost;
			}
			
			
		}	
		return cost;
	}
	

	public boolean pathAtNeutralThreshold(float cost) {
		// always return false for now ==> do not remove anz matches for now
		//return cost == neuCost;
		return false;
	}
	


}
