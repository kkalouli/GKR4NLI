package gnli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.robrua.nlp.bert.Bert;
import com.robrua.nlp.bert.FullTokenizer;

import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import gnli.GNLIGraph;
import gnli.InferenceChecker.EntailmentRelation;
import jigsaw.JIGSAW;
import gnli.InitialTermMatcher;
import sem.graph.SemGraph;
import sem.graph.SemanticGraph;
import sem.mapper.DepGraphToSemanticGraph;



/**
 * This is the main class of GKR4NLI. It runs the whole computation of inference, from
 * the semantic preprocessing to the final inference label.
 * @author Katerina Kalouli, 2019
 *
 */
public class InferenceComputer {

	private static KB kb;
	private static DepGraphToSemanticGraph semGraph;
	private boolean learning;
	private Properties props;
	private String sumoKB;
	private Bert bert;
	private FullTokenizer tokenizer;
	private IRAMDictionary wnDict;
	private HashMap<String, ArrayList<HeadModifierPathPair>> entailRolePaths;
	private HashMap<String, ArrayList<HeadModifierPathPair>> neutralRolePaths;
	private HashMap<String, ArrayList<HeadModifierPathPair>> contraRolePaths;
	private ArrayList<String> pathsAndCtxs;
	private String sumoContent;
	private String bertVocab;
	private HashMap<String,String> associationRules;
	private HashMap<String,String> vnToPWNMap;
	//private HashMap<String,String> assoRulesMap;


