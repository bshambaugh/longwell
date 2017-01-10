var Calendar = new Object();

Calendar.parseDate = function(s) {
  try {
    if (s.length == 0) {
      return null;
    }
    var avoidZero = function(s) {
      return (s.substr(0,1) == "0") ? s.substr(1) : s;
    }
    var year = parseInt(s.substr(0,4));
    var month = parseInt(avoidZero(s.substr(5,2))) - 1;
    var day = parseInt(avoidZero(s.substr(8,2)));
    var hour = parseInt(avoidZero(s.substr(11,2)));
    var minute = parseInt(avoidZero(s.substr(14,2)));
    var second = parseInt(avoidZero(s.substr(17,2)));
    return new Date(year, month, day, hour, minute, second);
  } catch (e) {
    return null;
  }
}

Calendar.init = function(id, items) {
  Calendar._items = items;
  
  var earliest = new Date(2100, 0, 1).getTime();
  var latest = 0;

  for (var i = 0; i < items.length; i++) {
    var item = items[i];
    var startTime = item.start;
    var endTime = item.end;
    
    if ((startTime) && startTime.getTime() < earliest) {
      earliest = startTime.getTime();
    }
    if ((endTime) && endTime.getTime() > latest) {
      latest = endTime.getTime();
    }
  }
  
  var earliestDate = new Date(earliest);
  earliestDate.setHours(0);
  earliestDate.setMinutes(0);
  earliestDate.setSeconds(0);
  earliest = earliestDate.getTime();
  
  var timeSlotsArray = [];
  
  var headerRow = "<th></th>";
  var genericRow = "";
  var date = new Date(earliest);
  while (date.getTime() < latest) {
    headerRow += "<th>" + date.toLocaleDateString() + "</th>";
    genericRow += "<td><div class='lw_time_slot' /></td>";
    
    var a = [];
    for (h = 0; h < 48; h++) {
      a.push({ partitions : 0 , shifting : 0, html : "" });
    }
    timeSlotsArray.push(a);
    
    date = new Date(date.getTime() + 86400000);
  }

  var s = "<table cellpadding='0' cellspacing='0' border='0'><thead><tr>" + headerRow + "</tr></thead><tbody>";
  for (var h = 0; h < 24; h++) {
    s += "<tr><td class='lw_on_the_hour'>" + h + ":00</td>" + genericRow + "</tr>"
      + "<tr><td class='lw_on_the_half_hour'>" + h + ":30</td>" + genericRow + "</tr>";
  }
  s += "</tbody></table>";
    
  var calendarElmt = document.getElementById(id);
  calendarElmt.onmouseover = function() {
    Calendar.hidePreview();
  }
  calendarElmt.innerHTML = s;
 
  for (var i = 0; i < items.length; i++) {
    var item = items[i];
    var startTime = item.start;
    var endTime = item.end;
    if (startTime && endTime) {
      var diffStart = startTime.getTime() - earliest;
      var diffEnd = endTime.getTime() - earliest;
      
      var whichDay = 0;
      while (diffStart >= 86400000) {
        diffStart -= 86400000;
        diffEnd -= 86400000;
        whichDay++;
      }
      
      item.whichDay = whichDay;
      
      var timeSlots = timeSlotsArray[whichDay];
      
      var whichHalfHourStart = 0;
      var whichHalfHourEnd = 0;
      while (diffStart >= 1800000) {
        diffStart -= 1800000;
        diffEnd -= 1800000;
        whichHalfHourStart++;
        whichHalfHourEnd++;
      }
      while (diffEnd >= 1800000) {
        diffEnd -= 1800000;
        whichHalfHourEnd++;
      }
      
      if (whichHalfHourEnd == whichHalfHourStart)
        whichHalfHourEnd++;
        
      item.whichHalfHourStart = whichHalfHourStart;
      item.whichHalfHourEnd = whichHalfHourEnd;
      
      var partitions = 0;
      for (var t = whichHalfHourStart; t < whichHalfHourEnd; t++) {
        if (partitions < timeSlots[t].partitions)
          partitions = timeSlots[t].partitions;
      }
      
      item.partitionIndex = partitions;
      
      partitions++;
      for (var t = whichHalfHourStart; t < whichHalfHourEnd; t++) {
        timeSlots[t].partitions = partitions;
      }
      
      item.shifting = timeSlots[whichHalfHourStart].shifting;
      timeSlots[whichHalfHourStart].shifting -= (whichHalfHourEnd - whichHalfHourStart) * 8 - 1.5;
    }
  }
  
  var earliestHalfHour = 48;
  var latestHalfHour = 0;
  
  for (var i = 0; i < items.length; i++) {
    var item = items[i];
    var startTime = item.start;
    var endTime = item.end;
    if (startTime && endTime) {
      earliestHalfHour = Math.min(earliestHalfHour, item.whichHalfHourStart);
      latestHalfHour = Math.max(latestHalfHour, item.whichHalfHourEnd);
      
      var timeSlots = timeSlotsArray[item.whichDay];
      
      var partitions = timeSlots[item.whichHalfHourStart].partitions;
      var partitionIndex = item.partitionIndex;
      
      var partitionWidthPercents = Math.floor(100 / partitions);
      var partitionLeft = partitionIndex * partitionWidthPercents;
      
      var slots = item.whichHalfHourEnd - item.whichHalfHourStart;
      var heightInEm = 8 * slots - 1.5 ;
      var topInEm = item.shifting;
      
      var widthString = "width: " + (partitionWidthPercents) + "%;";
      var leftString = "left: " + partitionLeft + "%;";
      var topString = "top: " + topInEm + "em;";
      var heightString = "height: " + heightInEm + "em;";
      var html = 
        "<div class='lw_calendar_item' " +
        "onmouseover='Calendar.showPreview(" + i + ", this); event.stopPropagation();'" +
        "style='position: relative;" + 
        topString + 
        leftString + 
        widthString + 
        heightString + 
        "' " +
        "><div class='lw_calendar_item_frame'><div class='lw_calendar_item_inner'>";
      html += "<a href=\"" + item.focusURL + "\">";
      html += item.title;
      html += "</a></div></div></div>";
        
      timeSlots[item.whichHalfHourStart].html += html;
    }
  }
  
  var rows = calendarElmt.firstChild.tBodies[0].rows;
  
  if (earliestHalfHour % 2 == 1) earliestHalfHour--;
  if (latestHalfHour % 2 == 1) latestHalfHour++;
  for (h = 0; h < earliestHalfHour && h < latestHalfHour; h++) {
    rows[h].style.display = "none";
  }
  for (h = Math.max(earliestHalfHour, latestHalfHour); h < 48; h++) {
    rows[h].style.display = "none";
  }
  
  for (whichDay = 0; whichDay < timeSlotsArray.length; whichDay++) {
    var timeSlots = timeSlotsArray[whichDay];
    for (var whichHalfHour = 0; whichHalfHour < 48; whichHalfHour++) {
      var o = timeSlots[whichHalfHour];
      if (o.html.length > 0) {
        var cell = rows[whichHalfHour].cells[whichDay+1];
        cell.firstChild.innerHTML = o.html;
      }
    }
  }
}

Calendar.showPreview = function(i, elmt) {
  var item = Calendar._items[i];
  
  var html = "<div class='lw_calendar_item_preview_close'><img src='resources/images/cross.png' onclick='Calendar.hidePreview()' title='close'/></div>" +
  "<div class='lw_calendar_item_preview_title'>" + item.title + "</div>" +
  "<div class='lw_calendar_item_preview_body'>" + item.preview + "</div>";
  
  //var top = Math.floor(elmt.offsetHeight / 2);
  var top = 0;
  var left = Math.floor(elmt.offsetWidth / 2);
  var node = elmt;
  while (node) {
      top += node.offsetTop;
      left += node.offsetLeft;
      node = node.offsetParent;
  }

  var previewElmtContent = document.getElementById("lw_calendar_item_preview_content");
  previewElmtContent.innerHTML = html;
  
  var previewElmt = document.getElementById("lw_calendar_item_preview");
  previewElmt.style.left = left + "px";
  previewElmt.style.top = top + "px";
  previewElmt.style.display = "block";
}

Calendar.hidePreview = function() {
  var previewElmt = document.getElementById("lw_calendar_item_preview");
  previewElmt.style.display = "none";
}