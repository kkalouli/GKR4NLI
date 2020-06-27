package gnli;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;

import gnli.InferenceChecker.EntailmentRelation;
import sem.graph.SemGraph;
import sem.mapper.GKRServlet.getMxGraphConcurrentTask;


// uncomment to use through Gretty plugin
//@WebServlet(name = "XplaiNLIServlet", urlPatterns = {"xplainli"}, loadOnStartup = 1) 
public class XplaiNLIServlet extends HttpServlet {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -2259876163739962321L;
		private InferenceComputer inferenceComputer;
		private InferenceDecision inferenceDecision;
		private HashMap<String,String> examples;
		private MongoClient mongoClient;
		private MongoDatabase database;
		private String dlDecision;
		private String hyDecision;
		private HashMap<String,Integer> featuresIds;
		private LinkedHashMap<String,Boolean> featuresValues;
		private LinkedHashMap<String,String> featuresNames;
		private boolean HNegation;
		private boolean PNegation;
		private boolean lexOverlap;
		private boolean lengMatch;
		private boolean wordHeurE;
		private boolean wordHeurC;
		private boolean wordHeurN;
		private String premise;
		private String hypothesis;
		private HashMap<Integer,ArrayList<String>> rulesMap;
		private String[] featsOfHybrid;
		private String finalJson;
		private ArrayList<String> heurWordsEntail;
		private ArrayList<String> heurWordsContra;
		private ArrayList<String> heurWordsNeu;


		
		public XplaiNLIServlet(){
			super();
			this.mongoClient = new MongoClient();
			this.database = mongoClient.getDatabase("judgments");
			this.dlDecision = "";
			this.hyDecision = "";
			this.HNegation = false;
			this.PNegation = false;
			this.lexOverlap = false;
			this.lengMatch = false;
			this.wordHeurE = false;
			this.wordHeurC = false;
			this.wordHeurN = false;
			this.hypothesis = "";
			this.premise = "";
			this.finalJson = "";
			this.heurWordsEntail = new ArrayList<String>();
			this.heurWordsContra = new ArrayList<String>();
			this.heurWordsNeu = new ArrayList<String>();
			try {
				this.inferenceComputer = new InferenceComputer();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.examples = new HashMap<String,String>();
			examples.put("-1", "The dog is walking.;;The animal is walking.");
			examples.put("-2", "The judge advised the doctor.;;The doctor advised the judge.");
			examples.put("-3", "John forgot to close the window.;;John closed the window.");
			examples.put("-4", "No woman is walking.;;A woman is walking.");
			examples.put("-5", "Mary believes that John is handsome.;;John is handsome.");

			this.featuresIds = new HashMap<String,Integer>();
			featuresIds.put("Pver", 1);
			featuresIds.put("Hver", 2);
			featuresIds.put("Pantiver", 3);
			featuresIds.put("Hantiver", 4);
			featuresIds.put("Paver", 5);
			featuresIds.put("Haver", 6);
			featuresIds.put("eq", 7);
			featuresIds.put("super", 8);
			featuresIds.put("sub", 9);
			featuresIds.put("dis", 10);
			featuresIds.put("contraFlag", 11);
			featuresIds.put("Pneg", 12);
			featuresIds.put("Hneg", 13);
			featuresIds.put("lexOver", 14);
			featuresIds.put("lenMis", 15);
			featuresIds.put("wordHeurE", 16);
			featuresIds.put("wordHeurC", 17);
			featuresIds.put("wordHeurN", 18);
			
			this.featuresNames = new LinkedHashMap<String,String>();
			featuresNames.put("Pver", "VERIDICAL Context");
			featuresNames.put("Hver", "VERIDICAL Context");
			featuresNames.put("Pantiver", "ANTIVERIDICAL Context");
			featuresNames.put("Hantiver", "ANTIVERIDICAL Context");
			featuresNames.put("Paver", "AVERIDICAL Context");
			featuresNames.put("Haver", "AVERIDICAL Context");
			featuresNames.put("eq", "EQUALS Match");
			featuresNames.put("super", "SUPERCLASS Match");
			featuresNames.put("sub", "SUBCLASS Match");
			featuresNames.put("dis", "DISJOINT Match");
			featuresNames.put("contraFlag", "CONTRADICTION Flag");
			featuresNames.put("Pneg", "Negation");
			featuresNames.put("Hneg", "Negation");
			featuresNames.put("lexOver", "Lexical Overlap");
			featuresNames.put("lenMis", "Length Mismatch");
			featuresNames.put("wordHeurE", "Word Heuristics Entailment");
			featuresNames.put("wordHeurC", "Word Heuristics Contradiction");
			featuresNames.put("wordHeurN", "Word Heuristics Neutral");
			
			this.featuresValues = new LinkedHashMap<String,Boolean>();
			
			rulesMap = new HashMap<Integer,ArrayList<String>>();
			// contradiction implementation with different contexts 1
			rulesMap.put(1, new ArrayList<String>(Arrays.asList("Hantiver", "Pver", "dis")));
			rulesMap.put(2, new ArrayList<String>(Arrays.asList("Hantiver", "Pver", "contraFlag")));
			rulesMap.put(3, new ArrayList<String>(Arrays.asList("Hantiver", "Pver", "eq")));
			rulesMap.put(4, new ArrayList<String>(Arrays.asList("Hantiver", "Pver", "sub")));
			// contradiction implementation with different contexts 2
			rulesMap.put(5, new ArrayList<String>(Arrays.asList("Hver", "Pantiver", "dis")));
			rulesMap.put(6, new ArrayList<String>(Arrays.asList("Hver", "Pantiver", "contraFlag")));
			rulesMap.put(7, new ArrayList<String>(Arrays.asList("Hver", "Pantiver", "eq")));
			rulesMap.put(8, new ArrayList<String>(Arrays.asList("Hver", "Pantiver", "super")));
			// entail or disjoint implementation with veridical contexts 
			rulesMap.put(9, new ArrayList<String>(Arrays.asList("Hver", "Pver", "eq", "dis", "contraFlag")));
			rulesMap.put(10, new ArrayList<String>(Arrays.asList("Hver", "Pver", "sub", "dis", "contraFlag")));
			rulesMap.put(11, new ArrayList<String>(Arrays.asList("Hver", "Pver", "dis", "contraFlag")));
			rulesMap.put(12, new ArrayList<String>(Arrays.asList("Hver", "Pver", "dis", "contraFlag")));
			rulesMap.put(13, new ArrayList<String>(Arrays.asList("Hver", "Pver", "super")));
			// none relation is not being captured right now
			//rulesMap.put(14, new ArrayList<String>(Arrays.asList("Hver", "Pver", "none")));
			rulesMap.put(15, new ArrayList<String>(Arrays.asList("Hver", "Pver", "eq", "contraFlag")));
			rulesMap.put(16, new ArrayList<String>(Arrays.asList("Hver", "Pver", "sub", "contraFlag")));
			// entail or disjoint implementation with antiveridical contexts 
			rulesMap.put(17, new ArrayList<String>(Arrays.asList("Hantiver", "Pantiver", "eq", "dis", "contraFlag")));
			rulesMap.put(18, new ArrayList<String>(Arrays.asList("Hantiver", "Pantiver", "super", "dis", "contraFlag")));
			rulesMap.put(19, new ArrayList<String>(Arrays.asList("Hantiver", "Pantiver", "dis", "contraFlag")));
			rulesMap.put(20, new ArrayList<String>(Arrays.asList("Hantiver", "Pantiver", "dis", "contraFlag")));
			rulesMap.put(21, new ArrayList<String>(Arrays.asList("Hantiver", "Pantiver", "sub")));
			// none relation is not being captured right now
			//rulesMap.put(22, new ArrayList<String>(Arrays.asList("Hantiver", "Pantiver", "none")));
			rulesMap.put(23, new ArrayList<String>(Arrays.asList("Hantiver", "Pantiver", "eq", "contraFlag")));
			rulesMap.put(24, new ArrayList<String>(Arrays.asList("Hantiver", "Pantiver", "super", "contraFlag")));
			// all neutral ones - all remaining
			rulesMap.put(25, new ArrayList<String>(Arrays.asList()));
			
			
		
		}
	    protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
	    	 String judgment = request.getParameter("judge");
		     System.out.println(judgment);
		     if (judgment != null){
		        	if (judgment.equals("correct")){
		        		registerInDB(this.database, "correct", inferenceDecision, dlDecision, hyDecision);
		        	} 
		        	else if (judgment.equals("bert_correct")){
		        		registerInDB(this.database, "bert_correct",  inferenceDecision, dlDecision, hyDecision);
		        	} 
		        	else if (judgment.equals("sym_correct")){
		        		registerInDB(this.database, "sym_correct",  inferenceDecision, dlDecision, hyDecision);
		        	} 
		        	else if (judgment.equals("none_correct")){
		        		registerInDB(this.database, "none_correct",  inferenceDecision, dlDecision, hyDecision);
		        	}
		        }

	    }

	    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
	 
