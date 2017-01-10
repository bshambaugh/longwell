/*==================================================
 *  Graphics Utility Functions and Constants
 *==================================================
 */

Longwell.Graphics = new Object();
Longwell.Graphics.pngIsTranslucent = !(Longwell.Platform.isIE && !Longwell.Platform.isIE7 && Longwell.Platform.isWin32);

Longwell.Graphics.createTranslucentImage = function(doc, url, verticalAlign) {
    var elmt;
    if (Longwell.Graphics.pngIsTranslucent) {
        elmt = doc.createElement("img");
        elmt.setAttribute("src", url);
    } else {
        elmt = doc.createElement("div");
        elmt.style.display = "inline";
        elmt.style.width = "1px";  // just so that IE will calculate the size property
        elmt.style.height = "1px";
        elmt.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + url +"', sizingMethod='image')";
    }
    elmt.style.verticalAlign = (verticalAlign != null) ? verticalAlign : "middle";
    return elmt;
};

Longwell.Graphics.setOpacity = function(elmt, opacity) {
    if (Longwell.Platform.isIE) {
        elmt.style.filter = "progid:DXImageTransform.Microsoft.Alpha(Style=0,Opacity=" + opacity + ")";
    } else {
        var o = (opacity / 100).toString();
        elmt.style.opacity = o;
        elmt.style.MozOpacity = o;
    }
};

Longwell.Graphics._bubbleMargins = {
    top:      33,
    bottom:   42,
    left:     33,
    right:    40
}

// pixels from boundary of the whole bubble div to the tip of the arrow
Longwell.Graphics._arrowOffsets = { 
    top:      0,
    bottom:   9,
    left:     1,
    right:    8
}

Longwell.Graphics._bubblePadding = 15;
Longwell.Graphics._bubblePointOffset = 6;
Longwell.Graphics._halfArrowWidth = 18;

