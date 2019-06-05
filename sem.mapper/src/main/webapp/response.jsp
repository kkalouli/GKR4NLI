<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
<title>GKR Parser</title>
<meta charset="UTF-8"/>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet" />
<link href="css/custom.css" rel="stylesheet" />
<style type="text/css">

.tab { margin-left: 30px; }
 
</style>
</head>
    <body style="background-color:#ffe4b2">
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

	<% if(request.getAttribute("error") != null){ %>
			<h2 class="tab" style="color:red">${error}</h2>
		<% } %>

	 <object>${graph}</object> 

	<h2 class="tab" id="dep_graph">Dependency Graph </h2>
	 <h2 class="tab"><img src="${image_folder}/deps_${counter}.png" height="auto" width="auto"></h2> <br>
	<h2 class="tab" id="con_graph">Concept Graph</h2>
	<h2 class="tab"><img src="${image_folder}/roles_${counter}.png" height="auto" width="auto"></h2> <br>
	<h2 class="tab" id="ctx_graph">Context Graph</h2>
	<h2 class="tab"><img src="${image_folder}/ctxs_${counter}.png" height="auto" width="auto"></h2> <br>
	<h2 class="tab" id="prop_graph">Properties Graph</h2>
	<h2 class="tab"><img src="${image_folder}/props_${counter}.png" height="auto" width="auto"></h2> <br>
	<h2 class="tab" id="lex_graph">Lexical Graph</h2>
	<h2 class="tab"><img src="${image_folder}/lex_${counter}.png" height="auto" width="auto"></h2> <br>
	<h2  class="tab" id="link_graph">Coreference Graph</h2>
	<h2 class="tab"><img src="${image_folder}/coref_${counter}.png" height="auto" width="auto"></h2> <br>
	
    </body>
</html>