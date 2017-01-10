/*==================================================
 *  Debug Utility Functions
 *==================================================
 */

Longwell.Debug = new Object();

Longwell.Debug.log = function(msg) {
    var f;
    if ("console" in window && "log" in window.console) { // FireBug installed
        f = function(msg2) {
            console.log(msg2);
        }
    } else {
        f = function(msg2) {
            alert(msg2);
        }
    }
    Longwell.Debug.exception = f;
    f(msg);
};

Longwell.Debug.exception = function(e) {
    var f;
    if ("console" in window && "error" in window.console) { // FireBug installed
        f = function(e2) {
            console.error("%o", e2);
        }
    } else {
        f = function(e2) {
            alert("Caught exception: " + (Longwell.Platform.isIE ? e2.message : e2));
        }
    }
    Longwell.Debug.exception = f;
    f(e);
};

