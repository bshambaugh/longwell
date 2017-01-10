function filterTags(e, valueListID) {
    var inputElement = getTarget(e);
    var list = document.getElementById(valueListID);
    var text = inputElement.value;
    var regexp = new RegExp(text, "ig");

    var values = list.getElementsByTagName("div");
    for (var i = 0; i < values.length; i++) {
        var value = values[i];

        if (text.length > 0) {
	    try {
                var tagLabel = value.firstChild.nextSibling.firstChild.nodeValue;
                if (tagLabel.match(regexp)) {
                    if (value.style.display != "block") {
                        value.style.display = "block";
                    }
                } else {
                    if (value.style.display != "none") {
                        value.style.display = "none";
                    }
                }
	    } catch (e) {
	        if (value.style.display != "none") {
	  	    value.style.display = "none";
		}
	    }
        } else {
            value.style.display = "block";
        }
    }
}
