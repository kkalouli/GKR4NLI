<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
	

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet" />
<link href="css/custom.css" rel="stylesheet" />
<style type="text/css">
#loader {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  background: rgba(0,0,0,0.75) url(images/bunny_loop.gif) no-repeat center center;
  z-index: 10000;
}

 .tab { margin-left: 30px; }
 
</style>
</head>

<body style="background-color:#ffe4b2">

   
<div id="loader"></div>
	<h1 class="tab" id="top" >
		<font color="#349aff"><big>G</big><small>raphical</small> <big>K</big><small>nowledge</small>
		<big>R</big><small>epresentations</small> <small>for</small>  <big>N</big><small>atural</small>
		<big>L</big><small>anguage</small> <big>I</big><small>nference</small> </font>
	</h1>

	<p class="tab">
		This is the Graphical Knowledge Representation (GKR) parser. It
		transforms a given sentence into a layered semantic graph. The
		semantic graph consists of (currently) 6 subgraphs: dependency graph,
		concept graph, context graph, lexical graph, properties graph and
		coreference graph. Each of those encodes different kinds of
		information present in the sentence. This makes the GKR representation
		flexible enough to be expanded with additional subgraphs as necessary,
		and modular to be used in different NLP applications. The
		representation is especially targeted towards Natural Language
		Inference (NLI) but also simpler semantic similarity tasks. <br>
		This demo seeks to give the chance to interested researchers to have a
		go on our system. The system is still under implementation and we are
		thankful for any comments or discussions. <br> We are currently
		also implementing a hybrid rule-based and machine-learning NLI system
		based on GKR, which you can also try at its initial experimental
		version.
	</p>

	</p>
	

	<h2 class="tab" id="download"><font color="#349aff">Download</font></h2>
	<p class="tab">
		The source code of GKR is publicly available on <a
			href="https://github.com/kkalouli/GKR_semantic_parser"> github</a>.
	</p>

	<br>

	<h2 class="tab" id="online"><font color="#349aff">Online Demo for GKR</font></h2>
	<p class="tab">
		Enter a sentence below to try our GKR parser online: (please use punctuation marks)<br>
		<form class="tab" method="post" action="gkr">  
  <input type="text" id="sentence-input" name="sentence" />
   <!-- <input type="submit" id="process-button" value="Process" /> -->
     <button class="btn btn-primary mb1 bg-blue" type="submit">Submit</button>
</form>


	<br>


	<h3 class="tab"><font color="#349aff">Examples</font></h3>
	<form class="tab" method="post" action="gkr"> 
  <input type="radio" name="id" value="-1" checked> The boy faked the illness. <br>
  <input type="radio" name="id" value="-2"> Negotiations prevented the strike.<br>
  <input type="radio" name="id" value="-3"> The dog is not eating the food. <br>
  <input type="radio" name="id" value="-4"> The boy walked or drove to school..<br>
  <input type="radio" name="id" value="-5"> No woman is walking.<br>
  <input type="radio" name="id" value="-6"> Max forgot to close the door.<br>
  <button class="btn btn-primary" type="submit">Submit</button>
	</form>
	<!-- 
	<ul>
		<li id="-1"> The boy faked the illness. 
		<li id="-2"> Negotiations prevented the strike.
		<li id="-3"> The dog is not eating the food.
		<li id="-4"> The boy walked or drove to school.
		<li id="-5" > No woman is walking.
		<li id="-6"> Max forgot to close the door.
		
	</ul>
	-->
	</p>
	
	<br>

	<h2 class="tab" id="publications"><font color="#349aff">Publications</font></h2>
	<ul>
		<li><a href="http://aclweb.org/anthology/W18-1304"> GKR: the
				Graphical Knowledge Representation for semantic parsing</a> Kalouli,
			A.-L. and Richard Crouch. 2018. SEMBEaR @NAACL 2018.
		<li><a href="http://aclweb.org/anthology/S18-2013"> Named
				Graphs for Semantic Representations </a> Crouch, R. and A.-L. Kalouli.
			2018. *SEM 2018.
		<li><a href="https://easychair.org/publications/preprint/rXqs">
				Graph Knowledge Representations for SICK </a> Kalouli, A.-L., Richard
			Crouch, Valeria de Paiva and Livy Real. 2018. 5th Workshop on Natural
			Language and Computer Science @FLoC 2018
	</ul>


	<br>

	<h2 class="tab" id="contact"><font color="#349aff">Contact</font></h2>
	<ul>
		<li>aikaterini (dash) lida (dot) kalouli (at) uni (dash) konstanz
			(dot) de
		<li>dick (dot) crouch (at) gmail (dot)com
	</ul>
	
		<br>
	<br>
	
	<p align="center"> Copyright 2018 Aikaterini-Lida Kalouli and Richard Crouch </p>
	

<script src="http://code.jquery.com/jquery.js"></script>
 
<!-- Latest compiled and minified JavaScript  -->
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
<script>
var spinner = $('#loader');
$(function() {
  $('form').submit(function(e) {
   <!-- e.preventDefault(); -->
    spinner.show();
    $.ajax({
      url: '/GKRServlet',
      <!--data: "Mary love John.", -->
      method: 'post'
      <!-- dataType: 'JSON' -->
    }).done(function(resp) {
      spinner.hide();
      <!-- alert(resp.status); -->
    });
  });
});
</script>


</body>
</html>











