/*======================================================================
 *  QueryFacet
 *======================================================================
 */
 
Longwell.QueryFacet = function(type, propertyURI, forward, valueClass, facetCollection) {
    this.type = type;
    this.propertyURI = propertyURI;
    this.forward = forward;
    this.valueClass = valueClass;
    this.facetCollection = facetCollection;
    
    this.selections = [];
};

Longwell.QueryFacet.NESTED = -1;
Longwell.QueryFacet.LIST = 0;
Longwell.QueryFacet.RANGE = 1;
Longwell.QueryFacet.TEXT = 2;

Longwell.QueryFacet.getValueClassFromProperty = function(property) {
    if (property.isDateTime > 0.5) {
        return "java.util.Date";
    } else if (property.isInteger > 0.5) {
        return "java.lang.Long";
    } else if (property.isNumeric > 0.5) {
        return "java.lang.Double";
    } else {
        return "org.openrdf.model.Value";
    }
}

Longwell.QueryFacet.getFacetTypeFromValueClass = function(valueClass) {
    if (valueClass == "java.lang.Long" || 
        valueClass == "java.lang.Double" ||
        valueClass == "java.util.Date") {
        return Longwell.QueryFacet.RANGE;
    } else if (valueClass == "org.openrdf.model.Value") {
        return Longwell.QueryFacet.LIST; 
    } else {
        throw new Error("Unsupported value class");
    }
};

Longwell.QueryFacet.prototype.addSelection = function(o) {
    switch (this.type) {
    case Longwell.QueryFacet.LIST:
        for (var i = 0; i < this.selections.length; i++) {
            var item = this.selections[i];
            if (item.value == o.value) {
                return;
            }
        }
        this.selections.push({ value: o.value });
        break;
    case Longwell.QueryFacet.RANGE:
        if (o.min == null) {
            for (var i = 0; i < this.selections.length; i++) {
                var range = this.selections[i];
                if (range.min == null) {
                    return;
                }
            }
            this.selections.push({ min: null });
        } else {
            for (var i = 0; i < this.selections.length; i++) {
                var range = this.selections[i];
                if (range.min != null) {
                    if (range.min == o.min && range.max == o.max) {
                        return;
                    }
                }
            }
            this.selections.push({ min: o.min, max: o.max });
        }
        break;
    case Longwell.QueryFacet.TEXT:
        for (var i = 0; i < this.selections.length; i++) {
            var item = this.selections[i];
            if (item.value == o.value) {
                return;
            }
        }
        this.selections.push({ value: o.value });
        break;
    default:
        throw new Error("Cannot add selection to facet type " + this.type);
    }
};

Longwell.QueryFacet.prototype.removeSelection = function(o) {
    switch (this.type) {
    case Longwell.QueryFacet.LIST:
        for (var i = 0; i < this.selections.length; i++) {
            var item = this.selections[i];
            if (item.value == o.value) {
                this.selections.splice(i, 1);
                break;
            }
        }
        break;
    case Longwell.QueryFacet.RANGE:
        if (o.min == null) {
            for (var i = 0; i < this.selections.length; i++) {
                var range = this.selections[i];
                if (range.min == null) {
                    this.selections.splice(i, 1);
                    break;
                }
            }
        } else {
            for (var i = 0; i < this.selections.length; i++) {
                var range = this.selections[i];
                if (range.min != null) {
                    if (range.min == o.min && range.max == o.max) {
                    this.selections.splice(i, 1);
                    break;
                    }
                }
            }
        }
        break;
    case Longwell.QueryFacet.TEXT:
        for (var i = 0; i < this.selections.length; i++) {
            var item = this.selections[i];
            if (item.value == o.value) {
                this.selections.splice(i, 1);
                break;
            }
        }
        break;
    default:
        throw new Error("Cannot add selection to facet type " + this.type);
    }
    
    if (this.selections.length == 0) {
        this.facetCollection.removeFacet(this);
    }
};

Longwell.QueryFacet.prototype.getOpeningXMLTag = function() {
    return "<facet" +
        " propertyURI='" + this.propertyURI + "'" + 
        " forward='"     + (this.forward ? 'true' : 'false') + "'" +
        " type='"        + this.type + "'" +
        " valueClass='"  + this.valueClass + "'>";
};