	    	// if one of the examples was selected (recognized at the presense of an id), get the xml from the file
	        if ( request.getParameter("id") != null){
	        	String id = request.getParameter("id");
	        	if (Integer.parseInt(id) < 0){
	        		request.setAttribute("pair", examples.get(id));
	    	        request.setAttribute("premise", examples.get(id).split(";;")[0]);
	    	        request.setAttribute("hypothesis", examples.get(id).split(";;")[1]);
	        		finalJson =  getStoredJson(id);
	    	        request.setAttribute("jsonFinal", finalJson);
	    	        request.getRequestDispatcher("index.jsp").forward(request, response);
	        		return;
	        	}
	        }
	        premise = request.getParameter("premise");
	        hypothesis = request.getParameter("hypothesis");
	        //System.out.println("test1");
	        if (premise.equals("") || hypothesis.equals("")) {
	        	System.out.println("found nothing");
	        	request.getRequestDispatcher("index.jsp").forward(request, response); 
	        	return;
	        }
	        if(!request.getParameter("premise").matches("(\\w*(\\s|,|\\.|\\?|!|\"|-|')*)*") || !request.getParameter("hypothesis").matches("(\\w*(\\s|,|\\.|\\?|!|\"|-|')*)*")){
				request.setAttribute("error", "Please enter only letters, numbers, and spaces.");
				request.getRequestDispatcher("index.jsp").forward(request,response);
				return;
			}
	        if (!premise.endsWith(".") && !premise.endsWith("?") && !premise.endsWith("!")){
	        	premise = premise+".";
	        }
	        if (!hypothesis.endsWith(".") && !hypothesis.endsWith("?") && !hypothesis.endsWith("!")){
	        	hypothesis = hypothesis+".";
	        }
	        
