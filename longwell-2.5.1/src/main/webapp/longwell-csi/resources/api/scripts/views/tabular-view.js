Longwell.UI.TabularView = function(viewPanel, div, settings) {
    this._viewPanel = viewPanel;
    this._div = div;
    
    this._itemsPerPage = 20;
    this._startIndex = 0;
    this._endIndex = this._startIndex + this._itemsPerPage;
    this._totalItemCount = 0;
    
    this._settings = {
        sortPropertyURI:    null,
        sortAscending:      true,
        columnPropertyURIs: []
    };
    if (settings != null) {
        if ("sortPropertyURI" in settings) {
            this._settings.sortPropertyURI = settings.sortPropertyURI;
        }
        if ("sortAscending" in settings) {
            this._settings.sortAscending = settings.sortAscending;
        }
        if ("columnPropertyURIs" in settings) {
            this._settings.columnPropertyURIs = settings.columnPropertyURIs;
        }
    };
    
    this._xmlHttpQueue = new Longwell.XmlHttpQueue();
    this._constructUI();
};

Longwell.UI.TabularView.prototype._constructUI = function() {
    var progressAnimation = Longwell.Graphics.createAnimationIcon();
    this._xmlHttpQueue.setAnimation(this._progressAnimation);
    
    var ui = this._viewPanel.getUI();
    
    /*
     *  Summary controls
     */
    var summaryTemplate = {
        tag: "table",
        className: "longwell-tabularView-header",
        children: [
            {   tag: "tr",
                children: [
                    {   tag: "td",
                        width: "0",
                        children: [
                            { elmt: progressAnimation.element }
                        ]
                    },
                    {   tag: "td",
                        children: [
                            {   tag: "span",
                                field: "summarySpan"
                            }
                        ]
                    },
                    {   tag: "td",
                        field: "pagingControlsTD",
                        children: [
                            {   elmt:  ui.createActionLink("\u00ab", this, this._onFirstPageClick, 0),
                                field: "firstLink"
                            },
                            " ",
                            {   elmt:  ui.createActionLink("\u2039", this, this._onPreviousPageClick, 0),
                                field: "previousLink"
                            },
                            " ",
                            {   tag: "input",
                                type: "text",
                                size: "3",
                                field: "pageInput"
                            },
                            " ",
                            {   elmt:  ui.createActionLink("next \u203a", this, this._onNextPageClick, 0),
                                field: "nextLink"
                            },
                            " ",
                            {   elmt:  ui.createActionLink("\u00bb", this, this._onLastPageClick, 0),
                                field: "lastLink"
                            }
                        ]
                    }
                ]
            }
        ]
    };
    this._summaryDOM = Longwell.DOM.createDOMFromTemplate(document, summaryTemplate);
    this._div.appendChild(this._summaryDOM.elmt);
    
    ui.registerEventWithObject(this._summaryDOM.pageInput, "keyup", this, this._onPageInputKeyUp, 0);
    
    /*
     *  Column controls
     */
    var columnControlsTemplate = {
        tag: "div",
        className: "longwell-tabularView-tableControls",
        children: [
            {   elmt:  ui.createActionLink("auto-pick columns", this, this._onResetColumnsClick, 0),
                field: "resetColumnsLink"
            },
            " | ",
            {   elmt:  ui.createActionLink("add column...", this, this._onAddColumnClick, 0),
                field: "addColumnLink"
            }
        ]
    };
    this._columnControlsDOM = Longwell.DOM.createDOMFromTemplate(document, columnControlsTemplate);
    this._div.appendChild(this._columnControlsDOM.elmt);
    
    /*
     *  Results table
     */
    this._bodyElement = document.createElement("div");
    this._div.appendChild(this._bodyElement);
    
    /*
     *  Append controls
     */
    var tv = this;
    var createAppendLink = function(text, count, field) {
        var handler = function(elmt, evt, target) { 
            tv._onAppendMoreItems(count); 
            Longwell.DOM.cancelEvent(evt);
            return false;
        };
        return {
            elmt:   ui.createActionLink(text, null, handler, 0),
            field:  field
        };
    }
    var appendControlsTemplate = {
        tag: "div",
        className: "longwell-tabularView-tableControls",
        children: [
            createAppendLink("append 10 more items", 10, "append10Link"),
            " | ",
            createAppendLink("20 more", 20, "append20Link"),
            " | ",
            createAppendLink("50 more", 50, "append50Link")
        ]
    };
    this._appendControlsDOM = Longwell.DOM.createDOMFromTemplate(document, appendControlsTemplate);
    this._appendControlsDOM.append10Link.setAttribute("disabled", true);
    this._appendControlsDOM.append20Link.setAttribute("disabled", true);
    this._appendControlsDOM.append50Link.setAttribute("disabled", true);
    this._div.appendChild(this._appendControlsDOM.elmt);

    /*
     * Footer page control
     */
    var footerTemplate = {
        tag: "table",
        className: "longwell-tabularView-footer",
        children: [
            {   tag: "tr",
                children: [
                    {   tag: "td",
                        field: "pagingControlsTD",
                        children: [
                            {   elmt:  ui.createActionLink("\u00ab", this, this._onFirstPageClick, 0),
                                field: "firstLink"
                            },
                            {   elmt:  ui.createActionLink("\u2039", this, this._onPreviousPageClick, 0),
                                field: "previousLink"
                            },
                            {   tag: "input",
                                type: "text",
                                size: "3",
                                field: "pageInput"
                            },
                            {   elmt:  ui.createActionLink("next \u203a", this, this._onNextPageClick, 0),
                                field: "nextLink"
                            },
                            {   elmt:  ui.createActionLink("\u00bb", this, this._onLastPageClick, 0),
                                field: "lastLink"
                            }
                        ]
                    }
                ]
            }
        ]
    };
    this._footerDOM = Longwell.DOM.createDOMFromTemplate(document, footerTemplate);
    this._div.appendChild(this._footerDOM.elmt);
    
    ui.registerEventWithObject(this._footerDOM.pageInput, "keyup",  this, this._onPageInputKeyUp, 0);
};

