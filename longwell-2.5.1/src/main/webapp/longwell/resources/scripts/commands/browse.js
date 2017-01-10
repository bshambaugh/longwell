function toggleBroadeningFacet(facetID) {
	var showControl = document.getElementById("lw_" + facetID + "_show_broadening_facet");
	var hideControl = document.getElementById("lw_" + facetID + "_hide_broadening_facet");

	var display = showControl.style.display;
	if (display == "none") {
		showControl.style.display = "inline";
		hideControl.style.display = "none";
	} else {
		showControl.style.display = "none";
		hideControl.style.display = "block";
	}
}

