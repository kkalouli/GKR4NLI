package gnli;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gnli.MatchEdge;
import sem.graph.SemGraph;
import sem.graph.SemanticEdge;
import sem.graph.SemanticGraph;
import sem.graph.SemanticNode;
import sem.graph.vetypes.GraphLabels;
import sem.graph.vetypes.RoleEdge;
import sem.graph.vetypes.SkolemNode;
import sem.graph.vetypes.TermNode;
import sem.mapper.DepGraphToSemanticGraph;

public class GNLIGraph implements Serializable {
	private static final long serialVersionUID = 7867467000348756256L;
	protected SemanticGraph gnliGraph;
	protected SemanticGraph textGraph;
	protected SemanticGraph hypothesisGraph;
	protected SemanticGraph matchGraph;
	
	
	/**
	 * Create a gnli graph from a list of premise and conclusion graphs
	 * @param text
	 * @param hypothesis
	 */
	
	public GNLIGraph(final List<SemanticGraph> texts,
			final List<SemanticGraph> hypotheses) {
		gnliGraph = new SemanticGraph();
		matchGraph = new SemanticGraph();
		textGraph = new SemanticGraph();
		hypothesisGraph = new SemanticGraph();
		for (SemanticGraph semanticGraph : hypotheses) {
			addGraph(semanticGraph, hypothesisGraph);
		}
		for (SemanticGraph semanticGraph : texts) {
			addGraph(semanticGraph, textGraph);
		}

		//hypothesisGraph.display();
		//textGraph.display();
		// textGraph.display();
		gnliGraph.merge(hypothesisGraph);
		gnliGraph.merge(textGraph);
		gnliGraph.merge(matchGraph);
		gnliGraph.setRootNode(hypothesisGraph.getRootNode());
		//gnliGraph.display();

	}
	
	/**
	 * 	Create a new semantic graph from a semantic analysis, and add it to the
	 *  current graph
	 * @param graph
	 * @param mainGraph
	 */

	private void addGraph(SemanticGraph graph, SemanticGraph mainGraph) {
		mainGraph.merge(graph);
		if (mainGraph.getRootNode() == null) {
			mainGraph.setRootNode(graph.getRootNode());
		}
		if (mainGraph.getName() == null || mainGraph.getName().isEmpty()) {
			mainGraph.setName(graph.getName());
		}
	}
	
		
	/**
	 * Get the full gnliGraph
	 * 
	 * @return
	 */
	public SemanticGraph getGNLIGraph() {
		return gnliGraph;
	}

	/**
	 * Get the semantic graph for the Text
	 * 
	 * @return
	 */
	public SemanticGraph getTextGraph() {
		return textGraph;
	}

	/**
	 * Get the semantic graph for the Hypothesis
	 * 
	 * @return
	 */
	public SemanticGraph getHypothesisGraph() {
		return hypothesisGraph;
	}

	/**
	 * Get the graph matching premise to conclusion terms
	 * 
	 * @return
	 */
	public SemanticGraph getMatchGraph() {
		return matchGraph;
	}


	/**
	 * Add a new (match) edge to the graph
	 * 
	 * @param edge
	 * @param premiseTerm
	 * @param conclusionTerm
	 */
	public void addMatchEdge(SemanticEdge edge, SemanticNode<?> textTerm,
			SemanticNode<?> hypothesisTerm) {
		// Term matches are held on the link sub-graph of the hypothesis
		this.matchGraph.addLinkEdge(edge, textTerm, hypothesisTerm);
		this.gnliGraph.addLinkEdge(edge, textTerm, hypothesisTerm);
	}
	

	/**
	 * Remove the specified match edge from the graph
	 * 
	 * @param match
	 */
	public void removeMatchEdge(SemanticEdge match) {
		this.matchGraph.removeLinkEdge(match);
		this.gnliGraph.removeLinkEdge(match);
	}

	
	/**
	 * Remove the specified match node from the graph
	 * 
	 * @param match
	 */
	public void removeMatchNode(SemanticNode<?> node) {
		this.matchGraph.removeLinkNode(node);
		this.gnliGraph.removeLinkNode(node);
	}

	/**
	 * Does the hypothesis term have any matches
	 * 
	 * @param hTerm
	 * @return
	 */
	public boolean hasMatch(TermNode hTerm) {
		return matchGraph.getLinkedNodes().contains(hTerm);
	}

