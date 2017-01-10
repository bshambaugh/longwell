Longwell.UI.ListView = function(viewPanel, div, settings) {
	this._active = true;
    this._viewPanel = viewPanel;
    this._div = div;
    
    this._itemsPerPage = 10;
    this._startIndex = 0;
    this._endIndex = this._startIndex + this._itemsPerPage;
    this._totalItemCount = 0;
    
    this._settings = {
        sortPropertyURI:    "label",
        sortAscending:      true
    };
    if (settings != null) {
        if ("sortPropertyURI" in settings) {
            this._settings.sortPropertyURI = settings.sortPropertyURI;
        }
        if ("sortAscending" in settings) {
            this._settings.sortAscending = settings.sortAscending;
        }
    };
    
    this._xmlHttpQueue = new Longwell.XmlHttpQueue();
    this._constructUI();
};

Longwell.UI.ListView.prototype._constructUI = function() {
    var ui = this._viewPanel.getUI();
    var makeActionLink = function(text) {
        var a = document.createElement("a");
        a.className = "action";
        a.href = "javascript:";
        a.appendChild(document.createTextNode(text));
        return a;
    }
    
    /*
     *  Header
     */
    var progressAnimation = Longwell.Graphics.createAnimationIcon();
    this._xmlHttpQueue.setAnimation(this._progressAnimation);
    
    var headerTemplate = {
        tag: "table",
        className: "longwell-listView-header",
        children: [
            {   tag: "tr",
                children: [
                    {   tag: "td",
                        children: [
                            {   elmt: progressAnimation.element },
                            {   tag: "span",
                                field: "summarySpan"
                            }
                        ]
                    },
                    {   tag: "td",
                        style: "text-align: center",
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
                            {   elmt:  ui.createActionLink("next page \u203a", this, this._onNextPageClick, 0),
                                field: "nextLink"
                            },
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
    this._headerDOM = Longwell.DOM.createDOMFromTemplate(document, headerTemplate);
    this._div.appendChild(this._headerDOM.elmt);
    
    ui.registerEventWithObject(this._headerDOM.pageInput,             "keyup",  this, this._onPageInputKeyUp, 0);
    ui.registerEventWithObject(this._headerDOM.sortSelect,            "change", this, this._onSortSelectChange, 0);
    ui.registerEventWithObject(this._headerDOM.sortAscendingCheckbox, "click",  this, this._onSortAscendingCheckboxClick, 0);
    
    /*
     *  Body
     */
    this._bodyElement = document.createElement("div");
    this._bodyElement.style.textAlign = "left";
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
        className: "longwell-listView-footer",
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
                            {   elmt:  ui.createActionLink("next page \u203a", this, this._onNextPageClick, 0),
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

Longwell.UI.ListView.prototype.dispose = function() {
	this._active = false;
    this._viewPanel.getUI().getLongwell().getQE().unregisterListener(this);
};

Longwell.UI.ListView.prototype.initialize = function() {
    this._viewPanel.getUI().getLongwell().getQE().registerListener(this);
    this._resetView();
};

Longwell.UI.ListView.prototype.getSettings = function() {
    return this._dupSettings();
};

Longwell.UI.ListView.prototype.onCurrentFacetsChange = function() {
    this._resetView();
};

Longwell.UI.ListView.prototype._resetView = function() {
    if (this._active) {
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
    }
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


Longwell.UI.ListView.prototype._refreshPage = function(startIndex) {
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

Longwell.UI.ListView.prototype._applySettings = function(settings) {
    var s = this._makeFetchSortedItemsRequest(0, 0 + this._itemsPerPage, settings);
    var tv = this;
    this._xmlHttpQueue.queue(function(cont) {
        tv._viewPanel.getUI().getLongwell().callAPI({
            call:   "fetch-sorted-items",
            body:   s,
            fDone:  function(o) {
                try {
                    tv._settings = settings;
                    tv._headerDOM.sortSelect.value = settings.sortPropertyURI;
                    tv._headerDOM.sortAscendingCheckbox.checked = settings.sortAscending;
                    
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

Longwell.UI.ListView.prototype._onResetColumns = function(o) {
    var settings = {};
    
    o.sort(function(p1, p2) {
        p1 = Longwell.ProfileData.propertyIndex[p1];
        p2 = Longwell.ProfileData.propertyIndex[p2];
        
        var c = p2.uniqueness - p1.uniqueness;
        if (c == 0) {
            c = p1.label.localeCompare(p2.label);
        }
        return c;
    });
    
    var select = this._headerDOM.sortSelect;
    var checkbox = this._headerDOM.sortAscendingCheckbox;
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
    
    var columnPropertyURIs = o.slice(0, 5);
    settings.sortPropertyURI = this._settings.sortPropertyURI;
    settings.sortAscending = this._settings.sortAscending;
    
    var mostUnique = -1;
    var mostUniquePropertyURI = null;
    var foundSortPropertyURI = false;
    for (var i = 0; i < columnPropertyURIs.length; i++) {
        var columnPropertyURI = columnPropertyURIs[i];
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

Longwell.UI.ListView.prototype._onRefreshPage = function(o) {
    this._startIndex = o.startIndex;
    this._endIndex = o.endIndex;
    this._totalItemCount = o.totalItemCount;
    
    this._updateSummary(o);
    this._updatePaging(o);
    this._updateWholeTable(o);
};

Longwell.UI.ListView.prototype._onAppendPage = function(o) {
    if (o.startIndex == this._endIndex) {
        this._endIndex = o.endIndex;
        this._appendItemsToTable(o);
    }
};

Longwell.UI.ListView.prototype._updatePaging = function(o) {
    var pageCount = Math.ceil(o.totalItemCount / this._itemsPerPage);
    var currentPage = this._startIndex / this._itemsPerPage;
    
    this._headerDOM.firstLink.setAttribute("disabled", currentPage < 1);
    this._headerDOM.previousLink.setAttribute("disabled", currentPage < 1);
    this._headerDOM.nextLink.setAttribute("disabled", currentPage >= pageCount - 1);
    this._headerDOM.lastLink.setAttribute("disabled", currentPage >= pageCount - 1);
    this._headerDOM.pageInput.value = pageCount > 0 ? (currentPage + 1) : 0;

    this._footerDOM.firstLink.setAttribute("disabled", currentPage < 1);
    this._footerDOM.previousLink.setAttribute("disabled", currentPage < 1);
    this._footerDOM.nextLink.setAttribute("disabled", currentPage >= pageCount - 1);
    this._footerDOM.lastLink.setAttribute("disabled", currentPage >= pageCount - 1);
    this._footerDOM.pageInput.value = pageCount > 0 ? (currentPage + 1) : 0;

    this._appendControlsDOM.append10Link.setAttribute("disabled", this._endIndex == o.totalItemCount);
    this._appendControlsDOM.append20Link.setAttribute("disabled", o.totalItemCount - this._endIndex <= 10);
    this._appendControlsDOM.append50Link.setAttribute("disabled", o.totalItemCount - this._endIndex <= 20);
};

Longwell.UI.ListView.prototype._updateSummary = function(o) {
    var ui = this._viewPanel.getUI();
    
    var summaryElement = this._headerDOM.summarySpan;
    summaryElement.innerHTML = "";
    summaryElement.appendChild(document.createTextNode(" "));
    
    var span = document.createElement("span");
    span.className = "item-count";
    span.appendChild(document.createTextNode(o.totalItemCount));
    summaryElement.appendChild(span);
    
    if (o.totalItemCount > 1) {
        if (o.totalItemCount > this._itemsPerPage) {
            var pageCount = Math.ceil(o.totalItemCount / this._itemsPerPage);
            summaryElement.appendChild(document.createTextNode(" item(s) in " + pageCount + " pages."));
        } else {
            summaryElement.appendChild(document.createTextNode(" items."));
        }
    } else {
        summaryElement.appendChild(document.createTextNode(" item."));
    }
};

Longwell.UI.ListView.prototype._updateWholeTable = function(o) {
    var bodyElement = this._bodyElement;
    bodyElement.innerHTML = "";
    
    var table = document.createElement("table");
    table.className = "longwell-listView-table";
    this._itemTable = table;
    
    this._appendItemsToTable(o);
    
    bodyElement.appendChild(table);
};

Longwell.UI.ListView.prototype._appendItemsToTable = function(o) {
    var ui = this._viewPanel.getUI();
    var table = this._itemTable;
    var tr, td;
    
    var items = o.items;
    var rows = table.rows.length;
    for (var i = 0; i < items.length; i++) {
        var item = items[i];
        var index = (o.startIndex - this._startIndex) + i;
        
        tr = table.insertRow(rows + i);
        
        td = tr.insertCell(0);
        td.className = "item-number";
        td.appendChild(document.createTextNode((o.startIndex + i + 1) + "."));
        
        td = tr.insertCell(1);
        td.appendChild(document.createTextNode(item.label));
        
        var img = document.createElement("img");
        img.src = Longwell.urlPrefix + "images/progress-running.gif";
        img.style.verticalAlign = "middle";
        td.appendChild(img);
        
        this._loadView(item.uri, td, true);
    }
};

Longwell.UI.ListView.prototype._onFirstPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(0);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.ListView.prototype._onPreviousPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(this._startIndex - this._itemsPerPage);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.ListView.prototype._onNextPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(this._startIndex + this._itemsPerPage);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.ListView.prototype._onLastPageClick = function(a, evt, target) {
    if (a.getAttribute("disabled") != "true") {
        this._goToItems(Math.floor(this._totalItemCount / this._itemsPerPage) * this._itemsPerPage);
    }
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.ListView.prototype._onPageInputKeyUp = function(a, evt, target) {
    // would be preferable to use evt.currentTarget, but not IE compatible
    if (evt.keyCode == 13) {
        var headIn = parseInt(this._headerDOM.pageInput.value) - 1;
        var footIn = parseInt(this._footerDOM.pageInput.value) - 1;
        var page = (headIn == this._startIndex) ? ((footIn == this._startIndex)  ? this._startIndex : footIn) : headIn;
        this._goToItems(page * this._itemsPerPage);
    }
};

Longwell.UI.ListView.prototype._goToItems = function(startIndex) {
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

Longwell.UI.ListView.prototype._onAppendMoreItems = function(more) {
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

Longwell.UI.ListView.prototype._makeFetchSortedItemsRequest = function(startIndex, endIndex, settings) {
    if (settings == null) {
        settings = this._settings;
    }
    
    var s = "<page";
    s += " startIndex='" + startIndex + "'";
    s += " endIndex='" + endIndex + "'";
    s += " sortPropertyURI='" + settings.sortPropertyURI + "'";
    s += " ascending='" + settings.sortAscending + "'";
    s += ">\n";
    s += this._viewPanel.getUI().getLongwell().getQE().getXML("query", " ");
    s += "<columns />";
    s += "</page>";
    
    return s;
};

Longwell.UI.ListView.prototype._dupSettings = function() {
    return {
        sortPropertyURI:    this._settings.sortPropertyURI,
        sortAscending:      this._settings.sortAscending
    };
};

Longwell.UI.ListView.prototype._loadView = function(uri, elmt, fresnel) {
    var ui = this._viewPanel.getUI();

    var tv = this;
    var viewChangeHandler = function(e, evt, target) { 
    	tv._loadView(uri, elmt, !fresnel);
        Longwell.DOM.cancelEvent(evt);
        return false;
    };

    var fError = function(statusText, status, xmlhttp) {
        Longwell.Debug.log(statusText);
    };

    var summaryViewSelectorTemplate = {
        tag: "div",
        className: "lw_item_views",
        children: [
            {   tag: "span",
            	className: "lw_view",
                children: [
                    {   elmt:  ui.createActionLink("Complete", this, viewChangeHandler, 0) }
                ]
            },
            {   tag: "span",
            	className: "lw_view lw_view_summary",
                children: [
                	"Summary"
                ]
            }
        ]
    };

    var completeViewSelectorTemplate = {
        tag: "div",
        className: "lw_item_views",
        children: [
            {   tag: "span",
            	className: "lw_view lw_view_complete",
                children: [
                	"Complete"
                ]
            },
            {   tag: "span",
            	className: "lw_view",
                children: [
                    {   elmt:  ui.createActionLink("Summary", this, viewChangeHandler, 0) }
                ]
            }
        ]
    };

    var fDone = function(xmlhttp) {
    	elmt.innerHTML = "";
    	
    	if (xmlhttp.responseText.indexOf("lw_fresnel_rendered") > 0) {
		    var viewSelectors = Longwell.DOM.createDOMFromTemplate(document, summaryViewSelectorTemplate);
		    elmt.appendChild(viewSelectors.elmt);
	
		    var container = document.createElement("div");
		    container.innerHTML = xmlhttp.responseText;
		    elmt.appendChild(container);
	    	        
	        var titleElmt = document.getElementById("lw_item_" + uri + "_title");
	        if (titleElmt) {
	        	var resultElmt = document.getElementById("fresnel_title_" + uri);
		        if (resultElmt && resultElmt.childNodes[0]) {
		            var title = document.createTextNode(resultElmt.childNodes[0].nodeValue);
		            titleElmt.appendChild(title);
		        }
	        }
	
	        var styleElmt = document.getElementById("fresnel_styles_" + uri);
	        if (styleElmt) {
	            var styles = styleElmt.getElementsByTagName("span");
	            for (var i = 0; i < styles.length; i++) {
	                registerStylesheet(Longwell.Configuration.resourcePath + styles[i].childNodes[0].nodeValue);
	            }
	        }
    	} else {
	    	if (xmlhttp.responseText.indexOf("lw_fresnel_available") > 0) {
			    var viewSelectors = Longwell.DOM.createDOMFromTemplate(document, completeViewSelectorTemplate);
			    elmt.appendChild(viewSelectors.elmt);
		
			    var container = document.createElement("div");
			    container.innerHTML = xmlhttp.responseText;
			    elmt.appendChild(container);
	    	} else {
	    		elmt.innerHTML = xmlhttp.responseText;
	    	}
    	}
    };
   
    Longwell.XmlHttp.post(this._viewPanel.getUI().getItemViewURL(uri, fresnel), "", fError, fDone);
};

Longwell.UI.ListView.prototype._onSortAscendingCheckboxClick = function(checkbox, evt, target) {
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

Longwell.UI.ListView.prototype._onSortSelectChange = function(select, evt, target) {
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