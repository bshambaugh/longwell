var points = new Array();
var counter = 0;
var poi_id = "p";
var Mmap;
var ATTENDEEMODE = 0;
var GALWAYMODE = 1;
var currentMode;
var mainGeocodeCache;
var mainGeocoder;
var modes = new Array();
modes[ATTENDEEMODE] = new Array();
modes[ATTENDEEMODE]['zoom'] = 2;
modes[ATTENDEEMODE]['centerLat'] = '36.87962060502676';
modes[ATTENDEEMODE]['centerLng'] = '-7.03125';
modes[ATTENDEEMODE]['type'] = 'http://xmlns.com/foaf/0.1/Person';
modes[ATTENDEEMODE]['typeLabel'] = 'Person';
modes[ATTENDEEMODE]['typeTitle'] = 'Name';
modes[ATTENDEEMODE]['typeTitleProperty'] = new Array('foaf:name','rdf:value');
modes[ATTENDEEMODE]['autocomplete'] = true;
modes[ATTENDEEMODE]['mapMode'] = G_MAP_TYPE;
modes[GALWAYMODE] = new Array();
modes[GALWAYMODE]['zoom'] = 15;
modes[GALWAYMODE]['centerLat'] = '53.27419584888388';
modes[GALWAYMODE]['centerLng'] = '-9.049043655395508';
modes[GALWAYMODE]['type'] = 'http://simile.mit.edu/2005/05/ontologies/location#Point';
modes[GALWAYMODE]['typeLabel'] = 'Point';
modes[GALWAYMODE]['typeTitle'] = 'Label';
modes[GALWAYMODE]['typeTitleProperty'] = new Array('rdfs:label');
modes[GALWAYMODE]['autocomplete'] = false;
modes[GALWAYMODE]['mapMode'] = G_MAP_TYPE;

var newPointIconUrl = document.location.href;
newPointIconUrl = newPointIconUrl.substr(0, newPointIconUrl.indexOf("/resources")) + "/resources/marker?x=0&y=0&s=1&w=40&h=34&label=?&colorCode=1";

if (navigator.appName == 'Microsoft Internet Explorer') {
    document.ondblclick = handleDblClick;
} else {
    window.ondblclick = handleDblClick;
}

function handleDblClick(e) {
    removeLatestPoint();
}

function _makeMarker(map, x) {
    // location
    var point = new GLatLng(x.getElementsByTagName("point")[0].getAttribute("lat"), x.getElementsByTagName("point")[0].getAttribute("lng"));
    // icon
    var icon = new GIcon();
    var imageurl = x.getElementsByTagName("icon")[0].getAttribute("image");
    // for safari, where & => &#38; and .replace doesn't seem to work
    while (imageurl.indexOf('&#38;') != -1) {
        var pos = imageurl.indexOf('&#38;');
        imageurl = imageurl.substring(0, pos) + "&" + imageurl.substring(pos + "&#38;".length);
    }
    icon.image = imageurl;
    icon.shadow = "http://www.google.com/mapfiles/shadow50.png";
    icon.iconSize = new GSize(40, 34);
    icon.shadowSize = new GSize(67, 34);
    icon.iconAnchor = new GPoint(19, 34);
    icon.infoWindowAnchor = new GPoint(20, 0);

    // marker
    var marker = new GMarker(point, icon);
    var info = x.getElementsByTagName("div")[0];

    addPoint(poi_id, marker, info);

    return marker;
}

// displays xml generated by longwell
function _Gdisplay(x) {
    document.getElementById('loading').style.display='none';
    if (GBrowserIsCompatible()) {
        // new map
        var map = new GMap2(document.getElementById("map"));
        Mmap = map;

        // center
        var mapinfo = x.documentElement.getElementsByTagName("center");
        var center = new GLatLng(modes[currentMode]['centerLat'], modes[currentMode]['centerLng']);
        map.setCenter(center);
 
        // zoom
        map.setZoom(modes[currentMode]['zoom']);

        // add controls
        map.addControl(new GLargeMapControl());
        map.addControl(new GMapTypeControl());
        map.addControl(new HelpControl());

        // go through XML for each location
        var markers = x.documentElement.getElementsByTagName("location");
        for (var i = 0; i < markers.length; i++) {
            var marker = _makeMarker(map, markers[i]);
            map.addOverlay(marker);
        }

        GEvent.addListener(map, "click", function(overlay, point) {
            if (overlay) {
                var overlayId = getId(overlay);
                var domFrag;
                if (!points[overlayId]['info']) {
                    domFrag = makeInfoWindow(overlayId);
                } else {
                    domFrag = points[overlayId]['info'];
                }
                overlay.openInfoWindow(domFrag);
                map.panTo(point);
            } else if (point) {
                var icon = new GIcon();
                icon.image = newPointIconUrl;
                icon.shadow = "http://www.google.com/mapfiles/shadow50.png";
                icon.iconSize = new GSize(40, 34);
                icon.shadowSize = new GSize(67, 34);
                icon.iconAnchor = new GPoint(19, 34);
                icon.infoWindowAnchor = new GPoint(20, 0);
                var marker = new GMarker(point, icon);
                map.addOverlay(marker);
                addPoint(poi_id, marker, false);
            }
        });
        Mmap.setMapType(modes[currentMode]['mapMode']);
    }
    mainGeocodeCache = new LongwellGeocodeCache()
    mainGeocoder = new LongwellClientGeocoder(mainGeocodeCache);
}

