var HTTPUtilities = {
    doGet : function(url, onError, onDone) {
        var xmlhttp = new XMLHttpRequest();
        
        xmlhttp.open('GET', url, true);
        //xmlhttp.overrideMimeType("text/xml");
        xmlhttp.onreadystatechange = function() {
            HTTPUtilities_onReadyStateChange(xmlhttp, onError, onDone);
        };
        xmlhttp.send(null);
    },
    doPost : function(url, body, onError, onDone) {
        var xmlhttp = new XMLHttpRequest();
        
        xmlhttp.open('POST', url, true);
        //xmlhttp.overrideMimeType("text/xml");
        xmlhttp.onreadystatechange = function() {
            HTTPUtilities_onReadyStateChange(xmlhttp, onError, onDone);
        };
        xmlhttp.send(body);
    }
};

var HTTPUtilities_onReadyStateChange = function(xmlhttp, onError, onDone) {
    switch (xmlhttp.readyState) {

        // Request not yet made
        case 1:
        break;

        // Contact established with server but nothing downloaded yet
        case 2:
            try {
                // Check for HTTP status 200
                if (xmlhttp.status != 200) {
                    if (onError) {
                        onError(
                            xmlhttp.status,
                            xmlhttp.statusText
                        );
                        xmlhttp.abort();
                    }
                }
            } catch (e) {
                Debug.onCaughtException(e);
            }
        break;

        // Called multiple while downloading in progress
        case 3:
        break;

        // Download complete
        case 4:
            try {
                if (onDone && xmlhttp.status == 200) {
                    onDone(xmlhttp.responseText);
                }
            } catch (e) {
                Debug.onCaughtException(e);
            }
        break;
    }
};
