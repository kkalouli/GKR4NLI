package gnli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.articulate.sigma.KB;
import com.articulate.sigma.KButilities;
import com.articulate.sigma.Formula;
import gnli.GNLIGraph;
import gnli.MatchContent;
import gnli.MatchEdge;
import gnli.MatchOrigin;
import gnli.Specificity;
import sem.graph.SemanticEdge;
import sem.graph.SemanticNode;
import sem.graph.vetypes.SenseNode;
import sem.graph.vetypes.SenseNodeContent;
import sem.graph.vetypes.SkolemNode;
import sem.graph.vetypes.SkolemNodeContent;
import sem.graph.vetypes.TermNode;



/**
 * Performs initial term matching on an {@link EcdGraph}. 
 * Will add {@link MatchEdge}s to the graph
 *
 */
public class InitialTermMatcher {
	private GNLIGraph gnliGraph;
	private final List<CheckedTermNode> hypothesisTerms = Collections.synchronizedList(new ArrayList<CheckedTermNode>());
	private final List<TermNode> textTerms = Collections.synchronizedList(new ArrayList<TermNode>());
	private KB kb;
	private double highestCosSimil;
	private ArrayList<String> POSToExclude;

	

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
	public InitialTermMatcher(GNLIGraph gnliGraph, KB kb) {
		this.gnliGraph = gnliGraph;
		for (TermNode hTerm : gnliGraph.getHypothesisGraph().getSkolems()) {
			if (gnliGraph.getHypothesisGraph().isLexCoRef(hTerm) == false) // && gnliGraph.getHypothesisGraph().isRstr(hTerm) == false)
				this.hypothesisTerms.add(new CheckedTermNode(hTerm));
		}
		for (TermNode hTerm : gnliGraph.getHypothesisGraph().getDerivedSkolems()) {
			if (gnliGraph.getHypothesisGraph().isLexCoRef(hTerm) == false) // && gnliGraph.getHypothesisGraph().isRstr(hTerm) == false)
				this.hypothesisTerms.add(new CheckedTermNode(hTerm));
		}
		for (TermNode tTerm : gnliGraph.getTextGraph().getSkolems()) {
			if (gnliGraph.getTextGraph().isLexCoRef(tTerm) == false) // && gnliGraph.getTextGraph().isRstr(tTerm) == false)
				this.textTerms.add(tTerm);
		}
		for (TermNode tTerm : gnliGraph.getTextGraph().getDerivedSkolems()) {
			if (gnliGraph.getTextGraph().isLexCoRef(tTerm) == false) // && gnliGraph.getTextGraph().isRstr(tTerm) == false)
				this.textTerms.add(tTerm);
		}
		this.kb = kb;
		this.highestCosSimil = 0.0;
		this.POSToExclude = new ArrayList<String>();
		this.POSToExclude.add("PRP");
		this.POSToExclude.add("IN");
		this.POSToExclude.add("DT");
		this.POSToExclude.add("WDT");
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
			HashMap<MatchContent, TermNode> senseContents = new HashMap<MatchContent,TermNode>();
			for (TermNode tTerm : textTerms) {
				HashMap<MatchContent, TermNode>  contents = checkSenseMatch(hTerm, tTerm, senseContents);
				senseContents.putAll(contents);
			}

			if (!senseContents.isEmpty()){
				List<MatchContent> keys = new ArrayList<MatchContent>(senseContents.keySet());
				Collections.sort(keys);
				double minCost = keys.get(0).getScore();
				for (MatchContent key : keys){
					if (key.getScore() == minCost){
						final MatchEdge senseMatch = new MatchEdge("sense",keys.get(0));
						gnliGraph.addMatchEdge(senseMatch, hTerm.node, senseContents.get(key));
						hTerm.pendMatch();
					}
				}
			}
		}