// run after page is loaded
function _initGMap() {

    var urlOffset = document.location.search.indexOf("url=") + "url=".length;
    var modeOffset = document.location.search.indexOf("mode=");
    var url = document.location.search.substring(urlOffset, modeOffset);
    currentMode = document.location.search.substring(modeOffset + "mode=".length, document.location.search.length);
    url = unescape(url);
    try {
        var xmlhttp = GXmlHttp.create();
        xmlhttp.open("GET", url, true);
        xmlhttp.onreadystatechange=function() {
            if (xmlhttp.readyState==4) {
                _Gdisplay(xmlhttp.responseXML);
            }
        }
        xmlhttp.send(null); // Whoops, IE *needs* this to be last, Moz is lenient.
    } catch (e) {
        // um...this try/catch seems to enable Safari functionality...
    }
}

function removePoint(obj) {
    Mmap.closeInfoWindow();
    Mmap.removeOverlay(points[obj.parentNode.parentNode.getAttribute("id")]['marker']);
    points[obj.parentNode.parentNode.getAttribute("id")] = null;
}

function removeLatestPoint() {
    if (points[poi_id + (counter - 1)] && !points[poi_id + (counter - 1)]['info']) {
        Mmap.removeOverlay(points[poi_id + (counter - 1)]['marker']);
        points[poi_id + (counter - 1)] = null;
    }

    if (points[poi_id + (counter - 2)] && !points[poi_id + (counter - 2)]['info']) {
        Mmap.removeOverlay(points[poi_id + (counter - 2)]['marker']);
        points[poi_id + (counter - 2)] = null;
    }
}

function setLabel(obj) {
    var outerDiv = obj.parentNode.parentNode;
    var id = outerDiv.getAttribute("id");
    if (!points[id]) {
      points[id] = [];
      try {
        var lat = parseFloat(outerDiv.getAttribute("lat"));
        var lng = parseFloat(outerDiv.getAttribute("lng"));
        
        var point = new GLatLng(lat, lng);
        
        var icon = new GIcon();
        icon.image = newPointIconUrl;
        icon.shadow = "http://www.google.com/mapfiles/shadow50.png";
        icon.iconSize = new GSize(40, 34);
        icon.shadowSize = new GSize(67, 34);
        icon.iconAnchor = new GPoint(19, 34);
        icon.infoWindowAnchor = new GPoint(20, 0);
        var marker = new GMarker(point, icon);
        
        points[id]['marker'] = marker;
      } catch (e) {
        alert(e);
      }
    }

    var txt = outerDiv.getElementsByTagName("div")[0].getElementsByTagName("input")[0].value;
    txt = txt.replace(/^\s+/g, "").replace(/\s+$/g, "");
    if (txt == '') {
       alert('Please give this ' + modes[currentMode]['typeLabel'] + ' a ' + modes[currentMode]['typeTitle']);
       return;
    }
    
    var identifier = document.getElementById('lw_names_completion_uri_' + id);
    points[id]['label'] = txt;
    points[id]['uri'] = identifier.value;
    
    Mmap.closeInfoWindow();
    
    var idx = document.location.href.indexOf("resources/");
    HTTPUtilities.doPost(
      document.location.href.substring(0, idx) + 
        "default?command=upload&format=rdfxml", 
      processRDFXML(points[id]), 
      function(status, text) { 
        alert("Could not upload: " + text + "\nStatus:" + status);
      }, 
      function() { 
        alert("Added " + modes[currentMode]['typeLabel']); 
        document.location.reload(false); 
      }
    );
}

function getId(obj) {
    var out;
    for (p in points) {
       if (p != 'clear' && points[p] != null) {
           if (points[p]['marker'].getPoint().lat() == obj.getPoint().lat() && points[p]['marker'].getPoint().lng() == obj.getPoint().lng()) {
               out = p;
           }
       }
    }
    return out;
}

