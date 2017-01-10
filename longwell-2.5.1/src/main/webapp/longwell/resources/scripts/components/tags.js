var Tags = {};

Tags.onTagInputKeyUp = function(evt, objectURI, profileID, tagFunction) {
	var completionDiv = document.getElementById("lw_tags_completion_" + objectURI);
	var completionDivVisible = completionDiv.style.display == "block";

	evt = (evt) ? evt : event;

	if (completionDivVisible) {
		if (evt.keyCode == 13) { // Enter
			Tags._complete(objectURI, profileID);
		} else if (evt.keyCode == 27) { // ESC
			Tags._hideCompletionDiv(objectURI);
		} else if (evt.keyCode == 38 /* arrow up */ || evt.keyCode == 40 /* arrow down */) {
			var completionSelect = document.getElementById("lw_tags_completion_selector_" + objectURI);
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
			Tags._suggestCompletion(objectURI, profileID);
		}
	} else {
		if (evt.keyCode == 13) { // Enter
			Tags.tag(objectURI, profileID, tagFunction);
		} else {
			Tags._suggestCompletion(objectURI, profileID);
		}
	}
}

Tags.onTagInputBlur = function(evt, objectURI, profileID) {
	Tags._hideCompletionDiv(objectURI);
}

Tags._complete = function(objectURI, profileID) {
	var completionDiv = document.getElementById("lw_tags_completion_" + objectURI);
	var completionSelect = document.getElementById("lw_tags_completion_selector_" + objectURI);
	var i = completionSelect.selectedIndex;
	if (i >= 0) {
		var item = completionSelect.item(i);
		var completion = item.text;

		var input = document.getElementById('lw_tags_' + objectURI);
		var text = input.value;
		var cursor = Tags._findCursor(input);
		var startOfTag = Tags._findStartOfTag(text, cursor);
		var prefix = text.substring(startOfTag, cursor);

		if (completion.toLowerCase().indexOf(prefix.toLowerCase()) == 0) {
			var newText = text.substring(0, startOfTag) + completion + text.substring(cursor);
			input.value = newText;

			cursor = startOfTag + completion.length;
			input.selectionStart = cursor;
			input.selectionEnd = cursor;
		}
	}
	Tags._hideCompletionDiv(objectURI);
}

Tags._suggestCompletion = function(objectURI, profileID) {
	try {
		var input = document.getElementById('lw_tags_' + objectURI);
		var text = input.value;
		var cursor = Tags._findCursor(input);
		var startOfTag = Tags._findStartOfTag(text, cursor);
		var prefix = text.substring(startOfTag, cursor);

		if ((prefix) && prefix.length > 0) {
			HTTPUtilities.doPost(
				g_contextPath + "/default?command=tag",
				"complete\n" + prefix,
				function(status, statusText) {
				},
				function (text) {
					Tags._onCompletionAvailable(objectURI, text);
				}
			);
		} else {
			Tags._hideCompletionDiv(objectURI);
		}
	} catch (e) {
		Debug.onCaughtException(e);
	}
}

Tags._hideCompletionDiv = function(objectURI) {
	var completionDiv = document.getElementById("lw_tags_completion_" + objectURI);
	completionDiv.style.display = "none";
}

Tags._onCompletionAvailable = function(objectURI, completionText) {
	try {
		var input = document.getElementById('lw_tags_' + objectURI);
		var text = input.value;
		var cursor = Tags._findCursor(input);
		var startOfTag = Tags._findStartOfTag(text, cursor);
		var prefix = text.substring(startOfTag, cursor);

		var lines = completionText.split("\n");
		if (prefix == lines[0]) {
			var completionSelect = document.getElementById("lw_tags_completion_selector_" + objectURI);
			var elmt;

			while (completionSelect.length > 0) {
				completionSelect.remove(0);
			}

			for (var i = 1; i < lines.length; i++) {
				if (lines[i].length > 0) {
					elmt = document.createElement("OPTION");
					elmt.text = lines[i];

					completionSelect.appendChild(elmt);
				}
			}

			var completionDiv = document.getElementById("lw_tags_completion_" + objectURI);
			if (completionSelect.length > 0) {
				completionSelect.selectedIndex = 0;

				var left = 0;
				var top = 0;
				elmt = input;
				while (elmt) {
					left += elmt.offsetLeft;
					top += elmt.offsetTop;
					elmt = elmt.offsetParent;
				}

				completionDiv.style.width = input.offsetWidth + "px";
				completionDiv.style.left = left + "px";
				completionDiv.style.top = (top + input.offsetHeight) + "px";
				completionDiv.style.display = "block";
			} else {
				completionDiv.style.display = "none";
			}
		}
	} catch (e) {
		Debug.onCaughtException(e);
	}
}

Tags._findCursor = function(input) {
	return Math.min(input.selectionStart, input.selectionEnd);
}

Tags._findStartOfTag = function(text, cursor) {
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

Tags.tag = function(objectURI, profileID, tagFunction) {
	try {
		var input = document.getElementById('lw_tags_' + objectURI);
		var text = input.value;

		if (tagFunction) {
			tagFunction(text);
		} else {
			Tags._tagFunction(objectURI, profileID, text);
		}
	} catch (e) {
		Debug.onCaughtException(e);
	}
}

Tags.tagAndPublish = Tags.tag; // for now, we publish everything that's tagged anyway

Tags.getTagLabels = function(objectURI) {
	var input = document.getElementById('lw_tags_' + objectURI);
	return input.value;
}

Tags._tagFunction = function(objectURI, profileID, tagLabels) {
	var fSave;

	if (profileID != "default") {
		fSave = function() {
			HTTPUtilities.doPost(
				g_contextPath + "/" + profileID + "?command=save",
				objectURI,
				function(status, statusText) {
					window.alert("Failed to save tidbit after tagging.");
				},
				function (text) {
					reloadView(objectURI, profileID, "lw_item_", "", [], true);
				}
			);
		};
	} else {
		fSave = function() {
			reloadView(objectURI, profileID, "lw_item_", "", [], true);
		};
	}

	HTTPUtilities.doPost(
		g_contextPath + "/" + profileID + "?command=tag",
		"tag\n" + objectURI + "\n" + tagLabels,
		function(status, statusText) {
			alert("Failed to tag");
		},
		function (text) {
			fSave();
		}
	);
}
