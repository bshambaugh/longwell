function loadFacetData(profileID, propertyURI, phase, bodyPrefix, panePrefix, configured) {
    // if 'more' phase, change appearance of facet body to prevent confusion
    if (phase == 'more') {
        var elmt = document.getElementById(bodyPrefix + propertyURI);
        elmt.innerHTML = '<div style="text-align: center; height: 100%; padding: 20px 0;">Loading...</div>';
    }

    // setup async call for facet values
    var s = "&outerQuery=" + encodeURIComponent(g_outerQuery);
    HTTPUtilities.doGet(
        g_contextPath + "/" + profileID + "?command=facet&facetURI=" + encodeURIComponent(propertyURI) + "&phase=" + phase + s,
        function(status, statusText) {
            alert("Failed to load facets for item with URI = " + propertyURI);
        },
        function(text) {
            try {
                var elmt = document.getElementById(bodyPrefix + propertyURI);
                elmt.style.display = "none";
                elmt.innerHTML = text;
		        unregisterFacet();
                if (text.indexOf("<div class=\"lw_empty\">") < 0) {
                    var paneElmt = document.getElementById(panePrefix + propertyURI);
                    paneElmt.style.display = "block"; 
                    if (configured) {
                      elmt.style.display = "block";
                    }
                }
            } catch (e) {
                alert(e);
            }
        }
    );
}

var g_facetCount = 0;
var g_facetLoadCounter = 0;

function registerFacetCount(n) {
    g_facetCount = n;
    g_facetLoadCounter = n;
}

function unregisterFacet() {
    var progressBar = document.getElementById("lw_sidebar_facet_progress");
    --g_facetLoadCounter;
    var width = Math.round((1 - ( g_facetLoadCounter / g_facetCount )) * 100);
    progressBar.innerHTML = "<p>Loading facets...</p><div id=\"progress_meter\"><img src=\"" + g_resourcePath + "/images/meter.gif\" width=\"" + width + "\" height=\"15\" /></div>";
    if (g_facetLoadCounter == 0) {
    	document.getElementById("lw_sidebar_facet_progress").style.display = "none";
    }
}
