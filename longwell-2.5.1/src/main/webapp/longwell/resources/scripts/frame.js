function toggleToolsTab(e) {
    var target = getTarget(e);
    if (!target.className) {
   		target = target.parentNode;
	} else {
		target = target.className;
	}
    toggleToolsTab2(target);
}
function toggleToolsTab2(target) {
    var body = document.getElementById(target + "_body");
    var invisible = (body.style.display != "block");
    var tools = document.getElementById("lw_tools");
    var tabs = tools.getElementsByTagName("li");
    var tab;
    var i;

    for (i = 0; i < tabs.length; i++) {
        tab = tabs[i];
        document.getElementById(tab.className + "_body").style.display = "none";
    }
    body.style.display = (invisible) ? "block" : "none";

    var top_tabs = document.getElementById("lw_tools_top_tabs");
    top_tabs.style.display = (invisible) ? "block" : "none";
    tabs = top_tabs.getElementsByTagName("li");

    for (i = 0; i < tabs.length; i++) {
        tab = tabs[i];
        tab.style.visibility = (tab.className == target) ? "hidden" : "visible";
    }

    var bottom_tabs = document.getElementById("lw_tools_bottom_tabs");
    tabs = bottom_tabs.getElementsByTagName("li");
    for (i = 0; i < tabs.length; i++) {
        tab = tabs[i];
        tab.style.visibility = (tab.className == target || !invisible) ? "visible" : "hidden";
    }
}

function getElementStyle(elem, property) {
    if (elem.currentStyle) {
        return elem.currentStyle[property];
    } else if (window.getComputedStyle) {
        var compStyle = window.getComputedStyle(elem, "");
        return compStyle.getPropertyValue(property);
    }
    return "";
}

function toggleFacet(e, id) {
    var target = getTarget(e);
    if (!target.className) target = target.parentNode;
    if (target.className == "lw_title_open") {
        target.className = "lw_title_closed";
    } else {
        target.className = "lw_title_open";
    }
    var drawer = document.getElementById("lw_facet_body_" + id);
    drawer.style.display = (drawer.style.display == "none") ? "block" : "none";
}
