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
	private boolean tHasComplexCtxs;
	private boolean hHasComplexCtxs;
	private boolean contraFlag;
	private boolean tVeridical;
	private boolean tAntiveridical;
	private boolean tAveridical;
	private boolean hVeridical;
	private boolean hAntiveridical;
	private boolean hAveridical;
	private boolean equalsRel;
	private boolean superRel;
	private boolean subRel;
	private boolean disjointRel;
	private Integer ruleUsed;
			
		InferenceDecision(EntailmentRelation relation, double matchStrength,double matchConfidence, Integer ruleUsed, boolean tHasComplexCtxs, boolean hHasComplexCtxs, boolean contraFlag,
				boolean tVeridical, boolean tAntiveridical, boolean tAveridical, boolean hVeridical, boolean hAntiveridical, boolean hAveridical,
				boolean equalsRel, boolean superRel, boolean subRel, boolean disjointRel, EntailmentRelation alternativeRelation, ArrayList<MatchEdge> justifications, boolean looseContra, boolean looseEntail, GNLIGraph gnliGraph){
			this.relation = relation;
			this.alternativeRelation = alternativeRelation;
			this.matchStrength = matchStrength;
			this.matchConfidence = matchConfidence;
			this.justifications = justifications;
			this.looseContra = looseContra;
			this.looseEntail = looseEntail;
			this.gnliGraph = gnliGraph;
			this.tHasComplexCtxs = tHasComplexCtxs;
			this.hHasComplexCtxs = hHasComplexCtxs;
			this.contraFlag = contraFlag;
			this.tVeridical = tVeridical;
			this.tAntiveridical = tAntiveridical;
			this.tAveridical = tAveridical;
			this.hVeridical = hVeridical;
			this.hAntiveridical = hAntiveridical;
			this.hAveridical = hAveridical;
			this.equalsRel = equalsRel;
			this.superRel = superRel;
			this.subRel = subRel;
			this.disjointRel = disjointRel;
			this.ruleUsed = ruleUsed;
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
			
		public boolean tHasComplexCtxs(){
			return tHasComplexCtxs;
		}
		
		public boolean hHasComplexCtxs(){
			return hHasComplexCtxs;
		}
		
		public boolean hasContraFlag() {
			return contraFlag;
		}
		
		public boolean tHasAverCtx() {
			return tAveridical;
		}
		
		public boolean tHasVerCtx() {
			return tVeridical;
		}
		
		public boolean tHasAntiVerCtx() {
			return tAntiveridical;
		}
		
		public boolean hHasAverCtx() {
			return hAveridical;
		}
		
		public boolean hHasVerCtx() {
			return hVeridical;
		}
		
		public boolean hHasAntiVerCtx() {
			return hAntiveridical;
		}
		
		public boolean hasEqualsRel() {
			return equalsRel;
		}
		
		public boolean hasSuperRel() {
			return superRel;
		}
		
		public boolean hasSubRel() {
			return subRel;
		}
		
		public boolean hasDisjointRel() {
			return disjointRel;
		}
		
		public Integer getRuleUsed() {
			return ruleUsed;
		}
}
	
