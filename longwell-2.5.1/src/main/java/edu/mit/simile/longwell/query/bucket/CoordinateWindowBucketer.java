package edu.mit.simile.longwell.query.bucket;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.openrdf.model.Literal;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.CacheFactory;
import edu.mit.simile.longwell.CoordinateWindow;
import edu.mit.simile.longwell.Coordinates;
import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.CoordinateWindowProjector.CoordinateWindowProjection;
import edu.mit.simile.longwell.schema.SchemaModel;

public class CoordinateWindowBucketer extends BucketerBase {
	final protected Cache m_coordinateWindowParamToProjectionToResult;
	
	public CoordinateWindowBucketer(Profile profile) {
		super(profile);
		
		CacheFactory f = profile.getCacheFactory();
		m_coordinateWindowParamToProjectionToResult = f.getCache("coordinate-window-param", "projection", false);
	}
	
	protected Set internalGetBucket(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {
		String[] params = splitParameter(parameter);
		FixedSetBuilder builder = new FixedSetBuilder();
		
		for (int i = 0; i < params.length; i++) {
			internalGetOneBucket(projection, params[i], builder);
		}
		return builder.buildFixedSet();
	}
	
	protected void internalGetOneBucket(IProjection projection, String parameter, FixedSetBuilder builder) throws QueryEvaluationException, RepositoryException {
		CoordinateWindowProjection cwProjection = (CoordinateWindowProjection) projection;
		Set newObjects = (Set) m_coordinateWindowParamToProjectionToResult.get(parameter, projection);
		if (null == newObjects) {
			CoordinateWindow cw = parameterToCoordinateWindow(parameter);
			newObjects = cwProjection.getObjects(cw);
			m_coordinateWindowParamToProjectionToResult.put(parameter, projection, newObjects);
		}
		if (newObjects != null) {
			builder.addAll(newObjects);
		}
	}
	
	protected CoordinateWindow parameterToCoordinateWindow(String parameter) {
		String[] points = new String[2];
		points[0] = parameter.substring(0, parameter.indexOf("|"));
		points[1] = parameter.substring(parameter.indexOf("|") + 1);
		String[] sw = points[0].split("~");
		String[] ne = points[1].split("~");
		return new CoordinateWindow(new Coordinates(Double.parseDouble(sw[1]), Double.parseDouble(sw[0])),
				new Coordinates(Double.parseDouble(ne[1]), Double.parseDouble(ne[0])));
	}
	
	// cannot be used in conventional manner for narrowing...
	protected List internalSuggestNarrowingBuckets(IProjection projection, float desirable) {
		List bucketThemes = new ArrayList();
		return bucketThemes;
	}
	
	// ...or broadening
	protected BroadeningResult internalSuggestBroadeningBuckets(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {
		SchemaModel schemaModel = m_profile.getSchemaModel();
		String locale = projection.getLocale();
		String[] params = splitParameter(parameter);
		
		List<Bucket> existingBuckets = new ArrayList<Bucket>();
		for (int i = 0; i < params.length; i++) {
			String param = "";
			for (int j = 0; j < params.length; j++) {
				if (i != j) {
					if (param.length() == 0) {
						param += encodeParameter(params[j]);
					} else {
						param += "," + encodeParameter(params[j]);
					}
				}
			}
			
			existingBuckets.add(new Bucket(this.getClass().getName(), param, valueToLabel(params[i],
					schemaModel, locale), 0));
		}
		List bucketThemes = new ArrayList();
		return new BroadeningResult(existingBuckets, bucketThemes);
	}
	
	protected String individualParameterToDescription(String parameter, String locale) {
		return valueToLabel(parameter, m_profile.getSchemaModel(), locale);
	}
	
	protected String valueToLabel(Object object, SchemaModel schemaModel, String locale) {
		ResourceBundle resources = getResources(locale);
		
		String literalBucketLabelFormat = resources.getString("LiteralBucketLabelFormat");
		String emptyBucketLabel = resources.getString("EmptyBucketLabel");
		String s = null, label;
		if (object instanceof Literal) {
			s = ((Literal) object).getLabel();
		} else if (object instanceof String) {
			s = (String) object;
		}
		
		if (s != null) {
			CoordinateWindow c = parameterToCoordinateWindow(s);
			s = c.toString();
		}
		
		label = (s == null) ?
				emptyBucketLabel
				: MessageFormat.format(literalBucketLabelFormat, new Object[] { s });
		
		return label;
	}
}


class CoordinateWindowBucket extends Bucket {
	final CoordinateWindow m_window;
	
	public CoordinateWindowBucket(String bucketerName, String bucketerParameter, String label, int count, CoordinateWindow window) {
		super(bucketerName, bucketerParameter, label, count);
		m_window = window;
	}
}

class CoordinateFrameBucketComparator implements Comparator {
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object o1, Object o2) {
		CoordinateWindowBucket cf1 = (CoordinateWindowBucket) o1;
		CoordinateWindowBucket cf2 = (CoordinateWindowBucket) o2;
		
		return cf1.m_window.compareTo(cf2.m_window);
	}
}
