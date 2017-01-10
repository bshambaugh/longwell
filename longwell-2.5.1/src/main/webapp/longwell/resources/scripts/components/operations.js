var Operations = {};

Operations.trust = function(objectURI, profileID, title) {
    try {
        if (confirm("Activate scraper '" + title + "'?\n\nSystem items contain executable code that may harm your computer. Activate it only if it comes from a location that you trust.")) {
            var postURL = g_contextPath + "/" + profileID + "?command=system";
            HTTPUtilities.doPost(
                postURL,
                "trust\n" + objectURI,
                function(status, statusText) {
                    window.alert("Failed to incorporate the scraper into the system.");
                },
                function (text) {
                    reloadView(objectURI, profileID, "lw_item_", "", [], true);
                }
            );
        }
    } catch (e) {
        Debug.onCaughtException(e);
    }
}

Operations.distrust = function(objectURI, profileID, title) {
    try {
        if (confirm("De-activate scraper '" + title + "'?")) {
            var postURL = g_contextPath + "/" + profileID + "?command=system";
            HTTPUtilities.doPost(
                postURL,
                "distrust\n" + objectURI,
                function(status, statusText) {
                    window.alert("Failed to remove the scraper from the system.");
                },
                function (text) {
                    reloadView(objectURI, profileID, "lw_item_", "", [], true);
                }
            );
        }
    } catch (e) {
        Debug.onCaughtException(e);
    }
}

Operations.save = function(objectURI, profileID) {
    try {
        HTTPUtilities.doPost(
            g_contextPath + "/" + profileID + "?command=save",
            objectURI,
            function(status, statusText) {
                window.alert("Failed to save the item.");
            },
            function (text) {
                reloadView(objectURI, profileID, "lw_item_", "", [], true);
            }
        );
    } catch (e) {
        Debug.onCaughtException(e);
    }
}

Operations.remove = function(objectURI, title) {
    try {
        if (confirm("Delete '" + title + "'?")) {
            var postURL = g_profileURL + "command=remove";
            HTTPUtilities.doPost(
                postURL,
                objectURI,
                function(status, statusText) {
                    window.alert("Failed to delete the item.");
                },
                function (text) {
                    location.href = location.href; // trying to refresh
                }
            );
        }
    } catch (e) {
        Debug.onCaughtException(e);
    }
}

Operations.publish = function(objectURI, profileID) {
    try {
        HTTPUtilities.doPost(
            g_contextPath + "/" + profileID + "?command=publish",
            objectURI,
            function(status, statusText) {
                var guess = "";
                if (status == 503) {
                    guess = "Perhaps you have not registered for any bank account.";
                }
                window.alert("Failed to publish the item.\n" + guess + "\nStatus code: " + status);
            },
            function (text) {
                reloadView(objectURI, profileID, "lw_item_", "", [], true);
            }
        );
    } catch (e) {
        Debug.onCaughtException(e);
    }
}

Operations.persist = function(objectURI, profileID) {
    try {
        var f = function() {
            HTTPUtilities.doPost(
                g_contextPath + "/default?command=persist",
                objectURI,
                function(status, statusText) {
                    var guess = "";
                    if (status == 503) {
                        guess = "Perhaps you have not registered for any bank account.";
                    }
                    window.alert("Failed to deposit the item.\n" + guess + "\nStatus code: " + status);
                },
                function (text) {
                    reloadView(objectURI, profileID, "lw_item_", "", [], true);
                }
            );
        }

        if (profileID != "default") {
            HTTPUtilities.doPost(
                g_contextPath + "/" + profileID + "?command=save",
                objectURI,
                function(status, statusText) {
                    window.alert("Failed to save the item locally first.");
                },
                function (text) {
                    f();
                }
            );
        } else {
            f();
        }
    } catch (e) {
        Debug.onCaughtException(e);
    }
}
Operations.saveAll = function(url) {
    try {
        HTTPUtilities.doPost(
            url,
            "",
            function(status, statusText) {
                alert("Failed to save the selected items.");
            },
            function (text) {
                alert("All the selected items have been saved.");
                window.location.reload(true);
            }
        );
    } catch (e) {
        Debug.onCaughtException(e);
    }
}

Operations.removeAll = function(url, count) {
    try {
        if (confirm("Delete all " + count + " items?")) {
            HTTPUtilities.doPost(
                url,
                "",
                function(status, statusText) {
                    alert("Failed to delete the items.");
                },
                function (text) {
                    alert("All the selected items have been deleted.");
                }
            );
        }
    } catch (e) {
        Debug.onCaughtException(e);
    }
}

Operations.publishAll = function(publishAllURL, saveAllURL) {
    try {
        var f = function() {
            HTTPUtilities.doPost(
                publishAllURL,
                "",
                function(status, statusText) {
                    var guess = "";
                    if (status == 503) {
                        guess = "Perhaps you have not registered for any bank account.";
                    }
                    window.alert("Failed to publish tidbits.\n" + guess + "\nStatus code: " + status);
                },
                function (text) {
                    alert("All the selected items have been published.");
                }
            );
        };

        if (saveAllURL) {
            HTTPUtilities.doPost(
                saveAllURL,
                "",
                function(status, statusText) {
                    alert("Failed to save items before publishing.");
                },
                function (text) {
                    f();
                }
            );
        } else {
            f();
        }
    } catch (e) {
        Debug.onCaughtException(e);
    }
}

Operations.persistAll = function(persistAllURL, saveAllURL) {
    try {
        var f = function() {
            HTTPUtilities.doPost(
                persistAllURL,
                "",
                function(status, statusText) {
                    var guess = "";
                    if (status == 503) {
                        guess = "Perhaps you have not registered for any bank account.";
                    }
                    window.alert("Failed to deposit the items.\n" + guess + "\nStatus code: " + status);
                },
                function (text) {
                    alert("All the selected items have been deposited.");
                }
            );
        };

        if (saveAllURL) {
            HTTPUtilities.doPost(
                saveAllURL,
                "",
                function(status, statusText) {
                    alert("Failed to save the items before depositing.");
                },
                function (text) {
                    f();
                }
            );
        } else {
            f();
        }
    } catch (e) {
        Debug.onCaughtException(e);
    }
}
