function browseTo(url) {
	window.location = url;
}

function hideTextFieldLabel(id) {
	var textfield = document.getElementById(id);
	var label = document.getElementById(id + "-label");

	textfield.style.opacity = 1;
	label.style.display = "none";
}

function showTextFieldLabel(id) {
	var textfield = document.getElementById(id);
	var label = document.getElementById(id + "-label");

	if (textfield.value != "") {
		textfield.style.opacity = 1;
		label.style.display = "none";
	} else {
		textfield.style.opacity = 0;
		label.style.display = "block";
	}
}

function freeTextSearch(evt) {
	evt = (evt) ? evt : event;

	var charCode = (evt.charCode) ? evt.charCode : ((evt.which) ? evt.which : evt.keyCode);
	if (charCode == 13 || charCode == 3) {
		var textbox = getTarget(evt);
		var text = textbox.value;

		window.location =
			g_urlWithoutPaging +
			'&-=@lwq.project.TextIndexProjector;;@lwq.bucket.TextIndexBucketer;' + text;
	}
}

function getTarget(e) {
	var event;

	if (!e) {
		event = window.event;
	} else {
		event = e;
	}

	var targ;
	if (event.target) {
	   targ = event.target;
	} else if (event.srcElement) {
	   targ = event.srcElement;
	}
	return targ;
}

function getElementStyle(elem, property) {
    if (elem.currentStyle) {
        return elem.currentStyle[property];
    } else if (window.getComputedStyle) {
        var compStyle = window.getComputedStyle(elem, "");
        return compStyle.getPropertyValue(property);
    }
    return "";
}

var g_stylesheets = new Array();

function initStylesheets() {
    for(var i = 0; i < document.styleSheets.length; i++) {
        g_stylesheets[document.styleSheets.item(i)] = true;
    }
}

initStylesheets();

function registerStylesheet(url) {
    if (!g_stylesheets[url]) {
        g_stylesheets[url] = true;
        var style = document.createElement("link");
        style.rel = "stylesheet";
		style.href= url;
		style.type = "text/css";
		document.body.appendChild(style);
    }
}