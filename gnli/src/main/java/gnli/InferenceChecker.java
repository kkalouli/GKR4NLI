package gnli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import gnli.InferenceChecker.EntailmentRelation;
import sem.graph.SemanticEdge;
import sem.graph.SemanticGraph;
import sem.graph.SemanticNode;
import sem.graph.vetypes.SenseNode;
import sem.graph.vetypes.SenseNodeContent;
import sem.graph.vetypes.SkolemNode;
import sem.graph.vetypes.SkolemNodeContent;
import sem.graph.vetypes.TermNode;



/**
 * This class assigns and holds the final inference label for a given pair.
 * @author Katerina Kalouli, 2019
 *
 */
public class InferenceChecker {

	public enum EntailmentRelation {ENTAILMENT, CONTRADICTION, NEUTRAL, UNKNOWN}
	private GNLIGraph gnliGraph;
	private EntailmentRelation entailmentRelation;
	private HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> hypothesisContexts;
	private HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> textContexts;
	private SemanticNode<?> hypRootCtx;
	private SemanticNode<?> textRootCtx;
	private double matchStrength;
	private double matchConfidence;
	private ArrayList<MatchEdge> justifications;
	private boolean looseContra;
	private boolean looseEntail;
	private InferenceComputer infComputer;
	private SpecificityUpdater updater;
	private String labelToLearn;
	private String pairID;
	private HashMap<String,String> vnToPWNMap;
	private boolean rootsWasEmpty;
	private boolean tHasComplexCtxs;
	private boolean hHasComplexCtxs;
	private boolean contraFlag;
	private boolean tVeridical;
	private boolean tAntiveridical;
	private boolean tAveridical;
	private boolean hVeridical;
	private boolean hAntiveridical;
	private boolean hAveridical;
	private boolean equalsRel;
	private boolean superRel;
	private boolean subRel;
	private boolean disjointRel;
	private Integer ruleUsed;
	
	enum Polarity {VERIDICAL, ANTIVERIDICAL, AVERIDICAL}

	/**
	 * Constuctor of the class. Most parameters are passed from the {@link InferenceComputer}.
	 * @param gnliGraph
	 * @param infComputer
	 * @param updater
	 * @param labelToLearn
	 * @param pairID
	 */
	public InferenceChecker(GNLIGraph gnliGraph, InferenceComputer infComputer, SpecificityUpdater updater, String labelToLearn, String pairID) {
		this.gnliGraph = gnliGraph;
		this.entailmentRelation = EntailmentRelation.UNKNOWN; 
		looseContra = false;
		looseEntail = false;
		matchStrength = 0;
		matchConfidence = 0;
		justifications = new ArrayList<MatchEdge>();
		this.infComputer = infComputer;
		this.updater = updater;
		this.labelToLearn = labelToLearn;
		this.pairID = pairID;
		this.vnToPWNMap = infComputer.getVnToPWNMap();
		this.rootsWasEmpty = false;
		this.tHasComplexCtxs = false;
		this.hHasComplexCtxs = false;
		this.contraFlag = false;
		this.tVeridical = false;
		this.tAntiveridical = false;
		this.tAveridical = false;
		this.equalsRel = false;
		this.superRel = false;
		this.subRel = false;
		this.disjointRel = false;
		this.ruleUsed = 0;


	}
	
	/** 
	 * Return the inference decision, along with additional features (some of them are used by the
	 * hybrid classifier as features)  
	 * @return
	 */
	public InferenceDecision getInferenceDecision() {	
		if (this.hypothesisContexts == null) {
			initialize();
		}
		if (this.entailmentRelation == EntailmentRelation.UNKNOWN) {
			computeInferenceRelation();
		}
						
		return new InferenceDecision(entailmentRelation,matchStrength, matchConfidence, ruleUsed, tHasComplexCtxs, hHasComplexCtxs, contraFlag,
				tVeridical, tAntiveridical,  tAveridical, hVeridical, hAntiveridical, hAveridical,
				equalsRel, superRel, subRel, disjointRel, justifications, looseContra, looseEntail, gnliGraph);
	}
	
