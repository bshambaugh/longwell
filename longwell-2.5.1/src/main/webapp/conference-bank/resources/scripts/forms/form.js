var Form = new Object();

Form.onCaughtException = function(e) {
  alert(e);
}

Form.onLoad = function() {
  var s = "";
  s += "<div class='buttons-div'>"
  s +=   "<button type='button' onclick='Form.publish();'>Publish</button>";
  s +=   "<p>Publish directly to the Semantic Bank as an anonymous contribution.</p>";
  s +=   "<button type='button' onclick='Form.retrieveN3();'>Preview</button>";
  s +=   "<p>Preview N3 raw data. If you are running Firefox with Piggy Bank, it will show you this data in a better presentation. You can publish the data from Piggy Bank under your own account.</p>";
  s += "</div>";
  
  document.getElementById("form-controls").innerHTML = s;
}

Form.trimString = function(s) {
  var i = 0;
  var spaceChars = " \n\r\t" + String.fromCharCode(160) /* &nbsp; */;
  while (i < s.length) {
    var c = s.charAt(i);
    if (spaceChars.indexOf(c) < 0) {
        break;
    }
    i++;
  }
  
  s = s.substring(i);
  
  i = s.length;
  while (i > 0) {
    var c = s.charAt(i - 1);
    if (spaceChars.indexOf(c) < 0) {
        break;
    }
    i--;
  }
  
  return s.substring(0, i);
}

Form.getText = function(controlId) {
  return Form.trimString(document.getElementById(controlId).value);
}

Form.processForm = function(mimetype, publish) {
  var uri = window.itemURI;
  if ((!uri) || uri.length == 0) {
    alert("You must specify a URI for this item.");
    return;
  }
  
  var types = window.itemTypes;
  
  Form.s = "";
  Form.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
  Form.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
  Form.addPrefix("dc", "http://purl.org/dc/elements/1.1/");
  processForm(uri, types);
  
  if (publish) {
    Form.appendLine(Form.encodeURI(uri) + " <http://simile.mit.edu/2005/04/ontologies/publishing#status> <http://simile.mit.edu/2005/04/ontologies/publishing#Public> .");
  }
}

Form.retrieve = function(mimetype) {
  Form.processForm(mimetype, false);
  
  var url = "../../default?command=url-unwrapper&mimetype=" + encodeURIComponent(mimetype)
    + "&content=" + encodeURIComponent(Form.s);
  
  window.open(url, "_top");
}

Form.retrieveN3 = function() {
  Form.retrieve("application/n3");
}

Form.retrieveText = function() {
  Form.retrieve("text/plain");
}

Form.addPrefix = function(prefix, expansion) {
  Form.s += "@prefix " + prefix + ": <" + expansion + "> .\n";
}

Form.appendLine = function(s) {
  Form.s += s + "\n";
}

Form.appendURIAndTypes = function(uri, types) {
  Form.appendLine(Form.encodeURI(uri) + " rdf:type " + Form.encodeURI(types[0]));
  for (t = 1; t < types.length; t++) {
    Form.appendLine(" ; rdf:type " + Form.encodeURI(types[t]));
  }
}

Form.encodeURI = function(uri) {
  return "<" + uri + ">";
}

Form.encodeLiteral = function(l) {
  return '"' + l + '"';
}

Form.appendTextProperty = function(property, textboxId) {
  var s = Form.getText(textboxId);
  if (s.length > 0) {
    Form.appendLine(" ; " + property + " \"" + s + "\"");
    return s;
  } else {
    return null;
  }
}

Form.appendSelectProperty = function(property, selectId) {
  var s = Form.getText(selectId);
  if (s.length > 0) {
    Form.appendLine(" ; " + property + " \"" + s + "\"");
    return s;
  } else {
    return null;
  }
}

Form.initializeTextBox = function(data, key, textboxId) {
  var a = data[key];
  if (a && a.length > 0) {
    document.getElementById(textboxId).value = a[0];
  }
}

Form.initializeSelect = function(data, key, selectId) {
  var a = data[key];
  if (a && a.length > 0) {
    document.getElementById(selectId).value = a[0];
  }
}

Form.publish = function() {
  Form.processForm("application/n3", true);
  
  var url = document.location.href;
  url = url.substr(0, url.indexOf("/resources/forms"));

  var urlPost = url + "/anonymous?command=upload";
  
  var onError = function(status, statusText) {
    alert("Failed to publish as anonymous.\nError:" + statusText);
  };
  var onDone = function(responseText) {
    if (confirm("Success! Browse to the new item on the bank?")) {
      // do not re-encode window.itemURI, will fail to focus
      // er?  does not appear to be encoded in the first place... - RL
      window.open(url + "/default?command=focus&objectURI=" + encodeURIComponent(window.itemURI), "_top");
    }
  };
  
  HTTPUtilities.doPost(urlPost, Form.s, onError, onDone);
}
