function promptForTimelineParameters(baseURL) {
    var dialog = document.getElementById("lw_timeline_dialog");
    if (dialog == null) {
        dialog = document.createElement("div");
        dialog.id = "lw_timeline_dialog";
        
        var titleDiv = document.createElement("div");
        titleDiv.id = "lw_timeline_dialog_title";
        titleDiv.innerHTML = "Configure Timeline View";
        dialog.appendChild(titleDiv);
        
        var bodyDiv = document.createElement("div");
        bodyDiv.id = "lw_timeline_dialog_body";
        dialog.appendChild(bodyDiv);
        
        var messageP = document.createElement("p");
        messageP.innerHTML = "Please select the appropriate properties for the following timeline settings:";
        bodyDiv.appendChild(messageP);
        
        var table = document.createElement("table");
        table.width = "100%";
        bodyDiv.appendChild(table);
        
        var fillSelect = function (s) {
            var option = document.createElement("option");
            option.innerHTML = "(none)";
            option.value = "";
            s.appendChild(option);
            
            for (var i = 0; i < commonProperties.length; i++) {
                var property = commonProperties[i];
                option = document.createElement("option");
                option.innerHTML = property.label;
                option.value = property.uri;
                s.appendChild(option);
            }
        };
        
        /*
         *  Start time
         */
        var tr = table.insertRow(0);
        var td = tr.insertCell(0);
        td.innerHTML = "Start Time";
        
        td = tr.insertCell(1);
        td.width = "70%";
        var select = document.createElement("select");
        select.id = "lw_timeline_starttime_select";
        fillSelect(select);
        td.appendChild(select);
        
        /*
         *  End time
         */
        tr = table.insertRow(1);
        td = tr.insertCell(0);
        td.innerHTML = "End Time";
        
        td = tr.insertCell(1);
        select = document.createElement("select");
        select.id = "lw_timeline_endtime_select";
        fillSelect(select);
        td.appendChild(select);
        
        /*
         *  Preview text
         */
        tr = table.insertRow(2);
        td = tr.insertCell(0);
        td.innerHTML = "Preview Text";
        
        td = tr.insertCell(1);
        select = document.createElement("select");
        select.id = "lw_timeline_preview_select";
        fillSelect(select);
        td.appendChild(select);

        /*
         *  Color by
         */
        tr = table.insertRow(3);
        td = tr.insertCell(0);
        td.innerHTML = "Color By";
        
        td = tr.insertCell(1);
        select = document.createElement("select");
        select.id = "lw_timeline_color_select";
        fillSelect(select);
        td.appendChild(select);
        
        /*
         *  Time zone
         */
        var timeZones = [
            {   title:  "GMT-12:00 - International Date Line West", value: -12 },
            {   title:  "GMT-11:00 - Midway Island, Samoa", value: -11 },
            {   title:  "GMT-10:00 - Hawaii", value: -10 },
            {   title:  "GMT-09:00 - Alaska", value: -9 },
            {   title:  "GMT-08:00 - Pacific Time (US & Canada); Tijuana", value: -8 },
            {   title:  "GMT-07:00 - Arizona", value: -7 },
            {   title:  "GMT-07:00 - Chihuahua, La Paz, Mazatlan", value: -7 },
            {   title:  "GMT-07:00 - Mountain Time (US & Canada)", value: -7 },
            {   title:  "GMT-06:00 - Central America", value: -6 },
            {   title:  "GMT-06:00 - Central Time (US & Canada)", value: -6 },
            {   title:  "GMT-06:00 - Guadalajara, Mexico City, Monterrey", value: -6 },
            {   title:  "GMT-06:00 - Saskatchewan", value: -6 },
            {   title:  "GMT-05:00 - Bogota, Lima, Quito", value: -5 },
            {   title:  "GMT-05:00 - Eastern Time (US & Canada)", value: -5 },
            {   title:  "GMT-05:00 - Indiana (East)", value: -5 },
            {   title:  "GMT-04:00 - Atlantic Time (Canada)", value: -4 },
            {   title:  "GMT-04:00 - Caracas, La Paz", value: -4 },
            {   title:  "GMT-04:00 - Santiago", value: -4 },
            {   title:  "GMT-03:30 - Newfoundland", value: -3.5 },
            {   title:  "GMT-03:00 - Brasilia", value: -3 },
            {   title:  "GMT-03:00 - Buenos Aires, Georgetown", value: -3 },
            {   title:  "GMT-03:00 - Greenland", value: -3 },
            {   title:  "GMT-02:00 - Mid-Atlantic", value: -2 },
            {   title:  "GMT-01:00 - Azores", value: -1 },
            {   title:  "GMT-01:00 - Cape Verde Is.", value: -1 },
            {   title:  "GMT - Casablanca, Monrovia", value: 0 },
            {   title:  "GMT - Greenwich Mean Time: Dublin, Edinburgh, Lisbon, London", value: 0 },
            {   title:  "GMT+01:00 - Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna", value: 1 },
            {   title:  "GMT+01:00 - Belgrade, Bratislava, Budapest, Ljubljana, Prague", value: 1 },
            {   title:  "GMT+01:00 - Brussels, Copenhagen, Madrid, Paris", value: 1 },
            {   title:  "GMT+01:00 - Sarajevo, Skopje, Warsaw, Zagreb", value: 1 },
            {   title:  "GMT+01:00 - West Central Africa", value: 1 },
            {   title:  "GMT+02:00 - Athens, Beirut, Istanbul, Minsk", value: 2 },
            {   title:  "GMT+02:00 - Bucharest", value: 2 },
            {   title:  "GMT+02:00 - Cairo", value: 2 },
            {   title:  "GMT+02:00 - Harare, Pretoria", value: 2 },
            {   title:  "GMT+02:00 - Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius", value: 2 },
            {   title:  "GMT+02:00 - Jerusalem", value: 2 },
            {   title:  "GMT+02:00 - Baghdad", value: 2 },
            {   title:  "GMT+02:00 - Kuwait, Riyadh", value: 2 },
            {   title:  "GMT+03:00 - Moscow, St. Petersburg, Volgograd", value: 3 },
            {   title:  "GMT+03:00 - Nairobi", value: 3 },
            {   title:  "GMT+03:30 - Tehran", value: 3.5 },
            {   title:  "GMT+04:00 - Abu Dhabi, Muscat", value: 4 },
            {   title:  "GMT+04:00 - Baku, Tbilisi, Yerevan", value: 4 },
            {   title:  "GMT+04:30 - Kabul", value: 4.5 },
            {   title:  "GMT+05:00 - Ekaterinburg", value: 5 },
            {   title:  "GMT+05:00 - Islamabad, Karachi, Tashkent", value: 5 },
            {   title:  "GMT+05:30 - Chennai, Kolkata, Mumbai, New Delhi", value: 5.5 },
            {   title:  "GMT+05:45 - Kathmandu", value: 5.75 },
            {   title:  "GMT+06:00 - Almaty, Novosibirsk", value: 6 },
            {   title:  "GMT+06:00 - Astana, Dhaka", value: 6 },
            {   title:  "GMT+06:00 - Sri Jayawardenepura", value: 6 },
            {   title:  "GMT+06:30 - Rangoon", value: 6.5 },
            {   title:  "GMT+07:00 - Bangkok, Hanoi, Jakarta", value: 7 },
            {   title:  "GMT+07:00 - Krasnoyarsk", value: 7 },
            {   title:  "GMT+08:00 - Beijing, Chongqing, Hong Kong, Urumqi", value: 8 },
            {   title:  "GMT+08:00 - Irkutsk, Ulaan Bataar", value: 8 },
            {   title:  "GMT+08:00 - Kuala Lumpur, Singapore", value: 8 },
            {   title:  "GMT+08:00 - Perth", value: 8 },
            {   title:  "GMT+08:00 - Taipei", value: 8 },
            {   title:  "GMT+09:00 - Osaka, Sapporo, Tokyo", value: 9 },
            {   title:  "GMT+09:00 - Seoul", value: 9 },
            {   title:  "GMT+09:00 - Yakutsk", value: 9 },
            {   title:  "GMT+09:30 - Adelaide", value: 9.5 },
            {   title:  "GMT+09:30 - Darwin", value: 9.5 },
            {   title:  "GMT+10:00 - Brisbane", value: 10 },
            {   title:  "GMT+10:00 - Canberra, Melbourne, Sydney", value: 10 },
            {   title:  "GMT+10:00 - Guam, Port Moresby", value: 10 },
            {   title:  "GMT+10:00 - Hobart", value: 10 },
            {   title:  "GMT+10:00 - Vladivostok", value: 10 },
            {   title:  "GMT+11:00 - Magadan, Solomon Is., New Caledonia", value: 11 },
            {   title:  "GMT+12:00 - Auckland, Wellington", value: 12 },
            {   title:  "GMT+12:00 - Fiji, Kamchatka, Marshall Is.", value: 12 },
            {   title:  "GMT+13:00 - Nuku'alofa", value: 13 }
        ];
        tr = table.insertRow(4);
        td = tr.insertCell(0);
        td.innerHTML = "Time Zone";
        
        td = tr.insertCell(1);
        select = document.createElement("select");
        select.id = "lw_timeline_timezone_select";
        for (var z = 0; z < timeZones.length; z++) {
            var zone = timeZones[z];
            option = document.createElement("option");
            option.innerHTML = zone.title;
            option.value = zone.value;
            select.appendChild(option);
        }
        select.value = 0;
        td.appendChild(select);
        
        /*
         *  Buttons
         */
        var buttonsDiv = document.createElement("div");
        buttonsDiv.id = "lw_timeline_dialog_buttons";
        bodyDiv.appendChild(buttonsDiv);
        
        var button = document.createElement("button");
        button.innerHTML = "Cancel";
        button.onclick = closeTimelineDialog;
        buttonsDiv.appendChild(button);

        button = document.createElement("button");
        button.innerHTML = "Show Timeline";
        button.onclick = function() { showTimeline(baseURL); };
        buttonsDiv.appendChild(button);

        document.body.appendChild(dialog);
    }
    dialog.style.display = "block";
}

function closeTimelineDialog() {
    document.getElementById("lw_timeline_dialog").style.display = "none";
}

function showTimeline(baseURL) {
    var startTime = document.getElementById("lw_timeline_starttime_select").value;
    var endTime = document.getElementById("lw_timeline_endtime_select").value;
    var previewText = document.getElementById("lw_timeline_preview_select").value;
    var color = document.getElementById("lw_timeline_color_select").value;
    var timeZone = document.getElementById("lw_timeline_timezone_select").value;
    
    browseTo(baseURL + "&resultsViewParam=" + 
        encodeURIComponent([ startTime, endTime, previewText, color, timeZone ].join(",")));
}
