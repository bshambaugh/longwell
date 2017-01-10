Longwell.UI.ViewPanel = function(ui, div, settings) {
    this._ui = ui;
    this._div = div;
    this._div.className = this._div.className.split(" ").concat("longwell-viewPanel").join(" ");
    
    this._viewEntries = [];
    
    if (settings != null) {
        for (var i = 0; i < settings.length; i++) {
            var entry = settings[i];
            this._viewEntries.push(entry);
        }
    }
};

Longwell.UI.ViewPanel._viewInfos = null;
Longwell.UI.ViewPanel._staticInitialize = function() {
    if (Longwell.UI.ViewPanel._viewInfos == null) {
        Longwell.UI.ViewPanel._viewInfos = []
        Longwell.UI.ViewPanel._viewInfos["tabular-view"] = {
            constructor:    Longwell.UI.TabularView,
            label:          "Table",
            icon:           Longwell.urlPrefix + "images/table-icon.png"
        };
        Longwell.UI.ViewPanel._viewInfos["list-view"] = {
            constructor:    Longwell.UI.ListView,
            label:          "List",
            icon:           Longwell.urlPrefix + "images/list-icon.png"
        };
        Longwell.UI.ViewPanel._viewInfos["timeline-view"] = {
            constructor:    Longwell.UI.TimelineView,
            label:          "Time",
            icon:           Longwell.urlPrefix + "images/timeline-icon.png"
        };
        Longwell.UI.ViewPanel._viewInfos["map-view"] = {
            constructor:    Longwell.UI.MapView,
            label:          "Map",
            icon:           Longwell.urlPrefix + "images/map-icon.png"
        };
        Longwell.UI.ViewPanel._viewInfos["grid-view"] = {
            constructor:    Longwell.UI.GridView,
            label:          "Images",
            icon:           Longwell.urlPrefix + "images/grid-icon.png"
        };
    }
}

Longwell.UI.ViewPanel.prototype.getUI = function() { return this._ui; };

Longwell.UI.ViewPanel.prototype.initialize = function(mode) {
    Longwell.UI.ViewPanel._staticInitialize();
    
    if (mode == Longwell.UI.FRONT_PAGE) {
        this._initializeFrontPage();
    } else if (mode == Longwell.UI.NORMAL_PAGE) {
        this._initializeNormalPage();
    }
};

Longwell.UI.ViewPanel.prototype.toJSON = function() {
    var entries = [];
    for (var i = 0; i < this._viewEntries.length; i++) {
        var entry = this._viewEntries[i];
        if (entry.view != null) {
            entries.push({ name: entry.name, settings: entry.view.getSettings() });
        }
    }
    return entries;
};

Longwell.UI.ViewPanel.prototype.switchView = function(oldView, newViewName, newViewSettings) {
    var viewEntry;
    var viewIndex = 0;
    while (viewIndex < this._viewEntries.length) {
        viewEntry = this._viewEntries[viewIndex];
        if (viewEntry.view == oldView) {
            break;
        }
        viewIndex++;
    }
    if (viewIndex >= this._viewEntries.length) {
        return;
    }
    
    var oldViewName = viewEntry.name;
    var oldViewSettings = oldView.getSettings();
    
    var vp = this;
    this._ui.getLongwell().getHistory().addAction({
        perform: function() {
            vp._switchView(newViewName, newViewSettings, viewIndex);
        },
        undo: function() {
            vp._switchView(oldViewName, oldViewSettings, viewIndex);
        }
    });
};

Longwell.UI.ViewPanel.prototype.removeView = function(oldView) {
    var viewEntry;
    var viewIndex = 0;
    while (viewIndex < this._viewEntries.length) {
        viewEntry = this._viewEntries[viewIndex];
        if (viewEntry.view == oldView) {
            break;
        }
        viewIndex++;
    }
    if (viewIndex >= this._viewEntries.length) {
        return;
    }
    
    var oldViewName = viewEntry.name;
    var oldViewSettings = oldView.getSettings();
    
    var vp = this;
    this._ui.getLongwell().getHistory().addAction({
        perform: function() {
            vp._removeView(viewIndex);
        },
        undo: function() {
            vp._insertView(oldViewName, oldViewSettings, viewIndex);
        }
    });
};

