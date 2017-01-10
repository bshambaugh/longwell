var tl;
var eventSource;
var centerDate;

function initTimeline(timelineAPI) {
    var styles = [
        {   icon:       timelineAPI + "images/dull-blue-circle.png",
            color:      "#8085BC"
        },
        {   icon:       timelineAPI + "images/dull-red-circle.png",
            color:      "#BA6B66"
        },
        {   icon:       timelineAPI + "images/dull-green-circle.png",
            color:      "#6FBC6E"
        },
        {   icon:       timelineAPI + "images/blue-circle.png",
            color:      "#3734C1"
        },
        {   icon:       timelineAPI + "images/red-circle.png",
            color:      "#B94643"
        },
        {   icon:       timelineAPI + "images/green-circle.png",
            color:      "#27C82A"
        }
    ];
    var nextStyle = 0;
    var colorKeys = [];
    var styleAssignment = [];
    
    var eventIndex = new Timeline.EventIndex();
    for (var i = 0; i < itemData.length; i++) {
        var item = itemData[i];
        var startDate = Timeline.DateTime.parseIso8601DateTime(item.start);
        var endDate = Timeline.DateTime.parseIso8601DateTime(item.end);
        if (startDate == null) {
            startDate = endDate;
            endDate = null;
        }
        if (startDate == null) {
            continue;
        }
        if (endDate != null) {
            if (endDate.getTime() < startDate.getTime()) {
                var tempDate = startDate;
                startDate = endDate;
                endDate = tempDate;
            }
        }
        
        var style;
        if (item.colorKey in styleAssignment) {
            style = styleAssignment[item.colorKey];
        } else {
            var styleIndex = (nextStyle < styles.length) ? nextStyle++ : (styles.length - 1);
            style = styles[styleIndex];
            
            styleAssignment[item.colorKey] = style;
            if (item.colorKey == "") {
                colorKeys.push("n/a");
                styleAssignment["n/a"] = style;
            } else {
                colorKeys.push(item.colorKey);
            }
        }
        
        var evt = new Timeline.DefaultEventSource.Event(
            startDate,
            endDate,
            null,
            null,
            endDate == null,
            item.title,
            item.preview,
            null,           // image
            item.focusURL,  // link
            style.icon,     // icon
            style.color,    // color
            null            // text color
        );
        eventIndex.add(evt);
    }
    
    eventSource = new Timeline.DefaultEventSource(eventIndex);
    
    var earliest = eventSource.getEarliestDate();
    var latest = eventSource.getLatestDate();
    var duration = latest.getTime() - earliest.getTime();
    
    var intervalUnit = Timeline.DateTime.MILLENNIUM;
    while (intervalUnit > 0) {
        var intervalLength = Timeline.DateTime.gregorianUnitLengths[intervalUnit];
        if (duration / intervalLength > 50) {
            break;
        }
        intervalUnit--;
    }

    centerDate = new Date(Math.round((earliest.getTime() + latest.getTime()) / 2));
    var bandInfos = [
        Timeline.createBandInfo({
            width:          "75%", 
            intervalUnit:   intervalUnit, 
            intervalPixels: 150,
            eventSource:    eventSource,
            date:           centerDate,
            timeZone:       timeZone
        }),
        Timeline.createBandInfo({
            width:          "25%", 
            intervalUnit:   intervalUnit + 1, 
            intervalPixels: 200,
            eventSource:    eventSource,
            date:           centerDate,
            timeZone:       timeZone,
            showEventText:  false, 
            trackHeight:    0.5,
            trackGap:       0.2
        })
    ];
    bandInfos[1].syncWith = 0;
    bandInfos[1].highlight = true;
    bandInfos[1].eventPainter.setLayout(bandInfos[0].eventPainter.getLayout());
    
    var div = document.createElement("div");
    div.id = "tl";
    div.className = "timeline-default";
    div.style.height = "500px";
    document.getElementById("lw_content").appendChild(div);
    
    tl = Timeline.create(div, bandInfos, Timeline.HORIZONTAL);
    
    /*
     *  Create legend
     */
    var tableLegend = document.createElement("table");
    tableLegend.className = "timeline-legend";
    for (var i = 0; i < colorKeys.length; i++) {
        var tr = tableLegend.insertRow(i);
        
        var td = tr.insertCell(0);
        var divColor = document.createElement("div");
        divColor.style.width = "2em";
        divColor.style.height = "1em";
        divColor.style.background = styleAssignment[colorKeys[i]].color;
        td.appendChild(divColor);
        
        td = tr.insertCell(1);
        td.innerHTML = colorKeys[i];
    }
    document.getElementById("lw_content").appendChild(tableLegend);
    
    /*
     *  Create filtering/highlighting controls
     */
    var table = document.createElement("table");
    var tr = table.insertRow(0);
    
    var td = tr.insertCell(0);  td.innerHTML = "Filter:";
    td = tr.insertCell(1);      td.innerHTML = "Highlight:";
    
    var handler = function(elmt, evt, target) {
        onTimelineFilterHighlightKeyPress(tl, [0,1], table);
    };
    
    tr = table.insertRow(1);
    tr.style.verticalAlign = "top";
    
    td = tr.insertCell(0);
    
    var input = document.createElement("input");
    input.type = "text";
    Timeline.DOM.registerEvent(input, "keypress", handler);
    td.appendChild(input);
    
    var theme = Timeline.getDefaultTheme();
    for (var i = 0; i < theme.event.highlightColors.length; i++) {
        td = tr.insertCell(i + 1);
        
        input = document.createElement("input");
        input.type = "text";
        Timeline.DOM.registerEvent(input, "keypress", handler);
        td.appendChild(input);
        
        var divColor = document.createElement("div");
        divColor.style.height = "0.5em";
        divColor.style.background = theme.event.highlightColors[i];
        td.appendChild(divColor);
    }
    
    td = tr.insertCell(tr.cells.length);
    
    var button = document.createElement("button");
    button.innerHTML = "Clear All";
    Timeline.DOM.registerEvent(button, "click", function() {
        clearAllTimelineFilterHighlight(tl, [0,1], table);
    });
    td.appendChild(button);
    
    button = document.createElement("button");
    button.innerHTML = "Re-center Timeline";
    Timeline.DOM.registerEvent(button, "click", centerTimeline);
    td.appendChild(button);
    
    document.getElementById("lw_content").appendChild(table);
}


