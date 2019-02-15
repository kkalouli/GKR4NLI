package gnli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import semantic.graph.SemanticEdge;
import semantic.graph.SemanticGraph;
import semantic.graph.SemanticNode;
import semantic.graph.vetypes.ContextNode;
import semantic.graph.vetypes.SkolemNode;
import semantic.graph.vetypes.SkolemNodeContent;
import semantic.graph.vetypes.TermNode;



public class InferenceChecker {

	public enum EntailmentRelation {ENTAILMENT, CONTRADICTION, NEUTRAL, UNKNOWN}
	private GNLIGraph gnliGraph;
	private EntailmentRelation entailmentRelation;
	private HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> hypothesisContexts;
	private HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> textContexts;
	private SemanticNode<?> hypRootCtx;
	private SemanticNode<?> textRootCtx;
	private double matchStrength;
	private MatchEdge justification;
	private boolean looseContra;
	private boolean looseEntail;
	
	enum Polarity {VERIDICAL, ANTIVERIDICAL, AVERIDICAL}

	
	public InferenceChecker(GNLIGraph gnliGraph) {
		this.gnliGraph = gnliGraph;
		this.entailmentRelation = EntailmentRelation.UNKNOWN; 
		looseContra = false;
		looseEntail = false;
		matchStrength = 2;
	}
	
	
	public InferenceDecision getInferenceDecision() {	
		if (this.hypothesisContexts == null) {
			initialize();
		}
		if (this.entailmentRelation == EntailmentRelation.UNKNOWN) {
			computeInferenceRelation();
		}
		return new InferenceDecision(entailmentRelation,matchStrength, justification, looseContra, looseEntail);
	}
	
	
	private void computeInferenceRelation() {
		MatchEdge rootNodeMatch = null;
		SemanticNode<?> hypRootNode = null;
		for (MatchEdge matchEdge : gnliGraph.getMatches()){
			SemanticNode<?> hTerm = gnliGraph.getStartNode(matchEdge);
			if (hTerm.equals(gnliGraph.getHypothesisGraph().getRootNode())){
				hypRootNode = hTerm;
				rootNodeMatch = matchEdge;
			}
			SemanticNode<?> tTerm = gnliGraph.getFinishNode(matchEdge);
			HashMap<SemanticNode<?>,Polarity> hTermCtxs = hypothesisContexts.get(hTerm);
			HashMap<SemanticNode<?>,Polarity> tTermCtxs = textContexts.get(tTerm);
			if (hTermCtxs == null && tTermCtxs == null)
				continue;
			// in case one of the nodes is not in the context graph but we still need to find out its context
			// for now everythign defaults to veridical to the top node; this can change by setting it to
			// veridical to whatever node is in the node.getContent().getContext()
			if (hTermCtxs == null){
				hTermCtxs = new HashMap<SemanticNode<?>,Polarity>();
				hTermCtxs.put(hypRootCtx, Polarity.VERIDICAL);		
			}
			if (tTermCtxs == null){
				tTermCtxs = new HashMap<SemanticNode<?>,Polarity>();
				hTermCtxs.put(textRootCtx, Polarity.VERIDICAL);
			}			
			if (contradiction(hTerm, tTerm, matchEdge, hTermCtxs, tTermCtxs, true)) {
				return;
			}
			/*if (contradiction(hTerm, tTerm, matchEdge, hTermCtxs, tTermCtxs, false)) {
				looseContra = true;
			}*/
			
		}
		if (entailment(rootNodeMatch, hypRootNode, true)) {
			return;
		}
		/*if (entailment(rootNodeMatch, hypRootNode, false)) {
			looseEntail = true;
		}*/
		
		if (looseContra == false && looseEntail == false){
			this.entailmentRelation = EntailmentRelation.NEUTRAL;
			justification = null;
		}
		
	}
	
