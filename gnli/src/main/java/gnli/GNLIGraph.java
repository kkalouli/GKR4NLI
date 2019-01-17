package gnli;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sem.mapper.DepGraphToSemanticGraph;
import semantic.graph.SemGraph;
import semantic.graph.SemanticEdge;
import semantic.graph.SemanticGraph;
import semantic.graph.SemanticNode;
import semantic.graph.vetypes.GraphLabels;
import semantic.graph.vetypes.RoleEdge;
import semantic.graph.vetypes.TermNode;

public class GNLIGraph {
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
	 * Does the hypothesis term have any matches
	 * 
	 * @param hTerm
	 * @return
	 */
	public boolean hasMatch(TermNode hTerm) {
		return matchGraph.getLinkedNodes().contains(hTerm);
	}

	
	/**
	 * Open graphical display of gnliGraph
	 */
	public void display() {
		Set<SemanticNode<?>> nodes = new HashSet<SemanticNode<?>>();
		Set<SemanticEdge> edges = new HashSet<SemanticEdge>();
		Map<Color, List<SemanticNode<?>>> nodeProperties = new HashMap<Color, List<SemanticNode<?>>>();
		Map<Color, List<SemanticEdge>> edgeProperties = new HashMap<Color, List<SemanticEdge>>();
		List<SemanticNode<?>> textNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.GREEN, textNodes);
		List<SemanticEdge> textEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.GREEN, textEdges);
		List<SemanticNode<?>> hypothesisNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.ORANGE, hypothesisNodes);
		List<SemanticEdge> hypothesisEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.ORANGE, hypothesisEdges);
		List<SemanticEdge> matchEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.BLACK, matchEdges);

		for (SemanticNode<?> tNode : this.textGraph.getRoleGraph()
				.getNodes()) {
			nodes.add(tNode);
			textNodes.add(tNode);
		}
		for (SemanticEdge tEdge : this.textGraph.getRoleGraph().getEdges()) {
			edges.add(tEdge);
			textEdges.add(tEdge);
		}
		for (SemanticNode<?> hNode : this.hypothesisGraph.getRoleGraph()
				.getNodes()) {
			nodes.add(hNode);
			hypothesisNodes.add(hNode);
		}
		for (SemanticEdge hEdge : this.hypothesisGraph.getRoleGraph()
				.getEdges()) {
			edges.add(hEdge);
			hypothesisEdges.add(hEdge);
		}
		nodes.addAll(this.hypothesisGraph.getRoleGraph().getNodes());
		edges.addAll(this.hypothesisGraph.getRoleGraph().getEdges());
		//addDisplayLinks(nodes, edges, textNodes, textEdges, hypothesisNodes,
				//hypothesisEdges, matchEdges);
		SemGraph subGraph = this.gnliGraph.getSubGraph(nodes, edges);
		subGraph.display(nodeProperties, edgeProperties);
	}
	
	public static void main(String args[]) throws IOException {
		DepGraphToSemanticGraph semGraph = new DepGraphToSemanticGraph();
		List<SemanticGraph> texts = new ArrayList<SemanticGraph>();
		List<SemanticGraph> hypotheses = new ArrayList<SemanticGraph>();
		SemanticGraph graphT = semGraph.sentenceToGraph("The man is riding a bike.");
		texts.add(graphT);
		SemanticGraph graphH = semGraph.sentenceToGraph("The man is not riding a bike.");
		hypotheses.add(graphH);
		GNLIGraph gnli = new GNLIGraph(texts, hypotheses);
		//gnli.display();

	}

}
