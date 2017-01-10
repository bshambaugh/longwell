/*==================================================
 *  Longwell.XmlHttp
 *==================================================
 */

Longwell.XmlHttp = new Object();

/**
 *  Callback for XMLHttp onRequestStateChange.
 */
Longwell.XmlHttp._onReadyStateChange = function(xmlhttp, fError, fDone) {
    switch (xmlhttp.readyState) {
    // 1: Request not yet made
    // 2: Contact established with server but nothing downloaded yet
    // 3: Called multiple while downloading in progress
    
    // Download complete
    case 4:
        try {
            if (xmlhttp.status == 200) {
                if (fDone) {
                    fDone(xmlhttp);
                }
            } else {
                if (fError) {
                    fError(
                        xmlhttp.statusText,
                        xmlhttp.status,
                        xmlhttp
                    );
                }
            }
        } catch (e) {
            Longwell.Debug.exception(e);
        }
        break;
    }
};

/**
 *  Creates an XMLHttpRequest object. On the first run, this
 *  function creates a platform-specific function for
 *  instantiating an XMLHttpRequest object and then replaces
 *  itself with that function.
 */
Longwell.XmlHttp._createRequest = function() {
    if (Longwell.Platform.isIE) {
        var programIDs = [
        "Msxml2.XMLHTTP",
        "Microsoft.XMLHTTP",
        "Msxml2.XMLHTTP.4.0"
        ];
        for (var i = 0; i < programIDs.length; i++) {
            try {
                var programID = programIDs[i];
                var f = function() {
                    return new ActiveXObject(programID);
                };
                var o = f();
                
                // We are replacing the Longwell._createXmlHttpRequest
                // function with this inner function as we've
                // found out that it works. This is so that we
                // don't have to do all the testing over again
                // on subsequent calls.
                Longwell.XmlHttp._createRequest = f;
                
                return o;
            } catch (e) {
                // silent
            }
        }
        throw new Error("Failed to create an XMLHttpRequest object");
    } else {
        try {
            var f = function() {
                return new XMLHttpRequest();
            };
            var o = f();
            
            // We are replacing the Longwell._createXmlHttpRequest
            // function with this inner function as we've
            // found out that it works. This is so that we
            // don't have to do all the testing over again
            // on subsequent calls.
            Longwell.XmlHttp._createRequest = f;
            
            return o;
        } catch (e) {
            throw new Error("Failed to create an XMLHttpRequest object");
        }
    }
};

/**
 *  Performs an asynchronous HTTP GET.
 *  fError is of the form function(statusText, statusCode, xmlhttp).
 *  fDone is of the form function(xmlhttp).
 */
Longwell.XmlHttp.get = function(url, fError, fDone) {
    var xmlhttp = Longwell.XmlHttp._createRequest();
    
    xmlhttp.open("GET", url, true);
    //xmlhttp.overrideMimeType("text/xml");
    xmlhttp.onreadystatechange = function() {
        Longwell.XmlHttp._onReadyStateChange(xmlhttp, fError, fDone);
    };
    xmlhttp.send(null);
};

/**
 *  Performs an asynchronous HTTP POST.
 *  fError is of the form function(statusText, statusCode, xmlhttp).
 *  fDone is of the form function(xmlhttp).
 */
Longwell.XmlHttp.post = function(url, body, fError, fDone) {
    var xmlhttp = Longwell.XmlHttp._createRequest();
    
    xmlhttp.open("POST", url, true);
    //xmlhttp.overrideMimeType("text/xml");
    xmlhttp.onreadystatechange = function() {
        Longwell.XmlHttp._onReadyStateChange(xmlhttp, fError, fDone);
    };
    xmlhttp.send(body);
};

/*==================================================
 *  Longwell.XmlHttpQueue
 *==================================================
 */

Longwell.XmlHttpQueue = function() {
    this._jobs = [];
    this._animation = null;
    this._running = false;
};

Longwell.XmlHttpQueue.prototype.clear = function() {
    this._jobs = [];
};

Longwell.XmlHttpQueue.prototype.setAnimation = function(animation) {
    this._animation = animation;
};

Longwell.XmlHttpQueue.prototype.queue = function(f) {
    this._jobs.push(f);
    if (!this._running) {
        this._running = true;
        
        var queue = this;
        window.setTimeout(
            function() { queue._next(); },
            10
        );
    }
};

Longwell.XmlHttpQueue.prototype._next = function() {
    this._runAnimation();
    
    while (this._jobs.length > 0) {
        try {
            var queue = this;
            var cont = function() { queue._next(); };
            var job = this._jobs.shift();
            
            job(cont);
            return;
        } catch (e) {
            Longwell.Debug.exception(e);
        }
    }
    
    this._running = false;
    this._stopAnimation();
};

Longwell.XmlHttpQueue.prototype._runAnimation = function() {
    if (this._animation != null) {
        var animation = this._animation;
        window.setTimeout(function() { animation.run(); }, 0);
    }
}

Longwell.XmlHttpQueue.prototype._stopAnimation = function() {
    if (this._animation != null) {
        var animation = this._animation;
        window.setTimeout(function() { animation.stop(); }, 0);
    }
}