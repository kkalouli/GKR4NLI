<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
 <title>XplaiNLI</title>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet" />
 <link rel="stylesheet" href="main.css">
<style type="text/css">
#loader {
display: none;
display: none;
position: fixed;
top: 0;
left: 0;
right: 0;
bottom: 0;
width: 100%;
background: rgba(0,0,0,0.75) url(images_default/loader1.gif) no-repeat center center;
z-index: 10000;
}



 .tab { margin-left: 30px; }
 
 .submitbox {
    width: 300px;
}
 
</style>


  <script src="https://code.jquery.com/jquery-3.3.1.slim.min.js" integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo" crossorigin="anonymous"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js" integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1" crossorigin="anonymous"></script>
  <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js" integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM" crossorigin="anonymous"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/d3/3.5.17/d3.min.js"></script>

</head>

<body> <!-- style="background-color:#ffe4b2"> -->

<div id="mainContainer">
<header style="margin-left: 30px; margin-top: 30px;"><h1>XplaiNLI: eXplainable Natural Language Inference</h1></header>
<hr>

<div class="alert alert-success tab" role="alert" style="width: 96%;">
  <p>XplainNLI is an interactive, user-friendly, visualization interface for NLI. It computes inference with
                                           		a deep learning (DL), a symbolic and a hybrid approach and attempts to explain which features lead to the decision of
                                           		each component. The user can define their own heuristics as potential explanations for the decisions and also annotate
                                           		the pair with the correct label. More details in our papers (see below). </p>
</div>

<div id="code" >
	<h3 class="tab">Download</h3> 
	<p class="tab"> The source code of XplaiNLI is publicly available on <a href="https://github.com/kkalouli/XplaiNLI"> github </a>. </p>
</div>

   
<div id="loader"></div>
	
<div id="input">
<h3 class="tab" id="online">Demo</h3>
	<p class="tab instruction">Enter a sentence pair below:<br>
	<form id='submitForm' action='xplainli' method='POST' target='_self' class='tab'>
        <label for='premise' >Premise: </label><br/>
        <input type='text' name='premise' id='premise' style="width: 500px;" /><br/>
        <label for='hypothesis' >Hypothesis:</label><br/>
        <input type='text' name='hypothesis' id='hypothesis' style="width: 500px;"/><br/>
        <!--button class="btn btn-primary mb1" type="submit">Submit</button-->
  <!--     </form> -->
<br>
<p><b>Specify heuristic keywords (optional): </b> <span class="glyphicon glyphicon-info-sign" title="You can define your own heuristics here. Input the words in the corresponding field, depending on whether they should appear in the premise or hypothesis and depending on the label that they are expected to deliver (E, C, N). You can input more than one words separated by semicolon. 
At the moment, we are using 
- for C: (N/n)ot, (n/N)ever, (N/n)obody, (N/n)othing, n't, (n/N)o, sleeping, tv, cat, any
- for E: outdoors, instrument, outside, animal, (S/s)ome, (S/s)omething, (S/s)ometimes, (V/v)arious
- for N: tall, first, competition, sad, favorite, also, because, popular, (M/m)any, (M/m)ost"></span><br>
    <form id='submitForm' action='xplainli' method='POST' target='_self' class='tab'>
        <label for='premise' >Premise: </label><br>
        <label class="label-small">Entailment: </label><input type='text' name='premiseEntailment' id='premiseEntailment' style="width: 100px; margin-left: 2px;" />
        <label class="label-small">Contradiction: </label><input type='text' name='premiseContradiction' id='premiseContradiction' style="width: 100px; margin-left: 2px;" />
        <label class="label-small">Neutral: </label><input type='text' name='premiseNeutral' id='premiseNeutral' style="width: 100px; margin-left: 2px;" />
        <br>
        <label for='hypothesis' >Hypothesis:</label><br>
        <label class="label-small">Entailment: </label><input type='text' name='hypothesisEntailment' id='hypothesisEntailment' style="width: 100px; margin-left: 2px;" />
        <label class="label-small">Contradiction: </label><input type='text' name='hypothesisContradiction' id='hypothesisContradiction' style="width: 100px; margin-left: 2px;" />
        <label class="label-small">Neutral: </label><input type='text' name='hypothesisNeutral' id='hypothesisNeutral' style="width: 100px; margin-left: 2px;" />
        <br>
        <button class="btn btn-primary mb1" type="submit">Submit</button>
    </form>
