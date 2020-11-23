var data=JSON.parse(document.getElementById('jsonFinal').innerHTML); 
var premise = document.getElementById('premiseSent').innerHTML;
var hypothesis = document.getElementById('hypothesisSent').innerHTML;
console.log(data);
var sentences = [premise, hypothesis];
var classes = ["ENTAILMENT", "CONTRADICTION", "NEUTRAL"];
var classToColor = {"ENTAILMENT": "#9bd3cb", "CONTRADICTION": "#FFABAB", "NEUTRAL": "#b1becd"};
var ruleColor, bertColor;
var featuresY = 205;
var featureWidth = 30;
var featureSize = 10;
var featureBoundingBox = 50;

// Define the div for the tooltip
var div = d3.select("body").append("div")
    .attr("class", "tooltip")
    .style("opacity", 0);

var mapWithExplanations = {
    "FEATURE RECTANGLE EXPLANATION": "The explainable features used by each of the components: on the left side are the features of the symbolic component and on the right side, the possible features of the deep-learning component.  The features that are relevant for this pair are colored and contain a checkmark, if the feature's value is true or do not contain  a checkmark, if the value is false. The color of the features encodes the inference relation that each approach predicted: green is for E, red for C and grey for N. DL features with lower opacity mean that these features should --according to the litearure -- lead to a different inference label than the one actually predicted by the model. Symbolic features marked with an 'H' were used for the decision of the hybrid engine (the darker the grey, the more weight this feature had for the decision)", // those are the rectangles with the feature circles on top
    "CLASS RECTANGLE EXPLANATION": "The colored features above are linked here with the corresponding inference label which is again encoded by color.", // those are the rectangles with the three circles aligned vertically
    "CLASS EXPLANATION": "The bold label is the final label chosen by the hybrid system. All three labels are clickable buttons to provide your annotation of the pair. Thanks for the feedback!", //those are the class labels (buttons)
    "VERIDICAL Context": "Some concept of the GKR concept graph of the sentence is instantiated in the world of the speaker, i.e., the concept exists in this world (e.g., The dog is eating: eating takes places in this world).",
    "ANTIVERIDICAL Context": "Some concept of the GKR concept graph of the sentence is not instantiated in the top, actual world, i.e., the concept does not exist in this world (e.g., The dog is not eating: eating does not take place in this world).  ",
    "AVERIDICAL Context": "Some concept of the GKR concept graph of the sentence might be instantiated in the top, actual world, i.e., the concept might exist in this world (e.g., The dog might be eating: eating might take place or not in this world). ",
    "EQUALS Match": "At least one of the matched terms has the specificity 'equals'.",
    "SUPERCLASS Match": "At least one of the matched terms has the specificity 'superclass'.",
    "SUBCLASS Match": "At least one of the matched terms has the specificity 'subclass'.",
    "DISJOINT Match": "At least one of the matched terms has the specificity 'disjoint'.",
    "CONTRADICTION Flag": "The semantic roles of the matched terms are mostly found in contradictions.",
    "Negation": "The sentence contains a negation of some sort (for now: not, no, never, nothing, nobody, n't)",
    "Lexical Overlap": "P and H contain the same words (max. difference of 1 word).",
    "Length Mismatch": "H is at least three words longer than P.",
    "Word Heuristics Entailment": "The pair contains at least one word associated with entailments according to the literature (for now: outdoors, instrument, outside, animal, something, sometimes, some, various). Feel free to test your own heuristics in the fields provided at the top of this demo. ",
    "Word Heuristics Contradiction": "The pair contains at least one word associated with contradictions according to the literature (for now: sleeping, tv, cat, any). Feel free to test your own heuristics in the fields provided at the top of this demo.  ",
    "Word Heuristics Neutral": "The pair contains at least one word associated with neutral  pairs according to the literature (for now: tall, first, competition, sad, favorite, also, because, popular, many, most). Feel free to test your own heuristics in the fields provided at the top of this demo. "};

function getData() {
    d3.select("#visualization").select('svg').selectAll("*").remove();
    showClasses();
    showClassificationResults();
    showVis();
}

getData();