Longwell.UI.ViewPanel.prototype.addView = function() {
    var dialog = Longwell.Graphics.createPopupDialog(this._ui, "Add View", true);
    
    var vp = this;
    var viewIndex = this._viewEntries.length;
    
    var onClick = function(a, evt, target) {
        var viewName = a.getAttribute("viewName");
        vp._ui.getLongwell().getHistory().addAction({
            perform: function() {
                vp._insertView(viewName, {}, viewIndex);
            },
            undo: function() {
                vp._removeView(viewIndex);
            }
        });
        
        dialog.close();
        
        Longwell.DOM.cancelEvent(evt);
        return false;
    };
    
    dialog.dialogDiv.style.left = "40%";
    dialog.dialogDiv.style.right = "40%";
    dialog.dialogDiv.style.top = "15em";
    
        var p = document.createElement("p");
        p.appendChild(document.createTextNode("Choose the type of view to add:"));
        dialog.bodyDiv.appendChild(p);
        
        for (viewName in Longwell.UI.ViewPanel._viewInfos) {
            var info = Longwell.UI.ViewPanel._viewInfos[viewName];
            if (typeof info == "object") {
                var a = document.createElement("a");
                a.className = "longwell-menu-item";
                a.href = "javascript:";
                a.setAttribute("viewName", viewName);
                dialog.bodyDiv.appendChild(a);
                this._ui.registerEvent(a, "click", onClick, 1);
                
                var div = document.createElement("div");
                a.appendChild(div);
                
                var img = Longwell.Graphics.createTranslucentImage(document, info.icon);
                div.appendChild(img);
                
                div.appendChild(document.createTextNode(" " + info.label));
            }
        }
        
    dialog.open();
};

Longwell.UI.ViewPanel.prototype._initializeFrontPage = function() {
    /*
     *  Save existing views' settings
     */
    for (var i = 0; i < this._viewEntries.length; i++) {
        var entry = this._viewEntries[i];
        entry.settings = entry.view.getSettings();
        entry.view.dispose();
        entry.view = null;
        entry.viewDiv = null;
    }
    
    this._div.innerHTML = "";
    var template = {
        elmt: this._div,
        children: [
            {   tag: "div",
                className: "longwell-viewPanel-frontPage-message",
                children: [
                    {   tag: "img",
                        src: Longwell.urlPrefix + "images/progress-running.gif"
                    },
                    " Loading..."
                ]
            }
        ]
    };
    Longwell.DOM.createDOMFromTemplate(document, template);
    
    this._fetchStartingPointFacets();
};

Longwell.UI.ViewPanel.prototype._fetchStartingPointFacets = function() {
    var s = "<update-several-facets>\n";
    s += this._ui.getLongwell().getQE().getXML("query", " ");
    s += " <facets>\n";
    
    var startingPoints = Longwell.Configuration.ui.viewPanel.startingPoints;
    for (var i = 0; i < startingPoints.length; i++) {
        var startingPoint = startingPoints[i];
        s += "<facet" +
            " propertyURI='" + startingPoint.propertyURI + "'" +
            " forward='" + startingPoint.forward + "'" +
            " />\n";
    }

    s += " </facets>\n";
    s += "</update-several-facets>";
    
    var viewPanel = this;
    this._ui.getLongwell().callAPI({
        call:   "update-several-facets",
        body:   s,
        fDone:  function(o) {
            viewPanel._onFetchStartingPointFacets(o);
        },
        fError: function(statusText, status, xmlhttp) {
            Longwell.Debug.log(statusText);
            cont();
        }
    });
};

