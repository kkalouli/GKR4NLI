package gnli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;
import static java.util.Map.Entry.*;
import semantic.graph.SemanticEdge;
import semantic.graph.SemanticNode;
import semantic.graph.SemanticGraph;
import semantic.graph.vetypes.GraphLabels;
import semantic.graph.vetypes.SkolemNode;
import semantic.graph.vetypes.TermNode;



public class SpecificityUpdater {
	// matchAgenda orders matches: we want to update specificities starting from 
		// lower nodes in the graphs (fewest modifiers) and working up
		private List<MatchAgendaItem> matchAgenda;
		private List<MatchAgendaItem> newAgenda;
		private GNLIGraph gnliGraph;
		private PathScorer pathScorer;
		private int initialAgendaSize;
		private static int MAX_AGENDA_SIZE = 1000;
		
		public SpecificityUpdater(GNLIGraph gnliGraph, PathScorer pathScorer) {
			this.gnliGraph = gnliGraph;
			this.pathScorer = pathScorer;
			this.pathScorer.setGNLIGraph(gnliGraph);
			this.matchAgenda = new ArrayList<MatchAgendaItem>();
			// Set up match agenda and filter:
			for (MatchEdge match: gnliGraph.getMatches()) {
					MatchAgendaItem item = new MatchAgendaItem();
					item.match = match;
					item.noOfMods = getNoOfHypAndTextMods(match);
					this.matchAgenda.add(item);	
			}
			Collections.sort(this.matchAgenda);
			initialAgendaSize = matchAgenda.size();
		}

		


		/**
		 * Updates specificity relations on matches until no more updates can be made
		 */
		public void updateSpecifity() {
			while (update());
			gnliGraph.getMatchGraph().display();
		}
		

		private boolean update() {
			int initialSize = this.matchAgenda.size();
			if (initialSize == 0 || initialSize > MAX_AGENDA_SIZE) {
				// No more matches on agenda
				return false;
			}
			// Initialize new agenda for next rounds:
			this.newAgenda = new ArrayList<MatchAgendaItem>(initialSize);
			
			// Take each item of agenda in turn.
			// Agenda is ordered to have matches with fewest text and hypothesis modifiers first.
			// This increases the chances of the early matches being updated to completion, and
			// minimizes the number of full rounds taken
			for (MatchAgendaItem item : this.matchAgenda) {
				boolean updateComplete = updateMatch(item.match);
				if (!updateComplete) {
					newAgenda.add(item);
				}
			}
			// update and sort the agenda for the next round
			this.matchAgenda = newAgenda;
			Collections.sort(this.matchAgenda);
			return initialSize != this.matchAgenda.size();
		}
		
		

		private boolean updateMatch(MatchEdge match) {
			boolean updateComplete = false;
			HypoTextMatch hypoTextMatch = new HypoTextMatch(match);
			if (doesNotNeedUpdating(hypoTextMatch)){
				match.setComplete(true);
				updateComplete = true;
			} else if (hypOrTextHasMods(hypoTextMatch)){
				finalizeMatch(match);
				match.setComplete(true);
				updateComplete = true;
			} else if (hypAndTextHaveMods(hypoTextMatch)){
				finalizeMatch(match);
				match.setComplete(true);
				updateComplete = true;
			}
			return updateComplete;
		}
		
		private boolean doesNotNeedUpdating(HypoTextMatch hypoTextMatch){
			 // If it's a coreference link, it's always going to stay identical
			if (hypoTextMatch.match.getSpecificity() == Specificity.NONE || hypoTextMatch.match.getSpecificity() == Specificity.DISJOINT 
					|| hypoTextMatch.match.getLabel().startsWith("coref") || hypoTextMatch.match.isComplete() ||
					(hypoTextMatch.hypothesisModifiers.isEmpty() && hypoTextMatch.textModifiers.isEmpty()) ){
				return true;
			} else
				return false;
		}
		
