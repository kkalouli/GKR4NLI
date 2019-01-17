package gnli;

import java.util.Arrays;
import java.util.List;

import gnli.GNLIGraph;
import gnli.InitialTermMatcher;


public class InferenceComputer {
	

	public InferenceComputer() {
	}
	
	/**
	 * Run ecd on a set of premise/text and conclusion/hypothesis semantic graphs
	 * @param premises
	 * @param hypothesis
	 * @param debug
	 *         displays additional diagnostic information if true
	 * @return
	 *  An {@link EcdResult} which comprises an {@link EcdGraph} plus scores.
	 */	
	/*public EcdResult ecd(final List<SemanticGraph> premises,
			final SemanticGraph hypothesis, final boolean debug) {

		long start = System.currentTimeMillis();
		//LexicalUtil.startTimer();
		
		// Set up an initial graph containing the premises and hypothesis
		final EcdGraph ecdGraph = new EcdGraph(premises,
				Arrays.asList(new SemanticGraph[] { hypothesis }));
		long end = System.currentTimeMillis();
		System.out.println();
		//System.out.println("Facts : "
				//+ DurationFormatUtils
				//.formatDuration(end - start, "HH:mm:ss:SS"));

		// Go through premise and hypothesis graphs making initial term matches.
		// This only compares nodes in the graphs, and takes no account of the edges
		// in the graphs.
		// These matches will be recorded on the ecd graph as extra match edges
		start = System.currentTimeMillis();
		final InitialTermMatcher initialTermMatcher = new InitialTermMatcher(
				ecdGraph);
		initialTermMatcher.process();
		end = System.currentTimeMillis();
		//System.out.println("Initial term match: "
		//		+ DurationFormatUtils
		//		.formatDuration(end - start, "HH:mm:ss:SS"));
		if (debug) {System.out.println(ecdGraph.matchDisplay(false));}

		// Now look at the arc structure of the premise and hypothesis graphs to
		// update the specificity relations on the initial term matches
		start = System.currentTimeMillis();
		final SpecificityUpdater su = new SpecificityUpdater(ecdGraph, pathScorer);
		su.updateSpecifity();
		end = System.currentTimeMillis();
		//System.out.println("Specificity update: "
		//		+ DurationFormatUtils
			//	.formatDuration(end - start, "HH:mm:ss:SS"));
		if (debug) {System.out.println(ecdGraph.matchDisplay(true));}
		if (debug) {ecdGraph.display();}
		
		// Now look at the updated matches and context veridicalities to
		// determine entailment relations
		start = System.currentTimeMillis();
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
		return retval;
	}*/

}
