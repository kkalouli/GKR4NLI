package sem.graph.vetypes;

import java.io.Serializable;

import sem.graph.EdgeContent;
import sem.graph.SemanticEdge;

/**
 * A {@link SemanticEdge} for connecting naive semantic nodes
 *
 */
public class NSemEdge extends SemanticEdge implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7624250997899740437L;

	public NSemEdge(String label, EdgeContent content) {
		super(label, content);
	}

}
