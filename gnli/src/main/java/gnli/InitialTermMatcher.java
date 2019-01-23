package gnli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Set;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.EProver;
import com.articulate.sigma.Formula;
import com.articulate.sigma.KBcache;

import gnli.GNLIGraph;
import gnli.MatchContent;
import gnli.MatchEdge;
import gnli.MatchOrigin;
import gnli.Specificity;
import semantic.graph.SemanticEdge;
import semantic.graph.SemanticGraph;
import semantic.graph.SemanticNode;
import semantic.graph.vetypes.SenseNode;
import semantic.graph.vetypes.SenseNodeContent;
import semantic.graph.vetypes.SkolemNodeContent;
import semantic.graph.vetypes.TermNode;



/**
 * Performs initial term matching on an {@link EcdGraph}. 
 * Will add {@link MatchEdge}s to the graph
 *
 */
public class InitialTermMatcher {
	private GNLIGraph gnliGraph;
	private final List<CheckedTermNode> hypothesisTerms = Collections.synchronizedList(new ArrayList<CheckedTermNode>());
	private final List<TermNode> textTerms = Collections.synchronizedList(new ArrayList<TermNode>());
	private final List<TermNode> derivedTextTerms = Collections.synchronizedList(new ArrayList<TermNode>());
	private KB kb;
	

	enum Matched {
		YES, NO, PENDING
	}
	

	/**
	 * Keep track of the match status of node
	 *
	 */
	class CheckedTermNode {
		Matched matched;
		TermNode node;

		CheckedTermNode(TermNode node) {
			this.node = node;
			matched = Matched.NO;
		}

		boolean isMatched() {
			return matched == Matched.YES;
		}

		boolean isPending() {
			return matched == Matched.PENDING;
		}

		void updateMatch() {
			if (matched == Matched.PENDING) {
				matched = Matched.YES;
			}
		}

		void pendMatch() {
			matched = Matched.PENDING;
		}
	}

	

	/**
	 * Create an InitialTermMatcher for the gnliGraph
	 * 
	 * @param ecdGraph
	 */
	public InitialTermMatcher(GNLIGraph gnliGraph) {
		this.gnliGraph = gnliGraph;
		for (TermNode hTerm : gnliGraph.getHypothesisGraph().getSkolems()) {
			this.hypothesisTerms.add(new CheckedTermNode(hTerm));
		}
		for (TermNode tTerm : gnliGraph.getTextGraph().getSkolems()) {
			this.textTerms.add(tTerm);
		}
		
		KBmanager.getMgr().initializeOnce("/Users/kkalouli/Documents/.sigmakee/KBs");	
		this.kb = KBmanager.getMgr().getKB("SUMO");
	}
	

	/**
	 * Apply initial term matches
	 */
	public void process() {
		matchExplicitTerms();
		//gnliGraph.display();
		//matchDerivedTerms();
		matchCoreferences();
		
	}

	/**
	 * Perform matches on explicit terms, i.e. terms that have
	 * not been introduced by lexical / naive semantics
	 */
	public void matchExplicitTerms() {
		for (CheckedTermNode hTerm : hypothesisTerms) {
			for (TermNode tTerm : textTerms) {
				checkStemMatch(hTerm, tTerm);
			}
		}
		updatePendingMatches();
		for (CheckedTermNode hTerm : hypothesisTerms) {
			for (TermNode tTerm : textTerms) {
				checkSurfaceMatch(hTerm, tTerm);
			}
		}
		updatePendingMatches();
		for (CheckedTermNode hTerm : hypothesisTerms) {
			for (TermNode tTerm : textTerms) {
				checkSenseMatch(hTerm, tTerm);
			}
		}
		updatePendingMatches();
		for (CheckedTermNode hTerm : hypothesisTerms) {
			for (TermNode tTerm : textTerms) {
				checkConceptMatch(hTerm, tTerm);
			}
		}
		updatePendingMatches();
		for (CheckedTermNode hTerm : hypothesisTerms) {
			for (TermNode tTerm : textTerms) {
				checkEmbedMatch(hTerm, tTerm);
			}
		}
		updatePendingMatches();

		
	
	}


