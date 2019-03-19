package gnli;

import java.io.IOException;

import com.articulate.sigma.KBmanager;


public class SUMORunner {

	
	public SUMORunner(){
	
		KBmanager.getMgr().initializeOnce("/Users/kkalouli/Documents/.sigmakee/KBs");
	
	}
	
	
	public static void main(String args[]) throws IOException {
		SUMORunner runner = new SUMORunner();
	}
	
	
}