	/**
	 * Main method for the computation of the inference relation. The method determines the root node match
	 * on which the inference should be computed. If no root node match can be found, we fall-back
	 * to the next highest (in terms of graph) match.  
	 */
	private void computeInferenceRelation() {
		HashMap<SemanticNode<?>,MatchEdge> rootNodeMatches = new HashMap<SemanticNode<?>,MatchEdge>();
		//gnliGraph.getMatchGraph().display();
		// find the root node of the role graph; there might be more than one root nodes or a double root node
		for (MatchEdge matchEdge : gnliGraph.getMatches()){
			SemanticNode<?> hTerm = gnliGraph.getStartNode(matchEdge);
			SemanticNode<?> tTerm = gnliGraph.getFinishNode(matchEdge);
			boolean expletive = false;
			// get all separate root nodes		
			if (gnliGraph.getHypothesisGraph().getRoleGraph().getInEdges(hTerm).isEmpty()){
				for (SemanticNode<?> inReach : gnliGraph.getTextGraph().getRoleGraph().getInReach(tTerm)){
					if (inReach.getLabel().startsWith("be_"))
						expletive = true;
				}
				//gnliGraph.getHypothesisGraph().displayRoles();
				//gnliGraph.getHypothesisGraph().displayContexts();
				if (expletive == false)
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
			// comment out following lines in order to always reach the extractItemsSetsForAssociationRuleMining method
			// in order to get all paths and their contexts (for training)		
			//if (contradiction(hTerm, tTerm, matchEdge, hTermCtxs, tTermCtxs, true)) {
			//	return;
			//}
			
			//check whether there is a contradiction
			contradiction(hTerm, tTerm, matchEdge, hTermCtxs, tTermCtxs, true);

			
			/* This is needed to extract the feature "hasCOmplexCtxs" for the hybrid classifier.
			There is a complex ctxs if there are not only "ctx_hd" edges but also other embedded edges,
			like veridical/antiveridical/averidical. */
			for (SemanticNode<?> key: hTermCtxs.keySet()) {
				if (key.getLabel().contains("top")) {
					hHasComplexCtxs = false;
				} else {
					hHasComplexCtxs = true;
					break;
				}			
			}
			for (SemanticNode<?> key: tTermCtxs.keySet()) {
				if (key.getLabel().contains("top")) {
					tHasComplexCtxs = false;
				} else {
					tHasComplexCtxs = true;
					break;
				}			
			}
			
		}
		
		// if no root node match can be found, then fall-back to the next highest match, i.e. match with the most modifiers.
		if (rootNodeMatches.isEmpty()){
			if (updater.getMatchAgendaStable() != null && !updater.getMatchAgendaStable().isEmpty()) {
				MatchEdge matchWithTheMostModifiers = updater.getMatchAgendaStable().get(updater.getMatchAgendaStable().size()-1).match;
				SemanticNode<?> hTerm = gnliGraph.getMatchGraph().getStartNode(matchWithTheMostModifiers);			
				rootNodeMatches.put(hTerm,matchWithTheMostModifiers);
				rootsWasEmpty = true;
			} 
		}

		// comment out following lines in order to extractItemsSetsForAssociationRuleMining
		// in order to get all paths and their contexts (for training)
		//if (!rootNodeMatches.isEmpty() && entailmentOrDisjoint(rootNodeMatches, true)){
		//	return;
		//}
		
		/* if root node matches are found, then extract the features for the hybrid system
		and decide on final inference label (after investigating entailment and disjointess). */
		if (!rootNodeMatches.isEmpty()) { 
			try {
				//extractItemsSetsForAssociationRuleMining(rootNodeMatches);
				extractFeaturesForInferenceDecision(rootNodeMatches);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (this.entailmentRelation == EntailmentRelation.UNKNOWN && rootsWasEmpty == false) {
				if (entailmentOrDisjoint(rootNodeMatches, true))
					return; 
			} else if (this.entailmentRelation != EntailmentRelation.UNKNOWN)
				return;
		
		} 
		/* if no root node matches can be found (even after considering the highest match),
		there are no features to be extracted for the hybrid classifier and thus just output
		some text message. */
		else {
			infComputer.getPathsAndCtxs().add(pairID+"\tnothing_found\t"+labelToLearn);
		}
				
		// if no entailment or contradiction, then neutral
		if (looseContra == false && looseEntail == false){
			this.entailmentRelation = EntailmentRelation.NEUTRAL;
			this.ruleUsed = 25;
		}
		
	}
	
	/**
	 * 	In case one of the nodes is not in the context graph but we still need to find out its context, 
	 * we get the ctx of that node from its SkolemNodeContent, find this context within the hypothesisContexts 
	 * (whatever ctx is in the SkolemContent will necessarily be one of the ctxs of the contetx graph), and put
	 * the ctx found as the key of the hash and the veridical as the value and the root node as another key and
	 * and veridicality of the mother ctx as the value

	 * @param term
	 * @param mode
	 * @return
	 */
	private HashMap<SemanticNode<?>,Polarity> fillEmptyContexts(TermNode term, String mode){
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
		 String ctx = "";
		 if (term instanceof SkolemNode)
			 ctx = ((SkolemNodeContent) term.getContent()).getContext();
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
			// the combined nodes are not included in the ctxs so they have to be treated separately
			else if (inReach.getLabel().contains("_or_") || inReach.getLabel().contains("_and_")){
				// if the term has top as its context, then it is veridical
				if (ctx.equals("top"))
					termCtxs.put(root, Polarity.VERIDICAL);
				// if ctx is empty and the term is included in the ctxs, then get the veridicality of the term node
				else if (ctxs.containsKey(term))
					termCtxs.put(root, ctxs.get(term).get(root));
				break;
			}
		}
		return termCtxs;
	}
	
	/**
	 * Check whether the given match is contradictory or not. This concerns contradictions
	 * coming from opposite contexts/instantiabilities. Note that within this method, there
	 * are also cases of opposite context/instantiabilities that lead to entailment and not
	 * contradiction.
	 * @param hTerm
	 * @param tTerm
	 * @param matchEdge
	 * @param hTermCtxs
	 * @param tTermCtxs
	 * @param strict
	 * @return
	 */
	private boolean contradiction(SemanticNode<?> hTerm, SemanticNode<?> tTerm, MatchEdge matchEdge, HashMap<SemanticNode<?>,Polarity> hTermCtxs,
			HashMap<SemanticNode<?>,Polarity> tTermCtxs, boolean strict){
		Specificity matchSpecificity = null;
		if (strict == true)
			matchSpecificity = matchEdge.getSpecificity();
		else 
			matchSpecificity = matchEdge.getOriginalSpecificity();
		Polarity hPolarity = hTermCtxs.get(hypRootCtx);		
		Polarity tPolarity = tTermCtxs.get(textRootCtx);
		double cost = matchEdge.getFeatureScore("cost");
		if (matchSpecificity!= Specificity.NONE) {
			if (hPolarity == Polarity.ANTIVERIDICAL && tPolarity == Polarity.VERIDICAL) {
					if ( matchSpecificity == Specificity.DISJOINT) {
					this.entailmentRelation = EntailmentRelation.ENTAILMENT;
					penalizeLooseMatching(EntailmentRelation.ENTAILMENT, strict, matchEdge);
					this.ruleUsed = 1;
					return true;
				}
				// if there is a contraFlag
				if (costInContraBounds(cost) ) {
					this.entailmentRelation = EntailmentRelation.ENTAILMENT;
					penalizeLooseMatching(EntailmentRelation.ENTAILMENT, strict, matchEdge);
					this.ruleUsed = 2;
					return true;
				}
				if (matchSpecificity == Specificity.EQUALS) {
					this.entailmentRelation = EntailmentRelation.CONTRADICTION;
					penalizeLooseMatching(EntailmentRelation.CONTRADICTION, strict, matchEdge);
					this.ruleUsed = 3;
					return true;
				}
				if (matchSpecificity == Specificity.SUBCLASS) {
					this.entailmentRelation = EntailmentRelation.CONTRADICTION;
					penalizeLooseMatching(EntailmentRelation.CONTRADICTION, strict, matchEdge);
					this.ruleUsed = 4;
					return true;
				}
			} else if (hPolarity == Polarity.VERIDICAL && tPolarity == Polarity.ANTIVERIDICAL) {
				if (matchSpecificity == Specificity.DISJOINT) {
					this.entailmentRelation = EntailmentRelation.ENTAILMENT;
					penalizeLooseMatching(EntailmentRelation.ENTAILMENT, strict, matchEdge);
					this.ruleUsed = 5;
					return true;
				}
				// if there is a contraFlag
				if (costInContraBounds(cost)) {
					this.entailmentRelation = EntailmentRelation.ENTAILMENT;
					penalizeLooseMatching(EntailmentRelation.ENTAILMENT, strict, matchEdge);
					this.ruleUsed = 6;
					return true;
				}
				if (matchSpecificity == Specificity.EQUALS) {
					this.entailmentRelation = EntailmentRelation.CONTRADICTION;
					penalizeLooseMatching(EntailmentRelation.CONTRADICTION, strict, matchEdge);
					this.ruleUsed = 7;
					return true;
				}
				if (matchSpecificity == Specificity.SUPERCLASS) {
					this.entailmentRelation = EntailmentRelation.CONTRADICTION;
					penalizeLooseMatching(EntailmentRelation.CONTRADICTION, strict, matchEdge);
					this.ruleUsed = 8;
					return true;
				}
			}
		} 
		return false;
	}
	
	/**
	 * Extract for all root nodes matches, the paths and contexts of these matches. This was needed
	 * for experimenting with classifiers on this data. It is not used in the current version of the system.
	 * @param rootNodeMatches
	 * @return
	 * @throws IOException
	 */
	private String extractAllPathsAndContext(HashMap<SemanticNode<?>, MatchEdge> rootNodeMatches) throws IOException{
		String toWrite = "";
		for (SemanticNode<?> key : rootNodeMatches.keySet()){		
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
			String matchSpec = rootNodeMatches.get(key).getSpecificity().toString();
			toWrite += matchSpec+":"+tPolarity.toString()+"-"+hPolarity.toString()+"\t";
			// get the paths and the ctxs of each path
			for (HeadModifierPathPair just: rootNodeMatches.get(key).getJustification()){
				String encodedCtxAndPath = ":";
				if (just.getModifiersPair() != null)
					encodedCtxAndPath = ((MatchEdge) just.getModifiersPair()).getSpecificity().toString()+":";	
				List<SemanticEdge> tPath = just.getPremisePath();
				List<SemanticEdge> hPath = just.getHypothesisPath();
				if (tPath != null){
					// for each path, get the concept to which the path ends
					for (SemanticEdge e: tPath){
						SemanticNode<?> finish = gnliGraph.getTextGraph().getRoleGraph().getEndNode(e);
						// get the context of the concept mapped tot his edge
						Polarity ctx = fillEmptyContexts((TermNode) finish, "txt").get(textRootCtx);					
						String ctxToAdd = "";
						if (ctx != null)
							 ctxToAdd = ctx.toString();		
						encodedCtxAndPath += ctxToAdd+"/"+e.getLabel()+",";
					}
				}
				encodedCtxAndPath += "-";
				if (hPath != null){
					for (SemanticEdge e: hPath){
						SemanticNode<?> finish = gnliGraph.getHypothesisGraph().getRoleGraph().getEndNode(e);
						Polarity ctx = fillEmptyContexts((TermNode) finish, "hyp").get(hypRootCtx);
						String ctxToAdd = "";
						if (ctx != null)
							 ctxToAdd = ctx.toString();		
						encodedCtxAndPath += ctxToAdd+"/"+e.getLabel()+",";
					}
				}
				toWrite += encodedCtxAndPath+"\t";
			}
		}	
		return toWrite;
	}
	
	/**
	 * Experimenting with weighting the rules of the association rule mining algorithm
	 * because some path combinations are found in more than one inference relations.
	 * This method is currently not used within the system: no differences in performance. 
	 * @param encodedCtxAndPath
	 * @return
	 */
	private String computeDecisionOfAssociationMining(String encodedCtxAndPath){
		String relation = "";
		// split the path into its elements
		String[] elementsOfPath = encodedCtxAndPath.split("\t");
		// convert array to list
		List<String> listOfPath = new ArrayList<String>(Arrays.asList(elementsOfPath)); 
		// make a copy of the list of the path because we are going to remove elements from it
		List<String> copyOfListOfPath = new ArrayList<String>(Arrays.asList(elementsOfPath)); 
		int lengthOfPath = elementsOfPath.length;
		// counter to keep track of the common elements between path and current rule
		int currentCommonElem = 0;
		// list to store rules that were found to have more common elements with the path than the threshold 
		ArrayList<String> currentCommonRules = new ArrayList<String>();
		for (String rule : infComputer.getAssociationRules().keySet()){
			// split also the rule into elements
			String[] elementsOfRule = rule.split(",\\s");
			List<String> listOfRule = new ArrayList<String>(Arrays.asList(elementsOfRule)); 
			// keeps the elements of path that are also included in the rule
			try {
				if (listOfPath.retainAll(listOfRule)){
					// the listOfPath now only contains the common elements, so the size of the list is the number
					// of common elements
					int noOfCommonElem = listOfPath.size();
					// if the no of common elements is greater than the previous one,
					if (noOfCommonElem > currentCommonElem){
						// set the current number as the greatest one
						currentCommonElem = noOfCommonElem;
						// remove all previous common rules from the list and add this one
						currentCommonRules.clear();
						currentCommonRules.add(rule);
					}
					// if the no of common elements is equal to the previous one,
					else if (noOfCommonElem == currentCommonElem && noOfCommonElem != 0 ){
						// just add the current rule
						currentCommonRules.add(rule);
					}
				}
			} catch (Exception e){
				System.out.println(e);
			}
		}
		// we now want to make sure that elements were not found as common elements,
		// are not included themselves (as a whole) to any other rule
		// this would annulate them
		ArrayList<String> relationsExtracted = new ArrayList<String>();
		// otherwise, listOfPath is already trimmed at this point
		List<String> listOfPath2 = new ArrayList<String>(Arrays.asList(elementsOfPath)); 
		for (String commonRule : currentCommonRules){
			String[] elementsOfRule = commonRule.split(",\\s");
			List<String> listOfRule = new ArrayList<String>(Arrays.asList(elementsOfRule)); 		
			// keeps the elements of path that are also included in the rule
			listOfPath2.retainAll(listOfRule);
			// from the list containing all elements, remove the common ones (listOfPath now only includes common ones due to
			// previous step)
			copyOfListOfPath.removeAll(listOfPath2);
			String[] remainingElements = copyOfListOfPath.toArray(new String[0]);
			List<String> listOfRemainingElements = new ArrayList<String>( Arrays.asList(remainingElements)); 
			// check if all remaining elements are contained in a rule, if not add the relation of the current 
			// rule to the list of relations
			if (!infComputer.getAssociationRules().keySet().containsAll(listOfRemainingElements)){
				relationsExtracted.add(infComputer.getAssociationRules().get(commonRule));
			}
		}
		// check to see which relation exists how many times
		int countEntail = 0;
		int countContra = 0;
		int countNeut = 0;
		for (String rel : relationsExtracted){
			if (rel.contains("N"))
				countNeut ++;
			else if (rel.contains("C"))
				countContra ++;
			else if (rel.contains("E"))
				countEntail ++;
		}
		if (countEntail > countContra && countContra >= countNeut)
			relation = "ENTAILMENT";
		else if (countContra > countEntail && countEntail >= countNeut)
			relation = "CONTRADICTION";
		else if (countNeut > countContra && countContra >= countEntail)
			relation = "NEUTRAL";
		
		return relation;	
	}
	
	/** 
	 * Extract the path combinations that are used for the training of the association
	 * rule mining algorithm. It extracts more features than the ones actually used for the
	 * association rule mining algorithm. This method is not called every time the pipeline runs.
	 * It was run once to extract the combinations on which the association rule mining could
	 * be applied and was then commented out. 
	 * @param rootNodeMatches
	 * @throws IOException
	 */
	private void extractItemsSetsForAssociationRuleMining(HashMap<SemanticNode<?>, MatchEdge> rootNodeMatches) throws IOException{
		String toWrite = pairID+"\t";
		// keep track of whether there was a real root (verb) matchign or whether one of the other
		//matches was used as root
		String rootsEmpty = "no";
		if (rootsWasEmpty == true)
			rootsEmpty = "yes";
		toWrite += rootsEmpty+"\t";
		for (SemanticNode<?> key : rootNodeMatches.keySet()){		
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
			String matchSpec = rootNodeMatches.get(key).getOriginalSpecificity().toString();
			String keyVnClass = "";
			float keySenseScore = 0;
			String tTermVnClass = "";
			float tTermSenseScore = 0;
			// get the senseKey of the key (hTerm)
			Set<SemanticNode<?>> keySenseNodes = gnliGraph.getHypothesisGraph().getLexGraph().getOutNeighbors(key);
			for (SemanticNode<?> sNode : keySenseNodes) {
				if (sNode instanceof SenseNode) {
					String keySenseKey = ((SenseNodeContent) sNode.getContent()).getSenseKey();					
					if (vnToPWNMap.containsKey(keySenseKey)) {
						 keyVnClass = vnToPWNMap.get(keySenseKey);
						 if (((SenseNodeContent) sNode.getContent()).getSenseScore() > keySenseScore) {
							 keySenseScore = ((SenseNodeContent) sNode.getContent()).getSenseScore();
						 }
					}
						
				}
			}
			// get the senseKey of the tTerm
			Set<SemanticNode<?>> tTermSenseNodes = gnliGraph.getTextGraph().getLexGraph().getOutNeighbors(tTerm);
			for (SemanticNode<?> sNode : tTermSenseNodes) {
				if (sNode instanceof SenseNode) {
					String tTermSenseKey = ((SenseNodeContent) sNode.getContent()).getSenseKey();					
					if (vnToPWNMap.containsKey(tTermSenseKey)) {
						tTermVnClass = vnToPWNMap.get(tTermSenseKey);
						if (((SenseNodeContent) sNode.getContent()).getSenseScore() >= tTermSenseScore) {
							tTermSenseScore = ((SenseNodeContent) sNode.getContent()).getSenseScore();
						 }
					}
				}
			}
			//toWrite += matchSpec+":"+tPolarity.toString()+"-"+hPolarity.toString()+"\t";
			toWrite += ((SkolemNodeContent) key.getContent()).getStem()+"-"+((SkolemNodeContent) tTerm.getContent()).getStem()+":"+keyVnClass+"-"+tTermVnClass+":"+matchSpec+":"+tPolarity.toString()+"-"+hPolarity.toString()+"\t";
			// get the paths and the ctxs of each path
			for (HeadModifierPathPair just: rootNodeMatches.get(key).getJustification()){
				String encodedCtxAndPath = ":";
				if (just.getModifiersPair() != null)
					encodedCtxAndPath = ((MatchEdge) just.getModifiersPair()).getOriginalSpecificity().toString()+":";	
				List<SemanticEdge> tPath = just.getPremisePath();
				List<SemanticEdge> hPath = just.getHypothesisPath();
				if (tPath != null){
					// for each path, get the concept to which the path ends
					for (SemanticEdge e: tPath){
						SemanticNode<?> finish = gnliGraph.getTextGraph().getRoleGraph().getEndNode(e);
						// get the context of the concept mapped tot his edge
						Polarity ctx = fillEmptyContexts((TermNode) finish, "txt").get(textRootCtx);					
						String ctxToAdd = "";
						if (ctx != null)
							 ctxToAdd = ctx.toString();		
						encodedCtxAndPath += ctxToAdd+"/"+e.getLabel()+",";
					}
				}
				encodedCtxAndPath += "-";
				if (hPath != null){
					for (SemanticEdge e: hPath){
						SemanticNode<?> finish = gnliGraph.getHypothesisGraph().getRoleGraph().getEndNode(e);
						Polarity ctx = fillEmptyContexts((TermNode) finish, "hyp").get(hypRootCtx);
						String ctxToAdd = "";
						if (ctx != null)
							 ctxToAdd = ctx.toString();		
						encodedCtxAndPath += ctxToAdd+"/"+e.getLabel()+",";
					}
				}
				toWrite += encodedCtxAndPath+"\t";
			}
		}
		infComputer.getPathsAndCtxs().add(toWrite+labelToLearn);
	}
	
	/**
	 * Extracts features for the training of the hybrid classifier. The features are added to the
	 * InferenceDecision output. 
	 * @param rootNodeMatches
	 * @throws IOException
	 */
	private void extractFeaturesForInferenceDecision(HashMap<SemanticNode<?>, MatchEdge> rootNodeMatches) throws IOException{
		for (SemanticNode<?> key : rootNodeMatches.keySet()){		
			double cost = rootNodeMatches.get(key).getFeatureScore("cost");
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
			String matchSpec = rootNodeMatches.get(key).getSpecificity().toString();
			if (hPolarity == Polarity.AVERIDICAL) {
				hAveridical = true;
			} 
			if (hPolarity == Polarity.ANTIVERIDICAL) {
				hAntiveridical = true;
			} 
			if (hPolarity == Polarity.VERIDICAL) {
				hVeridical = true;
			}
			
			if (tPolarity == Polarity.AVERIDICAL) {
				tAveridical = true;
			} 
			if (tPolarity == Polarity.ANTIVERIDICAL) {
				tAntiveridical = true;
			} 
			if (tPolarity == Polarity.VERIDICAL) {
				tVeridical = true;
			}
			
			if (matchSpec.equals("EQUALS")) {
				equalsRel = true;
			}
			
			if (matchSpec.equals("SUPERCLASS")) {
				superRel = true;
			}
			
			if (matchSpec.equals("SUBCLASS")) {
				subRel = true;
			}
			
			if (matchSpec.equals("DISJOINT")) {
				disjointRel = true;
			}
			if (costInContraBounds(cost)) {
				contraFlag = true;
			}
			
			// get the paths and the ctxs of each path
			for (HeadModifierPathPair just: rootNodeMatches.get(key).getJustification()){
				String justSpec = "";
				if (just.getModifiersPair() != null)
					justSpec = ((MatchEdge) just.getModifiersPair()).getSpecificity().toString();	
				List<SemanticEdge> tPath = just.getPremisePath();
				List<SemanticEdge> hPath = just.getHypothesisPath();
				Polarity tCtx = null;
				Polarity hCtx = null;
				if (tPath != null){
					// for each path, get the concept to which the path ends
					for (SemanticEdge e: tPath){
						SemanticNode<?> finish = gnliGraph.getTextGraph().getRoleGraph().getEndNode(e);
						// get the context of the concept mapped tot his edge
						Polarity ctx = fillEmptyContexts((TermNode) finish, "txt").get(textRootCtx);					
						if (ctx != null)
							tCtx = ctx;
					}
				}
				if (hPath != null){
					for (SemanticEdge e: hPath){
						SemanticNode<?> finish = gnliGraph.getHypothesisGraph().getRoleGraph().getEndNode(e);
						Polarity ctx = fillEmptyContexts((TermNode) finish, "hyp").get(hypRootCtx);
						if (ctx != null)
							hCtx = ctx;		
					}
				}
				if (hCtx == Polarity.AVERIDICAL) {
					hAveridical = true;
				} 
				if (hCtx == Polarity.ANTIVERIDICAL) {
					hAntiveridical = true;
				} 
				if (hCtx == Polarity.VERIDICAL) {
					hVeridical = true;
				}
				
				if (tCtx == Polarity.AVERIDICAL) {
					tAveridical = true;
					//System.out.println("test1");
				} 
				if (tCtx == Polarity.ANTIVERIDICAL) {
					tAntiveridical = true;
				} 
				if (tCtx == Polarity.VERIDICAL) {
					tVeridical = true;
				}
					
				if (justSpec.equals("EQUALS")) {
					equalsRel = true;
				}
				
				if (justSpec.equals("SUPERCLASS")) {
					superRel = true;
				}
				
				if (justSpec.equals("SUBCLASS")) {
					subRel = true;
				}
				
				if (justSpec.equals("DISJOINT")) {
					disjointRel = true;
				}				
			}
		}
			
	}

	/**
	 * Check whether there is an entailment or disjointness between the terms of a match. This method
	 * considers the cases where the two terms have the same contexts/instantiabilities. 
	 * @param rootNodeMatches
	 * @param strict
	 * @return
	 */
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
			// check whether there are complex contexts for getting it as a feature
			for (SemanticNode<?> cCtx: hTermCtxs.keySet()) {
				if (cCtx.getLabel().contains("top")) {
					hHasComplexCtxs = false;
				} else {
					hHasComplexCtxs = true;
					break;
				}			
			}
			for (SemanticNode<?> cCtx: tTermCtxs.keySet()) {
				if (cCtx.getLabel().contains("top")) {
					tHasComplexCtxs = false;
				} else {
					tHasComplexCtxs = true;
					break;
				}			
			}
			Polarity hPolarity = hTermCtxs.get(hypRootCtx);
			Polarity tPolarity = tTermCtxs.get(textRootCtx);
			double cost = rootNodeMatches.get(key).getFeatureScore("cost");
			// both terms are instantiated:
			if (hPolarity == Polarity.VERIDICAL && tPolarity == Polarity.VERIDICAL) {
				// it is not a contradiction if there is no disjoint relation and there is no contraFlag
				if (matchSpecificity != Specificity.DISJOINT && !costInContraBounds(cost))
					disjoint = false;
				// if there is equals relation but there is a contraFlag or neutralFlag, then there is no entail but contradiction
				if ( (matchSpecificity == Specificity.EQUALS && costInContraBounds(cost) )
						|| ( matchSpecificity == Specificity.EQUALS && costInNeutralBounds(cost) )){
					entail = false;
					this.ruleUsed = 9;
				}
				// if there is subclass relation but there is a contraFlag or neutralFlag, then there is no entail but contradiction
				if ( (matchSpecificity == Specificity.SUBCLASS && costInContraBounds(cost) )
						|| ( matchSpecificity == Specificity.SUBCLASS && costInNeutralBounds(cost)) ) {
					entail = false;
					this.ruleUsed = 10;
				}
				if (matchSpecificity == Specificity.DISJOINT && costInContraBounds(cost) ) {
					disjoint = false;
					this.ruleUsed = 11;
				}
				else if (matchSpecificity == Specificity.DISJOINT && !costInContraBounds(cost) ) {
					entail = false;
					this.ruleUsed = 12;
				}
				// if there is neither equals nor subclass, there is no entail
				if (matchSpecificity != Specificity.EQUALS && matchSpecificity != Specificity.SUBCLASS && matchSpecificity != Specificity.DISJOINT  ) {
					entail = false;
					disjoint = false;
					this.ruleUsed = 13;
				} 
				if (matchSpecificity == Specificity.NONE){
					disjoint = false;
					entail = false;
					this.ruleUsed = 14;
				} 
				if (ruleUsed < 9 && matchSpecificity == Specificity.EQUALS ){
					this.ruleUsed = 15;
				} else if (ruleUsed < 9 && matchSpecificity == Specificity.SUBCLASS ){
					this.ruleUsed = 16;
				}
				
			}
			// both terms are uninstantiated
			else if (hPolarity == Polarity.ANTIVERIDICAL && tPolarity == Polarity.ANTIVERIDICAL) {
				if (matchSpecificity != Specificity.DISJOINT && !costInContraBounds(cost) ){
					disjoint = false;
				}
				// if there is equals relation but there is a contraFlag or neutralFlag, then there is no entail but contradiction
				if ( (matchSpecificity == Specificity.EQUALS && costInContraBounds(cost) )
						|| ( matchSpecificity == Specificity.EQUALS && costInNeutralBounds(cost) )){
					entail = false;
					this.ruleUsed = 17;
				}
				// if there is superclass relation but there is a contraFlag or neutralFlag, then there is no entail but contradiction
				if ( (matchSpecificity == Specificity.SUPERCLASS && costInContraBounds(cost) )
						|| ( matchSpecificity == Specificity.SUPERCLASS && costInNeutralBounds(cost) ) ){
					entail = false;
					this.ruleUsed = 18;
				}
				if (matchSpecificity == Specificity.DISJOINT && costInContraBounds(cost) ) {
					disjoint = false;
					this.ruleUsed = 19;
				}
				else if (matchSpecificity == Specificity.DISJOINT && !costInContraBounds(cost) ) {
					entail = false;
					this.ruleUsed = 20;
				}
				
				// if there is neither equals nor superclass, there is no entail
				if (matchSpecificity != Specificity.EQUALS && matchSpecificity != Specificity.SUPERCLASS && matchSpecificity != Specificity.DISJOINT  ){
					entail = false;		
					disjoint = false;
					this.ruleUsed = 21;
				}
				if (matchSpecificity == Specificity.NONE){
					disjoint = false;
					entail = false;
					this.ruleUsed = 22;
				} 
				if (ruleUsed < 9 && matchSpecificity == Specificity.EQUALS ){
					this.ruleUsed = 23;
				} else if (ruleUsed < 9 && matchSpecificity == Specificity.SUPERCLASS ){
					this.ruleUsed = 24;
				}
			} else{
				entail = false;
				disjoint = false;
				this.ruleUsed = 25;
			}
			// if at least one disjoint case was found, do not go to the next rootNode because it is certainly contradiction
			if (entail == false)
				break;
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
	
	// Define whether a cost is within the bounds for a contradiction, i.e. has a contra_flag
	private boolean costInContraBounds(double cost){
		if (cost >= 175.0)  //if (cost <= 225.0 && cost >= 175.0)
			return true;
		else
			return false;
	}
	
	// Define whether a cost is within the bounds for a neutral, i.e. has a neutral_flag
	private boolean costInNeutralBounds(double cost){
		if (cost <= 125.0 && cost >= 75.0)
			return true;
		else
			return false;
	}
	
	/**
	 * Calculate the penalty of a match based on its depth and distance penalty. 
	 * @param relation
	 * @param strict
	 * @param matchEdge
	 */
	private void penalizeLooseMatching(EntailmentRelation relation, boolean strict, MatchEdge matchEdge){
		this.matchStrength = 0;
		this.matchConfidence = 0;
		if (this.entailmentRelation == relation){
			if (strict == false)
				matchStrength = 1;
			matchStrength -= matchEdge.getFeatureScore("depth")-matchEdge.getFeatureScore("distance");
			matchConfidence = matchEdge.getFeatureScore("cost");
			justifications.add(matchEdge);
		}
	}
	
	/**
	 * Initialize the process by retrieving the hypothesis and premise contexts.
	 */
	private void initialize() {
		this.hypothesisContexts = getContextHeads(gnliGraph.getHypothesisGraph());
		this.textContexts = getContextHeads(gnliGraph.getTextGraph());
	}
	
	/**
	 * Get the veridicality of a node within a certain context. Return a hashmap with a semantic node
	 * as the key and a hashmap as the value; this hashmap contains the contexts as keys and the 
	 * instantiability of that node within these contexts as values.    
	 * @param semGraph
	 * @return
	 */
	private HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> getContextHeads(SemanticGraph semGraph){
		HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>> ctxHeadsWithVerid = new HashMap<SemanticNode<?>,HashMap<SemanticNode<?>,Polarity>>();
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
					}
					else if (semGraph.equals(gnliGraph.getTextGraph())){
						this.textRootCtx = motherCtx;				
					}
					hashOfCtxsAndVerid.put(motherCtx, Polarity.VERIDICAL);
				} 
				//
				else {
					if (semGraph.equals(gnliGraph.getHypothesisGraph())){
						this.hypRootCtx = inNeighbors.iterator().next();
					}
					else if (semGraph.equals(gnliGraph.getTextGraph())){
						this.textRootCtx = inNeighbors.iterator().next();
					}
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
		return ctxHeadsWithVerid;
	}
	
