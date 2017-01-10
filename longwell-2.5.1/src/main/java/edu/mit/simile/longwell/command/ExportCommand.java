package edu.mit.simile.longwell.command;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

import edu.mit.simile.longwell.LongwellURL;
import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.QueryManager;
import edu.mit.simile.longwell.Utilities;
import edu.mit.simile.longwell.query.engine.QueryEngine;
import edu.mit.simile.longwell.schema.SchemaModel;
import edu.mit.simile.velocity.InjectionManager;

public class ExportCommand extends CommandBase {

    public ExportCommand(InjectionManager injectionManager, String template) {
        super(injectionManager, template);
    }
    
    public void execute(Message msg) throws ServletException {
        String format = msg.m_query.getFirstParamValue("format");
        if (format == null) {
            format = "RDFXML";
        }
        setContentType(format, msg.m_response);

        Set objects = null;

        String objectURI = msg.m_query.getFirstParamValue("objectURI");
        if (objectURI == null) {
            objects = getMany(msg);
        } else {
            objects = new HashSet<URI>();
            objects.add(new URIImpl(objectURI));
        }

        if ("GMap".equals(format)) {
            exportGMap(msg, objects);
        } else if ("iCal".equals(format)) {
            exportICal(msg, objects);
        } else {
            exportRDF(msg, format, objects);
        }
    }