	        //Timestamp timestamp1 = new Timestamp(System.currentTimeMillis());
	       //System.out.println(timestamp1);
	    	ExecutorService es = Executors.newFixedThreadPool(1);
		    Future<String> dlDec = es.submit(new getDlDecisionConcurrentTask(premise,hypothesis));
	        this.inferenceDecision = inferenceComputer.computeInferenceOfPair(premise, hypothesis, "N");
	        //System.out.println(inferenceDecision.getEntailmentRelation());
	        //this.dlDecision = getDLOutput(premise, hypothesis);
	        try {
				this.dlDecision = dlDec.get();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ExecutionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        //System.out.println(dlDecision);
	        this.hyDecision = getHybridOutput(dlDecision, inferenceDecision);
	        //Timestamp timestamp2 = new Timestamp(System.currentTimeMillis());
	        //System.out.println(timestamp2);
	        //System.out.println(hyDecision);
	        if (this.inferenceDecision == null) this.inferenceDecision = new InferenceDecision(EntailmentRelation.UNKNOWN, 0.0, 0.0, 0, false, false, false, false, false, false, false, false,
	        		false, false, false, false, false, EntailmentRelation.UNKNOWN, null, false, false, null);

	        try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	              
	        this.heurWordsEntail.clear();
	        this.heurWordsContra.clear();
	        this.heurWordsNeu.clear();
	        if (!request.getParameter("premiseEntailment").equals("")) {
	        	String[] words = request.getParameter("premiseEntailment").split(";");
	        	for (String w: words) {
	        		this.heurWordsEntail.add(w);
	        	}
	        }
	        if (!request.getParameter("premiseContradiction").equals("")) {
	        	String[] words = request.getParameter("premiseContradiction").split(";");
	        	for (String w: words) {
	        		this.heurWordsContra.add(w);
	        	}	        	
	        } 
	        if (!request.getParameter("premiseNeutral").equals("")) {
	        	String[] words = request.getParameter("premiseNeutral").split(";");
	        	for (String w: words) {
	        		this.heurWordsNeu.add(w);
	        	}
	        } 
	        
	        if (!request.getParameter("hypothesisEntailment").equals("")) {
	        	String[] words = request.getParameter("hypothesisEntailment").split(";");
	        	for (String w: words) {
	        		this.heurWordsEntail.add(w);
	        	}
	        }
	        if (!request.getParameter("hypothesisContradiction").equals("")) {
	        	String[] words = request.getParameter("hypothesisContradiction").split(";");
	        	for (String w: words) {
	        		this.heurWordsContra.add(w);
	        	}
	        } 
	        if (!request.getParameter("hypothesisNeutral").equals("")) {
	        	String[] words = request.getParameter("hypothesisNeutral").split(";");
	        	for (String w: words) {
	        		this.heurWordsNeu.add(w);
	        	}
	        } 
	        
			this.HNegation = false;
			this.PNegation = false;
			this.lexOverlap = false;
			this.lengMatch = false;
			this.wordHeurE = false;
			this.wordHeurC = false;
			this.wordHeurN = false;
	        checkForBertFeatures();
	        featuresValues.clear();
	        fillMapOfFeaturesValues();
	        createJSONObject();
	        request.setAttribute("premise", premise);
	        request.setAttribute("hypothesis", hypothesis);
	        /*request.setAttribute("relation", inferenceDecision.getEntailmentRelation());
	        request.setAttribute("hVeridical", inferenceDecision.hHasVerCtx());
	        request.setAttribute("hAveridical", inferenceDecision.hHasAverCtx());
	        request.setAttribute("hAntiveridical", inferenceDecision.hHasAntiVerCtx());
	        request.setAttribute("tVeridical", inferenceDecision.tHasVerCtx());
	        request.setAttribute("tAveridical", inferenceDecision.tHasAverCtx());
	        request.setAttribute("tAntiveridical", inferenceDecision.tHasAntiVerCtx());
	        request.setAttribute("equalsRel", inferenceDecision.hasEqualsRel());
	        request.setAttribute("superRel", inferenceDecision.hasSuperRel());
	        request.setAttribute("subRel", inferenceDecision.hasSubRel());
	        request.setAttribute("disjointRel", inferenceDecision.hasDisjointRel());
	        request.setAttribute("hComplexCtxs", inferenceDecision.hHasComplexCtxs());
	        request.setAttribute("tComplexCtxs", inferenceDecision.tHasComplexCtxs());
	        request.setAttribute("contraFlag", inferenceDecision.hasContraFlag());
	        request.setAttribute("dlDecision", dlDecision);
	        request.setAttribute("hyDecision", hyDecision);	 */ 
	        request.setAttribute("jsonFinal", finalJson);
	        request.getRequestDispatcher("index.jsp").forward(request, response); 
	    }
	    
	    
	    protected String getStoredJson(String id) {
	    	BufferedReader br;
	    	String toAdd = "";
	    	String path = "/home/kkalouli/Documents/Programs/tomcat2/webapps/gnli/";
	    	//String path = "/home/kkalouli/Documents/project/semantic_processing/gnli/src/main/webapp/";
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(path+"examples/"+id+".json"), "UTF-8"));	
		    	String strLine;	    	
		    	while ((strLine = br.readLine()) != null) {
						toAdd += strLine;
		    	}
		    	br.close();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println(toAdd);
			return toAdd;
	    }
	    

	    
	    public class getDlDecisionConcurrentTask implements Callable<String> {
			 private String dlDecision;
		 
		    public getDlDecisionConcurrentTask(String premise, String hypothesis) {
		    	this.dlDecision = getDLOutput(premise, hypothesis);
		    	//System.out.println(dlDecision);
		    }
		 
		    public String call() {
		    	return dlDecision;
		    }
		}
	    
	    protected String getDLOutput(String premise, String hypothesis){
	    	String dlDecision = "";
	    	String s = null;
	    	//System.out.println(premise);
	    	//System.out.println(hypothesis);
	    	try {
	                Process p = Runtime.getRuntime().exec(new String[]{"/home/kkalouli/Documents/virtEnv1/bin/python", "/home/kkalouli/Documents/project/semantic_processing/gnli/src/main/webapp/get_xlnet_inference_decision.py", premise, hypothesis});
	                //System.out.println(p);
	                BufferedReader stdInput = new BufferedReader(new 
	                     InputStreamReader(p.getInputStream()));
	                //System.out.println(stdInput.toString());
	                BufferedReader stdError = new BufferedReader(new 
	                     InputStreamReader(p.getErrorStream()));

	                // read the output from the command
	                //System.out.println("Here is the standard output of the command:\n");
	                while ((s = stdInput.readLine()) != null) {
	                	//System.out.println("ok");
	                	dlDecision = s;
	                	//System.out.println(s);
	                }
	                
	                // read any errors from the attempted command
	                /*System.out.println("Here is the standard error of the command (if any):\n");
	                while ((s = stdError.readLine()) != null) {
	                    //System.out.println(s);
	                }*/
	                
	            }
	            catch (IOException e) {
	                System.out.println("exception happened - here's what I know: ");
	                e.printStackTrace();
	                System.exit(-1);
	            }
	    	
	    	if (dlDecision.equals("0")) {
	    		dlDecision = "ENTAILMENT";
	    	}
	    	else if (dlDecision.equals("1")) {
	    		dlDecision = "CONTRADICTION";
	    	}
	    	else if (dlDecision.equals("2")) {
	    		dlDecision = "NEUTRAL";
	    	}
	    	
	    	return dlDecision;
	    }
	    
	    protected String getHybridOutput(String bertLabel, InferenceDecision inferenceDecision ){
	    	String hyDecision = "";
	    	String output = "";
	    	String s = null;
	    	String ver = "0";
	    	String aver = "0";
	    	String antiver = "0";
	    	String complexCtxs = "0";
	    	String contraCost = "0";
	    	String eq = "0";
	    	String sub = "0";
	    	String superclass = "0";
	    	String dis = "0";
	    	
	        String ruleLabel = inferenceDecision.getEntailmentRelation().toString();
	        //System.out.println(ruleLabel);
	        if (inferenceDecision.hHasVerCtx() || inferenceDecision.tHasVerCtx())
	        	ver = "1";
	        if (inferenceDecision.hHasAverCtx() || inferenceDecision.tHasAverCtx())
	        	aver = "1";
	        if (inferenceDecision.hHasAntiVerCtx() || inferenceDecision.tHasAntiVerCtx())
	        	antiver = "1";
	        if ( inferenceDecision.hHasComplexCtxs() || inferenceDecision.tHasComplexCtxs())
	        	complexCtxs = "1";
	        if (inferenceDecision.hasContraFlag())
	        	contraCost = "1";
	        if (inferenceDecision.hasSuperRel())
	        	superclass = "1";
	        if (inferenceDecision.hasSubRel())
	        	sub = "1";
	        if (inferenceDecision.hasEqualsRel())
	        	eq = "1";
	        if (inferenceDecision.hasDisjointRel())
	        	dis = "1";
	        
	        String features = "\\["+complexCtxs+","+contraCost+","+ver+","+antiver+","+aver+","+eq+","+superclass+","+sub+","+dis+"\\]";
	        //System.out.println(features);   	
	        
	    	try {
	                Process p = Runtime.getRuntime().exec(new String[]{"/home/kkalouli/Documents/virtEnv1/bin/python", "/home/kkalouli/Documents/project/semantic_processing/gnli/src/main/webapp/read_hybrid_model_and_classify_sample.py", features, bertLabel, ruleLabel});
	                
	                BufferedReader stdInput = new BufferedReader(new 
	                     InputStreamReader(p.getInputStream()));

	                BufferedReader stdError = new BufferedReader(new 
	                     InputStreamReader(p.getErrorStream()));

	                // read the output from the command
	                //System.out.println("Here is the standard output of the command:\n");
	                while ((s = stdInput.readLine()) != null) {
	                	output = s;
	                	//System.out.println(s);
	                }
	                
	                // read any errors from the attempted command
	                /*System.out.println("Here is the standard error of the command (if any):\n");
	                while ((s = stdError.readLine()) != null) {
	                    System.out.println(s);
	                }*/
	                
	            }
	            catch (IOException e) {
	                System.out.println("exception happened - here's what I know: ");
	                e.printStackTrace();
	                System.exit(-1);
	            }
	    		//System.out.println(output);
	    		if (!output.equals("")) {
	    			String[] listOfFeats = output.replace("[","").replace("]","").replace("'","").split(",");
	    			hyDecision = listOfFeats[listOfFeats.length-1].replace(" ", "");
	    			listOfFeats = (String[]) ArrayUtils.remove(listOfFeats,listOfFeats.length-1);
	    			listOfFeats = (String[]) ArrayUtils.remove(listOfFeats,3);
	    			featsOfHybrid = listOfFeats;
	    		}
	    	
	    	return hyDecision;
	    }
	    
	    protected void registerInDB(MongoDatabase database, String type, InferenceDecision ruleDec, String dlDec, String hyDec) {	
	    	MongoCollection<Document> collection = database.getCollection(type);
	    	//Gson gson = new Gson();
	    	//BasicDBObject objRuleDec = BasicDBObject.parse(gson.toJson(ruleDec));
	    	Document pair = new Document("ruleDec", "")
                    .append("dlDec", dlDec)
                    .append("hyDec", hyDec);
	    	collection.insertOne(pair);
	    	
	    }
	    
	    protected void fillMapOfFeaturesValues(){
			featuresValues.put("Pver", inferenceDecision.tHasVerCtx());
			featuresValues.put("Hver", inferenceDecision.hHasVerCtx());
			featuresValues.put("Pantiver", inferenceDecision.tHasAntiVerCtx());
			featuresValues.put("Hantiver", inferenceDecision.hHasAntiVerCtx());
			featuresValues.put("Paver", inferenceDecision.tHasAverCtx());
			featuresValues.put("Haver", inferenceDecision.hHasAverCtx());
			featuresValues.put("eq",  inferenceDecision.hasEqualsRel());
			featuresValues.put("super", inferenceDecision.hasSuperRel());
			featuresValues.put("sub", inferenceDecision.hasSubRel());
			featuresValues.put("dis", inferenceDecision.hasDisjointRel());
			featuresValues.put("contraFlag", inferenceDecision.hasContraFlag());
			featuresValues.put("Pneg", PNegation);
			featuresValues.put("Hneg", HNegation);
			featuresValues.put("lexOver", lexOverlap);
			featuresValues.put("lenMis", lengMatch);
			featuresValues.put("wordHeurE", wordHeurE);		
			featuresValues.put("wordHeurC", wordHeurC);
			featuresValues.put("wordHeurN", wordHeurN);
	    }
	    
	    
	    protected void checkForBertFeatures(){
	    	// negation words
	    	ArrayList<String> negationWords = new ArrayList<String>();
	    	negationWords.add("not ");
	    	negationWords.add("no ");
	    	negationWords.add("never ");
	    	negationWords.add("nobody ");
	    	negationWords.add("nothing ");
	    	negationWords.add("Not ");
	    	negationWords.add("No ");
	    	negationWords.add("Never ");
	    	negationWords.add("Nobody ");
	    	negationWords.add("Nothing ");
	    	negationWords.add("n't ");
	    	//System.out.println(negationWords);
	    	
	    	for (String word : negationWords) {
	    		if (premise.contains(word)) {
	    			PNegation = true;
	    		}
	    		if (hypothesis.contains(word)) {
	    			HNegation = true;
	    			//System.out.println("found");
	    		}
	    	}
	    	
	    	// heuristics words
	    	// ENTAIL
	    	heurWordsEntail.add("outdoors");
	    	heurWordsEntail.add("instrument");
	    	heurWordsEntail.add("outside");
	    	heurWordsEntail.add("animal");
	    	heurWordsEntail.add("some ");
	    	heurWordsEntail.add("Some ");
	    	heurWordsEntail.add("something");
	    	heurWordsEntail.add("Something");
	    	heurWordsEntail.add("sometimes");
	    	heurWordsEntail.add("Sometimes");
	    	heurWordsEntail.add("various");
	    	
	    	for (String word : heurWordsEntail) {
	    		if (hypothesis.contains(word)) {
	    			wordHeurE = true;
	    		}
	    	}
	    	
	    	// CONTRA
	    	heurWordsContra.add("sleeping");
	    	heurWordsContra.add("tv");
	    	heurWordsContra.add("cat");
	    	heurWordsContra.add("any");
	    	
	    	for (String word : heurWordsContra) {
	    		if (hypothesis.contains(word)) {
	    			wordHeurC = true;
	    		}
	    		if (premise.contains(word)) {
	    			wordHeurC = true;
	    		}
	    	}
	    	
	    	// NEUTRAL
	    	heurWordsNeu.add("tall");
	    	heurWordsNeu.add("first");
	    	heurWordsNeu.add("competition");
	    	heurWordsNeu.add("sad");
	    	heurWordsNeu.add("favorite");
	    	heurWordsNeu.add("also");
	    	heurWordsNeu.add("because");
	    	heurWordsNeu.add("popular");
	    	heurWordsNeu.add("many");
	    	heurWordsNeu.add("most");
	    	
	    	for (String word : heurWordsNeu) {
	    		if (hypothesis.contains(word)) {
	    			wordHeurN = true;
	    		}
	    	}
	    	
	    	// lexical Overlap
	    	List<String> premiseList = new ArrayList<String>(Arrays.asList(premise.replace(".", "").split(" ")));
	    	List<String> hypothesisList = new ArrayList<String>(Arrays.asList(hypothesis.replace(".", "").split(" ")));
	    	List<String> difference = new ArrayList<>(premiseList);
	    	difference.removeAll(hypothesisList);
	    	if (premiseList.containsAll(hypothesisList))
	    		lexOverlap = true;
	    	else if (difference.size() == 1)
	    		lexOverlap = true;
	    	
	    	// length mismatch
	    	if (hypothesisList.size() > premiseList.size() + 3)
	    		lengMatch = true;
	    	
	    	
	    }
	    
	    
	   
	    @SuppressWarnings("unchecked")
		protected void createJSONObject() {
	    	JSONObject jsonFinal = new JSONObject();

	    	// decisions array
	    	JSONArray decisionsArray = new JSONArray();
	    	JSONObject itemRule = new JSONObject();
	    	itemRule.put("rule", inferenceDecision.getEntailmentRelation().toString());
	    	JSONObject itemDl = new JSONObject();
	    	itemDl.put("bert", dlDecision);
	    	JSONObject itemHybrid = new JSONObject();
	    	itemHybrid.put("hybrid", hyDecision);
	    	decisionsArray.add(itemRule);
	    	decisionsArray.add(itemDl);
	    	decisionsArray.add(itemHybrid);	    	
	    	jsonFinal.put("decisions", decisionsArray);
	    	
	    	//"features" = [  {"name":"feayure_name1", "attirbutes"=[ {"id":1, value: true}, {"id":2, value:false} ] }, {}  ],    	
	    	// features array
	    	JSONArray featuresArray = new JSONArray();
	    	for (String key : featuresValues.keySet()) {
	    		if (key.startsWith("H")) 
	    			continue;
	    		JSONObject itemFeat = new JSONObject();
	    		itemFeat.put("name", featuresNames.get(key));
	    		JSONArray attrArray = new JSONArray();
	    		JSONObject feat = new JSONObject();
	    		feat.put("id", featuresIds.get(key));
	    		feat.put("value", featuresValues.get(key));
	    		attrArray.add(feat);
	    		if (key.startsWith("P")) {
		    		JSONObject feat2 = new JSONObject();
		    		feat2.put("id", featuresIds.get(key.replace("P", "H")));
		    		feat2.put("value", featuresValues.get(key.replace("P", "H")));
		    		attrArray.add(feat2);
	    		}
	    		itemFeat.put("attributes", attrArray);
	    		featuresArray.add(itemFeat);
	    	}	    	
	    	jsonFinal.put("features", featuresArray);
	    	
	    	// symbolic rules array
	    	JSONArray rulesSymbolicArray = new JSONArray();
	    	Integer ruleUsed = this.inferenceDecision.getRuleUsed();
	    	//System.out.println(ruleUsed);
	    	ArrayList<String> rules = new ArrayList<String>();
	    	// to account for cases where there is no specific rule applying, resulting to neutral
	    	if (ruleUsed == 25) {
	    		for (String key : featuresValues.keySet()) {
	    			if ( !key.equals("lexOver") & ( key.contains("ver") ||
	    					key.equals("eq")|| key.equals("super")
	    					|| key.equals("sub") || key.equals("dis") ) ) {
	    				boolean value = featuresValues.get(key);
	    				//System.out.println(value);
	    				if (value == true)
	    					rules.add(key);
	    			}
	    		}
	    	} else {
	    		rules = rulesMap.get(ruleUsed);
	    	}
	    	//System.out.println(rules);
	    	for (String r : rules) {
	    		Integer id = featuresIds.get(r);
	    		rulesSymbolicArray.add(id);
	    	}
	    	jsonFinal.put("rulesSymbolic", rulesSymbolicArray);
	    	
	    	
	    	// hzbrid rules array
	    	JSONArray rulesHybridArray = new JSONArray();
	    	for (String feat : featsOfHybrid) {
	    		if (feat.replace(" ","").equals("VER")) {
	    			rulesHybridArray.add(1);
	    			rulesHybridArray.add(2);
	    		} else if (feat.replace(" ","").equals("ANTIVER")) {
	    			rulesHybridArray.add(3);
	    			rulesHybridArray.add(4);
	    		} else if (feat.replace(" ","").equals("AVER")) {
	    			rulesHybridArray.add(5);
	    			rulesHybridArray.add(6);
	    		} else {
	    			rulesHybridArray.add(featuresIds.get(feat.replace(" ","")));
	    		}
	    	}
	    	jsonFinal.put("rulesHybrid", rulesHybridArray);
	    	
	    	// dl rules array
	    	JSONArray rulesDLArray = new JSONArray();
	    	if (wordHeurE == true) {
		    	Integer id = featuresIds.get("wordHeurE");
		    	JSONObject relationObj = new JSONObject();
		    	relationObj.put("id",id);
		    	if (dlDecision.equals("ENTAILMENT"))
		    		relationObj.put("value",true);
		    	else
		    		relationObj.put("value",false);
		    	rulesDLArray.add(relationObj);
		    }
		    if (lexOverlap == true) {
		    	Integer id = featuresIds.get("lexOver");
		    	JSONObject relationObj = new JSONObject();
		    	relationObj.put("id",id);
		    	if (dlDecision.equals("ENTAILMENT"))
		    		relationObj.put("value",true);
		    	else
		    		relationObj.put("value",false);
		    	rulesDLArray.add(relationObj);
		    }
	    	if (wordHeurC == true) {
	    		Integer id = featuresIds.get("wordHeurC");
	    		JSONObject relationObj = new JSONObject();
	    		relationObj.put("id",id);
		    	if (dlDecision.equals("CONTRADICTION"))
		    		relationObj.put("value",true);
		    	else
		    		relationObj.put("value",false);
		    	rulesDLArray.add(relationObj);
	    	}
	    	if (PNegation == true) {
	    		//System.out.println("pnegation");
	    		Integer id = featuresIds.get("Pneg");
	    		JSONObject relationObj = new JSONObject();
	    		relationObj.put("id",id);
		    	if (dlDecision.equals("CONTRADICTION"))
		    		relationObj.put("value",true);
		    	else
		    		relationObj.put("value",false);
		    	rulesDLArray.add(relationObj);
	    	}
	    	if (HNegation == true) {
	    		//System.out.println("hnegation");
	    		Integer id = featuresIds.get("Hneg");
	    		JSONObject relationObj = new JSONObject();
	    		relationObj.put("id",id);
		    	if (dlDecision.equals("CONTRADICTION"))
		    		relationObj.put("value",true);
		    	else
		    		relationObj.put("value",false);
		    	rulesDLArray.add(relationObj);
	    	}

    		if (wordHeurN == true) {
    			Integer id = featuresIds.get("wordHeurN");
    			JSONObject relationObj = new JSONObject();
    			relationObj.put("id",id);
		    	if (dlDecision.equals("NEUTRAL"))
		    		relationObj.put("value",true);
		    	else
		    		relationObj.put("value",false);	
		    	rulesDLArray.add(relationObj);
    		}
	    	if (lengMatch == true) {
	    		Integer id = featuresIds.get("lenMis");
	    		JSONObject relationObj = new JSONObject();
	    		relationObj.put("id",id);
		    	if (dlDecision.equals("NEUTRAL"))
		    		relationObj.put("value",true);
		    	else
		    		relationObj.put("value",false);	
		    	rulesDLArray.add(relationObj);
	    	}

	    	jsonFinal.put("rulesDL", rulesDLArray);
	    	
	    	//array of match between hybrid and bert-rule
	    	if (hyDecision.equals(dlDecision) && hyDecision.equals(inferenceDecision.getEntailmentRelation().toString()))
	    		jsonFinal.put("match", "B");
	    	else if (hyDecision.equals(dlDecision))
	    		jsonFinal.put("match", "DL");
	    	else if (hyDecision.equals(inferenceDecision.getEntailmentRelation().toString()))
	    		jsonFinal.put("match", "R");
	    	
	    	finalJson = jsonFinal.toString();
	    	System.out.println(jsonFinal.toString());

	    }

}
