package gnli;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.Document;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import gnli.InferenceChecker.EntailmentRelation;

//uncomment to use through Gretty plugin
//@WebServlet(name = "DBServlet", urlPatterns = {"dbserv"}, loadOnStartup = 1) 
public class DBServlet extends HttpServlet {
	
	private MongoClient mongoClient;
	private MongoDatabase database;
	
	public DBServlet() {
		this.mongoClient = new MongoClient();
		this.database = mongoClient.getDatabase("judgments");
		
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
	    	response.setContentType("text/html;charset=UTF-8");
	    	PrintWriter out = response.getWriter();
	    	Random r = new Random();
	    	try {
	    		int pval = Integer.parseInt(request.getParameter("pval"));
	    		float randomval = r.nextFloat();
	    		int seedval = (int)(10.0F * randomval);
	    		out.print(pval + seedval);
	    	}
	    	finally {
	    		out.close();
	    	}
	    }

	    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
	     	
	        String judgment = request.getParameter("judge");
	        System.out.println(judgment);
	        //InferenceDecision ruleDec = (InferenceDecision) request.getAttribute("infDec");
	        String dlDec = request.getParameter("dlDecision");
	        System.out.println(dlDec);
	        /*String hyDec = request.getParameter("hyDec");
	        if (judgment != null){
	        	if (judgment.equals("correct")){
	        		registerInDB(this.database, "correct", null, null, null);
	        	} 
	        	else if (judgment.equals("bert_correct")){
	        		registerInDB(this.database, "bert_correct", null, null, null);
	        	} 
	        	else if (judgment.equals("sym_correct")){
	        		registerInDB(this.database, "sym_correct", null, null, null);
	        	} 
	        	else if (judgment.equals("none_correct")){
	        		registerInDB(this.database, "none_correct", null, null, null);
	        	}
	        }

	        request.getRequestDispatcher("responseAfterFdback.jsp").forward(request, response); */
	    }
	    
	    protected void registerInDB(MongoDatabase database, String type, InferenceDecision ruleDec, String dlDec, String hyDec) {	
	    	MongoCollection<Document> collection = database.getCollection(type);
	    	Gson gson = new Gson();
	    	BasicDBObject objRuleDec = BasicDBObject.parse(gson.toJson(ruleDec));
	    	Document pair = new Document("ruleDec", objRuleDec)
                    .append("dlDec", dlDec)
                    .append("hyDec", hyDec);
	    	collection.insertOne(pair);
	    	
	    }


}
