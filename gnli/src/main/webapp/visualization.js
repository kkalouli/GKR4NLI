var data = JSON.parse(document.getElementById('jsonFinal').innerHTML);
var premise = document.getElementById('premiseSent').innerHTML;
var hypothesis = document.getElementById('hypothesisSent').innerHTML;
var sentences = [premise, hypothesis];
var classes = ["ENTAILMENT", "CONTRADICTION", "NEUTRAL"];
var classToColor = {"ENTAILMENT": "#9bd3cb", "CONTRADICTION": "#FFABAB", "NEUTRAL": "#b1becd"};
var ruleColor, dlColor;
var featuresY = 205;
var featureWidth = 30;
var featureSize = 10;
var ruleBoundingBox = 50;
var numberOfRules = 8;
var numberOfDLRules = 6;
var roundedCornerDegree = 3;

var paddingForSentences = {
    textX: 70,
    premiseY: 40,
    hypothesisY: 75,
    premiseWidth: 60,
    hypothesisWidth: 80
};

var paddingForVisualization = {
    labelX: 220,
    premiseY: 55,
    hypothesisY: 85,
    ruleVisLineX: 520,
    dlVisLineX: 770,
    dlVisX: 650
};

var paddingForClasses = {barWidth: 150, labelX: 260, classX: -30, classY: 15, classHeight: 30, classYPadding: 20};

// div for the tooltip
var divForTooltip = d3.select("body").append("div")
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
    "Word Heuristics Neutral": "The pair contains at least one word associated with neutral  pairs according to the literature (for now: tall, first, competition, sad, favorite, also, because, popular, many, most). Feel free to test your own heuristics in the fields provided at the top of this demo. "
};

function getData() {
    d3.select("#visualization").select('svg').selectAll("*").remove();
    showClasses();
    showClassificationResults();
    displayVis();
}

getData();

// on mouseeover, show the tooltip
function mouseOver(explanationName, event) {
    divForTooltip.style("opacity", 1);
    divForTooltip.html(mapWithExplanations[explanationName])
        .style("left", (event.pageX) + "px")
        .style("top", (event.pageY - 28) + "px");
}

// on mouseeover, hide the tooltip
function mouseOut() {
    divForTooltip.style("opacity", 0);
}

// the main visualization with rules and dl components
function displayVis() {
    var svg = d3.select("#visualization").select('svg');

    /*************************************sentences****************************************/
    svg.append("text").text("Premise: ").attr("x", paddingForSentences.textX).attr("y", paddingForSentences.premiseY);
    svg.append("text").text("Hypothesis: ").attr("x", paddingForSentences.textX).attr("y", paddingForSentences.hypothesisY);

    svg.append("text").text(sentences[0]).attr("x", paddingForSentences.textX + paddingForSentences.premiseWidth).attr("y", paddingForSentences.premiseY).attr("class", "textItalic");
    svg.append("text").text(sentences[1]).attr("x", paddingForSentences.textX + paddingForSentences.hypothesisWidth).attr("y", paddingForSentences.hypothesisY).attr("class", "textItalic");
    /********************************************************************************************/

    /*************************************visualization****************************************/
    // labels
    svg.append("text").text("Premise").attr("x", paddingForVisualization.labelX).style("text-anchor", "end").attr("y", featuresY + paddingForVisualization.premiseY).attr("class", "textItalic");
    svg.append("text").text("Hypothesis").attr("x", paddingForVisualization.labelX).style("text-anchor", "end").attr("y", featuresY + paddingForVisualization.hypothesisY).attr("class", "textItalic");
    // connecting line for rule vis to classification results
    svg.append("line").attr("x1", paddingForVisualization.ruleVisLineX).attr("x2", paddingForVisualization.ruleVisLineX).attr("y1", featuresY + 90).attr("y2", featuresY + 2 * ruleBoundingBox + 30).style("stroke", "black");
    // connecting line for DL vis to classification results
    svg.append("line").attr("x1", paddingForVisualization.dlVisLineX).attr("x2", paddingForVisualization.dlVisLineX).attr("y1", featuresY + 90).attr("y2", featuresY + 2 * ruleBoundingBox + 30).style("stroke", function (d) {
        var color = "none";
        if (data.rulesDL.length > 0) {
            color = "black";
        }
        return color;
    });

    // rule vis contains 8 rules
    svg.append("rect").attr("width", ruleBoundingBox * numberOfRules - 10).attr("height", ruleBoundingBox + 10).attr("x", paddingForVisualization.labelX + 30).attr("y", featuresY + 35).attr("class", "visBox").attr("rx", roundedCornerDegree)
        .on("mouseover", function () {
            mouseOver("FEATURE RECTANGLE EXPLANATION", d3.event)
        })
        .on("mouseout", mouseOut);
    // DL vis contains 6 rules
    svg.append("rect").attr("width", ruleBoundingBox * numberOfDLRules - 10).attr("height", ruleBoundingBox + 10).attr("x", paddingForVisualization.dlVisX).attr("y", featuresY + 35).attr("class", "visBox").attr("rx", roundedCornerDegree)
        .on("mouseover", function () {
            mouseOver("FEATURE RECTANGLE EXPLANATION", d3.event)
        })
        .on("mouseout", mouseOut);

    //rules
    var g = svg.selectAll(".feature")
        .data(data.features)
        .enter()
        .append("g")
        .attr("transform", function (d, i) {
            return "translate(" + (300 + i * ruleBoundingBox) + "," + ruleBoundingBox + ")"
        })
        .on("mouseover", function (d) {
            return mouseOver(d.name, d3.event);
        })
        .on("mouseout", mouseOut);

    //attributes
    g.each(function (d) {
        showAttributes(d3.select(this), d.attributes);
    });

    // show feature names
    g.append("text")
        .text(function (d) {
            return d.name
        })
        .attr("dx", -180)
        .attr("dy", 50)
        .attr("transform", "rotate(-65)")
        .attr("text-anchor", "start");
    /**************************************************************************************/
}

