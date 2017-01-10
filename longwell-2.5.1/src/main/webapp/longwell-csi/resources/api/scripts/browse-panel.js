/*======================================================================
 *  BrowsePanel
 *======================================================================
 */
Longwell.UI.BrowsePanel = function(ui, div) {
    this._ui = ui;
    this._div = div;
    this._div.className = this._div.className.split(" ").concat("longwell-browsePanel").join(" ");
    
    this._facets = [];
    
    this._changeFacetActions = [];
    
    this._facetsToUpdate = [];
    this._updateAllFacets = false;
    this._queuedFacetUpdates = false;
    
    this._xmlHttpQueue = new Longwell.XmlHttpQueue();
};

Longwell.UI.BrowsePanel.prototype.getUI = function() { return this._ui; };

Longwell.UI.BrowsePanel.prototype.initialize = function(mode) {
    if (mode == Longwell.UI.FRONT_PAGE) {
        this._initializeFrontPage();
    } else if (mode == Longwell.UI.NORMAL_PAGE) {
        this._initializeNormalPage();
    }
};

Longwell.UI.BrowsePanel.prototype.toJSON = function() {
    return null;
};

Longwell.UI.BrowsePanel.prototype._initializeFrontPage = function() {
    this._div.style.display = "none";
    this._div.innerHTML = "";
    
    this._ui.getLongwell().getQE().unregisterListener(this);
};

Longwell.UI.BrowsePanel.prototype._initializeNormalPage = function() {
    for (var i = 0; i < this._facets.length; i++) {
        this._facets[i].dispose();
    }
    this._facets = [];
    
    this._div.innerHTML = "";
    this._div.style.display = "block";
    
    var progressAnimation = Longwell.Graphics.createAnimationIcon();
    this._xmlHttpQueue.setAnimation(progressAnimation);
    
    var template = {
        elmt: this._div,
        children: [
            {   tag: "div",
                className: "longwell-browsePanel-progress",
                children: [ { elmt: progressAnimation.element } ]
            },
            {   tag: "h2",
                children: [ "Text Search" ]
            },
            {   tag: "div",
                className: "longwell-browsePanel-searchContainer",
                field: "searchesDiv"
            },
            {   tag: "div",
                className: "longwell-browsePanel-searchBox",
                children: [
                    {   tag: "input",
                        type: "text",
                        field: "searchInput"
                    }
                ]
            },
            {   tag: "p"
            },
            {   tag: "h2",
                children: [ "Attribute Filters" ]
            },
            {   tag: "div",
                className: "longwell-browsePanel-facetContainer",
                field: "facetsDiv"
            },
            {   tag: "div",
                className: "longwell-browsePanel-message",
                field: "messageDiv",
                children: [
                    "Loading..."
                ]
            }
        ]
    }
    this._dom = Longwell.DOM.createDOMFromTemplate(document, template);
    this._ui.registerEventWithObject(this._dom.searchInput, "keyup", this, this._onSearchKeyUp, 0);
    
    /*
     *  Append the text searches
     */
    var searches = this._ui.getLongwell().getQE().getCurrentFacets().getTextSearches();
    for (var i = 0; i < searches.length; i++) {
        var node = document.createElement("div");
        node.className = "search";
        node.appendChild(document.createTextNode(searches[i]));
        
        this._ui.registerEventWithObject(
            node, "click", this, this._onSearchClick, 0);
            
        this._dom.searchesDiv.appendChild(node);
    }

    this._ui.getLongwell().getQE().registerListener(this);
    this.onCurrentFacetsChange();
};

