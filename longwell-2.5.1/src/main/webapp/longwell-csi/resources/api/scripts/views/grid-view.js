Longwell.UI.GridView = function(viewPanel, div, settings) {
    this._viewPanel = viewPanel;
    this._div = div;
    
    this._itemsPerPage = 20;
    this._startIndex = 0;
    this._endIndex = this._startIndex + this._itemsPerPage;
    this._totalItemCount = 0;
    
    this._settings = {
        sortPropertyURI:       null,
        sortAscending:         true,
        thumbnailPropertyURIs: [],
        columnPropertyURIs:    []
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
        if ("thumbnailPropertyURIs" in settings) {
            this._settings.thumbnailPropertyURIs = settings.thumbnailPropertyURIs;
        }
    };
    
    this._xmlHttpQueue = new Longwell.XmlHttpQueue();
    this._constructUI();
};

Longwell.UI.GridView.prototype._constructUI = function() {
    var progressAnimation = Longwell.Graphics.createAnimationIcon();
    this._xmlHttpQueue.setAnimation(this._progressAnimation);
    
    var ui = this._viewPanel.getUI();
    
    /*
     *  Summary controls
     */
    var summaryTemplate = {
        tag: "table",
        className: "longwell-gridView-header",
        width: "100%",
        children: [
            {   tag: "tr",
                children: [
                    {   tag: "td",
                        width: "0",
                        children: [
                            { elmt: progressAnimation.element },
                            {   tag: "span",
                                field: "summarySpan"
                            }
                        ]
                    },
                    {   tag: "td",
                        field: "pagingControlsTD",
                        children: [
                            {   elmt:  ui.createActionLink("\u00ab first", this, this._onFirstPageClick, 0),
                                field: "firstLink"
                            },
                            " ",
                            {   elmt:  ui.createActionLink("\u2039 previous", this, this._onPreviousPageClick, 0),
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
                            {   elmt:  ui.createActionLink("last \u00bb", this, this._onLastPageClick, 0),
                                field: "lastLink"
                            }
                        ]
                    }
                ]
            },
            {   tag: "tr",
                children: [
                    {   tag: "td",
                        colspan: "2",
                        align: "left",
                        children: [
                            "Sort by ",
                            {   tag: "select",
                                field: "sortSelect"
                            },
                            " ",
                            {   tag: "input",
                                type: "checkbox",
                                style: "vertical-align: middle",
                                field: "sortAscendingCheckbox"
                            },
                            " ascending"
                        ]
                    }
                ]
            }
        ]
    };
    this._summaryDOM = Longwell.DOM.createDOMFromTemplate(document, summaryTemplate);
    ui.registerEventWithObject(this._summaryDOM.sortSelect, "change", this, this._onSortSelectChange, 0);
    ui.registerEventWithObject(this._summaryDOM.sortAscendingCheckbox, "click",  this, this._onSortAscendingCheckboxClick, 0);
    this._div.appendChild(this._summaryDOM.elmt);
    
    ui.registerEventWithObject(this._summaryDOM.pageInput, "keyup", this, this._onPageInputKeyUp, 0);
    
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
        className: "longwell-gridView-tableControls",
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
        className: "longwell-gridView-footer",
        children: [
            {   tag: "tr",
                children: [
                    {   tag: "td",
                        field: "pagingControlsTD",
                        children: [
                            {   elmt:  ui.createActionLink("\u00ab first", this, this._onFirstPageClick, 0),
                                field: "firstLink"
                            },
                            {   elmt:  ui.createActionLink("\u2039 previous", this, this._onPreviousPageClick, 0),
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
                            {   elmt:  ui.createActionLink("last \u00bb", this, this._onLastPageClick, 0),
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

Longwell.UI.GridView.prototype.dispose = function() {
    this._viewPanel.getUI().getLongwell().getQE().unregisterListener(this);
};

Longwell.UI.GridView.prototype.initialize = function() {
    this._viewPanel.getUI().getLongwell().getQE().registerListener(this);
    this._resetView(false);
};

Longwell.UI.GridView.prototype.getSettings = function() {
    return this._dupSettings();
};

Longwell.UI.GridView.prototype.onCurrentFacetsChange = function() {
    this._resetView(false);
};

Longwell.UI.GridView.prototype._resetView = function(force) {
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

Longwell.UI.GridView.prototype._refreshPage = function(startIndex) {
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

Longwell.UI.GridView.prototype._applySettings = function(settings) {
    var s = this._makeFetchSortedItemsRequest(0, 0 + this._itemsPerPage, settings);
    var tv = this;
    this._xmlHttpQueue.queue(function(cont) {
        tv._viewPanel.getUI().getLongwell().callAPI({
            call:   "fetch-sorted-items",
            body:   s,
            fDone:  function(o) {
                try {
                    tv._summaryDOM.sortSelect.value = settings.sortPropertyURI;
                    tv._summaryDOM.sortAscendingCheckbox.checked = settings.sortAscending;
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

Longwell.UI.GridView.prototype._onResetColumns = function(o) {
    var doNotAutoPickProperties = [];
    var thumbnailProperties = Longwell.Configuration.ui.viewPanel.views["grid-view"].thumbnailProperties;
    
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
    
    var thumbProperties = [];
    for (var i = 0; i < o.length; i++) {
        var p = o[i];
        if (p in thumbnailProperties || thumbnailProperties[p]) {
            thumbProperties.push(p);
        }
    }
    thumbProperties.sort(function(p1, p2) {
        p1 = Longwell.ProfileData.propertyIndex[p1];
        p2 = Longwell.ProfileData.propertyIndex[p2];
        
        var c = p2.uniqueness - p1.uniqueness;
        if (c == 0) {
            c = p1.label.localeCompare(p2.label);
        }
        return c;
    });

    var select = this._summaryDOM.sortSelect;
    var checkbox = this._summaryDOM.sortAscendingCheckbox;
    while (select.length > 0) {
        select.remove(0);
    }
    for (var i = 0; i < o.length; i++) {
        var option = document.createElement("option");
        var propertyURI = o[i];
        var property = Longwell.ProfileData.propertyIndex[propertyURI];
        option.value = propertyURI;
        option.text = property.label;
        try {
            select.add(option);
        } catch (e) {
            select.add(option, null);
        }
    }

    settings.columnPropertyURIs = commonProperties.slice(0, 3);
    settings.sortPropertyURI = this._settings.sortPropertyURI;
    settings.sortAscending = this._settings.sortAscending;
    settings.thumbnailPropertyURIs = thumbProperties;

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

Longwell.UI.GridView.prototype._onRefreshPage = function(o) {
    this._startIndex = o.startIndex;
    this._endIndex = o.endIndex;
    this._totalItemCount = o.totalItemCount;
    
    this._updateSummary(o);
    this._updatePaging(o);
    this._updateWholeTable(o);
};

Longwell.UI.GridView.prototype._onAppendPage = function(o) {
    if (o.startIndex == this._endIndex) {
        this._endIndex = o.endIndex;
        this._appendItems(o);
    }
};

Longwell.UI.GridView.prototype._updatePaging = function(o) {
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

Longwell.UI.GridView.prototype._updateSummary = function(o) {
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

Longwell.UI.GridView.prototype._updateWholeTable = function(o) {
    var bodyElement = this._bodyElement;
    bodyElement.innerHTML = "";

    /*
     *  Items
     */
    var items = document.createElement("div");
    items.className = "longwell-gridView-container";
    this._itemsContainer = items;

    this._appendItems(o);

    var breaker = document.createElement("div");
    breaker.className = "longwell-gridView-breaker";

    bodyElement.appendChild(items);
    bodyElement.appendChild(breaker);
};

Longwell.UI.GridView.prototype._appendItems = function(o) {
    var ui = this._viewPanel.getUI();
    var container = this._itemsContainer;
    
    var items = o.items;
    var itemDiv, itemTitle;
    for (var i = 0; i < items.length; i++) {
        var item = items[i];
        var index = (o.startIndex - this._startIndex) + i;
        
        itemDiv = document.createElement("div");
        itemDiv.className = "longwell-gridView-item";
        
        itemTitle = document.createElement("span");
        itemTitle.className = "item-label";
        itemTitle.appendChild(ui.createElementForURI(item.uri, item.label));
        itemDiv.appendChild(itemTitle);
        
        if ("properties" in item) {
            for (var j = 0; j < item.properties.length; j++) {
                var values = item.properties[j];
                
                for (var v = 0; v < values.length; v++) {
                    var value = values[v];
                    
                    if (v > 0) {
                        var separator = document.createElement("span");
                        separator.innerHTML = " &bull; ";
                        itemDiv.appendChild(separator);
                    }
                    if ("uri" in value) {
			var make = function() {
	                        var thumb = document.createElement("img");
        	                thumb.src = value.uri;
				thumb.setAttribute('alt', value.label);
				return thumb;
			}
                        itemDiv.appendChild(ui.createNodeForURI(item.uri, make));
                    } else {
			var make = function() {
	                        var thumb = document.createElement("img");
      		                thumb.src = value.label;
				thumb.setAttribute('alt', value.label);
				return thumb;
			}
                        itemDiv.appendChild(ui.createNodeForURI(item.uri, make));
                    }
                }
            }
        }
        container.appendChild(itemDiv);
    }
};

Longwell.UI.GridView.prototype._onFirstPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(0);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.GridView.prototype._onPreviousPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(this._startIndex - this._itemsPerPage);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.GridView.prototype._onNextPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(this._startIndex + this._itemsPerPage);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.GridView.prototype._onLastPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(Math.floor(this._totalItemCount / this._itemsPerPage) * this._itemsPerPage);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.GridView.prototype._onPageInputKeyUp = function(a, evt, target) {
    if (evt.keyCode == 13) {
        var headIn = parseInt(this._summaryDOM.pageInput.value) - 1;
        var footIn = parseInt(this._footerDOM.pageInput.value) - 1;
        var page = (headIn == this._startIndex) ? ((footIn == this._startIndex)  ? this._startIndex : footIn) : headIn;
        this._goToItems(page * this._itemsPerPage);
    }
};

Longwell.UI.GridView.prototype._goToItems = function(startIndex) {
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

Longwell.UI.GridView.prototype._onAppendMoreItems = function(more) {
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

Longwell.UI.GridView.prototype._makeFetchSortedItemsRequest = function(startIndex, endIndex, settings) {
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
    for (var i = 0; i < settings.thumbnailPropertyURIs.length; i++) {
        var thumbnailPropertyURI = settings.thumbnailPropertyURIs[i];
        s += "  <column uri='" + thumbnailPropertyURI + "' />\n";
    }
    s += " </columns>\n";
    
    s += "</page>";
    
    return s;
};

Longwell.UI.GridView.prototype._dupSettings = function() {
    return {
        sortPropertyURI:    this._settings.sortPropertyURI,
        sortAscending:      this._settings.sortAscending,
        columnPropertyURIs: [].concat(this._settings.columnPropertyURIs),
        thumbnailPropertyURIs: [].concat(this._settings.thumbnailPropertyURIs)
    };
};

Longwell.UI.GridView.prototype._fillSelectWithProperties = function(select) {
   tv._fillSelectWithTheseProperties(select, [].concat(Longwell.ProfileData.properties));
};

Longwell.UI.GridView.prototype._fillSelectWithTheseProperties = function(select, properties) {
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

Longwell.UI.GridView.prototype._onSortAscendingCheckboxClick = function(checkbox, evt, target) {
    var oldSettings = this._settings;
    var newSettings = this._dupSettings();
    newSettings.sortAscending = !newSettings.sortAscending;
    
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

Longwell.UI.GridView.prototype._onSortSelectChange = function(select, evt, target) {
    var oldSettings = this._settings;
    var newSettings = this._dupSettings();
    newSettings.sortPropertyURI = select.value;
    
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