function centerTimeline() {
    tl.getBand(0).setCenterVisibleDate(Timeline.DateTime.parseGregorianDateTime(centerDate));
}

var timerID = null;
function onTimelineFilterHighlightKeyPress(timeline, bandIndices, table) {
    if (timerID != null) {
        window.clearTimeout(timerID);
    }
    timerID = window.setTimeout(function() {
        performTimelineFiltering(timeline, bandIndices, table);
    }, 300);
}
function performTimelineFiltering(timeline, bandIndices, table) {
    timerID = null;
    
    var cleanString = function (s) {
        return s.replace(/^\s+/, '').replace(/\s+$/, '');
    }
    
    var tr = table.rows[1];
    var text = cleanString(tr.cells[0].firstChild.value);
    
    var filterMatcher = null;
    if (text.length > 0) {
        var regex = new RegExp(text, "i");
        filterMatcher = function(evt) {
            return regex.test(evt.getText()) || regex.test(evt.getDescription());
        };
    }
    
    var regexes = [];
    var hasHighlights = false;
    for (var x = 1; x < tr.cells.length - 1; x++) {
        var input = tr.cells[x].firstChild;
        var text2 = cleanString(input.value);
        if (text2.length > 0) {
            hasHighlights = true;
            regexes.push(new RegExp(text2, "i"));
        } else {
            regexes.push(null);
        }
    }
    var highlightMatcher = hasHighlights ? function(evt) {
        var text = evt.getText();
        var description = evt.getDescription();
        for (var x = 0; x < regexes.length; x++) {
            var regex = regexes[x];
            if (regex != null && (regex.test(text) || regex.test(description))) {
                return x;
            }
        }
        return -1;
    } : null;
    
    for (var i = 0; i < bandIndices.length; i++) {
        var bandIndex = bandIndices[i];
        timeline.getBand(bandIndex).getEventPainter().setFilterMatcher(filterMatcher);
        timeline.getBand(bandIndex).getEventPainter().setHighlightMatcher(highlightMatcher);
    }
    timeline.paint();
}

function clearAllTimelineFilterHighlight(timeline, bandIndices, table) {
    var tr = table.rows[1];
    for (var x = 0; x < tr.cells.length - 1; x++) {
        tr.cells[x].firstChild.value = "";
    }
    
    for (var i = 0; i < bandIndices.length; i++) {
        var bandIndex = bandIndices[i];
        timeline.getBand(bandIndex).getEventPainter().setFilterMatcher(null);
        timeline.getBand(bandIndex).getEventPainter().setHighlightMatcher(null);
    }
    timeline.paint();
}
