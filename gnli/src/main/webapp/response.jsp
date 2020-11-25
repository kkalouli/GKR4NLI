<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
 <title>Hy-NLI</title>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet" />
 <link rel="stylesheet" href="main2.css">
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
background: rgba(0,0,0,0.75) url(images_default/loader3.gif) no-repeat center center;
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
<header style="margin-left: 30px; margin-top: 30px;"><h1>Hy-NLI: a Hybrid System for Natural Language Inference</h1></header>
<hr>

<div class="alert alert-success tab" role="alert" style="width: 96%; background-color: #c3d3d8">
  <p style="color:#3a707e">Hy-NLI is a hybrid system for NLI. It computes inference with a deep learning (DL) and a symbolic approach and then its hybrid component
  determines which of the two labels should be trusted for a given pair. For the DL approach, it currently uses the BERT (Devlin et al, 2018) model, which is further fine-tuned on the 
  SICK (Marelli et al, 2014) corpus. The symbolic approach computes inference based on a version of Natural Logic (Valencia, 1991; MacCartney, 2009)
  and on the Graphical Knowledge Representation (Kalouli et Crouch, 2018). The hybrid classifier is an MLP trained model.
  Please find more details in our paper: </p>
  <p style="color:#3a707e"> Kalouli, A.-L., R. Crouch and V. de Paiva. 2020. Hy-NLI: a Hybrid system for Natural Language Inference. In Proceedings of COLING 2020. </p>
  <p style="color:#3a707e"> Note that Hy-NLI targets performance, rather than explainability. If you are interested in the explainability of our system, check out our demo on XplaiNLI: http://bit.ly/XplaiNLI   </p>
</div>

<div id="code" >
	<h3 class="tab">Download</h3> 
	<p class="tab"> The source code of Hy-NLI is publicly available on <a href="https://github.com/kkalouli/Hy-NLI"> github </a>. </p>
</div>

   
<div id="loader"></div>
	
<div id="input">
<h3 class="tab" id="online">Demo</h3>
	<p class="tab instruction">Enter a sentence pair below:<br>
	<form id='submitForm' action='hynli' method='POST' target='_self' class='tab'>
        <label for='premise' >Premise: </label><br/>
        <input type='text' name='premise' id='premise' style="width: 500px;" /><br/>
        <label for='hypothesis' >Hypothesis:</label><br/>
        <input type='text' name='hypothesis' id='hypothesis' style="width: 500px;"/><br/>
        <!--button class="btn btn-primary mb1" type="submit">Submit</button-->
  <!--     </form> -->
  <button class="btn btn-primary mb1" type="submit">Submit</button>
  </form>
<br>
<p class="tab instruction">You can also choose from the given examples:<br>
    <form class="tab" method="post" action="hynli">
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
<p class="tab instruction"> Premise: ${premise}</p> 
<p class="tab instruction"> Hypothesis: ${hypothesis}</p> 
<br> <br>
<h3 style="color:#3a707e; position:relative; left:360px; top:-20px; width:200px; height:200px; border:none;"> GKR4NLI </h3>
<img src="images_default/symb_tap.png" alt="tap_symb" style="position:relative; left:350px; top:-200px; width:130px; height:100px; border:none;">
<% if(request.getAttribute("sym_label").equals("ENTAILMENT")){ %>
		<img src="images_default/entail.png" alt="entail" style="position:relative; left:300px; top:-130px; width:50px; height:50px; border:none;">
		<% } else if (request.getAttribute("sym_label").equals("CONTRADICTION")){ %>
		<img src="images_default/contra.png" alt="contra" style="position:relative; left:300px; top:-130px; width:50px; height:50px; border:none;">
		<% } else if (request.getAttribute("sym_label").equals("NEUTRAL")){ %>
		<img src="images_default/neutral.png" alt="neutral" style="position:relative; left:300px; top:-130px; width:50px; height:50px; border:none;">
				<% } %>
<h3 style="color:#3a707e; position:relative; left:570px; top:-350px; width:200px; height:200px; border:none;"> BERT </h3>
<img src="images_default/dl_tap.png" alt="tap_dl" style="position:relative; left:530px; top:-530px; width:130px; height:100px; border:none;">
<% if(request.getAttribute("dl_label").equals("ENTAILMENT")){ %>
		<img src="images_default/entail.png" alt="entail" style="position:relative; left:395px; top:-460px; width:50px; height:50px; border:none;">
		<% } else if (request.getAttribute("dl_label").equals("CONTRADICTION")){ %>
		<img src="images_default/contra.png" alt="contra" style="position:relative; left:395px; top:-460px; width:50px; height:50px; border:none;">
		<% } else if (request.getAttribute("dl_label").equals("NEUTRAL")){ %>
		<img src="images_default/neutral.png" alt="neutral" style="position:relative; left:395px; top:-460px; width:50px; height:50px; border:none;">
		<% } %>
<img src="images_default/funnel.png" alt="funnel" style="position:relative; left:220px; top:-380px; width:200px; height:100px; border:none;">
<h3 style="color:#3a707e; position:relative; left:460px; top:-490px; width:200px; height:0px; border:none;"> Hy-NLI </h3>
<% if(request.getAttribute("hy_label").equals("ENTAILMENT")){ %>
		<img src="images_default/entail.png" alt="entail" style="position:relative; left:480px; top:-410px; width:50px; height:50px; border:none;">
		<% } else if (request.getAttribute("hy_label").equals("CONTRADICTION")){ %>
		<img src="images_default/contra.png" alt="contra" style="position:relative; left:480px; top:-410px; width:50px; height:50px; border:none;">
		<% } else if (request.getAttribute("hy_label").equals("NEUTRAL")){ %>
		<img src="images_default/neutral.png" alt="neutral" style="position:relative; left:480px; top:-410px; width:50px; height:50px; border:none;">
		<% } %>



<div id="additionalInfo">
	<h3 style="position:relative;top:-350px;" class="tab" id="publications">Publications</h3>
    	<ul style="position:relative;top:-350px;">
    	    <li> Explainable Natural Language Inferencethrough Visual Analytics. Kalouli,
    			A.-L., R. Sevastjanova, R. Crouch, V. de Paiva and M. El-Assady. 2020. In Proceedings of the COLING 2020 System Demonstrations (link coming soon).
    		<li> Hy-NLI: a Hybrid system for Natural Language Inference. Kalouli,
    			A.-L., R. Crouch and V. de Paiva. 2020. In Proceedings of COLING 2020 (link coming soon).
    		<li> <a href="http://aclweb.org/anthology/W19-3305/">GKR: Bridging the gap between symbolic/structural and distributional meaning representations</a> Kalouli,
    			A.-L., R. Crouch and V. de Paiva. 2019, 1st International Workshop on Designing Meaning Representations (DMR) @ACL 2019.
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
    	
  

     <h3 style="position:relative;top:-350px;" class="tab" id="contact">Contact</h3>
    	<ul style="position:relative;top:-350px;">
    		<li>aikaterini (dash) lida (dot) kalouli (at) uni (dash) konstanz(dot) de
    		<li>dick (dot) crouch (at) gmail (dot)com
    		<li>valeria (dot) depaiva (at) gmail (dot) com
    	</ul>
</div>
</div>

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

