<%@ page contentType="text/html; charset=iso-8859-1" language="java" %>
<html>
<head>
<title>GKR Parser</title>
<meta charset="UTF-8"/>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet" />
<style type="text/css">

.tab { margin-left: 30px; }

.block { margin-left: 30px; position: fixed; }

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
       
   </script>

</head>
 
	<body style="background-color:#ffe4b2"  >  <!--the following is needed if the graphs should be loaded at once: onload="loadAllXmls()"  -->

 	<h1 class="tab" id="top"> <font color="#349aff"><big>H</big><small>y</small> <small>-</small>  <big>NLI</big><small>: a</small>
		<big>H</big><small>ybrid</small> <big>N</big><small>atural</small> <big>L</big><small>anguage</small> 
		<big>I</big><small>nference</small> <small>system</small>  </font>
	</h1>

	<h4 class="tab" id="premise"><font color="#349aff">Premise: </font> </h4><p class="tab">${premise} </p>
	<h4 class="tab" id="hypothesis"><font color="#349aff">Hypothesis: </font> </h4><p class="tab">${hypothesis} </p> <br>
	<form class="tab" action="index.jsp">
	 <button class="btn btn-primary" type="submit">Home</button>
</form>

	<h3 class="tab" align="left"> <small> Does the final inference label seem right to you?  </small> </h3>
	<form class="tab" method="get" action="gnli"> 
  <input type="radio" name="judge" value="correct" checked> Yes. <br>
  <input type="radio" name="judge" value="bert_correct"> No: correct is the label of the deep learning component.<br>
  <input type="radio" name="judge" value="sym_correct"> No: correct is the label of the symbolic component. <br>
  <input type="radio" name="judge" value="none_correct"> No and neither components give the right label. <br>
   <button class="btn btn-primary" type="submit">Submit</button>
	</form>
			
	<% if(request.getAttribute("error") != null){ %>
			<h2 class="tab" style="color:red">${error}</h2>
		<% } %>	
	
	<h3 class="tab" id="premise"><font color="#349aff">relation: </font> </h3><p class="tab">${relation}</div>
	<h3 class="tab" id="premise"><font color="#349aff">hVeridical: </font> </h3><p class="tab">${hVeridical}</div>
	<h3 class="tab" id="premise"><font color="#349aff">hAveridical: </font> </h3><p class="tab">${hAveridical}</div>
	<h3 class="tab" id="premise"><font color="#349aff">hAntiveridical: </font> </h3><p class="tab">${hAntiveridical}</div>
	<h3 class="tab" id="premise"><font color="#349aff">tVeridical: </font> </h3><p class="tab">${tVeridical}</div>
	<h3 class="tab" id="premise"><font color="#349aff">tAveridical: </font> </h3><p class="tab">${tAveridical}</div>
	<h3 class="tab" id="premise"><font color="#349aff">tAntiveridical: </font> </h3><p class="tab">${tAntiveridical}</div>
	<h3 class="tab" id="premise"><font color="#349aff">equalsRel: </font> </h3><p class="tab">${equalsRel}</div>
	<h3 class="tab" id="premise"><font color="#349aff">superRel: </font> </h3><p class="tab">${superRel}</div>
	<h3 class="tab" id="premise"><font color="#349aff">subRel: </font> </h3><p class="tab">${subRel}</div>
	<h3 class="tab" id="premise"><font color="#349aff">disjointRel:</font> </h3><p class="tab">${disjointRel}</div>
	<h3 class="tab" id="premise"><font color="#349aff">hComplexCtxs: </font> </h3><p class="tab">${hComplexCtxs}</div>
	<h3 class="tab" id="premise"><font color="#349aff">tComplexCtxs: </font> </h3><p class="tab">${tComplexCtxs}</div>
	<h3 class="tab" id="premise"><font color="#349aff">contraFlag: </font> </h3><p class="tab">${contraFlag}</div>
	<h3 class="tab" id="premise"><font color="#349aff">dlDecision: </font> </h3><p class="tab">${dlDecision}</div>
	<h3 class="tab" id="premise"><font color="#349aff">hyDecision: </font> </h3><p class="tab">${hyDecision}</div>
	
	<div id="plainContainer" style="overflow:auto;width:100%;height:70%;padding-left:80px;">   
	   <!-- Creates a container for the graph with a grid wallpaper style="overflow:auto;width:1000;height:200px"  -->
   	 
	
   </div>

    </body>
</html>