package sem.mapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import semantic.graph.EdgeContent;
import semantic.graph.SemGraph;
import semantic.graph.SemanticEdge;
import semantic.graph.SemanticGraph;
import semantic.graph.SemanticNode;
import semantic.graph.vetypes.ContextHeadEdge;
import semantic.graph.vetypes.ContextNode;
import semantic.graph.vetypes.ContextNodeContent;
import semantic.graph.vetypes.GraphLabels;
import semantic.graph.vetypes.RoleEdge;
import semantic.graph.vetypes.RoleEdgeContent;
import semantic.graph.vetypes.SkolemNode;
import semantic.graph.vetypes.SkolemNodeContent;

public class ContextMapper {
	private semantic.graph.SemanticGraph graph;
	private SemGraph depGraph;
	private HashMap<SemanticNode<?>,SemanticNode<?>> negCtxs;
	private HashMap<SemanticNode<?>,String> coordCtxs;
	private boolean coord;
	private boolean verbCoord;
	private boolean clausalCoord;
	private boolean disjunction;
	private HashMap<SemanticNode<?>,String> implCtxs;
	private HashMap<String,String> mapOfImpl;
	private ArrayList<String> verbalForms;
	
	public ContextMapper(semantic.graph.SemanticGraph graph, ArrayList<String> verbalForms){
		this.verbalForms = verbalForms;
		this.graph = graph;
		this.depGraph = this.graph.getDependencyGraph();
		this.negCtxs = new HashMap<SemanticNode<?>,SemanticNode<?>>();
		this.coordCtxs = new HashMap<SemanticNode<?>,String>();
		this.coord = false;
		this.verbCoord = false;
		this.disjunction = false;
		this.implCtxs = new HashMap<SemanticNode<?>,String>();
		
		// read the file with the implicative/factive signatures
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream("implicatives3.txt"), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String strLine;
		this.mapOfImpl = new HashMap<String,String>();
		//store the file in a hashmap. key: the stem of the word and its complement (that/to). Value: the truth value
		try {
			while ((strLine = br.readLine()) != null) { 
				String sign = strLine.substring(0,strLine.lastIndexOf("_"));
				String words = strLine.substring(strLine.indexOf("= ")+2);
				String comple = strLine.substring(strLine.lastIndexOf("_"), strLine.indexOf("=")-1);
				for (String word : words.split(" ")){
					word = word.replace(",", "");
					mapOfImpl.put(word+comple,sign);
				}		
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	/**
	 * Integrates all possible contexts of the sentence. 
	 * @throws IOException 
	 */
	public void integrateAllContexts(){
		try {
			integrateImplicativeContexts();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		integrateCoordinatingContexts();
		integrateNegativeContexts();
		integrateModalContexts();
		if (coord == false)
			checkForPostIntegrationMistakes();
		SemGraph conGraph = graph.getContextGraph();	
		SemanticNode<?> predNode = null;
		// make sure that all "root" nodes existing in the role graph have been added to the context graph; if not add them now
		for (SemanticNode<?> node : graph.getRoleGraph().getNodes()){
			boolean foundNode = false;
			// do the following only for the root nodes: the ones without inNeighbors
			if (graph.getRoleGraph().getInNeighbors(node).isEmpty()){
				for (SemanticNode<?> ctxNode : graph.getContextGraph().getNodes()){
					// check if this node or its context exist in the context graph
					if (ctxNode.equals(node) || ctxNode.getLabel().equals("ctx("+node.getLabel()+")")){
						foundNode = true;
					}
				}
				// if it doesnt, add it now to the contxex graph
				if (foundNode == false)
					addSelfContextNodeAndEdgeToGraph(node);
			// if there are no "root" nodes at all (all nodes have incoming edges), then there is probably a copula verb with a relative clause 
			} else{
				for (SemanticEdge ed : graph.getRoleGraph().getInEdges(node)){
					// get the head of the relative clause which is also the predicate ans store it in the predNode
					if (ed.getLabel().equals("rstr")){
						predNode = graph.getStartNode(ed);
						break;
					}
				}
				
			}

		}		
		// if the conGraph does not have any ndoes up until now, add the predNode as ctx
		if (conGraph.getNodes().isEmpty() && predNode != null){
			addSelfContextNodeAndEdgeToGraph(predNode);
		}
		// make sure that the context graph is left with one top context; if there are more than one "tops", then
		//merge the two tops to one
		int noOfTops = 0;
		ArrayList<SemanticNode<?>> listOfTops = new ArrayList<SemanticNode<?>>();
		SemanticNode<?> firstTop = null;
		for (SemanticNode<?> cNode: conGraph.getNodes()){			
			// if no incoming edges, it is the top context
			if (conGraph.getInEdges(cNode).isEmpty()){
				noOfTops ++;
				// if there are more than one tops,
				if (noOfTops > 1){
					// get all the edges of this top and add them to the first top which was found
					for (SemanticEdge edge : graph.getContextGraph().getEdges(cNode)){
						ContextHeadEdge ctxEdge = new ContextHeadEdge(edge.getLabel(), new RoleEdgeContent());
						graph.addContextEdge(ctxEdge, firstTop, graph.getFinishNode(edge));
					}
					listOfTops.add(cNode);
					continue;
				}
				// otherwise (=if it is the first top), set the label to top
				cNode.setLabel("top");
				firstTop = cNode;
			} 
		}
		// remove all edges of the other tops
		for (SemanticNode<?> top : listOfTops){
			graph.removeContextNode(top);
		}
			
	}
	
	/**
	 * Check if there are any postintegration changes necessary.
	 * Current case: there is an implcative verb and its embedded verb is negated, e.g. She knew that he had not arrived yet.
	 * In such cases there are still two top contexts involved: the top context of the implicative verb and the top contect of the 
	 * negated verb. Thus, the negated context has to be embedded into the implicative context.  
	 */
	private void checkForPostIntegrationMistakes(){
		HashMap<String, SemanticNode<?>> nodeLabels = new HashMap<String, SemanticNode<?>>();
		ArrayList<SemanticNode<?>> sameNodes = new ArrayList<SemanticNode<?>>();	
		// find any ctx nodes that are exactly the same because those are going to be merged into one
		for (SemanticNode<?> cNode: graph.getContextGraph().getNodes()){
			if (!nodeLabels.containsKey(cNode.getLabel())){
				nodeLabels.put(cNode.getLabel(), cNode);
			}
			else {
				sameNodes.add(cNode);
				sameNodes.add(nodeLabels.get(cNode.getLabel()));
			}
		}
		// initialize the nodes and edges that are going to be added
		SemanticNode<?> top = null;
		SemanticEdge connector = null;
		SemanticNode<?> nodeToEmbed = null;
		ArrayList<SemanticNode<?>> nodesToRemove = new ArrayList<SemanticNode<?>>();
		ArrayList<SemanticEdge> edgesToRemove = new ArrayList<SemanticEdge>();
		if (sameNodes.isEmpty())
			return;
		// go through the same nodes
		for (SemanticNode<?> node : sameNodes){
			// find the context in which they belong
			SemanticNode<?> ctx = graph.getContextGraph().getInNeighbors(node).iterator().next();
			SemanticNode<?> ctxHead = null;
			// get the head of the context they belong
			for (SemanticEdge out : graph.getContextGraph().getOutEdges(ctx)){
				if (out.getLabel().equals("ctx_hd"))
					ctxHead = graph.getFinishNode(out);
			}	
			// if this head is implicative, then this is the head to be taken as the top context (and the other one will be embedded into that)
			if (implCtxs.containsKey(ctxHead)){
				top = ctx;
				connector = graph.getContextGraph().getEdges(ctx,node).iterator().next();
				// do not put the head node of this node to the nodesToRemove
				for (SemanticEdge out : graph.getContextGraph().getOutEdges(node)){
					if (out.getLabel().equals("ctx_hd"))
						continue;
					else
						nodesToRemove.add(graph.getFinishNode(out));
				}			
				edgesToRemove.addAll(graph.getContextGraph().getOutEdges(node));
				nodesToRemove.add(node);
				edgesToRemove.add(connector);
			// if it's not implicative, then it's gonna be the one to be embedded
			} else {
				nodeToEmbed = ctx;
			}		
		}
		// add the new edge between top and nodeToEmbed
		ContextHeadEdge ctxEdge = new ContextHeadEdge(connector.getLabel(), new RoleEdgeContent());
		graph.addContextEdge(ctxEdge, top, nodeToEmbed);
		
		// remove all things to remove
		for (SemanticNode<?> n:  nodesToRemove){
			graph.removeContextNode(n);
		}
		for (SemanticEdge e:  edgesToRemove){
			graph.removeContextEdge(e);
		}
	}

	/**
	 * Integrates any modal contexts of the sentence. 
	 */
	private void integrateModalContexts(){
		Set<SemanticNode<?>> nodes = depGraph.getNodes();
		for (SemanticNode<?> node : nodes){	
			if (((SkolemNodeContent) node.getContent()).getStem().equals("might") 
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("should")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("may")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("must")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("can")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("could")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("ought")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("need")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("would")){
				// create the self node of the modal
				SemanticNode<?> modalNode = addSelfContextNodeAndEdgeToGraph(node);
				// get the head of the modal
				SemanticNode<?> head = depGraph.getInNeighbors(node).iterator().next();
				//System.out.println(head);
				SemanticNode<?> headNode  = null;
				// initializing the node of the negation of the modal (negModalNode) to see if there is negation
				// over the modal or somewhere else in the sentence
				SemanticNode<?> negModalNode = null;
				for (Entry<SemanticNode<?>, SemanticNode<?>> e : negCtxs.entrySet()) {
					SemanticNode<?> key = e.getKey();
					SemanticNode<?> value = e.getValue();
					if (value.equals(head)){
						negModalNode = key;
					}
				}		
				// create the head node and edge between the modal and its verb head only if there is no negation involved
				// or if there is no negation of the modal but of another word
				if (negCtxs.isEmpty() || negModalNode == null){
					// create the self node of the head
					headNode = addSelfContextNodeAndEdgeToGraph(head);
					String label = ((SkolemNodeContent) node.getContent()).getStem();
					ContextHeadEdge labelEdgeHead = new ContextHeadEdge(label, new  RoleEdgeContent());
					graph.addContextEdge(labelEdgeHead, modalNode, headNode);
					// re-set the context of the head of the modal to the modal itself
					((SkolemNodeContent) head.getContent()).setContext(headNode.getLabel());
				} 
				/* if negation is involved, then there are two cases:
				 * 1. negation over the modal (can, could, would): here the neg context graph is practically 
				 * disolved and we just keep the negation context node which is then used as top for the 
				 * modal context graph to eb created
				 */
				else if (((SkolemNodeContent) node.getContent()).getStem().equals("can") 
						|| ((SkolemNodeContent) node.getContent()).getStem().equals("could")
						|| ((SkolemNodeContent) node.getContent()).getStem().equals("need")
						|| ((SkolemNodeContent) node.getContent()).getStem().equals("would")){
					// remove all edges and nodes that are dependent on the negation_context node
					for (SemanticNode<?> sNode : graph.getContextGraph().getOutNeighbors(negModalNode)){
						for (SemanticEdge sEdge : graph.getContextGraph().getEdges(sNode)){
							if (sEdge.getLabel().equals("antiveridical")){
								headNode = graph.getContextGraph().getEndNode(sEdge);
								graph.getContextGraph().removeEdge(sEdge);
							}					
						}
					}
					// create the edge between the modal and its head
					String label = ((SkolemNodeContent) node.getContent()).getStem();
					ContextHeadEdge labelEdgeHead = new ContextHeadEdge(label, new  RoleEdgeContent());
					graph.addContextEdge(labelEdgeHead, modalNode, headNode);		
					// create and edge between the negation_context node and the modal (so that the negation is over the modal)
					ContextHeadEdge labelEdgeCan = new ContextHeadEdge(GraphLabels.ANTIVER, new  RoleEdgeContent());
					graph.addContextEdge(labelEdgeCan, negModalNode, modalNode);
					// re-set the context of the head of the modal to the ctx of that head  
					((SkolemNodeContent) head.getContent()).setContext(headNode.getLabel());
					// re-set the context of the modal to the ctx of that modal
					((SkolemNodeContent) node.getContent()).setContext(modalNode.getLabel());	
				} 
				/*
				 * 2. negation is under the modal (all other modals): here the negation context graph is combined with the
				 * modal graph to be created in a way that the negated graph is embedded under the modal graph
				 */
				else {
					String label = ((SkolemNodeContent) node.getContent()).getStem();
					ContextHeadEdge labelEdgeHead = new ContextHeadEdge(label, new  RoleEdgeContent());
					graph.addContextEdge(labelEdgeHead, modalNode, negModalNode);
					// resetting the contexts
					for (SemanticEdge s : graph.getContextGraph().getOutEdges(negModalNode)){
						if (s.getLabel().equals("ctx_hd")){
							SemanticNode<?> negNode = graph.getFinishNode(s);
							// re-set the context of the negation to its ctx
							((SkolemNodeContent) negNode.getContent()).setContext(negModalNode.getLabel());
						} else if (s.getLabel().equals("antiveridical")){
							headNode = graph.getFinishNode(s);
							// re-set the context of the head of the modal to the ctx of that head
							((SkolemNodeContent) head.getContent()).setContext(headNode.getLabel());
						}
					}

				}
			}
		}
	}
	
	/**
	 * It is called from the integrateNegativeContexts() and deals with the negation with not, none, never and neither. 
	 * Not can be negating a predicate or a quantifier: 
	 * a. The dog is not carrying the stick.
	 * b. Not many people came.
	 * In a. the head of the negation is a predicate and all arguments exist in top. In b. the head of the negation is
	 * the head of the noun that the negation is modifying. All arguments exist in top.
	 * None is always depending on a noun phras, e.g. none of the students, but again the predicate is begin negated.
	 * Neither is negating both parts of the coordination; the predicate is being negated.
	 * Never is negating the predicate.
	 * @param node
	 * @param negNode
	 * @param previousNegNode
	 */
	private void integrateNotContexts(SemanticNode<?> node, SemanticNode<?> negNode, SemanticNode<?> previousNegNode){
		// initialize the head of the negation
		SemanticNode<?> head = null;
		// initialize the context of the head of the negation
		SemanticNode<?> ctxHead = null;
		// get the dep head
		SemanticNode<?> depHead = depGraph.getInNeighbors(node).iterator().next();
		//If we have verbal coordination, then the combined node of the two predicates has to be negated
		if (coordCtxs.containsKey(depHead) && coordCtxs.get(depHead).equals("verbal")){
			// find the role head of this dep head
			head = graph.getRoleGraph().getInNeighbors(depHead).iterator().next();
			// if the negated node doesnt already exist (in cases of combined predicates), then add the self node of the negation
			if (!graph.getContextGraph().containsNode(previousNegNode)){
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
			}
			// set the previousNegNode to the current one.
			previousNegNode = negNode;
			/* if there is disjunction in the sentence (and not only conjunction), then we have to first find the context of the
			 * current node-predicate and then find the context of this context.
			 *  For that we need to get the ctxOfDepHead (= the context of the current node-predicate)
			and ctxHead (= the context of the combined node)
			 */
			if (disjunction == true){
				SemanticNode<?> ctxOfDepHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
				ctxHead = graph.getContextGraph().getInNeighbors(ctxOfDepHead).iterator().next();
				/*
				 * if there is also clausal coordination, then the edge linking the top node with the combined node of the predicates
				 * should be removed and a new edge from the top to the negated context should be created.
				 */
				if (clausalCoord == true) {
					SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
					SemanticNode<?> top = graph.getStartNode(orEdge);
					graph.removeContextEdge(orEdge);
					ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
					graph.addContextEdge(label, top, negNode);
				}
				// if there is no disjunction, then we just need to find the context of the current node-predicate. 
			} else {
				// find the context of the current node-predicate
				ctxHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
			}
			/*
			 * if we dont have verbal coordination, but either clausal or noun/adject/etc, or if we have coord generally, 
			 *  then the coordinated nodes have to be added to their parent. 
			 */
		} else if ((coordCtxs.containsKey(depHead) && !coordCtxs.get(depHead).equals("verbal")) || coord == true) {
			/*
			 * If there is dijunction and also clausal coordination, then the edge linking the top node with the current node
			 * should be removed and a new edge from the top to the negated context should be created.
			 */
			if (disjunction == true && coordCtxs.containsKey(depHead) && coordCtxs.get(depHead).equals("clausal")){
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
				ctxHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
				SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
				SemanticNode<?> top = graph.getStartNode(orEdge);
				graph.removeContextEdge(orEdge);
				ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
				graph.addContextEdge(label, top, negNode);
				// if no disjunction, then just create the self neg node and get the parent of the current node	
			} else {
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
				if (!verbalForms.contains(((SkolemNodeContent) depHead.getContent()).getPosTag())){
					SemanticNode<?> headOfHead = depGraph.getInNeighbors(depHead).iterator().next();
					head = headOfHead;
				} else {
					head = depGraph.getInNeighbors(node).iterator().next();
				}
				// create the self node of the head of the negation
				ctxHead = addSelfContextNodeAndEdgeToGraph(head);
			}
			//If there is no coordination but simple negation.
		} else {
			// create the self node of the negation
			negNode = addSelfContextNodeAndEdgeToGraph(node);
			// first if to deal with none negation
			if (!verbalForms.contains(((SkolemNodeContent) depHead.getContent()).getPosTag())){
				SemanticNode<?> headOfHead = depGraph.getInNeighbors(depHead).iterator().next();
				head = headOfHead;
			} else {				
				head = depHead;
			}
			// create the self node of the head of the negation
			ctxHead = addSelfContextNodeAndEdgeToGraph(head);
		}

		// create and add the edge between the negation and its head 
		ContextHeadEdge labelEdge = new ContextHeadEdge(GraphLabels.ANTIVER, new  RoleEdgeContent());
		graph.addContextEdge(labelEdge, negNode, ctxHead);	
		// put the negated node created and its head to the hash
		negCtxs.put(negNode, head);
		// re-set the context of the head of the negation to the ctx of that head
		if (head instanceof SkolemNode)
			((SkolemNodeContent) head.getContent()).setContext(ctxHead.getLabel());
		setContextsRecursively(head, head);
		//graph.displayContexts();
	}
	
	/**
	 * It is called from integrateNegativeContexts() and deals with the sentences:
	 * No dog is carrying a stick.
	 * The dog is carrying no stick.
	 * --> the dog and the stick exist only in the ctx of the carry and the head of the negation is a NP
	 * @param node
	 * @param negNode
	 * @param previousNegNode
	 */
	private void integrateNoContexts(SemanticNode<?> node, SemanticNode<?> negNode, SemanticNode<?> previousNegNode){
		// get the head of the negation
		SemanticNode<?> head = depGraph.getInNeighbors(node).iterator().next();;
		SemanticNode<?> ctxHead = null;
		// get the head of the head in order to hopefully reach the main verb
		SemanticNode<?> headOfHead = depGraph.getInNeighbors(head).iterator().next();
		//If we have verbal coordination, then the combined node of the two predicates has to be negated
		if (coordCtxs.containsKey(headOfHead) && coordCtxs.get(headOfHead).equals("verbal")){
			if (!graph.getContextGraph().containsNode(previousNegNode)){
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
			}
			previousNegNode = negNode;
			/* if there is disjunction in the sentence (and not only conjunction), then we have to first find the context of the
			 * current headOfHead and then find the context of this context.
			 *  For that we need to get the ctxOfDepHead (= the context of the current node-predicate)
			and ctxHead (= the context of the combined node)
			 */
			if (disjunction == true){
				SemanticNode<?> ctxOfDepHead = graph.getContextGraph().getInNeighbors(headOfHead).iterator().next();
				ctxHead = graph.getContextGraph().getInNeighbors(ctxOfDepHead).iterator().next();
				/*
				 * if there is also clausal coordination, then the edge linking the top node with the combined node of the predicates
				 * should be removed and a new edge from the top to the negated context should be created.
				 */
				if (clausalCoord == true) {
					SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
					SemanticNode<?> top = graph.getStartNode(orEdge);
					graph.removeContextEdge(orEdge);
					ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
					graph.addContextEdge(label, top, negNode);
				}
				// if there is no disjunction
			} else {
				// find the context head of the head of the head
				ctxHead = graph.getContextGraph().getInNeighbors(headOfHead).iterator().next();
			}
			// set the context of the children of this context to this context
			for (SemanticNode<?> child : graph.getOutNeighbors(ctxHead)){
				if (child instanceof SkolemNode)
					((SkolemNodeContent) child.getContent()).setContext(ctxHead.getLabel());
				setContextsRecursively(child, child);
			}
			/*
			 * if we dont have verbal coordination, but either clausal or noun/adject/etc, or if we have coord generally, 
			 *  then the coordinated nodes have to be linked to their parent. 
			 */
		} else if ((coordCtxs.containsKey(headOfHead) && !coordCtxs.get(headOfHead).equals("verbal")) || coord == true) {
			/*
			 * If there is dijunction and also clausal coordination, then the edge linking the top node with the current node
			 * should be removed and a new edge from the top to the negated context should be created.
			 */
			if (disjunction == true && coordCtxs.containsKey(headOfHead) && coordCtxs.get(headOfHead).equals("clausal")){
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
				ctxHead = graph.getContextGraph().getInNeighbors(headOfHead).iterator().next();
				SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
				SemanticNode<?> top = graph.getStartNode(orEdge);
				graph.removeContextEdge(orEdge);
				ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
				graph.addContextEdge(label, top, negNode);		
				// if no disjunction, then just create the self neg node and get the parent of the current node	
			} else {
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
				head = depGraph.getInNeighbors(node).iterator().next();
				// create the self node of the head of the negation
				ctxHead = addSelfContextNodeAndEdgeToGraph(headOfHead);
			}
			//If there is no coordination but simple negation.
		} else {
			// create the self node of the negation
			negNode = addSelfContextNodeAndEdgeToGraph(node);
			head = depGraph.getInNeighbors(node).iterator().next();
			// create the self node of the head of the negation
			ctxHead = addSelfContextNodeAndEdgeToGraph(headOfHead);
		}

		// create and add the edge between the negation and the head of its head 
		ContextHeadEdge labelEdge = new ContextHeadEdge(GraphLabels.ANTIVER, new  RoleEdgeContent());
		graph.addContextEdge(labelEdge, negNode, ctxHead);
		// the negated argument must be instantiated within the context of its head; it doesnt belong to top anymore		
		ContextHeadEdge instEdge = new ContextHeadEdge(GraphLabels.VER, new  RoleEdgeContent());
		graph.addContextEdge(instEdge, ctxHead, head);		
		// the existing ctx of the negated argument is set to the next context
		if (head instanceof SkolemNode)
			((SkolemNodeContent) head.getContent()).setContext(ctxHead.getLabel());	
		setContextsRecursively(head, head);
		// put the negated node created and its head to the hash
		negCtxs.put(negNode, headOfHead);
		// re-set the context of the head of the head of the negation to the ctx of that head
		if (headOfHead instanceof SkolemNode)
			((SkolemNodeContent) headOfHead.getContent()).setContext(ctxHead.getLabel());
		setContextsRecursively(headOfHead, headOfHead);

		/*
		 * if there is coordination (noun, adj, adv), then the children of this coordination should be added as 
		 * instantiable nodes to their mother context since they dont exist in top anymore. Their conext should also
		 * be adjusted.
		 */
		if (coord == true) {
			SemanticNode<?> combHead = graph.getRoleGraph().getInNeighbors(head).iterator().next();
			for (SemanticNode<?> child : graph.getRoleGraph().getOutNeighbors(combHead)){
				if (child instanceof SkolemNode && !child.equals(head)) {
					((SkolemNodeContent) child.getContent()).setContext(ctxHead.getLabel());
					// the negated argument must be instantiated within the context of its head; it doesnt belong to top anymore		
					ContextHeadEdge instEdge2 = new ContextHeadEdge(GraphLabels.VER, new  RoleEdgeContent());
					graph.addContextEdge(instEdge2, ctxHead, child);
				}
				setContextsRecursively(child, child);
			}
		}	
	}
	
	/**
	 * It is called from integrateNegativeContexts() and deals with the sentences:
	 * Nobody is carrying a stick.
	 * The dog is carrying nothing.
	 * --> the dog and the stick exist only in the ctx of the carry and the head of the negation is a predicate
	 * @param node
	 * @param negNode
	 * @param previousNegNode
	 */
	private void integrateNobodyContexts(SemanticNode<?> node, SemanticNode<?> negNode, SemanticNode<?> previousNegNode){
		SemanticNode<?> head = null;
		SemanticNode<?> ctxHead = null;
		// get the head of the negation
		SemanticNode<?> depHead = depGraph.getInNeighbors(node).iterator().next();			
		//If we have verbal coordination, then the combined node of the two predicates has to be negated
		if (coordCtxs.containsKey(depHead) && coordCtxs.get(depHead).equals("verbal")){
			// find the role head of this dep head
			head = graph.getRoleGraph().getInNeighbors(depHead).iterator().next();
			if (!graph.getContextGraph().containsNode(previousNegNode)){
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
			}
			previousNegNode = negNode;
			/* if there is disjunction in the sentence (and not only conjunction), then we have to first find the context of the
			 * current node-predicate and then find the context of this context.
			 *  For that we need to get the ctxOfDepHead (= the context of the current node-predicate)
			and ctxHead (= the context of the combined node)
			 */
			if (disjunction == true){
				SemanticNode<?> ctxOfDepHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
				ctxHead = graph.getContextGraph().getInNeighbors(ctxOfDepHead).iterator().next();
				/*
				 * if there is also clausal coordination, then the edge linking the top node with the combined node of the predicates
				 * should be removed and a new edge from the top to the negated context should be created.
				 */
				if (clausalCoord == true) {
					SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
					SemanticNode<?> top = graph.getStartNode(orEdge);
					graph.removeContextEdge(orEdge);
					ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
					graph.addContextEdge(label, top, negNode);
				}
			// if no disjunction, then just create the self neg node and get the parent of the current node	
			} else {
				// find the context head of this role head
				ctxHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
			}
			/*
			 * if we dont have verbal coordination, but either clausal or noun/adject/etc, or if we have coord generally, 
			 *  then the coordinated nodes have to be linked to their parent. 
			 */
		} else if ((coordCtxs.containsKey(depHead) && !coordCtxs.get(depHead).equals("verbal")) || coord == true) {
			/*
			 * If there is dijunction and also clausal coordination, then the edge linking the top node with the current node
			 * should be removed and a new edge from the top to the negated context should be created.
			 */
			if (disjunction == true && coordCtxs.containsKey(depHead) && coordCtxs.get(depHead).equals("clausal")){
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
				ctxHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
				SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
				SemanticNode<?> top = graph.getStartNode(orEdge);
				graph.removeContextEdge(orEdge);
				ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
				graph.addContextEdge(label, top, negNode);	
			// if no disjunction, then just create the self neg node and get the parent of the current node	
			} else {
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
				head = depGraph.getInNeighbors(node).iterator().next();
				// create the self node of the head of the negation
				ctxHead = addSelfContextNodeAndEdgeToGraph(head);
			}			
		} //If there is no coordination but simple negation.
		else {
		// create the self node of the negation
		negNode = addSelfContextNodeAndEdgeToGraph(node);
		head = depGraph.getInNeighbors(node).iterator().next();
		// create the self node of the head of the negation
		ctxHead = addSelfContextNodeAndEdgeToGraph(head);
	}

		// create and add the edge between the negation and the head of its head 
		ContextHeadEdge labelEdge = new ContextHeadEdge(GraphLabels.ANTIVER, new  RoleEdgeContent());
		graph.addContextEdge(labelEdge, negNode, ctxHead);
		//depending on the word, we need a new node to carry the meaning
		SemanticNode<?> finish = null;
		if (((SkolemNodeContent) node.getContent()).getStem().equals("nobody") ){
			// create new skolem node with new content; we need a skolem node and not jsut a context node
			// because we want to save the context info of this node in itself
			SkolemNodeContent content = new SkolemNodeContent();
			content.setSurface("person");
			content.setStem("person");
			content.setPosTag("NN");
			content.setPartOfSpeech("NN");
			// the position is set to be the negative position of the word it is replacing, e.g. nobody
			content.setPosition(((SkolemNodeContent) node.getContent()).getPosition()*-1);
			content.setDerived(false);		
			content.setSkolem("person"+"_"+content.getPosition());
			finish = new SkolemNode(content.getSkolem(), content);
		} else if (((SkolemNodeContent) node.getContent()).getStem().equals("nothing") ) {
			// create new skolem node with new content; we need a skolem node and not jsut a context node
			// because we want to save the context info of this node in itself
			SkolemNodeContent content = new SkolemNodeContent();
			content.setSurface("thing");
			content.setStem("thing");
			content.setPosTag("NN");
			content.setPartOfSpeech("NN");
			// the position is set to be the negative position of the word it is replacing, e.g. nobody
			content.setPosition(((SkolemNodeContent) node.getContent()).getPosition()*-1);
			content.setDerived(false);		
			content.setSkolem("thing"+"_"+content.getPosition());
			finish = new SkolemNode(content.getSkolem(), content);
		}

		// the negated argument must be instantiated within the context of its head; it doesnt belong to top anymore	
		ContextHeadEdge instEdge = new ContextHeadEdge(GraphLabels.VER, new  RoleEdgeContent());
		graph.addContextEdge(instEdge, ctxHead, finish);
		// the existing ctx of the negated argument is set to the next context
		((SkolemNodeContent) finish.getContent()).setContext(ctxHead.getLabel());
		setContextsRecursively(finish, finish);
		// put the negated node created and its head to the hash
		negCtxs.put(negNode, head);
		// re-set the context of the head of the negation to the ctx of that head
		if (head instanceof SkolemNode)
			((SkolemNodeContent) head.getContent()).setContext(ctxHead.getLabel());
		setContextsRecursively(head, head);
	}	


	/**
	 * Integrates all negative contexts of the sentence. 
	 */
	private void integrateNegativeContexts(){
		SemanticNode<?> negNode = null;
		Set<SemanticNode<?>> nodes = depGraph.getNodes();
		SemanticNode<?> previousNegNode = null;
		for (SemanticNode<?> node : nodes){
			/* First if clause deals with the negation with not, none, never and neither. Not can be negating a predicate or a quantifier: 
			 * a. The dog is not carrying the stick.
			 * b. Not many people came.
			 * In a. the head of the negation is a predicate and all arguments exist in top. In b. the head of the negation is
			 * the head of the noun that the negation is modifying. All arguments exist in top.
			 * None is always depending on a noun phras, e.g. none of the students, but again the predicate is begin negated.
			 * Neither is negatibe both parts of the coordination; the predicate is being negated.
			 * Never is negating the predicate.
			 */
			if (((SkolemNodeContent) node.getContent()).getStem().equals("not") 
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("n't")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("none")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("never")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("neither")){	
				integrateNotContexts( node, negNode, previousNegNode);
			}
			/*
			 * second else if deals with the sentences:
			 * No dog is carrying a stick.
			 * The dog is carrying no stick.
			 * --> the dog and the stick exist only in the ctx of the carry and the head of the negation is a NP
			 */
			else if (((SkolemNodeContent) node.getContent()).getStem().equals("no")){
				integrateNoContexts (node, negNode, previousNegNode);		
			} 
			/*
			 * third else if deals with the sentences:
			 * Nobody is carrying a stick.
			 * The dog is carrying nothing.
			 * --> the dog and the stick exist only in the ctx of the carry and the head of the negation is a predicate
			 */
			else if (((SkolemNodeContent) node.getContent()).getStem().equals("nobody") 
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("nothing") 
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("noone") ){
				integrateNobodyContexts( node, negNode, previousNegNode);
			}	
		}
	}


	/***
	 * Adds a context edge to the big graph. The context edge has the label "head" and starts from the ctx of the word
	 * and finishes at the word itself. 
	 * @param node
	 */
	private SemanticNode<?> addSelfContextNodeAndEdgeToGraph(SemanticNode<?> node){
		ContextHeadEdge cHEdge = new ContextHeadEdge(GraphLabels.CONTEXT_HEAD, new  RoleEdgeContent());
		SemanticNode<?> start = new ContextNode("ctx("+node.getLabel()+")", new ContextNodeContent());
		for (SemanticNode<?> contextN : graph.getContextGraph().getNodes()){
			if (start.getLabel().equals(contextN.getLabel()) && !verbalForms.contains(((SkolemNode) node).getPartOfSpeech())){
				start = contextN;
			}
		}
		graph.addContextEdge(cHEdge, start, node);
		return start;
	}

	/**
	 * Recursively sets the contexts of the children of the given parent node
	 * @param firstParent: the same as the parent of the 1st iteration. Then, the firstParent remains the same so that the context is always the same. 
	 * @param parent
	 */
	private void setContextsRecursively(SemanticNode<?> firstParent, SemanticNode<?> parent){
		Set<SemanticNode<?>> outNeighbors = graph.getRoleGraph().getOutNeighbors(parent);
		if (!outNeighbors.isEmpty()){
			for (SemanticNode<?> neighbor: outNeighbors){
				if (neighbor instanceof SkolemNode){
					if (!graph.getContextGraph().containsNode(neighbor) && verbalForms.contains(((SkolemNodeContent) neighbor.getContent()).getPosTag()) ) {
						((SkolemNodeContent) neighbor.getContent()).setContext(graph.getContextGraph().getInNeighbors(firstParent).iterator().next().getLabel());
					}
				}
				setContextsRecursively(firstParent,neighbor);
			}
		}
	}
	
	/**
	 * It is called from integrateCoordinatingContexts() and deals with disjunction: or, either/or
	 * @param edge
	 */
	private void integrateDisjunction(SemanticEdge edge){
		disjunction = true;
		// find the edge of the coord and get the start and the end nodes of this edge ( = the two parts of the coord)
		SemanticNode<?> start = depGraph.getStartNode(edge);
		SemanticNode<?> finish = depGraph.getEndNode(edge);
		// create the contexts of the two nodes and get them back
		SemanticNode<?> ctxOfStart = this.addSelfContextNodeAndEdgeToGraph(start);
		SemanticNode<?> ctxOfFinish = this.addSelfContextNodeAndEdgeToGraph(finish);
		/* check if the parents of the start node are not empty. if they are not empty, then there is no predicate coordination.*/
		if (!depGraph.getInNeighbors(start).isEmpty()){
			//get the parent of the coord
			SemanticNode<?> parentOfStart = depGraph.getInNeighbors(start).iterator().next();
			// get the context of the parent
			SemanticNode<?> ctxOfParent = this.addSelfContextNodeAndEdgeToGraph(parentOfStart);
			// create contexts from the parent to the coordinated children
			ContextHeadEdge labelStart = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
			// create the edges between the parent of the coord and the two coordinating nodes
			graph.addContextEdge(labelStart, ctxOfParent, ctxOfStart);
			ContextHeadEdge labelFinish = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
			graph.addContextEdge(labelFinish, ctxOfParent, ctxOfFinish);
			coord = true;
			//put the start and the finish into the hash of coordination (node:type of coordination). The hash will be needed when chcking for negative contexts.
			if (!coordCtxs.containsKey(start))
				coordCtxs.put(start, "other");
			if (!coordCtxs.containsKey(finish))
				coordCtxs.put(finish, "other");
			// if the parent of the start node are empty (= parents), then there is predicate coordination
		} else {
			// if the start and the finish are depending on other nodes in teh role graph, then there is no clausal coord but simple verbal one 
			if (!graph.getRoleGraph().getInNeighbors(start).isEmpty() && !graph.getRoleGraph().getInNeighbors(finish).isEmpty() ){
				// if we have verbal coordination, create a comb node of the two from whcih the two predicates will depend
				SemanticNode<?> combNode = graph.getRoleGraph().getInNeighbors(start).iterator().next();
				ContextNode top = new ContextNode("ctx("+combNode.getLabel()+")", new ContextNodeContent());
				graph.addNode(top);
				ContextHeadEdge labelStart = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
				graph.addContextEdge(labelStart, top, ctxOfStart);
				((SkolemNodeContent) start.getContent()).setContext(ctxOfStart.getLabel());
				ContextHeadEdge labelFinish = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
				graph.addContextEdge(labelFinish, top, ctxOfFinish);
				((SkolemNodeContent) finish.getContent()).setContext(ctxOfFinish.getLabel());
				// again, put everything int he hash
				verbCoord = true;
				if (!coordCtxs.containsKey(start) || (coordCtxs.containsKey(start) && coordCtxs.get(start).equals("clausal")))
					coordCtxs.put(start, "verbal");
				if (!coordCtxs.containsKey(finish))
					coordCtxs.put(finish, "verbal");
			// if they are not depending on any other nodes of the role graph, it means we have clausal coordination
			} else {
				// in this case we create a node top from which the two separate sentences depend
				ContextNode top = new ContextNode("top", new ContextNodeContent());
				graph.addNode(top);
				ContextHeadEdge labelStart = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
				// if there is also verbCoord within one of the sentences, then the combined node of this predicate should be used as the start
				if (verbCoord == true){
					ctxOfStart = graph.getContextGraph().getInNeighbors(ctxOfStart).iterator().next();
				}
				graph.addContextEdge(labelStart, top, ctxOfStart);
				((SkolemNodeContent) start.getContent()).setContext(ctxOfStart.getLabel());
				ContextHeadEdge labelFinish = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
				graph.addContextEdge(labelFinish, top, ctxOfFinish);
				((SkolemNodeContent) finish.getContent()).setContext(ctxOfFinish.getLabel());
				clausalCoord = true;
				// again, put everything int he hash
				if (!coordCtxs.containsKey(start))
					coordCtxs.put(start, "clausal");
				if (!coordCtxs.containsKey(finish))
					coordCtxs.put(finish, "clausal");
			}
		}
	}
	
	/**
	 * It is called from integrateCoordinatingContexts() and deals with conjunction: and, neither/nor
	 * @param edge
	 */
	private void integrateConjunction(SemanticEdge edge){
		// find the edge of the coord and get the start and the end nodes of this edge ( = the two parts of the coord)
		SemanticNode<?> start = depGraph.getStartNode(edge);
		SemanticNode<?> finish = depGraph.getEndNode(edge);
		// if the start and the finish are depending on other nodes in teh role graph, then there is no clausal coord but simple verbal one 
		if (!graph.getRoleGraph().getInNeighbors(start).isEmpty() && !graph.getRoleGraph().getInNeighbors(finish).isEmpty() ){
			/* check if the start node has a parent. If empty, there is predicate coordination.
			 * and we need to separate the two predicates*/
			if  (depGraph.getInNeighbors(start).isEmpty()) {
				SemanticNode<?> combNode = graph.getRoleGraph().getInNeighbors(start).iterator().next(); 
				ContextHeadEdge combEdge1 = new ContextHeadEdge(GraphLabels.CONTEXT_HEAD, new  RoleEdgeContent());
				ContextHeadEdge combEdge2 = new ContextHeadEdge(GraphLabels.CONTEXT_HEAD, new  RoleEdgeContent());
				SemanticNode<?> ctxOfCombNode = new ContextNode("ctx("+combNode.getLabel()+")", new ContextNodeContent());
				graph.addContextEdge(combEdge1, ctxOfCombNode, start);
				graph.addContextEdge(combEdge2, ctxOfCombNode, finish);
				verbCoord = true;
				// again, put everything int he hash. If the "start" verb is already in the hash and has the value "clausal"
				// then add it again with the type "verbal". The type "verbal" is then the main one for this verb.  
				if (!coordCtxs.containsKey(start) || (coordCtxs.containsKey(start) && coordCtxs.get(start).equals("clausal")))
					coordCtxs.put(start, "verbal");
				if (!coordCtxs.containsKey(finish))
					coordCtxs.put(finish, "verbal");
			// if we dont have predicate coordination, we have simple one
			} else {
				coord = true;
				if (!coordCtxs.containsKey(start))
					coordCtxs.put(start, "other");
				if (!coordCtxs.containsKey(finish))
					coordCtxs.put(finish, "other");
			}
		// if they are not depending on any other nodes of the role graph, it means we have clausal coordination	
		} else {
			clausalCoord = true;
			// again, put everything in the hash
			if (!coordCtxs.containsKey(start))
				coordCtxs.put(start, "clausal");
			if (!coordCtxs.containsKey(finish))
				coordCtxs.put(finish, "clausal");
		}
	}

	/***
	 * Integrates all coordinating contexts. 
	 */
	private void integrateCoordinatingContexts() {
		SemGraph depGraph = graph.getDependencyGraph();
		Set<SemanticEdge> edges = depGraph.getEdges();
		for (SemanticEdge edge : edges){
			// if there is disjunction
			if (edge.getLabel().equals("conj:or")){
				integrateDisjunction(edge);
			// if there is conjunction or there is neither/nor (in neither/nor both coordinated terms are negated and thus we have the same behavior as in conjunction)
			} else if (edge.getLabel().equals("conj:and")  || edge.getLabel().equals("conj:nor")){
				integrateConjunction(edge);
			}
		}
	}

	/**
	 * Integrates contexts as they are appearing from raise and control verbs.
	 */
	private void integrateClausalContexts(){
		SemGraph roleGraph = graph.getRoleGraph();
		Set<SemanticEdge> edges = roleGraph.getEdges();
		for (SemanticEdge edge : edges){
			if (edge.getLabel().equals("sem_comp") || edge.getLabel().equals("sem_xcomp")){
				SemanticNode<?> start = graph.getStartNode(edge);
				SemanticNode<?> finish = graph.getFinishNode(edge);
				if (!implCtxs.containsKey(start)){
					SemanticNode<?> ctxOfStart = this.addSelfContextNodeAndEdgeToGraph(start);
					SemanticNode<?> ctxOfFinish = this.addSelfContextNodeAndEdgeToGraph(finish);
					String label = start.getLabel().substring(0,start.getLabel().indexOf("_"));
					ContextHeadEdge labelEdge = new ContextHeadEdge(label, new  RoleEdgeContent());
					graph.addContextEdge(labelEdge,ctxOfStart, ctxOfFinish);
					if (finish instanceof SkolemNode)
						((SkolemNodeContent) finish.getContent()).setContext(ctxOfStart.getLabel());
				}
			}
		}
	}

	/**
	 * Integrates the implicative/factive contexts for the words the introduce such contexts. 
	 * The signatures of the words are looked up from a text file. 
	 * @throws IOException
	 */
	private void integrateImplicativeContexts() throws IOException{
		//go through each node of the role graph and see if it is such a word
		for (SemanticNode<?> node : graph.getRoleGraph().getNodes()){
			if (!(node instanceof SkolemNode))
				continue;
			String stem = (String) ((SkolemNodeContent) node.getContent()).getStem();
			// set the negation to false (negation alters the truth condition, thus we need it)
			boolean isNeg = false;
			// set the default complement to "n", "null", when there is no complement at all
			String comple = "_n";
			// go through the out edges of this node and extract negation and the complement, if present
			Set<SemanticEdge> edgesOfNode = depGraph.getOutEdges(node);			
			for (SemanticEdge edge : edgesOfNode){
				if (edge.getLabel().equals("neg"))
					isNeg = true;					
			}
			Set<SemanticEdge> roleEdgesOfNode = graph.getRoleGraph().getOutEdges(node);
			for (SemanticEdge edge : roleEdgesOfNode){
				if ( edge.getLabel().equals("sem_xcomp")){
					comple = "_to";
					break;
				}
				if (edge.getLabel().equals("sem_comp")){
					SemanticNode<?> endNode =graph.getFinishNode(edge);
					for (SemanticEdge outEdge : graph.getDependencyGraph().getOutEdges(endNode)){
						if (outEdge.getLabel().equals("mark")){
							comple = "_that";
						} 
					}
					break;
				}
				if (edge.getLabel().equals("sem_obj"))
					comple = "_other";
			}
			// if the hash does not contain a key with this stem and this complement, go to the next node
			if (!mapOfImpl.containsKey(stem+comple)){
				if (!comple.equals("_n"))
					comple = "_other";
				if (!mapOfImpl.containsKey(stem+comple)) 
					continue;
			}
			String truth;
			//System.out.println(mapOfImpl);
			// depending on whether there is negaiton or not, take the correct truth condition
			if (isNeg == true){
				truth = mapOfImpl.get(stem+comple).split("_")[1];
			} else{
				truth = mapOfImpl.get(stem+comple).split("_")[0];
			}
			// put the node into the hash with the implicatives
			implCtxs.put(node, "impl");
			ContextHeadEdge labelEdge = null;
			// depending on the turht value, create the correct edge
			if (truth.equals("N")){
				labelEdge = new ContextHeadEdge(GraphLabels.ANTIVER, new  RoleEdgeContent());
			} else if (truth.equals("P")){
				labelEdge = new ContextHeadEdge(GraphLabels.VER, new  RoleEdgeContent());
			}  else if (truth.equals("U")){
				labelEdge = new ContextHeadEdge(GraphLabels.AVER, new  RoleEdgeContent());
			}  
			// add the edge and the nodes to the context graph
			SemanticNode<?> ctxOfImpl = addSelfContextNodeAndEdgeToGraph(node);
			Set<SemanticNode<?>>children = graph.getRoleGraph().getOutNeighbors(node);
			boolean foundComplAsChild = false;
			for (SemanticNode<?> child : children){
				if (child instanceof SkolemNode){
					SemanticEdge edgeToChild = graph.getRoleGraph().getEdges(node, child).iterator().next();
					if (edgeToChild.getLabel().equals("sem_comp") || edgeToChild.getLabel().equals("sem_xcomp") ){
						SemanticNode<?> ctxOfChild = addSelfContextNodeAndEdgeToGraph(child);
						graph.addContextEdge(labelEdge,ctxOfImpl, ctxOfChild);
						((SkolemNodeContent) child.getContent()).setContext(ctxOfImpl.getLabel());
						foundComplAsChild = true;
					}
				}
			}
			if (foundComplAsChild == false){
				for (SemanticNode<?> child : children){
					if (child instanceof SkolemNode){
						if (graph.getRoleGraph().getEdges(node, child).iterator().next().getLabel().equals("sem_obj")){
							SemanticNode<?> ctxOfChild = addSelfContextNodeAndEdgeToGraph(child);
							graph.addContextEdge(labelEdge,ctxOfImpl, ctxOfChild);
							((SkolemNodeContent) child.getContent()).setContext(ctxOfImpl.getLabel());
						}
					}
				}	
			}	
		}
	}
}
