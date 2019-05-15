package sem.mapper;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@WebServlet(name = "GKRServlet", urlPatterns = {"gkr"}, loadOnStartup = 1) 
public class GKRServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2259876163739962321L;
	private DepGraphToSemanticGraph semConverter;
	private String imagePath;
	private sem.graph.SemanticGraph graph;
	private int counter;
	private HashMap<String,String> examples;
	
	public GKRServlet(){
		super();
		this.semConverter = new DepGraphToSemanticGraph();
		this.counter = 0;
		this.examples = new HashMap<String,String>();
		examples.put("-1", "The boy faked the illness.");
		examples.put("-2", "Negotiations prevented the strike.");
		examples.put("-3", "The dog is not eating the food.");
		examples.put("-4", "The boy walked or drove to school.");
		examples.put("-5", " No woman is walking.");
		examples.put("-6", "Max forgot to close the door.");

	}
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.getWriter().print("Hello, World!");     
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if ( request.getParameter("id") != null){
        	String id = request.getParameter("id");
        	if (Integer.parseInt(id) < 0){
        		request.setAttribute("sent", examples.get(id));
        		request.setAttribute("counter", String.valueOf(id));
        		request.getRequestDispatcher("response.jsp").forward(request, response); 
        		return;
        	}
        }
        String sentence = request.getParameter("sentence");
        counter++;
        this.graph = semConverter.sentenceToGraph(sentence, sentence);
        if (this.graph == null) this.graph = new sem.graph.SemanticGraph();
        createImages(counter);
        try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        request.setAttribute("sent", sentence);
        request.setAttribute("counter", counter);
        request.getRequestDispatcher("response.jsp").forward(request, response); 
    }
    
    protected void createImages(Integer counter){
    	ServletContext context = getServletContext();
    	String fullPath = context.getRealPath("src/main/webapp/images/");
    	String typo = "/Library/Tomcat/webapps/sem.mapper/";
    	String kate_apache = "/Users/kkalouli/Documents/Programs/apache-tomcat-9.0.19/webapps/sem.mapper/";
    	String kate_eclip = "src/main/webapp/";
        try {
			ImageIO.write(graph.saveRolesAsImage(),"png", new File(typo+"images/roles_"+counter+".png")); 
			ImageIO.write(graph.saveDepsAsImage(),"png", new File(typo+"images/deps_"+counter+".png"));
		    ImageIO.write(graph.saveContextsAsImage(),"png", new File(typo+"images/ctxs_"+counter+".png"));
		    ImageIO.write(graph.savePropertiesAsImage(),"png", new File(typo+"images/props_"+counter+".png"));
		    ImageIO.write(graph.saveLexAsImage(),"png", new File(typo+"images/lex_"+counter+".png"));
		    ImageIO.write(graph.saveCorefAsImage(),"png", new File(typo+"images/coref_"+counter+".png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
}