function showVis() {

    var svg = d3.select("#visualization").select('svg');

    //show sentences
    svg.append("text").text("Premise: ").attr("x", 70).attr("y", 40);
    svg.append("text").text("Hypothesis: ").attr("x", 70).attr("y", 75);

    svg.append("text").text(sentences[0]).attr("x", 130).attr("y", 40).style("font-style", "italic");
    svg.append("text").text(sentences[1]).attr("x", 150).attr("y", 75).style("font-style", "italic");

    
    svg.append("text").text("Premise").attr("x", 220).style("text-anchor", "end").attr("y", featuresY + 55).style("font-style", "italic");
    svg.append("text").text("Hypothesis").attr("x", 220).style("text-anchor", "end").attr("y", featuresY + 85).style("font-style", "italic");

    
    svg.append("line").attr("x1", 520).attr("x2", 520).attr("y1", featuresY + 90).attr("y2", featuresY + 2 * featureBoundingBox + 30).style("stroke", "black");

    svg.append("line").attr("x1", 770).attr("x2", 770).attr("y1", featuresY + 90).attr("y2", featuresY + 2 * featureBoundingBox + 30).style("stroke", function (d) {
        var color = "none";
        if (data.rulesDL.length > 0) {
            color = "black";
        }
        return color;
    });

    svg.append("rect").attr("width", featureBoundingBox * 8 - 10).attr("height", featureBoundingBox + 10).attr("x", 250).attr("y", featuresY + 35).style("fill", "white").style("stroke", "black").attr("rx", 3)
        .on("mouseover", function(d) {
            div.style("opacity", .9);
            div.html(mapWithExplanations["FEATURE RECTANGLE EXPLANATION"])
                .style("left", (d3.event.pageX) + "px")
                .style("top", (d3.event.pageY - 28) + "px");
        })
        .on("mouseout", function(d) {
            div.style("opacity", 0);
        })
    svg.append("rect").attr("width", featureBoundingBox * 6 - 10).attr("height", featureBoundingBox + 10).attr("x", 650).attr("y", featuresY + 35).style("fill", "white").style("stroke", "black").attr("rx", 3)
        .on("mouseover", function(d) {
            div.style("opacity", .9);
            div.html(mapWithExplanations["FEATURE RECTANGLE EXPLANATION"])
                .style("left", (d3.event.pageX) + "px")
                .style("top", (d3.event.pageY - 28) + "px");
        })
        .on("mouseout", function(d) {
            div.style("opacity", 0);
        })

    // show features
    var g = svg.selectAll(".feature")
        .data(data.features)
        .enter()
        .append("g")
        .attr("transform", function (d, i) {
            showAttributes(d3.select(this), d.attributes);
            return "translate(" + (300 + i * featureBoundingBox) + "," + featureBoundingBox + ")"
        })
        .on("mouseover", function(d) {
            div.style("opacity", .9);
            div.html(mapWithExplanations[d.name])
                .style("left", (d3.event.pageX) + "px")
                .style("top", (d3.event.pageY - 28) + "px");
        })
        .on("mouseout", function(d) {
            div.style("opacity", 0);
        })

    // show feature names
    g.append("text")
        .text(function (d) {
            return d.name
        })
        .attr("dx", -180)
        .attr("dy", 50)
        .attr("transform", "rotate(-65)")
        .attr("text-anchor", "start");
}

function getColor(color, model, d) {
    data.decisions.forEach(function (value) {
        if (value[model] != undefined) {
            if (value[model] === d) {
                color = "black";
            }
        }
    });
    return color;
}

function showClasses() {
    var svg = d3.select("#visualization").select('svg').append("g").attr("id", "resultG").attr("transform", "translate(" + 600 + "," + 250 + ")");
    var barWidth = 150;
    var labelX = 220;
    var labelFontSize = 24;
    svg.append("text").text("Symbolic").attr("x", -130).attr("y", labelX+40).style("font-size", labelFontSize);
    svg.append("text").text("Hybrid").attr("x", 10).attr("y", labelX+40).style("font-size", labelFontSize);
    svg.append("text").text("Deep Learning").attr("x", 130).attr("y", labelX+40).style("font-size", labelFontSize);

    var g = svg.selectAll(".result").data(classes).enter().append("g").attr("transform", function (d, i) {
        return "translate(" + -30 + "," + (i * 50 + 90) + ")"
    });

    g.append("rect").attr("width", barWidth).attr("height", 30).style("fill", function (d) {
        return classToColor[d];
    }).style("rx", 3).on({
        "mouseover": function(d) {
            d3.select(this).style("cursor", "pointer"); 
          },
          "mouseout": function(d) {
            d3.select(this).style("cursor", "default"); 
          },
          "click": function(d) {
        	  svg.append("text").text("Thanks for your feedback!").attr("x", -30).attr("y", labelX+80).style("font-size", 28).style('fill', '#9bd3cb');
          }
        }).on("mouseover", function(d) {
        div.style("opacity", .9);
        div.html(mapWithExplanations["CLASS EXPLANATION"])
            .style("left", (d3.event.pageX) + "px")
            .style("top", (d3.event.pageY - 28) + "px");
    })
        .on("mouseout", function(d) {
            div.style("opacity", 0);
        })

    g.append("line").attr("x1", -25).attr("x2", 0).attr("y1", 15).attr("y2", 15).style("stroke", function (d) {
        var color = "none";
        if (data.match === "R" || data.match === "B") {
            color = getColor(color, "rule", d);
        }
        return color;
    });
    
    g.append("line").attr("x1", barWidth).attr("x2", barWidth + 25).attr("y1", 15).attr("y2", 15).style("stroke", function (d) {
        var color = "none";
        if (data.match === "DL" || data.match === "B") {
            color = getColor(color, "bert", d);
        }
        return color;
    });

    g.append("text").text(function (d) {
        return d;
    }).attr("y", 20).attr("x", function (d) {
        return barWidth/2;
    })
    .style("text-anchor", "middle")
    .style("fill", "white").style("font-weight", function (d) {
        var weight = "normal";
        data.decisions.forEach(function (value) {
            if (value["hybrid"] != undefined) {
                if (value["hybrid"].trim() == d) {
                    weight = "bold";
                }
            }
        });
        return weight;
    });
}


