Longwell.UI.TimelineView = function(viewPanel, div, settings) {
    this._viewPanel = viewPanel;
    this._div = div;
    
    this._totalItemCount = 0;
    this._settings = {
        timeIndirectPropertyURI:    null,
        startPropertyURI:           null,
        endPropertyURI:             null,
        
        colorIndirectPropertyURI:   null,
        colorPropertyURI:           null,
        
        timelineHeight:                  400
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

Longwell.UI.TimelineView.prototype.dispose = function() {
    this._viewPanel.getUI().getLongwell().getQE().unregisterListener(this);
    if (this._timeline != null) {
        this._timeline = null;
    }
};

Longwell.UI.TimelineView.prototype.getSettings = function() {
    return this._dupSettings();
};

Longwell.UI.TimelineView.prototype.initialize = function() {
    this._viewPanel.getUI().getLongwell().getQE().registerListener(this);
    this._constructUI();
    
    if (this._isSufficientlyConfigured()) {
        this._fetchItems();
    } else {
        this._openConfigureDialog();
    }
};

Longwell.UI.TimelineView.prototype._isSufficientlyConfigured = function() {
    return this._settings.startPropertyURI != null;
};

Longwell.UI.TimelineView.prototype._constructUI = function() {
    var ui = this._viewPanel.getUI();
    
    var progressAnimation = Longwell.Graphics.createAnimationIcon();
    this._xmlHttpQueue.setAnimation(progressAnimation);
    
    var configured = this._isSufficientlyConfigured();
    var colorConfigured = this._settings.colorPropertyURI != null;
    
    var template = {
        elmt: this._div,
        children: [
            {   tag: "div",
                className: "longwell-timelineView-header",
                children: [
                    {   tag: "div",
                        className: "longwell-timelineView-header-notConfigured",
                        style: "display: " + (configured ? "none" : "block"),
                        field: "notConfiguredDiv",
                        children: [
                            "You need to ",
                            { elmt: ui.createActionLink("configure", this, this._openConfigureDialog, 0) },
                            " this timeline before you see any results."
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
                className: "longwell-timelineView-timeline",
                style: "height: " + this._settings.timelineHeight + "px",
                field: "timelineDiv"
            },
            {   tag: "div",
                className: "longwell-timelineView-resizer",
                field: "resizeDiv"
            },
            {   tag: "div",
                className: "longwell-timelineView-footer",
                style: "display: " + (configured ? "block" : "none"),
                field: "footerDiv",
                children: [
                    {   tag: "div",
                        className: "longwell-timelineView-colorPanel",
                        style: "display: " + (colorConfigured ? "none" : "block"),
                        field: "colorNotConfiguredDiv",
                        children: [
                            { elmt: ui.createActionLink("Show items in different colors", this, this._openColorDialog, 0) },
                        ]
                    },
                    {   tag: "div",
                        className: "longwell-timelineView-colorPanel",
                        style: "display: " + (colorConfigured ? "block" : "none"),
                        field: "colorConfiguredDiv",
                        children: [
                            {   tag: "div",
                                className: "longwell-timelineView-colorLegend",
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
    
    ui.registerForDragging(this._dom.resizeDiv, this);
};

Longwell.UI.TimelineView.prototype._openConfigureDialog = function(elmt, evt, target) {
    var dialog = Longwell.Graphics.createPopupDialog(this._viewPanel.getUI(), "Configure Map");
    
    var tv = this;
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
    
    var insertPropertySelect = function(table, text, likelyProperty, optional) {
        var tr = table.insertRow(table.rows.length);
        var td = tr.insertCell(0);
        td = tr.insertCell(1);
        
        var select = document.createElement("select");
        select.size = 1;
        tv._fillSelectWithProperties(select, likelyProperty);
        
        if (optional) {
            var option = document.createElement("option");
            option.text = "(not available)";
            option.value = "";
            try {
                select.add(option);
            } catch (e) {
                select.add(option, null);
            }
        }
        
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
        
        section = createSection("Start time and end time are encoded in...");
        table = document.createElement("table");
        section.appendChild(table);
            var startFieldSelect = insertPropertySelect(table, " start time field", "isDateTime", false);
            var endFieldSelect = insertPropertySelect(table, " end time field", "isDateTime", true);
                
        section = createSection("Time data is associated...");
        table = document.createElement("table");
        section.appendChild(table);
            var directRadio = insertRadioRow(table, "direct-indirect", "direct", true, "directly with the items to be plotted");
            var indirectRadio = insertRadioRow(table, "direct-indirect", "indirect", false, "indirectly through something else related to these items");
            var relatedFieldSelect = insertPropertySelect(table, " related field");
            
    dialog.onOK = function() {
        tv._dom.elmt.scrollIntoView();
        
        var settings = tv._dupSettings();
        settings.startPropertyURI = startFieldSelect.value;
        settings.endPropertyURI = endFieldSelect.value.length > 0 ? endFieldSelect.value : null;
        
        if (directRadio.checked) {
            settings.timeIndirectPropertyURI = null;
        } else {
            settings.timeIndirectPropertyURI = relatedFieldSelect.value;
        }
        
        var oldSettings = this._settings;
        tv._viewPanel.getUI().getLongwell().getHistory().addAction({
            perform: function() {
                tv._applySettings(settings);
            },
            undo: function() {
                tv._applySettings(oldSettings);
            }
        });
    };
    
    dialog.open();
    
    if (evt != null) {
        Longwell.DOM.cancelEvent(evt);
        return false;
    }
};

Longwell.UI.TimelineView.prototype._openColorDialog = function(elmt, evt, target) {
    var dialog = Longwell.Graphics.createPopupDialog(this._viewPanel.getUI(), "Configure Colors");
    
    var tv = this;
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
        tv._fillSelectWithProperties(select, likelyProperty);
        
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
        tv._dom.elmt.scrollIntoView();
        
        var settings = tv._dupSettings();
        settings.colorPropertyURI = colorPropertySelect.value;
        
        if (directRadio.checked) {
            settings.colorIndirectPropertyURI = null;
        } else {
            settings.colorIndirectPropertyURI = relatedFieldSelect.value;
        }
        
        var oldSettings = this._settings;
        tv._viewPanel.getUI().getLongwell().getHistory().addAction({
            perform: function() {
                tv._applySettings(settings);
            },
            undo: function() {
                tv._applySettings(oldSettings);
            }
        });
    };
    
    dialog.open();
    
    if (evt != null) {
        Longwell.DOM.cancelEvent(evt);
        return false;
    }
};

Longwell.UI.TimelineView.prototype.onCurrentFacetsChange = function() {
    if (this._isSufficientlyConfigured()) {
        this._fetchItems();
    }
};

Longwell.UI.TimelineView.prototype._fetchItems = function() {
    var s = this._makeFetchTimelineItemsRequest(this._settings);
    
    var tv = this;
    this._xmlHttpQueue.queue(function(cont) {
        tv._viewPanel.getUI().getLongwell().callAPI({
            call:   "fetch-timeline-items",
            body:   s,
            fDone:  function(o) {
                try {
                    tv._onFetchItems(o);
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

Longwell.UI.TimelineView.prototype._applySettings = function(settings) {
    var s = this._makeFetchTimelineItemsRequest(settings);
    
    var tv = this;
    this._xmlHttpQueue.queue(function(cont) {
        tv._viewPanel.getUI().getLongwell().callAPI({
            call:   "fetch-timeline-items",
            body:   s,
            fDone:  function(o) {
                try {
                    tv._settings = settings;
                    if (tv._isSufficientlyConfigured()) {
                        tv._dom.configuredDiv.style.display = "block";
                        tv._dom.notConfiguredDiv.style.display = "none";
                        tv._dom.footerDiv.style.display = "block";
                        
                        var colorConfigured = tv._settings.colorPropertyURI != null;
                        tv._dom.colorNotConfiguredDiv.style.display = colorConfigured ? "none" : "block";
                        tv._dom.colorConfiguredDiv.style.display = colorConfigured ? "block" : "none";
                        
                        tv._onFetchItems(o);
                    } else {
                        tv._dom.configuredDiv.style.display = "none";
                        tv._dom.notConfiguredDiv.style.display = "block";
                        tv._dom.footerDiv.style.display = "none";
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

Longwell.UI.TimelineView._styles = null;

Longwell.UI.TimelineView.prototype._defineStyles = function() {
    if (Longwell.UI.TimelineView._styles == null) {
        Longwell.UI.TimelineView._styles = [
            {   icon:       Timeline.urlPrefix + "images/dull-blue-circle.png",
                color:      "#8085BC"
            },
            {   icon:       Timeline.urlPrefix + "images/dull-red-circle.png",
                color:      "#BA6B66"
            },
            {   icon:       Timeline.urlPrefix + "images/dull-green-circle.png",
                color:      "#6FBC6E"
            },
            {   icon:       Timeline.urlPrefix + "images/blue-circle.png",
                color:      "#3734C1"
            },
            {   icon:       Timeline.urlPrefix + "images/red-circle.png",
                color:      "#B94643"
            },
            {   icon:       Timeline.urlPrefix + "images/green-circle.png",
                color:      "#27C82A"
            }
        ];
    }
};

Longwell.UI.TimelineView.prototype._onFetchItems = function(data) {
    Longwell.UI.TimelineView.prototype._defineStyles();
    var styles = Longwell.UI.TimelineView._styles;
    
    /*
     *  Parse items and assign styles
     */
    var mappable = 0;
    
    var eventIndex = new Timeline.EventIndex();
    
    var nextStyle = 0;
    var colorKeys = [];
    var styleAssignment = [];
    
    for (var i = 0; i < data.items.length; i++) {
        var item = data.items[i];
        if ("uri" in item) {
            var startDate = Timeline.DateTime.parseIso8601DateTime(item.start);
            var endDate = Timeline.DateTime.parseIso8601DateTime(item.end);
            if (startDate == null) {
                startDate = endDate;
                endDate = null;
            }
            if (startDate == null) {
                continue;
            }
            if (endDate != null) {
                if (endDate.getTime() < startDate.getTime()) {
                    var tempDate = startDate;
                    startDate = endDate;
                    endDate = tempDate;
                }
            }
            
            var style;
            var colorKey = ("colorKey" in item && item.colorKey != null) ? (item.colorKey.length > 0 ? item.colorKey : "(empty string)") : "(missing)"
            if (colorKey in styleAssignment) {
                style = styleAssignment[colorKey];
            } else {
                var styleIndex = (nextStyle < styles.length) ? nextStyle++ : (styles.length - 1);
                style = styles[styleIndex];
                styleAssignment[colorKey] = style;
                
                colorKeys.push(colorKey);
            }
            
            var evt = new Timeline.DefaultEventSource.Event(
                startDate,
                endDate,
                null,
                null,
                endDate == null,
                item.label,
                "",             // preview text
                null,           // image
                null,           // link
                style.icon,     // icon
                style.color,    // color
                null            // text color
            );
            eventIndex.add(evt);
                
            mappable++;
        }
    }
    
    /*
     *  Configure timeline
     */
    eventSource = new Timeline.DefaultEventSource(eventIndex);
    
    var earliest = eventSource.getEarliestDate();
    var latest = eventSource.getLatestDate();
    var duration = latest.getTime() - earliest.getTime();
    
    var intervalUnit = Timeline.DateTime.MILLENNIUM;
    while (intervalUnit > 0) {
        var intervalLength = Timeline.DateTime.gregorianUnitLengths[intervalUnit];
        if (duration / intervalLength > 50) {
            break;
        }
        intervalUnit--;
    }

    centerDate = new Date(Math.round((earliest.getTime() + latest.getTime()) / 2));
    var bandInfos = [
        Timeline.createBandInfo({
            width:          "75%", 
            intervalUnit:   intervalUnit, 
            intervalPixels: 150,
            eventSource:    eventSource,
            date:           centerDate
        }),
        Timeline.createBandInfo({
            width:          "25%", 
            intervalUnit:   intervalUnit + 1, 
            intervalPixels: 200,
            eventSource:    eventSource,
            date:           centerDate,
            showEventText:  false, 
            trackHeight:    0.5,
            trackGap:       0.2
        })
    ];
    bandInfos[1].syncWith = 0;
    bandInfos[1].highlight = true;
    bandInfos[1].eventPainter.setLayout(bandInfos[0].eventPainter.getLayout());

    this._timeline = Timeline.create(this._dom.timelineDiv, bandInfos, Timeline.HORIZONTAL);
    
    /*
     *  Update summary information
     */
    var summarySpan = this._dom.summarySpan;
    summarySpan.innerHTML = "";
    
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
    
    /*
     *  Create legend
     */
    var colorLegendDiv = this._dom.colorLegendDiv;
    colorLegendDiv.innerHTML = "";
    
    var tableLegend = document.createElement("table");
    for (var i = 0; i < colorKeys.length; i++) {
        var tr = tableLegend.insertRow(i);
        
        var td = tr.insertCell(0);
        var divColor = document.createElement("div");
        divColor.style.width = "2em";
        divColor.style.height = "1em";
        divColor.style.background = styleAssignment[colorKeys[i]].color;
        td.appendChild(divColor);
        
        td = tr.insertCell(1);
        td.innerHTML = colorKeys[i];
    }
    colorLegendDiv.appendChild(tableLegend);
};

Longwell.UI.TimelineView.prototype._makeFetchTimelineItemsRequest = function(settings) {
    var s = "<fetch>\n";
    s += this._viewPanel.getUI().getLongwell().getQE().getXML("query", " ");
    
    s += " <properties\n";
    if (settings.timeIndirectPropertyURI != null) {
        s += "  timeIndirectPropertyURI='" + settings.timeIndirectPropertyURI + "'\n";
    }
    s += " startPropertyURI='" + settings.startPropertyURI + "'\n";
    if (settings.endPropertyURI != null) {
        s += " endPropertyURI='" + settings.endPropertyURI + "'\n";
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

Longwell.UI.TimelineView.prototype._dupSettings = function() {
    var settings = {};
    for (field in this._settings) {
        settings[field] = this._settings[field];
    }
    return settings;
};

Longwell.UI.TimelineView.prototype._fillSelectWithProperties = function(select, likelyProperty) {
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

Longwell.UI.TimelineView.prototype.onDragStart = function() {
};

Longwell.UI.TimelineView.prototype.onDragBy = function(x, y) {
    this._settings.timelineHeight = Math.max(this._settings.timelineHeight + y, 150);
    this._dom.timelineDiv.style.height = this._settings.timelineHeight + "px";
};

Longwell.UI.TimelineView.prototype.onDragEnd = function() {
    if ("_timeline" in this) {
        this._timeline.layout();
    }
};