		private boolean hypOrTextHasMods(HypoTextMatch hypoTextMatch){
			MatchEdge m = hypoTextMatch.match;
			if (hypoTextMatch.textModifiers.isEmpty()){
				// H more specific
				m.setSpecificity(switchSpecificity(m.getSpecificity(),Specificity.SUPERCLASS));
				m.addJustification(justificationOfSpecificityUpdateNoText(hypoTextMatch.hypothesisTerm, hypoTextMatch.hypothesisModifiers.get(0)));
				return true;
			} else if (hypoTextMatch.hypothesisModifiers.isEmpty()){
				// T more specific
				m.setSpecificity(switchSpecificity(m.getSpecificity(),Specificity.SUBCLASS));
				m.addJustification(justificationOfSpecificityUpdateNoHypothesis(hypoTextMatch.textTerm, hypoTextMatch.textModifiers.get(0)));
				return true;
			} else 
				return false;
		}
		
		private boolean hypAndTextHaveMods(HypoTextMatch hypoTextMatch){			
			List<SkolemNode> unprocessedTextRestr = hypoTextMatch.textModifiers;
			boolean complete = true;
			for (SkolemNode hRestr : hypoTextMatch.hypothesisModifiers){
				 List<MatchEdge> tOutMatches = gnliGraph.getOutMatches(hRestr);
				 for (MatchEdge m : tOutMatches){
					 if (unprocessedTextRestr.contains(gnliGraph.getMatchGraph().getFinishNode(m)))
						 unprocessedTextRestr.remove(gnliGraph.getMatchGraph().getFinishNode(m));
				 }
				switch (tOutMatches.size()){
				 case 0:
					updateWithUnmatchedModifier(hypoTextMatch);
					break;
				case 1:
					complete = tOutMatches.get(0).isComplete();
					if (complete) {
						updateWithOneMatchedModifier(hypoTextMatch, tOutMatches.get(0));
					} else {
					// otherwise, wait until the tOutMatches is completed before updating with it
						complete = false;
					}
					break;
				default:
					// More than one possible modifier match for the cmod.
					complete = updateWithMultipleMatchedModifiers(hypoTextMatch, tOutMatches);
					break;
				}
				if (doesNotNeedUpdating(hypoTextMatch)) {
					// match specificity has been made NONE or DISJOINT: further modifiers
					// will not change this, so stop now
					complete = true;
				}
			}
			if (!unprocessedTextRestr.isEmpty()){
				for (SkolemNode n : unprocessedTextRestr){
					hypoTextMatch.match.setSpecificity(switchSpecificity(hypoTextMatch.match.getSpecificity(),Specificity.SUBCLASS));
					hypoTextMatch.match.addJustification(justificationOfSpecificityUpdateNoHypothesis(hypoTextMatch.textTerm, n));
				}
			}
			return complete;
		}
		
		// hypothesis modifier has no corresponding match in the text graph
		private void updateWithUnmatchedModifier(HypoTextMatch hypoTextMatch){
			hypoTextMatch.match.setSpecificity(switchSpecificity(hypoTextMatch.match.getSpecificity(),Specificity.SUPERCLASS));
			hypoTextMatch.match.addJustification(justificationOfSpecificityUpdateNoText(hypoTextMatch.hypothesisTerm, hypoTextMatch.hypothesisModifiers.get(0)));
		}
		
		// // hypothesis modifier has exactly one corresponding match in the text graph: 
		// the match can be to a text modifier which is not dominated from the text match term
		// or the match can be to a text modifier which is dominated by the text match term
		private void updateWithOneMatchedModifier(HypoTextMatch hypoTextMatch, MatchEdge outEdge){
			SkolemNode tRestr = (SkolemNode) gnliGraph.getFinishNode(outEdge);
			SemanticNode<?> hMod = gnliGraph.getStartNode(outEdge);
			if (!gnliGraph.getTextGraph().getDependencyGraph().getOutReach(hypoTextMatch.textTerm).contains(tRestr)){
				// H more specific
				hypoTextMatch.match.setSpecificity(switchSpecificity(hypoTextMatch.match.getSpecificity(),Specificity.SUPERCLASS));
				hypoTextMatch.match.addJustification(justificationOfSpecificityUpdateNoText(hypoTextMatch.hypothesisTerm, (SkolemNode) hMod));
			} else{
				Specificity spec = outEdge.getSpecificity();
				HeadModifierPathPair justification = justificationOfSpecificityUpdate(hypoTextMatch.hypothesisTerm, hMod,
						hypoTextMatch.textTerm, tRestr, hypoTextMatch.match, outEdge);
				if (justification != null){
					hypoTextMatch.match.setSpecificity(switchSpecificity(hypoTextMatch.match.getSpecificity(),spec));
					hypoTextMatch.match.addJustification(justification);
				} else{
					gnliGraph.removeMatchEdge(hypoTextMatch.match);
					matchAgenda.remove(hypoTextMatch);
				}
			}
		}
		