function showClassificationResults() {
    var svg = d3.select("#visualization").select('#resultG');

    var g = svg.selectAll(".result").data(classes).enter().append("g").attr("transform", function (d, i) {
        return "translate(" + -80 + "," + (105 + i * 50) + ")"
    });

    g.append("circle").attr("r", featureSize).style("stroke", function (d) {
        return "black";
    }).style("fill", function (d) {
        var color = "white";
        data.decisions.forEach(function (value) {
            if (value["rule"] != undefined) {
                if (value["rule"] === d) {
                    color = classToColor[d];
                    ruleColor = color;
                }
            }
        });
        return color;
    });

    g.append("circle").attr("r", featureSize).attr("cx", 250).style("stroke", function (d) {
        return "black";
    }).style("fill", function (d) {
        var color = "white";
        data.decisions.forEach(function (value) {
            if (value["bert"] != undefined) {
                if (value["bert"] === d) {
                    color = classToColor[d];
                    bertColor = color;
                }
            }
        });
        return color;
    });
    
    

    svg.append("rect").attr("width", 50).attr("height", 140).attr("x", -105).attr("y", 85).style("fill", "white").style("fill-opacity", 0).style("stroke", "black").attr("rx", 3)
        .on("mouseover", function(d) {
            div.style("opacity", .9);
            div.html(mapWithExplanations["CLASS RECTANGLE EXPLANATION"])
                .style("left", (d3.event.pageX) + "px")
                .style("top", (d3.event.pageY - 28) + "px");
        })
        .on("mouseout", function(d) {
            div.style("opacity", 0);
        })
    svg.append("rect").attr("width", 50).attr("height", 140).attr("x", 145).attr("y", 85).style("fill", "white").style("fill-opacity", 0).style("stroke", "black").attr("rx", 3)
        .on("mouseover", function(d) {
            div.style("opacity", .9);
            div.html(mapWithExplanations["CLASS RECTANGLE EXPLANATION"])
                .style("left", (d3.event.pageX) + "px")
                .style("top", (d3.event.pageY - 28) + "px");
        })
        .on("mouseout", function(d) {
            div.style("opacity", 0);
        })
}

function showAttributes(obj, d) {
    //show values for the attributes
    d.forEach(function (val, i) {
    	console.log(i);
        obj.append("circle")
            .attr("r", 8)
            .attr("cy", i * featureWidth + featuresY)
            .attr("cx", -featureWidth)
            .style("stroke", function (d) {
                return "black";
            }).style("fill", function () {
            var color = "none";
            var rulesdl = [];
            data.rulesDL.forEach(function (value) {
                rulesdl.push(value.id);
            });
            if (data.rulesSymbolic.indexOf(val.id) > -1) {
                color = ruleColor;
            } else if (rulesdl.indexOf(val.id) > -1) {
                color = bertColor;
            }
            //	color = "#b1becd";
            return color;
        }).style("opacity", function () {
            var opacity = 1;
            data.rulesDL.forEach(function (value) {
                if (value.id === val.id) {
                    if (value.value === true) {
                        opacity = 1;
                    } else {
                        opacity = 0.2;
                    }
                }
            });
            /*data.rulesHybrid.forEach(function (value) {
                if (value === val.id) {
                        opacity = 0.1;
                }
            });*/
            return opacity;
        });
        obj.append("image")
            .attr("xlink:href", function () {
                if (val.value === true)
                    return "tick.svg";
            }).attr("y", i * featureWidth + featuresY - 5).attr("x", -featureWidth - 5)
            .attr("width", featureSize).attr("height", featureSize)

        obj.append("text")
            .text(function () {
                var t = "";
                data.rulesHybrid.forEach(function (value) {
                    if (value === val.id) {
                        t = "H";
                    }
                });
                return t;
            })
            .style("opacity", function () {
                var o = 1;
                data.rulesHybrid.forEach(function (value, c) {
                    if (value === val.id) {
                        o= 1 - 0.1*c;
                    }
                });
                return o;
            })
            .attr("font-size", 10)
            .attr("y", i * featureWidth + featuresY - 5)
            .attr("x", -featureWidth + 6);
    })

}


