package sem.mapper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


// uncomment to use through Gretty plugin
//@WebServlet(name = "GKRServlet", urlPatterns = {"gkr"}, loadOnStartup = 1) 
public class GKRServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2259876163739962321L;
	private DepGraphToSemanticGraph semConverter;
	private sem.graph.SemanticGraph graph;
	private HashMap<String,String> examples;
	
	public GKRServlet(){
		super();
		this.semConverter = new DepGraphToSemanticGraph();
		this.examples = new HashMap<String,String>();
		examples.put("-1", "The boy faked the illness.");
		examples.put("-2", "Negotiations prevented the strike.");
		examples.put("-3", "The dog is not eating the food.");
		examples.put("-4", "John or Mary won the competition.");
		examples.put("-5", " No woman is walking.");
		examples.put("-6", "Max forgot to close the door.");
		examples.put("-7", "John might apply for the position.");

	}
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.getWriter().print("");     
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    	// if one of the examples was selected (recognized at the presense of an id), get the xml from the file
        if ( request.getParameter("id") != null){
        	String id = request.getParameter("id");
        	if (Integer.parseInt(id) < 0){
        		request.setAttribute("sent", examples.get(id));
        		ArrayList<String> xmls =  getXmlsAsList(id);
        	    request.setAttribute("roleGraph", xmls.get(0));
        	    request.setAttribute("depsGraph", xmls.get(1));
        	    request.setAttribute("ctxGraph", xmls.get(2));
        	    request.setAttribute("propsGraph", xmls.get(3));
        	    request.setAttribute("lexGraph", xmls.get(4));
        	    request.setAttribute("corefGraph", xmls.get(5));
        		request.getRequestDispatcher("response.jsp").forward(request, response); 
        		return;
        	}
        }
        if(!request.getParameter("sentence").matches("(\\w*(\\s|,|\\.|\\?|!|\")*)*")){
			request.setAttribute("error", "Please enter only letters, numbers, and spaces.");
			request.getRequestDispatcher("response.jsp").forward(request,response);
			return;
		}
        String sentence = request.getParameter("sentence");
        if (sentence.equals("")) {
        	 request.getRequestDispatcher("index.html").forward(request, response); 
        	 return;
        }
        if (!sentence.endsWith("."))
        	sentence = sentence.concat(".");
        this.graph = semConverter.sentenceToGraph(sentence, sentence);
        if (this.graph == null) this.graph = new sem.graph.SemanticGraph();
        String roleGraph = graph.getRoleGraph().getMxGraph();
        String depsGraph = graph.getDependencyGraph().getMxGraph();
        String ctxGraph = graph.getContextGraph().getMxGraph();
        String lexGraph = graph.getLexGraph().getMxGraph();
        String propsGraph = graph.getPropertyGraph().getMxGraph();
        String corefGraph = graph.getLinkGraph().getMxGraph();
        //System.out.println(mx);
        //createImages(counter);
        try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        request.setAttribute("sent", sentence);
        request.setAttribute("roleGraph", roleGraph);
        request.setAttribute("depsGraph", depsGraph);
        request.setAttribute("ctxGraph", ctxGraph);
        request.setAttribute("lexGraph", lexGraph);
        request.setAttribute("propsGraph", propsGraph);
        request.setAttribute("corefGraph", corefGraph);
        request.getRequestDispatcher("response.jsp").forward(request, response); 
    }
    
    
    protected ArrayList<String> getXmlsAsList(String id) {
    	BufferedReader br;
    	ArrayList<String> xmls = new ArrayList<String>();
    	String path = "/home/kkalouli/Documents/Programs/apache-tomcat-9.0.20/webapps/sem.mapper/";
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(path+"examples/"+id+".txt"), "UTF-8"));	
	    	String strLine;
	    	String toAdd = "";
	    	while ((strLine = br.readLine()) != null) {
				if (strLine.startsWith("</mxGraphModel>")){
					toAdd += strLine;
					xmls.add(toAdd);
					toAdd = "";
				} else if (strLine.startsWith("\n")){
					continue;
				} else {
					toAdd += strLine;
				}
	    	}
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
		return xmls;
    }
    
    /*protected void createImages(Integer counter){
    	ServletContext context = getServletContext();
    	String fullPath = context.getRealPath("src/main/webapp/images/");
    	String typo = "/Library/Tomcat/webapps/sem.mapper/";
    	String kate_apache = "/Users/kkalouli/Documents/Programs/apache-tomcat-9.0.19/webapps/sem.mapper/";
    	String kate_appRun = "src/main/webapp/";
    	String gpu_appRun = "/home/kkalouli/Documents/project/semantic_processing/sem.mapper/src/main/webapp/";
    	String gpu_apache = "/home/kkalouli/Documents/Programs/apache-tomcat-9.0.20/webapps/sem.mapper/";
    	try {
			ImageIO.write(graph.saveRolesAsImage(),"png", new File(kate_appRun+"images/roles_"+counter+".png")); 
			ImageIO.write(graph.saveDepsAsImage(),"png", new File(kate_appRun+"images/deps_"+counter+".png"));
		    ImageIO.write(graph.saveContextsAsImage(),"png", new File(kate_appRun+"images/ctxs_"+counter+".png"));
		    ImageIO.write(graph.savePropertiesAsImage(),"png", new File(kate_appRun+"images/props_"+counter+".png"));
		    ImageIO.write(graph.saveLexAsImage(),"png", new File(kate_appRun+"images/lex_"+counter+".png"));
		    ImageIO.write(graph.saveCorefAsImage(),"png", new File(kate_appRun+"images/coref_"+counter+".png"));
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }*/
    
    
}