function getLabel(id) {
    return points[id]['label'];
}

function makeInfoWindow(id) {
    var contain = document.createElement("div");
    contain.setAttribute("id", id);
    contain.setAttribute("class", "lw_map_new_marker_bubble");
    
    var textboxId = "lw_names_" + id;
    var buttonSaveId = "lw_new_marker_save_" + id;
    var buttonRemoveId = "lw_new_marker_remove_" + id;
    
    var autocompleteHandlers = 
      "onkeyup='onLabelTextboxKeyUp(event, \"" + textboxId + "\", \"" + buttonSaveId + "\", \"default\");' " +
      "onblur='onLabelTextboxBlur(event, \"" + buttonRemoveId + "\", \"default\");' ";
    
    var html = 
            "<div class='lw_map_new_marker_title'>";
    html +=   modes[currentMode]['typeTitle'] + ":<br/>";
    html +=   "<input " + autocompleteHandlers + "style='display: inline;' id='" + textboxId + "' size='20' type='text' />";
    html +=   "<div class='lw_map_new_marker_completion' id='lw_names_completion_" + id + "'>";
    html +=     "<select size='5' id='lw_names_completion_selector_" + id + "'></select>";
    html +=   "</div>";
    html +=   "<input id='lw_names_completion_uri_" + id + "' type='hidden' />";
    html += "</div>";
    html += "<div class='lw_map_new_marker_controls'>";
    html +=   "<button onclick='removePoint(this); return false;' id='" + buttonRemoveId + "'>";
    html +=     "Remove";
    html +=   "</button>";
    html +=   "<button onclick='setLabel(this); return false;' id='" + buttonSaveId + "'>";
    html +=     "Save " + modes[currentMode]['typeLabel'];
    html +=   "</button>";
    html += "</div>";
    
/*    if (navigator.appName != 'Microsoft Internet Explorer') {
        // ie uses absolute percentages; odd; works better w/o suggestions
        contain.style.width = "200%";
        labP.style.width = "100%";
        rem.style.width = "70%";
        set.style.width = "70%";
    }
*/
    contain.innerHTML = html;
    
    return contain;
}

function onLabelTextboxKeyUp(event, textboxId, buttonId, profileId) {
  f = function(text) {
    setLabel(document.getElementById(buttonId));
  }
  
  if (modes[currentMode]['autocomplete']) {
    Names.onNameInputKeyUp(event, document.getElementById(textboxId), profileId, f);
  } else if (event.keyCode == 13) {
    f();
  }
}

function onLabelTextboxBlur(event, buttonId, profileId) {
  if (modes[currentMode]['autocomplete']) {
    Names.onNameInputBlur(event, document.getElementById(buttonId), profileId);
  }
}

function addPoint(id, obj, info) {
    points[id + counter] = new Array();
    points[id + counter]['info'] = info;
    points[id + counter]['marker'] = obj;
    points[id + counter]['uri'] = '';
    points[id + counter++]['label'] = "";
}

function processRDFXML(p) {
    var answer = "";
    answer += "<?xml version='1.0'?>\n"
    answer += "<rdf:RDF xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'\n";
    answer += "  xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'\n";
    answer += "  xmlns:foaf='http://xmlns.com/foaf/0.1/'\n";
    answer += "  xmlns:loc='http://simile.mit.edu/2005/05/ontologies/location#'>\n";
    if (p != null) {
        if (p['uri'] != "") {
            answer += "<rdf:Description rdf:about='" + p['uri'] + "'>\n";
        } else {
            answer += "<rdf:Description>\n";
        }
        if (p['label'] != "") {
            for (var j = 0; j < modes[currentMode]['typeTitleProperty'].length; j++) {
                var prop = modes[currentMode]['typeTitleProperty'][j];
                answer += "<" + prop + ">" + p['label'] + "</" + prop + ">\n";
            }
        }
        answer += "<rdf:type rdf:resource='" + modes[currentMode]['type']  + "'/>\n";
        answer += "<loc:coordinates>" + p['marker'].getPoint().lat() + "," + p['marker'].getPoint().lng()  + "</loc:coordinates>\n";
        answer += "</rdf:Description>\n";
    }
    answer += "</rdf:RDF>\n";
    return answer;
}

// Help controls
function HelpControl() { }

HelpControl.prototype = new GControl();

