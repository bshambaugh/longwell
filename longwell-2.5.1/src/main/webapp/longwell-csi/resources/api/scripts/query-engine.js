/*======================================================================
 *  QueryEngine
 *======================================================================
 */
Longwell.QueryEngine = function(longwell, params) {
    this._longwell = longwell;
    
    this._rootFacets = new Longwell.QueryFacetCollection((params != null && "root" in params) ? 
        params.root : 
        Longwell.Configuration.queryEngine.rootQuery
    );
    this._currentFacets = new Longwell.QueryFacetCollection((params != null && "current" in params) ? params.current : []);
    
    this._listeners = [];
};

Longwell.QueryEngine.prototype.registerListener = function(listener) {
    this._listeners.push(listener);
};

Longwell.QueryEngine.prototype.unregisterListener = function(listener) {
    for (var i = 0; i < this._listeners.length; i++) {
        if (this._listeners[i] === listener) {
            this._listeners.splice(i, 1);
            break;
        }
    }
};

Longwell.QueryEngine.prototype.getRootFacets = function() { return this._rootFacets; };
Longwell.QueryEngine.prototype.getCurrentFacets = function() { return this._currentFacets; };

Longwell.QueryEngine.prototype.toJSON = function() {
    return {
        root:       this._rootFacets.toJSON(),
        current:    this._currentFacets.toJSON()
    };
};

Longwell.QueryEngine.prototype.hasNoSelection = function() {
    return this._currentFacets.isEmpty();
};

Longwell.QueryEngine.prototype.createTransaction = function(fOnCommit) {
    return new Longwell.QueryEngine._Transaction(this, fOnCommit);
};

Longwell.QueryEngine.prototype.clearFacets = function() {
    var currentFacets = this._currentFacets;
    this._currentFacets = new Longwell.QueryFacetCollection([]);
    
    this._notifyListeners(function(listener) { listener.onCurrentFacetsChange(); });
    
    return currentFacets;
};

Longwell.QueryEngine.prototype.restoreFacets = function(oldFacets) {
    this._currentFacets = oldFacets;
    this._notifyListeners(function(listener) { listener.onCurrentFacetsChange(); });
};

Longwell.QueryEngine.prototype._addFacetSelection = function(params) {
    this._currentFacets.addFacetSelection(params);
};

Longwell.QueryEngine.prototype._removeFacetSelection = function(params) {
    this._currentFacets.removeFacetSelection(params);
};

Longwell.QueryEngine.prototype._removeFacet = function(params) {
    var facet = this._currentFacets.getFacet(params.type, params.propertyURI, params.forward);
    if (facet != null) {
        this._currentFacets.removeFacet(facet);
    }
    return facet;
};

Longwell.QueryEngine.prototype._addFacet = function(facet) {
    this._currentFacets.addFacet(facet);
};

Longwell.QueryEngine.prototype._addTextSearch = function(text) {
    this._addFacetSelection({
        type:           Longwell.QueryFacet.TEXT,
        propertyURI:    "",
        forward:        true,
        valueClass:     "java.lang.String",
        selection:      { value: text }
    });
};

Longwell.QueryEngine.prototype._removeTextSearch = function(text) {
    this._removeFacetSelection({
        type:           Longwell.QueryFacet.TEXT,
        propertyURI:    "",
        forward:        true,
        valueClass:     "java.lang.String",
        selection:      { value: text }
    });
};

Longwell.QueryEngine.prototype._notifyListeners = function(f) {
    var listeners = [].concat(this._listeners);
    for (var i = 0; i < listeners.length; i++) {
        try {
            f(listeners[i]);
        } catch (e) {
            Longwell.Debug.exception(e);
        }
    }
};

Longwell.QueryEngine.prototype.getXML = function(elmtName, indent) {
    var s = indent + "<" + elmtName + ">\n";
    var indent2 = indent + " ";
    s += this._rootFacets.getXML("root", indent2);
    s += this._currentFacets.getXML("current", indent2);
    s += indent + "</" + elmtName + ">\n";
    return s;
};

/*======================================================================
 *  Transaction
 *======================================================================
 */

Longwell.QueryEngine._Transaction = function(qe, fOnCommit) {
    this._qe = qe;
    this._fOnCommit = fOnCommit;
    this._committed = false;
    this._actions = [];
};

Longwell.QueryEngine._Transaction.prototype.addFacetSelection = function(params) {
    if (this._committed) {
        throw new Error("Transaction already committed");
    }
    
    var qe = this._qe;
    this._actions.push(function() {
        qe._addFacetSelection(params);
    });
};

Longwell.QueryEngine._Transaction.prototype.removeFacetSelection = function(params) {
    if (this._committed) {
        throw new Error("Transaction already committed");
    }
    
    var qe = this._qe;
    this._actions.push(function() {
        qe._removeFacetSelection(params);
    });
};

Longwell.QueryEngine._Transaction.prototype.removeFacet = function(params) {
    if (this._committed) {
        throw new Error("Transaction already committed");
    }
    
    var qe = this._qe;
    this._actions.push(function() {
        params.facet = qe._removeFacet(params);
    });
};

Longwell.QueryEngine._Transaction.prototype.addFacet = function(params) {
    if (this._committed) {
        throw new Error("Transaction already committed");
    }
    
    var qe = this._qe;
    this._actions.push(function() {
        qe._addFacet(params.facet);
    });
};

Longwell.QueryEngine._Transaction.prototype.addTextSearch = function(text) {
    if (this._committed) {
        throw new Error("Transaction already committed");
    }
    
    var qe = this._qe;
    this._actions.push(function() {
        qe._addTextSearch(text);
    });
};

Longwell.QueryEngine._Transaction.prototype.removeTextSearch = function(text) {
    if (this._committed) {
        throw new Error("Transaction already committed");
    }
    
    var qe = this._qe;
    this._actions.push(function() {
        qe._removeTextSearch(text);
    });
};

Longwell.QueryEngine._Transaction.prototype.commit = function() {
    if (this._committed) {
        throw new Error("Transaction already committed");
    }
    this._committed = true;
    
    for (var i = 0; i < this._actions.length; i++) {
        this._actions[i]();
    }
    this._actions = [];
    
    this._qe._notifyListeners(function(listener) { listener.onCurrentFacetsChange(); });
    
    if (this._fOnCommit != null) {
        this._fOnCommit();
    }
};

Longwell.QueryEngine._Transaction.prototype.isCommitted = function() {
    return this._committed;
};