Longwell.UI.BrowsePanel.prototype.onCurrentFacetsChange = function() {
    var browsePanel = this;
    this._xmlHttpQueue.queue(function(cont) {
        browsePanel._ui.getLongwell().callAPI({
            call:   "fetch-common-properties",
            body:   browsePanel._ui.getLongwell().getQE().getXML("query", ""),
            fDone:  function(o) {
                try {
                    browsePanel._onFetchCommonProperties(o);
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

Longwell.UI.BrowsePanel.prototype.requestFacetUpdate = function(facet) {
    this._facetsToUpdate.push(facet);
    this._queueUpdateFacets(true);
};

Longwell.UI.BrowsePanel.prototype.queueChangeFacetSelectionAction = function(action) {
    this._changeFacetActions.push(action);
    
    var browsePanel = this;
    this._xmlHttpQueue.queue(function(cont) { browsePanel._performActions(cont); });
};

Longwell.UI.BrowsePanel.prototype._performActions = function(cont) {
    try {
        var transaction = this._ui.getLongwell().getQE().createTransaction();
        
        for (var i = 0; i < this._changeFacetActions.length; i++) {
            this._changeFacetActions[i].perform(transaction);
        }
        
        this._changeFacetActions = [];
        
        transaction.commit();
        // soon we'll get notified from the QE
    } finally {
        cont();
    }
};

Longwell.UI.BrowsePanel.prototype._onFetchCommonProperties = function(o) {
    var hideProperties = Longwell.Configuration.ui.browsePanel.hideProperties;
    
    var topMostProperties = [].concat(Longwell.Configuration.ui.browsePanel.topMostProperties);
    var topMostPropertyIndex = {};
    for (var i = 0; i < topMostProperties.length; i++) {
        topMostPropertyIndex[topMostProperties[i]] = i;
    }
    
    this._dom.messageDiv.style.display = "none";
    
    var autoOpenFacets = this._facets.length == 0;
    var divFacets = this._dom.facetsDiv;
    
    var commonProperties = [];
    var newPropertyIndex = {};
    for (var i = 0; i < o.length; i++) {
        var p = o[i];
        if (!(p in hideProperties) || !hideProperties[p]) {
            commonProperties.push(p);
            newPropertyIndex[p] = true;
        }
    }
    commonProperties.sort(function(p1, p2) {
        var i1 = p1 in topMostPropertyIndex ? topMostPropertyIndex[p1] : Number.MAX_VALUE;
        var i2 = p2 in topMostPropertyIndex ? topMostPropertyIndex[p2] : Number.MAX_VALUE;
        if (i1 < i2) {
            return -1;
        } else if (i1 > i2) {
            return 1;
        }
        
        p1 = Longwell.ProfileData.propertyIndex[p1];
        p2 = Longwell.ProfileData.propertyIndex[p2];
        
        return p1.label.localeCompare(p2.label);
    });
    
    /*
     *  Remove DOMs of old facets that have no selection
     */
    var oldPropertyIndex = [];
    for (var i = 0; i < this._facets.length; i++) {
        var facet = this._facets[i];
        if (!facet.hasSelection() && !(facet.getPropertyURI() in newPropertyIndex)) {
            facet.dispose();
            this._facets.splice(i, 1);
            
            divFacets.removeChild(divFacets.childNodes[i]);
            
            i--;
        } else {
            oldPropertyIndex[facet.getPropertyURI()] = true;
        }
    }
    
    /*
     *  Construct DOMs for new facets
     */
    for (var i = 0; i < commonProperties.length; i++) {
        var propertyURI = commonProperties[i];
        if (!(propertyURI in oldPropertyIndex)) {
            this._constructFacetDOM(propertyURI, divFacets);
        }
    }
    
    if (autoOpenFacets) {
        if (topMostProperties.length > 0) {
            var facetsToOpen = [];
            for (var i = 0; i < this._facets.length && facetsToOpen.length < 3; i++) {
                var facet = this._facets[i];
                if (facet.getPropertyURI() in topMostPropertyIndex) {
                    facetsToOpen.push(facet);
                }
            }
        } else {
            var facetsToOpen = [].concat(this._facets);
            facetsToOpen.sort(function(f1, f2) {
                p1 = Longwell.ProfileData.propertyIndex[f1.getPropertyURI()];
                p2 = Longwell.ProfileData.propertyIndex[f2.getPropertyURI()];
                
                return p1.uniqueness - p2.uniqueness;
            });
            facetsToOpen = facetsToOpen.slice(0, 2);
        }
        
        for (i = 0; i < facetsToOpen.length; i++) {
            facetsToOpen[i].open();
        }
    }
    
    this._queueUpdateFacets(false);
};

Longwell.UI.BrowsePanel.prototype._constructFacetDOM = function(propertyURI, divFacets) {
    var property = Longwell.ProfileData.propertyIndex[propertyURI];
    var valueClass = Longwell.QueryFacet.getValueClassFromProperty(property);
    var type = Longwell.QueryFacet.getFacetTypeFromValueClass(valueClass);
    
    var template = {
        tag:         "div",
        className:   "longwell-browsePanel-facet",
        propertyURI: propertyURI,
        forward:     "true",
        type:        type,
        children: [
            {   tag: "div",
                className: "longwell-browsePanel-facet-title",
                field: "divTitle",
                children: [
                    {   tag:        "div",
                        field:      "divSelection",
                        className:  "selection",
                        title:      "Un-select all",
                        children: [
                            "0",
                            { elmt: Longwell.Graphics.createTranslucentImage(document, Longwell.urlPrefix + "images/black-check.png") }
                        ]
                    },
                    {   elmt: Longwell.Graphics.createTranslucentImage(document, Longwell.urlPrefix + "images/right-arrow.png"),
                        field: "imgRightArrow"
                    },
                    {   elmt: Longwell.Graphics.createTranslucentImage(document, Longwell.urlPrefix + "images/down-arrow.png"),
                        field: "imgDownArrow"
                    },
                    property.label
                ]
            },
            {   tag: "div",
                className: "longwell-browsePanel-facet-body",
                field: "divBody",
                children: [
                    {   tag: "div",
                        className: "longwell-browsePanel-facet-message",
                        children: [
                            {   tag: "img",
                                src: Longwell.urlPrefix + "images/progress-running.gif"
                            },
                            " Loading..."
                        ]
                    }
                ]
            }
        ]
    };
    var result = Longwell.DOM.createDOMFromTemplate(document, template);
    
    var facet = null;
    if (type == Longwell.QueryFacet.LIST) {
        facet = new Longwell.UI.ListFacet(propertyURI, true, valueClass, this, result.elmt);
    } else if (type == Longwell.QueryFacet.RANGE) {
        facet = new Longwell.UI.RangeFacet(propertyURI, valueClass, this, result.elmt);
    }
    if (facet != null) {
        facet._setUI = function() {
            var classes = [ "longwell-browsePanel-facet" ];
            if (this._opened) {
                classes.push("longwell-browsePanel-facet-open");
                result.imgRightArrow.style.display = "none";
                result.imgDownArrow.style.display = "inline";
            } else {
                result.imgRightArrow.style.display = "inline";
                result.imgDownArrow.style.display = "none";
            }
            if (this._hasSelection) {
                classes.push("longwell-browsePanel-facet-hasSelection");
            }
            this._div.className = classes.join(" ");
        };
        
        result.imgDownArrow.style.display = "none";
        this._ui.registerEventWithObject(result.divSelection, "click", this, this._onFacetTitleSelectionClick, 0);
        this._ui.registerEventWithObject(result.divTitle, "click", facet, facet.toggle, 0);
        
        divFacets.appendChild(result.elmt);
        this._facets.push(facet);
    }
};

Longwell.UI.BrowsePanel.prototype._queueUpdateFacets = function(requesting) {
    if (!requesting) {
        this._updateAllFacets = true;
    }
    if (!this._queuedFacetUpdates) {
        this._queuedFacetUpdates = true;
        
        var browsePanel = this;
        this._xmlHttpQueue.queue(function(cont) { 
            browsePanel._queuedFacetUpdates = false;
            browsePanel._updateFacets(cont); 
        });
    }
};

Longwell.UI.BrowsePanel.prototype._updateFacets = function(cont) {
    var s = "<update-several-facets>\n";
    s += this._ui.getLongwell().getQE().getXML("query", " ");
    s += " <facets>\n";
    
    var hasFacetsToUpdate = false;
    if (this._updateAllFacets) {
        for (var i = 0; i < this._facets.length; i++) {
            var facet = this._facets[i];
            if (facet.isOpened() || facet.hasSelection()) {
                s += facet.getXMLForUpdate(" ");
                hasFacetsToUpdate = true;
            }
        }
    } else {
        for (var i = 0; i < this._facetsToUpdate.length; i++) {
            var facet = this._facetsToUpdate[i];
            s += facet.getXMLForUpdate(" ");
            hasFacetsToUpdate = true;
        }
    }
    this._facetsToUpdate = [];
    this._updateAllFacets = false;

    s += " </facets>\n";
    s += "</update-several-facets>";
    
    if (hasFacetsToUpdate) {
        var browsePanel = this;
        this._ui.getLongwell().callAPI({
            call:   "update-several-facets",
            body:   s,
            fDone:  function(o) {
                try {
                    browsePanel._onUpdateSeveralFacets(o);
                } finally {
                    cont();
                }
            },
            fError: function(statusText, status, xmlhttp) {
                Longwell.Debug.log(statusText);
                cont();
            }
        });
    } else {
        cont();
    }
};

Longwell.UI.BrowsePanel.prototype._onUpdateSeveralFacets = function(o) {
    for (var i = 0; i < o.facets.length; i++) {
        var facetData = o.facets[i];
        for (var j = 0; j < this._facets.length; j++) {
            var facet = this._facets[j];
            if (facetData.propertyURI == facet.getPropertyURI() &&
                facetData.forward == facet.isForward()) {
                facet.update(facetData);
                break;
            }
        }
    }
};

Longwell.UI.BrowsePanel.prototype._onSearchKeyUp = function(inputSearch, evt, target) {
    if (evt.keyCode == 13) { // enter
        var text = inputSearch.value;
        var browsePanel = this;
        var fPerform = function() {
            browsePanel.queueChangeFacetSelectionAction({
                perform: function(transaction) {
                    transaction.addTextSearch(text);
                    browsePanel._addTextSearch(text);
                }
            });
        };
        var fUndo = function() {
            browsePanel.queueChangeFacetSelectionAction({
                perform: function(transaction) {
                    transaction.removeTextSearch(text);
                    browsePanel._removeTextSearch(text);
                }
            });
        };
        
        inputSearch.select(); // visual feedback
        
        var history = this._ui.getLongwell().getHistory();
        history.addAction({ perform: fPerform, undo: fUndo });
        
        Longwell.DOM.cancelEvent(evt);
        return false;
    }
};

Longwell.UI.BrowsePanel.prototype._addTextSearch = function(text) {
    var divSearches = this._dom.searchesDiv;
    var node = divSearches.firstChild;
    while (node != null) {
        if (node.firstChild.nodeValue == text) {
            return;
        }
        node = node.nextSibling;
    }
    
    node = document.createElement("div");
    node.className = "search";
    node.appendChild(document.createTextNode(text));
    this._ui.registerEventWithObject(
        node, "click", this, this._onSearchClick, 0);
    
    divSearches.appendChild(node);
};

Longwell.UI.BrowsePanel.prototype._removeTextSearch = function(text) {
    var divSearches = this._dom.searchesDiv;
    var node = divSearches.firstChild;
    while (node != null) {
        if (node.firstChild.nodeValue == text) {
            divSearches.removeChild(node);
            return;
        }
        node = node.nextSibling;
    }
};

Longwell.UI.BrowsePanel.prototype._onSearchClick = function(node, evt, target) {
    var text = node.firstChild.nodeValue;
    var browsePanel = this;
    var fPerform = function() {
        browsePanel.queueChangeFacetSelectionAction({
            perform: function(transaction) {
                transaction.removeTextSearch(text);
                browsePanel._removeTextSearch(text);
            }
        });
    };
    var fUndo = function() {
        browsePanel.queueChangeFacetSelectionAction({
            perform: function(transaction) {
                transaction.addTextSearch(text);
                browsePanel._addTextSearch(text);
            }
        });
    };
    
    var history = this._ui.getLongwell().getHistory();
    history.addAction({ perform: fPerform, undo: fUndo });
};

Longwell.UI.BrowsePanel.prototype._onFacetTitleSelectionClick = function(divSelection, evt, target) {
    var div = divSelection.parentNode.parentNode;
    var params = { 
        propertyURI:    div.getAttribute("propertyURI"),
        forward:        "true" == div.getAttribute("forward"),
        type:           parseInt(div.getAttribute("type"))
    };
    
    var browsePanel = this;
    var fPerform = function() {
        browsePanel.queueChangeFacetSelectionAction({
            perform: function(transaction) {
                transaction.removeFacet(params);
            }
        });
    };
    var fUndo = function() {
        browsePanel.queueChangeFacetSelectionAction({
            perform: function(transaction) {
                transaction.addFacet(params);
            }
        });
    };
    
    var history = this._ui.getLongwell().getHistory();
    history.addAction({ perform: fPerform, undo: fUndo });
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};