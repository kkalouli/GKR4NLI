package gnli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import sem.graph.SemanticEdge;
import sem.graph.SemanticGraph;
import sem.graph.SemanticNode;
import sem.graph.vetypes.SkolemNode;
import sem.graph.vetypes.SkolemNodeContent;



public class InferenceChecker {

	public enum EntailmentRelation {ENTAILMENT, CONTRADICTION, NEUTRAL, UNKNOWN}
	private GNLIGraph gnliGraph;
	private EntailmentRelation entailmentRelation;
	private HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> hypothesisContexts;
	private HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> textContexts;
	private SemanticNode<?> hypRootCtx;
	private SemanticNode<?> textRootCtx;
	private double matchStrength;
	private ArrayList<MatchEdge> justifications;
	private boolean looseContra;
	private boolean looseEntail;
	
	enum Polarity {VERIDICAL, ANTIVERIDICAL, AVERIDICAL}

	
	public InferenceChecker(GNLIGraph gnliGraph) {
		this.gnliGraph = gnliGraph;
		this.entailmentRelation = EntailmentRelation.UNKNOWN; 
		looseContra = false;
		looseEntail = false;
		matchStrength = 0;
		justifications = new ArrayList<MatchEdge>();
	}
	
	
	public InferenceDecision getInferenceDecision() {	
		if (this.hypothesisContexts == null) {
			initialize();
		}
		if (this.entailmentRelation == EntailmentRelation.UNKNOWN) {
			computeInferenceRelation();
		}
		return new InferenceDecision(entailmentRelation,matchStrength, justifications, looseContra, looseEntail, gnliGraph);
	}
	
	
	private void computeInferenceRelation() {
		HashMap<SemanticNode<?>,MatchEdge> rootNodeMatches = new HashMap<SemanticNode<?>,MatchEdge>();
		gnliGraph.getMatchGraph().display();
		// find the root node of the role graph; there might be more than one root nodes or a double root node
		for (MatchEdge matchEdge : gnliGraph.getMatches()){
			SemanticNode<?> hTerm = gnliGraph.getStartNode(matchEdge);
			// get all separate root nodes
			if (gnliGraph.getHypothesisGraph().getRoleGraph().getInEdges(hTerm).isEmpty()){
				//gnliGraph.getHypothesisGraph().displayRoles();
				//gnliGraph.getHypothesisGraph().displayContexts();
				rootNodeMatches.put(hTerm,matchEdge);
			} else {
				// get all double root nodes
				for (SemanticNode<?> node : gnliGraph.getHypothesisGraph().getRoleGraph().getNodes()){
					if (gnliGraph.getHypothesisGraph().getRoleGraph().getInEdges(node).isEmpty()){
						if (!gnliGraph.getHypothesisGraph().getRoleGraph().getEdges(node, hTerm).isEmpty()){
							if (gnliGraph.getHypothesisGraph().getRoleGraph().getEdges(node, hTerm).iterator().next().getLabel().equals("is_element") ||
									( node.getLabel().contains("be_") && gnliGraph.getHypothesisGraph().getRoleGraph().getEdges(node, hTerm).iterator().next().getLabel().equals("sem_subj"))  ){
								//gnliGraph.getHypothesisGraph().displayRoles();
								//gnliGraph.getHypothesisGraph().displayContexts();
								rootNodeMatches.put(hTerm,matchEdge);
							}
						}
					}
				}

				
			}
			//gnliGraph.getHypothesisGraph().displayDependencies();
			SemanticNode<?> tTerm = gnliGraph.getFinishNode(matchEdge);
			HashMap<SemanticNode<?>,Polarity> hTermCtxs = hypothesisContexts.get(hTerm);
			HashMap<SemanticNode<?>,Polarity> tTermCtxs = textContexts.get(tTerm);
			// in case one of the nodes is not in the context graph but we still need to find out its context
			// we get the ctx of that node from its SkolemNodeContent, find this context within the hypothesisContexts
			// (whatever ctx is in the SkolemContent will necessarily be one of the ctxs of the contetx graph), and put
			// the ctx found as the key of the hash and the veridical as the value and the root node as another key and
			// and veridicality of the mother ctx as the value
			if (hTermCtxs == null){
				hTermCtxs = fillEmptyContexts((SkolemNode) hTerm, "hyp");
			}
			if (tTermCtxs == null){
				tTermCtxs = fillEmptyContexts((SkolemNode) tTerm, "txt");
			}			
			if (contradiction(hTerm, tTerm, matchEdge, hTermCtxs, tTermCtxs, true)) {
				return;
			}
			/*if (contradiction(hTerm, tTerm, matchEdge, hTermCtxs, tTermCtxs, false)) {
				looseContra = true;
			}*/
			
		}
		if (!rootNodeMatches.isEmpty() && entailmentOrDisjoint(rootNodeMatches, true)) {
			return;
		}
		/*if (entailment(rootNodeMatch, hypRootNode, false)) {
			looseEntail = true;
		}*/
		
		if (looseContra == false && looseEntail == false){
			this.entailmentRelation = EntailmentRelation.NEUTRAL;
		}
		
	}
	
	
	private HashMap<SemanticNode<?>,Polarity> fillEmptyContexts(SkolemNode term, String mode){
		HashMap<SemanticNode<?>,Polarity> termCtxs = new HashMap<SemanticNode<?>,Polarity>();
		 HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> ctxs = null;
		 SemanticNode<?> root = null;
		 SemanticGraph graph = null;
		 if (mode.equals("hyp")){
			 ctxs = hypothesisContexts;
			 root = hypRootCtx;
			 graph = gnliGraph.getHypothesisGraph();
		 }
		 else {
			 ctxs = textContexts;
			 root = textRootCtx;
			 graph = gnliGraph.getTextGraph();
		 }
		String ctx = ((SkolemNodeContent) term.getContent()).getContext();
		for (SemanticNode<?> inReach : graph.getRoleGraph().getInReach(term)){
			if (ctxs.keySet().contains(inReach)){
				termCtxs.put(inReach, Polarity.VERIDICAL);
				// if the node is in top due to its original mapping, then keep it veridical in top
				if (ctx.equals("top"))
					termCtxs.put(root, Polarity.VERIDICAL);
				else
				// if the original mapping has set a different context to the node, then get the veridicality of this context and put it in top
					termCtxs.put(root, ctxs.get(inReach).get(root));
				break;
			}
		}
		return termCtxs;
	}
	