	/**
	 * Generate further matches on basis of previous ones
	 * and coreference links
	 */
	public void matchCoreferences() {
		addCoRefMatches();
	}

	private void updatePendingMatches() {
		for (CheckedTermNode cTerm : this.hypothesisTerms) {
			cTerm.updateMatch();
		}
	}

	/**
	 * Hypothesis and text terms have the same stem
	 * 
	 * @param chTerm
	 * @param tTerm
	 * @return
	 * 		A list of {@link MatchEdge}s (typically one or none)
	 */
	protected List<MatchEdge> checkStemMatch(CheckedTermNode cHTerm, TermNode tTerm) {
		List<MatchEdge> retval = new ArrayList<MatchEdge>();
		TermNode hTerm = cHTerm.node;
		String hStem = null;
		String tStem = null;
		if (hTerm.getContent().getClass().isAssignableFrom(SkolemNodeContent.class)) {
			hStem = ((SkolemNodeContent) hTerm.getContent()).getStem();
		}
		if (tTerm.getContent().getClass().isAssignableFrom(SkolemNodeContent.class)) {
			tStem = ((SkolemNodeContent) tTerm.getContent()).getStem();
		}
		if (hStem != null && !hStem.equals("_") && tStem != null && hStem.equals(tStem)) {
			final MatchContent linkContent = new MatchContent(MatchOrigin.MatchType.STEM);
			final MatchEdge stemMatch = new MatchEdge("stem", linkContent);
			gnliGraph.addMatchEdge(stemMatch, hTerm, tTerm);
			retval.add(stemMatch);
			cHTerm.pendMatch();
		}
		return retval;
	}

	/**
	 * Hypothesis and text terms have the same surface form (normally pre-empted
	 * by stem match)
	 * 
	 * @param chTerm
	 * @param tTerm
	 * @return
	 * 		A list of {@link MatchEdge}s (typically one or none)
	 */
	protected List<MatchEdge> checkSurfaceMatch(CheckedTermNode cHTerm,TermNode tTerm) {
		List<MatchEdge> retval = new ArrayList<MatchEdge>();
		if (cHTerm.isMatched()) {
			return retval;
		}
		TermNode hTerm = cHTerm.node;
		String hSurf = null;
		String tSurf = null;
		if (hTerm.getContent().getClass().isAssignableFrom(SkolemNodeContent.class)) {
			hSurf = ((SkolemNodeContent) hTerm.getContent()).getSurface();
		}
		if (tTerm.getContent().getClass().isAssignableFrom(SkolemNodeContent.class)) {
			tSurf = ((SkolemNodeContent) tTerm.getContent()).getSurface();
		}
		if (hSurf != null && tSurf != null && !hSurf.equals("_") && (hSurf.equals(tSurf) || stringEditDistance(hSurf,tSurf) > 0.85 )) {
			final MatchContent linkContent = new MatchContent(MatchOrigin.MatchType.SURFACE);
			final MatchEdge surfaceMatch = new MatchEdge("surface", linkContent);
			gnliGraph.addMatchEdge(surfaceMatch, hTerm, tTerm);
			retval.add(surfaceMatch);
			cHTerm.pendMatch();
		}
		return retval;
	}