Longwell.Graphics.createBubbleForPoint = function(doc, pageX, pageY, contentWidth, contentHeight) {
    var bubble = {
        _closed:    false,
        _doc:       doc,
        close:      function() { 
            if (!this._closed) {
                this._doc.body.removeChild(this._div);
                this._doc = null;
                this._div = null;
                this._content = null;
                this._closed = true;
            }
        }
    };
    
    var docWidth = doc.body.offsetWidth;
    var docHeight = doc.body.offsetHeight;
    
    var margins = Longwell.Graphics._bubbleMargins;
    var bubbleWidth = margins.left + contentWidth + margins.right;
    var bubbleHeight = margins.top + contentHeight + margins.bottom;
    
    var pngIsTranslucent = Longwell.Graphics.pngIsTranslucent;
    var urlPrefix = Longwell.urlPrefix;
    
    var setImg = function(elmt, url, width, height) {
        elmt.style.position = "absolute";
        elmt.style.width = width + "px";
        elmt.style.height = height + "px";
        if (pngIsTranslucent) {
            elmt.style.background = "url(" + url + ")";
        } else {
            elmt.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + url +"', sizingMethod='crop')";
        }
    }
    var div = doc.createElement("div");
    div.style.width = bubbleWidth + "px";
    div.style.height = bubbleHeight + "px";
    div.style.position = "absolute";
    div.style.zIndex = 1000;
    bubble._div = div;
    
    var divInner = doc.createElement("div");
    divInner.style.width = "100%";
    divInner.style.height = "100%";
    divInner.style.position = "relative";
    div.appendChild(divInner);
    
    var createImg = function(url, left, top, width, height) {
        var divImg = doc.createElement("div");
        divImg.style.left = left + "px";
        divImg.style.top = top + "px";
        setImg(divImg, url, width, height);
        divInner.appendChild(divImg);
    }
    
    createImg(urlPrefix + "images/bubble-top-left.png", 0, 0, margins.left, margins.top);
    createImg(urlPrefix + "images/bubble-top.png", margins.left, 0, contentWidth, margins.top);
    createImg(urlPrefix + "images/bubble-top-right.png", margins.left + contentWidth, 0, margins.right, margins.top);
    
    createImg(urlPrefix + "images/bubble-left.png", 0, margins.top, margins.left, contentHeight);
    createImg(urlPrefix + "images/bubble-right.png", margins.left + contentWidth, margins.top, margins.right, contentHeight);
    
    createImg(urlPrefix + "images/bubble-bottom-left.png", 0, margins.top + contentHeight, margins.left, margins.bottom);
    createImg(urlPrefix + "images/bubble-bottom.png", margins.left, margins.top + contentHeight, contentWidth, margins.bottom);
    createImg(urlPrefix + "images/bubble-bottom-right.png", margins.left + contentWidth, margins.top + contentHeight, margins.right, margins.bottom);
    
    var divClose = doc.createElement("div");
    divClose.style.left = (bubbleWidth - margins.right + Longwell.Graphics._bubblePadding - 16 - 2) + "px";
    divClose.style.top = (margins.top - Longwell.Graphics._bubblePadding + 1) + "px";
    divClose.style.cursor = "pointer";
    setImg(divClose, urlPrefix + "images/close-button.png", 16, 16);
    Longwell.DOM.registerEventWithObject(divClose, "click", bubble, bubble.close);
    divInner.appendChild(divClose);
        
    var divContent = doc.createElement("div");
    divContent.style.position = "absolute";
    divContent.style.left = margins.left + "px";
    divContent.style.top = margins.top + "px";
    divContent.style.width = contentWidth + "px";
    divContent.style.height = contentHeight + "px";
    divContent.style.overflow = "auto";
    divContent.style.background = "white";
    divInner.appendChild(divContent);
    bubble.content = divContent;
    
    (function() {
        if (pageX - Longwell.Graphics._halfArrowWidth - Longwell.Graphics._bubblePadding > 0 &&
            pageX + Longwell.Graphics._halfArrowWidth + Longwell.Graphics._bubblePadding < docWidth) {
            
            var left = pageX - Math.round(contentWidth / 2) - margins.left;
            left = pageX < (docWidth / 2) ?
                Math.max(left, -(margins.left - Longwell.Graphics._bubblePadding)) : 
                Math.min(left, docWidth + (margins.right - Longwell.Graphics._bubblePadding) - bubbleWidth);
                
            if (pageY - Longwell.Graphics._bubblePointOffset - bubbleHeight > 0) { // top
                var divImg = doc.createElement("div");
                
                divImg.style.left = (pageX - Longwell.Graphics._halfArrowWidth - left) + "px";
                divImg.style.top = (margins.top + contentHeight) + "px";
                setImg(divImg, urlPrefix + "images/bubble-bottom-arrow.png", 37, margins.bottom);
                divInner.appendChild(divImg);
                
                div.style.left = left + "px";
                div.style.top = (pageY - Longwell.Graphics._bubblePointOffset - bubbleHeight + 
                    Longwell.Graphics._arrowOffsets.bottom) + "px";
                
                return;
            } else if (pageY + Longwell.Graphics._bubblePointOffset + bubbleHeight < docHeight) { // bottom
                var divImg = doc.createElement("div");
                
                divImg.style.left = (pageX - Longwell.Graphics._halfArrowWidth - left) + "px";
                divImg.style.top = "0px";
                setImg(divImg, urlPrefix + "images/bubble-top-arrow.png", 37, margins.top);
                divInner.appendChild(divImg);
                
                div.style.left = left + "px";
                div.style.top = (pageY + Longwell.Graphics._bubblePointOffset - 
                    Longwell.Graphics._arrowOffsets.top) + "px";
                
                return;
            }
        }
        
        var top = pageY - Math.round(contentHeight / 2) - margins.top;
        top = pageY < (docHeight / 2) ?
            Math.max(top, -(margins.top - Longwell.Graphics._bubblePadding)) : 
            Math.min(top, docHeight + (margins.bottom - Longwell.Graphics._bubblePadding) - bubbleHeight);
                
        if (pageX - Longwell.Graphics._bubblePointOffset - bubbleWidth > 0) { // left
            var divImg = doc.createElement("div");
            
            divImg.style.left = (margins.left + contentWidth) + "px";
            divImg.style.top = (pageY - Longwell.Graphics._halfArrowWidth - top) + "px";
            setImg(divImg, urlPrefix + "images/bubble-right-arrow.png", margins.right, 37);
            divInner.appendChild(divImg);
            
            div.style.left = (pageX - Longwell.Graphics._bubblePointOffset - bubbleWidth +
                Longwell.Graphics._arrowOffsets.right) + "px";
            div.style.top = top + "px";
        } else { // right
            var divImg = doc.createElement("div");
            
            divImg.style.left = "0px";
            divImg.style.top = (pageY - Longwell.Graphics._halfArrowWidth - top) + "px";
            setImg(divImg, urlPrefix + "images/bubble-left-arrow.png", margins.left, 37);
            divInner.appendChild(divImg);
            
            div.style.left = (pageX + Longwell.Graphics._bubblePointOffset - 
                Longwell.Graphics._arrowOffsets.left) + "px";
            div.style.top = top + "px";
        }
    })();
    
    doc.body.appendChild(div);
    
    return bubble;
};

