/*
 * Utilities for handling RDF
 */

var Debug = {
	consoleService : null,
	
	onCaughtException : function (e) {
		try {
//			Debug.print(e.name + " [" + e.fileName + "," + e.lineNumber + "]:\n  message: " + e.message + "\n  description: " + e.description);
//			alert("Exception: " + e);
		} catch (e2) {
			Debug.print(e);
		}
	},
	
	print : function (msg) {
		try {
			if (!this.consoleService) {
				this.consoleService = Components.classes["@mozilla.org/consoleservice;1"]
					.getService(Components.interfaces.nsIConsoleService);
			}
			this.consoleService.logStringMessage(msg);
		} catch (e) {
			alert(e + "\nwhile trying to write to console:\n" + msg);
		}
	}
}
