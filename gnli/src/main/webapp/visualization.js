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

    svg.append("rect").attr("width", featureBoundingBox * 8 - 10).attr("height", featureBoundingBox + 10).attr("x", 250).attr("y", featuresY + 35).style("fill", "white").style("stroke", "black").attr("rx", 3);
    svg.append("rect").attr("width", featureBoundingBox * 6 - 10).attr("height", featureBoundingBox + 10).attr("x", 650).attr("y", featuresY + 35).style("fill", "white").style("stroke", "black").attr("rx", 3);

    // show features
    var g = svg.selectAll(".feature")
        .data(data.features)
        .enter()
        .append("g")
        .attr("transform", function (d, i) {
            showAttributes(d3.select(this), d.attributes);
            return "translate(" + (300 + i * featureBoundingBox) + "," + featureBoundingBox + ")"
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
        });

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
    
    

    svg.append("rect").attr("width", 50).attr("height", 140).attr("x", -105).attr("y", 85).style("fill", "none").style("stroke", "black").attr("rx", 3);
    svg.append("rect").attr("width", 50).attr("height", 140).attr("x", 145).attr("y", 85).style("fill", "none").style("stroke", "black").attr("rx", 3);
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