HelpControl.prototype.initialize = function(map) {
    var container = document.createElement("div");
    container.setAttribute("id", "help");

    var togglerImg = new Image();
    togglerImg.src = "../../../images/help.png";
    togglerImg.setAttribute("id", "help-text-toggler");
    togglerImg.setAttribute("class", "on");
    GEvent.addDomListener(togglerImg, "click", function() {
        toggleHelp();
    });

    container.appendChild(togglerImg);

    var helpText = document.createElement("div");
    helpText.setAttribute("id", "help-text");
    helpText.innerHTML = "<strong>Edit this map!</strong> Click the map to place a marker, then click the new marker to fill in a label, and save to put it in the bank.<br /><br />Use the <em>Find Location</em> box to either recenter the map on a locale (<em>Find</em>) or recenter and place a new marker on the map (<em>Add</em>).";
    GEvent.addDomListener(helpText, "click", function() {
        toggleHelp();
    });

    container.appendChild(helpText);
    map.getContainer().appendChild(container);
    return container;
}

HelpControl.prototype.getDefaultPosition = function() {
    return new GControlPosition(G_ANCHOR_BOTTOM_RIGHT, new GSize(5, 20));
}

function toggleHelp() {
    var helpText = document.getElementById('help-text');
    var obj = document.getElementById('help-text-toggler');
    if (obj.getAttribute("class") == 'off') {
        obj.setAttribute("class", "on");
        helpText.style.display = 'block';
    } else {
        obj.setAttribute("class", "off");
        helpText.style.display = 'none';
    }
}

function locate(address, key, callback) {
    mainGeocoder.getOne(address, key, callback);
}

function addPointFromLatLong(point) {
  if (point) {
    var icon = new GIcon();
    icon.image = newPointIconUrl;
    icon.shadow = "http://www.google.com/mapfiles/shadow50.png";
    icon.iconSize = new GSize(40, 34);
    icon.shadowSize = new GSize(67, 34);
    icon.iconAnchor = new GPoint(19, 34);
    icon.infoWindowAnchor = new GPoint(20, 0);
    var marker = new GMarker(point, icon);
    Mmap.addOverlay(marker);
    addPoint(poi_id, marker);
    Mmap.setCenter(point, 8);
  } else {
    alert("Couldn't find requested location, check spelling");
  }
}

function recenter(point) {
  if (point) {
    Mmap.setCenter(point, 8);
  } else {
    alert("Couldn't find requested location, check spelling");
  }
}

// Geocoding client
function LongwellClientGeocoder(cache) {
    GClientGeocoder.apply(this);
    this.setCache(cache);
}

LongwellClientGeocoder.prototype.setCache = GClientGeocoder.prototype.setCache;

LongwellClientGeocoder.prototype.getCache = GClientGeocoder.prototype.getCache;

LongwellClientGeocoder.prototype.getLatLng = GClientGeocoder.prototype.getLatLng;

LongwellClientGeocoder.prototype.getOne = function(address, key, callback) {
    var cache = this.getCache();
    cache.getCB(address, key, callback);
};

// Geocoding cache
function LongwellGeocodeCache() {
    GFactualGeocodeCache.apply(this);
}

LongwellGeocodeCache.prototype = new GFactualGeocodeCache();

LongwellGeocodeCache.prototype.reset = function() {
    // do nothing, never reset the cache from here
};

LongwellGeocodeCache.prototype.isCachable = GFactualGeocodeCache.prototype.isCachable;

LongwellGeocodeCache.prototype.toCanonical = GFactualGeocodeCache.prototype.toCanonical;

LongwellGeocodeCache.prototype.get = function() {
    return null;
};

LongwellGeocodeCache.prototype.getCB = function(address, key, callback) {
    var canonical = this.toCanonical(address);
    var idx = document.location.href.indexOf("resources/");
    var url = document.location.href.substring(0, idx) + "default?command=address&key=" + key + "&location=" + encodeURIComponent(canonical);
    var onError = function() { };
    var onDone = function(response) {
        var r = eval('(' + response + ')');
        if (r && r.Status && r.Status.code==200 && r.Placemark) {
            callback(new GLatLng(r.Placemark[0].Point.coordinates[1],r.Placemark[0].Point.coordinates[0]));
        } else {
            callback(null);
        }
    };
    HTTPUtilities.doGet(url, onError, onDone);
}

LongwellGeocodeCache.prototype.put = function(address, reply) {
    // do nothing
}

function removeFromMap(elmt) {
  if (!confirm("Remove item from map?")) {
    return;
  }
    
  var onError = function(status) {
    alert("Failed to remove item from map.\nError: " + status);
  };
  var onDone = function(responseText) {
    document.location.reload(false);
  }
  HTTPUtilities.doPost(elmt.href, "", onError, onDone);
  return false;
}