	private boolean contradiction(SemanticNode<?> hTerm, SemanticNode<?> tTerm, MatchEdge matchEdge, HashMap<SemanticNode<?>,Polarity> hTermCtxs,
			HashMap<SemanticNode<?>,Polarity> tTermCtxs, boolean strict){
		Specificity matchSpecificity = null;
		if (strict == true)
			matchSpecificity = matchEdge.getSpecificity();
		else 
			matchSpecificity = matchEdge.getOriginalSpecificity();
		Polarity hPolarity = hTermCtxs.get(hypRootCtx);
		Polarity tPolarity = tTermCtxs.get(textRootCtx);
		if (matchSpecificity!= Specificity.NONE && matchSpecificity != Specificity.DISJOINT) {
			if (hPolarity == Polarity.ANTIVERIDICAL && tPolarity == Polarity.VERIDICAL) {
				if (matchSpecificity == Specificity.EQUALS || matchSpecificity == Specificity.SUBCLASS) {
					this.entailmentRelation = EntailmentRelation.CONTRADICTION;
					penalizeLooseMatching(EntailmentRelation.CONTRADICTION, strict, matchEdge);
					return true;
				}
			} else if (hPolarity == Polarity.VERIDICAL && tPolarity == Polarity.ANTIVERIDICAL) {
				if (matchSpecificity == Specificity.EQUALS || matchSpecificity == Specificity.SUPERCLASS) {
					this.entailmentRelation = EntailmentRelation.CONTRADICTION;
					penalizeLooseMatching(EntailmentRelation.CONTRADICTION, strict, matchEdge);
					return true;
				}
			}
		} 
		return false;
	}
	