Longwell.UI.TabularView.prototype.dispose = function() {
    this._viewPanel.getUI().getLongwell().getQE().unregisterListener(this);
};

Longwell.UI.TabularView.prototype.initialize = function() {
    this._viewPanel.getUI().getLongwell().getQE().registerListener(this);
    this._resetView(false);
};

Longwell.UI.TabularView.prototype.getSettings = function() {
    return this._dupSettings();
};

Longwell.UI.TabularView.prototype.onCurrentFacetsChange = function() {
    this._resetView(false);
};

Longwell.UI.TabularView.prototype._resetView = function(force) {
    if (force || this._settings.columnPropertyURIs.length == 0) {
        var tv = this;
        this._xmlHttpQueue.queue(function(cont) {
            tv._viewPanel.getUI().getLongwell().callAPI({
                call:   "fetch-common-properties",
                body:   tv._viewPanel.getUI().getLongwell().getQE().getXML("query", ""),
                fDone:  function(o) {
                    try {
                        tv._onResetColumns(o);
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
    } else {
        this._refreshPage(0);
    }
};

Longwell.UI.TabularView.prototype._refreshPage = function(startIndex) {
    var s = this._makeFetchSortedItemsRequest(startIndex, startIndex + this._itemsPerPage);
    var tv = this;
    this._xmlHttpQueue.queue(function(cont) {
        tv._viewPanel.getUI().getLongwell().callAPI({
            call:   "fetch-sorted-items",
            body:   s,
            fDone:  function(o) {
                try {
                    tv._onRefreshPage(o);
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

Longwell.UI.TabularView.prototype._applySettings = function(settings) {
    var s = this._makeFetchSortedItemsRequest(0, 0 + this._itemsPerPage, settings);
    var tv = this;
    this._xmlHttpQueue.queue(function(cont) {
        tv._viewPanel.getUI().getLongwell().callAPI({
            call:   "fetch-sorted-items",
            body:   s,
            fDone:  function(o) {
                try {
                    tv._settings = settings;
                    tv._onRefreshPage(o);
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

Longwell.UI.TabularView.prototype._onResetColumns = function(o) {
    var doNotAutoPickProperties = Longwell.Configuration.ui.viewPanel.views["tabular-view"].doNotAutoPickProperties;
    
    var settings = {};
    
    var commonProperties = [];
    var newPropertyIndex = [];
    for (var i = 0; i < o.length; i++) {
        var p = o[i];
        if (!(p in doNotAutoPickProperties) || !doNotAutoPickProperties[p]) {
            commonProperties.push(p);
            newPropertyIndex[p] = true;
        }
    }
    commonProperties.sort(function(p1, p2) {
        p1 = Longwell.ProfileData.propertyIndex[p1];
        p2 = Longwell.ProfileData.propertyIndex[p2];
        
        var c = p2.uniqueness - p1.uniqueness;
        if (c == 0) {
            c = p1.label.localeCompare(p2.label);
        }
        return c;
    });
    
    settings.columnPropertyURIs = commonProperties.slice(0, 3);
    settings.sortPropertyURI = this._settings.sortPropertyURI;
    settings.sortAscending = this._settings.sortAscending;
    
    var mostUnique = -1;
    var mostUniquePropertyURI = null;
    var foundSortPropertyURI = false;
    for (var i = 0; i < settings.columnPropertyURIs.length; i++) {
        var columnPropertyURI = settings.columnPropertyURIs[i];
        var property = Longwell.ProfileData.propertyIndex[columnPropertyURI];
        
        if (columnPropertyURI == settings.sortPropertyURI) {
            foundSortPropertyURI = true;
            break;
        }
        
        if (property.uniqueness > mostUnique) {
            mostUnique = property.uniqueness;
            mostUniquePropertyURI = columnPropertyURI;
        }
    }
    
    if (!foundSortPropertyURI) {
        settings.sortPropertyURI = mostUniquePropertyURI;
        settings.sortAscending = true;
    }
    
    this._applySettings(settings);
};

Longwell.UI.TabularView.prototype._onRefreshPage = function(o) {
    this._startIndex = o.startIndex;
    this._endIndex = o.endIndex;
    this._totalItemCount = o.totalItemCount;
    
    this._updateSummary(o);
    this._updatePaging(o);
    this._updateWholeTable(o);
};

Longwell.UI.TabularView.prototype._onAppendPage = function(o) {
    if (o.startIndex == this._endIndex) {
        this._endIndex = o.endIndex;
        this._appendItemsToTable(o);
    }
};

Longwell.UI.TabularView.prototype._updatePaging = function(o) {
    var pageCount = Math.ceil(o.totalItemCount / this._itemsPerPage);
    var currentPage = this._startIndex / this._itemsPerPage;
    
    this._summaryDOM.firstLink.setAttribute("disabled", currentPage < 1);
    this._summaryDOM.previousLink.setAttribute("disabled", currentPage < 1);
    this._summaryDOM.nextLink.setAttribute("disabled", currentPage >= pageCount - 1);
    this._summaryDOM.lastLink.setAttribute("disabled", currentPage >= pageCount - 1);
    this._summaryDOM.pageInput.value = pageCount > 0 ? (currentPage + 1) : 0;

    this._footerDOM.firstLink.setAttribute("disabled", currentPage < 1);
    this._footerDOM.previousLink.setAttribute("disabled", currentPage < 1);
    this._footerDOM.nextLink.setAttribute("disabled", currentPage >= pageCount - 1);
    this._footerDOM.lastLink.setAttribute("disabled", currentPage >= pageCount - 1);
    this._footerDOM.pageInput.value = pageCount > 0 ? (currentPage + 1) : 0;

    this._appendControlsDOM.append10Link.setAttribute("disabled", this._endIndex == o.totalItemCount);
    this._appendControlsDOM.append20Link.setAttribute("disabled", o.totalItemCount - this._endIndex <= 10);
    this._appendControlsDOM.append50Link.setAttribute("disabled", o.totalItemCount - this._endIndex <= 20);
};

Longwell.UI.TabularView.prototype._updateSummary = function(o) {
    var ui = this._viewPanel.getUI();
    
    var summaryElement = this._summaryDOM.summarySpan;
    summaryElement.innerHTML = "";
    
    var span = document.createElement("span");
    span.className = "item-count";
    span.appendChild(document.createTextNode(o.totalItemCount));
    summaryElement.appendChild(span);
    
    if (o.totalItemCount > 1) {
        if (o.totalItemCount > this._itemsPerPage) {
            var pageCount = Math.ceil(o.totalItemCount / this._itemsPerPage);
            summaryElement.appendChild(document.createTextNode("\u00a0item(s)\u00a0in\u00a0" + pageCount + "\u00a0pages."));
        } else {
            summaryElement.appendChild(document.createTextNode("\u00a0items."));
        }
    } else {
        summaryElement.appendChild(document.createTextNode("\u00a0item."));
    }
};

Longwell.UI.TabularView.prototype._updateWholeTable = function(o) {
    var bodyElement = this._bodyElement;
    bodyElement.innerHTML = "";
    
    var makeActionLink = function(text) {
        var a = document.createElement("a");
        a.className = "action";
        a.href = "javascript:";
        a.appendChild(document.createTextNode(text));
        return a;
    };
    
    /*
     *  Item Table
     */
    var table = document.createElement("table");
    table.className = "longwell-tabularView-table";
    this._itemTable = table;
    
    /*
     *  Body
     */
    this._appendItemsToTable(o);
    
    /*
     *  Head
     */
    var thead = table.createTHead();
    var tr = thead.insertRow(0);
    
    var td = tr.insertCell(0);
    
    td = tr.insertCell(1);
    td.innerHTML = "<div>label</div>"; 
    
    for (var i = 0; i < this._settings.columnPropertyURIs.length; i++) {
        var columnPropertyURI = this._settings.columnPropertyURIs[i];
        var property = Longwell.ProfileData.propertyIndex[columnPropertyURI];
        
        td = tr.insertCell(i + 2);
        td.setAttribute("uri", columnPropertyURI);
        this._viewPanel.getUI().registerEventWithObject(td, "click", this, this._onColumnHeaderClick, 0);
        
        var tdDiv = document.createElement("div");
        td.appendChild(tdDiv);
        
        tdDiv.appendChild(document.createTextNode(property.label));
        
        /*
         *  Sort indicators
         */
        if (columnPropertyURI != this._settings.sortPropertyURI) {
            td.title = "Click to Sort";
        } else {
            td.title = "Click to Reverse Order";
            tdDiv.appendChild(Longwell.Graphics.createTranslucentImage(document, Longwell.urlPrefix + 
                (this._settings.sortAscending ? "images/up-arrow.png" : "images/down-arrow.png")));
        }
        
        if (property.isLatLong > 0.5) {
            tdDiv.appendChild(document.createTextNode(" "));
            
            var a = makeActionLink("\u00bbmap");
            a.setAttribute("uri", columnPropertyURI);
            a.title = "Plot items on a map using this field";
            tdDiv.appendChild(a);
            this._viewPanel.getUI().registerEventWithObject(a, "click", this, this._onMapColumnClick, 0);
        } else if (property.isDateTime > 0.5) {
            tdDiv.appendChild(document.createTextNode(" "));
            
            var a = makeActionLink("\u00bbtimeline");
            a.setAttribute("uri", columnPropertyURI);
            a.title = "Plot items on a timeline using this field";
            tdDiv.appendChild(a);
            this._viewPanel.getUI().registerEventWithObject(a, "click", this, this._onTimelineColumnClick, 0);
        }
            
        /*
         *  Remove column control
         */
        var imgColumnControl = Longwell.Graphics.createTranslucentImage(document, Longwell.urlPrefix + "images/close.png");
        imgColumnControl.className = "remove-column-control";
        imgColumnControl.title = "Remove this column";
        imgColumnControl.setAttribute("uri", columnPropertyURI);
        tdDiv.appendChild(imgColumnControl);
        this._viewPanel.getUI().registerEventWithObject(imgColumnControl, "click", this, this._onRemoveColumnClick, 0);
    }
    
    bodyElement.appendChild(table);
};

Longwell.UI.TabularView.prototype._appendItemsToTable = function(o) {
    var ui = this._viewPanel.getUI();
    var table = this._itemTable;
    var tr, td;
    
    var items = o.items;
    var rows = table.rows.length;
    for (var i = 0; i < items.length; i++) {
        var item = items[i];
        var index = (o.startIndex - this._startIndex) + i;
        
        tr = table.insertRow(rows + i);
        tr.className = (i % 2) == 0 ? "longwell-tabularView-evenRow" : "longwell-tabularView-oddRow";
        
        td = tr.insertCell(0);
        td.className = "item-number";
        td.appendChild(document.createTextNode((o.startIndex + i + 1) + "."));
        
        td = tr.insertCell(1);
        td.className = "item-label";
        td.appendChild(ui.createElementForURI(item.uri, item.label));
        
        if ("properties" in item) {
            for (var j = 0; j < item.properties.length; j++) {
                var values = item.properties[j];
                
                td = tr.insertCell(j + 2);
                
                for (var v = 0; v < values.length; v++) {
                    var value = values[v];
                    
                    if (v > 0) {
                        var separator = document.createElement("span");
                        separator.innerHTML = " &bull; ";
                        td.appendChild(separator);
                    }
                    if ("uri" in value) {
                        td.appendChild(ui.createElementForURI(value.uri, value.label));
                    } else {
                        td.appendChild(ui.createElementForLiteral(this._settings.columnPropertyURIs[j], value.label));
                    }
                }
            }
        }
    }
};

Longwell.UI.TabularView.prototype._onFirstPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(0);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.TabularView.prototype._onPreviousPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(this._startIndex - this._itemsPerPage);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.TabularView.prototype._onNextPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(this._startIndex + this._itemsPerPage);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.TabularView.prototype._onLastPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(Math.floor(this._totalItemCount / this._itemsPerPage) * this._itemsPerPage);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.TabularView.prototype._onPageInputKeyUp = function(a, evt, target) {
    if (evt.keyCode == 13) {
        var headIn = parseInt(this._summaryDOM.pageInput.value) - 1;
        var footIn = parseInt(this._footerDOM.pageInput.value) - 1;
        var page = (headIn == this._startIndex) ? ((footIn == this._startIndex)  ? this._startIndex : footIn) : headIn;
        this._goToItems(page * this._itemsPerPage);
    }
};

Longwell.UI.TabularView.prototype._goToItems = function(startIndex) {
    var currentStartIndex = this._startIndex;
    var tv = this;
    var history = this._viewPanel.getUI().getLongwell().getHistory();
    history.addAction({ 
        perform: function() {
            tv._refreshPage(startIndex);
        },
        undo: function() {
            tv._refreshPage(currentStartIndex);
        }
    });
};

Longwell.UI.TabularView.prototype._onResetColumnsClick = function(a, evt, target) {
    var oldSettings = this._settings;
    var tv = this;
    this._viewPanel.getUI().getLongwell().getHistory().addAction({
        perform: function() {
            tv._resetView(true);
        },
        undo: function() {
            tv._applySettings(oldSettings);
        }
    });
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.TabularView.prototype._onAddColumnClick = function(a, evt, target) {
   var tv = this;
   var fDone = function(propertyURIs) {
       var properties = [];
       for (var i = 0; i < propertyURIs.length; i++) {
           var propertyURI = propertyURIs[i];
           var property = Longwell.ProfileData.propertyIndex[propertyURI];
           properties.push({ uri: propertyURI, label: property.label });
       }

       var dialog = Longwell.Graphics.createPopupDialog(tv._viewPanel.getUI(), "Add Column");

       dialog.dialogDiv.style.left = "40%";
       dialog.dialogDiv.style.right = "40%";
       dialog.dialogDiv.style.top = "15em";
       
		var p = document.createElement("p");
		p.className = "section";
		p.appendChild(document.createTextNode("Pick a property to add:"));
		p.appendChild(document.createElement("br"));
		dialog.bodyDiv.appendChild(p);
		var select = document.createElement("select");
		select.size = 10;
		select.style.width = "100%";
		tv._fillSelectWithTheseProperties(select, properties);
		p.appendChild(select);
		dialog.onOK = function() {
			var settings = tv._dupSettings();
			settings.columnPropertyURIs.push(select.value);
			var oldSettings = tv._settings;
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
   }        
   
   this._xmlHttpQueue.queue(function(cont) {
       tv._viewPanel.getUI().getLongwell().callAPI({
           call:   "fetch-common-properties",
           body:   tv._viewPanel.getUI().getLongwell().getQE().getXML("query", ""),
           fDone:  function(o) {
               try {
                   fDone(o);
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
   
   Longwell.DOM.cancelEvent(evt);
   return false;
};


Longwell.UI.TabularView.prototype._onAppendMoreItems = function(more) {
    var s = this._makeFetchSortedItemsRequest(this._endIndex, this._endIndex + more);
    var tv = this;
    this._xmlHttpQueue.queue(function(cont) {
        tv._viewPanel.getUI().getLongwell().callAPI({
            call:   "fetch-sorted-items",
            body:   s,
            fDone:  function(o) {
                try {
                    tv._onAppendPage(o);
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

Longwell.UI.TabularView.prototype._makeFetchSortedItemsRequest = function(startIndex, endIndex, settings) {
    if (settings == null) {
        settings = this._settings;
    }
    
    var s = "<page";
    s += " startIndex='" + startIndex + "'";
    s += " endIndex='" + endIndex + "'";
    if (settings.sortPropertyURI != null) {
        s += " sortPropertyURI='" + settings.sortPropertyURI + "'";
    }
    s += " ascending='" + settings.sortAscending + "'";
    s += ">\n";
    s += this._viewPanel.getUI().getLongwell().getQE().getXML("query", " ");
    
    s += " <columns>\n";
    for (var i = 0; i < settings.columnPropertyURIs.length; i++) {
        var columnPropertyURI = settings.columnPropertyURIs[i];
        s += "  <column uri='" + columnPropertyURI + "' />\n";
    }
    s += " </columns>\n";
    
    s += "</page>";
    
    return s;
};

Longwell.UI.TabularView.prototype._onColumnHeaderClick = function(elmt, evt, target) {
    var propertyURI = elmt.getAttribute("uri");
    
    var oldSettings = this._settings;
    var newSettings = this._dupSettings();
    if (propertyURI == newSettings.sortPropertyURI) {
        newSettings.sortAscending = !newSettings.sortAscending;
    } else {
        newSettings.sortAscending = true;
        newSettings.sortPropertyURI = propertyURI;
    }
    
    var tv = this;
    this._viewPanel.getUI().getLongwell().getHistory().addAction({
        perform: function() {
            tv._applySettings(newSettings);
        },
        undo: function() {
            tv._applySettings(oldSettings);
        }
    });
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.TabularView.prototype._onRemoveColumnClick = function(elmt, evt, target) {
    var propertyURI = elmt.getAttribute("uri");
    
    var oldSettings = this._settings;
    var newSettings = this._dupSettings();
    
    var i = 0;
    for (var i = 0; i < newSettings.columnPropertyURIs.length; i++) {
        if (newSettings.columnPropertyURIs[i] == propertyURI) {
            newSettings.columnPropertyURIs.splice(i,1);
            break;
        }
    }
    
    var tv = this;
    this._viewPanel.getUI().getLongwell().getHistory().addAction({
        perform: function() {
            tv._applySettings(newSettings);
        },
        undo: function() {
            tv._applySettings(oldSettings);
        }
    });
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.TabularView.prototype._onMapColumnClick = function(a, evt, target) {
    this._viewPanel.switchView(this, "map-view", { latlngPropertyURI: a.getAttribute("uri") });
};

Longwell.UI.TabularView.prototype._onTimelineColumnClick = function(a, evt, target) {
    this._viewPanel.switchView(this, "timeline-view", { startPropertyURI: a.getAttribute("uri") });
};

Longwell.UI.TabularView.prototype._dupSettings = function() {
    return {
        sortPropertyURI:    this._settings.sortPropertyURI,
        sortAscending:      this._settings.sortAscending,
        columnPropertyURIs: [].concat(this._settings.columnPropertyURIs)
    };
};

Longwell.UI.TabularView.prototype._fillSelectWithProperties = function(select) {
   tv._fillSelectWithTheseProperties(select, [].concat(Longwell.ProfileData.properties));
};

Longwell.UI.TabularView.prototype._fillSelectWithTheseProperties = function(select, properties) {
   properties.sort(function(p1, p2) {
       return p1.label.localeCompare(p2.label);
   });
     for (var i = 0; i < properties.length; i++) {
       var property = properties[i];
       var option = document.createElement("option");
       option.text = property.label;
       option.value = property.uri;
       try {
           select.add(option);
       } catch (e) {
           select.add(option, null);
       }
   }
};
