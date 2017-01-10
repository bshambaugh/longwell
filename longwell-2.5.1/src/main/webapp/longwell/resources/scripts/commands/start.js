function filterClasses(e, valueListID) {
    var inputElement = getTarget(e);
    var list = document.getElementById(valueListID);
    var text = inputElement.value;
    var regexp = new RegExp(text, "ig");
    
    var values = list.getElementsByTagName("li");
    for (var i = 0; i < values.length; i++) {
        var value = values[i];
        
        if (text.length > 0) {
            var tagLabel = value.firstChild.firstChild.firstChild.nodeValue;
            if (tagLabel.match(regexp)) {
                if (value.style.display != "list-item") {
                    value.style.display = "list-item";
                }
            } else {
                if (value.style.display != "none") {
                    value.style.display = "none";
                }
            }
        } else {
            value.style.display = "list-item";
        }
    }
}
