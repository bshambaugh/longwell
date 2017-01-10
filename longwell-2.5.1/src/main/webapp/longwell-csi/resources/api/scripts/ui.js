Longwell.UI = function(longwell, controlDiv, browseDiv, viewDiv, params) {
    this._longwell = longwell;
    
    Longwell.DOM.protectUI(controlDiv);
    Longwell.DOM.protectUI(browseDiv);
    Longwell.DOM.protectUI(viewDiv);
    
    this._controlPanel = new Longwell.UI.ControlPanel(this, controlDiv, (params != null && "control" in params) ? params.control : {});
    this._browsePanel = new Longwell.UI.BrowsePanel(this, browseDiv, (params != null && "browse" in params) ? params.browse : {});
    this._viewPanel = new Longwell.UI.ViewPanel(this, viewDiv, (params != null && "view" in params) ? params.view : {});
    this._stack = [];
};

Longwell.UI.FRONT_PAGE = 0;
Longwell.UI.NORMAL_PAGE = 1;

Longwell.UI.prototype.getLongwell = function() { return this._longwell; };
Longwell.UI.prototype.getControlPanel = function() { return this._controlPanel; };
Longwell.UI.prototype.getBrowsePanel = function() { return this._browsePanel; };
Longwell.UI.prototype.getViewPanel = function() { return this._viewPanel; };

Longwell.UI.prototype.initialize = function() {
    Longwell.DOM.registerEventWithObject(document.body, "click", this, this._onBodyClick);
    Longwell.DOM.registerEventWithObject(document.body, "mousemove", this, this._onBodyMouseMove);
    Longwell.DOM.registerEventWithObject(document.body, "mouseup", this, this._onBodyMouseUp);
    
    this._draggedElement = null;
    this._lastCoords = null;
    
    this._longwell.getQE().registerListener(this);
    this.reconstruct();
};

Longwell.UI.prototype.toJSON = function() {
    return {
        view:      this._viewPanel.toJSON(),
        browse:    this._browsePanel.toJSON(),
        control:   this._controlPanel.toJSON()
    };
};

Longwell.UI.prototype.onCurrentFacetsChange = function() {
	this.reconstruct();
}

Longwell.UI.prototype.reconstruct = function() {
    var mode;
    if (this._longwell.getQE().hasNoSelection()) {
        mode = Longwell.UI.FRONT_PAGE;
        if (this._longwell.toFrontPage) this._longwell.toFrontPage();
    } else {
        mode = Longwell.UI.NORMAL_PAGE;
        if (this._longwell.toBrowsingPage) this._longwell.toBrowsingPage();
    }
    
    if (mode != this._mode) {
        this._mode = mode;
        this._reconstruct(this._mode);
    }
};

Longwell.UI.prototype._reconstruct = function(mode) {
    this._controlPanel.initialize(mode);
    this._browsePanel.initialize(mode);
    this._viewPanel.initialize(mode);
};

Longwell.UI.prototype.createElementForURI = function(uri, label, level) {
    var a = document.createElement("a");
    a.className = "longwell-uri";
    a.href = uri;
    a.appendChild(document.createTextNode(label));
    
    this.registerEventWithObject(a, "click", this, this._onItemLinkClick, (level) ? level : 0);
    
    return a;
};

Longwell.UI.prototype.createNodeForURI = function(uri, nodeMaker, level) {
    var a = document.createElement("a");
    a.className = "longwell-uri";
    a.href = uri;
    var n = nodeMaker();
    a.appendChild(n);
    
    this.registerEventWithObject(a, "click", this, this._onItemLinkClick, (level) ? level : 0);
    
    return a;
};

Longwell.UI.prototype.createElementForLiteral = function(propertyURI, value) {
    var span = document.createElement("span");
    span.className = "longwell-literal";
    
    var label = value;
    var innerNode = null;
    
    var property = Longwell.ProfileData.propertyIndex[propertyURI];
    try {
        if (property.isDateTime > 0.5) {
            var date = Longwell.DateTime.parseIso8601DateTime(value);
            label = date != null ? date.toLocaleString() : label;
        } else if (property.isURI > 0.5) {
            var label = value;
            if (label.length > 30) {
                label = label.substr(0, 15) + " ... " + label.substr(label.length - 15);
            }
            
            innerNode = document.createElement("a");
            innerNode.href = value;
            innerNode.target = "_blank";
            innerNode.appendChild(document.createTextNode(label));
        }
    } catch (e) {
    }
    
    if (innerNode == null) {
        innerNode = document.createTextNode(label);
    }
    span.appendChild(innerNode);
    
    return span;
};

Longwell.UI.prototype.createActionLink = function(text, obj, handler, level) {
    var a = document.createElement("a");
    a.className = "action";
    a.href = "javascript:";
    a.innerHTML = text;
    
    if (obj != null) {
        this.registerEventWithObject(a, "click", obj, handler, level);
    } else {
        this.registerEvent(a, "click", handler, level);
    }
    
    return a;
};