	/**
	 * Compute the veridicality/polarity of a node based on a sequence of embedded polarities/veridicalities.
	 * Note that this method does not efficiently deal with the relative polarity of
	 * factive/implicative verbs that have been embedded more than twice. For this computation,
	 * we need to take into account the exact signature of the factive/implicative, which is 
	 * not done currently.   
	 * @param shortestPath
	 * @return
	 * TODO: improve relative polarity computation
	 */
	private Polarity computeEdgePolarity(List<SemanticEdge> shortestPath){
		Polarity polar = null;
		for (SemanticEdge verEdge : shortestPath){
			if (verEdge.getLabel().equals("veridical") || verEdge.getLabel().equals("imperative")){
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
			else if  (verEdge.getLabel().equals("averidical") || verEdge.getLabel().equals("coord_or") || 
					verEdge.getLabel().equals("interrogative") || verEdge.getLabel().equals("may") ||
					verEdge.getLabel().equals("might") || verEdge.getLabel().equals("must") ||
					verEdge.getLabel().equals("ought") || verEdge.getLabel().equals("need") ||
					verEdge.getLabel().equals("should")){
				polar = Polarity.AVERIDICAL;
			}
			else {
				polar = Polarity.VERIDICAL;
			}
		}
		return polar;
	}
	
}