	/**
	 * Get all the matches between premise and conclusion nodes
	 * 
	 * @return
	 */
	public List<MatchEdge> getMatches() {
		List<MatchEdge> retval = new ArrayList<MatchEdge>();
		for (SemanticEdge edge : matchGraph.getLinks()) {
			if (edge.getClass().equals(MatchEdge.class)) {
				retval.add((MatchEdge) edge);
			}
		}
		return retval;
	}

	/**
	 * Get all the match edges for the conclusion node
	 * 
	 * @param node
	 * @return
	 */
	public List<MatchEdge> getOutMatches(SemanticNode<?> node) {
		List<MatchEdge> retval = new ArrayList<MatchEdge>();
		for (SemanticEdge edge : matchGraph.getOutEdges(node)) {
			if (edge.getClass().equals(MatchEdge.class)) {
				retval.add((MatchEdge) edge);
			}
		}
		return retval;
	}
	
	/**
	 * Get all the match edges for the premise node
	 * 
	 * @param node
	 * @return
	 */
	public List<MatchEdge> getInMatches(SemanticNode<?> node) {
		List<MatchEdge> retval = new ArrayList<MatchEdge>();
		for (SemanticEdge edge : matchGraph.getInEdges(node)) {
			if (edge.getClass().equals(MatchEdge.class)) {
				retval.add((MatchEdge) edge);
			}
		}
		return retval;
	}
	
	public List<MatchEdge> getMatches(SemanticNode<?> node, String mode) {
		if (mode.equals("hyp"))
			return getOutMatches(node);
		else
			return getInMatches(node);
	}
	
	/**
	 * Get the start node of an edge
	 * 
	 * @param edge
	 * @return
	 */
	public SemanticNode<?> getStartNode(SemanticEdge edge) {
		return gnliGraph.getStartNode(edge);
	}

	/**
	 * Get the end node of an edge
	 * 
	 * @param edge
	 * @return
	 */
	public SemanticNode<?> getFinishNode(SemanticEdge edge) {
		return gnliGraph.getFinishNode(edge);
	}
	
	public SemanticNode<?> getMatchedTextTerm(SemanticEdge match) {
		return this.getFinishNode(match);
	}

	public SemanticNode<?> getMatchedHypothesisTerm(SemanticEdge match) {
		return this.getStartNode(match);
	}
	
	public List<SkolemNode> getAllHModifiers(SemanticEdge match){	
		List<SkolemNode> dHMod = this.getHypothesisGraph().getAllModifiers((SkolemNode) this.getMatchedHypothesisTerm(match));
		return dHMod;
	}
	
	public List<SkolemNode> getAllTModifiers(SemanticEdge match){	
		List<SkolemNode> dTMod = this.getTextGraph().getAllModifiers((SkolemNode) this.getMatchedTextTerm(match));
		return dTMod;
	}

	
	/**
	 * Open graphical display of gnliGraph
	 */
	public void display() {
		Set<SemanticNode<?>> mergedNodes = new HashSet<SemanticNode<?>>();
		mergedNodes.addAll(this.hypothesisGraph.getRoleGraph().getNodes());
		mergedNodes.addAll(this.textGraph.getContextGraph().getNodes());
		
		Set<SemanticEdge> mergedEdges = new HashSet<SemanticEdge>();
		mergedEdges.addAll(this.hypothesisGraph.getRoleGraph().getEdges());
		mergedEdges.addAll(this.textGraph.getContextGraph().getEdges());
		
		SemGraph subgraph = this.gnliGraph.getSubGraph(mergedNodes, mergedEdges);
		subgraph.display();
	}
	
	
	public static void main(String args[]) throws IOException {
		DepGraphToSemanticGraph semGraph = new DepGraphToSemanticGraph();
		List<SemanticGraph> texts = new ArrayList<SemanticGraph>();
		List<SemanticGraph> hypotheses = new ArrayList<SemanticGraph>();
		String sent1 = "The man is riding a bike.";
		String sent2 = "The man is not riding a bike.";
		SemanticGraph graphT = semGraph.sentenceToGraph(sent1, sent1+" "+sent2);
		texts.add(graphT);
		SemanticGraph graphH = semGraph.sentenceToGraph(sent2, sent2+" "+sent1);
		hypotheses.add(graphH);
		GNLIGraph gnli = new GNLIGraph(texts, hypotheses);
		//gnli.display();

	}

}
