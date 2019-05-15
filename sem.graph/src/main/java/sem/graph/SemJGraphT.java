package sem.graph;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JWindow;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;

/**
 * Implementation of SemGraph via JGraphT
 *
 */
public class SemJGraphT implements SemGraph, Serializable{

	private static final long serialVersionUID = 4437969385952923418L;
	private DirectedGraph<SemanticNode<?>, SemanticEdge> graph;

	public SemJGraphT() {
		this.graph = new DirectedMultigraph<SemanticNode<?>, SemanticEdge>(SemanticEdge.class);
	}

	public SemJGraphT(DirectedGraph<SemanticNode<?>, SemanticEdge> graph) {
			this.graph = graph;
	}
	

	@Override
	public void addNode(SemanticNode<?> node) {
		graph.addVertex(node);	
	}

	@Override
	public void addEdge(SemanticNode<?> start, SemanticNode<?> end,
			SemanticEdge edge) {
		if (!graph.containsVertex(start)) {
			addNode(start);
		}
		if (!graph.containsVertex(end)) {
			addNode(end);
		}
		graph.addEdge(start, end, edge);			
		edge.sourceVertexId = start.label;
		edge.destVertexId = end.label;
	}

	@Override
	public void addUndirectedEdge(SemanticNode<?> node1, SemanticNode<?> node2,
			SemanticEdge edge) {
		System.err.println("SemJGraphT.addUndirectedEdge :: Not Implemented");	
	}

	@Override
	public void removeEdge(SemanticEdge edge) {
		graph.removeEdge(edge);	
	}
	
	@Override
	public void removeNode(SemanticNode<?> node) {
		graph.removeVertex(node);	
	}

	@Override
	public boolean containsNode(SemanticNode<?> node) {
		return graph.containsVertex(node);
	}

	@Override
	public boolean containsEdge(SemanticEdge edge) {
		return graph.containsEdge(edge);
	}

	@Override
	public Set<SemanticEdge> getEdges() {
		Set<SemanticEdge> retval = graph.edgeSet();
		if (retval == null) {
			return new HashSet<SemanticEdge>(0);
		} else {
			return retval;
		}
	}

	@Override
	public Set<SemanticEdge> getEdges(SemanticNode<?> node) {
		if (graph.containsVertex(node)) {
			return graph.edgesOf(node);
		} else {
			return new HashSet<SemanticEdge>(0);
		}
	}

	@Override
	public Set<SemanticEdge> getEdges(SemanticNode<?> start,
			SemanticNode<?> end) {
		Set<SemanticEdge> retval =  graph.getAllEdges(start, end);
		if (retval == null) {
			return new HashSet<SemanticEdge>(0);
		} else {
			return retval;
		}
	}

	@Override
	public Set<SemanticEdge> getInEdges(SemanticNode<?> node) {
		if (graph.containsVertex(node)) {
			return graph.incomingEdgesOf(node);
		} else { 
			return new HashSet<SemanticEdge>(0);
		}
	}

	@Override
	public Set<SemanticEdge> getOutEdges(SemanticNode<?> node) {
		if (graph.containsVertex(node)) {
			return graph.outgoingEdgesOf(node);
		} else { 
			return new HashSet<SemanticEdge>(0);
		}
	}

	@Override
	public Set<SemanticNode<?>> getNodes() {
		Set<SemanticNode<?>> retval =  graph.vertexSet();
		if (retval == null) {
			return new HashSet<SemanticNode<?>>();
		} else {
			return retval;
		}
	}

	@Override
	public Set<SemanticNode<?>> getNeighbors(SemanticNode<?> node) {
		if (graph.containsVertex(node)) {
			Set<SemanticNode<?>> retval = getInNeighbors(node);
			retval.addAll(getOutNeighbors(node));
			return retval;
		} else {
			return new HashSet<SemanticNode<?>>(0);
		}
	}