	private boolean entailmentOrDisjoint(HashMap<SemanticNode<?>, MatchEdge> rootNodeMatches , boolean strict){
		boolean entail = true;
		boolean disjoint = true;
		for (SemanticNode<?> key : rootNodeMatches.keySet()){
			Specificity matchSpecificity = null;
			if (strict == true)
				matchSpecificity = rootNodeMatches.get(key).getSpecificity();
			else 
				matchSpecificity = rootNodeMatches.get(key).getOriginalSpecificity();
			SemanticNode<?> tTerm = gnliGraph.getFinishNode(rootNodeMatches.get(key));
			HashMap<SemanticNode<?>,Polarity> hTermCtxs = hypothesisContexts.get(key);
			HashMap<SemanticNode<?>,Polarity> tTermCtxs = textContexts.get(tTerm);
			// in case one of the nodes is not in the context graph but we still need to find out its context
			// we get the ctx of that node from its SkolemNodeContent, find this context within the hypothesisContexts
			// (whatever ctx is in the SkolemContent will necessarily be one of the ctxs of the contetx graph), and put
			// the ctx found as the key of the hash and the veridical as the value and the root node as another key and
			// and veridicality of the mother ctx as the value
			if (hTermCtxs == null){
				hTermCtxs = fillEmptyContexts((SkolemNode) key, "hyp");
			}
			if (tTermCtxs == null){
				tTermCtxs = fillEmptyContexts((SkolemNode) tTerm, "txt");
			}	
			Polarity hPolarity = hTermCtxs.get(hypRootCtx);
			Polarity tPolarity = tTermCtxs.get(textRootCtx);
			if (hPolarity == Polarity.VERIDICAL && tPolarity == Polarity.VERIDICAL) {
				if (matchSpecificity != Specificity.DISJOINT){
					disjoint = false;
				} 
				if (matchSpecificity != Specificity.EQUALS && matchSpecificity != Specificity.SUBCLASS) {
					entail = false;		
				} 
				
			}
			else if (hPolarity == Polarity.ANTIVERIDICAL && tPolarity == Polarity.ANTIVERIDICAL) {
				if (matchSpecificity != Specificity.DISJOINT ){
					disjoint = false;
				}
				if (matchSpecificity != Specificity.EQUALS && matchSpecificity != Specificity.SUPERCLASS) {
					entail = false;		
				}
			} else{
				entail = false;
				disjoint = false;
			}
		}
		if (entail == true){
			this.entailmentRelation = EntailmentRelation.ENTAILMENT;
			for (MatchEdge edge : rootNodeMatches.values()){
				penalizeLooseMatching(EntailmentRelation.ENTAILMENT, strict, edge);
			}
			return entail;
		} else if (disjoint == true){
			this.entailmentRelation = EntailmentRelation.CONTRADICTION;
			for (MatchEdge edge : rootNodeMatches.values()){
				penalizeLooseMatching(EntailmentRelation.CONTRADICTION, strict, edge);
			}
			return disjoint;
		}
		return false;
	}
	
	
	private void penalizeLooseMatching(EntailmentRelation relation, boolean strict, MatchEdge matchEdge){
		this.matchStrength = 0;
		if (this.entailmentRelation == relation){
			if (strict == false)
				matchStrength = 1;
			matchStrength -= matchEdge.getScore();
			justifications.add(matchEdge);
		}
	}
	
	
	private void initialize() {
		this.hypothesisContexts = getContextHeads(gnliGraph.getHypothesisGraph());
		this.textContexts = getContextHeads(gnliGraph.getTextGraph());
	}
	
	
	private HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> getContextHeads(SemanticGraph semGraph){
		HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> ctxHeadsWithVerid = new HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>>();
		ArrayList<SemanticNode<?>> nodesVerInTop = new ArrayList<SemanticNode<?>>();
		SemanticNode<?> motherNode = null;
		for (SemanticNode<?> ctxNode : semGraph.getContextGraph().getNodes()){
			if (!ctxNode.getLabel().startsWith("ctx") && !ctxNode.getLabel().startsWith("top") ){
				/*if (ctxNode instanceof SkolemNode && ((SkolemNode) ctxNode).getContext().equals("top")){
					nodesVerInTop.add(ctxNode);
					continue;
				}*/
				HashMap<SemanticNode<?>, Polarity> hashOfCtxsAndVerid = new HashMap<SemanticNode<?>,Polarity>();
				SemanticNode<?> motherCtx = semGraph.getContextGraph().getInNeighbors(ctxNode).iterator().next();
				// if the ctxNode is not within a ctx(node) context
				if (motherCtx == null)
					continue;	
				Set<SemanticNode<?>> inNeighbors = semGraph.getContextGraph().getInNeighbors(motherCtx);
				// if the motherNode is at the same time the top node, i.e. does not have inNodes
				if (inNeighbors == null || inNeighbors.isEmpty()){
					if (semGraph.equals(gnliGraph.getHypothesisGraph())){
						this.hypRootCtx = motherCtx;
						motherNode = motherCtx;
					}
					else if (semGraph.equals(gnliGraph.getTextGraph())){
						this.textRootCtx = motherCtx;
						motherNode = motherCtx;
					}
					hashOfCtxsAndVerid.put(motherCtx, Polarity.VERIDICAL);
				} 
				else {
					Set<SemanticNode<?>> edgesCtxToInReachCtx = semGraph.getContextGraph().getInReach(motherCtx);
					for (SemanticNode<?> reach : edgesCtxToInReachCtx){
						if (reach.equals(motherCtx))
							continue;
						List<SemanticEdge> shortestPath = semGraph.getContextGraph().getShortestUndirectedPath(motherCtx, reach);
						Polarity polar = computeEdgePolarity(shortestPath);
						hashOfCtxsAndVerid.put(reach, polar);
						}
				}
				ctxHeadsWithVerid.put(ctxNode, hashOfCtxsAndVerid);
			}
		}
		/*for (SemanticNode<?> verInTop : nodesVerInTop){
			HashMap<SemanticNode<?>, Polarity> hashOfCtxsAndVerid = new HashMap<SemanticNode<?>,Polarity>();
			hashOfCtxsAndVerid.put(motherNode, Polarity.VERIDICAL);
			ctxHeadsWithVerid.put(verInTop, hashOfCtxsAndVerid);
		}*/
		return ctxHeadsWithVerid;
	}
	
	
	private Polarity computeEdgePolarity(List<SemanticEdge> shortestPath){
		Polarity polar = null;
		for (SemanticEdge verEdge : shortestPath){
			if (verEdge.getLabel().equals("veridical")){
				if (polar == null)
					polar = Polarity.VERIDICAL;
			}
			else if (verEdge.getLabel().equals("antiveridical")){
				if (polar == null)
					polar = Polarity.ANTIVERIDICAL;
				else if (polar.equals(Polarity.VERIDICAL))
					polar = Polarity.ANTIVERIDICAL;
				else if (polar.equals(Polarity.ANTIVERIDICAL))
					polar = Polarity.VERIDICAL;
			}
			else if  (verEdge.getLabel().equals("averidical")){
				polar = Polarity.AVERIDICAL;
			}
			else {
				polar = Polarity.VERIDICAL;
			}
		}
		return polar;
	}
	
}
