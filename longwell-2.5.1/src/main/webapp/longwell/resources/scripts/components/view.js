function loadView(objectURI, profileID, prefix, suffix, properties, fresnel, group) {
        var s = "";
        if (properties) {
                for (var i = 0; i < properties.length; i++) {
                        var pair = properties[i];
                        s = s + pair.name + "=" + pair.value + "\n";
                }
        }
        s += "outerQuery=" + g_outerQuery + "\n";
        
        if (!prefix) {
                prefix = "";
        }
        if (!suffix) {
                suffix = "";
        }

	var engine = "";
        if (fresnel) {
		engine += "&engine=fresnel";
        }

	var groupParam = "";
	if (group && group != "") {
		groupParam += "&group=" + encodeURIComponent(group) + "&";
	}
        HTTPUtilities.doPost(
                g_contextPath + "/" + profileID + "?command=view&objectURI=" + encodeURIComponent(objectURI) + engine + groupParam,
                s,
                function(status, statusText) {
                        alert("Failed to load view for item with URI = " + objectURI);
                },
                function (text) {
                   try {
                        var oldElmt = document.getElementById(prefix + objectURI + suffix);
                        var oldParent = oldElmt.parentNode;
                        
                        var newParent = document.createElement("div");
                        newParent.innerHTML = text;
                        
                        var newElmt = newParent.getElementsByTagName("div")[0];
                        
                        oldParent.replaceChild(newElmt, oldElmt);
                        if (fresnel) {
                            var titleElmt = document.getElementById(prefix + objectURI + "_title");
                            var resultElmt = document.getElementById("fresnel_title_" + objectURI);
                            if (resultElmt && resultElmt.childNodes[0]) {
                                var title = document.createTextNode(resultElmt.childNodes[0].nodeValue);
                                titleElmt.appendChild(title);
                            }
                            var styleElmt = document.getElementById("fresnel_styles_" + objectURI);
                            if (styleElmt) {
                                var styles = styleElmt.getElementsByTagName("span");
                                for (var i = 0; i < styles.length; i++) {
                                    registerStylesheet(g_resourcePath + "/" + styles[i].childNodes[0].nodeValue);
                                }
                            }
                        }
                    } catch (e) { 
                           alert(e); 
                    }
                }
        );
}

function reloadView(objectURI, profileID, prefix, suffix, properties, fresnel, group) {
        if (!properties) {
                properties = [];
        }
        properties.unshift({ name : "showRenderTime" , value : true });
        
        loadView(objectURI, profileID, prefix, suffix, properties, fresnel, group);
}

function expandReferers(objectURI, profileID) {
        var expandDiv = document.getElementById("expand-referers-" + objectURI);
        var collapseDiv = document.getElementById("collapse-referers-" + objectURI);
        
        expandDiv.style.display = "none";
        collapseDiv.style.display = "block";

        HTTPUtilities.doPost(
                g_contextPath + "/" + profileID + "?command=view-referers&objectURI=" + encodeURIComponent(objectURI),
                "outerQuery=" + g_outerQuery + "\n",
                function(status, statusText) {
                        alert("Failed to load view for item with URI = " + objectURI);
                },
                function (text) {
                        var elmt = document.getElementById("referers-" + objectURI);
                        elmt.innerHTML = text;
                }
        );
}

function setFresnelGroup(switcher) {
	var replaceIdx = g_outerQuery.indexOf("&group=");
	var endIdx = g_outerQuery.indexOf("&", replaceIdx + "&group=".length);
        var url = g_profileURL + g_outerQuery.substring(0, replaceIdx)
			+ "&group=" + encodeURIComponent(switcher.value)
			+ g_outerQuery.substring(endIdx);

	browseTo(url);
}

function collapseReferers(objectURI, profileID) {
        var expandDiv = document.getElementById("expand-referers-" + objectURI);
        var collapseDiv = document.getElementById("collapse-referers-" + objectURI);
        
        collapseDiv.style.display = "none";
        expandDiv.style.display = "block";
}

function slide(propertyURI, forward) {
        var prefix = (g_slidingURL.lastIndexOf("&") != -1) ? "-=" : "&-=";
        var url = g_slidingURL + prefix +
                encodeURIComponent(
                        "@lwq.project.PropertyProjector;" + (forward ? "!" : "") + propertyURI + ";" +
                        "@lwq.bucket.NestedQueryBucketer;" + g_slidingQuery);
                        
        browseTo(url);
}

function hop(objectURI, propertyURI, forward) {
        var url = g_slidingURL + "&-=" +
                encodeURIComponent(
                        "@lwq.project.PropertyProjector;" + (forward ? "!" : "") + propertyURI + ";" +
                        "@lwq.bucket.DistinctValueBucketer;r" + objectURI);
                        
        browseTo(url);
}