	/**
	 * Check that hypothesis term has sense that matches that of derived/lexical
	 * text term
	 * 
	 * @param chTerm
	 * @param tTerm
	 * @return
	 * 		A list of {@link MatchEdge}s (typically one or none)
	 */
	protected List<MatchEdge> checkSenseMatch(CheckedTermNode cHTerm,TermNode tTerm) {
		List<MatchEdge> retval = new ArrayList<MatchEdge>();
		if (cHTerm.isMatched()) {
			return retval;
		}
		TermNode hTerm = cHTerm.node;
		for (final SenseNode tSenseNode : gnliGraph.getTextGraph().getSenses(tTerm)) {
			String tSenseId = ((SenseNodeContent) tSenseNode.getContent()).getSenseId();
			List<String> tSynonyms = ((SenseNodeContent) tSenseNode.getContent()).getSynonyms();
			List<String> tAntonyms = ((SenseNodeContent) tSenseNode.getContent()).getAntonyms();
			Map<String, Integer> tSuperConcepts = ((SenseNodeContent) tSenseNode.getContent()).getSuperConcepts();
			Map<String, Integer> tSubConcepts = ((SenseNodeContent) tSenseNode.getContent()).getSubConcepts();
			for (final SenseNode hSenseNode : gnliGraph.getHypothesisGraph().getSenses(hTerm)) {
				String hSenseId = ((SenseNodeContent) hSenseNode.getContent()).getSenseId();
				List<String> hSynonyms = ((SenseNodeContent) hSenseNode.getContent()).getSynonyms();
				List<String> hAntonyms = ((SenseNodeContent) hSenseNode.getContent()).getAntonyms();
				Map<String, Integer> hSuperConcepts = ((SenseNodeContent) hSenseNode.getContent()).getSuperConcepts();
				Map<String, Integer> hSubConcepts = ((SenseNodeContent) hSenseNode.getContent()).getSubConcepts();
				MatchContent linkContent = null;
				if (tSenseId != null && hSenseId != null){
					if (tSenseId.equals(hSenseId)) {
						linkContent = new MatchContent(MatchOrigin.MatchType.SENSE, hSenseId, tSenseId,null, Specificity.EQUALS, 0f);
					} else if (tSynonyms.contains(hSenseId) || hSynonyms.contains(tSenseId)){
						linkContent = new MatchContent(MatchOrigin.MatchType.SENSE, hSenseId, tSenseId,null, Specificity.EQUALS, 1f);
					} else if (tSuperConcepts.keySet().contains(hSenseId)){
						linkContent = new MatchContent(MatchOrigin.MatchType.SENSE, hSenseId, tSenseId,null, Specificity.SUPERCLASS, tSuperConcepts.get(hSenseId));
					} else if (hSuperConcepts.keySet().contains(tSenseId)){
						linkContent = new MatchContent(MatchOrigin.MatchType.SENSE, hSenseId, tSenseId,null, Specificity.SUBCLASS, hSuperConcepts.get(tSenseId));
					} else if (hSubConcepts.keySet().contains(tSenseId)){
						linkContent = new MatchContent(MatchOrigin.MatchType.SENSE, hSenseId, tSenseId,null, Specificity.SUPERCLASS, hSubConcepts.get(tSenseId));
					} else if (tSubConcepts.keySet().contains(hSenseId)){
						linkContent = new MatchContent(MatchOrigin.MatchType.SENSE, hSenseId, tSenseId,null, Specificity.SUBCLASS, tSubConcepts.get(hSenseId));
					} else if (hAntonyms.contains(tSenseId) || tAntonyms.contains(hSenseId) ){
						linkContent = new MatchContent(MatchOrigin.MatchType.SENSE, hSenseId, tSenseId,null, Specificity.DISJOINT, 0f);
					}
					if (linkContent != null){
						final MatchEdge senseMatch = new MatchEdge("sense",linkContent);
						gnliGraph.addMatchEdge(senseMatch, hTerm, tTerm);
						retval.add(senseMatch);
						cHTerm.pendMatch();
					}
				}
			}
		}
		return retval;
	}
	