Longwell.Graphics.createAnimationIcon = function() {
    var span = document.createElement("span");
    
    var imgStop = document.createElement("img");
    imgStop.src = Longwell.urlPrefix + "images/progress.gif";
    imgStop.style.verticalAlign = "middle";
    span.appendChild(imgStop);
    
    var imgRunning = document.createElement("img");
    imgRunning.src = Longwell.urlPrefix + "images/progress-running.gif";
    imgRunning.style.display = "none";
    imgRunning.style.verticalAlign = "middle";
    span.appendChild(imgRunning);
    
    return {
        element:    span,
        run: function() {
            imgStop.style.display = "none";
            imgRunning.style.display = "inline";
        },
        stop: function() {
            imgStop.style.display = "inline";
            imgRunning.style.display = "none";
        }
    };
};

Longwell.Graphics.createPopupMenu = function(element, ui) {
    var div = document.createElement("div");
    div.className = "longwell-menu-popup longwell-ui-protection";
    
    var fClose = function() {
        document.body.removeChild(div);
        ui.popLevel();
    };
    var fCancel = function() {
        document.body.removeChild(div);
    };
    var fOpen = function() {
        var docWidth = document.body.offsetWidth;
        var docHeight = document.body.offsetHeight;
    
        var coords = Longwell.DOM.getPageCoordinates(element);
        div.style.top = (coords.top + element.scrollHeight) + "px";
        div.style.right = (docWidth - (coords.left + element.scrollWidth)) + "px";
        
        document.body.appendChild(div);
        
        ui.pushLevel(fCancel, false);
    };
    
    ui.registerEvent(
        element, "click", function(elmt, evt, target) { 
            fOpen();
            
            Longwell.DOM.cancelEvent(evt);
            return false;
        }, 0
    );
    
    return {
        element:    div,
        fOpen:      fOpen,
        fClose:     fClose
    };
};

Longwell.Graphics.createPopupDialog = function(ui, title, omitOK) {
    var o = {};
    
    o._dispose = function() {
        ui.popLevel();
        
        document.body.removeChild(o.dialogDiv);
        
        delete this.fOpen;
        delete this.fClose;
        delete this.fOnOK;
        delete this.fOnCancel;
        delete this._fCancel;
        delete this._fOnOKClick;
        delete this._fOnCancelClick;
        delete this._dispose;
        
        delete this.dialogDiv;
        delete this.bodyDiv;
        delete this.controlsDiv;
    };
    o.open = function() {
        document.body.appendChild(o.dialogDiv);
        o.dialogDiv.scrollIntoView();
        
        ui.pushLevel(o._onCancelClick, true);
    };
    o.close = function(ok) {
        if (ok) {
            o._onOKClick();
        } else {
            o._onCancelClick();
        }
    };
    
    o._onOKClick = function() {
        if ("onOK" in o) {
            try {
                o.onOK();
            } catch (e) {
                Longwell.Debug.exception(e);
            }
        }
        o._dispose();
    };
    o._onCancelClick = function() {
        if ("onCancel" in o) {
            try {
                o.onCancel();
            } catch (e) {
                Longwell.Debug.exception(e);
            }
        }
        o._dispose();
    };
    
    var div = document.createElement("div");
    div.className = "longwell-dialog longwell-ui-protection";
    
        var divTitle = document.createElement("div");
        divTitle.className = "title";
        divTitle.appendChild(document.createTextNode(title));
        div.appendChild(divTitle);
        
        var divBody = document.createElement("div");
        divBody.className = "body";
        div.appendChild(divBody);
        
        var divControls = document.createElement("div");
        divControls.className = "controls";
        div.appendChild(divControls);
        
            if (!(omitOK)) {
                var okButton = document.createElement("button");
                okButton.appendChild(document.createTextNode("OK"));
                Longwell.DOM.registerEvent(okButton, "click", o._onOKClick);
                divControls.appendChild(okButton);
            }
            
            var cancelButton = document.createElement("button");
            cancelButton.appendChild(document.createTextNode("Cancel"));
            Longwell.DOM.registerEvent(cancelButton, "click", o._onCancelClick);
            divControls.appendChild(cancelButton);

    o.dialogDiv = div;
    o.bodyDiv = divBody;
    o.controlsDiv = divControls;
    
    return o;
};