	/** 
	 * Instructor of the class. Calls all necessary external tools.
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public InferenceComputer() throws FileNotFoundException, UnsupportedEncodingException {
		// load the classloader to get the properties of the properties file
		InputStream properties = getClass().getClassLoader().getResourceAsStream("gnli.properties");
		this.props = new Properties();
		InputStreamReader inputReader = new InputStreamReader(properties);
		try {
			props.load(inputReader);
			inputReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// initialize the SUMO reader
        this.sumoKB = props.getProperty("sumo_kb");
		KBmanager.getMgr().initializeOnce(sumoKB);
		this.kb = KBmanager.getMgr().getKB("SUMO");
		//serializeKb();
		String wnInstall = props.getProperty("wn_location");
		String sumoInstall = props.getProperty("sumo_location");
		this.learning = false;
		// initialize bert and bertTokenizer so that there is only one instance
        this.bertVocab = props.getProperty("bert_vocab");
		this.bert = Bert.load("com/robrua/nlp/easy-bert/bert-uncased-L-12-H-768-A-12");
		this.tokenizer = new FullTokenizer(new File(bertVocab), true);
		// initialize only one instance of the PWN Dictionary
		this.wnDict = new RAMDictionary(new File(wnInstall), ILoadPolicy.NO_LOAD);
		try {
			this.wnDict.open();
			this.wnDict.load();
			// read the sumo files as one
			Scanner scanner = new Scanner(new File(sumoInstall), "UTF-8");
			this.sumoContent = scanner.useDelimiter("\\A").next();
			scanner.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		 * The below code is not used in the current version of the system. It was 
		 * used for experimenting with lexicalized association rule mining. As explained
		 * in the paper and the dissertation, this kind of association rule mining was
		 * not successful.  
		 */
		/*this.vnToPWNMap = new HashMap<String,String>();
		//this.assoRulesMap = new HashMap<String,String>();
		BufferedReader br = null;
		//BufferedReader brAssoRules = null;
		InputStreamReader inputReaderVn = null;
		//InputStreamReader inputReaderAssoRules = null;
		try {
			InputStream vnToPWN = getClass().getClassLoader().getResourceAsStream("mappings_vn_wd.txt");
			inputReaderVn = new InputStreamReader(vnToPWN, "UTF-8");
			br = new BufferedReader(inputReaderVn);
			
			//InputStream assoRules = getClass().getClassLoader().getResourceAsStream("association_rules.txt");
			//inputReaderAssoRules = new InputStreamReader(assoRules, "UTF-8");
			//brAssoRules = new BufferedReader(inputReaderAssoRules);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String strLine;
		String strLine2;
		//store the file in a hashmap. key: the PWN Sense key, Value: the VN class
		try {
			while ((strLine = br.readLine()) != null) { 
				String[] elems = strLine.split("\\t");
				vnToPWNMap.put(elems[0]+"::",elems[1]);	
			}
			br.close();
			inputReader.close();
			
			/*while ((strLine2 = br.readLine()) != null) { 
				String[] elems = strLine2.split("\\t");
				assoRulesMap.put(elems[0],elems[1]);	
			}
			brAssoRules.close();
			inputReaderAssoRules.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//for learning==true
		//this.entailRolePaths = new HashMap<String, ArrayList<HeadModifierPathPair>>();
		//this.neutralRolePaths =  new HashMap<String, ArrayList<HeadModifierPathPair>>();
		//this.contraRolePaths =  new HashMap<String, ArrayList<HeadModifierPathPair>>();
		//this.associationRules = new HashMap<String,String>();
		this.pathsAndCtxs = new ArrayList<String>();
		// for learning==false
		this.entailRolePaths = deserialize("entail");
		this.neutralRolePaths =  deserialize("neutral");
		this.contraRolePaths =  deserialize("contra");
		
		// get stats out of the serialized rolePaths
		/*ArrayList<String> all3 = new ArrayList<String>();
		ArrayList<String> entailAndNeutral = new ArrayList<String>();
		ArrayList<String> entailAndContra = new ArrayList<String>();
		ArrayList<String> neutralAndContra = new ArrayList<String>();
		ArrayList<String> neutral = new ArrayList<String>();
		ArrayList<String> contra = new ArrayList<String>();
		ArrayList<String> entail = new ArrayList<String>();
		
		for (String key: entailRolePaths.keySet()) {
			if (neutralRolePaths.keySet().contains(key) && contraRolePaths.keySet().contains(key)){
				int occurEntail = entailRolePaths.get(key).size();
				int occurContra = contraRolePaths.get(key).size();
				int occurNeutral = neutralRolePaths.get(key).size();
				all3.add(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurContra)+"\t"+Integer.toString(occurNeutral));
			}
			else if (neutralRolePaths.keySet().contains(key) && !contraRolePaths.keySet().contains(key)){
				int occurEntail = entailRolePaths.get(key).size();
				int occurNeutral = neutralRolePaths.get(key).size();
				entailAndNeutral.add(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurNeutral));
			}
			else if (!neutralRolePaths.keySet().contains(key) && contraRolePaths.keySet().contains(key)){
				int occurEntail = entailRolePaths.get(key).size();
				int occurContra = contraRolePaths.get(key).size();
				entailAndContra.add(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurContra));
			}
			else {
				entail.add(key);
			}
		}
		
		for (String key: contraRolePaths.keySet()) {
			if (neutralRolePaths.keySet().contains(key) && entailRolePaths.keySet().contains(key)){
				int occurEntail = entailRolePaths.get(key).size();
				int occurContra = contraRolePaths.get(key).size();
				int occurNeutral = neutralRolePaths.get(key).size();
				if (!all3.contains(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurContra)+"\t"+Integer.toString(occurNeutral))){				
					all3.add(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurContra)+"\t"+Integer.toString(occurNeutral));
				}
			}
			else if (neutralRolePaths.keySet().contains(key) && !entailRolePaths.keySet().contains(key)){
				int occurContra = contraRolePaths.get(key).size();
				int occurNeutral = neutralRolePaths.get(key).size();
				neutralAndContra.add(key+"\t"+Integer.toString(occurContra)+"\t"+Integer.toString(occurNeutral));

			}
			else if (!neutralRolePaths.keySet().contains(key) && entailRolePaths.keySet().contains(key)){
				int occurEntail = entailRolePaths.get(key).size();
				int occurContra = contraRolePaths.get(key).size();
				if (!entailAndContra.contains(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurContra))){
					entailAndContra.add(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurContra));
				}
			}
			else {
				contra.add(key);
			}
		}
		
		for (String key: neutralRolePaths.keySet()) {
			if (entailRolePaths.keySet().contains(key) && contraRolePaths.keySet().contains(key)){
				int occurEntail = entailRolePaths.get(key).size();
				int occurContra = contraRolePaths.get(key).size();
				int occurNeutral = neutralRolePaths.get(key).size();
				if (!all3.contains(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurContra)+"\t"+Integer.toString(occurNeutral))){
					all3.add(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurContra)+"\t"+Integer.toString(occurNeutral));
				}
			}
			else if (contraRolePaths.keySet().contains(key) && !entailRolePaths.keySet().contains(key)){
				int occurContra = contraRolePaths.get(key).size();
				int occurNeutral = neutralRolePaths.get(key).size();
				if (!neutralAndContra.contains(key+"\t"+Integer.toString(occurContra)+"\t"+Integer.toString(occurNeutral))){
					neutralAndContra.add(key+"\t"+Integer.toString(occurContra)+"\t"+Integer.toString(occurNeutral));
				}
			}
			else if (!contraRolePaths.keySet().contains(key) && entailRolePaths.keySet().contains(key)){
				int occurEntail = entailRolePaths.get(key).size();
				int occurNeutral = neutralRolePaths.get(key).size();
				if (!entailAndNeutral.contains(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurNeutral))){
					entailAndNeutral.add(key+"\t"+Integer.toString(occurEntail)+"\t"+Integer.toString(occurNeutral));
				}
			}
			else {
				neutral.add(key);
			}
		}
		
		System.out.println("all3");
		System.out.println(all3);
		System.out.println("entailAndNeutral");
		System.out.println(entailAndNeutral);
		System.out.println("entailAndContra");
		System.out.println(entailAndContra);
		System.out.println(neutralAndContra );
		System.out.println("neutralAndContra");
		System.out.println("neutral");
		System.out.println(neutral);
		System.out.println("contra");
		System.out.println(contra);
		System.out.println("entail");
		System.out.println(entail);
		
		*/
		