	@Override
	public Set<SemanticNode<?>> getInNeighbors(SemanticNode<?> node) {
		Set<SemanticNode<?>> retval = new HashSet<SemanticNode<?>>();
		if (graph.containsVertex(node)) {
			Set<SemanticEdge> in = graph.incomingEdgesOf(node);
			if (in != null) {
				for (SemanticEdge e : graph.incomingEdgesOf(node)) {
					retval.add(graph.getEdgeSource(e));
				}
			}
		}
		return retval;
	}

	@Override
	public Set<SemanticNode<?>> getOutNeighbors(SemanticNode<?> node) {
		Set<SemanticNode<?>> retval = new HashSet<SemanticNode<?>>();
		if (graph.containsVertex(node)) {
			Set<SemanticEdge> out = graph.outgoingEdgesOf(node);
			if (out != null) {
				for (SemanticEdge e : graph.outgoingEdgesOf(node)) {
					retval.add(graph.getEdgeTarget(e));
				}
			}
		}
		return retval;
	}

	@Override
	public Set<SemanticNode<?>> getOutReach(SemanticNode<?> node) {
		return new HashSet<SemanticNode<?>>(breadthFirstTraversal(this.graph, node));
	}

	
	@Override
	public List<SemanticNode<?>> breadthFirstTraversal(Graph<SemanticNode<?>, SemanticEdge> graph, SemanticNode<?> node) {
		List<SemanticNode<?>> retval = new ArrayList<SemanticNode<?>>();
		if (graph.containsVertex(node)) {
			BreadthFirstIterator<SemanticNode<?>, SemanticEdge> bfi 
				= new BreadthFirstIterator<SemanticNode<?>, SemanticEdge>(graph, node);
			while (bfi.hasNext()) {
				SemanticNode<?> rnode = bfi.next();
				retval.add(rnode);
			}
		}
		return retval;
	}

	@Override
	public Set<SemanticNode<?>> getInReach(SemanticNode<?> node) {
		//protected static List getAllParents(DefaultDirectedWeightedGraph<Vertex, IACEdge> graph, Vertex vertex) {
		//List parents = new ArrayList<>();
		EdgeReversedGraph<SemanticNode<?>, SemanticEdge> reversedGraph = new EdgeReversedGraph<>(graph);
		return new HashSet<SemanticNode<?>>(breadthFirstTraversal(reversedGraph, node));
	}

	
	
	@Override
	public SemanticNode<?> getStartNode(SemanticEdge edge) {
		if (graph.containsEdge(edge)) {
			return graph.getEdgeSource(edge);
		} else {
			return null;
		}
	}

	@Override
	public SemanticNode<?> getEndNode(SemanticEdge edge) {
		if (graph.containsEdge(edge)) {
			return graph.getEdgeTarget(edge);
		} else {
			return null;
		}
	}

	@Override
	public void merge(SemGraph other) {
		// This graph is assumed to be the main one.
		// Other is a pre-existing graph that needs to be made a sub-graph of this.
		// Note: It is down to subsequent code to ensure that any additions
		// to the other graph are also made to this one
		for (SemanticNode<?> node : other.getNodes()) {
			if (!this.graph.containsVertex(node)) {
				this.addNode(node);
			}
		}
		for (SemanticEdge edge : other.getEdges()) {
			if (!this.graph.containsEdge(edge)) {
				this.addEdge(other.getStartNode(edge), other.getEndNode(edge), edge);
			}
		}			
	}

	@Override
	public List<SemanticEdge> getShortestPath(SemanticNode<?> start,
			SemanticNode<?> end) {
		DijkstraShortestPath<SemanticNode<?>, SemanticEdge> dsp
			= new DijkstraShortestPath<SemanticNode<?>, SemanticEdge>(this.graph, start, end);
		return dsp.getPathEdgeList();
	}

	@Override
	public List<SemanticEdge> getShortestUndirectedPath(
			SemanticNode<?> start, SemanticNode<?> end) {
		UndirectedGraph<SemanticNode<?>, SemanticEdge> ugraph 
			= new AsUndirectedGraph<SemanticNode<?>, SemanticEdge>(this.graph);
		DijkstraShortestPath<SemanticNode<?>, SemanticEdge> dsp
			= new DijkstraShortestPath<SemanticNode<?>, SemanticEdge>(ugraph, start, end);
		return dsp.getPathEdgeList();
	}