		private boolean updateWithMultipleMatchedModifiers(HypoTextMatch hypoTextMatch, List<MatchEdge> tOutMatches){
			boolean complete = true;
			MatchEdge match = hypoTextMatch.match;
			List<HypoTextMatch> newMatches = new ArrayList<HypoTextMatch>();
			for (MatchEdge edg : tOutMatches){
				// make a new copy of the match and update it
				HypoTextMatch newMatch = new HypoTextMatch(match);
				updateWithOneMatchedModifier(newMatch, edg);
				newMatches.add(newMatch);
			}
			
			keepBestMatch(match, newMatches);
			return complete;
		}
		
		// set the content of the main match to the content of the best match (the one with the lowest cost)
		// newMatches are the corresponding matches with modifier terms of the main match
		private void keepBestMatch(MatchEdge match, List<HypoTextMatch> newMatches) {
			float maxCost = 20;
			HypoTextMatch bestMatch = null;
			for (HypoTextMatch newMatch : newMatches){
				if (newMatch.match.getJustification() != null){
					float cost = ((HeadModifierPathPair) newMatch.match.getJustification()).getCost();
					if (cost <= maxCost){
						maxCost = cost;
						bestMatch = newMatch;
					}
				}
			}
			match.setContent(bestMatch.match.getContent());
		}

		// Given that oldSpec is one of the best we've seen, should we keep newSpec
		private boolean keepSpecificity(Specificity newSpec, Specificity oldSpec) {
			switch (newSpec) {
			case DISJOINT:
				return oldSpec != Specificity.EQUALS;
			case EQUALS:
				return true;
			case NONE:
				return false;
			case SUBCLASS:
				return oldSpec != Specificity.EQUALS;
			case SUPERCLASS:
				return oldSpec != Specificity.EQUALS;
			default:
				return false;		
			}
		}

		
		
		private void finalizeMatch(MatchEdge match) {
			applyRestrictions(match);
			applyProperties(match);
		}
		
		
		private void applyRestrictions(MatchEdge match){
			SemanticNode<?> hypTerm = gnliGraph.getMatchGraph().getStartNode(match);
			SemanticNode<?> textTerm = gnliGraph.getMatchGraph().getFinishNode(match);
			List<TermNode> hypTermRestr = gnliGraph.getHypothesisGraph().getRestrictions((TermNode) hypTerm);
			List<TermNode> textTermRestr = gnliGraph.getTextGraph().getRestrictions((TermNode) textTerm);
			
			List<TermNode> unmatchedTextRestrs = textTermRestr;
			
			if (hypTermRestr.isEmpty() && textTermRestr.isEmpty())
				return;
			
			for (SemanticNode<?> hRstr : hypTermRestr){
				boolean matchFound = false;
				for (MatchEdge outM : gnliGraph.getOutMatches(hRstr)){
					SemanticNode<?> tRstr = gnliGraph.getMatchGraph().getFinishNode(outM);
					// if there is a matching restriction Tr, either as a regular concept restriction on T or as a relative clause restriction
					if (textTermRestr.contains(tRstr) || gnliGraph.getTextGraph().getDependencyGraph().getOutReach(textTerm).contains(tRstr)){
						match.setSpecificity(switchSpecificity(match.getSpecificity(), outM.getSpecificity()));
						match.addJustification(justificationOfSpecificityUpdate(hypTerm,hRstr,textTerm,tRstr,match,outM));
						unmatchedTextRestrs.remove(tRstr);
						matchFound = true;
					}
				}
				if (matchFound == false){
					match.setSpecificity(switchSpecificity(match.getSpecificity(), Specificity.SUPERCLASS));
					match.addJustification(justificationOfSpecificityUpdateNoText(hypTerm,hRstr));
				}
			}
			
			// check for any left text restr and do the same as above
			for (TermNode tRstr : unmatchedTextRestrs){
				boolean matchFound = false;
				for (MatchEdge inM : gnliGraph.getInMatches(tRstr)){
					SemanticNode<?> hRstr = gnliGraph.getMatchGraph().getStartNode(inM);
					if (gnliGraph.getHypothesisGraph().getRoleGraph().getOutReach(hypTerm).contains(hRstr)){
						match.setSpecificity(switchSpecificity(match.getSpecificity(), inM.getSpecificity()));
						match.addJustification(justificationOfSpecificityUpdate(hypTerm,hRstr,textTerm,tRstr,match,inM));
						matchFound = true;
					}
				}
				if (matchFound == false){
					match.setSpecificity(switchSpecificity(match.getSpecificity(), Specificity.SUBCLASS));
					match.addJustification(justificationOfSpecificityUpdateNoHypothesis(textTerm,tRstr));
				}
			}
		}
		
