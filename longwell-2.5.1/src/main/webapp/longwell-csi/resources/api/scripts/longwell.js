Longwell.RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

Longwell.create = function(controlDiv, browseDiv, viewDiv) {
    return Longwell.createFromLocation(controlDiv, browseDiv, viewDiv, document.location);
};

Longwell.createFromLocation = function(controlDiv, browseDiv, viewDiv, location) {
    var query = location.search;
    var parameters = [];
    if (query.length > 0) {
        if (query.substr(0,1) == "?") {
            query = query.substr(1);
        }
        parameters = query.split("&");
    }
    
    var startWith = function(string, substring) {
        return string.substr(0, substring.length) == substring;
    }
    
    var state = null;
    for (var i = 0; i < parameters.length; i++) {
        if (startWith(parameters[i], "longwell-state=")) {
            try {
                state = decodeURIComponent(parameters[i].substr("longwell-state=".length)).parseJSON();
            } catch (e) {
                console.log(e);
            }
            break;
        }
    }
    
    var queries = (state != null && "queries" in state) ? state.queries : {};
    var views = (state != null && "views" in state) ? state.views : {};
    
    return Longwell.createFromParams({
        controlDiv:         controlDiv,
        browseDiv:          browseDiv,
        viewDiv:            viewDiv,
        queries:            queries,
        views:              views
    });
};

Longwell.createFromParams = function(params) {
    return new Longwell._Impl(params);
};

Longwell._Impl = function(params) {
    this._history = new Longwell.History(this);
    this._qe = new Longwell.QueryEngine(this, params.queries);
    this._ui = new Longwell.UI(this, params.controlDiv, params.browseDiv, params.viewDiv, params.views);
    
    this._history.initialize();
    this._ui.initialize();
};

Longwell._Impl.prototype.getQE = function() { return this._qe; };
Longwell._Impl.prototype.getHistory = function() { return this._history; };
Longwell._Impl.prototype.getUI = function() { return this._ui; };

Longwell._Impl.prototype.toJSON = function() {
    return {
        queries:    this._qe.toJSON(),
        views:      this._ui.toJSON()
    };
};

Longwell._Impl.prototype.callAPIWithObject = function(params) {
    var obj = params.obj;
    var fDone = params.fDone;
    params.fDone = function(o) { fDone.call(obj, o); };
    
    this.callAPI(params);
}

Longwell._Impl.prototype.callAPI = function(params) {
    var parameters = [ "command=api", "call=" + params.call ];
    if ("parameters" in params) {
        parameters = parameters.concat(params.parameters);
    }
    
    var url = Longwell.Configuration.contextPath +
        Longwell.Configuration.profileID + "?" + parameters.join("&");
        
    var fError = ("fError" in params) ? params.fError :
        function(statusText, status, xmlhttp) {
            Longwell.Debug.exception(statusText);
        };
    var fDone = function(xmlhttp) {
        var o = null;
        try {
            o = eval("(" + xmlhttp.responseText + ")");
        } catch (e) {
            fError("Invalid JSON\n" + e, null, xmlhttp);
            return;
        }
        params.fDone(o);
    };
    
    if ("body" in params) {
        Longwell.XmlHttp.post(url, params.body, fError, fDone);
    } else {
        Longwell.XmlHttp.get(url, fError, fDone);
    }
};
