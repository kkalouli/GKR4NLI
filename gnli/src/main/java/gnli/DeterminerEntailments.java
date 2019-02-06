package gnli;

import java.util.HashMap;
import java.util.Map;

/** 
 * Monotonicity properties of determiners
 * TODO: complete this
 *
 */
public class DeterminerEntailments {
	
	// Different specifiers/determiners have different monotonicity properties
	// + + some, at least N: some old men are keen gardeners  ⊨ some men are gardeners
	// - + all, every: all men are keen gardeners  ⊨ all old men are keen gardeners  ⊨ all old men are gardeners
	// - - no, at most N: no men are gardeners ⊨ no old men are keen gardeners
	// ? ?  Exactly 3, exactly N: Exactly 3 old men are keen gardeners  ?? Exactly 3 men are gardeners
	// ? + most, many: most men are keen gardeners ⊨ most men are gardeners
	// ? - few:  few men are gardeners ⊨ few men are keen gardeners
	// Downward monotonicity in the body (* -) (e.g. no, few) is accounted for by a scoping negation
	//   "no" = "not a"; "few" = "not many"

	private static final Map<String, Specificity> eMap = new HashMap<String, Specificity>();
	static {
		//Key string is <TSpecifier>_<TCardinality>_<HSpecifier>_<HCardinality>_<T-H-Specificity>
		// value is the specificity that needs to be set in order to get the entialment relation indicated in the examples on the right
		// it is not really the specificity in the sense of super-subclass (e.g. all old men is still a subclass of all men but in the
		// second entry the specificity has to be flipped to superclass in order to account for the fact that all old men =/=> all men)
		// in other words, superclass is always used when there is no entailment in this direction (but there is entailment in the opposite direction)
		// none is used when there is no entailment in none of the directions
		
		//************** a *****************
		// a-a
		eMap.put("a_sg_a_sg_SUPERCLASS",Specificity.SUPERCLASS);  			// a man =/=> an old man 
		eMap.put("a_sg_a_sg_SUBCLASS",Specificity.SUBCLASS);  				// an old man ===> a man
		eMap.put("a_sg_a_sg_EQUALS",Specificity.EQUALS);  					// a man ===> a man
		// a-many & many-a
		eMap.put("a_sg_many_pl_SUPERCLASS",Specificity.NONE); 				// a man =/=> many old men
		eMap.put("a_sg_many_pl_SUBCLASS",Specificity.NONE); 				// an old man =/=> many men
		eMap.put("a_sg_many_pl_EQUALS",Specificity.NONE); 					// a man =/=> many men
		eMap.put("many_pl_a_sg_SUPERCLASS",Specificity.NONE); 				// many men =/=> an old man 
		eMap.put("many_pl_a_sg_SUBCLASS",Specificity.NONE); 				// many old men =/=> a man
		eMap.put("many_pl_a_sg_EQUALS",Specificity.NONE); 					// many men =/=> a man
		// a-few & few-a
		eMap.put("a_sg_few_pl_SUPERCLASS",Specificity.NONE); 				// a man =/=> few old men
		eMap.put("a_sg_few_pl_SUBCLASS",Specificity.NONE); 					// an old man =/=> few men
		eMap.put("ea_sg_few_pl_EQUALS",Specificity.NONE); 					// a man =/=> few men
		eMap.put("few_pl_a_sg_SUPERCLASS",Specificity.NONE); 				// few men =/=> an old man  
		eMap.put("few_pl_a_sg_SUBCLASS",Specificity.NONE); 					// few old men =/=> a man 
		eMap.put("few_pl_a_sg_EQUALS",Specificity.NONE); 					// few men =/=> a man
		// a-much & much-a
		eMap.put("a_sg_much_sg_SUPERCLASS",Specificity.NONE); 				// a waterdrop =/=> much dirty water
		eMap.put("a_sg_much_sg_SUBCLASS",Specificity.NONE); 				// a clean waterdrop =/=> much water
		eMap.put("a_sg_much_sg_EQUALS",Specificity.NONE); 					// a waterdrop =/=> much water
		eMap.put("much_sg_a_sg_SUPERCLASS",Specificity.NONE); 				// much water =/=> a clean waterdrop
		eMap.put("much_sg_a_sg_SUBCLASS",Specificity.NONE); 				// much dirty water =/=> a waterdrop
		eMap.put("much_sg_a_sg_EQUALS",Specificity.NONE); 					// much water =/=> a waterdrop
		// a-little & little-a
		eMap.put("a_sg_little_sg_SUPERCLASS",Specificity.NONE); 			// a waterdrop =/=> little dirty water
		eMap.put("a_sg_little_sg_SUBCLASS",Specificity.NONE); 				// a clean waterdrop =/=> little water
		eMap.put("ea_sg_little_sg_EQUALS",Specificity.NONE); 				// a waterdrop =/=> little water
		eMap.put("little_sg_ea_sg_SUPERCLASS",Specificity.NONE); 			// little water =/=> a clean waterdrop
		eMap.put("little_sg_a_sg_SUBCLASS",Specificity.NONE); 				// little dirty water =/=> a waterdrop
		eMap.put("little_sg_a_sg_EQUALS",Specificity.NONE); 				// little water =/=> a waterdrop
		// a-most & most-a
		eMap.put("a_sg_most_sg_SUPERCLASS",Specificity.NONE); 				// a waterdrop =/=> most dirty water
		eMap.put("a_sg_most_sg_SUBCLASS",Specificity.NONE); 				// a dirty waterdrop =/=> most water
		eMap.put("a_sg_most_sg_EQUALS",Specificity.NONE); 					// a waterdrop ===> most water
		eMap.put("most_sg_a_sg_SUPERCLASS",Specificity.NONE); 				// most water =/=> a dirty waterdrop
		eMap.put("most_sg_a_sg_SUBCLASS",Specificity.NONE); 				// most dirty water =/=> a waterdrop
		eMap.put("most_sg_a_sg_EQUALS",Specificity.NONE);					// most water =/=> a waterdrop
		eMap.put("a_sg_most_pl_SUPERCLASS",Specificity.NONE); 				// a man ===> most old men
		eMap.put("a_sg_most_pl_SUBCLASS",Specificity.NONE); 				// an old man =/=> most men
		eMap.put("a_sg_most_pl_EQUALS",Specificity.NONE); 					// a man ===> most men
		eMap.put("most_pl_a_sg_SUPERCLASS",Specificity.NONE); 				// most men =/=> an old man
		eMap.put("most_pl_a_sg_SUBCLASS",Specificity.NONE); 				// most old men =/=> a man
		eMap.put("most_pl_a_sg_EQUALS",Specificity.NONE);					// most men =/=> a man
		// a-some & some-a
		eMap.put("a_sg_some_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// a man =/=> some old man 
		eMap.put("a_sg_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// an old man ===> some man
		eMap.put("a_sg_some_sg_EQUALS",Specificity.EQUALS); 				// a man ===> some man	
		eMap.put("some_sg_a_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// some man =/=> an old man
		eMap.put("some_sg_a_sg_SUBCLASS",Specificity.SUBCLASS); 			// some old man ===> a man
		eMap.put("some_sg_a_sg_EQUALS",Specificity.EQUALS); 				// some man ===> a man
		eMap.put("a_sg_some_pl_SUPERCLASS",Specificity.NONE); 				// a man =/=> some old men 
		eMap.put("a_sg_some_pl_SUBCLASS",Specificity.NONE); 				// an old man =/=> some men
		eMap.put("a_sg_some_pl_EQUALS",Specificity.NONE); 					// a man =/=> some men
		eMap.put("some_pl_a_sg_SUPERCLASS",Specificity.NONE); 				// some men =/=> an old man 
		eMap.put("some_pl_a_sg_SUBCLASS",Specificity.NONE); 				// some old men =/=> a man
		eMap.put("some_pl_a_sg_EQUALS",Specificity.NONE); 					// some men =/=> a man	
		// a-N & N-a		
		eMap.put("a_sg_N_pl_SUPERCLASS",Specificity.NONE); 					// a man =/=> N old men  
		eMap.put("a_sg_N_pl_SUBCLASS",Specificity.NONE); 					// an old man =/=> N men
		eMap.put("a_sg_N_pl_EQUALS",Specificity.NONE); 						// a man =/=> N men
		eMap.put("N_pl_a_sg_SUPERCLASS",Specificity.NONE); 					// N men =/=> an old man
		eMap.put("N_pl_a_sg_SUBCLASS",Specificity.NONE); 					// N old men =/=> a man
		eMap.put("N_pl_a_sg_EQUALS",Specificity.NONE); 						// N men =/=> a man
		eMap.put("a_sg_N_sg_SUPERCLASS",Specificity.SUPERCLASS); 			// a man =/=> 1 old man  
		eMap.put("a_sg_N_sg_SUBCLASS",Specificity.SUBCLASS); 				// an old man ===> 1 man
		eMap.put("a_sg_N_sg_EQUALS",Specificity.EQUALS); 					// a man ===> 1 man
		eMap.put("N_sg_a_sg_SUPERCLASS",Specificity.SUPERCLASS); 			// 1 man =/=> an old man
		eMap.put("N_sg_a_sg_SUBCLASS",Specificity.SUBCLASS); 				// 1 old man ===> a man
		eMap.put("N_sg_a_sg_EQUALS",Specificity.EQUALS); 					// 1 man =/=> a man

		//************** the *****************
		// the-the
		eMap.put("the_pl_the_pl_SUPERCLASS",Specificity.SUPERCLASS);  		// the men =/=> the old men 
		eMap.put("the_pl_the_pl_SUBCLASS",Specificity.SUBCLASS);  			// the old men ==> the men
		eMap.put("the_pl_the_pl_EQUALS",Specificity.EQUALS);  				// the men ===> the men		
		eMap.put("the_sg_the_sg_SUPERCLASS",Specificity.SUPERCLASS);  		// the man =/=> the old man
		eMap.put("the_sg_the_sg_SUBCLASS",Specificity.SUBCLASS);  			// the old man ===> the man 
		eMap.put("the_sg_the_sg_EQUALS",Specificity.EQUALS);  				// the man ===> the man		
		eMap.put("the_pl_the_sg_SUPERCLASS",Specificity.NONE);  			// the men =/=> the old man 
		eMap.put("the_pl_the_sg_SUBCLASS",Specificity.NONE);  				// the old men =/=> the man
		eMap.put("the_pl_the_sg_EQUALS",Specificity.SUBCLASS);  			// the men =/=> the man		
		eMap.put("the_sg_the_pl_SUPERCLASS",Specificity.NONE);  			// the man =/=> the old men
		eMap.put("the_sg_the_pl_SUBCLASS",Specificity.NONE);  				// the old man =/=> the men 
		eMap.put("the_sg_the_pl_EQUALS",Specificity.NONE);  				// the man =/=> the men		
		// the-many & many-the
		eMap.put("the_pl_many_pl_SUPERCLASS",Specificity.NONE); 			// the men =/=> many old men 
		eMap.put("the_pl_many_pl_SUBCLASS",Specificity.NONE); 				// the old men =/=> many men
		eMap.put("the_pl_many_pl_EQUALS",Specificity.NONE); 				// the men =/=> many men
		eMap.put("many_pl_the_pl_SUPERCLASS",Specificity.NONE); 			// many men =/=> the old men 
		eMap.put("many_pl_the_pl_SUBCLASS",Specificity.NONE); 				// many old men =/=> the men
		eMap.put("many_pl_the_pl_EQUALS",Specificity.NONE); 				// many men =/=> the men	
		eMap.put("the_sg_many_pl_SUPERCLASS",Specificity.NONE); 			// the man =/=> many old men 
		eMap.put("the_sg_many_pl_SUBCLASS",Specificity.NONE); 				// the old man =/=> many men
		eMap.put("the_sg_many_pl_EQUALS",Specificity.NONE); 				// the man =/=> many men
		eMap.put("many_pl_the_sg_SUPERCLASS",Specificity.NONE); 			// many men =/=> the old man 
		eMap.put("many_pl_the_sg_SUBCLASS",Specificity.NONE); 				// many old men =/=> the man
		eMap.put("many_pl_the_sg_EQUALS",Specificity.NONE); 				// many men =/=> the man
		// the-few & few-the
		eMap.put("the_pl_few_pl_SUPERCLASS",Specificity.NONE); 				// the men =/=> few old men 
		eMap.put("the_pl_few_pl_SUBCLASS",Specificity.NONE); 				// the old men =/=> few men
		eMap.put("the_pl_few_pl_EQUALS",Specificity.NONE); 					// the men =/=> few men
		eMap.put("few_pl_the_pl_SUPERCLASS",Specificity.NONE); 				// few men =/=> the old men 
		eMap.put("v_pl_the_pl_SUBCLASS",Specificity.NONE); 					// few old men =/=> the men
		eMap.put("few_pl_the_pl_EQUALS",Specificity.NONE); 					// few men =/=> the men	
		eMap.put("the_sg_few_pl_SUPERCLASS",Specificity.NONE); 				// the man =/=> few old men 
		eMap.put("the_sg_few_pl_SUBCLASS",Specificity.NONE); 				// the old man =/=> few men
		eMap.put("the_sg_few_pl_EQUALS",Specificity.NONE); 					// the man =/=> few men
		eMap.put("few_pl_the_sg_SUPERCLASS",Specificity.NONE); 				// few men =/=> the old man 
		eMap.put("few_pl_the_sg_SUBCLASS",Specificity.NONE); 				// few old men =/=> the man
		eMap.put("few_pl_the_sg_EQUALS",Specificity.NONE); 					// few men =/=> the man
		// the-much & much-the
		eMap.put("the_sg_much_sg_SUPERCLASS",Specificity.NONE); 			// the water =/=> much dirty water 
		eMap.put("the_sg_much_sg_SUBCLASS",Specificity.NONE); 				// the dirty water =/=> much water
		eMap.put("the_sg_much_sg_EQUALS",Specificity.NONE); 				// the water =/=> much water
		eMap.put("much_sg_the_sg_SUPERCLASS",Specificity.NONE); 			// much water =/=> the dirty water 
		eMap.put("much_sg_the_sg_SUBCLASS",Specificity.NONE); 				// much dirty water =/=> the water
		eMap.put("much_sg_the_sg_EQUALS",Specificity.NONE); 				// much water =/=> the water
		eMap.put("the_pl_much_sg_SUPERCLASS",Specificity.NONE); 			// the waterdrops =/=> much dirty water 
		eMap.put("the_pl_much_sg_SUBCLASS",Specificity.NONE); 				// the dirty waterdrops =/=> much water
		eMap.put("the_pl_much_sg_EQUALS",Specificity.NONE); 				// the waterdrops =/=> much water
		eMap.put("much_sg_the_pl_SUPERCLASS",Specificity.NONE); 			// much water =/=> the dirty waterdrops 
		eMap.put("much_sg_the_pl_SUBCLASS",Specificity.NONE); 				// much dirty water =/=> the waterdrops
		eMap.put("much_sg_the_pl_EQUALS",Specificity.NONE); 				// much water =/=> the waterdrops
		// the-little & little-the
		eMap.put("the_sg_little_sg_SUPERCLASS",Specificity.NONE); 			// the water =/=> little dirty water 
		eMap.put("the_sg_little_sg_SUBCLASS",Specificity.NONE); 			// the dirty water =/=> little water
		eMap.put("the_sg_little_sg_EQUALS",Specificity.NONE); 				// the water =/=> little water
		eMap.put("little_sg_the_sg_SUPERCLASS",Specificity.NONE); 			// little water =/=> the dirty water 
		eMap.put("little_sg_the_sg_SUBCLASS",Specificity.NONE); 			// little dirty water =/=> the water
		eMap.put("little_sg_the_sg_EQUALS",Specificity.NONE); 				// little water =/=> the water
		eMap.put("the_pl_little_sg_SUPERCLASS",Specificity.NONE); 			// the waterdrops =/=> little dirty water 
		eMap.put("the_pl_little_sg_SUBCLASS",Specificity.NONE); 			// the dirty waterdrops =/=> little water
		eMap.put("the_pl_little_sg_EQAULS",Specificity.NONE); 				// the waterdrops =/=> little water
		eMap.put("little_sg_the_pl_SUPERCLASS",Specificity.NONE); 			// little water =/=> the dirty waterdrops 
		eMap.put("little_sg_the_pl_SUBCLASS",Specificity.NONE); 			// little dirty water =/=> the waterdrops
		eMap.put("little_sg_the_pl_EQUALS",Specificity.NONE); 				// little water =/=> the waterdrops
		// the-most & most-the
		eMap.put("the_pl_most_pl_SUPERCLASS",Specificity.NONE); 			// the men =/=> most old men 
		eMap.put("the_pl_most_pl_SUBCLASS",Specificity.NONE); 				// the old men =/=> most men
		eMap.put("the_pl_most_pl_EQUALS",Specificity.NONE); 			// the men =/=> most men
		eMap.put("the_sg_most_sg_SUPERCLASS",Specificity.NONE); 			// the water =/=> most dirty water 
		eMap.put("the_sg_most_sg_SUBCLASS",Specificity.NONE); 				// the dirty water =/=> most water
		eMap.put("the_sg_most_sg_EQUALS",Specificity.NONE); 			// the water =/=> most water
		eMap.put("the_pl_most_sg_SUPERCLASS",Specificity.NONE); 			// the waterdrops =/=> most dirty water 
		eMap.put("the_pl_most_sg_SUBCLASS",Specificity.NONE); 				// the dirty waterdrops =/=> most water
		eMap.put("the_pl_most_sg_EQUALS",Specificity.NONE); 			// the waterdrops =/=> most water
		eMap.put("most_pl_the_pl_SUPERCLASS",Specificity.NONE); 			// most men =/=> the old men 
		eMap.put("most_pl_the_pl_SUBCLASS",Specificity.NONE); 				// most old men =/=> the men
		eMap.put("most_pl_the_pl_EQUALS",Specificity.NONE); 				// most men =/=> the men
		eMap.put("most_sg_the_sg_SUPERCLASS",Specificity.NONE); 			// most water =/=> the dirty water 
		eMap.put("most_sg_the_sg_SUBCLASS",Specificity.NONE); 				// most dirty water =/=> the water
		eMap.put("most_sg_the_sg_EQUALS",Specificity.NONE); 				// most water =/=> the water
		eMap.put("most_pl_the_sg_SUPERCLASS",Specificity.NONE); 			// most waterdrops =/=> the dirty water
		eMap.put("most_pl_the_sg_SUBCLASS",Specificity.NONE); 				// most dirty waterdrops =/=> the water
		eMap.put("most_pl_the_sg_EQUALS",Specificity.NONE); 				// most waterdrops =/=> the water
		// the-some & some-the
		eMap.put("the_pl_some_pl_SUPERCLASS",Specificity.NONE); 			// the men =/=> some old men 
		eMap.put("the_pl_some_pl_SUBCLASS",Specificity.SUBCLASS); 			// the old men ===> some men
		eMap.put("the_pl_some_pl_EQUALS",Specificity.SUBCLASS); 			// the men ===> some men	
		eMap.put("some_pl_the_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some men =/=> the old men 
		eMap.put("some_pl_the_pl_SUBCLASS",Specificity.NONE); 				// some old men =/=> the men
		eMap.put("some_pl_the_pl_EQUALS",Specificity.SUPERCLASS); 			// some men =/=> the men
		eMap.put("the_sg_some_sg_SUPERCLASS",Specificity.NONE); 		// the water =/=> some dirty water 
		eMap.put("the_sg_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// the dirty water ==> some water
		eMap.put("the_sg_some_sg_EQUALS",Specificity.SUBCLASS); 			// the water ===> some water	
		eMap.put("some_sg_the_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// some water =/=> the dirty water 
		eMap.put("some_sg_the_sg_SUBCLASS",Specificity.NONE); 				// some dirty water =/=> the water
		eMap.put("some_sg_the_sg_EQUALS",Specificity.SUPERCLASS); 			// some water =/=> the water	
		eMap.put("the_pl_some_sg_SUPERCLASS",Specificity.NONE); 			// the waterdrops =/=> some dirty water 
		eMap.put("the_pl_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// the dirty waterdrops ===> some water
		eMap.put("the_pl_some_sg_EQUALS",Specificity.SUBCLASS); 			// the waterdrops ==> some water	
		eMap.put("some_sg_the_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some water =/=> the dirty waterdrops 
		eMap.put("some_sg_the_pl_SUBCLASS",Specificity.NONE); 				// some dirty water =/=> the waterdrops
		eMap.put("some_sg_the_pl_EQUALS",Specificity.SUPERCLASS); 			// some water =/=> the waterdrops
		// the-every & every-the
		eMap.put("the_pl_every_sg_SUPERCLASS",Specificity.NONE);  			// the men =/=> every old man 
		eMap.put("the_pl_every_sg_SUBCLASS",Specificity.SUPERCLASS);  		// the old men =/=> every man
		eMap.put("the_pl_every_sg_EQUALS",Specificity.NONE);  				// the men =/=> every man		
		eMap.put("every_sg_the_pl_SUPERCLASS",Specificity.SUBCLASS);  		// every man ==> the old men
		eMap.put("every_sg_the_pl_SUBCLASS",Specificity.NONE);  			// every old man =/=> the men
		eMap.put("every_sg_the_pl_EQUALS",Specificity.NONE);  				// every man =/=> the men
		eMap.put("the_sg_every_sg_SUPERCLASS",Specificity.NONE);  			// the water =/=> every dirty waterdrop 
		eMap.put("the_sg_every_sg_SUBCLASS",Specificity.NONE);  			// the dirty water =/=> every waterdrop
		eMap.put("the_sg_every_sg_EQUALS",Specificity.NONE);  				// the water =/=> every waterdrop	
		eMap.put("every_sg_the_sg_SUPERCLASS",Specificity.NONE);  			// every waterdrop =/=> the dirty water
		eMap.put("every_sg_the_sg_SUBCLASS",Specificity.NONE);  			// every dirty waterdrop =/=> the water
		eMap.put("every_sg_the_sg_EQUALS",Specificity.EQUALS);  			// every waterdrop =/=> the water
		// the-N & the-all		: to check 
		eMap.put("the_pl_N_pl_SUPERCLASS",Specificity.NONE); 				// the men =/=> N old men 
		eMap.put("the_pl_N_pl_SUBCLASS",Specificity.NONE); 					// the old men =/=> N men
		eMap.put("the_pl_N_pl_EQUALS",Specificity.NONE); 					// the men =/=> N men	
		eMap.put("N_pl_the_pl_SUPERCLASS",Specificity.NONE); 				// N men =/=> the old men 
		eMap.put("N_pl_the_pl_SUBCLASS",Specificity.NONE); 					// N old men ===> the men
		eMap.put("N_pl_the_pl_EQUALS",Specificity.NONE); 					// N men ===> the men	
		eMap.put("the_sg_N_pl_SUPERCLASS",Specificity.NONE); 				// the water =/=> N dirty waterdrops 
		eMap.put("the_sg_N_pl_SUBCLASS",Specificity.NONE); 					// the dirty water =/=> N waterdrops
		eMap.put("the_sg_N_pl_EQUALS",Specificity.NONE); 					// the water =/=> N waterdrops	
		eMap.put("the_pl_N_sg_SUPERCLASS",Specificity.NONE); 				// the men =/=> 1 old man 
		eMap.put("the_pl_N_sg_SUBCLASS",Specificity.NONE); 					// the old men =/=> 1 man
		eMap.put("the_pl_N_sg_EQUALS",Specificity.NONE); 					// the men =/=> 1 man
		eMap.put("the_sg_N_sg_SUPERCLASS",Specificity.NONE); 				// the water =/=> 1 dirty waterdrop 
		eMap.put("the_sg_N_sg_SUBCLASS",Specificity.NONE); 					// the dirty water =/=> 1 waterdrop
		eMap.put("the_sg_N_sg_EQUALS",Specificity.NONE); 					// the water =/=> 1 waterdrop				
		eMap.put("N_pl_the_sg_SUPERCLASS",Specificity.NONE); 				// N waterdrops =/=> the dirty water 
		eMap.put("N_pl_the_sg_SUBCLASS",Specificity.NONE); 					// N dirty waterdrops =/=> the water
		eMap.put("N_pl_the_sg_EQUALS",Specificity.NONE); 					// N waterdrops =/=> the water
		eMap.put("N_sg_the_pl_SUPERCLASS",Specificity.NONE); 				// 1 man =/=> the old men 
		eMap.put("N_sg_the_pl_SUBCLASS",Specificity.NONE); 					// 1 old man =/=> the men
		eMap.put("N_sg_the_pl_EQUALS",Specificity.NONE); 					// 1 man =/=> the men
		eMap.put("N_sg_the_sg_SUPERCLASS",Specificity.NONE); 				// 1 waterdrop =/=> the dirty water 
		eMap.put("N_sg_the_sg_SUBCLASS",Specificity.NONE); 					// 1 dirty waterdrop =/=> the water
		eMap.put("N_sg_the_sg_EQUALS",Specificity.NONE); 					// 1 waterdrop =/=> the water
		
		
		//************** all *****************
		// all-all
		eMap.put("all_pl_all_pl_SUPERCLASS",Specificity.SUBCLASS);  		// all men ===> all old men 
		eMap.put("all_pl_all_pl_SUBCLASS",Specificity.SUPERCLASS);  		// all old men =/=> all men
		eMap.put("all_pl_all_pl_EQUALS",Specificity.EQUALS);  				// all men ===> all men
		eMap.put("all_sg_all_sg_SUPERCLASS",Specificity.SUBCLASS);  		// all water ===> all dirty water
		eMap.put("all_sg_all_sg_SUBCLASS",Specificity.SUPERCLASS);  		// all dirty water =/=> all water 
		eMap.put("all_sg_all_sg_EQUALS",Specificity.EQUALS);  				// all water ===> all water
		eMap.put("all_pl_all_sg_SUPERCLASS",Specificity.NONE);  			// all waterdrops =/=> all dirty water 
		eMap.put("all_pl_all_sg_SUBCLASS",Specificity.NONE);  				// all dirty waterdrops =/=> all water
		eMap.put("all_pl_all_sg_EQUALS",Specificity.EQUALS);  				// all waterdrops ===> all water		
		eMap.put("all_sg_all_pl_SUPERCLASS",Specificity.NONE);  			// all water =/=> all dirty waterdrops
		eMap.put("all_sg_all_pl_SUBCLASS",Specificity.NONE);  				// all dirty waterdrops =/=> all water 
		eMap.put("all_sg_all_pl_EQUALS",Specificity.EQUALS);  				// all waterdrops ===> all water			
		// all-many & many-all
		eMap.put("all_pl_many_pl_SUPERCLASS",Specificity.NONE); 			// all men =/=> many old men 
		eMap.put("all_pl_many_pl_SUBCLASS",Specificity.NONE); 				// all old men =/=> many men
		eMap.put("all_pl_many_pl_EQUALS",Specificity.NONE); 				// all men =/=> many men
		eMap.put("many_pl_all_pl_SUPERCLASS",Specificity.NONE); 			// many men =/=> all old men 
		eMap.put("many_pl_all_pl_SUBCLASS",Specificity.NONE); 				// many old men =/=> all men
		eMap.put("many_pl_all_pl_EQUALS",Specificity.NONE); 				// many men =/=> all men
		// all-few & few-all
		eMap.put("all_pl_few_pl_SUPERCLASS",Specificity.NONE); 				// all men =/=> few old men 
		eMap.put("all_pl_few_pl_SUBCLASS",Specificity.NONE); 				// all old men =/=> few men
		eMap.put("all_pl_few_pl_EQUALS",Specificity.NONE); 					// all men =/=> few men 
		eMap.put("few_pl_all_pl_SUPERCLASS",Specificity.NONE); 				// few men =/=> all old men 
		eMap.put("few_pl_all_pl_SUBCLASS",Specificity.NONE); 				// few old men =/=> all men 
		eMap.put("few_pl_all_pl_EQUALS",Specificity.NONE); 					// few men =/=> all men 
		// all-much & much-all
		eMap.put("all_sg_much_sg_SUPERCLASS",Specificity.NONE); 			// all water =/=> much dirty water 
		eMap.put("all_sg_much_sg_SUBCLASS",Specificity.NONE); 				// all dirty water =/=> much water
		eMap.put("all_sg_much_sg_EQUALS",Specificity.NONE); 				// all water =/=> much water
		eMap.put("much_sg_all_sg_SUPERCLASS",Specificity.NONE); 			// much water =/=> all dirty water 
		eMap.put("much_sg_all_sg_SUBCLASS",Specificity.NONE); 				// much dirty water =/=> all water
		eMap.put("much_sg_all_sg_EQUALS",Specificity.NONE); 				// much water =/=> all water
		eMap.put("all_pl_much_sg_SUPERCLASS",Specificity.NONE); 			// all waterdrops =/=> much dirty water 
		eMap.put("all_pl_much_sg_SUBCLASS",Specificity.NONE); 				// all dirty waterdrops =/=> much water
		eMap.put("all_pl_much_sg_EQUALS",Specificity.NONE); 				// all waterdrops =/=> much water
		eMap.put("much_sg_all_pl_SUPERCLASS",Specificity.NONE); 			// much water =/=> all dirty waterdrops 
		eMap.put("much_sg_all_pl_SUBCLASS",Specificity.NONE); 				// much dirty water =/=> all waterdrops
		eMap.put("much_sg_all_pl_EQUALS",Specificity.NONE); 				// much water =/=> all waterdrops
		// all-little & little-all
		eMap.put("all_sg_little_sg_SUPERCLASS",Specificity.NONE); 			// all water =/=> little dirty water 
		eMap.put("all_sg_little_sg_SUBCLASS",Specificity.NONE); 			// all dirty water =/=> little water
		eMap.put("all_sg_little_sg_EQUALS",Specificity.NONE); 				// all water =/=> little water
		eMap.put("little_sg_all_sg_SUPERCLASS",Specificity.NONE); 			// little water =/=> all dirty water 
		eMap.put("little_sg_all_sg_SUBCLASS",Specificity.NONE); 			// little dirty water =/=> all water
		eMap.put("little_sg_all_sg_EQUALS",Specificity.NONE); 				// little water =/=> all water
		eMap.put("all_pl_little_sg_SUPERCLASS",Specificity.NONE); 			// all waterdrops =/=> little dirty water 
		eMap.put("all_pl_little_sg_SUBCLASS",Specificity.NONE); 			// all dirty waterdrops =/=> little water
		eMap.put("all_pl_little_sg_EQAULS",Specificity.NONE); 				// all waterdrops =/=> little water
		eMap.put("little_sg_all_pl_SUPERCLASS",Specificity.NONE); 			// little water =/=> all dirty waterdrops 
		eMap.put("little_sg_all_pl_SUBCLASS",Specificity.NONE); 			// little dirty water =/=> all waterdrops
		eMap.put("little_sg_all_pl_EQUALS",Specificity.NONE); 				// little water =/=> all waterdrops
		// all-most & most-all
		eMap.put("all_pl_most_pl_SUPERCLASS",Specificity.NONE); 			// all men =/=> most old men 
		eMap.put("all_pl_most_pl_SUBCLASS",Specificity.NONE); 				// all old men =/=> most men
		eMap.put("all_pl_most_pl_EQUALS",Specificity.SUBCLASS); 			// all men ===> most men
		eMap.put("all_sg_most_sg_SUPERCLASS",Specificity.NONE); 			// all water =/=> most dirty water 
		eMap.put("all_sg_most_sg_SUBCLASS",Specificity.NONE); 				// all dirty water =/=> most water
		eMap.put("all_sg_most_sg_EQUALS",Specificity.SUBCLASS); 			// all water ===> most water
		eMap.put("all_pl_most_sg_SUPERCLASS",Specificity.NONE); 			// all waterdrops =/=> most dirty water 
		eMap.put("all_pl_most_sg_SUBCLASS",Specificity.NONE); 				// all dirty waterdrops =/=> most water
		eMap.put("all_pl_most_sg_EQUALS",Specificity.SUBCLASS); 			// all waterdrops =/=> most water
		eMap.put("most_pl_all_pl_SUPERCLASS",Specificity.NONE); 			// most men =/=> all old men 
		eMap.put("most_pl_all_pl_SUBCLASS",Specificity.NONE); 				// most old men =/=> all men
		eMap.put("most_pl_all_pl_EQUALS",Specificity.NONE); 				// most men =/=> all men
		eMap.put("most_sg_all_sg_SUPERCLASS",Specificity.NONE); 			// most water =/=> all dirty water 
		eMap.put("most_sg_all_sg_SUBCLASS",Specificity.NONE); 				// most dirty water =/=> all water
		eMap.put("most_sg_all_sg_EQUALS",Specificity.NONE); 				// most water =/=> all water
		eMap.put("most_pl_all_sg_SUPERCLASS",Specificity.NONE); 			// most waterdrops =/=> all dirty water
		eMap.put("most_pl_all_sg_SUBCLASS",Specificity.NONE); 				// most dirty waterdrops =/=> all water
		eMap.put("most_pl_all_sg_EQUALS",Specificity.NONE); 				// most waterdrops =/=> all water
		// all-some & some-all
		eMap.put("all_pl_some_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// all men ===> some old men 
		eMap.put("all_pl_some_pl_SUBCLASS",Specificity.SUBCLASS); 			// all old men ===> some men
		eMap.put("all_pl_some_pl_EQUALS",Specificity.SUBCLASS); 			// all men ===> some men
		eMap.put("all_sg_some_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// all water ===> some dirty water 
		eMap.put("all_sg_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// all dirty water ===> some water
		eMap.put("all_sg_some_sg_EQUALS",Specificity.SUBCLASS); 			// all water ===> some water
		eMap.put("all_pl_some_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// all waterdrops ===> some dirty water 
		eMap.put("all_pl_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// all dirty waterdrops ===> some water
		eMap.put("all_pl_some_sg_EQUALS",Specificity.SUBCLASS); 			// all waterdrops ===> some water
		eMap.put("some_pl_all_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some men =/=> all old men 
		eMap.put("some_pl_all_pl_SUBCLASS",Specificity.SUPERCLASS); 		// some old men =/=> all men
		eMap.put("some_pl_all_pl_EQUALS",Specificity.SUPERCLASS); 			// some men =/=> all men
		eMap.put("some_sg_all_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// some water =/=> all dirty water 
		eMap.put("some_sg_all_sg_SUBCLASS",Specificity.SUPERCLASS); 		// some dirty water =/=> all water
		eMap.put("some_sg_all_sg_EQUALS",Specificity.SUPERCLASS); 			// some water =/=> all water
		eMap.put("some_sg_all_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some water =/=> all dirty waterdrops 
		eMap.put("some_sg_all_pl_SUBCLASS",Specificity.SUPERCLASS); 		// some dirty water =/=> all waterdrops
		eMap.put("some_sg_all_pl_EQUALS",Specificity.SUPERCLASS); 			// some water =/=> all waterdrops
		// all-every & every-all
		eMap.put("all_pl_every_sg_SUPERCLASS",Specificity.SUBCLASS);  		// all men ===> every old man 
		eMap.put("all_pl_every_sg_SUBCLASS",Specificity.SUPERCLASS);  		// all old men =/=> every man
		eMap.put("all_pl_every_sg_EQUALS",Specificity.EQUALS);  			// all men ===> every man
		eMap.put("all_sg_every_sg_SUPERCLASS",Specificity.SUBCLASS);  		// all water ===> every dirty waterdrop 
		eMap.put("all_sg_every_sg_SUBCLASS",Specificity.SUPERCLASS);  		// all dirty water =/=> every waterdrop
		eMap.put("all_sg_every_sg_EQUALS",Specificity.EQUALS);  			// all water ===> every waterdrop
		eMap.put("every_sg_all_pl_SUPERCLASS",Specificity.SUBCLASS);  		// every man ==> all old men
		eMap.put("every_sg_all_pl_SUBCLASS",Specificity.SUPERCLASS);  		// every old man =/=> all men
		eMap.put("every_sg_all_pl_EQUALS",Specificity.EQUALS);  			// every old man ===> all old men
		eMap.put("every_sg_all_sg_SUPERCLASS",Specificity.SUBCLASS);  		// every waterdrop ==> all dirty water
		eMap.put("every_sg_all_sg_SUBCLASS",Specificity.SUPERCLASS);  		// every dirty waterdrop =/=> all water
		eMap.put("every_sg_all_sg_EQUALS",Specificity.EQUALS);  			// every waterdrop ===> all water
		// all-N & N-all		
		eMap.put("all_pl_N_pl_SUPERCLASS",Specificity.NONE); 				// all men =/=> N old men 
		eMap.put("all_pl_N_pl_SUBCLASS",Specificity.NONE); 					// all old men =/=> N men
		eMap.put("all_pl_N_pl_EQUALS",Specificity.NONE); 					// all men =/=> N men
		eMap.put("all_sg_N_pl_SUPERCLASS",Specificity.NONE); 				// all water =/=> N dirty waterdrops 
		eMap.put("all_sg_N_pl_SUBCLASS",Specificity.NONE); 					// all dirty water =/=> N waterdrops
		eMap.put("all_sg_N_pl_EQUALS",Specificity.NONE); 					// all water =/=> N waterdrops	
		eMap.put("all_pl_N_sg_SUPERCLASS",Specificity.NONE); 				// all men =/=> 1 old man 
		eMap.put("all_pl_N_sg_SUBCLASS",Specificity.NONE); 					// all old men =/=> 1 man
		eMap.put("all_pl_N_sg_EQUALS",Specificity.NONE); 					// all men =/=> 1 man
		eMap.put("all_sg_N_sg_SUPERCLASS",Specificity.NONE); 				// all water =/=> 1 dirty waterdrop1 
		eMap.put("all_sg_N_sg_SUBCLASS",Specificity.NONE); 					// all dirty water =/=> 1 waterdro1
		eMap.put("all_sg_N_sg_EQUALS",Specificity.NONE); 					// all water =/=> 1 waterdrop		
		eMap.put("N_pl_all_pl_SUPERCLASS",Specificity.NONE); 				// N men =/=> all old men 
		eMap.put("N_pl_all_pl_SUBCLASS",Specificity.NONE); 					// N old men =/=> all men
		eMap.put("N_pl_all_pl_EQUALS",Specificity.NONE); 					// N men =/=> all men
		eMap.put("N_pl_all_sg_SUPERCLASS",Specificity.NONE); 				// N waterdrops =/=> all dirty water 
		eMap.put("N_pl_all_sg_SUBCLASS",Specificity.NONE); 					// N dirty waterdrops =/=> all water
		eMap.put("N_pl_all_sg_EQUALS",Specificity.NONE); 					// N waterdrops =/=> all water
		eMap.put("N_sg_all_pl_SUPERCLASS",Specificity.NONE); 				// 1 man =/=> all old men 
		eMap.put("N_sg_all_pl_SUBCLASS",Specificity.NONE); 					// 1 old man =/=> all men
		eMap.put("N_sg_all_pl_EQUALS",Specificity.NONE); 					// 1 man =/=> all men
		eMap.put("N_sg_all_sg_SUPERCLASS",Specificity.NONE); 				// 1 waterdrop =/=> all dirty water 
		eMap.put("N_sg_all_sg_SUBCLASS",Specificity.NONE); 					// 1 dirty waterdrop =/=> all water
		eMap.put("N_sg_all_sg_EQUALS",Specificity.NONE); 					// 1 waterdrop =/=> all water
		
		//************** every *****************
		// every-every
		eMap.put("every_sg_every_sg_SUPERCLASS",Specificity.SUBCLASS);  	// every man ===> every old man 
		eMap.put("every_sg_every_sg_SUBCLASS",Specificity.SUPERCLASS);  	// every old man =/=> every man
		eMap.put("every_sg_every_sg_EQUALS",Specificity.EQUALS);  			// every man =/=> every man
		// every-many & many-every
		eMap.put("every_sg_many_pl_SUPERCLASS",Specificity.NONE); 			// every man =/=> many old men
		eMap.put("every_sg_many_pl_SUBCLASS",Specificity.NONE); 			// every old man =/=> many men
		eMap.put("every_sg_many_pl_EQUALS",Specificity.NONE); 				// every man =/=> many men
		eMap.put("many_pl_every_sg_SUPERCLASS",Specificity.NONE); 			// many men =/=> every old man 
		eMap.put("many_pl_every_sg_SUBCLASS",Specificity.NONE); 			// many old men =/=> every man
		eMap.put("many_pl_every_sg_EQUALS",Specificity.NONE); 				// many men =/=> every man
		// every-few & few-every
		eMap.put("every_sg_few_pl_SUPERCLASS",Specificity.NONE); 			// every man =/=> few old men
		eMap.put("every_sg_few_pl_SUBCLASS",Specificity.NONE); 				// every old man =/=> few men
		eMap.put("every_sg_few_pl_EQUALS",Specificity.NONE); 				// every man =/=> few men
		eMap.put("few_pl_every_sg_SUPERCLASS",Specificity.NONE); 			// few men =/=> every old man  
		eMap.put("few_pl_every_sg_SUBCLASS",Specificity.NONE); 				// few old men =/=> every man 
		eMap.put("few_pl_every_sg_EQUALS",Specificity.NONE); 				// few men =/=> every man
		// every-much & much-every
		eMap.put("every_sg_much_sg_SUPERCLASS",Specificity.NONE); 			// every waterdrop =/=> much dirty water
		eMap.put("every_sg_much_sg_SUBCLASS",Specificity.NONE); 			// every clean waterdrop =/=> much water
		eMap.put("every_sg_much_sg_EQUALS",Specificity.NONE); 				// every waterdrop =/=> much water
		eMap.put("much_sg_every_sg_SUPERCLASS",Specificity.NONE); 			// much water =/=> every clean waterdrop
		eMap.put("much_sg_every_sg_SUBCLASS",Specificity.NONE); 			// much dirty water =/=> every waterdrop
		eMap.put("much_sg_every_sg_EQUALS",Specificity.NONE); 				// much water =/=> every waterdrop
		// every-little & little-every
		eMap.put("every_sg_little_sg_SUPERCLASS",Specificity.NONE); 		// every waterdrop =/=> little dirty water
		eMap.put("every_sg_little_sg_SUBCLASS",Specificity.NONE); 			// every clean waterdrop =/=> little water
		eMap.put("every_sg_little_sg_EQUALS",Specificity.NONE); 			// every waterdrop =/=> little water
		eMap.put("little_sg_every_sg_SUPERCLASS",Specificity.NONE); 		// little water =/=> every clean waterdrop
		eMap.put("little_sg_every_sg_SUBCLASS",Specificity.NONE); 			// little dirty water =/=> every waterdrop
		eMap.put("little_sg_every_sg_EQUALS",Specificity.NONE); 			// little water =/=> every waterdrop
		// every-most & most-every
		eMap.put("every_sg_most_sg_SUPERCLASS",Specificity.NONE); 			// every waterdrop =/=> most dirty water
		eMap.put("every_sg_most_sg_SUBCLASS",Specificity.NONE); 			// every dirty waterdrop =/=> most water
		eMap.put("every_sg_most_sg_EQUALS",Specificity.SUBCLASS); 			// every waterdrop ===> most water
		eMap.put("most_sg_every_sg_SUPERCLASS",Specificity.NONE); 			// most water =/=> every dirty waterdrop
		eMap.put("most_sg_every_sg_SUBCLASS",Specificity.NONE); 			// most dirty water =/=> every waterdrop
		eMap.put("most_sg_every_sg_EQUALS",Specificity.SUPERCLASS);			// most water =/=> every waterdrop
		eMap.put("every_sg_most_pl_SUPERCLASS",Specificity.SUBCLASS); 		// every man ===> most old men
		eMap.put("every_sg_most_pl_SUBCLASS",Specificity.NONE); 			// every old man =/=> most men
		eMap.put("every_sg_most_pl_EQUALS",Specificity.SUBCLASS); 			// every man ===> most men
		eMap.put("most_pl_every_sg_SUPERCLASS",Specificity.NONE); 			// most men =/=> every old man
		eMap.put("most_pl_every_sg_SUBCLASS",Specificity.SUPERCLASS); 		// most old men =/=> every man
		eMap.put("most_pl_every_sg_EQUALS",Specificity.SUPERCLASS);			// most men =/=> every man
	
		// every-some & some-every
		eMap.put("every_sg_some_sg_SUPERCLASS",Specificity.SUBCLASS); 		// every waterdrop ==> some dirty water 
		eMap.put("every_sg_some_sg_SUBCLASS",Specificity.SUBCLASS); 		// every dirty waterdrop ===> some water
		eMap.put("every_sg_some_sg_EQUALS",Specificity.SUBCLASS); 			// every waterdrop ===> some water
		eMap.put("every_sg_some_pl_SUPERCLASS",Specificity.SUBCLASS); 		// every man ==> some old men 
		eMap.put("every_sg_some_pl_SUBCLASS",Specificity.SUBCLASS); 		// every old man ===> some men
		eMap.put("every_sg_some_pl_EQUALS",Specificity.SUBCLASS); 			// every man ===> some man
		eMap.put("some_sg_every_sg_SUPERCLASS",Specificity.SUPERCLASS); 	// some water =/=> every dirty waterdrop 
		eMap.put("some_sg_every_sg_SUBCLASS",Specificity.SUPERCLASS); 		// some dirty water =/=> every waterdrop
		eMap.put("some_sg_every_sg_EQUALS",Specificity.SUPERCLASS); 		// some water =/=> every waterdrop
		eMap.put("some_pl_every_sg_SUPERCLASS",Specificity.SUPERCLASS); 	// some men =/=> every old man 
		eMap.put("some_pl_every_sg_SUBCLASS",Specificity.SUPERCLASS); 		// some old men =/=> every man
		eMap.put("some_pl_every_sg_EQUALS",Specificity.SUPERCLASS); 		// some men =/=> every man
		
		// every-N & N-every		
		eMap.put("every_sg_N_pl_SUPERCLASS",Specificity.NONE); 				// every man =/=> N old men  
		eMap.put("every_sg_N_pl_SUBCLASS",Specificity.NONE); 				// every old man =/=> N men
		eMap.put("every_sg_N_pl_EQUALS",Specificity.NONE); 					// every man =/=> N men
		eMap.put("N_pl_every_sg_SUPERCLASS",Specificity.NONE); 				// N men =/=> every old man
		eMap.put("N_pl_every_sg_SUBCLASS",Specificity.NONE); 				// N old men =/=> every man
		eMap.put("N_pl_every_sg_EQUALS",Specificity.NONE); 					// N men =/=> every man
		eMap.put("every_sg_N_sg_SUPERCLASS",Specificity.NONE); 				// every man =/=> 1 old man  
		eMap.put("every_sg_N_sg_SUBCLASS",Specificity.NONE); 				// every old man =/=> 1 man
		eMap.put("every_sg_N_sg_EQUALS",Specificity.NONE); 					// every man =/=> 1 man
		eMap.put("N_sg_every_sg_SUPERCLASS",Specificity.NONE); 				// 1 man =/=> every old man
		eMap.put("N_sg_every_sg_SUBCLASS",Specificity.NONE); 				// 1 old man =/=> every man
		eMap.put("N_sg_every_sg_EQUALS",Specificity.NONE); 					// 1 man =/=> every man


		//************** some *****************
		// some-some
		eMap.put("some_sg_some_sg_SUPERCLASS",Specificity.SUPERCLASS);  	// some man =/=> some old man 
		eMap.put("some_sg_some_sg_SUBCLASS",Specificity.SUBCLASS);  		// some old man ==> some man
		eMap.put("some_sg_some_sg_EQUALS",Specificity.EQUALS);  			// some man ==> some man	
		eMap.put("some_sg_some_pl_SUPERCLASS",Specificity.SUPERCLASS);  	// some man =/=> some old men 
		eMap.put("some_sg_some_pl_SUBCLASS",Specificity.NONE);  			// some old man =/=> some men
		eMap.put("some_sg_some_pl_EQUALS",Specificity.SUPERCLASS);  			// some man =/=> some men		
		eMap.put("some_pl_some_pl_SUPERCLASS",Specificity.SUPERCLASS);  	// some men =/=> some old men 
		eMap.put("some_pl_some_pl_SUBCLASS",Specificity.SUBCLASS);  		// some old men ==> some men
		eMap.put("some_pl_some_pl_EQUALS",Specificity.EQUALS);  			// some men ==> some men		
		eMap.put("some_pl_some_sg_SUPERCLASS",Specificity.NONE);  			// some men =/=> some old man 
		eMap.put("some_pl_some_sg_SUBCLASS",Specificity.SUBCLASS);  		// some old men ==> some man
		eMap.put("some_pl_some_sg_EQUALS",Specificity.SUBCLASS);  			// some men ==> some man	
		// some-many & many-some
		eMap.put("some_sg_many_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some man =/=> many old men
		eMap.put("some_sg_many_pl_SUBCLASS",Specificity.NONE); 				// some old man =/=> many men
		eMap.put("some_sg_many_pl_EQUALS",Specificity.SUPERCLASS); 			// some man =/=> many men
		eMap.put("some_pl_many_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some men =/=> many old men
		eMap.put("some_pl_many_pl_SUBCLASS",Specificity.NONE); 				// some old men =/=> many men
		eMap.put("some_pl_many_pl_EQUALS",Specificity.SUPERCLASS); 			// some men =/=> many men
		eMap.put("many_pl_some_sg_SUPERCLASS",Specificity.NONE); 			// many men =/=> some old man 
		eMap.put("many_pl_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// many old men ===> some man
		eMap.put("many_pl_some_sg_EQUALS",Specificity.SUBCLASS); 			// many men ===> some man
		eMap.put("many_pl_some_pl_SUPERCLASS",Specificity.NONE); 			// many men =/=> some old men 
		eMap.put("many_pl_some_pl_SUBCLASS",Specificity.SUBCLASS); 			// many old men ===> some men
		eMap.put("many_pl_some_pl_EQUALS",Specificity.SUBCLASS); 			// many men ===> some men	
		// some-few & few-some
		eMap.put("some_sg_few_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some man =/=> few old men
		eMap.put("some_sg_few_pl_SUBCLASS",Specificity.NONE); 				// some old man =/=> few men
		eMap.put("some_sg_few_pl_EQUALS",Specificity.SUPERCLASS); 			// some man =/=> few men
		eMap.put("some_pl_few_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some men =/=> few old men
		eMap.put("some_pl_few_pl_SUBCLASS",Specificity.NONE); 				// some old men =/=> few men
		eMap.put("some_pl_few_pl_EQUALS",Specificity.SUPERCLASS); 			// some men =/=> few men
		eMap.put("few_pl_some_sg_SUPERCLASS",Specificity.NONE); 			// few men =/=> some old man 
		eMap.put("few_pl_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// few old men ===> some man
		eMap.put("few_pl_some_sg_EQUALS",Specificity.SUBCLASS); 			// few men ===> some man
		eMap.put("few_pl_some_pl_SUPERCLASS",Specificity.NONE); 			// few men =/=> some old men 
		eMap.put("few_pl_some_pl_SUBCLASS",Specificity.SUBCLASS); 			// few old men ===> some men
		eMap.put("few_pl_some_pl_EQUALS",Specificity.SUBCLASS); 			// few men ===> some men			
		// some-much & much-some
		eMap.put("some_sg_much_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// some water =/=> much dirty water
		eMap.put("some_sg_much_sg_SUBCLASS",Specificity.NONE); 				// some dirty water =/=> much water
		eMap.put("some_sg_much_sg_EQUALS",Specificity.SUPERCLASS); 			// some water =/=> much water
		eMap.put("some_pl_much_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// some waterdrops =/=> much dirty water
		eMap.put("some_pl_much_sg_SUBCLASS",Specificity.NONE); 				// some dirty waterdrops =/=> much water
		eMap.put("some_pl_much_sg_EQUALS",Specificity.SUPERCLASS); 			// some waterdrops =/=> much water
		eMap.put("much_sg_some_sg_SUPERCLASS",Specificity.NONE); 			// much water =/=> some dirty water 
		eMap.put("much_sg_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// much dirty water ===> some water
		eMap.put("much_sg_some_sg_EQUALS",Specificity.SUBCLASS); 			// much water ===> some water
		eMap.put("much_sg_some_pl_SUPERCLASS",Specificity.NONE); 			// much water =/=> some dirty waterdrops 
		eMap.put("much_sg_some_pl_SUBCLASS",Specificity.SUBCLASS); 			// much dirty water ===> some waterdrops
		eMap.put("much_sg_some_pl_EQUALS",Specificity.SUBCLASS); 			// much water ===> some waterdrops		
		// some-little & little-some
		eMap.put("some_sg_little_sg_SUPERCLASS",Specificity.SUPERCLASS); 	// some water =/=> little dirty water
		eMap.put("some_sg_little_sg_SUBCLASS",Specificity.NONE); 			// some dirty water =/=> little water
		eMap.put("some_sg_little_sg_EQUALS",Specificity.SUPERCLASS); 		// some water =/=> little water
		eMap.put("some_pl_little_sg_SUPERCLASS",Specificity.SUPERCLASS); 	// some waterdrops =/=> little dirty water
		eMap.put("some_pl_little_sg_SUBCLASS",Specificity.NONE); 			// some dirty waterdrops =/=> little water
		eMap.put("some_pl_little_sg_EQUALS",Specificity.SUPERCLASS); 		// some waterdrops =/=> little water
		eMap.put("little_sg_some_sg_SUPERCLASS",Specificity.NONE); 			// little water =/=> some dirty water 
		eMap.put("little_sg_some_sg_SUBCLASS",Specificity.SUBCLASS); 		// little dirty water ===> some water
		eMap.put("little_sg_some_sg_EQUALS",Specificity.SUBCLASS); 			// little water ===> some water
		eMap.put("little_sg_some_pl_SUPERCLASS",Specificity.NONE); 			// little water =/=> some dirty waterdrops 
		eMap.put("little_sg_some_pl_SUBCLASS",Specificity.SUBCLASS); 		// little dirty water ===> some waterdrops
		eMap.put("little_sg_some_pl_EQUALS",Specificity.SUBCLASS); 			// little water ===> some waterdrops		
		// some-most & most-some
		eMap.put("some_sg_most_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// some water =/=> most dirty water
		eMap.put("some_sg_most_sg_SUBCLASS",Specificity.NONE); 				// some dirty water =/=> most water
		eMap.put("some_sg_most_sg_EQUALS",Specificity.SUPERCLASS); 			// some water =/=> most water
		eMap.put("most_sg_some_sg_SUPERCLASS",Specificity.NONE); 			// most water =/=> some dirty water
		eMap.put("most_sg_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// most dirty water ===> some water
		eMap.put("most_sg_some_sg_EQUALS",Specificity.SUBCLASS);			// most water ===> some water		
		eMap.put("some_sg_most_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some man =/=> most old men
		eMap.put("some_sg_most_pl_SUBCLASS",Specificity.NONE); 				// some old man =/=> most men
		eMap.put("some_sg_most_pl_EQUALS",Specificity.SUPERCLASS); 			// some man =/=> most men
		eMap.put("most_pl_some_sg_SUPERCLASS",Specificity.NONE); 			// most men =/=> some old man
		eMap.put("most_pl_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// most old men ===> some man
		eMap.put("most_pl_some_sg_EQUALS",Specificity.SUBCLASS);			// most men ===> some man		
		eMap.put("some_pl_most_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some men =/=> most old men  
		eMap.put("some_pl_most_pl_SUBCLASS",Specificity.NONE); 				// some old men =/=> most men
		eMap.put("some_pl_most_pl_EQUALS",Specificity.SUPERCLASS); 			// some men =/=> most men
		eMap.put("most_pl_some_pl_SUPERCLASS",Specificity.NONE); 			// most men =/=> some old men
		eMap.put("most_pl_some_pl_SUBCLASS",Specificity.SUBCLASS); 			// most old men ===> some men
		eMap.put("most_pl_some_pl_EQUALS",Specificity.SUBCLASS); 			// most men ===> some men				
		// some-N & N-some		
		eMap.put("some_sg_N_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some man =/=> N old men  
		eMap.put("some_sg_N_pl_SUBCLASS",Specificity.NONE); 				// some old man =/=> N men
		eMap.put("some_sg_N_pl_EQUALS",Specificity.SUPERCLASS); 			// some man =/=> N men
		eMap.put("N_pl_some_sg_SUPERCLASS",Specificity.NONE); 				// N men =/=> some old man
		eMap.put("N_pl_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// N old men ===> some man
		eMap.put("N_pl_some_sg_EQUALS",Specificity.SUBCLASS); 				// N men ===> some man	
		eMap.put("some_sg_N_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// some man =/=> 1 old man  
		eMap.put("some_sg_N_sg_SUBCLASS",Specificity.SUPERCLASS); 			// some old man ===> 1 man
		eMap.put("some_sg_N_sg_EQUALS",Specificity.EQUALS); 				// some man ===> 1 man
		eMap.put("N_sg_some_sg_SUPERCLASS",Specificity.SUBCLASS); 			// 1 man =/=> some old man
		eMap.put("N_sg_some_sg_SUBCLASS",Specificity.SUBCLASS); 			// 1 old man ===> some man
		eMap.put("N_sg_some_sg_EQUALS",Specificity.EQUALS); 				// 1 man ===> some man	
		eMap.put("some_pl_N_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// some men =/=> N old men  
		eMap.put("some_pl_N_pl_SUBCLASS",Specificity.NONE); 				// some old men =/=> N men
		eMap.put("some_pl_N_pl_EQUALS",Specificity.SUPERCLASS); 			// some men =/=> N men
		eMap.put("N_pl_some_pl_SUPERCLASS",Specificity.NONE); 				// N men =/=> some old men
		eMap.put("N_pl_some_pl_SUBCLASS",Specificity.SUBCLASS); 			// N old men ===> some men
		eMap.put("N_pl_some_pl_EQUALS",Specificity.SUBCLASS); 				// N men ===> some men			
		eMap.put("some_pl_N_sg_SUPERCLASS",Specificity.NONE); 		// some men =/=> 1 old man  
		eMap.put("some_pl_N_sg_SUBCLASS",Specificity.NONE); 				// some old men =/=> 1 man
		eMap.put("some_pl_N_sg_EQUALS",Specificity.NONE); 					// some men =/=> 1 man
		eMap.put("N_sg_some_pl_SUPERCLASS",Specificity.NONE); 			// 1 man =/=> some old men
		eMap.put("N_sg_some_pl_SUBCLASS",Specificity.NONE); 			// 1 old man =/=> some men
		eMap.put("N_sg_some_pl_EQUALS",Specificity.NONE); 				// 1 man =/=> some men

		
		//************** many *****************
		// many-many
		eMap.put("many_pl_many_pl_SUPERCLASS",Specificity.SUPERCLASS);  	// many men =/=> many old men 
		eMap.put("many_pl_many_pl_SUBCLASS",Specificity.SUBCLASS);  		// many old men ==> many man
		eMap.put("many_pl_many_pl_EQUALS",Specificity.EQUALS);  			// many men ==> many men
		// many-few & few-many
		eMap.put("many_pl_few_pl_SUPERCLASS",Specificity.NONE); 		// many men =/=> few old men
		eMap.put("many_pl_few_pl_SUBCLASS",Specificity.NONE); 				// many old men =/=> few men
		eMap.put("many_pl_few_pl_EQUALS",Specificity.NONE); 			// many men =/=> few men
		eMap.put("few_pl_many_pl_SUPERCLASS",Specificity.NONE); 			// few men =/=> many old men 
		eMap.put("few_pl_many_pl_SUBCLASS",Specificity.NONE); 			// few old men =/=> many men
		eMap.put("few_pl_many_pl_EQUALS",Specificity.NONE); 			// few men =/=> many men			
		// many-much & much-many
		eMap.put("many_pl_much_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// many waterdrops =/=> much dirty water
		eMap.put("many_pl_much_sg_SUBCLASS",Specificity.SUBCLASS); 			// many dirty waterdrops ===> much water
		eMap.put("many_pl_much_sg_EQUALS",Specificity.EQUALS); 				// many waterdrops ===> much water
		eMap.put("much_sg_many_pl_SUPERCLASS",Specificity.SUPERCLASS); 		// much water =/=> many dirty waterdrops 
		eMap.put("much_sg_many_pl_SUBCLASS",Specificity.SUBCLASS); 			// much dirty water ===> many waterdrops
		eMap.put("much_sg_many_pl_EQUALS",Specificity.EQUALS); 				// much water ===> many waterdrops			
		// many-little & little-many
		eMap.put("many_pl_little_sg_SUPERCLASS",Specificity.NONE); 			// many waterdrops =/=> little dirty water
		eMap.put("many_pl_little_sg_SUBCLASS",Specificity.NONE); 			// many dirty waterdrops =/=> little water
		eMap.put("many_pl_little_sg_EQUALS",Specificity.NONE); 				// many waterdrops =/=> little water
		eMap.put("little_sg_many_pl_SUPERCLASS",Specificity.NONE); 			// little water =/=> many dirty waterdrops 
		eMap.put("little_sg_many_pl_SUBCLASS",Specificity.NONE); 			// little dirty water =/=> many waterdrops
		eMap.put("little_sg_many_pl_EQUALS",Specificity.NONE); 				// little water ===> many waterdrops		
		// many-most & most-many
		eMap.put("many_pl_most_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// many waterdrops =/=> most dirty water
		eMap.put("many_pl_most_sg_SUBCLASS",Specificity.SUPERCLASS); 		// many dirty waterdrops =/=> most water
		eMap.put("many_pl_most_sg_EQUALS",Specificity.SUPERCLASS); 			// many waterdrops =/=> most water
		eMap.put("most_sg_many_pl_SUPERCLASS",Specificity.SUBCLASS); 		// most water ===> many dirty waterdrops 
		eMap.put("most_sg_many_pl_SUBCLASS",Specificity.SUBCLASS); 			// most dirty water ===> many waterdrops
		eMap.put("most_sg_many_pl_EQUALS",Specificity.SUBCLASS); 			// most water ===> many waterdrops			
		// many-N & N-many		
		eMap.put("many_pl_N_pl_SUPERCLASS",Specificity.NONE); 				// many men =/=> N old men  
		eMap.put("many_pl_N_pl_SUBCLASS",Specificity.NONE); 				// many old men =/=> N men
		eMap.put("many_pl_N_pl_EQUALS",Specificity.NONE); 					// many men =/=> N men
		eMap.put("N_pl_many_pl_SUPERCLASS",Specificity.NONE); 				// N men =/=> many old men
		eMap.put("N_pl_many_pl_SUBCLASS",Specificity.NONE); 				// N old men =/=> many men
		eMap.put("N_pl_many_pl_EQUALS",Specificity.NONE); 					// N men =/=> many men	
		eMap.put("many_pl_N_sg_SUPERCLASS",Specificity.NONE); 				// many men =/=> 1 old man  
		eMap.put("many_pl_N_sg_SUBCLASS",Specificity.NONE); 				// many old men =/=> 1 man
		eMap.put("many_pl_N_sg_EQUALS",Specificity.NONE); 					// many men =/=> 1 man
		eMap.put("N_sg_many_pl_SUPERCLASS",Specificity.NONE); 				// 1 man =/=> many men
		eMap.put("N_sg_many_pl_SUBCLASS",Specificity.NONE); 				// 1 old man =/=> many men
		eMap.put("N_sg_many_pl_EQUALS",Specificity.NONE); 					// 1 man =/=> many men
		
		//************** much *****************
		// much-much
		eMap.put("much_sg_much_sg_SUPERCLASS",Specificity.SUPERCLASS);  	// much water =/=> much dirty water
		eMap.put("much_sg_much_sg_SUBCLASS",Specificity.SUBCLASS);  		// much dirty water ==> much water
		eMap.put("much_sg_much_sg_EQUALS",Specificity.EQUALS);  			// much water ==> much water
		// much-few & few-much
		eMap.put("much_sg_few_pl_SUPERCLASS",Specificity.NONE); 			// much water =/=> few dirty waterdrops
		eMap.put("much_sg_few_pl_SUBCLASS",Specificity.NONE); 				// much dirty water =/=> few waterdrops
		eMap.put("much_sg_few_pl_EQUALS",Specificity.NONE); 				// much water =/=> few waterdrops
		eMap.put("few_pl_much_sg_SUPERCLASS",Specificity.NONE); 			// few waterdrops =/=> much dirty water
		eMap.put("few_pl_much_sg_SUBCLASS",Specificity.NONE); 				// few dirty waterdrops =/=> much water
		eMap.put("few_pl_much_sg_EQUALS",Specificity.NONE); 				// few waterdrops =/=> much water				
		// much-little & little-much
		eMap.put("much_sg_little_sg_SUPERCLASS",Specificity.NONE); 			// much water =/=> little dirty water
		eMap.put("much_sg_little_sg_SUBCLASS",Specificity.NONE); 			// much dirty water =/=> little water
		eMap.put("much_sg_little_sg_EQUALS",Specificity.NONE); 				// much water =/=> little water
		eMap.put("little_sg_much_sg_SUPERCLASS",Specificity.NONE); 			// little water =/=> much dirty water
		eMap.put("little_sg_much_sg_SUBCLASS",Specificity.NONE); 			// little dirty water =/=> much water
		eMap.put("little_sg_much_sg_EQUALS",Specificity.NONE); 				// little water =/=> much water				
		// much-most & much-most
		eMap.put("much_sg_most_sg_SUPERCLASS",Specificity.SUPERCLASS); 		// much water =/=> most dirty water
		eMap.put("much_sg_most_sg_SUBCLASS",Specificity.NONE); 				// much dirty water =/=> most water
		eMap.put("much_sg_most_sg_EQUALS",Specificity.SUPERCLASS); 			// much water =/=> most water
		eMap.put("most_sg_much_sg_SUPERCLASS",Specificity.NONE); 			// most water =/=> much dirty water
		eMap.put("most_sg_much_sg_SUBCLASS",Specificity.SUBCLASS); 			// most dirty water ===> much water
		eMap.put("most_sg_much_sg_EQUALS",Specificity.SUBCLASS); 			// most water ===> much water			
		// much-N & N-much		
		eMap.put("much_sg_N_pl_SUPERCLASS",Specificity.NONE); 				// much water =/=> N dirty waterdrops 
		eMap.put("much_sg_N_pl_SUBCLASS",Specificity.NONE); 				// much dirty water =/=> N waterdrops
		eMap.put("much_sg_N_pl_EQUALS",Specificity.NONE); 					// much water men =/=> N waterdrops
		eMap.put("N_pl_much_sg_SUPERCLASS",Specificity.NONE); 				// N waterdrops =/=> much dirty water
		eMap.put("N_pl_much_sg_SUBCLASS",Specificity.NONE); 				// N dirty waterdrops =/=> much water
		eMap.put("N_pl_much_sg_EQUALS",Specificity.NONE); 					// N waterdrops =/=> much water		
		eMap.put("much_sg_N_sg_SUPERCLASS",Specificity.NONE); 				// much water =/=> 1 dirty waterdrop  
		eMap.put("much_sg_N_sg_SUBCLASS",Specificity.NONE); 				// much dirty water =/=> 1 waterdrop
		eMap.put("much_sg_N_sg_EQUALS",Specificity.NONE); 					// much water =/=> 1 waterdrop
		eMap.put("N_sg_much_sg_SUPERCLASS",Specificity.NONE); 				// 1 watedrop =/=> much dirty water
		eMap.put("N_sg_much_sg_SUBCLASS",Specificity.NONE); 				// 1 dirty waterdrop =/=> much water
		eMap.put("N_sg_much_sg_EQUALS",Specificity.NONE); 					// 1 dirty waterdrop =/=> much water

		//************** most *****************
		// most-most
		eMap.put("most_sg_most_sg_SUPERCLASS",Specificity.SUPERCLASS);  	// most water =/=> most dirty water
		eMap.put("most_sg_most_sg_SUBCLASS",Specificity.SUPERCLASS);  		// most dirty water =/=> most water
		eMap.put("most_sg_most_sg_EQUALS",Specificity.EQUALS);  			// most water ==> most water
		eMap.put("most_pl_most_pl_SUPERCLASS",Specificity.SUPERCLASS);  	// most waterdrops =/=> most dirty waterdrops
		eMap.put("most_pl_most_pl_SUBCLASS",Specificity.SUPERCLASS);  		// most dirty waterdrops =/=> much waterdrops
		eMap.put("most_pl_most_pl_EQUALS",Specificity.EQUALS);  			// most waterdrops ==> most waterdrops
		eMap.put("most_sg_most_pl_SUPERCLASS",Specificity.SUPERCLASS);  	// most water =/=> most dirty waterdrops
		eMap.put("most_sg_most_pl_SUBCLASS",Specificity.SUPERCLASS);  		// most dirty water =/=> most waterdrops
		eMap.put("most_sg_most_pl_EQUALS",Specificity.EQUALS);  			// most water ==> most waterdrops
		eMap.put("most_pl_most_sg_SUPERCLASS",Specificity.SUPERCLASS);  	// most waterdrops =/=> most dirty water
		eMap.put("most_pl_most_sg_SUBCLASS",Specificity.SUBCLASS);  		// most dirty waterdrops =/=> most water
		eMap.put("most_pl_most_sg_EQUALS",Specificity.EQUALS);  			// most water ==> most water
		// most-few & few-most
		eMap.put("most_sg_few_pl_SUPERCLASS",Specificity.NONE); 			// most water =/=> few dirty waterdrops
		eMap.put("most_sg_few_pl_SUBCLASS",Specificity.NONE); 				// most dirty water =/=> few waterdrops
		eMap.put("most_sg_few_pl_EQUALS",Specificity.NONE); 				// most water =/=> few waterdrops
		eMap.put("few_pl_most_sg_SUPERCLASS",Specificity.NONE); 			// few waterdrops =/=> most dirty water
		eMap.put("few_pl_most_sg_SUBCLASS",Specificity.NONE); 				// few dirty waterdrops =/=> most water
		eMap.put("few_pl_most_sg_EQUALS",Specificity.NONE); 				// few waterdrops =/=> most water				
		eMap.put("most_pl_few_pl_SUPERCLASS",Specificity.NONE); 			// most waterdrops =/=> few dirty waterdrops
		eMap.put("most_pl_few_pl_SUBCLASS",Specificity.NONE); 				// most dirty waterdrops =/=> few waterdrops
		eMap.put("most_pl_few_pl_EQUALS",Specificity.NONE); 				// most waterdrops =/=> few waterdrops
		eMap.put("few_pl_most_pl_SUPERCLASS",Specificity.NONE); 			// few waterdrops =/=> most dirty waterdrops
		eMap.put("few_pl_most_pl_SUBCLASS",Specificity.NONE); 				// few dirty waterdrops =/=> most waterdrops
		eMap.put("few_pl_most_pl_EQUALS",Specificity.NONE); 				// few waterdrops =/=> most waterdrops						
		// most-little & little-most
		eMap.put("most_sg_little_sg_SUPERCLASS",Specificity.NONE); 			// most water =/=> little dirty water
		eMap.put("most_sg_little_sg_SUBCLASS",Specificity.NONE); 			// most dirty water =/=> little water
		eMap.put("most_sg_little_sg_EQUALS",Specificity.NONE); 				// most water =/=> little water
		eMap.put("little_sg_most_sg_SUPERCLASS",Specificity.NONE); 			// little water =/=> most dirty water
		eMap.put("little_sg_most_sg_SUBCLASS",Specificity.NONE); 			// little dirty water =/=> most water
		eMap.put("little_sg_most_sg_EQUALS",Specificity.NONE); 				// little water =/=> most water				
		eMap.put("most_pl_little_sg_SUPERCLASS",Specificity.NONE); 			// most waterdrops =/=> litle dirty water
		eMap.put("most_pl_little_sg_SUBCLASS",Specificity.NONE); 			// most dirty waterdrops =/=> little water
		eMap.put("most_pl_little_sg_EQUALS",Specificity.NONE); 				// most waterdrops =/=> litte water
		eMap.put("little_sg_most_pl_SUPERCLASS",Specificity.NONE); 			// little water =/=> most dirty waterdrops
		eMap.put("little_sg_most_pl_SUBCLASS",Specificity.NONE); 			// little dirty water =/=> most waterdrops
		eMap.put("little_sg_most_pl_EQUALS",Specificity.NONE); 				// little water =/=> most waterdrops								
		// most-N & N-most		
		eMap.put("most_sg_N_pl_SUPERCLASS",Specificity.NONE); 				// most water =/=> N dirty waterdrops 
		eMap.put("most_sg_N_pl_SUBCLASS",Specificity.NONE); 				// most dirty water =/=> N waterdrops
		eMap.put("most_sg_N_pl_EQUALS",Specificity.NONE); 					// most water =/=> N waterdrops
		eMap.put("N_pl_most_sg_SUPERCLASS",Specificity.NONE); 				// N waterdrops =/=> most dirty water
		eMap.put("N_pl_most_sg_SUBCLASS",Specificity.NONE); 				// N dirty waterdrops =/=> most water
		eMap.put("N_pl_most_sg_EQUALS",Specificity.NONE); 					// N waterdrops =/=> most water		
		eMap.put("most_sg_N_sg_SUPERCLASS",Specificity.NONE); 				// most water =/=> 1 dirty waterdrop  
		eMap.put("most_sg_N_sg_SUBCLASS",Specificity.NONE); 				// most dirty water =/=> 1 waterdrop
		eMap.put("most_sg_N_sg_EQUALS",Specificity.NONE); 					// most water =/=> 1 waterdrop
		eMap.put("N_sg_most_sg_SUPERCLASS",Specificity.NONE); 				// 1 watedrop =/=> most dirty water
		eMap.put("N_sg_most_sg_SUBCLASS",Specificity.NONE); 				// 1 dirty waterdrop =/=> most water
		eMap.put("N_sg_most_sg_EQUALS",Specificity.NONE); 					// 1 dirty waterdrop =/=> most water
		eMap.put("most_pl_N_pl_SUPERCLASS",Specificity.NONE); 				// most waterdrops =/=> N dirty waterdrops 
		eMap.put("most_pl_N_pl_SUBCLASS",Specificity.NONE); 				// most dirty waterdrops =/=> N waterdrops
		eMap.put("most_pl_N_pl_EQUALS",Specificity.NONE); 					// most waterdrops =/=> N waterdrops
		eMap.put("N_pl_most_pl_SUPERCLASS",Specificity.NONE); 				// N waterdrops =/=> most dirty waterdrops
		eMap.put("N_pl_most_pl_SUBCLASS",Specificity.NONE); 				// N dirty waterdrops =/=> most waterdrops
		eMap.put("N_pl_most_pl_EQUALS",Specificity.NONE); 					// N waterdrops =/=> most waterdrops		
		eMap.put("most_pl_N_sg_SUPERCLASS",Specificity.NONE); 				// most waterdrops =/=> 1 dirty waterdrop  
		eMap.put("most_pl_N_sg_SUBCLASS",Specificity.NONE); 				// most dirty waterdrops =/=> 1 waterdrop
		eMap.put("most_pl_N_sg_EQUALS",Specificity.NONE); 					// most waterdrops =/=> 1 waterdrop
		eMap.put("N_sg_most_pl_SUPERCLASS",Specificity.NONE); 				// 1 watedrop =/=> most dirty waterdrops
		eMap.put("N_sg_most_pl_SUBCLASS",Specificity.NONE); 				// 1 dirty waterdrop =/=> most waterdrops
		eMap.put("N_sg_most_pl_EQUALS",Specificity.NONE); 					// 1 dirty waterdrop =/=> most waterdrops

		
		//************** N *****************
		// N-N (exactly N)
		eMap.put("N_sg_N_sg_SUPERCLASS",Specificity.SUPERCLASS);  			// 1 man =/=> 1 old man
		eMap.put("N_sg_N_sg_SUBCLASS",Specificity.SUBCLASS);  				// 1 old man ===> 1 man
		eMap.put("N_sg_N_sg_EQUALS",Specificity.EQUALS);  					// 1 man ===> 1 man
		eMap.put("N_pl_N_pl_SUPERCLASS",Specificity.SUPERCLASS);  			// N men =/=> N old men
		eMap.put("N_pl_N_pl_SUBCLASS",Specificity.SUBCLASS);  				// N old men ===> N men
		eMap.put("N_pl_N_pl_EQUALS",Specificity.EQUALS);  					// N men ===> N men
		eMap.put("N_sg_N_pl_SUPERCLASS",Specificity.NONE);  				// 1 man =/=> N old men
		eMap.put("N_sg_N_pl_SUBCLASS",Specificity.NONE);  					// 1 old man =/=> N men
		eMap.put("N_sg_N_pl_EQUALS",Specificity.NONE);  					// 1 man =/=> N men
		eMap.put("N_pl_N_sg_SUPERCLASS",Specificity.NONE);  				// N men =/=> 1 man
		eMap.put("N_pl_N_sg_SUBCLASS",Specificity.NONE);  					// N old men =/=> 1 man
		eMap.put("N_pl_N_sg_EQUALS",Specificity.NONE);  					// N men =/=> 1 man		
		// N-few & few-N
		eMap.put("N_sg_few_pl_SUPERCLASS",Specificity.NONE); 				// 1 man =/=> few old men
		eMap.put("N_sg_few_pl_SUBCLASS",Specificity.NONE); 					// 1 old man =/=> few men
		eMap.put("N_sg_few_pl_EQUALS",Specificity.NONE); 					// 1 man =/=> few men
		eMap.put("few_pl_N_sg_SUPERCLASS",Specificity.NONE); 				// few men =/=> 1 man
		eMap.put("few_pl_N_sg_SUBCLASS",Specificity.NONE); 					// few old men =/=> 1 man
		eMap.put("few_pl_N_sg_EQUALS",Specificity.NONE); 					// few men =/=> 1 man	
		eMap.put("N_pl_few_pl_SUPERCLASS",Specificity.NONE); 				// N men =/=> few old men
		eMap.put("N_pl_few_pl_SUBCLASS",Specificity.NONE); 					// N old men =/=> few men
		eMap.put("N_pl_few_pl_EQUALS",Specificity.NONE); 					// N men =/=> few men
		eMap.put("few_pl_N_pl_SUPERCLASS",Specificity.NONE); 				// few men =/=> N old men
		eMap.put("few_pl_N_pl_SUBCLASS",Specificity.NONE); 					// few old men =/=> N men
		eMap.put("few_pl_N_pl_EQUALS",Specificity.NONE); 					// few men =/=> N men							
		// N-little & little-N
		eMap.put("N_sg_little_sg_SUPERCLASS",Specificity.NONE); 			// N waterdrops =/=> little dirty water
		eMap.put("N_sg_little_sg_SUBCLASS",Specificity.NONE); 				// N dirty waterdrops =/=> little water
		eMap.put("N_sg_little_sg_EQUALS",Specificity.NONE); 				// N waterdrops =/=> little water
		eMap.put("little_sg_N_sg_SUPERCLASS",Specificity.NONE); 			// little water =/=> N dirty waterdrops
		eMap.put("little_sg_N_sg_SUBCLASS",Specificity.NONE); 				// little dirty water =/=> N waterdrops
		eMap.put("little_sg_N_sg_EQUALS",Specificity.NONE); 				// little water =/=> N waterdrops				
		eMap.put("N_pl_little_sg_SUPERCLASS",Specificity.NONE); 			// N waterdrops =/=> litle dirty water
		eMap.put("N_pl_little_sg_SUBCLASS",Specificity.NONE); 				// N dirty waterdrops =/=> little water
		eMap.put("N_pl_little_sg_EQUALS",Specificity.NONE); 				// N waterdrops =/=> litte water
		eMap.put("little_sg_N_pl_SUPERCLASS",Specificity.NONE); 			// little water =/=> N dirty waterdrops
		eMap.put("little_sg_N_pl_SUBCLASS",Specificity.NONE); 				// little dirty water =/=> N waterdrops
		eMap.put("little_sg_N_pl_EQUALS",Specificity.NONE); 				// little water =/=> N waterdrops								

	

	}
	
	public static Specificity newDeterminerSpecificity(String hSpecifier, String hCardinality, String tSpecifier, String tCardinality,Specificity originalSpecificity) {
		StringBuilder sb = new StringBuilder(tSpecifier).append('_').append(tCardinality)
				.append('_').append(hSpecifier).append('_').append(hCardinality)
				.append('_').append(originalSpecificity);
		Specificity retval = eMap.get(sb.toString());
		if (retval == null) {
			return originalSpecificity;
		} else {
			return retval;
		}
	}

}