Longwell.UI.prototype.registerEventWithObject = function(elmt, eventName, obj, handler, level) {
    this.registerEvent(elmt, eventName, function(elmt2, evt, target) {
        return handler.call(obj, elmt2, evt, target);
    }, level);
};

Longwell.UI.prototype.registerEvent = function(elmt, eventName, handler, level) {
    var ui = this;
    var handler2 = function(elmt, evt, target) {
        ui._popToLevel(level, false);
        if (ui._stack.length == level) {
            handler(elmt, evt, target);
        }
    }
    
    Longwell.DOM.registerEvent(elmt, eventName, handler2);
};

Longwell.UI.prototype.pushLevel = function(f, blocking) {
    this._stack.push({ f: f, blocking: blocking });
};

Longwell.UI.prototype.popLevel = function() {
    this._stack.pop();
};

Longwell.UI.prototype.popAllLevels = function() {
    this._popToLevel(0, true);
};

Longwell.UI.prototype.registerForDragging = function(elmt, callback) {
    var ui = this;
    this.registerEvent(elmt, "mousedown", function(elmt, evt, target) {
        ui._handleMouseDown(elmt, evt, callback);
    }, 0);
};

Longwell.UI.prototype.getItemViewURL = function(uri, fresnel, group, format) {
    var params = [ "command=view", "objectURI=" + encodeURIComponent(uri) ];
    if (fresnel) {
        params.push("engine=fresnel");
    }
    if (group != null && group != "") {
        params.push("group=" + encodeURIComponent(group));
    }
    if (format && format != "") {
        params.push("format=" + encodeURIComponent(format));
    }

    return Longwell.Configuration.contextPath + Longwell.Configuration.profileID + "?" + params.join("&");
};

Longwell.UI.prototype._popToLevel = function(level, force) {
    while (level < this._stack.length) {
        try {
            var o = this._stack.pop();
            if (o.blocking && !force) {
                this._stack.push(o);
                return;
            }
            o.f();
        } catch (e) {
        }
    }
};

Longwell.UI.prototype._onBodyClick = function(elmt, evt, target) {
    if (!("eventPhase" in evt) || evt.eventPhase == evt.BUBBLING_PHASE) {
        this._popToLevel(0, false);
    }
};

Longwell.UI.prototype._handleMouseDown = function(elmt, evt, callback) {
    try {
        this._draggedElement = elmt;
        this._draggedElementCallback = callback;
        this._lastCoords = { x: evt.clientX, y: evt.clientY };
        
        this._draggedElementCallback.onDragStart();
    } catch (e) {
        Longwell.Debug.exception(e);
        this._cancelDragging();
    }
};

Longwell.UI.prototype._onBodyMouseMove = function(elmt, evt, target) {
    if (this._draggedElement != null) {
        try {
            var diffX = evt.clientX - this._lastCoords.x;
            var diffY = evt.clientY - this._lastCoords.y;
            
            this._lastCoords = { x: evt.clientX, y: evt.clientY };
            
            this._draggedElementCallback.onDragBy(diffX, diffY);
        } catch (e) {
            Longwell.Debug.exception(e);
            this._cancelDragging();
        }
    }
};

Longwell.UI.prototype._onBodyMouseUp = function(elmt, evt, target) {
    if (this._draggedElement != null) {
        try {
            this._draggedElementCallback.onDragEnd();
        } finally {
            this._cancelDragging();
        }
    }
};

Longwell.UI.prototype._cancelDragging = function() {
    this._draggedElement = null;
    this._draggedElementCallback = null;
    this._lastCoords = null;
};

Longwell.UI.prototype._onItemLinkClick = function(a, evt, target) {
    var uri = a.href;
    var ui = this;
    
    var fError = function(statusText, status, xmlhttp) {
        Longwell.Debug.log(statusText);
    };
    var fDone = function(xmlhttp) {
        ui._showItemView(uri, a, xmlhttp.responseText);
    };
    
    Longwell.XmlHttp.post(this.getItemViewURL(uri, true), "", fError, fDone);
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.prototype._showItemView = function(uri, a, text) {
    var coords = Longwell.DOM.getPageCoordinates(a);
    var bubble = Longwell.Graphics.createBubbleForPoint(document, coords.left, coords.top, 400, 300);
    bubble.content.innerHTML = text;

    var titleElmt = document.getElementById("lw_item_" + uri + "_title");
    var resultElmt = document.getElementById("fresnel_title_" + uri);
    if (titleElmt && resultElmt && resultElmt.childNodes[0]) {
        var title = document.createTextNode(resultElmt.childNodes[0].nodeValue);
        titleElmt.appendChild(title);
    }
    var styleElmt = document.getElementById("fresnel_styles_" + uri);
    if (styleElmt) {
        var styles = styleElmt.getElementsByTagName("span");
        for (var i = 0; i < styles.length; i++) {
            registerStylesheet(Longwell.Configuration.resourcePath + styles[i].childNodes[0].nodeValue);
        }
    }
                            
    this.pushLevel(function() {bubble.close();}, false);
};
