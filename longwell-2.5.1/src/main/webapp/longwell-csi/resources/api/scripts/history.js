Longwell.History = function(longwell) {
    this._longwell = longwell;
    
    this._actions = [];
    this._baseIndex = 0;
    this._currentIndex = 0;
};

Longwell.History.MAX_HISTORY_LENGTH = 10;

Longwell.History.prototype.initialize = function() {
    var history = this;
    
    var iframe = document.createElement("iframe");
    iframe.style.position = "absolute";
    iframe.style.width = "10px";
    iframe.style.height = "10px";
    iframe.style.top = "0px";
    iframe.style.left = "0px";
    iframe.style.visibility = "hidden";
    iframe.src = Longwell.urlPrefix + "content/history.html?0";
    
    document.body.appendChild(iframe);
    
    Longwell.DOM.registerEvent(iframe, "load", function() {
        history._handleIFrameOnLoad();
    });
    this._iframe = iframe;
};

Longwell.History.prototype._handleIFrameOnLoad = function() {
    /*
     *  This function is invoked when the user herself
     *  navigates backward or forward. We need to adjust
     *  the application's state accordingly.
     */
     
    var q = this._iframe.contentWindow.location.search;
    var c = (q.length == 0) ? 0 : Math.max(0, parseInt(q.substr(1)));
    
    if (c < this._currentIndex) { // need to undo
        this._longwell.getUI().popAllLevels();
        
        while (this._currentIndex > c && 
               this._currentIndex > this._baseIndex) {
               
            this._currentIndex--;
            try {
                this._actions[this._currentIndex - this._baseIndex].undo();
            } catch (e) {
            }
        }
    } else if (c > this._currentIndex) { // need to redo
        this._longwell.getUI().popAllLevels();
        
        while (this._currentIndex < c && 
               this._currentIndex - this._baseIndex < this._actions.length) {
               
            try {
                this._actions[this._currentIndex - this._baseIndex].perform();
            } catch (e) {
            }
            this._currentIndex++;
        }
        
    } else {
        return;
    }
    
    var diff = c - this._currentIndex;
    this._currentIndex += diff;
    this._baseIndex += diff;
        
    this._iframe.contentWindow.location.search = "?" + c;
};

Longwell.History.prototype.addAction = function(action) {
    try {
        action.perform();
        
        this._actions = this._actions.slice(0, this._currentIndex - this._baseIndex);
        this._actions.push(action);
        this._currentIndex++;
        
        var diff = this._actions.length - Longwell.History.MAX_HISTORY_LENGTH;
        if (diff > 0) {
            this._actions = this._actions.slice(diff);
            this._baseIndex += diff;
        }
        
        this._iframe.contentWindow.location.search = "?" + this._currentIndex;
    } catch (e) {
        Longwell.Debug.exception(e);
    }
};
