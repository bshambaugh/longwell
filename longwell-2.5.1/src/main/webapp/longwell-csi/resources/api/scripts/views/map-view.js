Longwell.UI.MapView = function(viewPanel, div, settings) {
    this._viewPanel = viewPanel;
    this._div = div;
    
    this._totalItemCount = 0;
    this._settings = {
        latlngIndirectPropertyURI:  null,
        latlngPropertyURI:          null,
        latPropertyURI:             null,
        lngPropertyURI:             null,
        
        colorIndirectPropertyURI:   null,
        colorPropertyURI:           null,
        
        mapHeight:                  400
    };
    
    if (settings != null) {
        for (field in this._settings) {
            if (field in settings) {
                this._settings[field] = settings[field];
            }
        }
    };
    
    this._xmlHttpQueue = new Longwell.XmlHttpQueue();
    
};

Longwell.UI.MapView.prototype.dispose = function() {
    this._viewPanel.getUI().getLongwell().getQE().unregisterListener(this);
    if (this._map != null) {
        this._map = null;
        GUnload();
    }
};

Longwell.UI.MapView.prototype.getSettings = function() {
    return this._dupSettings();
};

Longwell.UI.MapView.prototype.initialize = function() {
    if ("GBrowserIsCompatible" in window) {
        if (GBrowserIsCompatible()) {
            this._viewPanel.getUI().getLongwell().getQE().registerListener(this);
            this._constructUI();
            
            if (this._isSufficientlyConfigured()) {
                this._fetchItems();
            } else {
                this._openConfigureDialog();
            }
        } else {
            var messageDiv = document.createElement("div");
            messageDiv.className = "longwell-mapView-errorMessage";
            messageDiv.innerHTML = "Sorry, Google Maps does not support your browser.";
            
            this._div.appendChild(messageDiv);
        }
    } else {
        var messageDiv = document.createElement("div");
        messageDiv.className = "longwell-mapView-errorMessage";
        messageDiv.innerHTML = 
            "Unable to embed Google Maps. " +
            "Perhaps this site has not registered for a Google Maps API key. " +
            "Tell this site's admin to follow <a href='http://simile.mit.edu/wiki2/Longwell_FAQ#How_do_I_make_Longwell_show_maps.3F'>these instructions</a> to enable maps.";
        
        this._div.appendChild(messageDiv);
    }
};

Longwell.UI.MapView.prototype._isSufficientlyConfigured = function() {
    return this._settings.latlngPropertyURI != null ||
        (this._settings.latPropertyURI != null &&
         this._settings.lngPropertyURI != null);
};

