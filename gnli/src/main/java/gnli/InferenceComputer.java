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
import sem.graph.SemanticGraph;
import sem.mapper.DepGraphToSemanticGraph;



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
	private String sumoContent;
	private String bertVocab;


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
		//KBmanager.getMgr().initializeOnce("/Users/caldadmin/Documents/.sigmakee/KBs");
		//KBmanager.getMgr().initializeOnce("/home/kkalouli/Documents/.sigmakee/KBs");
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
		// for learning==true
		//this.entailRolePaths = new HashMap<String, ArrayList<HeadModifierPathPair>>();
		//this.neutralRolePaths =  new HashMap<String, ArrayList<HeadModifierPathPair>>();
		//this.contraRolePaths =  new HashMap<String, ArrayList<HeadModifierPathPair>>();
		// for learning==false
		this.entailRolePaths = deserialize("entail");
		this.neutralRolePaths =  deserialize("neutral");
		this.contraRolePaths =  deserialize("contra");
		// comment out due to multithreading; comment in if you do not want multithreading
		this.semGraph = new DepGraphToSemanticGraph(bert, tokenizer, wnDict, sumoContent);
		//this.semGraph = new DepGraphToSemanticGraph();

	}
	
	public HashMap<String,ArrayList<HeadModifierPathPair>> getNeutralRolePaths(){
		return this.neutralRolePaths;
	}
	
	public HashMap<String,ArrayList<HeadModifierPathPair>> getEntailRolePaths(){
		return this.entailRolePaths;
	}
	
	public HashMap<String,ArrayList<HeadModifierPathPair>> getContraRolePaths(){
		return this.contraRolePaths;
	}
	
	public void setNeutralRolePaths(HashMap<String,ArrayList<HeadModifierPathPair>> neutralRolePaths){
		this.neutralRolePaths = neutralRolePaths;
	}
	
	public void setEntailRolePaths(HashMap<String,ArrayList<HeadModifierPathPair>> entailRolePaths){
		this.entailRolePaths = entailRolePaths;
	}
	
	public void setContraRolePaths(HashMap<String,ArrayList<HeadModifierPathPair>> contraRolePaths){
		this.contraRolePaths = contraRolePaths;
	}
	
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
	
	@SuppressWarnings("unchecked")
	public HashMap<String,ArrayList<HeadModifierPathPair>> deserialize(String type){
		HashMap<String,ArrayList<HeadModifierPathPair>> rolePaths = new HashMap<String,ArrayList<HeadModifierPathPair>>();
		FileInputStream fileIn;
		ObjectInputStream in;
		if (type.equals("entail")){
			try {
				fileIn = new FileInputStream("serialized_RolePaths_entail.ser");
				in = new ObjectInputStream(fileIn);
				rolePaths = (HashMap<String, ArrayList<HeadModifierPathPair>>) in.readObject();
				/*for (String key: rolePaths.keySet()) {
					if (key.equals("[sem_subj]/[sem_obj]")) {
						ArrayList<HeadModifierPathPair> list = rolePaths.get(key);
						String test = "";
					}
				}
				/*ArrayList<Integer> lengths = new ArrayList<Integer>();
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
				System.out.println("Average entail:"+String.valueOf(average));
				System.out.println("Max entail:"+String.valueOf(max));
				System.out.println("Min entail:"+String.valueOf(min));*/
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
				fileIn = new FileInputStream("serialized_RolePaths_contra.ser");
				in = new ObjectInputStream(fileIn);
		        rolePaths = (HashMap<String, ArrayList<HeadModifierPathPair>>) in.readObject();
		        /*for (String key: rolePaths.keySet()) {
					ArrayList<HeadModifierPathPair> list = rolePaths.get(key);
					String test = "";
				}
		        /*ArrayList<Integer> lengths = new ArrayList<Integer>();
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
				System.out.println("Average neutral:"+String.valueOf(average));
				System.out.println("Max neutral:"+String.valueOf(max));
				System.out.println("Min neutral:"+String.valueOf(min));*/
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
				fileIn = new FileInputStream("serialized_RolePaths_neutral.ser");
				in = new ObjectInputStream(fileIn);
		        rolePaths = (HashMap<String, ArrayList<HeadModifierPathPair>>) in.readObject();
		        /*ArrayList<Integer> lengths = new ArrayList<Integer>();
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
				System.out.println("Average neutral:"+String.valueOf(average));
				System.out.println("Max neutral:"+String.valueOf(max));
				System.out.println("Min neutral:"+String.valueOf(min));*/
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
	

	public InferenceDecision computeInference(String sent1, String sent2, String correctLabel, KB kb) throws FileNotFoundException, UnsupportedEncodingException {	
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
		GNLIGraph gnli = new GNLIGraph(texts, hypotheses);
		
		gnli.getHypothesisGraph().displayRolesAndCtxs();
		gnli.getTextGraph().displayRolesAndCtxs();
		gnli.getTextGraph().displayDependencies();
		//gnli.getTextGraph().displayLex();
		//gnli.getTextGraph().displayProperties();
		
		// Go through premise and hypothesis graphs making initial term matches.
		// This only compares nodes in the graphs, and takes no account of the edges
		// in the graphs.
		// These matches will be recorded on the ecd graph as extra match edges
		final InitialTermMatcher initialTermMatcher = new InitialTermMatcher(gnli, kb);
		initialTermMatcher.process();
		//gnli.display();
		gnli.matchGraph.display();
		//graphT.displayLex();
		//graphH.displayLex();
		//gnli.getHypothesisGraph().displayContexts();
		//gnli.getHypothesisGraph().displayDependencies();
		//gnli.getHypothesisGraph().displayRoles();
		//gnli.getHypothesisGraph().displayRolesAndCtxs();
		//gnli.getTextGraph().displayRolesAndCtxs();
		//gnli.getHypothesisGraph().displayDependencies();
		//gnli.getTextGraph().displayRoles();
		/*gnli.getTextGraph().displayContexts();
		gnli.getTextGraph().displayDependencies();
		gnli.getTextGraph().displayRoles();*/
		

		
		

		// Now look at the arc structure of the premise and hypothesis graphs to
		// update the specificity relations on the initial term matches
		String labelToLearn = "";
		if (learning == true)
			labelToLearn = correctLabel;
		PathScorer scorer = new PathScorer(gnli,100f, learning, this);
		final SpecificityUpdater su = new SpecificityUpdater(gnli,scorer, labelToLearn);
		su.updateSpecifity();	
		gnli.matchGraph.display();
		// Now look at the updated matches and context veridicalities to
		// determine entailment relations
		final InferenceChecker infCh = new InferenceChecker(gnli);
		InferenceDecision decision =  infCh.getInferenceDecision();
		return decision;
	}
	
	
	public void computeInferenceOfTestsuite(String file, DepGraphToSemanticGraph semGraph, KB kb) throws IOException{
		FileInputStream fileInput = new FileInputStream(file);
		InputStreamReader inputReader = new InputStreamReader(fileInput, "UTF-8");
		BufferedReader br = new BufferedReader(inputReader);
		// true stands for append = true (dont overwrite)
		FileWriter fileWriter =  new FileWriter(file.substring(0,file.indexOf(".txt"))+"_with_inference_relation.csv", true);
		BufferedWriter writer = new BufferedWriter(fileWriter);
		//FileOutputStream fileSer = new FileOutputStream(file.substring(0,file.indexOf(".txt"))+"_serialized_results.ser"); 
        //ObjectOutputStream writerSer = new ObjectOutputStream(fileSer); 
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
			String premise = elements[1];
			String hypothesis = elements[2];
			String correctLabel = elements[3];
			try {
				InferenceDecision decision = computeInference(premise, hypothesis, correctLabel, kb);
				if (decision != null){
					decisionGraphs.add(decision);
					String spec = "";
					if (decision.getJustifications() != null && !decision.getJustifications().isEmpty())
						spec = decision.getJustifications().toString();	
					writer.write(pair+"\t"+decision.getEntailmentRelation()+"\t"+decision.getMatchStrength()+"\t"+decision.isLooseContr()+
							"\t"+decision.isLooseEntail()+"\t"+spec+"\n");
					writer.flush();
					System.out.println("Processed pair "+ id);
				}
				else
					decisionGraphs.add(new InferenceDecision(EntailmentRelation.UNKNOWN, 0.0, null, false, false, null));
				
			} catch (Exception e){
				writer.write(pair+"\t"+"Exception found:"+e.getMessage()+"\n");
				writer.flush();
			}
			
		}

		// Method for serialization of object 
		//writerSer.writeObject(decisionGraphs);   
		
		writer.close();
		fileWriter.close();
		wnDict.close();
		//writerSer.close(); 
        //fileSer.close(); 
		this.serialize(entailRolePaths, "entail");
		this.serialize(neutralRolePaths, "neutral");
		this.serialize(contraRolePaths, "contra");
           
	}/*
	
	
	/***
	 * Process a single pair to find the inference relation. 
	 * Print the result on the console for now.
	 */
	public void computeInferenceOfPair(DepGraphToSemanticGraph semGraph, String premise, String hypothesis, String correctLabel, KB kb) throws FileNotFoundException, UnsupportedEncodingException{
		InferenceDecision decision = computeInference(premise, hypothesis, correctLabel, kb);
		System.out.println("Relation: "+decision.getEntailmentRelation());
		System.out.println("Strength of the Match: "+decision.getMatchStrength());
		System.out.println("It was loose contradiction: "+decision.isLooseContr());
		System.out.println("It was loose entailment: "+decision.isLooseEntail());
		System.out.println("Specificity that led to the decision:"+decision.getJustifications().toString());
		
	}
	
	
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

	
	public static void main(String args[]) throws IOException {
		//String configFile = "/Users/kkalouli/Documents/project/gnli/gnli.properties";
		//String configFile = "/Users/caldadmin/Documents/diss/gnli.properties";
		//String configFile = "/home/kkalouli/Documents/diss/gnli.properties";
		InferenceComputer comp = new InferenceComputer();
		//long startTime = System.currentTimeMillis();
		//DepGraphToSemanticGraph semGraph = new DepGraphToSemanticGraph();
		// TODO: change label for embed match
		String premise = "2 men are walking.";	
		String hypothesis = "2 people are walking.";
		//String file = "/Users/kkalouli/Documents/Stanford/comp_sem/SICK/annotations/to_check.txt"; //AeBBnA_and_PWN_annotated_checked_only_corrected_labels_split_pairs.txt";
		//String file = "/home/kkalouli/Documents/diss/SICK_train_trial/SICK_trial_and_train_both_dirs_corrected_only_entail_and_neutral_active.txt";
		String file = "/Users/kkalouli/Documents/Stanford/comp_sem/SICK/SICK_SemEval2014/sick_trial_and_train/to_check.txt";
		//String file = "/home/kkalouli/Documents/diss/to_check.txt";
		//comp.computeInferenceOfPair(semGraph, premise, hypothesis, "E", kb);
		comp.computeInferenceOfTestsuite(file, semGraph, kb);
		//long endTime = System.currentTimeMillis();
		//System.out.println("The whole thing took " + (endTime - startTime) + " milliseconds");
		//comp.deserializeFileWithComputedPairs(file);
		//comp.deserialize("entail");
		//comp.deserialize("contra");
	}
	
	

}
