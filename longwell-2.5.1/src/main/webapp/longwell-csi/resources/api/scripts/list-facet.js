/*======================================================================
 *  ListFacet
 *======================================================================
 */
Longwell.UI.ListFacet = function(propertyURI, forward, valueClass, browsePanel, div) {
    this._propertyURI = propertyURI;
    this._forward = forward;
    this._valueClass = valueClass;
    this._browsePanel = browsePanel;
    this._div = div;
    this._opened = false;
    
    this._sortByFrequency = true;
    this._sortAscending = false;
    
    this._items = [];
    this._hasSelection = 
        browsePanel.getUI().getLongwell().getQE().getCurrentFacets().getFacet(
            Longwell.QueryFacet.LIST, propertyURI, forward) != null;
};

Longwell.UI.ListFacet.prototype.getPropertyURI = function(){ return this._propertyURI; };
Longwell.UI.ListFacet.prototype.isForward = function(){ return this._forward; };

Longwell.UI.ListFacet.prototype.isOpened = function(){ return this._opened; };

Longwell.UI.ListFacet.prototype.hasSelection = function(){ return this._hasSelection; };

Longwell.UI.ListFacet.prototype.dispose = function() {
};

Longwell.UI.ListFacet.prototype.open = function(){ 
    if (!this._opened) {
        this.toggle();
    }
};

Longwell.UI.ListFacet.prototype.toggle = function() {
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

Longwell.UI.ListFacet.prototype.getXMLForUpdate = function(indent) {
    var s = indent + "<facet" +
        " propertyURI='" + this._propertyURI + "'" +
        " forward='" + this._forward + "'" +
        " />\n";
    
    return s;
};

Longwell.UI.ListFacet.prototype.update = function(data){
    var selectItems = [];
    var selectedItemIndex = [];
    
    var currentFacets = this._browsePanel.getUI().getLongwell().getQE().getCurrentFacets();
    var facet = currentFacets.getFacet(Longwell.QueryFacet.LIST, this._propertyURI, this._forward);
    
    var selectionCount = 0;
    if (facet != null) {
        for (var i = 0; i < facet.selections.length; i++) {
            var item = facet.selections[i];
            selectedItemIndex[item.value] = true;
            selectionCount++;
        }
    }
    this._hasSelection = selectionCount > 0;
    this._setUI();
    this._div.firstChild.firstChild.firstChild.nodeValue = selectionCount;
    
    var oldItems = this._items;
    var newItems = data.items;
    newItems.sort(this._getSortFunction());
    
    var listFacet = this;
    var makeNewItem = function(item) {
        var div = document.createElement("div");
        div.className = item.selected ? 
            "longwell-browsePanel-listFacet-item longwell-browsePanel-listFacet-item-checked" : 
            "longwell-browsePanel-listFacet-item";
        div.setAttribute("value", item.value);
        div.setAttribute("status", item.selected ? "checked" : "unchecked");
        
        var divCount = document.createElement("div");
        divCount.className = "count";
        divCount.appendChild(document.createTextNode(item.count));
        div.appendChild(divCount);
        
        var select = document.createElement("div");
        select.className = "select";
        div.appendChild(select);
        
        div.appendChild(document.createTextNode(item.label.length == 0 ? "(empty string)" : item.label));
        
        listFacet._browsePanel.getUI().registerEventWithObject(
            div, "click", listFacet, listFacet._onListItemClick, 0);
        
        return div;
    }
    
    var bodyDiv = this._div.childNodes[1];
    bodyDiv.style.visibility = "hidden";
    bodyDiv.innerHTML = "";
    
    for (var i = 0; i < newItems.length; i++) {
        var newItem = newItems[i];
        
        newItem.selected = (newItem.value in selectedItemIndex);
        delete selectedItemIndex[newItem.value];
        
        bodyDiv.appendChild(makeNewItem(newItem));
    }
    for (var i = 0; i < oldItems.length; i++) {
        var oldItem = oldItems[i];
        if (oldItem.value in selectedItemIndex) {
            oldItem.selected = true;
            oldItem.count = 0;
            newItems.push(oldItem);
            
            bodyDiv.appendChild(makeNewItem(oldItem));
        }
    }
    this._items = newItems;

    bodyDiv.style.visibility = "visible";
};

Longwell.UI.ListFacet.prototype._getSortFunction = function(){ 
    if (this._sortByFrequency) {
        var f = function(item1, item2) {
            var c = item1.count - item2.count;
            if (c == 0) {
                c = item1.value.localeCompare(item2.value);
            }
            return c;
        };
    } else {
        var f = function(item1, item2) {
            var c = item1.label.localeCompare(item2.label);
            if (c == 0) {
                c = item1.value.localeCompare(item2.value);
            }
            return c;
        };
    }
    
    return this._sortAscending ? f : 
        function(item1, item2) { return f(item2, item1); };
};

Longwell.UI.ListFacet.prototype._onListItemClick = function(itemDiv, evt, target) {
    var params = {
        type:           Longwell.QueryFacet.LIST,
        propertyURI:    this._propertyURI,
        forward:        this._forward,
        valueClass:     this._valueClass,
        selection:      { value: itemDiv.getAttribute("value") }
    };
    
    var browsePanel = this._browsePanel;
    var fCheck = function() {
        browsePanel.queueChangeFacetSelectionAction({
            perform: function(transaction) {
                transaction.addFacetSelection(params);
            }
        });
        itemDiv.setAttribute("status", "checked");
        itemDiv.className = "longwell-browsePanel-listFacet-item";
    };
    var fUncheck = function() {
        browsePanel.queueChangeFacetSelectionAction({
            perform: function(transaction) {
                transaction.removeFacetSelection(params);
            }
        });
        itemDiv.setAttribute("status", "unchecked");
        itemDiv.className = "longwell-browsePanel-listFacet-item longwell-browsePanel-listFacet-item-checked";
    };
    
    var history = this._browsePanel.getUI().getLongwell().getHistory();
    var status = itemDiv.getAttribute("status");
    if (status == "unchecked") {
        history.addAction({ perform: fCheck, undo: fUncheck });
    } else {
        history.addAction({ perform: fUncheck, undo: fCheck });
    }
};