Longwell.UI.MapView.prototype._constructUI = function() {
    var ui = this._viewPanel.getUI();
    
    var progressAnimation = Longwell.Graphics.createAnimationIcon();
    this._xmlHttpQueue.setAnimation(progressAnimation);
    
    var configured = this._isSufficientlyConfigured();
    var colorConfigured = this._settings.colorPropertyURI != null;
    
    var template = {
        elmt: this._div,
        children: [
            {   tag: "div",
                className: "longwell-mapView-header",
                children: [
                    {   tag: "div",
                        className: "longwell-mapView-header-notConfigured",
                        style: "display: " + (configured ? "none" : "block"),
                        field: "notConfiguredDiv",
                        children: [
                            "You need to ",
                            { elmt: ui.createActionLink("configure", this, this._openConfigureDialog, 0) },
                            " this map before you see any results."
                        ]
                    },
                    {   tag: "div",
                        style: "display: " + (configured ? "block" : "none"),
                        field: "configuredDiv",
                        children: [
                            { elmt: progressAnimation.element },
                            {   tag: "span",
                                field: "summarySpan"
                            },
                            " (",
                            { elmt: ui.createActionLink("reconfigure", this, this._openConfigureDialog, 0) },
                            ")"
                        ]
                    }
                ]
            },
            {   tag: "div",
                className: "longwell-mapView-map",
                style: "height: " + this._settings.mapHeight + "px",
                field: "mapDiv"
            },
            {   tag: "div",
                className: "longwell-mapView-resizer",
                field: "resizeDiv"
            },
            {   tag: "div",
                className: "longwell-mapView-footer",
                style: "display: " + (configured ? "block" : "none"),
                field: "footerDiv",
                children: [
                    {   tag: "div",
                        className: "longwell-mapView-colorPanel",
                        style: "display: " + (colorConfigured ? "none" : "block"),
                        field: "colorNotConfiguredDiv",
                        children: [
                            { elmt: ui.createActionLink("Show items in different colors", this, this._openColorDialog, 0) },
                        ]
                    },
                    {   tag: "div",
                        className: "longwell-mapView-colorPanel",
                        style: "display: " + (colorConfigured ? "block" : "none"),
                        field: "colorConfiguredDiv",
                        children: [
                            {   tag: "div",
                                className: "longwell-mapView-colorLegend",
                                field: "colorLegendDiv"
                            },
                            {   tag: "p",
                                style: "clear: both"
                            },
                            { elmt: ui.createActionLink("Change coloring scheme", this, this._openColorDialog, 0) },
                        ]
                    }
                ]
            }
        ]
    };
    this._dom = Longwell.DOM.createDOMFromTemplate(document, template);
    
    this._map = new GMap2(this._dom.mapDiv);
    this._map.enableDoubleClickZoom();
    this._map.enableContinuousZoom();
    
    this._map.addControl(new GSmallMapControl());
    this._map.addControl(new GMapTypeControl());
    this._map.addControl(new GScaleControl());
    this._map.setCenter(new GLatLng(20, 0), 2);
        
    ui.registerForDragging(this._dom.resizeDiv, this);
};

Longwell.UI.MapView.prototype._openConfigureDialog = function(elmt, evt, target) {
    var dialog = Longwell.Graphics.createPopupDialog(this._viewPanel.getUI(), "Configure Map");
    
    var mv = this;
    var insertRadioRow = function(table, name, value, checked, text) {
        var tr = table.insertRow(table.rows.length);
        var td = tr.insertCell(0);
        td.align = "right";
        
        var radio = Longwell.DOM.createRadioButton(name, value, checked);
        td.appendChild(radio);
        
        td = tr.insertCell(1);
        td.appendChild(document.createTextNode(text));
        
        return radio;
    };
    
    var insertPropertySelect = function(table, text, likelyProperty) {
        var tr = table.insertRow(table.rows.length);
        var td = tr.insertCell(0);
        td = tr.insertCell(1);
        
        var select = document.createElement("select");
        select.size = 1;
        mv._fillSelectWithProperties(select, likelyProperty);
        
        td.appendChild(select);
        td.appendChild(document.createTextNode(text));
        
        return select;
    };
    
    var createSection = function(text) {
        var p = document.createElement("p");
        p.className = "section";
        dialog.bodyDiv.appendChild(p);
        
        p.appendChild(document.createTextNode(text));
        p.appendChild(document.createElement("br"));
        
        return p;
    };
    
    dialog.dialogDiv.style.left = "30%";
    dialog.dialogDiv.style.right = "30%";
    dialog.dialogDiv.style.top = "15em";
    
        var section, table;
        
        section = createSection("Latitudes and longitudes are encoded...");
        table = document.createElement("table");
        section.appendChild(table);
            var singleFieldRadio = insertRadioRow(table, "encoding", "pair", true, "as pairs in a single field");
            var latlngFieldSelect = insertPropertySelect(table, " lat,lng field, e.g. \"-59.67,13.98\"", "isLatLong");
            
            var separateFieldsRadio = insertRadioRow(table, "encoding", "separate", false, "in 2 separate fields");
            var latFieldSelect = insertPropertySelect(table, " latitutde field, e.g., \"-59.67\"", "isNumeric");
            var lngFieldSelect = insertPropertySelect(table, " longitude field, e.g., \"13.98\"", "isNumeric");
                
        section = createSection("Latitude/Longitude data is associated...");
        table = document.createElement("table");
        section.appendChild(table);
            var directRadio = insertRadioRow(table, "direct-indirect", "direct", true, "directly with the items to be plotted");
            var indirectRadio = insertRadioRow(table, "direct-indirect", "indirect", false, "indirectly through something else related to these items");
            var relatedFieldSelect = insertPropertySelect(table, " related field");
            
    dialog.onOK = function() {
        mv._dom.elmt.scrollIntoView();
        
        var settings = mv._dupSettings();
        if (singleFieldRadio.checked) {
            settings.latPropertyURI = null;
            settings.lngPropertyURI = null;
            settings.latlngPropertyURI = latlngFieldSelect.value;
        } else {
            settings.latPropertyURI = latFieldSelect.value;
            settings.lngPropertyURI = lngFieldSelect.value;
            settings.latlngPropertyURI = null;
        }
        
        if (directRadio.checked) {
            settings.latlngIndirectPropertyURI = null;
        } else {
            settings.latlngIndirectPropertyURI = relatedFieldSelect.value;
        }
        
        var oldSettings = this._settings;
        mv._viewPanel.getUI().getLongwell().getHistory().addAction({
            perform: function() {
                mv._applySettings(settings);
            },
            undo: function() {
                mv._applySettings(oldSettings);
            }
        });
    };
    
    dialog.open();
    
    if (evt != null) {
        Longwell.DOM.cancelEvent(evt);
        return false;
    }
};