		private void applyProperties(MatchEdge match) {
			// get hyp and text terms of the match
			TermNode hypTerm = (TermNode) gnliGraph.getMatchGraph().getStartNode(match);
			TermNode textTerm = (TermNode) gnliGraph.getMatchGraph().getFinishNode(match);
			ArrayList<String> cardSpecHypTerm = getProperties(hypTerm, gnliGraph.getHypothesisGraph());
			ArrayList<String> cardSpecTextTerm = getProperties(textTerm, gnliGraph.getTextGraph());
			
			String hSpecifier = cardSpecHypTerm.get(1);
			if (hSpecifier == null) {
				return;
			}
			String hCardinality = cardSpecHypTerm.get(0);
			
			
			String tSpecifier = cardSpecTextTerm.get(1);
			if (tSpecifier == null) {
				return;
			}
			String tCardinality = cardSpecTextTerm.get(0);
			
			Specificity newSpecificity = DeterminerEntailments.newDeterminerSpecificity(hSpecifier, hCardinality, tSpecifier, tCardinality, match.getSpecificity());
			match.setSpecificity(newSpecificity);
		}
		
		
		private ArrayList<String> getProperties(TermNode term, SemanticGraph graph) {
			String cardinality = "";
			String specifier = "";
			ArrayList<String> cardSpec = new ArrayList<String>();
			for (SemanticEdge edge : graph.getPropertyGraph().getOutEdges(term)) {
				if (edge.getLabel().equals(GraphLabels.CARDINALITY)) {
					cardinality = gnliGraph.getFinishNode(edge).getLabel();
				} else if (edge.getLabel().equals(GraphLabels.SPECIFIER)) {
					specifier = gnliGraph.getFinishNode(edge).getLabel();
				}
			}
			if (cardinality != null) {
				if (specifier == null) {
					specifier = "bare";
				}
				cardSpec.add(cardinality);
				cardSpec.add(specifier);
			}
			return cardSpec;

		}
		
		
		
