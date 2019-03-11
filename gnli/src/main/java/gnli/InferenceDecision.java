package gnli;

import java.io.Serializable;
import java.util.ArrayList;

import gnli.InferenceChecker.EntailmentRelation;

public class InferenceDecision implements Serializable {

	private static final long serialVersionUID = 5461367650157213499L;
	private EntailmentRelation relation;
	private double matchStrength;
	private	ArrayList<MatchEdge> justifications;
	private	boolean looseContra;
	private	boolean looseEntail;
	private	GNLIGraph gnliGraph;
			
		InferenceDecision(EntailmentRelation relation, double matchStrength, ArrayList<MatchEdge> justifications, boolean looseContra, boolean looseEntail, GNLIGraph gnliGraph){
			this.relation = relation;
			this.matchStrength = matchStrength;
			this.justifications = justifications;
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
		
		public ArrayList<MatchEdge> getJustifications(){
			return justifications;
		}
		
		public GNLIGraph getGNLIGraph(){
			return gnliGraph;
		}
			
}
	
