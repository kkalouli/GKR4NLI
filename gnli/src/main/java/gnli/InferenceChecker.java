package gnli;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import sem.graph.SemanticEdge;
import sem.graph.SemanticGraph;
import sem.graph.SemanticNode;
import sem.graph.vetypes.SkolemNode;
import sem.graph.vetypes.SkolemNodeContent;
import sem.graph.vetypes.TermNode;



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
	private EntailmentRelation alternativeEntailmentRelation;
	
	enum Polarity {VERIDICAL, ANTIVERIDICAL, AVERIDICAL}

	
	public InferenceChecker(GNLIGraph gnliGraph, InferenceComputer infComputer, SpecificityUpdater updater, String labelToLearn, String pairID) {
		this.gnliGraph = gnliGraph;
		this.entailmentRelation = EntailmentRelation.UNKNOWN; 
		this.alternativeEntailmentRelation = EntailmentRelation.UNKNOWN; 
		looseContra = false;
		looseEntail = false;
		matchStrength = 0;
		matchConfidence = 0;
		justifications = new ArrayList<MatchEdge>();
		this.infComputer = infComputer;
		this.updater = updater;
		this.labelToLearn = labelToLearn;
		this.pairID = pairID;
	}
	
	
	public InferenceDecision getInferenceDecision() {	
		if (this.hypothesisContexts == null) {
			initialize();
		}
		if (this.entailmentRelation == EntailmentRelation.UNKNOWN) {
			computeInferenceRelation();
		}
		return new InferenceDecision(entailmentRelation,matchStrength, matchConfidence, alternativeEntailmentRelation, justifications, looseContra, looseEntail, gnliGraph);
	}
	
	
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
					if (inReach.getLabel().contains("be"))
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
			contradiction(hTerm, tTerm, matchEdge, hTermCtxs, tTermCtxs, true);
				//return;
			//}
			/*if (contradiction(hTerm, tTerm, matchEdge, hTermCtxs, tTermCtxs, false)) {
				looseContra = true;
			}*/
			
		}

		// comment out following lines in order to extractItemsSetsForAssociationRuleMining
		// in order to get all paths and their contexts (for training)
		if (!rootNodeMatches.isEmpty()){
			entailmentOrDisjoint(rootNodeMatches, true);
			//return;
		}
		/*if (entailment(rootNodeMatch, hypRootNode, false)) {
			looseEntail = true;
		}*/
		
		// include following lines in order to extract the paths and their contexts (for training and for testing)
		if (rootNodeMatches.isEmpty()){
			MatchEdge matchWithTheMostModifiers = updater.getMatchAgendaStable().get(updater.getMatchAgendaStable().size()-1).match;
			SemanticNode<?> hTerm = gnliGraph.getMatchGraph().getStartNode(updater.getMatchAgendaStable().get(updater.getMatchAgendaStable().size()-1).match);			
			rootNodeMatches.put(hTerm,matchWithTheMostModifiers);
		}
		try {
			// for training:
			//extractItemsSetsForAssociationRuleMining(rootNodeMatches);
			// for testing:
			String encodedPathAndCtx = extractAllPathsAndContext(rootNodeMatches);
			String relation = computeDecisionOfAssociationMining(encodedPathAndCtx);
			if (!relation.equals(""))
				this.alternativeEntailmentRelation = EntailmentRelation.valueOf(relation);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (looseContra == false && looseEntail == false){
			this.entailmentRelation = EntailmentRelation.NEUTRAL;
		}
		
	}
	
	
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
	
	
	private boolean contradiction(HashMap<SemanticNode<?>, MatchEdge> rootNodeMatches, boolean strict){
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
			if (matchSpecificity!= Specificity.NONE && matchSpecificity != Specificity.DISJOINT) {
				if (hPolarity == Polarity.ANTIVERIDICAL && tPolarity == Polarity.VERIDICAL) {
					if (matchSpecificity == Specificity.EQUALS || matchSpecificity == Specificity.SUBCLASS) {
						this.entailmentRelation = EntailmentRelation.CONTRADICTION;
						penalizeLooseMatching(EntailmentRelation.CONTRADICTION, strict, rootNodeMatches.get(key));
						return true;
					}
				} else if (hPolarity == Polarity.VERIDICAL && tPolarity == Polarity.ANTIVERIDICAL) {
					if (matchSpecificity == Specificity.EQUALS || matchSpecificity == Specificity.SUPERCLASS) {
						this.entailmentRelation = EntailmentRelation.CONTRADICTION;
						penalizeLooseMatching(EntailmentRelation.CONTRADICTION, strict, rootNodeMatches.get(key));
						return true;
					}
				}
			} 
		}
		return false;
	}
	
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
				List<SemanticEdge> hPath = just.getConclusionPath();
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
	
	private void extractItemsSetsForAssociationRuleMining(HashMap<SemanticNode<?>, MatchEdge> rootNodeMatches) throws IOException{
		String toWrite = pairID+"\t";
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
				List<SemanticEdge> hPath = just.getConclusionPath();
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
			double cost = rootNodeMatches.get(key).getFeatureScore("cost");
			if (hPolarity == Polarity.VERIDICAL && tPolarity == Polarity.VERIDICAL) {
				// it is not a contradiction if there is no disjoint relation and the score is below maxCost*2
				if (matchSpecificity != Specificity.DISJOINT && !costInContraBounds(cost)){
					disjoint = false;
				} 
				// if there is equals relation but the score is higher than maxCost*2, then there is no entail but contradiction
				if ( (matchSpecificity == Specificity.EQUALS && costInContraBounds(cost) )
						|| ( matchSpecificity == Specificity.EQUALS && costInNeutralBounds(cost) )){
					entail = false;
				}
				// if there is subclass relation but the score is higher than maxCost*2, then there is no entail but contradiction
				if ( (matchSpecificity == Specificity.SUBCLASS && costInContraBounds(cost) )
						|| ( matchSpecificity == Specificity.SUBCLASS && costInNeutralBounds(cost) ) ){
					entail = false;
				}
				// if there is neither equals nor subclass, there is no entail
				if (matchSpecificity != Specificity.EQUALS && matchSpecificity != Specificity.SUBCLASS) {
					entail = false;		
				} 
				if (matchSpecificity == Specificity.NONE){
					disjoint = false;
				} 
				
			}
			else if (hPolarity == Polarity.ANTIVERIDICAL && tPolarity == Polarity.ANTIVERIDICAL) {
				if (matchSpecificity != Specificity.DISJOINT && !costInContraBounds(cost)){
					disjoint = false;
				}
				// if there is equals relation but the score is higher than maxCost*2, then there is no entail but contradiction
				if ( (matchSpecificity == Specificity.EQUALS && costInContraBounds(cost) )
						|| ( matchSpecificity == Specificity.EQUALS && costInNeutralBounds(cost) )){
					entail = false;
				}
				// if there is superclass relation but the score is higher than maxCost*2, then there is no entail but contradiction
				if ( (matchSpecificity == Specificity.SUPERCLASS && costInContraBounds(cost) )
						|| ( matchSpecificity == Specificity.SUPERCLASS && costInNeutralBounds(cost) ) ){
					entail = false;
				}
				// if there is neither equals nor superclass, there is no entail
				if (matchSpecificity != Specificity.EQUALS && matchSpecificity != Specificity.SUPERCLASS) {
					entail = false;		
				}
				if (matchSpecificity == Specificity.NONE){
					disjoint = false;
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
	
	private boolean costInContraBounds(double cost){
		if (cost <= 225.0 && cost >= 175.0)
			return true;
		else
			return false;
	}
	
	private boolean costInNeutralBounds(double cost){
		if (cost <= 150.0 && cost >= 75.0)
			return true;
		else
			return false;
	}
	
	
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
	
	
	private void initialize() {
		this.hypothesisContexts = getContextHeads(gnliGraph.getHypothesisGraph());
		this.textContexts = getContextHeads(gnliGraph.getTextGraph());
	}
	
	
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
