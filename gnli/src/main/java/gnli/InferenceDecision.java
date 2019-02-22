package gnli;

import java.io.Serializable;

import gnli.InferenceChecker.EntailmentRelation;

public class InferenceDecision implements Serializable {
		
		/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		EntailmentRelation relation;
		double matchStrength;
		MatchEdge justification;
		boolean looseContra;
		boolean looseEntail;
		GNLIGraph gnliGraph;
			
		InferenceDecision(EntailmentRelation relation, double matchStrength, MatchEdge justification, boolean looseContra, boolean looseEntail, GNLIGraph gnliGraph){
			this.relation = relation;
			this.matchStrength = matchStrength;
			this.justification = justification;
			this.looseContra = looseContra;
			this.looseEntail = looseEntail;
			this.gnliGraph = gnliGraph;
		}
		
		public EntailmentRelation getEntailmentRelation(){
			return relation;
		}
		
		public Double getMatchStrength(){
			return matchStrength;
		}
		
		public boolean isLooseContr(){
			return looseContra;
		}
		
		public boolean isLooseEntail(){
			return looseEntail;
		}
		
		public MatchEdge getJustification(){
			return justification;
		}
		
		public GNLIGraph getGNLIGraph(){
			return gnliGraph;
		}
			
}
	
