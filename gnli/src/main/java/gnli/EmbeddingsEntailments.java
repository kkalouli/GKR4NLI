package gnli;

import java.util.HashMap;
import java.util.Map;

public class EmbeddingsEntailments {
	
	
	private static final Map<String, Specificity> eMap = new HashMap<String, Specificity>();
	static {
		eMap.put("boy->kid",Specificity.SUPERCLASS); 
		eMap.put("kid->boy",Specificity.SUBCLASS); 
	}
	

}