    protected Set getMany(Message msg) throws ServletException {
        try {
            Profile profile = msg.getProfile();
            QueryEngine queryModel = (QueryEngine) profile.getStructuredModel(QueryEngine.class);

            Set objects = queryModel.queryObjects(msg.m_query, false);
            if (objects == null) {
                objects = profile.getSchemaModel().getAllItems();
            }

            return objects;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected void exportRDF(Message msg, String format, Set objects) throws ServletException {
        try {
            OutputStream os = msg.m_response.getOutputStream();

            Profile profile = msg.getProfile();

            Repository local = Utilities.createMemoryRepository();
            try {
                Iterator i = objects.iterator();
                while (i.hasNext()) {
                    URI object = (URI) i.next();

                    profile.extractObject(object, local);
                }

                Utilities.writeRDF(local, os, format);
            } finally {
                local.shutDown();
            }

            os.close();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    static private class LLRecord {
        double m_latitude;

        double m_longitude;

        Set<URI> m_objects;

        Set<Integer> m_codes;
    }
    
    public class LegendEntry {
    		private String m_markerURL;
    		
    		private String m_label;
    		
    		public LegendEntry(String marker, String label) {
    			m_markerURL = marker;
    			m_label = label;
    		}
    		
    		public String getMarkerURL() {
    			return m_markerURL;
    		}
    		
    		public String getLabel() {
    			return m_label;
    		}
    }
    
    public class MapLocation {
		private int m_index;
    		private double m_latitude;
    		private double m_longitude;
    		private String m_markerURL;
    		private List<MapLocationItem> m_items;
    		
    		public MapLocation(int index, double latitude, double longitude, String marker) {
    			m_items = new ArrayList<MapLocationItem>();
    			m_index = index;
    			m_latitude = latitude;
    			m_longitude = longitude;
    			m_markerURL = marker;
    		}
    		
    		public void addItem(MapLocationItem item) {
    			m_items.add(item);
    		}
    		
    		public double lat() {
    			return m_latitude;
    		}
    		
    		public String getLatitude() {
    			return Double.toString(m_latitude);
    		}
    		
    		public double lng() {
    			return m_longitude;
    		}
    		
    		public String getLongitude() {
    			return Double.toString(m_longitude);
    		}
    		
    		public int getIndex() {
    			return m_index;
    		}
    		
    		public String getMarkerURL() {
    			return m_markerURL;
    		}
    		
    		public List getItems() {
    			return m_items;
    		}
    		
    		public Iterator getItemsIterator() {
    			return m_items.iterator();
    		}
    }
    
    public class MapLocationItem {
		private String m_label;
		private String m_itemURL;
		private String m_editURL;
		private String m_removeURL;

		public MapLocationItem(String label, String URL, String editURL, String removeURL) {
			m_label = label;
			m_itemURL = URL;
			m_editURL = editURL;
			m_removeURL = removeURL;
		}
		
		public String getLabel() {
			return m_label;
		}
		
		public String getItemURL() {
			return m_itemURL;
		}
		
		public String getEditURL() {
			return m_editURL;
		}
		
		public String getRemoveURL() {
			return m_removeURL;
		}
    }

    protected void exportGMap(Message msg, Set objects) throws ServletException {
        try {
            String[] properties = StringUtils.splitPreserveAllTokens(msg.m_query.getFirstParamValue("latlong"), ';');
            Set<URI> propertyURIs = new HashSet<URI>();

            for (int i = 0; i < properties.length; i++) {
                String property = properties[i];
                if (property != null && property.length() > 0) {
                    propertyURIs.add(new URIImpl(property));
                }
            }

            VelocityContext vcContext = createContext(msg);
            List<MapLocation> locations = new ArrayList<MapLocation>();
            
            Profile profile = msg.getProfile();
            RepositoryConnection c = null;
            try {
                c = profile.getRepository().getConnection();
                SchemaModel schemaModel = profile.getSchemaModel();

                double minLatitude = Double.POSITIVE_INFINITY;
                double maxLatitude = Double.NEGATIVE_INFINITY;
                double minLongitude = Double.POSITIVE_INFINITY;
                double maxLongitude = Double.NEGATIVE_INFINITY;

                Map<String,LLRecord> llToRecord = new HashMap<String,LLRecord>();
                Map<URI,Integer> typeToCode = new HashMap<URI,Integer>();
                int codeMax = 0;

                Iterator i = objects.iterator();
                while (i.hasNext()) {
                    URI object = (URI) i.next();

                    URI typeURI = schemaModel.getLearnedClassURIOfItem(object);
                    Integer code = typeToCode.get(typeURI);
                    if (code == null) {
                        code = new Integer(codeMax++);
                        typeToCode.put(typeURI, code);
                    }

                    Iterator j = propertyURIs.iterator();
                    while (j.hasNext()) {
                        URI property = (URI) j.next();
                        String latlong = profile.getQueryManager().getStringOfProperty(c, object, property);
                        if (latlong != null) {
                            LLRecord ll = (LLRecord) llToRecord.get(latlong);
                            if (ll == null) {
                                ll = new LLRecord();
                                int comma = latlong.indexOf(',');
                                if (comma > 0) {
                                    try {
                                        double latitude = Double.parseDouble(latlong.substring(0, comma));
                                        double longitude = Double.parseDouble(latlong.substring(comma + 1));
                                        
                                        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                                            break;
                                        }
                                        
                                        ll.m_objects = new HashSet<URI>();
                                        ll.m_codes = new HashSet<Integer>();

                                        minLatitude = Math.min(minLatitude, latitude);
                                        maxLatitude = Math.max(maxLatitude, latitude);
                                        minLongitude = Math.min(minLongitude, longitude);
                                        maxLongitude = Math.max(maxLongitude, longitude);

                                        ll.m_latitude = latitude;
                                        ll.m_longitude = longitude;
                                        ll.m_codes.add(code);
                                    } catch (Exception e) {
                                        // ignore
                                    }
                                }

                                llToRecord.put(latlong, ll);
                            }
                            if (ll.m_objects != null) {
                                ll.m_objects.add(object);
                            }
                            break;
                        }
                    }
                }

                double centerLatitude = (minLatitude + maxLatitude) / 2;
                double centerLongitude = (minLongitude + maxLongitude) / 2;
                double spanLatitude = Math.max(0.1, (maxLatitude - minLatitude));
                double spanLongitude = Math.max(0.1, (maxLongitude - minLongitude));

                msg.m_response.setContentType("text/xml");
                msg.m_response.setCharacterEncoding("UTF-8");

                vcContext.put("exportType", "GMap");
                vcContext.put("centerLatitude", Double.toString(centerLatitude));
                vcContext.put("centerLongitude", Double.toString(centerLongitude));
                
                vcContext.put("spanLatitude", Double.toString(spanLatitude));
                vcContext.put("spanLongitude", Double.toString(spanLongitude));

                LongwellURL url = new LongwellURL(msg);

                /*
                 * Legend
                 */
                List<LegendEntry> legend = new ArrayList<LegendEntry>();
                i = typeToCode.keySet().iterator();
                while (i.hasNext()) {
                    URI typeURI = (URI) i.next();
                    Integer code = (Integer) typeToCode.get(typeURI);
                    String markerURL = url.getContextPath() + "/resources/marker?x=0&y=0&s=1&w=40&h=34&label=%20&colorCode=" + code.intValue();
                    String typeLabel = schemaModel.getLabel(typeURI, "");
                    legend.add(new LegendEntry(StringEscapeUtils.escapeXml(markerURL), typeLabel));
                }
                vcContext.put("legend", legend);

                /*
                 * Individual locations
                 */
                URI rdfType = RDF.TYPE;
                i = llToRecord.keySet().iterator();
                int index = 0;
                while (i.hasNext()) {
                    String latlong = (String) i.next();
                    LLRecord ll = (LLRecord) llToRecord.get(latlong);
                    if (ll.m_objects != null) {
                        int objectCount = ll.m_objects.size();
                        String markerLabel = objectCount > 1 ? Integer.toString(objectCount) : "+";

                        String colorCode = "";
                        Iterator j = ll.m_codes.iterator();
                        while (j.hasNext()) {
                            Integer integer = (Integer) j.next();
                            if (colorCode.length() > 0) {
                                colorCode += ",";
                            }
                            colorCode += integer;
                        }

                        String markerURL = url.getContextPath() + "/resources/marker?x=0&y=0&s=1&w=40&h=34" + "&label=" + markerLabel + "&colorCode=" + colorCode;

                        MapLocation location = new MapLocation(index++, ll.m_latitude, ll.m_longitude, StringEscapeUtils.escapeXml(markerURL));

                        Iterator n = ll.m_objects.iterator();
                        while (n.hasNext()) {
                            URI object = (URI) n.next();
                            String objectURI = object.toString();
                            String label = StringEscapeUtils.escapeXml(schemaModel.getLabel(object, ""));
                            String objectURL = StringEscapeUtils.escapeXml(msg.getURL(object));

                            StringBuffer editURL = new StringBuffer();
                            {
                                editURL.append(url.getContextPath() + "/resources/forms/?uri="
                                        + Utilities.encode(objectURI));

                                Iterator types = profile.getQueryManager().listObjectsOfProperty(c, object, rdfType).iterator();
                                while (types.hasNext()) {
                                    URI typeURI = (URI) types.next();
                                    editURL.append("&amp;type=" + Utilities.encode(typeURI.toString()));
                                }
                            }

                            LongwellURL removeURL = url.changeCommandQuery("remove-location", "");
                            {
                                removeURL.getQuery().setParameter("objectURI", objectURI);
                                removeURL.getQuery().setParameter("latlong", msg.m_query.getFirstParamValue("latlong"));
                            }
                            
                            MapLocationItem item = new MapLocationItem(label, objectURL, editURL.toString(), StringEscapeUtils.escapeXml(removeURL.toURLString()));
                            location.addItem(item);
                        }
                        locations.add(location);
                    }
                }
            } finally {
                if (c != null) c.close();
            }
            
            vcContext.put("locations", locations);
            msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public class CalendarEvent {
    		private String m_start;
    		private String m_end;
    		private String m_URL;
    		private String m_summary;
    		
    		public CalendarEvent(String start, String end, String URL, String summary) {
    			m_start = start;
    			m_end = end;
    			m_URL = URL;
    			m_summary = summary;
    		}
    		
    		public String getStart() {
    			return m_start;
    		}
    		
    		public String getEnd() {
    			return m_end;
    		}
    		
    		public String getURL() {
    			return m_URL;
    		}
    		
    		public String getSummary() {
    			return m_summary;
    		}
    }
    
    protected void exportICal(Message msg, Set objects) throws ServletException {
        try {
            String[] properties = StringUtils.splitPreserveAllTokens(msg.m_query.getFirstParamValue("properties"), ',');

            URI startProperty = new URIImpl(properties[0]);
            URI endProperty = new URIImpl(properties[1]);
            // URI previewProperty = new URIImpl(properties[2]);

            VelocityContext vcContext = createContext(msg);
            List<CalendarEvent> events = new ArrayList<CalendarEvent>();

            Profile profile = msg.getProfile();
            RepositoryConnection c = null;
            try {
                c = profile.getRepository().getConnection();
                SchemaModel schemaModel = profile.getSchemaModel();
                QueryManager queryManager = profile.getQueryManager();

                msg.m_response.setCharacterEncoding("UTF-8");
                msg.m_response.setContentType("text/calendar");
                
                vcContext.put("exportType", "iCal");
                
                Iterator i = objects.iterator();
                while (i.hasNext()) {
                    URI object = (URI) i.next();

                    String objectURI = object.toString();
                    String label = schemaModel.getLabel(object, "");
                    // String objectURL =
                    // StringEscapeUtils.escapeJavaScript(msg.getURL(object));

                    String startString = toICalDate(queryManager.getStringOfProperty(c, object, startProperty));
                    String endString = toICalDate(queryManager.getStringOfProperty(c, object, endProperty));
                    // String previewString = RDFUtilities.getStringOfProperty(r,
                    // object, previewProperty);

                    if (startString != null && endString != null) {
                        CalendarEvent event = new CalendarEvent(startString,
                                endString,
                                StringEscapeUtils.escapeJavaScript(objectURI),
                                StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeXml(label)));
                        events.add(event);
                        /*
                         * sb.append("DESCRIPTION:"); if (previewString != null) {
                         * sb.append(StringEscapeUtils.escapeJavaScript(previewString)); }
                         * sb.append(StringEscapeUtils.escapeJavaScript("\n\nURL: <a
                         * href='"));
                         * sb.append(StringEscapeUtils.escapeJavaScript(objectURL));
                         * sb.append(StringEscapeUtils.escapeJavaScript("'>"));
                         * sb.append(StringEscapeUtils.escapeJavaScript(objectURL));
                         * sb.append(StringEscapeUtils.escapeJavaScript("</a>"));
                         * sb.append('\n');
                         */
                        // sb.append("CREATED:20060509T192258Z\n");
                        // sb.append("LAST-MODIFIED:20060509T192258Z\n");
                        // sb.append("DTSTAMP:20060510T014840Z\n");
                        // sb.append("ORGANIZER;CN=David
                        // Huynh:MAILTO:dfhuynh@gmail.com\n");
                        // sb.append("STATUS:CONFIRMED\n");
                        // sb.append("TRANSP:OPAQUE\n");
                    }
                }
            } finally {
                if (c != null) c.close();
            }
            
            vcContext.put("events", events);
            msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected String toICalDate(String s) {
        if (s != null) {
            Date d = Utilities.parseDate(s);
            if (d != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
                s = sdf.format(d);
            }
        }
        return s;
    }

    protected void setContentType(String format, HttpServletResponse response) {
        if ("RDFXML".equals(format)) {
            response.setContentType("application/rdf+xml");
        } else if ("GMap".equals(format)) {
            response.setContentType("application/xml");
        } else if ("N3".equals(format)) {
            response.setContentType("application/n3");
        } else {
            response.setContentType("text/plain");
        }
    }
}
