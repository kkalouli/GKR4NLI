package sem.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.robrua.nlp.bert.Bert;
import com.robrua.nlp.bert.FullTokenizer;

import edu.mit.jwi.IRAMDictionary;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.IntPair;
import jigsaw.JIGSAW;
import sem.graph.SemGraph;
import sem.graph.SemanticEdge;
import sem.graph.SemanticNode;
import sem.graph.vetypes.DefaultEdgeContent;
import sem.graph.vetypes.GraphLabels;
import sem.graph.vetypes.LexEdge;
import sem.graph.vetypes.LexEdgeContent;
import sem.graph.vetypes.LinkEdge;
import sem.graph.vetypes.PropertyEdge;
import sem.graph.vetypes.PropertyEdgeContent;
import sem.graph.vetypes.RoleEdge;
import sem.graph.vetypes.RoleEdgeContent;
import sem.graph.vetypes.SenseNode;
import sem.graph.vetypes.SenseNodeContent;
import sem.graph.vetypes.SkolemNode;
import sem.graph.vetypes.SkolemNodeContent;
import sem.graph.vetypes.ValueNode;
import sem.graph.vetypes.ValueNodeContent;

/**
 * Convert the Stanford graph to the whole GKR graph. 
 * @author Katerina Kalouli, 2017
 *
 */
