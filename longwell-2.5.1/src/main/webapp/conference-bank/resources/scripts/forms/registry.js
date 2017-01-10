var Registry = new Object();

var prefixFoaf = "http://xmlns.com/foaf/0.1/";
var prefixConf = "http://simile.mit.edu/2005/11/ontologies/conference#";

Registry.types = [
  { title: "Person",
    uri: prefixFoaf + "Person"
  },
  { title: "Paper Talk",
    uri: prefixConf + "PaperTalk"
  },
  { title: "Poster Talk",
    uri: prefixConf + "PosterTalk"
  }
];

Registry.forms = [
  { title: "Link to Paper", 
    url: "form-paper-talk-link-to-paper.html",
    type: prefixConf + "PaperTalk"
  }
  ,
  { title: "Link to Paper", 
    url: "form-poster-talk-link-to-paper.html",
    type: prefixConf + "PosterTalk"
  }
  ,
  { title: "Edit Personal Information", 
    url: "form-person-edit-general.html",
    type: prefixFoaf + "Person"
  }
  
  /* ,{ title: "Testing", 
    url: "form-testing.html",
    type: null
  } */
];
