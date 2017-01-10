/*==================================================
 *  Longwell API
 *
 *  This file will load all the Javascript files
 *  necessary to make Longwell work.
 *  It also detects the default locale.
 *
 *==================================================
 */
 
var Longwell = new Object();
Longwell.Platform = new Object();
    /*
        HACK: We need these 2 things here because we cannot simply append
        a <script> element containing code that accesses Longwell.Platform
        to initialize it because IE executes that <script> code first
        before it loads longwell.js and util/platform.js.
    */

(function() {
    var javascriptFiles = [
        "util/platform.js",
        "util/debug.js",
        "util/xmlhttp.js",
        "util/dom.js",
        "util/graphics.js",
        "util/date-time.js",
        "util/json.js",
        
        "longwell.js",
        "history.js",
        
        "query-facet.js",
        "query-engine.js",
        
        "ui.js",
        "control-panel.js",
        
        "browse-panel.js",
        "list-facet.js",
        "range-facet.js",
        
        "view-panel.js",
        "views/tabular-view.js",
        "views/list-view.js",
        "views/timeline-view.js",
        "views/map-view.js",
        "views/grid-view.js"
    ];
    var cssFiles = [
        "ui.css",
        "view-panel.css",
        "views/tabular-view.css",
        "views/list-view.css",
        "views/timeline-view.css",
        "views/map-view.css",
        "views/grid-view.css",
        "browse-panel.css"
    ];
    
    var localizedJavascriptFiles = [
    ];
    var localizedCssFiles = [
    ];
    
    // ISO-639 language codes, ISO-3166 country codes (2 characters)
    var supportedLocales = [
        "en"        // English
    ];
    
    try {
        var desiredLocales = [ "en" ];
        var defaultServerLocale = "en";
        
        (function() {
            var heads = document.documentElement.getElementsByTagName("head");
            for (var h = 0; h < heads.length; h++) {
                var scripts = heads[h].getElementsByTagName("script");
                for (var s = 0; s < scripts.length; s++) {
                    var url = scripts[s].src;
                    var i = url.indexOf("longwell-api.js");
                    if (i >= 0) {
                        Longwell.urlPrefix = url.substr(0, i);
                        
                        // Parse parameters
                        var q = url.indexOf("?");
                        if (q > 0) {
                            var params = url.substr(q + 1).split("&");
                            for (var p = 0; p < params.length; p++) {
                                var pair = params[p].split("=");
                                if (pair[0] == "locales") {
                                    desiredLocales = desiredLocales.concat(pair[1].split(","));
                                } else if (pair[0] == "defaultLocale") {
                                    defaultServerLocale = pair[1];
                                }
                            }
                        }
                        
                        return;
                    }
                }
            }
            throw new Error("Failed to derive URL prefix for Longwell API code files");
        })();
        
        var includeJavascriptFile = function(filename) {
            document.write("<script src='" + Longwell.urlPrefix + "scripts/" + filename + "' type='text/javascript'></script>");
        };
        var includeCssFile = function(filename) {
            document.write("<link rel='stylesheet' href='" + Longwell.urlPrefix + "styles/" + filename + "' type='text/css'/>");
        }
        
        /*
         *  Include non-localized files
         */
        for (var i = 0; i < javascriptFiles.length; i++) {
            includeJavascriptFile(javascriptFiles[i]);
        }
        for (var i = 0; i < cssFiles.length; i++) {
            includeCssFile(cssFiles[i]);
        }
        
        /*
         *  Include localized files
         */
        var loadLocale = [];
        loadLocale[defaultServerLocale] = true;
        
        var tryExactLocale = function(locale) {
            for (var l = 0; l < supportedLocales.length; l++) {
                if (locale == supportedLocales[l]) {
                    loadLocale[locale] = true;
                    return true;
                }
            }
            return false;
        }
        var tryLocale = function(locale) {
            if (tryExactLocale(locale)) {
                return locale;
            }
            
            var dash = locale.indexOf("-");
            if (dash > 0 && tryExactLocale(locale.substr(0, dash))) {
                return locale.substr(0, dash);
            }
            
            return null;
        }
        
        for (var l = 0; l < desiredLocales.length; l++) {
            tryLocale(desiredLocales[l]);
        }
        
        var defaultClientLocale = defaultServerLocale;
        var defaultClientLocales = ("language" in navigator ? navigator.language : navigator.browserLanguage).split(";");
        for (var l = 0; l < defaultClientLocales.length; l++) {
            var locale = tryLocale(defaultClientLocales[l]);
            if (locale != null) {
                defaultClientLocale = locale;
                break;
            }
        }
        
        for (var l = 0; l < supportedLocales.length; l++) {
            var locale = supportedLocales[l];
            if (loadLocale[locale]) {
                for (var i = 0; i < localizedJavascriptFiles.length; i++) {
                    includeJavascriptFile("l10n/" + locale + "/" + localizedJavascriptFiles[i]);
                }
                for (var i = 0; i < localizedCssFiles.length; i++) {
                    includeCssFile("l10n/" + locale + "/" + localizedCssFiles[i]);
                }
            }
        }
        
        document.write(
            "<script type='text/javascript'>" +
                "Longwell.Platform.serverLocale = '" + defaultServerLocale + "';" + 
                "Longwell.Platform.clientLocale = '" + defaultClientLocale + "';" +
            "</script>"
        );
        
        //var timelineAPI = "http://localhost:8888/api/timeline-api.js";
        var timelineAPI = "http://simile.mit.edu/timeline/api/timeline-api.js";
        var script = document.createElement("script");
        script.type = "text/javascript";
        script.src = timelineAPI;
        script.defer = true;
        document.documentElement.getElementsByTagName("head")[0].appendChild(script);
    } catch (e) {
        alert(e);
    }
})();