	protected List<MatchEdge> checkConceptMatch(CheckedTermNode cHTerm,TermNode tTerm) {
		List<MatchEdge> retval = new ArrayList<MatchEdge>();
		if (cHTerm.isMatched()) {
			return retval;
		}
		TermNode hTerm = cHTerm.node;
		for (final SenseNode tSenseNode : gnliGraph.getTextGraph().getSenses(tTerm)) {
			String tConcept = ((SenseNodeContent) tSenseNode.getContent()).getConcepts().get(0);
			for (final SenseNode hSenseNode : gnliGraph.getHypothesisGraph().getSenses(hTerm)) {
				String hConcept = ((SenseNodeContent) hSenseNode.getContent()).getConcepts().get(0);
				if (tConcept != null && hConcept != null && tConcept != "" && hConcept != ""){
					if (tConcept.equals(hConcept)) {
						final MatchContent linkContent = new MatchContent(MatchOrigin.MatchType.CONCEPT, ((SenseNodeContent) hSenseNode.getContent()).getSenseId(), ((SenseNodeContent) tSenseNode.getContent()).getSenseId(), tConcept, Specificity.EQUALS, 0f);
						final MatchEdge conceptMatch = new MatchEdge("concept",linkContent);
						gnliGraph.addMatchEdge(conceptMatch, hTerm, tTerm);
						retval.add(conceptMatch);
						cHTerm.pendMatch();
					} else {
						ArrayList<Formula> listOfRelations = kb.askWithRestriction(2, tConcept.substring(0,tConcept.length()-1), 1, hConcept.substring(0,hConcept.length()-1));
						listOfRelations.addAll(kb.askWithRestriction(2, hConcept.substring(0,hConcept.length()-1), 1, tConcept.substring(0,tConcept.length()-1)));
						for (Formula f :  listOfRelations){
							String firstArg = f.getArgument(1);
							String secondArg = f.getArgument(2);
							Specificity spec = null;
							for (String rel : f.gatherRelationConstants()){
								if (rel.equals("subclass") || rel.equals("instance")){
									if (hConcept.contains(firstArg) && tConcept.contains(secondArg))
										spec = Specificity.SUPERCLASS;
									else if (tConcept.contains(firstArg) && hConcept.contains(secondArg))
										spec = Specificity.SUBCLASS;
								} else if (rel.equals("partition") && spec == null){
									if (hConcept.contains(firstArg) && tConcept.contains(secondArg))
										spec = Specificity.SUBCLASS;
									else if (tConcept.contains(firstArg) && hConcept.contains(firstArg))
										spec = Specificity.SUPERCLASS;
								}
							}
							if (spec != null){
								final MatchContent linkContent = new MatchContent(MatchOrigin.MatchType.CONCEPT, ((SenseNodeContent) hSenseNode.getContent()).getSenseId(), ((SenseNodeContent) tSenseNode.getContent()).getSenseId(), tConcept, spec, 0f);
								final MatchEdge conceptMatch = new MatchEdge("concept",linkContent);
								gnliGraph.addMatchEdge(conceptMatch, hTerm, tTerm);
								retval.add(conceptMatch);
								cHTerm.pendMatch();
							}
						}
					}
				}
			}
		}
		return retval;
	}
	
	protected List<MatchEdge> checkEmbedMatch(CheckedTermNode cHTerm,TermNode tTerm) {
		List<MatchEdge> retval = new ArrayList<MatchEdge>();
		if (cHTerm.isMatched()) {
			return retval;
		}
		TermNode hTerm = cHTerm.node;
		for (final SenseNode tSenseNode : gnliGraph.getTextGraph().getSenses(tTerm)) {
			double[] tEmbed =  ((SenseNodeContent) tSenseNode.getContent()).getEmbed();
			for (final SenseNode hSenseNode : gnliGraph.getHypothesisGraph().getSenses(hTerm)) {
				double[] hEmbed = ((SenseNodeContent) hSenseNode.getContent()).getEmbed();
				if (tEmbed != null && hEmbed != null && computeCosineSimilarity(hEmbed, tEmbed) > 0.7) {
					final MatchContent linkContent = new MatchContent(MatchOrigin.MatchType.EMBED, Specificity.EQUALS, 3f);
					final MatchEdge conceptMatch = new MatchEdge("embed",linkContent);
					gnliGraph.addMatchEdge(conceptMatch, hTerm, tTerm);
					retval.add(conceptMatch);
					cHTerm.pendMatch();
				}
			}
		}
		return retval;
	}
	
