var Names = {};

Names.onNameInputKeyUp = function(evt, object, profileID, nameFunction) {
        var objectURI = object.parentNode.parentNode.getAttribute("id");
	var completionDiv = document.getElementById("lw_names_completion_" + objectURI);
	var completionDivVisible = completionDiv.style.display == "block";

	evt = (evt) ? evt : event;

	if (completionDivVisible) {
		if (evt.keyCode == 13) { // Enter
			Names._complete(objectURI, profileID);
		} else if (evt.keyCode == 27) { // ESC
			Names._hideCompletionDiv(objectURI);
		} else if (evt.keyCode == 38 /* arrow up */ || evt.keyCode == 40 /* arrow down */) {
			var completionSelect = document.getElementById("lw_names_completion_selector_" + objectURI);
			var i = completionSelect.selectedIndex;

			if (evt.keyCode == 38) {
				i = i - 1;
			} else if (evt.keyCode == 40) {
				i = i + 1;
			}

			i = Math.min(Math.max(i, 0), completionSelect.length - 1);
			completionSelect.selectedIndex = i;

			evt.cancelBubble = true;
		} else {
			Names._suggestCompletion(objectURI, profileID);
		}
	} else {
		if (evt.keyCode == 13) { // Enter
			Names.name(objectURI, profileID, nameFunction);
		} else {
			Names._suggestCompletion(objectURI, profileID);
		}
	}
}

Names.onNameInputBlur = function(evt, object, profileID) {
        var objectURI = object.parentNode.parentNode.getAttribute("id");
	Names._hideCompletionDiv(objectURI);
}

Names._complete = function(objectURI, profileID) {
	var completionDiv = document.getElementById("lw_names_completion_" + objectURI);
	var completionSelect = document.getElementById("lw_names_completion_selector_" + objectURI);
	var i = completionSelect.selectedIndex;
	if (i >= 0) {
		var item = completionSelect.item(i);
		var completion = item.text;
                var completionURI = item.value;

		var input = document.getElementById('lw_names_' + objectURI);
                var identifier = document.getElementById('lw_names_completion_uri_' + objectURI);
		var text = input.value;
		var cursor = Names._findCursor(input);
		var startOfName = Names._findStartOfName(text, cursor);
		var prefix = text.substring(startOfName, cursor);

		if (completion.toLowerCase().indexOf(prefix.toLowerCase()) == 0) {
			var newText = text.substring(0, startOfName) + completion + text.substring(cursor);
			input.value = newText;
                        identifier.value = completionURI;

			cursor = startOfName + completion.length;
			input.selectionStart = cursor;
			input.selectionEnd = cursor;
		}
	}
	Names._hideCompletionDiv(objectURI);
}

Names._suggestCompletion = function(objectURI, profileID) {
	try {
		var input = document.getElementById('lw_names_' + objectURI);
		var text = input.value;
		var cursor = Names._findCursor(input);
		var startOfName = Names._findStartOfName(text, cursor);
		var prefix = text.substring(startOfName, cursor);

		if ((prefix) && prefix.length > 0) {
                        var idx = document.location.href.indexOf("resources/");
			HTTPUtilities.doPost(
				document.location.href.substring(0, idx) + "default?command=name",
				"complete\n" + prefix,
				function(status, statusText) {
				},
				function (text) {
					Names._onCompletionAvailable(objectURI, text);
				}
			);
		} else {
			Names._hideCompletionDiv(objectURI);
		}
	} catch (e) {
		alert(e);
	}
}

Names._hideCompletionDiv = function(objectURI) {
	var completionDiv = document.getElementById("lw_names_completion_" + objectURI);
	completionDiv.style.display = "none";
}

Names._onCompletionAvailable = function(objectURI, completionText) {
	try {
		var input = document.getElementById('lw_names_' + objectURI);
		var text = input.value;
		var cursor = Names._findCursor(input);
		var startOfName = Names._findStartOfName(text, cursor);
		var prefix = text.substring(startOfName, cursor).toLowerCase();

		var lines = completionText.split("\n");
		if (prefix == lines[0]) {
			var completionSelect = document.getElementById("lw_names_completion_selector_" + objectURI);
			var elmt;

			while (completionSelect.length > 0) {
				completionSelect.remove(0);
			}

			for (var i = 1; i < lines.length; i++) {
				if (lines[i].length > 0) {
                                        parts = lines[i].split("|");
					elmt = document.createElement("OPTION");
					elmt.text = parts[0];
                                        elmt.value = parts[1];

					completionSelect.appendChild(elmt);
				}
			}

			var completionDiv = document.getElementById("lw_names_completion_" + objectURI);
			if (completionSelect.length > 0) {
				completionSelect.selectedIndex = 0;

				completionDiv.style.width = input.offsetWidth + "px";
				completionDiv.style.left = input.offsetLeft + "px";
				completionDiv.style.top = (input.offsetTop + input.offsetHeight) + "px";
				completionDiv.style.display = "block";
			} else {
				completionDiv.style.display = "none";
			}
		}
	} catch (e) {
		alert(e);
	}
}

Names._findCursor = function(input) {
  try {
    return Math.min(input.selectionStart, input.selectionEnd);
  } catch (e) {
    return 0;
  }
}

Names._findStartOfName = function(text, cursor) {
	var i = text.lastIndexOf(",", cursor);
	if (i < 0) {
		i = 0;
	} else {
		i += 1;
	}
	while (i < cursor && text.charAt(i) == " ") {
		i++;
	}
	return i;
}

Names.name = function(objectURI, profileID, nameFunction) {
	try {
		var input = document.getElementById('lw_names_' + objectURI);
		var text = input.value;

		if (nameFunction) {
			nameFunction(text);
		}
	} catch (e) {
		alert(e);
	}
}

Names.getNameLabels = function(objectURI) {
	var input = document.getElementById('lw_names_' + objectURI);
	return input.value;
}