Longwell.UI.ViewPanel.prototype._onFetchStartingPointFacets = function(o) {
    var startingPoints = Longwell.Configuration.ui.viewPanel.startingPoints;
    
    var table = document.createElement("table");
    
    var tr = table.insertRow(0);
    var template = {
        elmt: tr,
        vAlign: "top",
        children: [
            {   tag: "table",
                className: "longwell-viewPanel-frontPage-search-container",
                children: [
                    { tag: "tr",
                      children: [
                          {   tag: "td",
                              className: "longwell-viewPanel-frontPage-search-left" },
                          {   tag: "td",
                              colspan: startingPoints.length,
                              className: "longwell-viewPanel-frontPage-search",
                              children: [
                                  {   tag: "input",
                                      type: "text",
                                      value: "Type here to search",
                                      size: "40",
                                      field: "searchInput"
                                  }
                              ]
                          },
                          {   tag: "td",
                              className: "longwell-viewPanel-frontPage-search-right" }
                      ]
                   }
                ]
            }
        ]
    };
    var dom = Longwell.DOM.createDOMFromTemplate(document, template);
    this._ui.registerEventWithObject(dom.searchInput, "blur", this, this._onSearchInputBlur, 0);
    this._ui.registerEventWithObject(dom.searchInput, "focus", this, this._onSearchInputFocus, 0);
    this._ui.registerEventWithObject(dom.searchInput, "keyup", this, this._onSearchInputKeyUp, 0);
    
    tr = table.insertRow(1);
    tr.vAlign = "top";
    
    for (var i = 0; i < startingPoints.length; i++) {
        var startingPoint = startingPoints[i];
        
        var facet = null;
        var j = 0;
        while (j < o.facets.length) {
            facet = o.facets[j];
            if (facet.propertyURI == startingPoint.propertyURI &&
                facet.forward == startingPoint.forward) {
                break;
            }
            j++;
        }
        
        if (j >= o.facets.length) {
            continue;
        }
        
        var td = tr.insertCell(tr.cells.length);
        td.appendChild(this._constructStartingPointFacet(startingPoint, facet));
    }
    this._div.innerHTML = "";
    this._div.appendChild(table);
};

Longwell.UI.ViewPanel.prototype._constructStartingPointFacet = function(startingPoint, facet) {
    facet.items.sort(function(item1, item2) {
        //return item1.value.localeCompare(item2.value);
        return item2.count - item1.count;
    });
    
    var div = document.createElement("div");
    div.className = "longwell-viewPanel-frontPage-facet";
    
    var h1 = document.createElement("h1");
    h1.innerHTML = startingPoint.title;
    div.appendChild(h1);
    
    var ul = document.createElement("ul");
    div.appendChild(ul);
    
    for (var i = 0; i < facet.items.length; i++) {
        var item = facet.items[i];
        var li = document.createElement("li");
        li.className = (i % 2 == 0) ? "even" : "odd";
        
        var countDiv = document.createElement("div");
        countDiv.className = "longwell-viewPanel-frontPage-facet-count";
        countDiv.appendChild(document.createTextNode(item.count));
        li.appendChild(countDiv);
        
        var a = document.createElement("a");
        a.href = "javascript:";
        a.innerHTML = item.label;
        a.setAttribute("value", item.value);
        a.setAttribute("propertyURI", startingPoint.propertyURI);
        a.setAttribute("forward", startingPoint.forward ? "true" : "false");
        this._ui.registerEventWithObject(a, "click", this, this._onStartingPointClick, 0);
        li.appendChild(a);
        
        ul.appendChild(li);
    }
    
    return div;
};

