var Editing = new Object();

Editing._itemData = [];

Editing.onCaughtException = function(e) {
  alert(e);
}

Editing.onLoad = function() {
  var url = document.location.href;
  
  var uri;
  var types = [];
  
  var i = url.indexOf("?");
  if (i > 0) {
    var query = url.substr(i + 1);
    var parameters = query.split("&");
    for (i = 0; i < parameters.length; i++) {
      var parameter = parameters[i];
      var j = parameter.indexOf("=");
      if (j > 0) {
        var name = parameter.substr(0, j);
        var value = decodeURIComponent(parameter.substr(j+1));
        
        if (name == "uri") {
          uri = value;
        } else if (name == "type") {
          types.push(value);
        }
      }
    }
  }
  
  var fContinuation = function() {
    document.getElementById("loading-ui").style.display = "none";
    
    if (types.length > 0) {
      Editing.itemTypes = types;
      
      document.getElementById("form-ui").style.display = "block";
      
      Editing.listTypes(types);
      Editing.onTypeSelectChange();
    } else {
      document.getElementById("type-ui").style.display = "block";
      Editing.listTypes2();
    }
    
    Editing.setIFrameParameters();
  };
  
  if (uri) {
    var textbox = document.getElementById("uri-textbox")
    textbox.value = uri;
    textbox.readOnly = true;
    
    Editing.itemURI = uri;
    
    var url = document.location.href;
    url = url.substr(0, url.indexOf("/resources/forms"));
    url += "/default?command=export&format=N3&objectURI=" + encodeURIComponent(uri);
    
    var onError = function(status, statusText) {
      fContinuation();
    };
    var onDone = function(responseText) {
      Editing._processN3(responseText);
      fContinuation();
    };
    
    HTTPUtilities.doGet(url, onError, onDone);
  } else {
    Editing.itemURI = document.getElementById("uri-textbox").value;
    fContinuation();
  }
}

Editing.listForms = function(type) {
  var s = "";
  var count = 0;
  var formURL = null;
  for (var i = 0; i < Registry.forms.length; i++) {
    var form = Registry.forms[i];
    if ((!form.type) || form.type == type) {
      s += "<div><a href=\"" + form.url + "\" target='form-iframe'>" + form.title + "</a></div>";
      if (!formURL) {
        formURL = form.url;
      }
    }
  }
  
  document.getElementById("forms-list").innerHTML = s;
  if (formURL) {
    document.getElementById("form-iframe").src = formURL;
  }
}

Editing.listTypes = function(types) {
  var select = document.getElementById("type-select");
  var foundType = false;
  
  var type = null;
  
  for (var i = 0; i < Registry.types.length; i++) {
    var type2 = Registry.types[i];
    var elmt = document.createElement("option");
    elmt.setAttribute("value", type2.uri);
    elmt.innerHTML = type2.title;
    
    for (var t = 0; t < types.length; t++) {
      if (type2.uri == types[t]) {
        elmt.setAttribute("selected", "true");
        foundType = true;
        type = types[t];
      }
    }
    select.add(elmt, null);
  }
  
  if (!foundType) {
    var elmt = document.createElement("option");
    elmt.setAttribute("value", type);
    elmt.innerHTML = type;
    elmt.setAttribute("selected", "true");
    select.add(elmt, null);
  }
}

Editing.listTypes2 = function() {
  var s = "";
  var url = document.location.href;
  
  if (url.indexOf("?") > 0)
    url += "&type=";
  else
    url += "?type=";
  
  for (var i = 0; i < Registry.types.length; i++) {
    var type = Registry.types[i];
    
    s += "<div><a href=\"" + url + encodeURIComponent(type.uri) + "\">" + type.title + "</a></div>";
  }
  
  document.getElementById("types-list").innerHTML = s;
}

Editing.listTypesListing = function(base) {
  var s = "";
  var url = base;
  
  if (url.indexOf("?") > 0)
    url += "&type=";
  else
    url += "?type=";
  
  for (var i = 0; i < Registry.types.length; i++) {
    var type = Registry.types[i];
    
    s += "<li><a href=\"" + url + encodeURIComponent(type.uri) + "\">" + type.title + "</a></li>";
  }
  
  document.getElementById("types-list").innerHTML = s;
}

Editing.onTypeSelectChange = function() {
  var type = document.getElementById("type-select").value;
  
  Editing.itemTypes = [ type ];
  Editing.listForms(type);
}

Editing.onURITextboxChange = function() {
  var uri = document.getElementById("uri-textbox").value;
  
  Editing.itemURI = uri;
  Editing.setIFrameParameters();
}

Editing.onIFrameLoad = function() {
  Editing.setIFrameParameters();
}

Editing.setIFrameParameters = function() {
  var iframeWindow = document.getElementById("form-iframe").contentWindow
  iframeWindow.itemTypes = Editing.itemTypes;
  iframeWindow.itemURI = Editing.itemURI;
  
  try {
    iframeWindow.fillData(Editing._itemData);
  } catch (e) {
  }
}

Editing._processN3 = function(s) {
  var fInsert = function(key, value) {
    var values = Editing._itemData[key];
    if (!values) {
      values = [];
      Editing._itemData[key] = values;
    }
    values.push(value);
  };
  
  var lines = s.split("\n");
  var l = 0;
  
  var prefixes = [];
  
  while (l < lines.length) {
    var line = lines[l];
    if (line.indexOf("@prefix") >= 0) {
      var a = line.split(/\s+/);
      
      var prefix = a[1];
      prefix = prefix.substr(0, prefix.length - 1);
      
      var expansion = a[2];
      var n = expansion.lastIndexOf(">");
      expansion = expansion.substring(1, n);
      
      prefixes[prefix] = expansion;
      
      l++;
    } else if (line.match(/^\s*$/)) {
      l++;
    } else {
      break;
    }
  }
  lines.splice(0, l);
  
  s = lines.join(" ");
  s = s.replace(/^\s+/g, "");
  s = s.substr(s.search(/\s/));
  s = s.substr(0, s.lastIndexOf("."));
  
  pairs = s.split(";");
  for (p = 0; p < pairs.length; p++) {
    var pair = pairs[p].replace(/^\s+/, "").replace(/\s+$/, "");
    var n = pair.search(/\s/);
    var pair2 = pair.substr(n);
    
    var value = pair2.substr(pair2.search(/\S/));
    if (value.charAt(0) == '"') {
      value = value.substring(1, value.length - 1);
    }
    
    var predicate = pair.substr(0, n);
    
    n = predicate.indexOf(":");
    var prefix = predicate.substr(0, n);
    var suffix = predicate.substr(n+1);
    var expansion = prefixes[prefix];
    
    fInsert(suffix, value);
    fInsert(expansion + suffix, value);
  }
}