<br>
<p class="tab instruction">You can also choose from the given examples:<br>
    <form class="tab" method="post" action="xplainli">
        <input type="radio" name="id" value="-1" checked> P: The dog is walking. H: The animal is walking. <br>
        <input type="radio" name="id" value="-2"> P: The judge advised the doctor. H: The doctor advised the judge.<br>
        <input type="radio" name="id" value="-3"> P: John forgot to close the window. H: John closed the window. <br>
        <input type="radio" name="id" value="-4"> P: No woman is walking. H: A woman is walking. <br>
        <input type="radio" name="id" value="-5"> P: Mary believes that John is handsome. H: John is handsome.<br>
         <br>
        <button class="btn btn-primary mb1" type="submit">Submit</button>
    </form>
</div>
	
<br><br>
<div id="jsonFinal" style="display: none;">${jsonFinal}</div> 
<div id="premiseSent" style="display: none;">${premise}</div> 
<div id="hypothesisSent" style="display: none;">${hypothesis}</div> 
<div id="visualization" >
	<h3 class="tab">Explanation</h3> 
	<p class="tab"> After exploring the visualization, click on the inference label that you think is correct for this pair. Thanks for your feedback!</p>
	<p class="tab"> WARNING: The visualization has only been tested on Safari, Firefox and Chrome. </p> <br>
    <svg style="width: 100%; height: 100%;"></svg>
</div>


<br><br><br>

<div id="additionalInfo">
	<h3 class="tab" id="publications">Publications</h3>
    	<ul>
    	    <li> Explainable Natural Language Inferencethrough Visual Analytics. Kalouli,
    			A.-L., R. Sevastjanova, R. Crouch, V. de Paiva and M. El-Assady. 2020. In Proceedings of the COLING 2020 System Demonstrations (link coming soon).
    		<li> Hy-NLI: a Hybrid system for Natural Language Inference. Kalouli,
    			A.-L., R. Crouch and V. de Paiva. 2020. In Proceedings of COLING 2020 (link coming soon).
    		<li> <a href="https://ieeexplore.ieee.org/document/8807299/">  explAIner:  A Visual Analytics  Framework  for  Interactive  and  Explainable Machine Learning.</a>
			Spinner T.,  U.  Schlegel,  H.  Schaefer,  and M. El-Assady. 2020. IEEE Transactions on Visualization and Computer Graphics.
    		<li> <a href="http://aclweb.org/anthology/W19-3305/">GKR: Bridging the gap between symbolic/structural and distributional meaning representations</a> Kalouli,
    			A.-L., R. Crouch and V. de Paiva. 2019, 1st International Workshop on Designing Meaning Representations (DMR) @ACL 2019.
    		<li> <a href="https://www.aclweb.org/anthology/P19-3003/">lingvis.io - A Linguistic Visual Analytics Framework</a> 
    		  El-Assady M., W. Jentner, F. Sperrle, R. Sevastjanova,  A. Hautli-Janisz, M. Butt and D. Keim. 2019. ACL 2019 System Demo. 		
    		<li><a href="http://aclweb.org/anthology/W18-1304"> GKR: the
    				Graphical Knowledge Representation for semantic parsing</a> Kalouli,
    			A.-L. and R. Crouch. 2018. SEMBEaR @NAACL 2018.
    		<li><a href="http://aclweb.org/anthology/S18-2013"> Named
    				Graphs for Semantic Representations </a> Crouch, R. and A.-L. Kalouli.
    			2018. *SEM 2018.
    		<li><a href="https://easychair.org/publications/preprint/rXqs">
    				Graph Knowledge Representations for SICK </a> Kalouli, A.-L., R.
    			Crouch, V. de Paiva and L. Real. 2018. 5th Workshop on Natural
    			Language and Computer Science @FLoC 2018
    	</ul>
    	
  

     <h3 class="tab" id="contact">Contact</h3>
    	<ul>
    		<li>aikaterini (dash) lida (dot) kalouli (at) uni (dash) konstanz(dot) de
    		<li>dick (dot) crouch (at) gmail (dot)com
    		<li>valeria (dot) depaiva (at) gmail (dot) com
    		<li>rita (dot) sevastjanova (at) uni (dash) konstanz(dot) de
    		<li>mennatallah (dot) el-assady (at) uni (dash) konstanz(dot) de
    	</ul>
    	<br>
    	<br>
</div>
</div>

  <script src="visualization.js">  </script>
  <script src="http://code.jquery.com/jquery.js"></script>
  <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
<script>
var spinner = $('#loader');
$(function() {
  $('form').submit(function(e) {
    spinner.show();
  });
});
</script>
  
</body>
</html>

