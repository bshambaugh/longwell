Longwell.UI.ControlPanel = function(ui, div) {
    this._ui = ui;
    this._div = div;
};

Longwell.UI.ControlPanel.prototype.initialize = function(mode) {
    if (mode == Longwell.UI.FRONT_PAGE) {
        this._initializeFrontPage();
    } else if (mode == Longwell.UI.NORMAL_PAGE) {
        this._initializeNormalPage();
    }
};

Longwell.UI.ControlPanel.prototype.toJSON = function() {
    return null;
};

Longwell.UI.ControlPanel.prototype._initializeFrontPage = function() {
    this._div.style.display = "none";
    this._div.innerHTML = "";
};

Longwell.UI.ControlPanel.prototype._initializeNormalPage = function() {
    this._div.innerHTML = "";
    this._div.style.display = "block";
    
    var template = {
        elmt: this._div,
        children: [
            { elmt: this._ui.createActionLink("Add View", this, this._onAddViewClick, 0) },
            " | ",
            { elmt: this._ui.createActionLink("Start New Search", this, this._onClearAllFilterClick, 0) }
        ]
    };
    Longwell.DOM.createDOMFromTemplate(document, template);
};

Longwell.UI.ControlPanel.prototype._onAddViewClick = function(a, evt, target) {
    var viewPanel = this._ui.getViewPanel();
    viewPanel.addView();
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};

Longwell.UI.ControlPanel.prototype._onClearAllFilterClick = function(node, evt, target) {
    var o = {};
    var controlPanel = this;
    var fDo = function() {
        o.oldFacets = controlPanel._ui.getLongwell().getQE().clearFacets();
    };
    var fUndo = function() {
        controlPanel._ui.getLongwell().getQE().restoreFacets(o.oldFacets);
    };
    this._ui.getLongwell().getHistory().addAction({ perform: fDo, undo: fUndo });
    
    Longwell.DOM.cancelEvent(evt);
    return false;
};