Longwell.UI.MapView.prototype._openColorDialog = function(elmt, evt, target) {
    var dialog = Longwell.Graphics.createPopupDialog(this._viewPanel.getUI(), "Configure Colors");
    
    var mv = this;
    var insertRadioRow = function(table, name, value, checked, text) {
        var tr = table.insertRow(table.rows.length);
        var td = tr.insertCell(0);
        td.align = "right";
        
        var radio = Longwell.DOM.createRadioButton(name, value, checked);
        td.appendChild(radio);
        
        td = tr.insertCell(1);
        td.appendChild(document.createTextNode(text));
        
        return radio;
    };
    
    var insertPropertySelect = function(table, text, likelyProperty) {
        var tr = table.insertRow(table.rows.length);
        var td = tr.insertCell(0);
        td = tr.insertCell(1);
        
        var select = document.createElement("select");
        select.size = 1;
        mv._fillSelectWithProperties(select, likelyProperty);
        
        td.appendChild(select);
        td.appendChild(document.createTextNode(text));
        
        return select;
    };
    
    var createSection = function(text) {
        var p = document.createElement("p");
        p.className = "section";
        dialog.bodyDiv.appendChild(p);
        
        p.appendChild(document.createTextNode(text));
        p.appendChild(document.createElement("br"));
        
        return p;
    };
    
    dialog.dialogDiv.style.left = "30%";
    dialog.dialogDiv.style.right = "30%";
    dialog.dialogDiv.style.top = "15em";
    
        var section, table;
        
        section = createSection("Color items by a property...");
        table = document.createElement("table");
        section.appendChild(table);
            var directRadio = insertRadioRow(table, "direct-indirect", "direct", true, "of the items themselves");
            var indirectRadio = insertRadioRow(table, "direct-indirect", "indirect", false, "of something else related to these items");
            var relatedFieldSelect = insertPropertySelect(table, " related field");
            
        section = createSection("Choose the property to color by:");
        table = document.createElement("table");
        section.appendChild(table);
            var colorPropertySelect = insertPropertySelect(table, " color key");
                
    dialog.onOK = function() {
        mv._dom.elmt.scrollIntoView();
        
        var settings = mv._dupSettings();
        settings.colorPropertyURI = colorPropertySelect.value;
        
        if (directRadio.checked) {
            settings.colorIndirectPropertyURI = null;
        } else {
            settings.colorIndirectPropertyURI = relatedFieldSelect.value;
        }
        
        var oldSettings = this._settings;
        mv._viewPanel.getUI().getLongwell().getHistory().addAction({
            perform: function() {
                mv._applySettings(settings);
            },
            undo: function() {
                mv._applySettings(oldSettings);
            }
        });
    };
    
    dialog.open();
    
    if (evt != null) {
        Longwell.DOM.cancelEvent(evt);
        return false;
    }
};

