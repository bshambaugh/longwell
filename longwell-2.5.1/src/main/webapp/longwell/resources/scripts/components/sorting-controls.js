function initializeSorterBlock(name, enabled) {
	enableSorterBlock(name, enabled);
}

function enableSorterBlock(name, enabled) {
	var className
	if (enabled) {
		className = "lw_sorter_listbox";
	} else {
		className = "lw_sorter_listbox lw_inactive";
	}

	var sorterList = document.getElementById("lw_" + name + "_sorter_list");
	sorterList.disabled = !enabled;
	sorterList.className = className;

	var directionRadioButtons = document.getElementsByName("lw_" + name + "_sorter_direction");
	for(var i = 0; i < directionRadioButtons.length; i++) {
		directionRadioButtons[i].disabled = !enabled;
		directionRadioButtons[i].className = className;
	}
}

function checkSorterBlock(index) {
	var checkbox = document.getElementById("lw_" + index + "_sort_block_check");
	var checked = checkbox.checked;

	enableSorterBlock(index, checked);

	if (index < 3) {
		if (checked) {
			document.getElementById("lw_" + (index + 1) + "_sort_block_check").disabled = false;
		} else if (index < 3) {
			var nextCheckbox = document.getElementById("lw_" + (index + 1) + "_sort_block_check");
			nextCheckbox.checked = false;

			checkSorterBlock(index + 1);

			nextCheckbox.disabled = true;
		}
	}
}

function selectSorter(blockIndex, sorterIndex) {
	var sorterList = document.getElementById("lw_" + blockIndex + "_sorter_list");
	sorterList.item(sorterIndex).selected = true;

	var ascendingRadio = document.getElementById("lw_" + blockIndex + "_sorter_ascending");
	var descendingRadio = document.getElementById("lw_" + blockIndex + "_sorter_descending");

	var ascendingLabel = document.getElementById("lw_" + blockIndex + "_sorter_ascending_label");
	var descendingLabel = document.getElementById("lw_" + blockIndex + "_sorter_descending_label");

	ascendingLabel.innerHTML = ascendingRankingLabels[sorterIndex];
	descendingLabel.innerHTML = descendingRankingLabels[sorterIndex];

	if (rankingAscendingFlags[sorterIndex]) {
		ascendingRadio.checked = true;
	} else {
		descendingRadio.checked = true;
	}
}

function resort() {
	var url = urlWithoutOrders;
	var index = 1;

	while (true) {
		url += "&" + getSorterURLParameter(index);

		index = index + 1;
		try {
			var checkbox = document.getElementById("lw_" + index + "_sort_block_check");
			if (!(checkbox) || !(checkbox.checked)) {
				break;
			}
		} catch (e) {
			break;
		}
	}
	browseTo(url);
}

function autosort() {
	browseTo(urlWithoutOrders);
}

function getSorterURLParameter(blockIndex) {
	var sorterList = document.getElementById("lw_" + blockIndex + "_sorter_list");
	var selectedIndex = sorterList.selectedIndex;

	var ascendingRadio = document.getElementById("lw_" + blockIndex + "_sorter_ascending");
	if (ascendingRadio.checked) {
		return ascendingURLParameter[selectedIndex];
	} else {
		return descendingURLParameter[selectedIndex];
	}
}
