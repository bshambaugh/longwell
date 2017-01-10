package edu.mit.simile.longwell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringEscapeUtils;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.simile.longwell.TagModel.TagComparator;
import edu.mit.simile.longwell.schema.LearnedProperty;

/**
 * This class aggregates several utility methods that are injected in the 
 * velocity context and that would be verbose or slow to perform directly
 * in a velocity template.
 */
public class VelocityUtilities {

    public boolean isURI(Object o) {
        return o instanceof URI;
    }
    
    public boolean isResource(Object o) {
        return o instanceof Resource;
    }
    
    public boolean isNull(Object o) {
        return o == null;
    }

    public boolean isNotNull(Object o) {
        return o != null;
    }
    
    public String randomString() {
        return Long.toString(new Random().nextLong());
    }

    public boolean isImageURL(String url) {
        url = url.toLowerCase();
        return url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".gif") || url.endsWith(".png");
    }

    public SortedSet sort(Set items, Comparator c) {
        TreeSet t = new TreeSet(c);
        t.addAll(items);
        return t;
    }

    public String escape(String str) {
        return StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeXml(str));
    }

    public String escapeForJavascript(String str) {
        return StringEscapeUtils.escapeJavaScript(str);
    }

    public String escapeForXML(String str) {
        return StringEscapeUtils.escapeXml(str);
    }

    public String softHyphenate(String s) {
        String s2 = "";
        int i = 0;
        while (i < s.length()) {
            int j = s.indexOf('/', i);
            if (j < 0) {
                j = s.indexOf('&', i);
            }

            if (j < 0) {
                j = s.length();
            } else {
                j++;
            }

            if (j < s.length()) {
                s2 += "<span class=\"lw_item_soft_hyphen\"> </span>";
            }
            s2 += s.substring(i, j);
            i = j;
        }

        return s2;
    }

    public String shorten(String s) {
        if (s.length() > 50) {
            return s.substring(0, 50) + "...";
        }
        return s;
    }

    public boolean isTypeConfident(LearnedProperty property, int type) {
        return property.getTypeConfidence(type) > 0.5;
    }

    public String encodeURLComponent(String s) {
        return Utilities.encode(s);
    }

    public String decodeURLComponent(String s) {
        return Utilities.decode(s);
    }

    public Resource getResourceProperty(Resource object, String propertyURI, Profile profile) throws Exception {
        Value v = null;
        RepositoryConnection c = null;
        try {
            c = profile.getRepository().getConnection();
            v = profile.getQueryManager().getObjectOfProperty(c, object, new URIImpl(propertyURI));
        } finally {
            if (c != null) c.close();
        }
        return (v instanceof Resource) ? (Resource) v : null;
    }
    
    public String getLiteralProperty(Resource object, String propertyURI, Profile profile) throws Exception {
        String s = null;
        RepositoryConnection c = null;
        try {
            c = profile.getRepository().getConnection();
            s = profile.getQueryManager().getStringOfProperty(c, object, new URIImpl(propertyURI));
        } finally {
            if (c != null) c.close();
        }
        return (s != null) ? s : "";
    }

    public String getPropertyValueLabel(Resource object, String propertyURI, Profile profile, String locale) throws Exception {
        Value v = null;
        RepositoryConnection c = null;
        try {
            c = profile.getRepository().getConnection();
            v = profile.getQueryManager().getObjectOfProperty(c, object, new URIImpl(propertyURI));
        } finally {
            if (c != null) c.close();
        }
        if (v instanceof Literal) {
            return ((Literal) v).getLabel();
        } else if (v instanceof URI) {
            return profile.getSchemaModel().getLabel((URI) v, locale); 
        } else {
            return null;
        }
    }
    
    public Set getPropertyValues(Resource object, String propertyURI, Profile profile) throws Exception {
        Set values = null;
        RepositoryConnection c = null;
        try {
            c = profile.getRepository().getConnection();
            values = profile.getQueryManager().listObjectsOfProperty(c, object, new URIImpl(propertyURI));
        } finally {
            if (c != null) c.close();
        }
        return values;
    }

    public Object getArrayElement(Object[] a, int index) {
        return a[index];
    }
    
    public int getElementIntegerAttribute(Element element, String attribute) {
        return Integer.parseInt(element.getAttribute(attribute));
    }
    
    public boolean getElementBooleanAttribute(Element element, String attribute) {
        return "true".equals(element.getAttribute(attribute));
    }
    
    public List getElementsByTagName(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        List<Node> list = new ArrayList<Node>();
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(nodeList.item(i));
        }
        return list;
    }
    
    public String makeEllipticalText(String text, int maxLength) {
        if (text.length() > maxLength) {
            StringBuffer sb = new StringBuffer(maxLength + 3);
            sb.append(text.subSequence(0, maxLength));
            sb.append("...");
            return sb.toString();
        } else {
            return text;
        }
    }
    
    public String getBarWitdhFromDouble(double d, int size) {
        return ((int) (d * (double) size)) + "px";
    }
    
    
    public TagComparator makeTagComparator() {
    	return new TagComparator();
    }
}