Longwell.UI.MapView.prototype.onCurrentFacetsChange = function() {
    if (this._isSufficientlyConfigured()) {
        this._fetchItems();
    }
};

Longwell.UI.MapView.prototype._fetchItems = function() {
    var s = this._makeFetchMapItemsRequest(this._settings);
    
    var mv = this;
    this._xmlHttpQueue.queue(function(cont) {
        mv._viewPanel.getUI().getLongwell().callAPI({
            call:   "fetch-map-items",
            body:   s,
            fDone:  function(o) {
                try {
                    mv._onFetchItems(o);
                } finally {
                    cont();
                }
            },
            fError: function(statusText, status, xmlhttp) {
                Longwell.Debug.log(statusText);
                cont();
            }
        });
    });
};

Longwell.UI.MapView.prototype._applySettings = function(settings) {
    var s = this._makeFetchMapItemsRequest(settings);
    
    var mv = this;
    this._xmlHttpQueue.queue(function(cont) {
        mv._viewPanel.getUI().getLongwell().callAPI({
            call:   "fetch-map-items",
            body:   s,
            fDone:  function(o) {
                try {
                    mv._settings = settings;
                    if (mv._isSufficientlyConfigured()) {
                        mv._dom.configuredDiv.style.display = "block";
                        mv._dom.notConfiguredDiv.style.display = "none";
                        mv._dom.footerDiv.style.display = "block";
                        
                        var colorConfigured = mv._settings.colorPropertyURI != null;
                        mv._dom.colorNotConfiguredDiv.style.display = colorConfigured ? "none" : "block";
                        mv._dom.colorConfiguredDiv.style.display = colorConfigured ? "block" : "none";
                        
                        mv._onFetchItems(o);
                    } else {
                        mv._dom.configuredDiv.style.display = "none";
                        mv._dom.notConfiguredDiv.style.display = "block";
                        mv._dom.footerDiv.style.display = "none";
                    }
                } finally {
                    cont();
                }
            },
            fError: function(statusText, status, xmlhttp) {
                Longwell.Debug.log(statusText);
                cont();
            }
        });
    });
};

Longwell.UI.MapView._colors = [
    "254,119,107", 
    "33,126,174",
    "59,137,109", 
    "252,103,194"
];

