<%@ page contentType="text/html; charset=iso-8859-1" language="java" %>
<html>
<head>
<title>GKR Parser</title>
<meta charset="UTF-8"/>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet" />
<style type="text/css">

.tab { margin-left: 30px; }

#box { position: relative; margin-left: 30px; }

</style>

<!-- Sets the basepath for the library if not in same directory -->
   <script type="text/javascript">
      mxBasePath = 'src';
   </script>

   <!-- Loads and initializes the library -->
   <script type="text/javascript" src="mxClient.js"></script>

   <!-- Example code -->
   <script type="text/javascript">
    	
   // loads the created xml and decodes it into a mxgrah
        function loadXML(container,xmlString){
	   // check if browser is supported
    	  if (!mxClient.isBrowserSupported())
          {
             mxUtils.error('Browser is not supported!', 200, false);
          }
          else
          {
        	  // create new graph
	    	  var graph = new mxGraph(container);
	          new mxRubberband(graph);
	          // parse xml to a Document
	    	  var doc = mxUtils.parseXml(xmlString);
	    	  var codec = new mxCodec(doc);
	    	  // get first elements of Documents
	    	  var parent = graph.getDefaultParent();
	    	  var firstChild = doc.documentElement.firstChild;
	    	  var root = doc.documentElement.childNodes[1];
	    	  var id0 = root.childNodes[1];
	    	  var id1 = id0.childNodes[1];
	    	  //console.log(doc);
	    	  //console.log(parent);
	    	  //console.log(firstChild);
	    	  //console.log(root);
	    	  //console.log(id0);
	    	  //console.log(id1);
	    	  //console.log(graph);
	    	  // dict of node names as keys and vertics as values
	    	  var dictNodes = {};
	    	  // all the edges that need to be added
	    	  var edges = [];
	    	  // the nodes that have already been added
	    	  var addedNodes = [];
	 
	    	  // go through each of the children and get the attributes
	    	  for (var i = 1; i < id1.childNodes.length; i+=2) {
	    	      console.log("Node ID: " + i);
	    	      var cell = codec.decode(id1.childNodes[i]);
	    	      if (cell != null){
	    	    	 // check if this cell is an edge or a vertex
	    	    	var isEdge = cell.hasAttribute("edge");
	    	    	graph.getModel().beginUpdate();
	    	    	// only add vertex if it doesnt exist
	    	    	if (isEdge == false && addedNodes.includes(cell) == false ){
	    	    		var value = graph.insertVertex(parent, null, cell.getAttribute("value"), 400, 400, 80, 30, cell.getAttribute("style"));
	    	    		console.log(value);
	    	    		graph.updateCellSize(value, true);
	    	    		var geom = value.getGeometry();
	    	    		geom.width = geom.width > 80 ? geom.width : 80;
	    	    		geom.height = geom.height > 30 ? geom.width : 30;
	    	    		dictNodes[cell.getAttribute("id")] = value;
	    	    		addedNodes.push(cell);
	    	    	} else {
	    	    		// if it is an edge, push to the edges
	    	    		edges.push(cell);
	    	    	}
	    	    	graph.getModel().endUpdate();
	       	      }
	    	  }
	    	  console.log(edges);
	    	  //console.log(dict);
	    	  // go thrugh the edges and add them, get the source and the tagrt from the dict
	    	  for (var i = 0; i < edges.length; i++) {
	    		  var source = edges[i].getAttribute("source");
	    		  var target = edges[i].getAttribute("target");
	    		  var sourceNode = dictNodes[source];
	    		  var targetNode = dictNodes[target];
	    		  graph.insertEdge(parent, null, edges[i].getAttribute("value"), sourceNode, targetNode);
	    	  }
	    	  
	    	  // hierarchical layout
			  var layout = new mxHierarchicalLayout(graph);
			  layout.execute(parent);
          }
			
      }
      
   // load all xmls of all graphs 
      function loadAllXmls(){
    	  loadXML(document.getElementById('depContainer'), document.getElementById('depsGraph').innerHTML);
    	  loadXML(document.getElementById('roleContainer'), document.getElementById('roleGraph').innerHTML);
    	  loadXML(document.getElementById('ctxContainer'), document.getElementById('ctxGraph').innerHTML);
    	  loadXML(document.getElementById('propsContainer'), document.getElementById('propsGraph').innerHTML);
    	  loadXML(document.getElementById('lexContainer'), document.getElementById('lexGraph').innerHTML);
    	  loadXML(document.getElementById('corefContainer'), document.getElementById('corefGraph').innerHTML);
      }
   </script>

</head>
 
	<body style="background-color:#ffe4b2" onload="loadAllXmls()" >    

 	<h1 class="tab" id="top"> <font color="#349aff">
		<big>G</big><small>raphical</small> <big>K</big><small>nowledge</small>
		<big>R</big><small>epresentations</small> <small>for</small>  <big>N</big><small>atural</small>
		<big>L</big><small>anguage</small> <big>I</big><small>nference</small> </font>
	</h1>

	<h3 class="tab" id="sentence"><font color="#349aff">Sentence: </font> </h3><p class="tab">${sent} </p> <br>
	<form class="tab" action="index.jsp">
	 <button class="btn btn-primary" type="submit">Home</button>
</form>

	<h3 class="tab" align="left"> <small> Does this GKR graph seem wrong to you? Let us know <a
			href="mailto:aikaterini-lida.kalouli@uni-konstanz.de"> why!</a> </small> </h3>
			
	<h3 class="tab" align="left"> <small> Feel free to scroll up and down the graphs and to move the nodes and the edges to get a better view, if needed. </small> </h3>

	<% if(request.getAttribute("error") != null){ %>
			<h2 class="tab" style="color:red">${error}</h2>
		<% } %>	
	
	<h2 class="tab">Dependency Graph </h2>
	   <!-- Creates a container for the graph with a grid wallpaper style="overflow:hidden;width:600;height:400px" -->
   <div id="depContainer" style="overflow:auto;width:1000;height:200px;padding-left:80px;padding-bottom:50px"> </div>
   <div id="depsGraph">${depsGraph}</div>
	
	<h2 class="tab"> Concept Graph</h2>
   <div id="roleContainer" style="overflow:auto;width:1000;height:200px;padding-left:80px;padding-bottom:50px"> </div>
   <div id="roleGraph">${roleGraph}</div>

	<h2 class="tab">Context Graph</h2>
	<div id="ctxContainer" style="overflow:auto;width:1000;height:200px;padding-left:80px;padding-bottom:50px"> </div>
   <div id="ctxGraph">${ctxGraph}</div>
	
	
	<h2 class="tab" >Properties Graph</h2>
	<div id="propsContainer" style="overflow:auto;width:1000;height:200px;padding-left:80px;padding-bottom:50px"> </div>
   <div id="propsGraph">${propsGraph}</div>
	
	
	<h2 class="tab" >Lexical Graph</h2>
	<div id="lexContainer" style="overflow:auto;width:1000;height:200px;padding-left:80px;padding-bottom:50px"> </div>
   <div id="lexGraph">${lexGraph}</div>
	
	
	<h2  class="tab" >Coreference Graph</h2>
	<div id="corefContainer" style="overflow:auto;width:1000;height:200px;padding-left:80px;padding-bottom:50px"> </div>
   <div id="corefGraph">${corefGraph}</div>
	

    </body>
</html>