		updatePendingMatches();
		for (CheckedTermNode hTerm : hypothesisTerms) {
			HashMap<MatchContent, TermNode> conceptContents = new HashMap<MatchContent,TermNode>();
			for (TermNode tTerm : textTerms) {
				HashMap<MatchContent, TermNode>  contents = checkConceptMatch(hTerm, tTerm, conceptContents);
				conceptContents.putAll(contents);
			}

			if (!conceptContents.isEmpty()){
				List<MatchContent> keys = new ArrayList<MatchContent>(conceptContents.keySet());
				Collections.sort(keys);
				// after sorting, the key with the lowest penalty is first
				double minCost = keys.get(0).getScore();
				/// go through all keys and add ALL with the same low penalty
				for (MatchContent key : keys){
					if (key.getScore() == minCost){
						final MatchEdge senseMatch = new MatchEdge("concept",key);
						gnliGraph.addMatchEdge(senseMatch, hTerm.node, conceptContents.get(key));
						hTerm.pendMatch();
					}
				}
			}
		}
		
		updatePendingMatches();
		/*for (CheckedTermNode hTerm : hypothesisTerms) {
			TermNode similHTerm = null;
			TermNode similTTerm = null;
			for (TermNode tTerm : textTerms) {
				List<TermNode> bestMatches = getBestEmbedMatches(hTerm, tTerm, similHTerm, similTTerm);
				if (!bestMatches.isEmpty()){
					similHTerm = bestMatches.get(0);
					similTTerm = bestMatches.get(1);
				}
			}
			checkEmbedMatch(hTerm, similHTerm, similTTerm);
		}
		updatePendingMatches();*/
		
	
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
	protected HashMap<MatchContent,TermNode> checkSenseMatch(CheckedTermNode cHTerm,TermNode tTerm, HashMap<MatchContent, TermNode> contents) {
		if (cHTerm.isMatched()) {
			return new HashMap<MatchContent,TermNode>();
		}
		MatchContent linkContent = null;
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
				MatchOrigin.MatchType matchType  = MatchOrigin.MatchType.SENSE;
				if (hSenseId.startsWith("cmp_")){
					hSenseId = hSenseId.substring(4);
					matchType = MatchOrigin.MatchType.SENSE_CMP;
				}
				if (tSenseId.startsWith("cmp_")){
					tSenseId = tSenseId.substring(4);
					matchType = MatchOrigin.MatchType.SENSE_CMP;
				}
				/*
				 following code to get first an second super concept of each term: if the terms have a same super
				 concept with max depth 2, they are disjoint
				String hSuperConcept1 = "";
				String hSuperConcept2 = "";
				String tSuperConcept1 = "";
				String tSuperConcept2 = "";
				for (String superConcept : hSuperConcepts.keySet()){
					if (hSuperConcepts.get(superConcept) == 0){
						hSuperConcept1 = superConcept;
					} else if (hSuperConcepts.get(superConcept) == 1){
						hSuperConcept2 = superConcept;
					}
				}
				for (String superConcept : tSuperConcepts.keySet()){
					if (tSuperConcepts.get(superConcept) == 0){
						tSuperConcept1 = superConcept;
					} else if (tSuperConcepts.get(superConcept) == 1){
						tSuperConcept2 = superConcept;
					}
				}
				*/
				if (tSenseId != null && hSenseId != null && !tSenseId.equals("U") && !hSenseId.equals("U") && !((SkolemNodeContent) tTerm.getContent()).getStem().equals("be")
						&& !((SkolemNodeContent) hTerm.getContent()).getStem().equals("be") && !POSToExclude.contains(((SkolemNodeContent) tTerm.getContent()).getPartOfSpeech()) 
						&& !POSToExclude.contains(((SkolemNodeContent) hTerm.getContent()).getPartOfSpeech())){
					// maximum best score of a sense is 1. If hTerm and tTerm have both best scores, then max total score is 2, so penalty is 2 - actualScore 
					float penalty = 2 - (tSenseNode.getContent().getSenseScore() + hSenseNode.getContent().getSenseScore());
					if (tSenseId.equals(hSenseId)) {
						linkContent = new MatchContent(matchType, hSenseId, tSenseId,null, Specificity.EQUALS, penalty, 0);
					} else if (tSynonyms.contains(hTerm.getLabel().substring(0,hTerm.getLabel().indexOf("_"))) || hSynonyms.contains(tTerm.getLabel().substring(0,tTerm.getLabel().indexOf("_")))){
						linkContent = new MatchContent(matchType, hSenseId, tSenseId,null, Specificity.EQUALS, penalty, 0);
					} else if (tSuperConcepts.keySet().contains(hSenseId)){
						linkContent = new MatchContent(matchType, hSenseId, tSenseId,null, Specificity.SUBCLASS, penalty, tSuperConcepts.get(hSenseId));
					} else if (hSuperConcepts.keySet().contains(tSenseId)){
						linkContent = new MatchContent(matchType, hSenseId, tSenseId,null, Specificity.SUPERCLASS, penalty, hSuperConcepts.get(tSenseId));
					} else if (hSubConcepts.keySet().contains(tSenseId)){
						linkContent = new MatchContent(matchType, hSenseId, tSenseId,null, Specificity.SUBCLASS, penalty, hSubConcepts.get(tSenseId));
					} else if (tSubConcepts.keySet().contains(hSenseId)){
						linkContent = new MatchContent(matchType, hSenseId, tSenseId,null, Specificity.SUPERCLASS, penalty, tSubConcepts.get(hSenseId));
					} else if (hAntonyms.contains(tTerm.getLabel().substring(0,tTerm.getLabel().indexOf("_"))) || tAntonyms.contains(hTerm.getLabel().substring(0,hTerm.getLabel().indexOf("_"))) ){
						linkContent = new MatchContent(matchType, hSenseId, tSenseId,null, Specificity.DISJOINT, penalty, 0);
					}
					
					if (linkContent != null)
						contents.put(linkContent,tTerm);
				}

			}
		}
		return contents;
	}
	
	
	public Set<Formula> allTermsInFormulas(KB kb, String term) {
		HashSet<Formula> result = new HashSet<>();
		Pattern pattern = Pattern.compile("(\\s|\\()" + term + "(\\s|\\))");
		for (String f : kb.formulaMap.keySet()){
			Matcher matcher = pattern.matcher(f);
			if (matcher.find()) {
				result.add(kb.formulaMap.get(f));
			}
		}
		return result;
	}

	protected HashMap<MatchContent, TermNode> checkConceptMatch(CheckedTermNode cHTerm,TermNode tTerm, HashMap<MatchContent, TermNode> contents) {
		if (cHTerm.isMatched()) {
			return new HashMap<MatchContent,TermNode>();
		}
		TermNode hTerm = cHTerm.node;
		MatchContent linkContent = null;
		// sort the senses of the tTerm based on their score, get the first one and then its concept
		// we do not go through all concepts for now, because there is too much over-matching happening
		List<SenseNode> tSensesSorted = gnliGraph.getTextGraph().getSenses(tTerm);
		if (tSensesSorted.isEmpty())
			return contents;
		Collections.sort(tSensesSorted);
		SenseNode tSenseNode =  tSensesSorted.get(0);
		String tConcept = ((SenseNodeContent) tSenseNode.getContent()).getConcepts().get(0);
		// sort the senses of the hTerm based on their score, get the first one and then its concept
		// we do not go through all concepts for now, because there is too much over-matching happening
		List<SenseNode> hSensesSorted = gnliGraph.getHypothesisGraph().getSenses(hTerm);
		if (hSensesSorted.isEmpty())
			return contents;
		Collections.sort(hSensesSorted);
		SenseNode hSenseNode =  hSensesSorted.get(0);
		String hConcept = ((SenseNodeContent) hSenseNode.getContent()).getConcepts().get(0);

		if (tConcept != null && hConcept != null && tConcept != "" && hConcept != "" && !((SkolemNodeContent) tTerm.getContent()).getStem().equals("be")
				&& !((SkolemNodeContent) hTerm.getContent()).getStem().equals("be") && !POSToExclude.contains(((SkolemNodeContent) tTerm.getContent()).getPartOfSpeech()) 
				&& !POSToExclude.contains(((SkolemNodeContent) hTerm.getContent()).getPartOfSpeech())
				&& !tConcept.equals("SubjectiveAssessmentAttribute+") && !hConcept.equals("SubjectiveAssessmentAttribute+")){
			// maximum best score of a sense is 1. If hTerm and tTerm have both best scores, then max total score is 2, so penalty is 2 - actualScore 
			//float penalty = 2 - (tSenseNode.getContent().getSenseScore() + hSenseNode.getContent().getSenseScore());
			if (tConcept.equals(hConcept)) {
				linkContent = new MatchContent(MatchOrigin.MatchType.CONCEPT, ((SenseNodeContent) hSenseNode.getContent()).getSenseId(), ((SenseNodeContent) tSenseNode.getContent()).getSenseId(), tConcept, Specificity.EQUALS, 0, 0);
			} else if (tConcept.substring(0,tConcept.length()-1).equals(hConcept.substring(0,hConcept.length()-1)) && tConcept.substring(tConcept.length()-1).equals("+") && hConcept.substring(hConcept.length()-1).equals("=")) {
				linkContent = new MatchContent(MatchOrigin.MatchType.CONCEPT, ((SenseNodeContent) hSenseNode.getContent()).getSenseId(), ((SenseNodeContent) tSenseNode.getContent()).getSenseId(), tConcept, Specificity.SUBCLASS, 0, 0);
			} else if (tConcept.substring(0,tConcept.length()-1).equals(hConcept.substring(0,hConcept.length()-1)) && tConcept.substring(tConcept.length()-1).equals("=") && hConcept.substring(hConcept.length()-1).equals("+")) {
				linkContent = new MatchContent(MatchOrigin.MatchType.CONCEPT, ((SenseNodeContent) hSenseNode.getContent()).getSenseId(), ((SenseNodeContent) tSenseNode.getContent()).getSenseId(), tConcept, Specificity.SUPERCLASS, 0, 0);
			} 
			else {
				ArrayList<Formula> listOfRelations = kb.askWithRestriction(2, tConcept.substring(0,tConcept.length()-1), 1, hConcept.substring(0,hConcept.length()-1));
				listOfRelations.addAll(kb.askWithRestriction(2, hConcept.substring(0,hConcept.length()-1), 1, tConcept.substring(0,tConcept.length()-1)));
				//ArrayList<Formula> result3 = KButilities.termIntersection(kb,"Pilot","FlyingAircraft");
				//ArrayList<String> result14 = kb.getNearestRelations("Human"); // gives me the nearest neighbors, e.g. HumanChild
				//allTermsInFormulas(kb, "Pilot");
				//allTermsInFormulas(kb, "Kid");
				Specificity spec = null;	
				for (Formula f :  listOfRelations){
					String firstArg = f.getArgument(1);
					String secondArg = f.getArgument(2);						
					for (String rel : f.gatherRelationConstants()){
						if (rel.equals("subclass")){
							if (hConcept.contains(firstArg) && tConcept.contains(secondArg))
								spec = Specificity.SUPERCLASS;
							else if (tConcept.contains(firstArg) && hConcept.contains(secondArg))
								spec = Specificity.SUBCLASS;
						} else if (rel.equals("partition") && spec == null){
							if (hConcept.contains(firstArg) && tConcept.contains(secondArg))
								spec = Specificity.SUBCLASS;
							else if (tConcept.contains(firstArg) && hConcept.contains(firstArg))
								spec = Specificity.SUPERCLASS;
						} else if (rel.equals("instance") && spec == null){
							if (hConcept.contains(firstArg) && tConcept.contains(secondArg))
								spec = Specificity.SUBCLASS;
							else if (tConcept.contains(firstArg) && hConcept.contains(firstArg))
								spec = Specificity.SUPERCLASS;
						}
					}
				}
				// if there is no listOfRelations, check if there is some kind of sub/super class relation.
				if (listOfRelations.isEmpty()){
					boolean isSubclassTH = kb.isSubclass(tConcept.substring(0,tConcept.length()-1), hConcept.substring(0,hConcept.length()-1));
					boolean isSubclassHT = kb.isSubclass(hConcept.substring(0,hConcept.length()-1), tConcept.substring(0,tConcept.length()-1));
					if (isSubclassTH == true)
						spec = Specificity.SUBCLASS;
					else if (isSubclassHT == true)
						spec = Specificity.SUPERCLASS;
				}
				
				// check if some of the parents of the one term are also parents of the other term (only first two) ==> disjoint , e.g. red vs. yellow
				// get first 2 parents of tTerm
				/*ArrayList<Formula> formsTSuperConcepts = new ArrayList<Formula>();
				if (!kb.askWithRestriction(0,"subclass",1,tConcept.substring(0,tConcept.length()-1)).isEmpty() ){				
					formsTSuperConcepts.add(kb.askWithRestriction(0,"subclass",1,tConcept.substring(0,tConcept.length()-1)).get(kb.askWithRestriction(0,"subclass",1,tConcept.substring(0,tConcept.length()-1)).size()-1));
					if (kb.askWithRestriction(0,"subclass",1,tConcept.substring(0,tConcept.length()-1)).size() > 1)
						formsTSuperConcepts.add(kb.askWithRestriction(0,"subclass",1,tConcept.substring(0,tConcept.length()-1)).get(kb.askWithRestriction(0,"subclass",1,tConcept.substring(0,tConcept.length()-1)).size()-2));
				}
				if (!kb.askWithRestriction(0,"instance",1,tConcept.substring(0,tConcept.length()-1)).isEmpty()){
					formsTSuperConcepts.add(kb.askWithRestriction(0,"instance",1,tConcept.substring(0,tConcept.length()-1)).get(kb.askWithRestriction(0,"instance",1,tConcept.substring(0,tConcept.length()-1)).size()-1));
					if (kb.askWithRestriction(0,"instance",1,tConcept.substring(0,tConcept.length()-1)).size() > 1)
						formsTSuperConcepts.add(kb.askWithRestriction(0,"instance",1,tConcept.substring(0,tConcept.length()-1)).get(kb.askWithRestriction(0,"instance",1,tConcept.substring(0,tConcept.length()-1)).size()-2));
				}
				if (!kb.askWithRestriction(0,"subrelation",1,tConcept.substring(0,tConcept.length()-1)).isEmpty()){
					formsTSuperConcepts.add(kb.askWithRestriction(0,"subrelation",1,tConcept.substring(0,tConcept.length()-1)).get(kb.askWithRestriction(0,"subrelation",1,tConcept.substring(0,tConcept.length()-1)).size()-1));
					if (kb.askWithRestriction(0,"subrelation",1,tConcept.substring(0,tConcept.length()-1)).size() > 1)
						formsTSuperConcepts.add(kb.askWithRestriction(0,"subrelation",1,tConcept.substring(0,tConcept.length()-1)).get(kb.askWithRestriction(0,"subrelation",1,tConcept.substring(0,tConcept.length()-1)).size()-2));
				}
				if (!kb.askWithRestriction(0,"subAttribute",1,tConcept.substring(0,tConcept.length()-1)).isEmpty()){
					formsTSuperConcepts.add(kb.askWithRestriction(0,"subAttribute",1,tConcept.substring(0,tConcept.length()-1)).get(kb.askWithRestriction(0,"subAttribute",1,tConcept.substring(0,tConcept.length()-1)).size()-1));
					if (kb.askWithRestriction(0,"subAttribute",1,tConcept.substring(0,tConcept.length()-1)).size() > 1)
						formsTSuperConcepts.add(kb.askWithRestriction(0,"subAttribute",1,tConcept.substring(0,tConcept.length()-1)).get(kb.askWithRestriction(0,"subAttribute",1,tConcept.substring(0,tConcept.length()-1)).size()-2));
				}

																
				// get first 2 parents of hTerm
				ArrayList<Formula> formsHSuperConcepts = new ArrayList<Formula>();
				if (!kb.askWithRestriction(0,"subclass",1,hConcept.substring(0,hConcept.length()-1)).isEmpty() ){				
					formsHSuperConcepts.add(kb.askWithRestriction(0,"subclass",1,hConcept.substring(0,hConcept.length()-1)).get(kb.askWithRestriction(0,"subclass",1,hConcept.substring(0,hConcept.length()-1)).size()-1));
					if (kb.askWithRestriction(0,"subclass",1,hConcept.substring(0,hConcept.length()-1)).size() > 1)
						formsHSuperConcepts.add(kb.askWithRestriction(0,"subclass",1,hConcept.substring(0,hConcept.length()-1)).get(kb.askWithRestriction(0,"subclass",1,hConcept.substring(0,hConcept.length()-1)).size()-2));
				}
				if (!kb.askWithRestriction(0,"instance",1,hConcept.substring(0,hConcept.length()-1)).isEmpty()){
					formsHSuperConcepts.add(kb.askWithRestriction(0,"instance",1,hConcept.substring(0,hConcept.length()-1)).get(kb.askWithRestriction(0,"instance",1,hConcept.substring(0,hConcept.length()-1)).size()-1));
					if (kb.askWithRestriction(0,"instance",1,hConcept.substring(0,hConcept.length()-1)).size() > 1)
						formsHSuperConcepts.add(kb.askWithRestriction(0,"instance",1,hConcept.substring(0,hConcept.length()-1)).get(kb.askWithRestriction(0,"instance",1,hConcept.substring(0,hConcept.length()-1)).size()-2));
				}
				if (!kb.askWithRestriction(0,"subrelation",1,hConcept.substring(0,hConcept.length()-1)).isEmpty()){
					formsHSuperConcepts.add(kb.askWithRestriction(0,"subrelation",1,hConcept.substring(0,hConcept.length()-1)).get(kb.askWithRestriction(0,"subrelation",1,hConcept.substring(0,hConcept.length()-1)).size()-1));
					if (kb.askWithRestriction(0,"subrelation",1,hConcept.substring(0,hConcept.length()-1)).size() > 1)
						formsHSuperConcepts.add(kb.askWithRestriction(0,"subrelation",1,hConcept.substring(0,hConcept.length()-1)).get(kb.askWithRestriction(0,"subrelation",1,hConcept.substring(0,hConcept.length()-1)).size()-2));
				}
				if (!kb.askWithRestriction(0,"subAttribute",1,hConcept.substring(0,hConcept.length()-1)).isEmpty()){
					formsHSuperConcepts.add(kb.askWithRestriction(0,"subAttribute",1,hConcept.substring(0,hConcept.length()-1)).get(kb.askWithRestriction(0,"subAttribute",1,hConcept.substring(0,hConcept.length()-1)).size()-1));
					if (kb.askWithRestriction(0,"subAttribute",1,hConcept.substring(0,hConcept.length()-1)).size() > 1)
						formsHSuperConcepts.add(kb.askWithRestriction(0,"subAttribute",1,hConcept.substring(0,hConcept.length()-1)).get(kb.askWithRestriction(0,"subAttribute",1,hConcept.substring(0,hConcept.length()-1)).size()-2));
				}

				
				ArrayList<String> tParents = new ArrayList<String>();
				ArrayList<String> hParents = new ArrayList<String>();
				
				Iterator<Formula> itT = formsTSuperConcepts.iterator();
		        while (itT.hasNext()) {
		            Formula f = itT.next();
		            tParents.add(f.getArgument(2));
		        }
		        Iterator<Formula> itH = formsHSuperConcepts.iterator();
		        while (itH.hasNext()) {
		            Formula f = itH.next();
		            hParents.add(f.getArgument(2));
		        }
		        
		        for (String tP : tParents){
		        	for (String hP : hParents){
		        		if (tP.equals(hP)){
		        			spec = Specificity.DISJOINT;
		        		}
		        	}
		        }*/

				if (spec != null){
					linkContent = new MatchContent(MatchOrigin.MatchType.CONCEPT, ((SenseNodeContent) hSenseNode.getContent()).getSenseId(), ((SenseNodeContent) tSenseNode.getContent()).getSenseId(), tConcept, spec, 0, 0);
				}
			}
			if (linkContent != null)
				contents.put(linkContent,tTerm);
		}	
		return contents;
	}
	

	protected List<TermNode> getBestEmbedMatches(CheckedTermNode cHTerm,TermNode tTerm, TermNode bestSimilHTerm,TermNode bestSimilTTerm) {
		List<TermNode> bestMatches = new ArrayList<TermNode>();
		if (cHTerm.isMatched() ) {
			return bestMatches;
		}
		TermNode hTerm = cHTerm.node;
		for (final SenseNode tSenseNode : gnliGraph.getTextGraph().getSenses(tTerm)) {
			float[] tEmbed =  ((SenseNodeContent) tSenseNode.getContent()).getEmbed();
			for (final SenseNode hSenseNode : gnliGraph.getHypothesisGraph().getSenses(hTerm)) {
				float[] hEmbed = ((SenseNodeContent) hSenseNode.getContent()).getEmbed();
				// exclude high frequency, functional elements from being matched
				if (tEmbed != null && hEmbed != null && !POSToExclude.contains(((SkolemNodeContent) tTerm.getContent()).getPartOfSpeech()) &&
					!POSToExclude.contains(((SkolemNodeContent) hTerm.getContent()).getPartOfSpeech()) 
					&& !((SkolemNodeContent) tTerm.getContent()).getStem().equals("be") && !((SkolemNodeContent) hTerm.getContent()).getStem().equals("be")){
					double cosSimil = computeCosineSimilarity(hEmbed, tEmbed);
					if (cosSimil > highestCosSimil){
						highestCosSimil = cosSimil;
						bestSimilHTerm = hTerm;
						bestSimilTTerm = tTerm;
					}
				}
				break;
			}
			break;
		}
		bestMatches.add(bestSimilHTerm);
		bestMatches.add(bestSimilTTerm);
		return bestMatches;
	}
	
	protected  List<MatchEdge> checkEmbedMatch(CheckedTermNode cHTerm, TermNode similHTerm,TermNode similTTerm ) {
		List<MatchEdge> retval = new ArrayList<MatchEdge>();
		// avoid matching if the tTerm is already matched to another hTerm, to avoid overmatching
		if (similHTerm != null && similTTerm != null && gnliGraph.getInMatches(similTTerm).isEmpty() && highestCosSimil > 0.65){
			// subtract the similarity from 1 in order to have the result as the penalty(depth). This means
			// highest cosine similarity will give lower penalty than lower similarity.
			double depth = 1 - highestCosSimil;
			final MatchContent linkContent = new MatchContent(MatchOrigin.MatchType.EMBED, Specificity.EQUALS, 0, depth);
			final MatchEdge conceptMatch = new MatchEdge("embed",linkContent);
			gnliGraph.addMatchEdge(conceptMatch, similHTerm, similTTerm);
			retval.add(conceptMatch);
			cHTerm.pendMatch();
		}
		return retval;
	}
	
	private double computeCosineSimilarity(float[] vectorA, float[] vectorB) {
	    float dotProduct = 0;
	    float normA = 0;
	    float normB = 0;
	    double cosSimil = 0;
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
	
	// accrding to MacCartney diss
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
