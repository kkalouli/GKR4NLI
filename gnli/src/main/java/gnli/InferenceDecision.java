package gnli;

import java.io.Serializable;
import java.util.ArrayList;

import gnli.InferenceChecker.EntailmentRelation;

public class InferenceDecision implements Serializable {

	private static final long serialVersionUID = 5461367650157213499L;
	private EntailmentRelation relation;
	private EntailmentRelation alternativeRelation;
	private double matchStrength;
	private double matchConfidence;
	private	ArrayList<MatchEdge> justifications;
	private	boolean looseContra;
	private	boolean looseEntail;
	private	GNLIGraph gnliGraph;
			
		InferenceDecision(EntailmentRelation relation, double matchStrength,double matchConfidence, EntailmentRelation alternativeRelation, ArrayList<MatchEdge> justifications, boolean looseContra, boolean looseEntail, GNLIGraph gnliGraph){
			this.relation = relation;
			this.alternativeRelation = alternativeRelation;
			this.matchStrength = matchStrength;
			this.matchConfidence = matchConfidence;
			this.justifications = justifications;
			this.looseContra = looseContra;
			this.looseEntail = looseEntail;
			this.gnliGraph = gnliGraph;
		}
		
		public EntailmentRelation getEntailmentRelation(){
			return relation;
		}
		
		public EntailmentRelation getAlternativeEntailmentRelation(){
			return alternativeRelation;
		}
		
		public Double getMatchStrength(){
			return matchStrength;
		}
		
		public Double getMatchConfidence(){
			return matchConfidence;
		}
		
		public boolean isLooseContr(){
			return looseContra;
		}
		
		public boolean isLooseEntail(){
			return looseEntail;
		}
		
		public ArrayList<MatchEdge> getJustifications(){
			return justifications;
		}
		
		public GNLIGraph getGNLIGraph(){
			return gnliGraph;
		}
			
}
	