// returns color according to the model's type
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

// chow labels for different models
function showClasses() {
    var modelData = [{name: "Symbolic", x: -130}, {name: "Hybrid", x: 10}, {name: "Deep Learning", x: 130}];
    var svg = d3.select("#visualization").select('svg').append("g").attr("id", "resultG").attr("transform", "translate(" + 600 + "," + 250 + ")");
    var paddingTop = 90;
    // labels for the three models
    svg.selectAll(".modelLabel")
        .data(modelData)
        .enter()
        .append("text")
        .text(function (d) {
            return d.name;
        })
        .attr("x", function (d) {
            return d.x;
        })
        .attr("y", paddingForClasses.labelX)
        .attr("class", "modelLabel");

    // classification results
    var g = svg.selectAll(".result").data(classes).enter().append("g").attr("transform", function (d, i) {
        return "translate(" + paddingForClasses.classX + "," + (i * (paddingForClasses.classHeight + paddingForClasses.classYPadding) + paddingTop) + ")"
    });

    g.append("rect").attr("width", paddingForClasses.barWidth).attr("height", paddingForClasses.classHeight).style("fill", function (d) {
        return classToColor[d];
    }).style("rx", roundedCornerDegree)
        .on({
            "mouseover": function () {
                d3.select(this).style("cursor", "pointer");
                mouseOver("CLASS EXPLANATION", d3.event)
            },
            "mouseout": function () {
                d3.select(this).style("cursor", "default");
                mouseOut();
            },
            "click": function () {
                svg.append("text").text("Thanks for your feedback!")
                    .attr("x", paddingForClasses.classX).attr("y", paddingForClasses.labelX + 40).style("font-size", 28).style('fill', '#9bd3cb');
            }
        });

    // line connecting rule based model to the classification label
    g.append("line").attr("x1", paddingForClasses.classX + 5).attr("x2", 0).attr("y1", paddingForClasses.classY).attr("y2", paddingForClasses.classY).style("stroke", function (d) {
        var color = "none";
        if (data.match === "R" || data.match === "B") {
            color = getColor(color, "rule", d);
        }
        return color;
    });

    // line connecting DL model to the classification label
    g.append("line").attr("x1", paddingForClasses.barWidth).attr("x2", paddingForClasses.barWidth + 25).attr("y1", paddingForClasses.classY)
        .attr("y2", paddingForClasses.classY).style("stroke", function (d) {
        var color = "none";
        if (data.match === "DL" || data.match === "B") {
            color = getColor(color, "bert", d);
        }
        return color;
    });

    // classification result text label
    g.append("text").text(function (d) {
        return d;
    }).attr("y", 20).attr("x", paddingForClasses.barWidth / 2)
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

// results are visualized in a "traffic light" representation
function showClassificationResults() {
    var svg = d3.select("#visualization").select('#resultG');
    var trafficLightBoxes = [{x: -105, y: 85, width: 50, height: 140}, {x: 145, y: 85, width: 50, height: 140}];

    var g = svg.selectAll(".result").data(classes).enter().append("g").attr("transform", function (d, i) {
        return "translate(" + -80 + "," + (105 + i * 50) + ")"
    });

    g.append("circle").attr("r", featureSize).attr("class", "visBox")
        .style("fill", function (d) {
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

    g.append("circle").attr("r", featureSize).attr("cx", 250).attr("class", "visBox").style("fill", function (d) {
        var color = "white";
        data.decisions.forEach(function (value) {
            if (value["bert"] != undefined) {
                if (value["bert"] === d) {
                    color = classToColor[d];
                    dlColor = color;
                }
            }
        });
        return color;
    });


    // rectangles for "traffic light" vis
    svg.selectAll(".trafficLightBoxes")
        .data(trafficLightBoxes)
        .enter()
        .append("rect")
        .attr("x", function (d) {
            return d.x;
        })
        .attr("y", function (d) {
            return d.y;
        })
        .attr("width", function (d) {
            return d.width;
        })
        .attr("height", function (d) {
            return d.height;
        })
        .attr("class", "visBox")
        .style("fill-opacity", 0)
        .attr("rx", roundedCornerDegree)
        .on("mouseover", function () {
            return mouseOver("CLASS RECTANGLE EXPLANATION", d3.event)
        })
        .on("mouseout", mouseOut);
}

// attributes are displayed as circles
function showAttributes(obj, attributes) {
    //show values for the attributes
    obj.selectAll("attribute")
        .data(attributes)
        .enter()
        .append("circle")
        .attr("r", 8)
        .attr("cy", function (d, i) {
            return i * featureWidth + featuresY;
        })
        .attr("cx", -featureWidth)
        .style("stroke", "black")
        .style("fill", function (d) {
            var color = "none";
            var rulesdl = [];
            data.rulesDL.forEach(function (rule) {
                rulesdl.push(rule.id);
            });
            if (data.rulesSymbolic.indexOf(d.id) > -1) {
                color = ruleColor;
            } else if (rulesdl.indexOf(d.id) > -1) {
                color = dlColor;
            }
            return color;
        })
        .style("opacity", function (d) {
            var opacity = 1;
            data.rulesDL.forEach(function (rule) {
                if (d.id === rule.id) {
                    if (!d.value) {
                        opacity = 0.2;
                    }
                }
            });
            return opacity;
        });

    // if rules is learned, add checkbox
    obj.selectAll("attribute")
        .data(attributes)
        .enter()
        .append("image")
        .attr("xlink:href", function (d) {
            if (d.value)
                return "tick.svg";
        }).attr("y", function (d, i) {
        return i * featureWidth + featuresY - 5
    }).attr("x", -featureWidth - 5)
        .attr("width", featureSize).attr("height", featureSize);

    // show the probability/importance of the rule
    obj.selectAll("attribute")
        .data(attributes)
        .enter().append("text")
        .text(function (d) {
            var t = "";
            data.rulesHybrid.forEach(function (value) {
                if (value === d.id) {
                    t = "H";
                }
            });
            return t;
        })
        .style("opacity", function (d) {
            var o = 1;
            data.rulesHybrid.forEach(function (value, c) {
                if (value === d.id) {
                    o = 1 - 0.1 * c;
                }
            });
            return o;
        })
        .attr("font-size", 10)
        .attr("y", function (d, i) {
            return i * featureWidth + featuresY - 5
        })
        .attr("x", -featureWidth + 6);
}