Longwell.QueryFacet.prototype.getClosingXMLTag = function() {
    return "</facet>";
};

Longwell.QueryFacet.prototype.getXML = function(indent) {
    var s = indent + this.getOpeningXMLTag() + "\n";
    var indent2 = indent + " ";
    
    switch (this.type) {
    case Longwell.QueryFacet.NESTED:
        for (var i = 0; i < this.selections.length; i++) {
            s += this.selections[i].getXML(indent2) + "\n";
        }
        break;
    case Longwell.QueryFacet.LIST:
        for (var i = 0; i < this.selections.length; i++) {
            s += indent2 + this.getItemXMLTag(this.selections[i]) + "\n";
        }
        break;
    case Longwell.QueryFacet.RANGE:
        for (var i = 0; i < this.selections.length; i++) {
            s += indent2 + this.getRangeXMLTag(this.selections[i]) + "\n";
        }
        break;
    case Longwell.QueryFacet.TEXT:
        for (var i = 0; i < this.selections.length; i++) {
            s += indent2 + this.getTextXMLTag(this.selections[i]) + "\n";
        }
        break;
    default:
        throw new Error("Unknown facet type " + this.type);
    }
    s += indent + this.getClosingXMLTag() + "\n";
    
    return s;
};

Longwell.QueryFacet.prototype.getRangeXMLTag = function(range) {
    return "<item min='" + range.min + "' max='" + range.max + "'/>";
};

Longwell.QueryFacet.prototype.getItemXMLTag = function(item) {
    return "<item value='" + this.escapeForXMLAttribute(item.value) + "' />";
};

Longwell.QueryFacet.prototype.getTextXMLTag = function(item) {
    return "<item value='" + this.escapeForXMLAttribute(item.value) + "' />";
};

