package gnli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

import gnli.GNLIGraph;
import gnli.InitialTermMatcher;
import gnli.InferenceChecker.InferenceDecision;
import sem.mapper.DepGraphToSemanticGraph;
import semantic.graph.SemanticGraph;
import semantic.graph.SemanticNode;
import semantic.graph.vetypes.SkolemNodeContent;


public class InferenceComputer {
	
	private static KB kb;
	private static DepGraphToSemanticGraph semGraph;
	private boolean learning;
	

	public InferenceComputer() throws FileNotFoundException, UnsupportedEncodingException {
		KBmanager.getMgr().initializeOnce("/Users/kkalouli/Documents/.sigmakee/KBs");	
		this.kb = KBmanager.getMgr().getKB("SUMO");
		this.semGraph = new DepGraphToSemanticGraph();
		this.learning = true;
	}
	

	public InferenceDecision computeInference(DepGraphToSemanticGraph semGraph, String sent1, String sent2, String correctLabel, KB kb) throws FileNotFoundException, UnsupportedEncodingException {	
		List<SemanticGraph> texts = new ArrayList<SemanticGraph>();
		List<SemanticGraph> hypotheses = new ArrayList<SemanticGraph>();
		SemanticGraph graphT = semGraph.sentenceToGraph(sent1,sent1 + " " + sent2);
		texts.add(graphT);
		SemanticGraph graphH = semGraph.sentenceToGraph(sent2,sent2 + " " + sent2);
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
		

		// Now look at the arc structure of the premise and hypothesis graphs to
		// update the specificity relations on the initial term matches
		String labelToLearn = "";
		if (learning == true)
			labelToLearn = correctLabel;
		final SpecificityUpdater su = new SpecificityUpdater(gnli, new PathScorer(gnli,30f), labelToLearn);
		su.updateSpecifity();
		
		
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
			InferenceDecision decision = computeInference(semGraph, premise, hypothesis, correctLabel, kb);
			Specificity spec = null;
			if (decision.getJustification() != null)
				spec = decision.getJustification().getSpecificity();	
			writer.write(strLine+"\t"+decision.getEntailmentRelation()+"\t"+decision.getMatchStrength()+"\t"+decision.isLooseContr()+
					"\t"+decision.isLooseEntail()+"\t"+spec+"\n");
			writer.flush();
			System.out.println("Processed pair "+ id);
		}
		writer.close();
		br.close();
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
		System.out.println("Specificity that led to the decision:"+decision.getJustification().getSpecificity());
		
	}
	
	public static void main(String args[]) throws IOException {
		//KBmanager.getMgr().initializeOnce("/Users/kkalouli/Documents/.sigmakee/KBs");	
		//KB kb = KBmanager.getMgr().getKB("SUMO");
		InferenceComputer comp = new InferenceComputer();
		//DepGraphToSemanticGraph semGraph = new DepGraphToSemanticGraph();
		// TODO: change label for embed match
		String premise = "A brown dog is attacking another animal in front of the tall man in pants.";
		String hypothesis = "A brown dog is attacking another animal in front of the man in pants.";
		String file = "/Users/kkalouli/Documents/Stanford/comp_sem/SICK/annotations/test.txt"; //AeBBnA_and_PWN_annotated_checked_only_correcetd_labels_split_pairs.txt";
		//comp.computeInferenceOfPair(semGraph, premise, hypothesis, kb);
		comp.computeInferenceOfTestsuite(file, semGraph, kb);

	}

}
