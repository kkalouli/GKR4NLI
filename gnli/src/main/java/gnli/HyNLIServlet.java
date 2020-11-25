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

import gnli.InferenceChecker.EntailmentRelation;
import sem.graph.SemGraph;
import sem.mapper.GKRServlet.getMxGraphConcurrentTask;


/**
 * HttpServlet for running the demo of Hy-NLI. 
 * @author Katerina Kalouli, 2019
 *
 */
// uncomment to use through Gretty plugin
//@WebServlet(name = "HyNLIServlet", urlPatterns = {"hynli"}, loadOnStartup = 1) 
public class HyNLIServlet extends HttpServlet {
		
		private static final long serialVersionUID = -2259876163739962321L;
		private InferenceComputer inferenceComputer;
		private InferenceDecision inferenceDecision;
		private HashMap<String,String> examples;
		private String dlDecision;
		private String hyDecision;
		private String premise;
		private String hypothesis;
		private String finalJson;



		
		public HyNLIServlet(){
			super();
			this.dlDecision = "";
			this.hyDecision = "";
			this.hypothesis = "";
			this.premise = "";
			this.finalJson = "";
			try {
				this.inferenceComputer = new InferenceComputer();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// default examples
			this.examples = new HashMap<String,String>();
			examples.put("-1", "The dog is walking.;;The animal is walking.");
			examples.put("-2", "The judge advised the doctor.;;The doctor advised the judge.");
			examples.put("-3", "John forgot to close the window.;;John closed the window.");
			examples.put("-4", "No woman is walking.;;A woman is walking.");
			examples.put("-5", "Mary believes that John is handsome.;;John is handsome.");
		}
		
		
	    protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
	    }

	    /**
	     * Handle the main request of the demo. 
	     */
	    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
	 
	    	// if one of the examples was selected (recognized at the presense of an id), get the xml from the file
	        if ( request.getParameter("id") != null){
	        	String id = request.getParameter("id");
	        	System.out.println(id);
	        	if (Integer.parseInt(id) < 0){
	        		request.setAttribute("pair", examples.get(id));
	    	        request.setAttribute("premise", examples.get(id).split(";;")[0]);
	    	        request.setAttribute("hypothesis", examples.get(id).split(";;")[1]);
	        		ArrayList<String> labels =  getStoredJson(id);
	    	        request.setAttribute("dl_label", labels.get(1));
	    	        request.setAttribute("sym_label", labels.get(0));
	    	        request.setAttribute("hy_label", labels.get(2));
	    	        request.getRequestDispatcher("response.jsp").forward(request, response);
	        		return;
	        	}
	        }
	        premise = request.getParameter("premise");
	        hypothesis = request.getParameter("hypothesis");
	        //System.out.println("test1");
	        if (premise.equals("") || hypothesis.equals("")) {
	        	System.out.println("found nothing");
	        	request.getRequestDispatcher("hynli.jsp").forward(request, response); 
	        	return;
	        }
	        if(!request.getParameter("premise").matches("(\\w*(\\s|,|\\.|\\?|!|\"|-|')*)*") || !request.getParameter("hypothesis").matches("(\\w*(\\s|,|\\.|\\?|!|\"|-|')*)*")){
				request.setAttribute("error", "Please enter only letters, numbers, and spaces.");
				request.getRequestDispatcher("hynli.jsp").forward(request,response);
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
	        		false, false, false, false, false, null, false, false, null);

	        try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        request.setAttribute("premise", premise);
	        request.setAttribute("hypothesis", hypothesis);
	        request.setAttribute("dl_label", dlDecision);
	        request.setAttribute("sym_label", inferenceDecision.getEntailmentRelation().toString());
	        request.setAttribute("hy_label", hyDecision);
	        request.getRequestDispatcher("response.jsp").forward(request, response); 
	    }
	    
	    /**
	     * Get the json of the default examples so that they do not need to be run every time again. 
	     * @param id
	     * @return
	     */
	    protected ArrayList<String> getStoredJson(String id) {
	    	BufferedReader br;
	    	ArrayList<String> decisions = new ArrayList<String>();
	    	String path = "/home/kkalouli/Documents/Programs/tomcat2/webapps/gnli/";
	    	//String path = "/home/kkalouli/Documents/project/semantic_processing/gnli/src/main/webapp/";
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(path+"examples/"+id+"_hynli.txt"), "UTF-8"));	
		    	String strLine;	    	
		    	while ((strLine = br.readLine()) != null) {
		    		decisions.add(strLine);
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
			return decisions;
	    }
	    

	    /**
	     * Get the decision of the DL component as a concurrentTask for faster response.
	     * @author Katerina Kalouli, 2019
	     *
	     */
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
	    
	    /**
	     * Get the decision of the DL component by running the python script.
	     * @param premise
	     * @param hypothesis
	     * @return
	     */
	    protected String getDLOutput(String premise, String hypothesis){
	    	String dlDecision = "";
	    	String s = null;
	    	//System.out.println(premise);
	    	//System.out.println(hypothesis);
	    	try {
	                Process p = Runtime.getRuntime().exec(new String[]{"/home/kkalouli/Documents/virtEnv1/bin/python", "/home/kkalouli/Documents/project/semantic_processing/gnli/src/main/webapp/get_dl_inference_decision.py", premise, hypothesis});
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
	                    System.out.println(s);
	                }
	                System.out.println(dlDecision);*/
	                
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
	    	
	    	//System.out.println("test");
	    	//System.out.println(dlDecision);
	    	return dlDecision;
	    }
	    
	    /**
	     * Get the decision of the hybrid component by providing the features of the current pair
	     * and running the python script.
	     * @param bertLabel
	     * @param inferenceDecision
	     * @return
	     */
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
	                Process p = Runtime.getRuntime().exec(new String[]{"/home/kkalouli/Documents/virtEnv1/bin/python", "/home/kkalouli/Documents/project/semantic_processing/gnli/src/main/webapp/read_hynli_model_and_classify_sample.py", features, bertLabel, ruleLabel});
	                
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
	    			hyDecision = output;
	    		}
	    	
	    	return hyDecision;
	    }
}