		// comment out due to multithreading; comment in if you do not want multithreading
		this.semGraph = new DepGraphToSemanticGraph(bert, tokenizer, wnDict, sumoContent);
		//this.semGraph = new DepGraphToSemanticGraph();

	}
	
	// Low-level method, not used for now.
	public HashMap<String,String> getVnToPWNMap(){
		return vnToPWNMap;
	}
	
	// Low-level method, not used for now.
	public void setVnToPWNMap(HashMap<String,String> vnToPWNMap){
		this.vnToPWNMap = vnToPWNMap;
	}
	
	// Low-level methods, not used for now.
	/*public HashMap<String,String> getAssoRulesMap(){
		return assoRulesMap;
	}
	
	public void setAssoRulesMap(HashMap<String,String> assoRulesMap){
		this.assoRulesMap = assoRulesMap;
	}*/
	
	// Low-level method, not used for now.
	public HashMap<String,ArrayList<HeadModifierPathPair>> getNeutralRolePaths(){
		return this.neutralRolePaths;
	}
	
	// Low-level method, not used for now.
	public HashMap<String,ArrayList<HeadModifierPathPair>> getEntailRolePaths(){
		return this.entailRolePaths;
	}
	
	// Low-level method, not used for now.
	public HashMap<String,ArrayList<HeadModifierPathPair>> getContraRolePaths(){
		return this.contraRolePaths;
	}
	
	// Low-level method, not used for now.
	public void setNeutralRolePaths(HashMap<String,ArrayList<HeadModifierPathPair>> neutralRolePaths){
		this.neutralRolePaths = neutralRolePaths;
	}
	
	// Low-level method, not used for now.
	public void setEntailRolePaths(HashMap<String,ArrayList<HeadModifierPathPair>> entailRolePaths){
		this.entailRolePaths = entailRolePaths;
	}
	
	// Low-level method, not used for now.
	public void setContraRolePaths(HashMap<String,ArrayList<HeadModifierPathPair>> contraRolePaths){
		this.contraRolePaths = contraRolePaths;
	}
	
	// Low-level method, not used for now.
	public void setPathsAndCtxs(ArrayList<String> pathsAndCtxs) {
		this.pathsAndCtxs = pathsAndCtxs;
	}
	
	// Low-level method, not used for now.
	public ArrayList<String> getPathsAndCtxs() {
		return this.pathsAndCtxs;
	}
	
	// Low-level method, not used for now.
	public void setAssociationRules(HashMap<String,String> associationRules){
		this.associationRules = associationRules;
	}
	
	// Low-level method, not used for now.
	public HashMap<String,String> getAssociationRules(){
		return associationRules;
	}
	
	// Low-level method.
	public KB getKB() {
		return kb;
	}
	
	/**
	 * Serialize the path combinations of the pairs and write them to files depending on 
	 * what the gold label of each path combination is. Used for experimenting. Not used currently.
	 * @param rolePaths
	 * @param type
	 */
	private void serialize(HashMap<String,ArrayList<HeadModifierPathPair>> rolePaths, String type){	
		FileOutputStream fileOut;
		ObjectOutputStream out;
		if (type.equals("entail")){
			try {
				fileOut = new FileOutputStream("serialized_RolePaths_entail.ser");
				out = new ObjectOutputStream(fileOut); 
				out.writeObject(rolePaths);
				out.close();
				fileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		else if (type.equals("contra")){
			try {
				fileOut = new FileOutputStream("serialized_RolePaths_contra.ser");
				out = new ObjectOutputStream(fileOut); 
				out.writeObject(rolePaths);
				out.close();
				fileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			try {
				fileOut = new FileOutputStream("serialized_RolePaths_neutral.ser");
				out = new ObjectOutputStream(fileOut); 
				out.writeObject(rolePaths);
				out.close();
				fileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Deserialize the path combinations of the matches. Used for experimenting. 
	 * Not used currently.
	 * @param rolePaths
	 * @param type
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String,ArrayList<HeadModifierPathPair>> deserialize(String type){
		HashMap<String,ArrayList<HeadModifierPathPair>> rolePaths = new HashMap<String,ArrayList<HeadModifierPathPair>>();
		FileInputStream fileIn;
		ObjectInputStream in;
		if (type.equals("entail")){
			try {
				fileIn = new FileInputStream("serialized_RolePaths_entail_2nd_learning.ser");
				in = new ObjectInputStream(fileIn);
				rolePaths = (HashMap<String, ArrayList<HeadModifierPathPair>>) in.readObject();
				/*for (String key: rolePaths.keySet()) {
					//System.out.println("+++++++entail+++++");
		        	//System.out.println(key);
					if (key.equals("[sem_subj]/[sem_obj]")) {
						ArrayList<HeadModifierPathPair> list = rolePaths.get(key);
						String test = "";
					}
				}
				ArrayList<Integer> lengths = new ArrayList<Integer>();
				for (String key: rolePaths.keySet()){
					lengths.add(rolePaths.get(key).size());
				}
				int sum = 0;
				for (Integer av : lengths){
					sum += av;
				}
				int average = sum/lengths.size();
				int max = Collections.max(lengths);
				int min = Collections.min(lengths);
				//System.out.println("Average entail:"+String.valueOf(average));
				//System.out.println("Max entail:"+String.valueOf(max));
				//System.out.println("Min entail:"+String.valueOf(min));*/
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
		} else if (type.equals("contra")) {
			try {
				fileIn = new FileInputStream("serialized_RolePaths_contra_2nd_learning.ser");
				in = new ObjectInputStream(fileIn);
		        rolePaths = (HashMap<String, ArrayList<HeadModifierPathPair>>) in.readObject();
		        /*for (String key: rolePaths.keySet()) {
		        	//System.out.println("+++++++contra+++++");
		        	//System.out.println(key);
					if (key.equals("[sem_subj]/[sem_obj]")) {
						ArrayList<HeadModifierPathPair> list = rolePaths.get(key);
						String test = "";
					}
				}
		        ArrayList<Integer> lengths = new ArrayList<Integer>();
				for (String key: rolePaths.keySet()){
					ArrayList<HeadModifierPathPair> test = rolePaths.get(key);
					lengths.add(rolePaths.get(key).size());
				}
				int sum = 0;
				for (Integer av : lengths){
					sum += av;
				}
				int average = sum/lengths.size();
				int max = Collections.max(lengths);
				int min = Collections.min(lengths);
				//System.out.println("Average neutral:"+String.valueOf(average));
				//System.out.println("Max neutral:"+String.valueOf(max));
				//System.out.println("Min neutral:"+String.valueOf(min));*/
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
		} else {
			try {
				fileIn = new FileInputStream("serialized_RolePaths_neutral_2nd_learning.ser");
				in = new ObjectInputStream(fileIn);
		        rolePaths = (HashMap<String, ArrayList<HeadModifierPathPair>>) in.readObject();
		        /*for (String key: rolePaths.keySet()) {
		        	//System.out.println("+++++++neutral+++++");
		        	//System.out.println(key);
					if (key.equals("[sem_subj]/[sem_obj]")) {
						ArrayList<HeadModifierPathPair> list = rolePaths.get(key);
						String test = "";
					}
				}
		        ArrayList<Integer> lengths = new ArrayList<Integer>();
				for (String key: rolePaths.keySet()){
					ArrayList<HeadModifierPathPair> test = rolePaths.get(key);
					lengths.add(rolePaths.get(key).size());
				}
				int sum = 0;
				for (Integer av : lengths){
					sum += av;
				}
				int average = sum/lengths.size();
				int max = Collections.max(lengths);
				int min = Collections.min(lengths);
				//System.out.println("Average neutral:"+String.valueOf(average));
				//System.out.println("Max neutral:"+String.valueOf(max));
				//System.out.println("Min neutral:"+String.valueOf(min));*/
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
		}

		return rolePaths;
	}
	

	/**
	 * Runs all stages of the GKR4NLI pipeline, from semantic preprocessing to inference decision.
	 * @param sent1
	 * @param sent2
	 * @param correctLabel
	 * @param kb
	 * @param pairID
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public InferenceDecision computeInference(String sent1, String sent2, String correctLabel, KB kb, String pairID) throws FileNotFoundException, UnsupportedEncodingException {	
		//long startTime = System.currentTimeMillis();
		List<SemanticGraph> texts = new ArrayList<SemanticGraph>();
		List<SemanticGraph> hypotheses = new ArrayList<SemanticGraph>();
		
		// with multithreading: does not work for now (JIGSAW no multi-threading safe)
		/*ExecutorService es = Executors.newFixedThreadPool(2);
	    Future<SemanticGraph> textThread = es.submit(new GKRConcurrentTask(sent1, sent2));
	    Future<SemanticGraph> hypThread = es.submit(new GKRConcurrentTask(sent2, sent1));
		
		SemanticGraph graphT = null;
	    SemanticGraph graphH = null;
		try {
			graphT = textThread.get();
			graphH = hypThread.get();
			es.shutdown();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		// without multithreading
		SemanticGraph graphT = semGraph.sentenceToGraph(sent1, sent1+" "+sent2);
	    SemanticGraph graphH = semGraph.sentenceToGraph(sent2, sent2+" "+sent1);
		//long endTime = System.currentTimeMillis();
		//System.out.println("That took " + (endTime - startTime) + " milliseconds");
		texts.add(graphT);	
		hypotheses.add(graphH);
		
		// 1. Get semantic preprocessing done: convert sentences to their GKR graphs.
		GNLIGraph gnli = new GNLIGraph(texts, hypotheses);	
		//gnli.getHypothesisGraph().displayRolesAndCtxs();
		//gnli.getTextGraph().displayRolesAndCtxs();
		//gnli.getTextGraph().displayDependencies();
		//gnli.getTextGraph().displayLex();
		//gnli.getTextGraph().displayProperties();
		
		// 2. Go through premise and hypothesis graphs making initial term matches.
		// This only compares nodes in the graphs, and takes no account of the edges
		// in the graphs.
		// These matches will be recorded on the match graph as extra match edges
		final InitialTermMatcher initialTermMatcher = new InitialTermMatcher(gnli, kb);
		initialTermMatcher.process();
		//gnli.display();
		//gnli.matchGraph.display();
		//graphT.displayLex();
		//graphH.displayLex();
		//gnli.getHypothesisGraph().displayContexts();
		//gnli.getHypothesisGraph().displayDependencies();
		//gnli.getTextGraph().displayDependencies();
		//gnli.getHypothesisGraph().displayRoles();
		//gnli.getHypothesisGraph().displayRolesAndCtxs();
		//gnli.getTextGraph().displayRolesAndCtxs();
		//gnli.getHypothesisGraph().displayRolesCtxsAndProperties();
		//gnli.getTextGraph().displayRolesCtxsAndProperties();
		//gnli.getTextGraph().displayCoref();
		//gnli.getHypothesisGraph().displayDependencies();
		//gnli.getTextGraph().displayRoles();
		/*gnli.getTextGraph().displayContexts();
		gnli.getTextGraph().displayDependencies();
		gnli.getTextGraph().displayRoles();*/
		

		// 3. Now look at the arc structure of the premise and hypothesis graphs to
		// update the specificity relations on the initial term matches
		String labelToLearn = "";
		if (learning == true)
			labelToLearn = correctLabel;
		PathScorer scorer = new PathScorer(gnli,100f, 200f, learning, this);
		final SpecificityUpdater su = new SpecificityUpdater(gnli,scorer, labelToLearn);
		su.updateSpecifity();	
		//gnli.matchGraph.display();
		
		// 4. Now look at the updated matches and context veridicalities to
		// determine entailment relations
		final InferenceChecker infCh = new InferenceChecker(gnli, this, su, labelToLearn, pairID);
		InferenceDecision decision =  infCh.getInferenceDecision();
		//gnli.display();
		return decision;
	}
	
	/**
	 * Compute the inference relation for each sentence of a testsuite.  
	 * @param file
	 * @param semGraph
	 * @param kb
	 * @throws IOException
	 */
	public void computeInferenceOfTestsuite(String file, DepGraphToSemanticGraph semGraph, KB kb) throws IOException{
		FileInputStream fileInput = new FileInputStream(file);
		InputStreamReader inputReader = new InputStreamReader(fileInput, "UTF-8");
		BufferedReader br = new BufferedReader(inputReader);
		// true stands for append = true (dont overwrite)
		// writes the inference relation of a pair into a file (along with its justification and confidence)
		FileWriter fileWriter =  new FileWriter(file.substring(0,file.indexOf(".txt"))+"_with_inference_relation.csv", true);
		BufferedWriter writer = new BufferedWriter(fileWriter);
		writer.write("ID\tSentenceA\tSentenceB\tGoldLabel\tGKR4NLILabel\tJustification\n");
		// writes the semantic features of a pair into a file to be used for the further processing of the hybrid classifier
		FileWriter fileWriter2 =  new FileWriter(file.substring(0,file.indexOf(".txt"))+"_with_semantic_features.csv", true);
		BufferedWriter writer2 = new BufferedWriter(fileWriter2);
		writer2.write("ID\tSentenceA\tSentenceB\tGoldLabel\tComplexCtxs\tcontraFlag\tVeridical\t"
				+ "Antiveridical\tAveridical\tEquals\tSuperclass\tSubclass\tDisjoint\tGKR4NLILabel\n");
		//FileOutputStream fileSer = new FileOutputStream(file.substring(0,file.indexOf(".txt"))+"_serialized_results.ser"); 
        //ObjectOutputStream writerSer = new ObjectOutputStream(fileSer); 
		
		// read in the association rules if we want to use them; we also need to uncomment the initialization in the constructor
		/*FileInputStream fileAssocRules = new FileInputStream("association_rules_fpgrowth.txt");
		InputStreamReader inputAssocRules = new InputStreamReader(fileAssocRules, "UTF-8");
		BufferedReader brAssocRules = new BufferedReader(inputAssocRules);
		String ruleLine;
        while ((ruleLine = brAssocRules.readLine()) != null) {
        	String[] elements = ruleLine.split("\\t");        	
			associationRules.put(elements[1], elements[0]);
		}
        brAssocRules.close();
        inputAssocRules.close();
        fileAssocRules.close();
        */
        
		
        ArrayList<InferenceDecision> decisionGraphs = new ArrayList<InferenceDecision>();
        ArrayList<String> pairs = new ArrayList<String>();
        String strLine;
        while ((strLine = br.readLine()) != null) {
        	pairs.add(strLine);
		}
        br.close();
        inputReader.close();
        fileInput.close();
        for (String pair : pairs) {
			if (pair.startsWith("####")){
				writer.write(pair+"\n\n");
				writer.flush();
				continue;
			}
			String[] elements = pair.split("\t");
			String id = elements[0];
			// comment in for SICK test set to do only the one direction
			//if (id.contains("b"))
			//	continue;
			String premise = elements[1];
			String hypothesis = elements[2];
			String correctLabel = elements[3];
			try {
				InferenceDecision decision = computeInference(premise, hypothesis, correctLabel, kb, id);
				if (decision != null){
					decisionGraphs.add(decision);
					String spec = "";
					if (decision.getJustifications() != null && !decision.getJustifications().isEmpty())
						spec = decision.getJustifications().toString();	
					// write inference relation to file
					writer.write(pair+"\t"+decision.getEntailmentRelation()+"\t"+spec+"\n");
					writer.flush();
					// compute and write semantic features to other file
					String complexCtxs = (decision.hHasComplexCtxs() || decision.tHasComplexCtxs()) ? "1" : "0";
					String contraCost = decision.hasContraFlag() ? "1" : "0";
					String verid = (decision.hHasVerCtx() || decision.tHasVerCtx()) ? "1" : "0";
					String antiverid = (decision.hHasAntiVerCtx() || decision.tHasAntiVerCtx()) ? "1" : "0";
					String averid = (decision.hHasAverCtx() || decision.tHasAverCtx()) ? "1" : "0";
					String equals = decision.hasEqualsRel() ? "1" : "0";
					String superR = decision.hasSuperRel() ? "1" : "0";
					String sub = decision.hasSubRel() ? "1" : "0";
					String dis = decision.hasDisjointRel() ? "1" : "0";
					writer2.write(pair+"\t"+complexCtxs + "\t"+ contraCost + "\t" + verid + "\t" + antiverid +
							"\t" + averid + "\t" + equals + "\t" + superR + "\t" + sub + "\t" + dis + "\t" +
							decision.getEntailmentRelation().toString().substring(0,1)+ "\n");
					writer2.flush();
					System.out.println("Processed pair "+ id);
				}
				else
					decisionGraphs.add(new InferenceDecision(EntailmentRelation.UNKNOWN, 0.0, 0.0, 0, false, false, false,false,false,
							false, false, false, false, false, false, false, false, null, false, false, null));
				
			} catch (Exception e){
				writer.write(pair+"\t"+"Exception found:"+e.getMessage()+"\n");
				writer.flush();
			}
			
		}
		// file to write the paths and the contexts learnt
		FileWriter fileWriterCtxPaths =  new FileWriter(file.substring(0,file.indexOf(".txt"))+"_paths_and_ctx.csv", true);
		BufferedWriter writerCtxPaths = new BufferedWriter(fileWriterCtxPaths);
		// Method for serialization of object 
		//writerSer.writeObject(decisionGraphs);   
        for (String line : pathsAndCtxs){
        	writerCtxPaths.write(line+"\n");
        }
        
		writer.close();
		fileWriter.close();
		writer2.close();
		fileWriter2.close();
		writerCtxPaths.close();
		fileWriterCtxPaths.close();		
		wnDict.close();
		//writerSer.close(); 
        //fileSer.close(); 
		//this.serialize(entailRolePaths, "entail");
		//this.serialize(neutralRolePaths, "neutral");
		//this.serialize(contraRolePaths, "contra");
           
	}
	
	/**
	 * Process a single pair to find the inference relation. 
	 * Print the result on the console for now.
	 * @param premise
	 * @param hypothesis
	 * @param correctLabel
	 * @param kb
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public void computeInferenceOfPair(String premise, String hypothesis, String correctLabel, KB kb) throws FileNotFoundException, UnsupportedEncodingException{
		InferenceDecision decision = computeInference(premise, hypothesis, correctLabel, kb, "1");
		System.out.println("Relation: "+decision.getEntailmentRelation());
		System.out.println("Strength of the Match: "+decision.getMatchStrength());
		System.out.println("It was loose contradiction: "+decision.isLooseContr());
		System.out.println("It was loose entailment: "+decision.isLooseEntail());
		System.out.println("Specificity that led to the decision:"+decision.getJustifications().toString());
		
	}
	
	/***
	 * Process a single pair to find the inference relation. 
	 * Print the result on the console for now.
	 */
	public InferenceDecision computeInferenceOfPair(String premise, String hypothesis, String correctLabel) throws FileNotFoundException, UnsupportedEncodingException{
		InferenceDecision decision = computeInference(premise, hypothesis, correctLabel, this.getKB(), "1");
		return decision;
	}
	
	
	/**
	 * Deserialize the computed inference pairs, if they have been stored serialized.
	 * @param file
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<InferenceDecision> deserializeFileWithComputedPairs(String file){
		ArrayList<InferenceDecision> pairsToReturn = null;
			try {
				FileInputStream fileIn = new FileInputStream(file.substring(0,file.indexOf(".txt"))+"_serialized_results.ser");
				ObjectInputStream in = new ObjectInputStream(fileIn);
				pairsToReturn = (ArrayList<InferenceDecision>) in.readObject();
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
			return pairsToReturn;
	}
	
	/**
	 * Investigate the inference labels given to pairs, after deserializing the 
	 * file they were written. 
	 * @param file
	 */
	public void investigateSerializedDecisions(String file){
		ArrayList<InferenceDecision> decisions = deserializeFileWithComputedPairs(file);
		for (InferenceDecision dec : decisions){
			GNLIGraph gnliGraph = dec.getGNLIGraph();
			gnliGraph.display();
			SemanticGraph hGraph = gnliGraph.hypothesisGraph;
			SemanticGraph tGraph = gnliGraph.textGraph;
			SemanticGraph mGraph = gnliGraph.matchGraph;
			hGraph.displayRolesAndCtxs();
			tGraph.displayRolesAndCtxs();
			mGraph.display();
			String relation = dec.getEntailmentRelation().toString();
			ArrayList<MatchEdge> justif = dec.getJustifications();
			System.out.println(relation);	
		}
	}
	
	/**
	 * Needed for multi-threading. Not use for now. JIGSAW does not allow multi-threading.
	 * @author Katerina Kalouli, 2019
	 *
	 */
	public class GKRConcurrentTask implements Callable<SemanticGraph> {
		 private String sent1;
		 private String sent2;
		 //private DepGraphToSemanticGraph semGraph;
	 
	    public GKRConcurrentTask(String sent1, String sent2) {
	    	this.sent1 = sent1;
		    this.sent2 = sent2;
		    //this.semGraph = semGraph;
	    }
	 
	    
	    public SemanticGraph call() {
	    	DepGraphToSemanticGraph semGraph = new DepGraphToSemanticGraph(bert, tokenizer, wnDict, sumoContent);
	    	SemanticGraph graph = semGraph.sentenceToGraph(sent1, sent1+" "+sent2);
	        return graph;
	    }
	}

	/**
	 * Main method with which the inference relation for a pair or a testsuite can be computed.
	 * Make sure to define the file or the pair you want to have computed. 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		InferenceComputer comp = new InferenceComputer();
		String premise = "The writer is less pleasant than the girl.";	
		String hypothesis = "The girl is not less pleasant than the writer.";
		//String file = "/Users/kkalouli/Documents/Stanford/comp_sem/SICK/annotations/to_check.txt"; //AeBBnA_and_PWN_annotated_checked_only_corrected_labels_split_pairs.txt";
		//String file = "/home/kkalouli/Documents/diss/SICK_train_trial/SICK_trial_and_train_both_dirs_corrected_only_a_and_Cb_and_Eb.txt";
		//String file = "/Users/kkalouli/Documents/Stanford/comp_sem/SICK/SICK_SemEval2014/sick_trial_and_train/to_check.txt";
		//String file = "/home/kkalouli/Documents/diss/SICK_test/SICK_test_annotated_both_dirs_corrected.txt";
		//String file = "/home/kkalouli/Documents/diss/test_heidel.txt";
		//String file = "/home/kkalouli/Documents/diss/SICK_test/to_check.txt";
		String file = "/Users/kkalouli/Desktop/test.txt";
		//String file = "/Users/kkalouli/Documents/Corpora/FraCaS/single_premise/fracas_sent_only_single_premise.txt";
		//tring file = "/home/kkalouli/Desktop/SICK_trial_and_train_both_dirs_corrected_only_a_and_Cb_and_Eb.txt";
		//String file = "/home/kkalouli/Documents/diss/experiments/SICK_unique_sent_sanity_check.txt";
		//String file = "/home/kkalouli/Documents/diss/experiments/heuristics_evaluation_set_cleaned.txt";
		//String file = "/Users/kkalouli/Documents/QuestionsAtTheInterfaces/P8/heidelberg_collaboration/test_heidel.txt";
		//String file = "/home/kkalouli/Documents/diss/experiments/still_to_be_checked_only_NEUTRAL_low_jac_similarity_both_dirs.txt";
		//String file = "/home/kkalouli/Documents/diss/experiments/heuristics_evaluation_set_cleaned.txt";
		//String file = "/home/kkalouli/Desktop/not_run_through_gnli_yet_both_dirs.txt";
		//String file = "/Users/kkalouli/Desktop/test.txt";
		//String file = "/Users/kkalouli/Documents/Stanford/comp_sem/SICK/SICK reannotation/Larry_and_Hai_collaboration/to_check.txt";
		//String file = "/home/kkalouli/Documents/diss/experiments/still_to_be_checked_only_NEUTRAL_low_jac_similarity_both_dirs.txt";
		//String file = "/home/kkalouli/Documents/diss/experiments/heuristics_evaluation_set_cleaned.txt";
		
		//comp.computeInferenceOfPair(premise, hypothesis, "E", kb);
		comp.computeInferenceOfTestsuite(file, semGraph, kb);	
	}
}
