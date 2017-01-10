var Menu = {
};

Menu.onBodyMouseDown = function(evt) {
	evt = (evt) ? evt : event;
	var elmt = (evt.target) ? evt.target : evt.srcElement;
	var dismissMenu = true;

	while (elmt) {
		if (elmt.className) {
			if (elmt.className == "lw_popup_menu_item") {
				dismissMenu = false;
				break;
			}
		}
		elmt = elmt.parentNode;
	}

	if (dismissMenu) {
		Menu.hide();
	}
}

Menu.show = function(popupButtonID, popupTemplateID) {
	Menu.hide();

	Menu._popupTemplateID = popupTemplateID;

	var popupTemplate = document.getElementById(popupTemplateID);
	var popupButton = document.getElementById(popupButtonID);

	var popupID = Menu._popupTemplateIDToPopupID(popupTemplateID);
	var popup = document.getElementById(popupID);

	if (!popup) {
		popup = document.createElement("div");
		popup.id = popupID;
		popup.className = "lw_popup_menu";
		popup.innerHTML = popupTemplate.innerHTML;

		document.body.appendChild(popup);
	}

	var left = 0;
	var top = popupButton.offsetHeight;
	var elmt = popupButton;
	while (elmt) {
		left += elmt.offsetLeft;
		top += elmt.offsetTop;
		elmt = elmt.offsetParent;
	}

	popup.style.display = "block";
	popup.style.top = top + "px";
	popup.style.left = left + "px";

	Menu._popup = popup;
};

Menu.hide = function() {
	if (Menu._popup) {
		Menu._popup.style.display = "none";
		Menu._popup = null;
	}
}

Menu._popupTemplateIDToPopupID = function(popupTemplateID) {
	return popupTemplateID + "_popup";
}