Longwell.UI.MapView.prototype._onFetchItems = function(data) {
    var bounds = new GLatLngBounds();
    var mappable = 0;
    var locationMap = [];
    var colorKeys = [];
    
    this._map.clearOverlays();
    for (var i = 0; i < data.items.length; i++) {
        var item = data.items[i];
        if ("uri" in item) {
            var lat, lng;
            if ("latlng" in item) {
                var a = item.latlng.split(",");
                lat = a.length > 0 ? parseFloat(a[0]) : null;
                lng = a.length > 1 ? parseFloat(a[1]) : null;
            } else {
                lat = "lat" in item ? parseFloat(item.lat) : null;
                lng = "lng" in item ? parseFloat(item.lng) : null;
            }
            
            if (typeof lat == "number" && typeof lng == "number" && !isNaN(lat) && !isNaN(lng)) {
                var key = lat + "," + lng;
                var colorKey = this._settings.colorPropertyURI == null ? "" :
                    ("colorKey" in item && item.colorKey != null ? 
                        item.colorKey : "(missing data)");
                    
                colorKeys[colorKey] = true;
                
                var info = locationMap[key];
                if (info == null) {
                    info = { lat: lat, lng: lng, items: [], colorKey: colorKey };
                    locationMap[key] = info;
                } else {
                    if (info.colorKey != colorKey) {
                        info.colorKey = "(mixed)";
                    }
                }
                info.items.push(item);
                mappable++;
            }
        }
    }
    
    var iconSize = new GSize(40, 34);
    var iconAnchor = new GPoint(20, 34);
    var shadowSize = new GSize(50, 34);
    var infoWindowAnchor = new GPoint(20, 1);
    var imageMap = [ 6,0, 6,22, 15,22, 20,34, 25,25, 34,22, 34,0 ];
    
    var makeIcon = function(label, color) {
        var icon = new GIcon(G_DEFAULT_ICON);
        icon.image = Longwell.Configuration.resourcePath + "marker?x=0&y=0&s=1&w=40&h=34&label=" + encodeURIComponent(label) + "&rgb=" + color;
        icon.shadow = Longwell.urlPrefix + "images/map-marker-shadow.png";
        icon.iconSize = iconSize;
        icon.iconAnchor = iconAnchor;
        icon.imageMap = imageMap;
        icon.shadowSize = shadowSize;
        icon.infoWindowAnchor = infoWindowAnchor;
        return icon;
    };
    
    var colorMap = [];
    var iconMap = [];
    var usedColor = 0;
    
    colorMap["(mixed)"] = "255,255,255";
    iconMap["(mixed)"] = makeIcon(" ", "255,255,255");
    colorMap["(missing data)"] = "64,64,64";
    iconMap["(missing data)"] = makeIcon(" ", "64,64,64");
    delete colorKeys["(mixed)"];
    delete colorKeys["(missing data)"];
    
    for (colorKey in colorKeys) {
        if (colorKey != "toJSONString") {
            var color = usedColor < Longwell.UI.MapView._colors.length ?
                Longwell.UI.MapView._colors[usedColor++] : "128,128,128";
            colorMap[colorKey] = color;
            iconMap[colorKey] = makeIcon(" ", color);
        }
    }
    
    var ui = this._viewPanel.getUI();
    var makeMarker = function(info) {
        var count = info.items.length;
        
        var links = document.createElement("ul");
        for (var i = 0; i < info.items.length; i++) {
            var item = info.items[i];
            var li = document.createElement("li");
            li.appendChild(ui.createElementForURI(item.uri, item.label));
            links.appendChild(li);
        }
        
        var point = new GLatLng(info.lat, info.lng);
        var marker = new GMarker(point, 
            count == 1 ? iconMap[info.colorKey] : makeIcon(count, colorMap[info.colorKey]));
        GEvent.addListener(marker, "click", function() { marker.openInfoWindow(links); });

        bounds.extend(point);
        return marker;
    }
    
    if (mappable < 100 || window.confirm("There are " + mappable + " items to map. This will take a while. Proceed?")) {
        this._dom.mapDiv.style.display = "none";
        try {
            for (key in locationMap) {
                var info = locationMap[key];
                if (typeof info == "object") {
                    var marker = makeMarker(info);
                    this._map.addOverlay(marker);
                }
            }
        } finally {
            this._dom.mapDiv.style.display = "block";
        }
    }
    
    if (!bounds.isEmpty()) {
        var zoom = this._map.getBoundsZoomLevel(bounds);
        var center = bounds.getCenter();
        this._map.setCenter(center, zoom);
        this._map.savePosition();
    }
    
    var summarySpan = this._dom.summarySpan;
    summarySpan.innerHTML = "";
    
    var colorLegendDiv = this._dom.colorLegendDiv;
    colorLegendDiv.innerHTML = "";
    
    if (mappable == data.totalItemCount) {
        var countSpan = document.createElement("span");
        countSpan.className = "item-count";
        countSpan.appendChild(document.createTextNode(mappable));
        summarySpan.appendChild(countSpan);
        
        summarySpan.appendChild(document.createTextNode(mappable > 1 ? " items" : " item"));
    } else {
        var countSpan = document.createElement("span");
        countSpan.className = "item-count";
        countSpan.appendChild(document.createTextNode(data.totalItemCount));
        summarySpan.appendChild(countSpan);
        summarySpan.appendChild(document.createTextNode(data.totalItemCount > 1 ? " items, " : " item, "));
        
        var mappableSpan = document.createElement("span");
        mappableSpan.className = "item-count";
        mappableSpan.appendChild(document.createTextNode(mappable));
        summarySpan.appendChild(mappableSpan);
        summarySpan.appendChild(document.createTextNode(" can be mapped"));
    }
    
    for (colorKey in colorMap) {
        var color = colorMap[colorKey];
        if (typeof color == "string") {
            var entryDiv = document.createElement("div");
            var image = Longwell.Configuration.resourcePath + "marker?x=0&y=0&s=0.75&w=30&h=28&label=%20&rgb=" + color;
            entryDiv.className = "entry";
            entryDiv.style.background = "url(" + image + ") top left no-repeat";
            entryDiv.appendChild(document.createTextNode(colorKey));
            
            colorLegendDiv.appendChild(entryDiv);
        }
    }
};