	private double computeCosineSimilarity(double[] vectorA, double[] vectorB) {
	    double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    double cosSimil = 0.0;
	    for (int i = 0; i < vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    } 
	    cosSimil = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	    return cosSimil;
	}

			

	/**
	 * Expand out any initial term matches to apply to other members of
	 * coreference chains
	 */
	private void addCoRefMatches() {
		for (MatchEdge matchEdge : gnliGraph.getMatches()){
			SemanticNode<?> hTerm = gnliGraph.getStartNode(matchEdge);
			SemanticNode<?> tTerm = gnliGraph.getFinishNode(matchEdge);
			for (SemanticEdge linkEdge : gnliGraph.getHypothesisGraph().getLinks(hTerm)){
				SemanticNode<?> finish = gnliGraph.getHypothesisGraph().getFinishNode(linkEdge);
				MatchEdge corefMatch = new MatchEdge(matchEdge);
				corefMatch.setLabel("coref+" + matchEdge.getLabel());
				gnliGraph.addMatchEdge(corefMatch, finish, tTerm);
			}
			for (SemanticEdge linkEdge : gnliGraph.getTextGraph().getLinks(tTerm)){
				SemanticNode<?> finish = gnliGraph.getTextGraph().getFinishNode(linkEdge);
				MatchEdge corefMatch = new MatchEdge(matchEdge);
				corefMatch.setLabel("coref+" + matchEdge.getLabel());
				gnliGraph.addMatchEdge(corefMatch, hTerm, finish);
			}
		}
		
			
				
	}
	
	public float stringEditDistance (CharSequence tSeq, CharSequence hSeq) {                          
	    int tLen = tSeq.length() + 1;                                                     
	    int hLen = hSeq.length() + 1;                                                     
	                                                                                    
	    // the array of distances                                                       
	    int[] cost = new int[tLen];                                                     
	    int[] newcost = new int[tLen];                                                  
	                                                                                    
	    // initial cost of skipping prefix in String s0                                 
	    for (int i = 0; i < tLen; i++) cost[i] = i;                                     
	                                                                                    
	    // dynamically computing the array of distances                                  
	                                                                                    
	    // transformation cost for each letter in s1                                    
	    for (int j = 1; j < hLen; j++) {                                                
	        // initial cost of skipping prefix in String s1                             
	        newcost[0] = j;                                                             
	                                                                                    
	        // transformation cost for each letter in s0                                
	        for(int i = 1; i < tLen; i++) {                                             
	            // matching current letters in both strings                             
	            int match = (tSeq.charAt(i - 1) == hSeq.charAt(j - 1)) ? 0 : 1;             
	                                                                                    
	            // computing cost for each transformation                               
	            int cost_replace = cost[i - 1] + match;                                 
	            int cost_insert  = cost[i] + 1;                                         
	            int cost_delete  = newcost[i - 1] + 1;                                  
	                                                                                    
	            // keep minimum cost                                                    
	            newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
	        }                                                                           
	                                                                                    
	        // swap cost/newcost arrays                                                 
	        int[] swap = cost; cost = newcost; newcost = swap;                          
	    }                                                                               
	         
	 // the distance is the cost for transforming all letters in both strings        
	 int levDist = cost[tLen - 1];
	 // implement MacCartney's algorithm 
	 int maxSurface = Math.max(tSeq.length(),hSeq.length());
	 int penalty = 2;
	 float dividend  = maxSurface - penalty;
	 float result = levDist / dividend; 
	 float func = 1 - result;	 
	 float stringSim = Math.max(0, func);
	 return stringSim;
	 		                                                        
	}

}