		/**
		 * Given a conclusionTerm and its modifier, conclusionMod, and a premiseTerm matched with conclusionTerm, and another
		 * premise term (premiseMod) that is matched with conclusionMod, look for an acceptable path linking premiseTerm and premiseMod.
		 * Or vice versa
		 * @param conclusionTerm 
		 * 	A conclusion term that is matched with premiseTerm
		 * @param conclusionMod
		 * 	Either a direct modifier of conclusionTerm, or a term matched with the premiseMod
		 * @param premiseTerm
		 *  A premise term that is matched with conclusionTerm
		 * @param premiseMod
		 * 	Either a direct modifier of premiseTerm, or a term matched with conclusionMod
		 * @param modlink
		 * 	The match between conclusionMod and premiseMod
		 * @return
		 *  The paired head-modifier paths that justify a modifier specificity update, or null if it is unjustifiable
		 */
		private HeadModifierPathPair justificationOfSpecificityUpdate(SemanticNode<?> hypothesisTerm, SemanticNode<?> hypothesisMod,
				SemanticNode<?> textTerm, SemanticNode<?> textMod, MatchEdge hTTermsMatch, MatchEdge hTModifiersMatch) {
			HeadModifierPathPair mcp = new HeadModifierPathPair();
			mcp.setBasePair(hTTermsMatch);
			mcp.setModifiersPair(hTModifiersMatch);
			mcp.setConclusionPath(findModPath(hypothesisTerm, hypothesisMod, gnliGraph.getHypothesisGraph()));
			mcp.setPremisePath(findModPath(textTerm, textMod, gnliGraph.getTextGraph()));
			mcp.setCost(this.pathScorer.pathCost(mcp));
			
			if (this.pathScorer.pathBelowThreshold(mcp.getCost())) {
				return mcp;
			} else {
				return null;
			}
		}
		
		
		private HeadModifierPathPair justificationOfSpecificityUpdateNoHypothesis(SemanticNode<?> textTerm, SemanticNode<?> textMod) {
			HeadModifierPathPair mcp = new HeadModifierPathPair();
			mcp.setBasePair(null);
			mcp.setConclusionPath(null);
			mcp.setPremisePath(findModPath(textTerm, textMod, gnliGraph.getTextGraph()));
			mcp.setCost(this.pathScorer.pathCost(mcp));
			return mcp;
		}

		
		private HeadModifierPathPair justificationOfSpecificityUpdateNoText(SemanticNode<?> hypothesisTerm, SemanticNode<?> hypothesisMod) {
			HeadModifierPathPair mcp = new HeadModifierPathPair();
			mcp.setBasePair(null);
			mcp.setConclusionPath(findModPath(hypothesisTerm, hypothesisMod, gnliGraph.getHypothesisGraph()));
			mcp.setPremisePath(null);
			mcp.setCost(this.pathScorer.pathCost(mcp));
			return mcp;
		}
		
		
		// Find the shortest directed path form start to finish node.
		// Failing that, find the shortest undirected path
		private List<SemanticEdge> findModPath(SemanticNode<?> startNode, SemanticNode<?> finishNode, SemanticGraph graph) {
			if (!graph.getGraph().containsNode(startNode) || !graph.getGraph().containsNode(finishNode)) {
				return new ArrayList<SemanticEdge>(0);
			}
			List<SemanticEdge> retval = graph.getShortestPath(startNode, finishNode);
			if (retval == null || retval.isEmpty()) {
				retval = graph.getShortestUndirectedPath(startNode, finishNode);
			}
			return retval;
		}
		
		
		private Specificity switchSpecificity(Specificity current, Specificity specificityFactor) {
			Specificity updated = current;
			switch (current) {
			case EQUALS:
				updated = specificityFactor;
				break;
			case NONE:
				updated = Specificity.NONE;
				break;
			case SUBCLASS:
				switch (specificityFactor) {
				case EQUALS:
					updated = current;
					break;
				case NONE:
					updated = Specificity.NONE;
					break;
				case SUBCLASS:
					updated = Specificity.SUBCLASS;
					break;
				case SUPERCLASS:
					updated = Specificity.NONE;
					break;
				case DISJOINT:
					updated = Specificity.DISJOINT;
					break;
				default:
					break;
				}
				break;
			case SUPERCLASS:
				switch (specificityFactor) {
				case EQUALS:
					updated = current;
					break;
				case NONE:
					updated = Specificity.NONE;
					break;
				case SUBCLASS:
					updated = Specificity.NONE;
					break;
				case SUPERCLASS:
					updated = Specificity.SUPERCLASS;
					break;
				case DISJOINT:
					updated = Specificity.DISJOINT;
					break;
				default:
					break;
				}
				break;
			case DISJOINT:
				switch (specificityFactor) {
				case EQUALS:
					updated = current;
					break;
				case NONE:
					updated = Specificity.NONE;
					break;
				case SUBCLASS:
					updated = Specificity.DISJOINT;
					break;
				case SUPERCLASS:
					updated = Specificity.DISJOINT;
					break;
				case DISJOINT:
					updated = Specificity.DISJOINT;
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
			return updated;
		}
		
		
		
		class MatchAgendaItem implements Comparable<MatchAgendaItem>{
			int noOfMods;
			MatchEdge match;
			
			@Override
			public int compareTo(MatchAgendaItem o) {
				return this.noOfMods - o.noOfMods;
			}
		}
		private int getNoOfHypAndTextMods(SemanticEdge match) {
			// Agenda is sorted to prioritize matches for terms with the fewest modifiers
			int hMods = gnliGraph.getAllHModifiers(match).size();
			int tMods = gnliGraph.getAllTModifiers(match).size();
			return hMods + tMods;
		}
		
		class HypoTextMatch {
			MatchEdge match;
			SkolemNode textTerm;
			SkolemNode hypothesisTerm;
			List<SkolemNode> textModifiers;
			List<SkolemNode> hypothesisModifiers;
			
			HypoTextMatch(MatchEdge match) {
				this.match = match;
				hypothesisTerm = (SkolemNode) gnliGraph.getMatchedHypothesisTerm(match);
				textTerm = (SkolemNode) gnliGraph.getMatchedTextTerm(match);
				hypothesisModifiers = gnliGraph.getAllHModifiers(match);
				textModifiers = gnliGraph.getAllTModifiers(match);
				sortByNumberOfTextMatches(hypothesisModifiers);
				sortByNumberOfHypothesisMatches(textModifiers);
				
				if (match.getLabel().contains("sense_cmp")){
					ArrayList<SkolemNode> toRemove = new ArrayList<SkolemNode>();
					for (SkolemNode mod : hypothesisModifiers){
						for (SemanticEdge edge : gnliGraph.getHypothesisGraph().getInEdges(mod)){
							if (edge.getLabel().equals("cmp"))
								toRemove.add(mod);
						}
					}
					hypothesisModifiers.removeAll(toRemove);
					toRemove.clear();
					for (SkolemNode mod : textModifiers){
						for (SemanticEdge edge : gnliGraph.getTextGraph().getInEdges(mod)){
							if (edge.getLabel().equals("cmp"))
								toRemove.add(mod);
						}
					}
					textModifiers.removeAll(toRemove);
				}
				
			}

			// Sort a collection of (hypothesis) skolems by order of which
			// have fewest text term matches:
			private void sortByNumberOfTextMatches(List<SkolemNode> hypoMods) {
				HashMap<SkolemNode,Integer> sortedHypoMods = new HashMap<SkolemNode,Integer>();
				for (SkolemNode mod : hypoMods){
					int noOFOutMatchEdges = gnliGraph.getOutMatches(mod).size();
					sortedHypoMods.put(mod, noOFOutMatchEdges);
				}
				hypoMods.clear();
				Map<Object, Object> sorted = sortedHypoMods.entrySet().stream().sorted(comparingByValue()).collect(toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,LinkedHashMap::new));
				for (Object key : sorted.keySet()){
					hypoMods.add((SkolemNode) key);
				}			
			}
			
			// Sort a collection of (hypothesis) skolems by order of which
			// have fewest text term matches:
			private void sortByNumberOfHypothesisMatches(List<SkolemNode> texMods) {
				HashMap<SkolemNode,Integer> sortedTexMods = new HashMap<SkolemNode,Integer>();
				for (SkolemNode mod : texMods){
					int noOFInMatchEdges = gnliGraph.getInMatches(mod).size();
					sortedTexMods.put(mod, noOFInMatchEdges);
				}
				texMods.clear();
				Map<Object, Object> sorted = sortedTexMods.entrySet().stream().sorted(comparingByValue()).collect(toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,LinkedHashMap::new));
				for (Object key : sorted.keySet()){
					texMods.add((SkolemNode) key);
				}			
			}
		}
		
		
		
		
}