Longwell.UI.MapView.prototype._makeFetchMapItemsRequest = function(settings) {
    var s = "<fetch>\n";
    s += this._viewPanel.getUI().getLongwell().getQE().getXML("query", " ");
    
    s += " <properties\n";
    if (settings.latlngIndirectPropertyURI != null) {
        s += "  latlngIndirectPropertyURI='" + settings.latlngIndirectPropertyURI + "'\n";
    }
    if (settings.latlngPropertyURI != null) {
        s += " latlngPropertyURI='" + settings.latlngPropertyURI + "'\n";
    } else {
        s += " latPropertyURI='" + settings.latPropertyURI + "'\n";
        s += " lngPropertyURI='" + settings.lngPropertyURI + "'\n";
    }
    if (settings.colorIndirectPropertyURI != null) {
        s += "  colorIndirectPropertyURI='" + settings.colorIndirectPropertyURI + "'\n";
    }
    if (settings.colorPropertyURI != null) {
        s += "  colorPropertyURI='" + settings.colorPropertyURI + "'\n";
    }
    s += "  />\n";
    s += "</fetch>";
    
    return s;
};

Longwell.UI.MapView.prototype._dupSettings = function() {
    var settings = {};
    for (field in this._settings) {
        settings[field] = this._settings[field];
    }
    return settings;
};

Longwell.UI.MapView.prototype._fillSelectWithProperties = function(select, likelyProperty) {
    var properties = [].concat(Longwell.ProfileData.properties);
    properties.sort(function(p1, p2) {
        return p1.label.localeCompare(p2.label);
    });
    
    for (var i = 0; i < properties.length; i++) {
        var property = properties[i];
        var option = document.createElement("option");
        option.text = property.label;
        option.value = property.uri;
        if (likelyProperty != null && property[likelyProperty] > 0.5) {
            option.setAttribute("selected", "true");
        }
        try {
            select.add(option);
        } catch (e) {
            select.add(option, null);
        }
    }
};

Longwell.UI.MapView.prototype.onDragStart = function() {
};

Longwell.UI.MapView.prototype.onDragBy = function(x, y) {
    this._settings.mapHeight = Math.max(this._settings.mapHeight + y, 150);
    this._dom.mapDiv.style.height = this._settings.mapHeight + "px";
    this._map.checkResize();
};

Longwell.UI.MapView.prototype.onDragEnd = function() {
};
