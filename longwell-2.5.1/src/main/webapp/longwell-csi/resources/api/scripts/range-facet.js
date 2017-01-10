/*======================================================================
 *  RangeFacet
 *======================================================================
 */
Longwell.UI.RangeFacet = function(propertyURI, valueClass, browsePanel, div) {
    this._propertyURI = propertyURI;
    this._valueClass = valueClass;
    this._browsePanel = browsePanel;
    this._div = div;
    this._opened = false;
    
    this._sortAscending = false;
    
    this._ranges = [];
    this._hasSelection = 
        browsePanel.getUI().getLongwell().getQE().getCurrentFacets().getFacet(
            Longwell.QueryFacet.RANGE, propertyURI, true) != null;
};

Longwell.UI.RangeFacet.prototype.getPropertyURI = function(){ return this._propertyURI; };
Longwell.UI.RangeFacet.prototype.isForward = function(){ return true; };

Longwell.UI.RangeFacet.prototype.isOpened = function(){ return this._opened; };

Longwell.UI.RangeFacet.prototype.hasSelection = function(){ return this._hasSelection; };

Longwell.UI.RangeFacet.prototype.dispose = function() {
};

Longwell.UI.RangeFacet.prototype.open = function(){ 
    if (!this._opened) {
        this.toggle();
    }
};

Longwell.UI.RangeFacet.prototype.toggle = function() {
    this._opened = !this._opened;
    if (this._opened) {
        this._setUI();
        if (!this.hasSelection()) {
            this._browsePanel.requestFacetUpdate(this);
        }
    } else {
        this._setUI();
        if (!this.hasSelection()) {
            this._div.childNodes[1].innerHTML = "";
            this._items = [];
        }
    }
};

Longwell.UI.RangeFacet.prototype.getXMLForUpdate = function(indent) {
    var s = indent + "<facet" +
        " propertyURI='" + this._propertyURI + "'" +
        " forward='true'" +
        ">\n";
    
    var indent2 = indent + " ";
    
    for (var i = 0; i < this._ranges.length; i++) {
        var range = this._ranges[i];
        if (range.opened) {
            s += indent2 + "<range min='" + range.min + "' max='" + range.max + "' />\n";
        }
    }
    
    s += indent + "</facet>\n";
    
    return s;
};

/*
 *  TODO: Do real merging of new ranges and old ranges. This is quite complicated.
 *  Right now, we don't do that. As a result, selected ranges that have 0 count
 *  disappear.
 */
Longwell.UI.RangeFacet.prototype.update = function(data){
    var selectedRangeIndex = [];
    var key = function(range) { return range.min + "," + range.max; };
    
    this._hasSelection = false;
    
    var currentFacets = this._browsePanel.getUI().getLongwell().getQE().getCurrentFacets();
    var facet = currentFacets.getFacet(Longwell.QueryFacet.RANGE, this._propertyURI, true);
    var selectionCount = 0;
    if (facet != null) {
        for (var i = 0; i < facet.selections.length; i++) {
            var range = facet.selections[i];
            selectedRangeIndex[key(range)] = true;
            selectionCount++;
        }
    }
    this._hasSelection = selectionCount > 0;
    this._setUI();
    this._div.firstChild.firstChild.firstChild.nodeValue = selectionCount;

    
    var bodyDiv = this._div.childNodes[1];
    var scrollTop = bodyDiv.scrollTop;
    
    var rangeFacet = this;
    
    bodyDiv.style.visibility = "hidden";
    bodyDiv.innerHTML = "";
    
    var makeNewRange = function(range, index) {
        var expandable = range.count > 1;
        var div = document.createElement("div");
        var classes = [ "longwell-browsePanel-rangeFacet-range" ];
        if (expandable) {
            classes.push(range.opened ?
                "longwell-browsePanel-rangeFacet-range-opened" :
                "longwell-browsePanel-rangeFacet-range-closed"
            );
        }
        if (range.selected) {
            classes.push("longwell-browsePanel-rangeFacet-range-checked");
        }
        div.className = classes.join(" ");
        div.setAttribute("index", index);
        div.setAttribute("status", range.selected ? "checked" : "unchecked");
        
        var divCount = document.createElement("div");
        divCount.className = "count";
        divCount.appendChild(document.createTextNode(range.count));
        div.appendChild(divCount);
        
        var select = document.createElement("div");
        select.className = "select";
        div.appendChild(select);
        
        var space = document.createElement("div");
        space.className = "space";
        space.style.width = (range.level * 16) + "px";
        div.appendChild(space);
        
        var treeControl = document.createElement("div");
        treeControl.className = "tree-control";
        div.appendChild(treeControl);
        if (expandable) {
            rangeFacet._browsePanel.getUI().registerEventWithObject(
                treeControl, "click", rangeFacet, rangeFacet._onTreeControlClick, 0);
        }

        div.appendChild(document.createTextNode(range.label));
        
        rangeFacet._browsePanel.getUI().registerEventWithObject(
            div, "click", rangeFacet, rangeFacet._onRangeClick, 0);
        
        return div;
    }
    
    var newRanges = data.ranges;
    for (var i = 0; i < newRanges.length; i++) {
        var range = newRanges[i];
        range.opened = (i < newRanges.length - 1 && newRanges[i+1].level > range.level);
        range.selected = (key(range) in selectedRangeIndex);
        
        bodyDiv.appendChild(makeNewRange(range, i));
    }
    this._ranges = newRanges;
    
    bodyDiv.style.visibility = "visible";
    bodyDiv.scrollTop = scrollTop;
};

Longwell.UI.RangeFacet.prototype._onRangeClick = function(rangeDiv, evt, target) {
    var index = parseInt(rangeDiv.getAttribute("index"));
    var range = this._ranges[index];
    
    var params = {
        type:           Longwell.QueryFacet.RANGE,
        propertyURI:    this._propertyURI,
        forward:        true,
        valueClass:     this._valueClass,
        selection:      { min: range.min, max: range.max }
    };
    
    var browsePanel = this._browsePanel;
    var fCheck = function() {
        browsePanel.queueChangeFacetSelectionAction({
            perform: function(transaction) {
                transaction.addFacetSelection(params);
            }
        });
    };
    var fUncheck = function() {
        browsePanel.queueChangeFacetSelectionAction({
            perform: function(transaction) {
                transaction.removeFacetSelection(params);
            }
        });
    };
    
    var history = this._browsePanel.getUI().getLongwell().getHistory();
    var status = rangeDiv.getAttribute("status");
    if (status == "unchecked") {
        history.addAction({ perform: fCheck, undo: fUncheck });
    } else {
        history.addAction({ perform: fUncheck, undo: fCheck });
    }
};

Longwell.UI.RangeFacet.prototype._onTreeControlClick = function(treeControl, evt, target) {
    var rangeDiv = treeControl.parentNode;
    var index = parseInt(rangeDiv.getAttribute("index"));
    var range = this._ranges[index];
    
    range.opened = !range.opened;
    
    this._browsePanel.requestFacetUpdate(this);

    Longwell.DOM.cancelEvent(evt);
    
    return false;
};