public class DepGraphToSemanticGraph implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7281901236266524522L;
	private sem.graph.SemanticGraph graph;
	private SemanticGraph stanGraph;
	private ArrayList<String> verbalForms = new ArrayList<String>();
	private ArrayList<String> nounForms = new ArrayList<String>();
	private ArrayList<String> quantifiers = new ArrayList<String>();
	private ArrayList<String> whinterrogatives = new ArrayList<String>();
	private boolean interrogative;
	private List<SemanticGraphEdge> traversed;
	private StanfordParser parser;
	private SenseMappingsRetriever retriever;
	private InputStream configFile;


	/**
	 * Constructor to be used when DepGraphToSemanticGraph is called from the {@link InferenceComputer}.
	 * In this case, bert, bertTokenizer, the PWN Dict and the SUMo content are passed as parameters
	 * so that they are not called every time a new sentence is parsed
	 * @param bert
	 * @param tokenizer
	 * @param wnDict
	 * @param sumoContent
	 */
	public DepGraphToSemanticGraph(Bert bert, FullTokenizer tokenizer, IRAMDictionary wnDict, 
			String sumoContent) {
		verbalForms.add("MD");
		verbalForms.add("VB");
		verbalForms.add("VBD");
		verbalForms.add("VBG");
		verbalForms.add("VBN");
		verbalForms.add("VBP");
		verbalForms.add("VBZ");
		nounForms.add("NN");
		nounForms.add("NNP");
		nounForms.add("NNS");
		nounForms.add("NNPS");
		quantifiers.add("many");
		quantifiers.add("much");
		quantifiers.add("plenty");
		quantifiers.add("several");
		quantifiers.add("some");
		quantifiers.add("most");
		quantifiers.add("all");
		quantifiers.add("every");
		whinterrogatives.add("who");
		whinterrogatives.add("when");
		whinterrogatives.add("where");
		whinterrogatives.add("why");
		whinterrogatives.add("how");
		whinterrogatives.add("which");
		whinterrogatives.add("what");
		whinterrogatives.add("whose");
		whinterrogatives.add("whom");
		whinterrogatives.add("whether");
		whinterrogatives.add("if");
		this.graph = null;
		this.stanGraph = null;
		this.traversed = new ArrayList<SemanticGraphEdge>();
		try {
			this.parser = new StanfordParser();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.configFile = getClass().getClassLoader().getResourceAsStream("gkr.properties");
		this.retriever = new SenseMappingsRetriever(configFile, bert, tokenizer, wnDict, sumoContent);
		this.interrogative = false;
	}
	
	/**
	 * Constructor to be used when DepGraphToSemanticGraph is called from a main, only to
	 * parse specific sentences (without inference). Then, all libs are initialized here.
	 */
	public DepGraphToSemanticGraph() {
		verbalForms.add("MD");
		verbalForms.add("VB");
		verbalForms.add("VBD");
		verbalForms.add("VBG");
		verbalForms.add("VBN");
		verbalForms.add("VBP");
		verbalForms.add("VBZ");
		nounForms.add("NN");
		nounForms.add("NNP");
		nounForms.add("NNS");
		nounForms.add("NNPS");
		quantifiers.add("many");
		quantifiers.add("much");
		quantifiers.add("plenty");
		quantifiers.add("several");
		quantifiers.add("some");
		quantifiers.add("most");
		quantifiers.add("all");
		quantifiers.add("every");
		whinterrogatives.add("who");
		whinterrogatives.add("when");
		whinterrogatives.add("where");
		whinterrogatives.add("why");
		whinterrogatives.add("how");
		whinterrogatives.add("which");
		whinterrogatives.add("what");
		whinterrogatives.add("whose");
		whinterrogatives.add("whom");
		whinterrogatives.add("whether");
		whinterrogatives.add("if");
		this.graph = null;
		this.stanGraph = null;
		this.traversed = new ArrayList<SemanticGraphEdge>();
		try {
			this.parser = new StanfordParser();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.configFile = getClass().getClassLoader().getResourceAsStream("gkr.properties");
		this.retriever = new SenseMappingsRetriever(configFile);
		this.interrogative = false;
	}


	/***
	 * Convert the stanford graph to a SemanticGraph with dependencies, properties, lex features, roles, etc.
	 * @param stanGraph
	 * @return
	 */
	public sem.graph.SemanticGraph getGraph(SemanticGraph stanGraph, String sentence, String wholeCtx) {
		this.stanGraph = stanGraph;
		this.graph = new sem.graph.SemanticGraph();
		this.graph.setName(stanGraph.toRecoveredSentenceString());
		traversed.clear();
		//logger.log(Level.INFO, "Creating dependency graph");
		integrateDependencies();
		//logger.log(Level.INFO, "Creating concept graph");
		integrateRoles();
		//logger.log(Level.INFO, "Creating context graph");
		integrateContexts();
		//logger.log(Level.INFO, "Creating properties graph");
		integrateProperties();
		//graph.displayRoles();
		//logger.log(Level.INFO, "Creating lexical graph");
		integrateLexicalFeatures(wholeCtx);		
		//logger.log(Level.INFO, "Creating coreference graph");
		integrateCoRefLinks(sentence);
		//logger.log(Level.INFO, "Creating distributional graph");
		//integrateDistributionalReps();
		return this.graph;
	}

	public sem.graph.SemanticGraph getGraph() {
		return this.graph;
	}
	
	
	public ArrayList<String> getVerbalForms(){
		return verbalForms;
	}
	
	public ArrayList<String> getNounForms(){
		return nounForms;
	}
	
	public ArrayList<String> getQuantifiers(){
		return quantifiers;
	}
	
	public ArrayList<String> getWhInterrogatives(){
		return whinterrogatives;
	}
	
	public boolean isInterrogativeSent(){
		return interrogative;
	}

	/**
	 * Converts the edges of the stanford graph to edges of the semantic graph. 
	 * It finds all children of the given parent node and for each child it
	 * adds an edge from the parent to that child and recursively does the same
	 * for each child of the child.
	 * @param parent
	 * @param parentNode
	 */
	private void stanChildrenToSemGraphChildren(IndexedWord parent, SkolemNode parentNode){
		// get the children
		List<SemanticGraphEdge> children = stanGraph.outgoingEdgeList(parent);
		// iterate through the children
		for (SemanticGraphEdge child : children){
			if (traversed.contains(child)){
				break;
			}
			traversed.add(child);
			// get the role relation of the child and create a role edge
			String role = "";
			if (child.getRelation().getSpecific() == null)
				role = child.getRelation().getShortName(); 
			else
				role = child.getRelation().getShortName()+":"+child.getRelation().getSpecific();
			RoleEdge roleEdge = new RoleEdge(role, new RoleEdgeContent());
			// get the child's lemma
			String dependent = child.getDependent().lemma(); 
			// create a SkolemNodeContent for the child and create a SkolemNode
			SkolemNodeContent dependentContent = new SkolemNodeContent();
			dependentContent.setSurface(child.getDependent().originalText());
			dependentContent.setStem(child.getDependent().lemma());
			dependentContent.setPosTag(child.getDependent().tag());
			dependentContent.setPartOfSpeech(child.getDependent().tag());
			Double positionD = child.getDependent().pseudoPosition();
			dependentContent.setPosition(positionD.intValue());
			dependentContent.setDerived(false);	
			dependentContent.setSkolem(dependent+"_"+Integer.toString(positionD.intValue()));
			SkolemNode finish = new SkolemNode(dependentContent.getSkolem(), dependentContent);
			/* check if the same node already exists and if so, if it is a verb node. If it is no verb, then use the existing node as the finish node.
			this is necessary for sentences with noun or different-verbs coordination or control/raising verbs; 
			otherwise, the coord node /controlled subj is inserted twice because of the explicit enhanced dependencies. If however it is a verb (the same verb), 
			then we have a sentence of the type John works for Mary and for Anna. where we have to assume to different (same) verbs. */
			for (SemanticNode<?> node : graph.getDependencyGraph().getNodes()){
				if (node.getLabel().equals(finish.getLabel()) && !role.contains("conj")){ //!verbalForms.contains(finish.getPartOfSpeech())){
					finish = (SkolemNode) node;
				}
			}
			// add the dependency between the parent and the current child only if the parentNode != finish, otherwise loops
			if (!parentNode.equals(finish)){
				graph.addDependencyEdge(roleEdge, parentNode, finish);
			}

			// recursively, go back and do the same for each of the children of the child
			stanChildrenToSemGraphChildren(child.getDependent(), finish);		
		}
	}


	/**
	 * Convert the dependencies of the stanford graph to dependencies of the semantic graph.
	 * Start from the root node and then recursively visit all children of the root and all children
	 * of the children (in-depth). 
	 */
	private void integrateDependencies(){
		// get the root node of the stanford graph
		IndexedWord rootN = stanGraph.getFirstRoot();
		//stanGraph.prettyPrint();
		// create a new node for the semantic graph and define all its features 
		SkolemNodeContent rootContent = new SkolemNodeContent();
		rootContent.setSurface(rootN.originalText());
		rootContent.setStem(rootN.lemma());
		rootContent.setPosTag(rootN.tag());
		rootContent.setPartOfSpeech(rootN.tag());
		Double positionR = rootN.pseudoPosition();
		rootContent.setPosition(positionR.intValue());
		rootContent.setDerived(false);
		rootContent.setSkolem(rootN.lemma()+"_"+Integer.toString(positionR.intValue()));
		SkolemNode root = new SkolemNode(rootContent.getSkolem(), rootContent);
		// add the node as root node to the graph
		graph.setRootNode(root);
		// if there are no children of the root node at all (sentence only with imperative intransitive verb), just add this node the dep graph
		if (stanGraph.outgoingEdgeList(rootN).isEmpty()){
			graph.getDependencyGraph().addNode(root);
		} else {
			// based on the root node, go and find all children (and children of children)
			stanChildrenToSemGraphChildren(rootN, root);
		}

		/*
		 * Go through the finished dep graph and fix any cases that are dealt differently by CoreNLP than by us.
		 * For now, 2 things:
		 * 1)  only change the deps of the modals ought and need so they can be treated the same as the rest of the modals.
		 * When the modals have a complement with "to", they are (correctly) considered the roots of the sentences and the main verb
		 * gets to be the xcomp, e.g. Abrams need to hire Browne (as opposed to when they are found without "to", where they are
		 * considered plain aux of the main verb, e.g. Need Abrams hire Browne?)  However, we want to treat the former cases as aux as well
		 * so that the implementation of the role graph remains the same. Therefore, in the following we remove the x/ccomp edge
		 * and add the aux edge instead.  
		 * 2) if there are any quantifiers (e.g., few, little) involved with negative monotonicity in restriction position, then add a negated node in order to
		 * capture accordingly the contexts: few people = not many people
		 * For the moment, it doesnt work with "little". The quantifier "no" (not some) is separately treated in the context mapping. 
		 */
		boolean quantPresent = false;
		ArrayList<SemanticNode<?>> headsOfQuant = new ArrayList<SemanticNode<?>>();
		for (SemanticNode<?> node : graph.getDependencyGraph().getNodes()){
			if ( (((SkolemNodeContent) node.getContent()).getStem().equals("ought")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("need")) && graph.getInEdges(node).isEmpty()) {
				RoleEdge depEdge = new RoleEdge("aux", new RoleEdgeContent());
				List<SemanticEdge> outEdges = graph.getOutEdges(node);
				// if it is the x/ccomp edge, add the aux edge in its place
				for (SemanticEdge out : outEdges){
					if (out.getLabel().equals("xcomp") || out.getLabel().equals("ccomp")){
						SemanticNode<?> head = graph.getFinishNode(out);
						graph.addDependencyEdge(depEdge, head, node);	
					}
					// remove all out edges of the modal
					graph.removeDependencyEdge(out);
				}		
			} else if ( (((SkolemNodeContent) node.getContent()).getStem().equals("few"))) { 
				quantPresent = true;
				SemanticNode<?> head = graph.getInNeighbors(node).iterator().next();
				SemanticNode<?> headOfHead = graph.getInNeighbors(head).iterator().next();
				headsOfQuant.add(headOfHead);
			}
		}
		// if there are quantifiers like few and little (only few for now), add the 
		// not node at this step (cannot do it in previous step because concurrentmodificationException
		if (quantPresent == true) {
			for (SemanticNode<?> headOfHead : headsOfQuant) {
				RoleEdge depEdge = new RoleEdge("neg", new RoleEdgeContent());
				SkolemNodeContent notContent = new SkolemNodeContent();
				notContent.setSurface("not");
				notContent.setStem("not");
				notContent.setPosTag("RB");
				notContent.setPartOfSpeech("RB");
				int position = 0;
				notContent.setPosition(position);
				notContent.setDerived(false);
				notContent.setSkolem("not_0");
				SkolemNode not = new SkolemNode(notContent.getSkolem(), notContent);
				graph.addDependencyEdge(depEdge, headOfHead, not);	
			}

		}
	}

	/**
	 * Integrate the semantic roles of the graph.
	 */
	private void integrateRoles(){
		RolesMapper rolesMapper = new RolesMapper(graph,verbalForms, nounForms);
		rolesMapper.integrateAllRoles();
	}

	/**
	 * Adds the properties to the semantic graph. Searches for all nodes that are nouns or verbs
	 * and adds for each of them the relevant properties:
	 * - for nouns: cardinality, name, specifier and nmod_num
	 * - for verbs: tense and aspect
	 */
	private void integrateProperties(){
		SemGraph depGraph = graph.getDependencyGraph();
		Set<SemanticNode<?>> depNodes = depGraph.getNodes();
		// iterate through the nodes of the dep graph
		for (SemanticNode<?> node: depNodes){
			String tense = "";
			String aspect = "";
			String cardinality = "";
			String name = "";
			String specifier = "";
			String part_of = "";
			String pos = ((SkolemNode) node).getPartOfSpeech();
			// define the properties fro verbs
			if (verbalForms.contains(pos)){
				aspect = "not progressive";
				if (pos.equals("VB") || pos.equals("VBP") || pos.equals("VBZ"))
					tense = "present";
				else if (pos.equals("VBD") || pos.equals("VBN")){
					tense = "past";
				}
				else if (pos.equals("VBG"))
					aspect = "progressive";
				Set<SemanticEdge> inEdges = depGraph.getInEdges(node);
				if (!inEdges.isEmpty()){
					if (inEdges.iterator().next().getLabel().equals("aux") ){
						if (!tense.equals("")){
							PropertyEdge tenseEdge = new PropertyEdge(GraphLabels.TENSE, new PropertyEdgeContent());
							graph.addPropertyEdge(tenseEdge, depGraph.getInNeighbors(node).iterator().next(), new ValueNode(tense, new ValueNodeContent()));
						}
						continue;
					}
				}

				// adding the property edge tense
				if (!tense.equals("")){
					PropertyEdge tenseEdge = new PropertyEdge(GraphLabels.TENSE, new PropertyEdgeContent());
					graph.addPropertyEdge(tenseEdge, node, new ValueNode(tense, new ValueNodeContent()));
				}
				// adding the property edge aspect
				if (!aspect.equals("")){
					PropertyEdge aspectEdge = new PropertyEdge(GraphLabels.ASPECT, new PropertyEdgeContent());
					graph.addPropertyEdge(aspectEdge, node, new ValueNode(aspect, new ValueNodeContent()));
				}
				// define the properties for nouns
			} else if (nounForms.contains(pos) && !node.getLabel().toLowerCase().contains("none")){
				if (pos.equals("NN")){
					cardinality = "sg";
					name = "common";
				} else if (pos.equals("NNP")){
					cardinality = "sg";
					name = "proper";
				} else if (pos.equals("NNPS")){
					cardinality = "pl";
					name = "proper";
				} else if (pos.equals("NNS")){
					cardinality = "pl";
					name = "common";
				}
				// checks if there is a quantification with of, e.g. five of the seven
				boolean existsQMod = false;
				// going through the out edges of this node to see if there are any specifiers
				for (SemanticEdge edge: graph.getDependencyGraph().getOutEdges(node)){
					// depending on the case, define the specifier
					String depOfDependent = edge.getLabel();
					String determiner = "";
					// need to check whether the finish node is not empty: in the case of negation or modals, the finish node will
					// be empty because it is not added as a normal concept node. But in these cases, the node also does not need to
					// be a determiner
					if (graph.getFinishNode(edge) != null)
						determiner = ((SkolemNodeContent) graph.getFinishNode(edge).getContent()).getStem(); //edge.getDestVertexId().substring(0,edge.getDestVertexId().indexOf("_"));
					if (depOfDependent.equals("det") && existsQMod == false) {					
						specifier = determiner; 
						// only if there is no quantification with of, assign this determiner as the cardinatlity
					} else if (depOfDependent.equals("nummod") && existsQMod == false){
						specifier = determiner;
						// otherwise we introduce the part_of edge
					} else if (depOfDependent.equals("nummod") && existsQMod == true){
						part_of = determiner;
						// if there is det:qmod there is quantification with of. We also check if there is any quantification on the quantification, e.g.
						// "any five of the seven". In this case,  any becomes the specifier of the five
					}else if (depOfDependent.equals("det:qmod")){
						specifier = determiner;
						existsQMod = true;
						// get the outNode (the node corresponding to the string determiner)
						SemanticNode<?> outNode = graph.getDependencyGraph().getEndNode(edge);
						// check if there are outEdges that are "det"
						for (SemanticEdge outEdge : graph.getDependencyGraph().getOutEdges(outNode)){
							if (outEdge.getLabel().equals("det")){
								specifier = outEdge.getDestVertexId().substring(0,outEdge.getDestVertexId().indexOf("_"));
							}
						}
					} else if (depOfDependent.equals("amod") && (quantifiers.contains(determiner.toLowerCase()) )  ){
						specifier = determiner;
						// do the following adjustments for quantifiers with negative monotonicity in restriction type
					} else if (determiner.equals("no")){
						specifier = "some";
						//((SkolemNodeContent) graph.getFinishNode(edge).getContent()).setSurface("some");
						//((SkolemNodeContent) graph.getFinishNode(edge).getContent()).setStem("some");
						//((SkolemNodeContent) graph.getFinishNode(edge).getContent()).setSkolem("some_"+Integer.toString(((SkolemNodeContent) graph.getFinishNode(edge).getContent()).getPosition()));
						//graph.getFinishNode(edge).setLabel(((SkolemNodeContent) graph.getFinishNode(edge).getContent()).getSkolem());					
					} else if (determiner.equals("few")){
						specifier = "many";	
						((SkolemNodeContent) graph.getFinishNode(edge).getContent()).setSurface("many");
						((SkolemNodeContent) graph.getFinishNode(edge).getContent()).setStem("many");
						((SkolemNodeContent) graph.getFinishNode(edge).getContent()).setSkolem("many_"+Integer.toString(((SkolemNodeContent) graph.getFinishNode(edge).getContent()).getPosition()));
						graph.getFinishNode(edge).setLabel(((SkolemNodeContent) graph.getFinishNode(edge).getContent()).getSkolem());					
					}
				}
				// check if there is a "none" involved: "none" is not recognized as a det:qmod so we have to look for it separately
				for (SemanticEdge edge: graph.getDependencyGraph().getInEdges(node)){
					if (edge.getLabel().equals("nmod:of") && graph.getDependencyGraph().getStartNode(edge).getLabel().toLowerCase().contains("none")){
						specifier = "none";
					}
				}
				// adding the property edge cardinality (singular, plural)
				if (!cardinality.equals("")){
					PropertyEdge cardinalityEdge = new PropertyEdge(GraphLabels.CARDINAL, new PropertyEdgeContent());
					graph.addPropertyEdge(cardinalityEdge, node, new ValueNode(cardinality, new ValueNodeContent()));
				}
				// adding the property edge name (proper or common)
				if (!name.equals("")){
					PropertyEdge typeEdge = new PropertyEdge(GraphLabels.NTYPE, new PropertyEdgeContent());
					graph.addPropertyEdge(typeEdge, node, new ValueNode(name, new ValueNodeContent()));
				}
				// adding the property edge specifier (the, a, many, few, N, etc)
				if (!specifier.equals("")){
					PropertyEdge specifierEdge = new PropertyEdge(GraphLabels.SPECIFIER, new PropertyEdgeContent());
					graph.addPropertyEdge(specifierEdge, node, new ValueNode(specifier, new ValueNodeContent()));
				}
				else if (specifier.equals("")){
					PropertyEdge specifierEdge = new PropertyEdge(GraphLabels.SPECIFIER, new PropertyEdgeContent());
					graph.addPropertyEdge(specifierEdge, node, new ValueNode("bare", new ValueNodeContent()));
				}
				// adding the property edge part_of (e.g. five of seven, five is the specifier and seven the part_of)
				if (!part_of.equals("")){
					PropertyEdge partOfEdge = new PropertyEdge(GraphLabels.PART_OF, new PropertyEdgeContent());
					graph.addPropertyEdge(partOfEdge, node, new ValueNode(part_of, new ValueNodeContent()));
				}
			} 
			// check if there is a direct or indirect question and add property for wh-interrogatives
			else if (interrogative == true && whinterrogatives.contains(((SkolemNodeContent) node.getContent()).getStem())){
				String stem = (String) ((SkolemNodeContent) node.getContent()).getStem();
				String label = "unk";
				if (stem.equals("who"))
					label = "personal";
				else if (stem.equals("where"))
					label = "locative";
				else if (stem.equals("when"))
					label = "temporal";
				else if (stem.equals("why"))
					label = "causal";
				else if (stem.equals("what"))
					label = "object";
				else if (stem.equals("which"))
					label = "group";
				else if (stem.equals("how"))
					label = "manner";
				else if (stem.equals("whose"))
					label = "possessive";
				else if (stem.equals("whom"))
					label = "pers.acc.";
				PropertyEdge interrEdge = new PropertyEdge(GraphLabels.SPECIFIER, new PropertyEdgeContent());
				graph.addPropertyEdge(interrEdge, node, new ValueNode(label, new ValueNodeContent()));
			}
		}
	}

	/**
	 * Maps each node to its lexical semantics and adds the corresponding sense nodes and lex edges to
	 * the semantic graph . For the moment, it maps the disambiguated sense of the node (this gets to be
	 * the label of the SenseNode), the concept of the node (this is set to the concept of the SenseContent),
	 * the subConcepts of the node (there are set to be the subconcepts of the SenseContent) and the 
	 * superConcepts of the node (these are set to be the superconcepts of the SenseContent). At the moment, the
	 * racs of the SenseNode are left empty. 
	 */
	private void integrateLexicalFeatures(String wholeCtx){
		HashMap <String, Map<String,Float>> senses = null;
		//long startTime = System.currentTimeMillis();	
		/* within this method, two main, long processes take place
		1. the computation of the embedding of each word
		2 the WSD of each word
		==> put 1. in a separate thread to speed up process 
		*/ 
		 ExecutorService executorService = Executors.newSingleThreadExecutor();
		 Future future = executorService.submit(new LexicalGraphConcurrentTask(wholeCtx));
		 executorService.shutdown();
		try {		
			senses = retriever.disambiguateSensesWithJIGSAW(wholeCtx); // stanGraph.toRecoveredSentenceString());
			// next line needed for non-multithreading
			//retriever.getEmbedForWholeCtx(wholeCtx);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//long endTime = System.currentTimeMillis();
		//System.out.println("That took " + (endTime - startTime) + " milliseconds");

		SemGraph roleGraph = graph.getRoleGraph();
		Set<SemanticNode<?>> roleNodes = roleGraph.getNodes();
		for (SemanticNode<?> node: roleNodes){
			if (node instanceof SkolemNode){
				retriever.extractNodeEmbedFromSequenceEmbed((SkolemNode) node);
				// for this node, get each sense it is associated with, as a map of sense:concept:senseScore. 
				// the inner map of String,Float only contains one element each time 
				HashMap<String, Map<String, Float>> lexSem;
				lexSem = retriever.mapNodeToSenseAndConcept((SkolemNode) node, graph, senses);
				// someone is not included in the PWD3.0
				if (lexSem.isEmpty() && node.getLabel().contains("someone")){
					String sense = "00007846";
					SenseNodeContent senseContent = new SenseNodeContent(sense);
					senseContent.addConcept("Human=");
					try {
						retriever.getLexRelationsOfSynset(((SkolemNode) node).getStem(), sense, ((SkolemNode) node).getPartOfSpeech());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					senseContent.setHierarchyPrecomputed(true);
					senseContent.setSubConcepts(retriever.getSubConcepts());
					senseContent.setSuperConcepts(retriever.getSuperConcepts());
					senseContent.setSynonyms(retriever.getSynonyms());
					senseContent.setHypernyms(retriever.getHypernyms());
					senseContent.setHyponyms(retriever.getHyponyms());
					senseContent.setAntonyms(retriever.getAntonyms());
					senseContent.setEmbed(retriever.getEmbed());
					senseContent.setSenseScore(0);
					senseContent.setSenseKey(retriever.getSenseKey());
					
					// create new Sense Node
					SenseNode senseNode = new SenseNode(sense, senseContent);
					// create new LexEdge

					LexEdge edge = new LexEdge(GraphLabels.LEX, new LexEdgeContent());
					graph.addLexEdge(edge, node, senseNode);

					retriever.setSubConcepts(new HashMap<String,Integer>());
					retriever.setSuperConcepts(new HashMap<String,Integer>());
					retriever.setSynonyms(new ArrayList<String>());
					retriever.setHypernyms(new ArrayList<String>());
					retriever.setHyponyms(new ArrayList<String>());
					retriever.setAntonyms(new ArrayList<String>());		
					retriever.setSenseKey("");

				}
				// if no senses are found for this node, then add only the embed as lexical node
				else if (lexSem.isEmpty()){
					String sense = "00000000";
					SenseNodeContent senseContent = new SenseNodeContent(sense);
					senseContent.addConcept("");
					senseContent.setHierarchyPrecomputed(true);
					senseContent.setSubConcepts(new HashMap<String, Integer>());
					senseContent.setSuperConcepts(new HashMap<String, Integer>());
					senseContent.setSynonyms(new ArrayList<String>());
					senseContent.setHypernyms(new ArrayList<String>());
					senseContent.setHyponyms(new ArrayList<String>());
					senseContent.setAntonyms(new ArrayList<String>());
					senseContent.setEmbed(retriever.getEmbed());
					senseContent.setSenseScore(0);	
					senseContent.setSenseKey("");
					// create new Sense Node
					SenseNode senseNode = new SenseNode(sense, senseContent);
					// create new LexEdge
					LexEdge edge = new LexEdge(GraphLabels.LEX, new LexEdgeContent());
					graph.addLexEdge(edge, node, senseNode);		
					
				}				
				for (String key : lexSem.keySet()){
					String sense = key;
					try {
						retriever.getLexRelationsOfSynset(((SkolemNode) node).getStem(), sense, ((SkolemNode) node).getPartOfSpeech());

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// get the first element of the inner map as the concept (only one element anyway)
					String concept = lexSem.get(key).keySet().iterator().next();
					// create new sense Content
					SenseNodeContent senseContent = new SenseNodeContent(sense);
					senseContent.addConcept(concept);
					senseContent.setHierarchyPrecomputed(true);
					senseContent.setSubConcepts(retriever.getSubConcepts());
					senseContent.setSuperConcepts(retriever.getSuperConcepts());
					senseContent.setSynonyms(retriever.getSynonyms());
					senseContent.setHypernyms(retriever.getHypernyms());
					senseContent.setHyponyms(retriever.getHyponyms());
					senseContent.setAntonyms(retriever.getAntonyms());
					senseContent.setEmbed(retriever.getEmbed());
					senseContent.setSenseScore(lexSem.get(key).get(concept));
					senseContent.setSenseKey(retriever.getSenseKey()); 

					// create new Sense Node
					SenseNode senseNode = new SenseNode(sense, senseContent);
					// create new LexEdge

					LexEdge edge = new LexEdge(GraphLabels.LEX, new LexEdgeContent());
					graph.addLexEdge(edge, node, senseNode);

					retriever.setSubConcepts(new HashMap<String,Integer>());
					retriever.setSuperConcepts(new HashMap<String,Integer>());
					retriever.setSynonyms(new ArrayList<String>());
					retriever.setHypernyms(new ArrayList<String>());
					retriever.setHyponyms(new ArrayList<String>());
					retriever.setAntonyms(new ArrayList<String>());		
					retriever.setSenseKey("");
				}	

			}
			// for now, embed is the same for all senses of a given word, so only initialize at the end of each node 
			retriever.setEmbed(null);
		}
		// make sure you get indirect semantics for things added later to the context graph
		for (SemanticNode<?> ctxNode : graph.getContextGraph().getNodes()){
			if (ctxNode.getLabel().contains("person_") || ctxNode.getLabel().contains("thing_")){
				String sense = "";
				String concept = "";
				if (ctxNode.getLabel().contains("person")){
					sense = "00007846";
					concept = "Human=";
					try {
						retriever.getLexRelationsOfSynset("person", "00007846", "NN");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if (ctxNode.getLabel().contains("thing")){
					sense = "04424218";
					concept = "Artifact=";
					try {
						retriever.getLexRelationsOfSynset("thing", "04424218", "NN");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						retriever.getLexRelationsOfSynset(((SkolemNode) ctxNode).getStem(), sense, ((SkolemNode) ctxNode).getPartOfSpeech());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				// create new sense Content
				SenseNodeContent senseContent = new SenseNodeContent(sense);
				senseContent.addConcept(concept);
				senseContent.setHierarchyPrecomputed(true);
				senseContent.setSubConcepts(retriever.getSubConcepts());
				senseContent.setSuperConcepts(retriever.getSuperConcepts());
				senseContent.setSynonyms(retriever.getSynonyms());
				senseContent.setHypernyms(retriever.getHypernyms());
				senseContent.setHyponyms(retriever.getHyponyms());
				senseContent.setAntonyms(retriever.getAntonyms());
				senseContent.setEmbed(retriever.getEmbed());
				senseContent.setSenseScore(0);
				senseContent.setSenseKey(retriever.getSenseKey());
				
				// create new Sense Node
				SenseNode senseNode = new SenseNode(sense, senseContent);
				// create new LexEdge

				LexEdge edge = new LexEdge(GraphLabels.LEX, new LexEdgeContent());
				graph.addLexEdge(edge, ctxNode, senseNode);

				retriever.setSubConcepts(new HashMap<String,Integer>());
				retriever.setSuperConcepts(new HashMap<String,Integer>());
				retriever.setSynonyms(new ArrayList<String>());
				retriever.setHypernyms(new ArrayList<String>());
				retriever.setHyponyms(new ArrayList<String>());
				retriever.setAntonyms(new ArrayList<String>());		
				retriever.setSenseKey("");
			}
		}
	}

	/** 
	 * 
	 * @author kkalouli
	 * Computes embeds in a different thread for faster processing. 
	 */
	public class LexicalGraphConcurrentTask implements Runnable{
		private String wholeCtx;

		public LexicalGraphConcurrentTask(String wholeCtx) {
			this.wholeCtx = wholeCtx;
		}

		public void run() {
			retriever.getEmbedForWholeCtx(wholeCtx);
		}
	}


	/*** 
	 * create the context graph by taking into account the different "markers of contexts".
	 * Once the graph is created, go through it and assign contexts to each skolem of the dependency graph. 
	 */
	private void integrateContexts(){
		ContextMapper ctxMapper = new ContextMapper(graph, verbalForms, interrogative);
		ctxMapper.integrateAllContexts();
	}

	/*** 
	 * create the distributional graph by computing a distributional representation for each subgraph introduced
	 * by a context of the context graph  
	 */
	private void integrateDistributionalReps(){
		DistributionMapper distrMapper = new DistributionMapper(graph);
		/*try {
			distrMapper.mapCtxsToDistrReps(plainSkolemsWriter);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};*/
	}

	/***
	 * Create the link graph by resolving the coreferences. Uses the stanford CoreNLP software but also the stanford dependencies directly.
	 * @param sentence
	 */
	private void integrateCoRefLinks(String sentence){
		// ge the corefrence chains as those are given by CoreNLP
		Collection<CorefChain> corefChains = parser.getCoreference(sentence);
		for (CorefChain cc: corefChains){	
			SemanticNode<?> startNode = null;
			SemanticNode<?> finishNode = null;
			for (IntPair k : cc.getMentionMap().keySet()){
				// find in the role graph the node with the position equal to the position that the coreference element has
				for (SemanticNode<?> n : graph.getRoleGraph().getNodes()){
					if (n instanceof SkolemNode){
						if (((SkolemNodeContent) n.getContent()).getPosition() == k.getTarget()){
							// in the first pass of this chain, set the startNode, in all other ones set the finishNode (the start Node remains the same)
							if (startNode == null){ //) && !((SkolemNodeContent) n.getContent()).getPartOfSpeech().contains("PRP")){
								startNode = n;
							} else {
								finishNode = n;
							}
						}
					}
				}
				
				// if all passes are over and there is coreference, add the links
				if (startNode != null && finishNode != null){
					LinkEdge linkEdge = new LinkEdge(GraphLabels.PRONOUN_RESOLUTION, new DefaultEdgeContent());
					graph.addLinkEdge(linkEdge, startNode, finishNode);
				}
			}
		}
		// the coreference CoreNLP does not show the appositives; these are in the form of dependencies in the dependency graph, so we need to extract them from there
		// (the appositives are also included in the role graph as restrictions)
		SemanticNode<?> startNode = null;
		SemanticNode<?> finishNode = null;
		for (SemanticEdge depEdge : graph.getDependencyGraph().getEdges()){
			// check for the existence of appositives and add the coreference link
			if (depEdge.getLabel().equals("appos")){
				startNode = graph.getStartNode(depEdge);
				finishNode = graph.getFinishNode(depEdge);
				LinkEdge linkEdge = new LinkEdge(GraphLabels.APPOS_IDENTICAL_TO, new DefaultEdgeContent());
				graph.addLinkEdge(linkEdge, startNode, finishNode);
			}
		}
	}

	/**
	 * Returns the semantic graph of the given sentence. 
	 * It runs the stanford parser, gets the graph and turns this graph to the semantic graph.
	 * @param sentence
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public sem.graph.SemanticGraph sentenceToGraph(String sentence, String wholeCtx){	
		if (sentence.contains("?"))
			this.interrogative = true;
		SemanticGraph stanGraph = parser.parseOnly(sentence);
		sem.graph.SemanticGraph graph = this.getGraph(stanGraph, sentence, wholeCtx);
		this.interrogative = false;
		return graph;
	}

	/***
	 * Process a testsuite of sentences with GKR. One sentence per line.
	 * Lines starting with # are considered comments.
	 * The output is formatted as string: in this format only the dependency graph, the
	 * concepts graph, the contextual graph and the properties graph are displayed
	 * @param file
	 * @param semConverter
	 * @throws IOException
	 */

	public void processTestsuite(String file) throws IOException{
		FileInputStream fileInput = new FileInputStream(file);
		InputStreamReader inputReader = new InputStreamReader(fileInput, "UTF-8");
		BufferedReader br = new BufferedReader(inputReader);
		// true stands for append = true (dont overwrite)
		BufferedWriter writer = new BufferedWriter( new FileWriter(file.substring(0,file.indexOf(".txt"))+"_processed.csv", true));
		FileOutputStream fileSer = null;
		ObjectOutputStream writerSer = null;
		String strLine;
		ArrayList<sem.graph.SemanticGraph> semanticGraphs = new ArrayList<sem.graph.SemanticGraph>();
		while ((strLine = br.readLine()) != null) {
			if (strLine.startsWith("#")){
				writer.write(strLine+"\n\n");
				writer.flush();
				continue;
			}
			String text = strLine.split("\t")[1];
	        if (!text.endsWith(".") && !text.endsWith("?") && !text.endsWith("!")){
	        	text = text+".";
	        }
			SemanticGraph stanGraph = parser.parseOnly(text);
			sem.graph.SemanticGraph graph = this.getGraph(stanGraph, text, text);
			//System.out.println(graph.displayAsString());
			writer.write(strLine+"\n"+graph.displayAsString()+"\n\n");
			writer.flush();
			System.out.println("Processed sentence "+ strLine.split("\t")[0]);
			if (graph != null)
				semanticGraphs.add(graph);
		}
		// serialize and write to file
		try {
			fileSer = new FileOutputStream("serialized_SemanticGraphs.ser");
			writerSer = new ObjectOutputStream(fileSer);
			writerSer.writeObject(semanticGraphs); 				
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		writer.close();
		br.close();
		fileSer.close();
		writerSer.close();
		fileInput.close();
		inputReader.close();
		//plainSkolemsWriter.close();
	}
	
	/***
	 * Process a testsuite for the DEMO.
	 * Process a testsuite of sentences with GKR. One sentence per line.
	 * Lines starting with # are considered comments.
	 * The output is formatted as string: in this format only the dependency graph, the
	 * concepts graph, the contextual graph and the properties graph are displayed
	 * @param file
	 * @param semConverter
	 * @throws IOException
	 */

	public String processDemoTestsuite(InputStream file) throws IOException{
		InputStreamReader inputReader = new InputStreamReader(file, "UTF-8");
		BufferedReader br = new BufferedReader(inputReader);
		// true stands for append = true (dont overwrite)
		String outputName = "/home/kkalouli/Documents/Programs/apache-tomcat-9.0.20/webapps/sem.mapper/processed.txt";
		BufferedWriter writer = new BufferedWriter( new FileWriter(outputName, true));
		FileOutputStream fileSer = null;
		ObjectOutputStream writerSer = null;
		String strLine;
		ArrayList<sem.graph.SemanticGraph> semanticGraphs = new ArrayList<sem.graph.SemanticGraph>();
		while ((strLine = br.readLine()) != null) {
			if (strLine.startsWith("####")){
				writer.write(strLine+"\n\n");
				writer.flush();
				continue;
			}
			String text = strLine.split("\t")[1];
	        if (!text.endsWith(".") && !text.endsWith("?") && !text.endsWith("!")){
	        	text = text+".";
	        }
	        if (!text.matches("(\\w*(\\s|,|\\.|\\?|!|\"|-|')*)*")) {
	        	writer.write(strLine+"\nSentence cannot be processed. Invalid characters.\n\n");
	        	continue;
	        }
			SemanticGraph stanGraph = parser.parseOnly(text);
			sem.graph.SemanticGraph graph = this.getGraph(stanGraph, text, text);
			//System.out.println(graph.displayAsString());
			writer.write(strLine+"\n"+graph.displayAsString()+"\n\n");
			///System.out.println("Processed sentence "+ strLine.split("\t")[0]);
			//if (graph != null)
			//	semanticGraphs.add(graph);
		}
		// serialize and write to file
		/*try {
			fileSer = new FileOutputStream("serialized_SemanticGraphs.ser");
			writerSer = new ObjectOutputStream(fileSer);
			writerSer.writeObject(semanticGraphs); 				
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
		writer.close();
		br.close();
		//fileSer.close();
		//writerSer.close();
		//fileInput.close();
		inputReader.close();
		return outputName;
		//plainSkolemsWriter.close();*/
	}


	/***
	 * Process a single sentence with GKR. 
	 * You can comment in or out the subgraphs that you want to have displayed.
	 * @throws IOException 
	 */
	public sem.graph.SemanticGraph processSentence(String sentence, String wholeCtx) throws IOException{
		if (!sentence.endsWith("."))
			sentence = sentence+".";
		if (!wholeCtx.endsWith("."))
			wholeCtx = wholeCtx+".";
		sem.graph.SemanticGraph graph = this.sentenceToGraph(sentence, wholeCtx);
		graph.displayContexts();
		graph.displayRoles();
		graph.displayDependencies();
		graph.displayProperties();
		graph.displayLex();	
		graph.displayRolesAndCtxs();
		graph.displayCoref();
		graph.displayRolesAndLinks();
		graph.displayRolesCtxsAndProperties();
		/*String ctxs = graph.getContextGraph().getMxGraph();
		String roles = graph.getRoleGraph().getMxGraph();
		String deps = graph.getDependencyGraph().getMxGraph();
		String props = graph.getPropertyGraph().getMxGraph();
		String lex = graph.getLexGraph().getMxGraph();
		String coref = graph.getLinkGraph().getMxGraph();
		String rolesAndCtx = graph.getRolesAndCtxGraph().getMxGraph();
		String rolesAndCoref = graph.getRolesAndCorefGraph().getMxGraph();
		BufferedWriter writer = new BufferedWriter( new FileWriter("-13.txt", true));
		writer.write(roles);
		writer.write("\n\n");
		writer.write(deps);
		writer.write("\n\n");
		writer.write(ctxs);
		writer.write("\n\n");
		writer.write(props);
		writer.write("\n\n");
		writer.write(lex);
		writer.write("\n\n");
		writer.write(coref);
		writer.write("\n\n");
		writer.write(rolesAndCtx);
		writer.write("\n\n");
		writer.write(rolesAndCoref);
		writer.write("\n\n");
		writer.flush();
		writer.close();*/
		//ImageIO.write(graph.saveDepsAsImage(),"png", new File("/Users/kkalouli/Desktop/deps.png"));
		System.out.println(graph.displayAsString());
		for (SemanticNode<?> node : graph.getDependencyGraph().getNodes()){
			System.out.println(node.getLabel()+((SkolemNodeContent) node.getContent()).getContext());
		}
		return graph;
	}


	@SuppressWarnings("unchecked")
	public ArrayList<sem.graph.SemanticGraph> deserializeFileWithComputedPairs(String file){
		ArrayList<sem.graph.SemanticGraph> semanticGraphs = null;
		try {
			FileInputStream fileIn = new FileInputStream("serialized_SemanticGraphs.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			semanticGraphs = (ArrayList<sem.graph.SemanticGraph>) in.readObject();
			fileIn.close();
			in.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return semanticGraphs;
	}



	public static void main(String args[]) throws IOException {
		DepGraphToSemanticGraph semConverter = new DepGraphToSemanticGraph();
		//semConverter.deserializeFileWithComputedPairs("/Users/kkalouli/Documents/Stanford/comp_sem/forDiss/test.txt");
		//semConverter.processTestsuite("/Users/kkalouli/Documents/Stanford/comp_sem/forDiss/HP_testsuite/HP_testsuite_shortened_active.txt");
		//semConverter.processTestsuite("/home/kkalouli/Documents/diss/experiments/UD_corpus_cleaned.txt");
		String sentence = "Mary must not go to the cinema."; //A family is watching a little boy who is hitting a baseball.";
		String context = "The kid faked the illness.";
		semConverter.processSentence(sentence, sentence+" "+context);
	}
}