Longwell.QueryFacet.prototype.escapeForXMLAttribute = function(str) {
    return str.replace(/&/g,"&amp;").replace(/"/g,"&quot;").replace(/'/g,"&apos;").replace(/>/g,"&gt;").replace(/</g,"&lt;");
};

Longwell.QueryFacet.prototype.toJSON = function() {
    var result = [
        this.type,
        this.propertyURI,
        this.forward ? 0 : 1,
        this.valueClass
    ];
    
    if (this.type == Longwell.QueryFacet.NESTED) {
        result.push(this.selections[0].toJSON)
    } else if (this.type == Longwell.QueryFacet.LIST) {
        var a = [];
        for (var i = 0; i < this.selections.length; i++) {
            a.push(this.selections[i].value);
        }
        result.push(a);
    } else if (this.type == Longwell.QueryFacet.RANGE) {
        var a = [];
        for (var i = 0; i < this.selections.length; i++) {
            var s = this.selections[i];
            if ("max" in s) {
                a.push([s.min,s.max]);
            } else {
                a.push([s.min]);
            }
        }
        result.push(a);
    } else if (this.type == Longwell.QueryFacet.TEXT) {
        var a = [];
        for (var i = 0; i < this.selections.length; i++) {
            a.push(this.selections[i].value);
        }
        result.push(a);
    } else {
        throw new Error("Unknown facet type " + facet.type);
    }
    return result;
};

Longwell.QueryFacet.parseFromUrlJSON = function(o, facetCollection) {
    var facet = new Longwell.QueryFacet(
        /* type */          o[0],
        /* propertyURI */   o[1],
        /* forward */       o[2] == 0,
        /* valueClass */    o[3],
                            facetCollection
    );
    
    if (facet.type == Longwell.QueryFacet.NESTED) {
        facet.selections = [ Longwell.QueryFacet.parseFromUrlJSON(o[4]) ];
    } else if (facet.type == Longwell.QueryFacet.LIST) {
        facet.selections = Longwell.QueryFacet.parseItemsFromUrlJSON(o[4], facet.valueClass);
    } else if (facet.type == Longwell.QueryFacet.RANGE) {
        facet.selections = Longwell.QueryFacet.parseRangesFromUrlJSON(o[4], facet.valueClass);
    } else if (facet.type == Longwell.QueryFacet.TEXT) {
        facet.selections = Longwell.QueryFacet.parseTextSearchesFromUrlJSON(o[4], facet.valueClass);
    } else {
        throw new Error("Unknown facet type " + facet.type);
    }
    
    return facet;
};

Longwell.QueryFacet.parseItemsFromUrlJSON = function(a, valueClass) {
    var results = [];
    for (var i = 0; i < a.length; i++) {
        var o = a[i];
        results.push({ value: o });
    }
    return results;
};

Longwell.QueryFacet.parseRangesFromUrlJSON = function(a, valueClass) {
    var results = [];
    for (var i = 0; i < a.length; i++) {
        var o = a[i];
        results.push({ 
            min: o[0],
            max: o.length > 1 ? o[1] : null
        });
    }
    return results;
};

Longwell.QueryFacet.parseTextSearchesFromUrlJSON = function(a, valueClass) {
    var results = [];
    for (var i = 0; i < a.length; i++) {
        var o = a[i];
        results.push({ value: o });
    }
    return results;
};

/*======================================================================
 *  QueryFacetCollection
 *======================================================================
 */
 
Longwell.QueryFacetCollection = function(facetsFromUrlJSON) {
    this.facets = [];
    if (facetsFromUrlJSON != null) {
        for (var i = 0; i < facetsFromUrlJSON.length; i++) {
            this.facets.push(Longwell.QueryFacet.parseFromUrlJSON(facetsFromUrlJSON[i], this));
        }
    }
};

Longwell.QueryFacetCollection.prototype.isEmpty = function() {
    return this.facets.length == 0;
};

Longwell.QueryFacetCollection.prototype.getXML = function(elmtName, indent) {
    var s = indent + "<" + elmtName + ">\n";
    var indent2 = indent + " ";
    for (var i = 0; i < this.facets.length; i++) {
        s += this.facets[i].getXML(indent2);
    }
    s += indent + "</" + elmtName + ">\n";
    return s;
};

Longwell.QueryFacetCollection.prototype.getFacet = function(type, propertyURI, forward) {
    for (var i = 0; i < this.facets.length; i++) {
        var facet = this.facets[i];
        if (type == facet.type && 
            propertyURI == facet.propertyURI && 
            forward == facet.forward
        ) {
            return facet;
        }
    }
    return null;
};

Longwell.QueryFacetCollection.prototype.getTextSearches = function() {
    var facet = this.getFacet(Longwell.QueryFacet.TEXT, "", true);
    var searches = [];
    if (facet != null) {
        for (var i = 0; i < facet.selections.length; i++) {
            searches.push(facet.selections[i].value);
        }
    }
    return searches;
};

Longwell.QueryFacetCollection.prototype.addFacet = function(facet) {
    var facet2 = this.getFacet(facet.type, facet.propertyURI, facet.forward);
    if (facet2 != null) {
        for (var i = 0; i < facet.selections.length; i++) {
            facet2.addSelection(facet.selections[i]);
        }
    } else {
        this.facets.push(facet);
    }
};

Longwell.QueryFacetCollection.prototype.addFacetSelection = function(params) {
    var facet = this.getFacet(params.type, params.propertyURI, params.forward);
    if (facet == null) {
        facet = new Longwell.QueryFacet(
            params.type, 
            params.propertyURI, 
            params.forward, 
            params.valueClass, 
            this
        );
            
        this.facets.push(facet);
    }
    facet.addSelection(params.selection);
};

Longwell.QueryFacetCollection.prototype.removeFacetSelection = function(params) {
    var facet = this.getFacet(params.type, params.propertyURI, params.forward);
    if (facet != null) {
        facet.removeSelection(params.selection);
    }
};

Longwell.QueryFacetCollection.prototype.addNestedFacet = function(queryFacetCollection, propertyURI, forward) {
    var facet = new Longwell.QueryFacet(
        Longwell.QueryFacet.NESTED, 
        propertyURI, 
        forward, 
        "org.openrdf.model.Value",
        this
    );
            
    this.facets.push(facet);
};

Longwell.QueryFacetCollection.prototype.removeFacet = function(facet) {
    for (var i = 0; i < this.facets.length; i++) {
        if (this.facets[i] == facet) {
            this.facets.splice(i, 1);
            break;
        }
    }
};

Longwell.QueryFacetCollection.prototype.toJSON = function() {
    var results = [];
    for (var i = 0; i < this.facets.length; i++) {
        results.push(this.facets[i].toJSON());
    }
    return results;
};