	private boolean contradiction(SemanticNode<?> hTerm, SemanticNode<?> tTerm, MatchEdge matchEdge, HashMap<SemanticNode<?>,Polarity> hTermCtxs,
			HashMap<SemanticNode<?>,Polarity> tTermCtxs, boolean strict){
		Specificity matchSpecificity = null;
		if (strict == true)
			matchSpecificity = matchEdge.getSpecificity();
		else 
			matchSpecificity = matchEdge.getOriginalSpecificity();
		if (matchSpecificity!= Specificity.NONE && matchSpecificity != Specificity.DISJOINT) {
			Polarity hPolarity = hTermCtxs.get(hypRootCtx);
			Polarity tPolarity = tTermCtxs.get(textRootCtx);
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
	
	private boolean entailment(MatchEdge matchEdge, SemanticNode<?> hypRootNode, boolean strict){
		Specificity matchSpecificity = null;
		if (strict == true)
			matchSpecificity = matchEdge.getSpecificity();
		else 
			matchSpecificity = matchEdge.getOriginalSpecificity();
		SemanticNode<?> tTerm = gnliGraph.getFinishNode(matchEdge);
		HashMap<SemanticNode<?>,Polarity> hTermCtxs = hypothesisContexts.get(hypRootNode);
		HashMap<SemanticNode<?>,Polarity> tTermCtxs = textContexts.get(tTerm);
		Polarity hPolarity = hTermCtxs.get(hypRootCtx);
		Polarity tPolarity = tTermCtxs.get(textRootCtx);
		if (hPolarity == Polarity.VERIDICAL && tPolarity == Polarity.VERIDICAL) {
			if (matchSpecificity == Specificity.EQUALS || matchSpecificity == Specificity.SUBCLASS) {
				this.entailmentRelation = EntailmentRelation.ENTAILMENT;
				penalizeLooseMatching(EntailmentRelation.ENTAILMENT, strict, matchEdge);
				return true;
			}
		}
		else if (hPolarity == Polarity.ANTIVERIDICAL && tPolarity == Polarity.ANTIVERIDICAL) {
			if (matchSpecificity == Specificity.EQUALS || matchSpecificity == Specificity.SUPERCLASS) {
				this.entailmentRelation = EntailmentRelation.ENTAILMENT;
				penalizeLooseMatching(EntailmentRelation.ENTAILMENT, strict, matchEdge);
				return true;
			}
		}
		return false;
	}
	
	
	private void penalizeLooseMatching(EntailmentRelation relation, boolean strict, MatchEdge matchEdge){
		this.matchStrength = 2;
		if (this.entailmentRelation == relation){
			if (strict == false)
				matchStrength = 1;
			matchStrength -= matchEdge.getScore();
			justification = matchEdge;
		}
	}
	
	
	private void initialize() {
		this.hypothesisContexts = getContextHeads(gnliGraph.getHypothesisGraph());
		this.textContexts = getContextHeads(gnliGraph.getTextGraph());
	}
	
	
	private HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> getContextHeads(SemanticGraph semGraph){
		HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> ctxHeadsWithVerid = new HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>>();
		SemanticNode<?> root = null;
		for (SemanticNode<?> ctxNode : semGraph.getContextGraph().getNodes()){
			if (!ctxNode.getLabel().startsWith("ctx") && !ctxNode.getLabel().startsWith("top") ){
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
						root = hypRootCtx;
					}
					else if (semGraph.equals(gnliGraph.getTextGraph())){
						this.textRootCtx = motherCtx;
						root = textRootCtx;
					}
					hashOfCtxsAndVerid.put(motherCtx, Polarity.VERIDICAL);
				} 
				else {
					Set<SemanticNode<?>> edgesCtxToInReachCtx = semGraph.getContextGraph().getInReach(motherCtx);
					for (SemanticNode<?> reach : edgesCtxToInReachCtx){
						List<SemanticEdge> shortestPath = semGraph.getContextGraph().getShortestUndirectedPath(motherCtx, reach);
						Polarity polar = computeEdgePolarity(shortestPath);
						hashOfCtxsAndVerid.put(reach, polar);
						}
				}
				ctxHeadsWithVerid.put(ctxNode, hashOfCtxsAndVerid);
			}
		}
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
		}
		return polar;
	}
	
	class InferenceDecision{
		
		EntailmentRelation relation;
		double matchStrength;
		MatchEdge justification;
		boolean looseContra;
		boolean looseEntail;
			
		InferenceDecision(EntailmentRelation relation, double matchStrength, MatchEdge justification, boolean looseContra, boolean looseEntail){
			this.relation = relation;
			this.matchStrength = matchStrength;
			this.justification = justification;
			this.looseContra = looseContra;
			this.looseEntail = looseEntail;
		}
		
		public EntailmentRelation getEntailmentRelation(){
			return relation;
		}
		
		public Double getMatchStrength(){
			return matchStrength;
		}
		
		public boolean isLooseContr(){
			return looseContra;
		}
		
		public boolean isLooseEntail(){
			return looseEntail;
		}
		
		public MatchEdge getJustification(){
			return justification;
		}
			
	}
	


	
	
}
