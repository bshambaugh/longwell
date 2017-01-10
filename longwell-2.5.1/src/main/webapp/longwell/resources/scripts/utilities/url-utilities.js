var URLUtilities = {
	getRootURL : function() {
		return global_contextPath;
	}
	makeURL : function(command, profileID, query) {
		return URLUtilities.getRootURL() + "/" + command + "/" + profileID + "?" + query;
	}
}