	@Override
	public void display() {
		try {
			JGraph jgraph = new JGraph(new JGraphModelAdapter<SemanticNode<?>, SemanticEdge>(this.graph));
			JGraphFacade facade = new JGraphFacade(jgraph);
			JGraphLayout layout = new JGraphHierarchicalLayout();
			layout.run(facade);
			@SuppressWarnings("rawtypes")
			Map nested = facade.createNestedMap(true, true);
			jgraph.getGraphLayoutCache().edit(nested);
			jgraph.setVisible(true);
			// Show in Frame
			JFrame frame = new JFrame();
			frame.getContentPane().add(new JScrollPane(jgraph));
			frame.pack();
			frame.setVisible(true);	
		} catch (IllegalArgumentException e) {
			// run(facade) sometimes triggers "Comparison method violates its general contract!"
			e.printStackTrace();
		}
	}
	
	public BufferedImage saveGraphAsImage(){
		BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.setFont(new Font("Arial Black", Font.BOLD, 5));
        g.drawString("graph not available", 0, 0);
		g.dispose();
		if (this.graph.vertexSet().isEmpty())
			return image;
		JGraph jgraph = new JGraph(new JGraphModelAdapter<SemanticNode<?>, SemanticEdge>(this.graph));
		JGraphFacade facade = new JGraphFacade(jgraph);
		JGraphLayout layout = new JGraphHierarchicalLayout();
		layout.run(facade);
		@SuppressWarnings("rawtypes")
		Map nested = facade.createNestedMap(true, true);
		jgraph.getGraphLayoutCache().edit(nested);
		jgraph.setVisible(true);
		// Show in Frame
		JScrollPane component = new JScrollPane(jgraph);
		JFrame frame = new JFrame();
		frame.setBackground(Color.WHITE);
		frame.setUndecorated(true);
		frame.getContentPane().add(new JScrollPane(jgraph));
		frame.pack();
		frame.setLocation(-2000, -2000);
		frame.setVisible(true);	
		try
		{
			image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);		
			Graphics2D graphics2D = image.createGraphics();
			frame.paint(graphics2D);	
			component.print(graphics2D);
			graphics2D.dispose();
			frame.dispose();
			//ImageIO.write(image,"png", new File(imagePath));
		}
		catch(Exception exception)
		{
			//code
		}
		return image;


//Create image from graph
/*JGraphModelAdapter<SemanticNode<?>, SemanticEdge> graphModel = new JGraphModelAdapter<SemanticNode<?>, SemanticEdge>(this.graph);
JGraph jgraph = new JGraph (graphModel);
BufferedImage image = jgraph.getImage(Color.WHITE, 5);
try {
	ImageIO.write(image, "PNG", new File("/Users/kkalouli/Desktop/img.png"));
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

mxGraph graphMx = new mxGraph();

for (SemanticEdge edge : graph.getEdges()){
	SemanticNode<?> start = graph.getStartNode(edge);
	SemanticNode<?> finish = graph.getEndNode(edge);
	graphMx.insertVertex(graphMx.getDefaultParent(), "Start", "Start", 0.0, 0.0, 50.0, 30.0, "rounded");
	graphMx.insertVertex(graphMx.getDefaultParent(), "Ende", "Ende", 0.0, 0.0, 50.0, 30.0, "rounded");

	graphMx.insertEdge(graphMx.getDefaultParent(), null, "", ((mxGraphModel)graphMx.getModel()).getCell("Start"), ((mxGraphModel)graphMx.getModel()).getCell("Ende"));
	
}

graphMx.insertVertex(graphMx.getDefaultParent(), "Start", "Start", 0.0, 0.0, 50.0, 30.0, "rounded");
graphMx.insertVertex(graphMx.getDefaultParent(), "Ende", "Ende", 0.0, 0.0, 50.0, 30.0, "rounded");

graphMx.insertEdge(graphMx.getDefaultParent(), null, "", ((mxGraphModel)graphMx.getModel()).getCell("Start"), ((mxGraphModel)graphMx.getModel()).getCell("Ende"));

mxIGraphLayout layout = new mxHierarchicalLayout(graphMx);
layout.execute(graphMx.getDefaultParent());

BufferedImage image1 = mxCellRenderer.createBufferedImage(graphMx, null, 1, Color.WHITE, true, null);
try {
	ImageIO.write(image1, "PNG", new File("/Users/kkalouli/Desktop/img1.png"));
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

/*JGraph jgraph = new JGraph(new JGraphModelAdapter<SemanticNode<?>, SemanticEdge>(this.graph));
ListenableDirectedGraph<SemanticNode<?>, SemanticEdge> g = new ListenableDirectedGraph<SemanticNode<?>, SemanticEdge>(graph);	
// create a visualization using JGraph, via an adapter
jgxAdapter = new JGraphXAdapter<String, DefaultEdge>(g);

    getContentPane().add(new mxGraphComponent(jgxAdapter));
BufferedImage image = mxCellRenderer.createBufferedImage(g, null, 1, Color.WHITE, true, null);
ImageIO.write(image, "PNG", new File("C:\\Temp\\graph.png"));*/
}

	



	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void display(Map<Color, List<SemanticNode<?>>> nodeProperties,
			Map<Color, List<SemanticEdge>> edgeProperties) {
		try {
			// Get basic graph with hierarchical layout
			JGraphModelAdapter<SemanticNode<?>, SemanticEdge> jgAdapter = new JGraphModelAdapter<SemanticNode<?>, SemanticEdge>(this.graph);
			JGraph jgraph = new JGraph(jgAdapter);
			JGraphFacade facade = new JGraphFacade(jgraph);
			JGraphLayout layout = new JGraphHierarchicalLayout();
			layout.run(facade);
			Map nested = facade.createNestedMap(true, true);
			jgraph.getGraphLayoutCache().getModel().beginUpdate();
			jgraph.getGraphLayoutCache().edit(nested);

			// Add additional node and edge properties
			// Currently, only colours
			Map nested1 = new HashMap();
			for (Entry<Color, List<SemanticNode<?>>> kv : nodeProperties.entrySet()) {
				Map nodeColor = new HashMap();
				GraphConstants.setBackground(nodeColor, kv.getKey());
				for (SemanticNode<?> n : kv.getValue()) {
					DefaultGraphCell cell = jgAdapter.getVertexCell(n);
					nested1.put(cell, nodeColor);
				}
			}
			for (Entry<Color, List<SemanticEdge>> kv : edgeProperties.entrySet()) {
				Map edgeColor = new HashMap();
				GraphConstants.setLineColor(edgeColor, kv.getKey());
				for (SemanticEdge e : kv.getValue()) {
					DefaultGraphCell cell = jgAdapter.getEdgeCell(e);
					nested1.put(cell, edgeColor);
				}
			}
			jgraph.getGraphLayoutCache().getModel().beginUpdate();
			jgraph.getGraphLayoutCache().edit(nested1);
			jgraph.getGraphLayoutCache().getModel().endUpdate();
			jgraph.getGraphLayoutCache().getModel().endUpdate();

			// Show in Frame
			JFrame frame = new JFrame();
			frame.getContentPane().add(new JScrollPane(jgraph));
			frame.pack();
			frame.setVisible(true);		
		} catch (IllegalArgumentException e) {
			// run(facade) sometimes triggers "Comparison method violates its general contract!"
			e.printStackTrace();
		}
	}

	@Override
	public SemGraph getSubGraph(Set<SemanticNode<?>> nodes,
			Set<SemanticEdge> edges) {
		DirectedSubgraph<SemanticNode<?>, SemanticEdge> subGraph = new DirectedSubgraph<SemanticNode<?>, SemanticEdge>(this.graph, nodes, edges);
		return new SemJGraphT(subGraph);
	}



}
