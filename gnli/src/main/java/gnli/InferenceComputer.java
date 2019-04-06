package gnli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

import gnli.GNLIGraph;
import gnli.InferenceChecker.EntailmentRelation;
import gnli.InitialTermMatcher;
import sem.graph.SemanticGraph;
import sem.mapper.DepGraphToSemanticGraph;



public class InferenceComputer {

	private static KB kb;
	private static DepGraphToSemanticGraph semGraph;
	private boolean learning;
	private Properties props;
	private String sumoKB;

	public InferenceComputer() throws FileNotFoundException, UnsupportedEncodingException {
		// load the classloader to get the properties of the properties file
		InputStream properties = getClass().getClassLoader().getResourceAsStream("gnli.properties");
		this.props = new Properties();
		try {
			props.load(new InputStreamReader(properties));
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
		this.semGraph = new DepGraphToSemanticGraph();
		this.learning = true;
	}
	

	public InferenceDecision computeInference(DepGraphToSemanticGraph semGraph, String sent1, String sent2, String correctLabel, KB kb) throws FileNotFoundException, UnsupportedEncodingException {	
		List<SemanticGraph> texts = new ArrayList<SemanticGraph>();
		List<SemanticGraph> hypotheses = new ArrayList<SemanticGraph>();
		SemanticGraph graphT = semGraph.sentenceToGraph(sent1, sent1+" "+sent2);
		texts.add(graphT);
		SemanticGraph graphH = semGraph.sentenceToGraph(sent2, sent2+" "+sent1);
		hypotheses.add(graphH);
		GNLIGraph gnli = new GNLIGraph(texts, hypotheses);
		
		// Go through premise and hypothesis graphs making initial term matches.
		// This only compares nodes in the graphs, and takes no account of the edges
		// in the graphs.
		// These matches will be recorded on the ecd graph as extra match edges
		final InitialTermMatcher initialTermMatcher = new InitialTermMatcher(gnli, kb);
		initialTermMatcher.process();
		//gnli.display();
		//gnli.matchGraph.display();
		/*gnli.getHypothesisGraph().displayContexts();
		gnli.getHypothesisGraph().displayDependencies();
		gnli.getHypothesisGraph().displayRoles();
		gnli.getTextGraph().displayContexts();
		gnli.getTextGraph().displayDependencies();
		gnli.getTextGraph().displayRoles();*/
		

		

		// Now look at the arc structure of the premise and hypothesis graphs to
		// update the specificity relations on the initial term matches
		String labelToLearn = "";
		if (learning == true)
			labelToLearn = correctLabel;
		PathScorer scorer = new PathScorer(gnli,50f, learning);
		final SpecificityUpdater su = new SpecificityUpdater(gnli,scorer, labelToLearn);
		//System.out.println(scorer.getAllowedRolePaths());
		su.updateSpecifity();	
		scorer.serialize(scorer.getEntailRolePaths(), "entail");
		scorer.serialize(scorer.getNeutralRolePaths(), "neutral");
		// Now look at the updated matches and context veridicalities to
		// determine entailment relations
		final InferenceChecker infCh = new InferenceChecker(gnli);
		InferenceDecision decision =  infCh.getInferenceDecision();
		return decision;
	}
	
	
	public void computeInferenceOfTestsuite(String file, DepGraphToSemanticGraph semGraph, KB kb) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		// true stands for append = true (dont overwrite)
		BufferedWriter writer = new BufferedWriter( new FileWriter(file.substring(0,file.indexOf(".txt"))+"_with_inference_relation.csv", true));
		FileOutputStream fileSer = new FileOutputStream(file.substring(0,file.indexOf(".txt"))+"_serialized_results.ser"); 
        ObjectOutputStream writerSer = new ObjectOutputStream(fileSer); 
        ArrayList<InferenceDecision> decisionGraphs = new ArrayList<InferenceDecision>();
		String strLine;
		while ((strLine = br.readLine()) != null) {
			if (strLine.startsWith("####")){
				writer.write(strLine+"\n\n");
				writer.flush();
				continue;
			}
			String[] elements = strLine.split("\t");
			String id = elements[0];
			String premise = elements[1];
			String hypothesis = elements[2];
			String correctLabel = elements[3];
			try {
				InferenceDecision decision = computeInference(semGraph, premise, hypothesis, correctLabel, kb);
				if (decision != null){
					decisionGraphs.add(decision);
					String spec = "";
					if (decision.getJustifications() != null && !decision.getJustifications().isEmpty())
						spec = decision.getJustifications().toString();	
					writer.write(strLine+"\t"+decision.getEntailmentRelation()+"\t"+decision.getMatchStrength()+"\t"+decision.isLooseContr()+
							"\t"+decision.isLooseEntail()+"\t"+spec+"\n");
					writer.flush();
					System.out.println("Processed pair "+ id);
				}
				else
					decisionGraphs.add(new InferenceDecision(EntailmentRelation.UNKNOWN, 0.0, null, false, false, null));
				
			} catch (Exception e){
				writer.write(strLine+"\t"+"Exception found:"+e.getMessage()+"\n");
				writer.flush();
			}
			
		}
		// Method for serialization of object 
		writerSer.writeObject(decisionGraphs);   
		
		writer.close();
		br.close();
		writerSer.close(); 
        fileSer.close(); 
           
	}/*
	
	
	/***
	 * Process a single pair to find the inference relation. 
	 * Print the result on the console for now.
	 */
	public void computeInferenceOfPair(DepGraphToSemanticGraph semGraph, String premise, String hypothesis, String correctLabel, KB kb) throws FileNotFoundException, UnsupportedEncodingException{
		InferenceDecision decision = computeInference(semGraph, premise, hypothesis, correctLabel, kb);
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
	
	public static void main(String args[]) throws IOException {
		//String configFile = "/Users/kkalouli/Documents/project/gnli/gnli.properties";
		//String configFile = "/Users/caldadmin/Documents/diss/gnli.properties";
		//String configFile = "/home/kkalouli/Documents/diss/gnli.properties";
		InferenceComputer comp = new InferenceComputer();
		//DepGraphToSemanticGraph semGraph = new DepGraphToSemanticGraph();
		// TODO: change label for embed match
		String premise = "There is no man in a black jacket doing tricks on a motorbike.";
		String hypothesis = "A person in a black jacket is doing tricks on a motorbike.";
		String file = "/Users/kkalouli/Documents/Stanford/comp_sem/SICK/annotations/to_check.txt"; //AeBBnA_and_PWN_annotated_checked_only_corrected_labels_split_pairs.txt";
		//String file = "/Users/caldadmin/Documents/diss/to_check.txt";
		//String file = "/home/kkalouli/Documents/diss/to_check.txt";
		//comp.computeInferenceOfPair(semGraph, premise, hypothesis, "C", kb);
		comp.computeInferenceOfTestsuite(file, semGraph, kb);
		//comp.deserializeFileWithComputedPairs(file);
	}
	
	

}
