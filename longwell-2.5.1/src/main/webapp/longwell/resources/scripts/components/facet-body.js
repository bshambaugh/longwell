function restrict(e, valueListID) {
    var inputElement = getTarget(e);
    var list = document.getElementById(valueListID);
    var text = inputElement.value;
    var regexp = new RegExp(text, "ig");
    
    var values = list.getElementsByTagName("div");
    for (var i = 0; i < values.length; i++) {
        var value = values[i];
        
        if (text.length > 0) {
            var restrictionValue = value.firstChild.firstChild.nodeValue;
            
            if (restrictionValue.match(regexp)) {
                if (value.style.display != "block") {
                    value.style.display = "block";
                }
            } else {
                if (value.style.display != "none") {
                    value.style.display = "none";
                }
            }
        } else {
            value.style.display = "block";
        }
    }
}

function selectBucketTheme(select, name) {
	var selectedIndex = select.selectedIndex;
	
	var bucketThemes = document.getElementsByName(name);
	for (var i = 0; i < bucketThemes.length; i++) {
		var bucketTheme = bucketThemes[i];
		if (i == selectedIndex) {
			bucketTheme.style.visibility = "visible";
		} else {
			bucketTheme.style.visibility = "hidden";
		}
	}
}
