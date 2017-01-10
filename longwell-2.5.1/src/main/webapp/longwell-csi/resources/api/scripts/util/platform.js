/*==================================================
 *  Platform Utility Functions and Constants
 *==================================================
 */

Longwell.Platform.isIE = false;
Longwell.Platform.isIE7 = false;
Longwell.Platform.isWin = false;
Longwell.Platform.isWin32 = false;

(function() {
    Longwell.Platform.isIE= (navigator.appName.indexOf("Microsoft") != -1);
    
	var ua = navigator.userAgent.toLowerCase(); 
	Longwell.Platform.isWin = (ua.indexOf('win') != -1);
	Longwell.Platform.isWin32 = Longwell.Platform.isWin && (   
        ua.indexOf('95') != -1 || 
        ua.indexOf('98') != -1 || 
        ua.indexOf('nt') != -1 || 
        ua.indexOf('win32') != -1 || 
        ua.indexOf('32bit') != -1
    );
    
    if (Longwell.Platform.isIE) {
        var offset = ua.indexOf("msie ");
        if (offset >= 0) {
            var s = ua.substring(offset + 5, ua.indexOf(";", offset));
            if (s.indexOf("7") == 0) {
                Longwell.Platform.isIE7 = true;
            }
        }
    }
    
    if (!("localeCompare" in String.prototype)) {
        String.prototype.localeCompare = function(s) {
            return this == s ? 0 : (this < s ? -1 : 1);
        }
    }
})();

Longwell.Platform.getDefaultLocale = function() {
    return Longwell.Platform.clientLocale;
};