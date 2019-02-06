package gnli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import gnli.GNLIGraph;
import gnli.InitialTermMatcher;
import sem.mapper.DepGraphToSemanticGraph;
import semantic.graph.SemanticGraph;


public class InferenceComputer {
	
	

	public InferenceComputer() {
	}
	

	public void computeInference(String sent1, String sent2) throws FileNotFoundException, UnsupportedEncodingException {	
		DepGraphToSemanticGraph semGraph = new DepGraphToSemanticGraph();
		List<SemanticGraph> texts = new ArrayList<SemanticGraph>();
		List<SemanticGraph> hypotheses = new ArrayList<SemanticGraph>();
		SemanticGraph graphT = semGraph.sentenceToGraph(sent1,sent1 + " " + sent2);
		graphT.displayLex();
		texts.add(graphT);
		SemanticGraph graphH = semGraph.sentenceToGraph(sent2,sent2 + " " + sent2);
		hypotheses.add(graphH);
		GNLIGraph gnli = new GNLIGraph(texts, hypotheses);
		
		// Go through premise and hypothesis graphs making initial term matches.
		// This only compares nodes in the graphs, and takes no account of the edges
		// in the graphs.
		// These matches will be recorded on the ecd graph as extra match edges
		final InitialTermMatcher initialTermMatcher = new InitialTermMatcher(gnli);
		initialTermMatcher.process();
		gnli.display();
		gnli.matchGraph.display();
		

		// Now look at the arc structure of the premise and hypothesis graphs to
		// update the specificity relations on the initial term matches
		final SpecificityUpdater su = new SpecificityUpdater(gnli, new PathScorer(gnli,30f));
		su.updateSpecifity();
		
		
		// Now look at the updated matches and context veridicalities to
		// determine entailment relations
		/*start = System.currentTimeMillis();
		final EntailmentChecker ck = new EntailmentChecker(ecdGraph);
		EcdResult retval = ck.getEcdResult();
		end = System.currentTimeMillis();
		//System.out.println("Entailment Check: "
			//	+ DurationFormatUtils
			//	.formatDuration(end - start, "HH:mm:ss:SS"));
		//System.out.println("DB Lookup : "
		//		+ DurationFormatUtils
		//		.formatDuration(LexicalUtil.getTime(), "HH:mm:ss:SS"));
		
		// Return the EcdResult
		// TODO: In future, it may be better to return the EcdGraph
		return retval;*/
	}
	
	public static void main(String args[]) throws IOException {
		InferenceComputer comp = new InferenceComputer();
		// TODO: change label for embed match
		String text = "All little girls buy dresses.";
		String hypothesis = "All girls buy dresses.";
		comp.computeInference(text, hypothesis);

	}

}
