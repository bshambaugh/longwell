var ATTENDEEMODE = 0;
var GALWAYMODE = 1;

function mapmode(el, mode, prefix, url) {
    var fr = document.getElementById('mapframe');
    fr.src = prefix + "&url=" + url + "&mode=" + mode;
    var moder = document.getElementById('mapframe-control');
    for (var i = 0; i < moder.childNodes.length; i++) {
        if (moder.childNodes[i].style) {
            moder.childNodes[i].style.background = '#fff';
        }
    }
    el.parentNode.style.background = '#eee';
}