Longwell.UI.ViewPanel.prototype._onStartingPointClick = function(a, evt, target) {
    var viewPanel = this;
    var params = {
        type:           Longwell.QueryFacet.LIST,
        propertyURI:    a.getAttribute("propertyURI"),
        forward:        a.getAttribute("forward") == "true",
        valueClass:     "org.openrdf.model.Value",
        selection:      { value: a.getAttribute("value") }
    };
    
    var fDo = function() {
        var transaction = viewPanel._ui.getLongwell().getQE().createTransaction();
        transaction.addFacetSelection(params);
        transaction.commit();
    };
    var fUndo = function() {
        var transaction = viewPanel._ui.getLongwell().getQE().createTransaction();
        transaction.removeFacetSelection(params);
        transaction.commit();
    };
    this._ui.getLongwell().getHistory().addAction({ perform: fDo, undo: fUndo });
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.ViewPanel.prototype._onSearchInputBlur = function(input, evt, target) {
    if (input.value=='') input.value='Type here to search';
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.ViewPanel.prototype._onSearchInputFocus = function(input, evt, target) {
    if (input.value=='Type here to search') input.value='';
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.ViewPanel.prototype._onSearchInputKeyUp = function(input, evt, target) {
    if (evt.keyCode == 13) {
        var viewPanel = this;
        var text = input.value;
        var fDo = function() {
            var transaction = viewPanel._ui.getLongwell().getQE().createTransaction();
            transaction.addTextSearch(text);
            transaction.commit();
        };
        var fUndo = function() {
            var transaction = viewPanel._ui.getLongwell().getQE().createTransaction();
            transaction.removeTextSearch(text);
            transaction.commit();
        };
        this._ui.getLongwell().getHistory().addAction({ perform: fDo, undo: fUndo });
        
        Longwell.DOM.cancelEvent(evt);
        return false;
    }
};

Longwell.UI.ViewPanel.prototype._initializeNormalPage = function() {
    this._div.innerHTML = "";
    if (this._viewEntries.length == 0) {
        this._insertView("list-view", {}, 0);
    } else { // restore previous views
        var oldEntries = this._viewEntries;
        this._viewEntries = [];
        
        for (var i = 0; i < oldEntries.length; i++) {
            var entry = oldEntries[i];
            this._insertView(entry.name, entry.settings, i);
        }
    }
};

Longwell.UI.ViewPanel.prototype._switchView = function(viewName, viewSettings, viewIndex) {
    this._removeView(viewIndex);
    this._insertView(viewName, viewSettings, viewIndex);
};

Longwell.UI.ViewPanel.prototype._removeView = function(viewIndex) {
    if (viewIndex < this._viewEntries.length) {
        this._viewEntries[viewIndex].view.dispose();
        this._div.removeChild(this._viewEntries[viewIndex].viewDiv);
        
        this._viewEntries.splice(viewIndex, 1);
        
        this._div.setAttribute("multiple", this._viewEntries.length > 1);
    }
};

Longwell.UI.ViewPanel.prototype._insertView = function(viewName, viewSettings, viewIndex) {
    var info = Longwell.UI.ViewPanel._viewInfos[viewName];
    var viewEntry = { name: viewName };
    
    viewEntry.viewDiv = this._createViewDiv(info, viewEntry);
    viewEntry.view = new info.constructor(this, viewEntry.viewDiv, viewSettings);
    
    this._viewEntries.splice(viewIndex, 0, viewEntry);
    if (viewIndex < this._div.childNodes.length) {
        this._div.insertBefore(viewEntry.viewDiv, this._div.childNodes[viewIndex]);
    } else {
        this._div.appendChild(viewEntry.viewDiv);
    }
    viewEntry.viewDiv.scrollIntoView();
    viewEntry.view.initialize();
    
    this._div.setAttribute("multiple", this._viewEntries.length > 1);
};

Longwell.UI.ViewPanel.prototype._createViewDiv = function(info, viewEntry) {
    var viewPanel = this;
    
    var viewDiv = document.createElement("div");
    viewDiv.className = "longwell-viewPanel-viewContainer";
    
    /*
     *  View closing button
     */
    var divClose = document.createElement("close");
    divClose.className = "closeView";
    viewDiv.appendChild(divClose);
    this._ui.registerEvent(divClose, "click", function() {
        viewPanel.removeView(viewEntry.view);
    }, 0);
    
    /*
     *  View switching menu
     */
    var viewSelectorDiv = document.createElement("div");
    viewSelectorDiv.className = "viewSelector";
    viewSelectorDiv.title = "Show Results in Different Ways";
    viewDiv.appendChild(viewSelectorDiv);
    
    var viewSelectorIndicator = Longwell.Graphics.createTranslucentImage(document, Longwell.urlPrefix + "images/down-arrow.png");
    viewSelectorDiv.appendChild(viewSelectorIndicator);

        viewSelectorDiv.appendChild(document.createTextNode(info.label + " "));
        
        var img = Longwell.Graphics.createTranslucentImage(document, info.icon);
        viewSelectorDiv.appendChild(img);
        
        var menu = Longwell.Graphics.createPopupMenu(viewSelectorDiv, this._ui);
        menu.element.style.width = "15em";
            
            var onClick = function(a, evt, target) {
                viewPanel._onSwitchViewClick(a, evt, viewEntry);
            };
            
            for (viewName in Longwell.UI.ViewPanel._viewInfos) {
                var info2 = Longwell.UI.ViewPanel._viewInfos[viewName];
                if (typeof info2 == "object" && info2 != info) {
                    var a = document.createElement("a");
                    a.className = "longwell-menu-item";
                    a.href = "javascript:";
                    a.setAttribute("viewName", viewName);
                    menu.element.appendChild(a);
                    this._ui.registerEvent(a, "click", onClick, 0);
                    
                    var div = document.createElement("div");
                    a.appendChild(div);
                    
                    var img2 = Longwell.Graphics.createTranslucentImage(document, info2.icon);
                    div.appendChild(img2);
                    
                    div.appendChild(document.createTextNode(" " + info2.label));
                }
            }
    
    return viewDiv;
};

Longwell.UI.ViewPanel.prototype._onSwitchViewClick = function(a, evt, viewEntry) {
    this.switchView(viewEntry.view, a.getAttribute("viewName"), {});
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};
