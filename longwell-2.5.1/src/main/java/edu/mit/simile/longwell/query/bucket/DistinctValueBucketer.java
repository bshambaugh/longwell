package edu.mit.simile.longwell.query.bucket;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.schema.SchemaModel;

public class DistinctValueBucketer extends BucketerBase {

    public DistinctValueBucketer(Profile profile) {
        super(profile);
    }

    protected Set internalGetBucket(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {
        String[] params = splitParameter(parameter);
        FixedSetBuilder builder = new FixedSetBuilder();

        for (int i = 0; i < params.length; i++) {
            builder.addAll(projection.getObjects(parameterToNode(params[i])));
        }
        return builder.buildFixedSet();
    }

    protected List internalSuggestNarrowingBuckets(IProjection projection, float desirable) throws QueryEvaluationException, RepositoryException {
        SchemaModel schemaModel = m_profile.getSchemaModel();
        String locale = projection.getLocale();
        List<BucketTheme> bucketThemes = new ArrayList<BucketTheme>();

        if (desirable < 1
                && (projection.getUniqueness() * desirable > 0.5f || projection.countObjects((Object) null) > projection
                        .countObjects() / 3)) {
            return bucketThemes;
        }

        Map m = projection.getValueToObjectsMap();

        Set values = m.keySet();
        if (desirable < 1 && values.size() == 1) {
            return bucketThemes;
        }

        int totalCount = projection.countObjects();
        float valuesToObjectsRatio = values.size() / (float) totalCount;

        if (desirable == 1 || valuesToObjectsRatio < 0.5f * desirable) { 
            // TODO (SM) should we make the facet uniqueness threshold configurable?
            Set<Bucket> buckets = new TreeSet<Bucket>(new Bucket.BucketComparator());

            Iterator i = values.iterator();
            while (i.hasNext()) {
                Object value = i.next();

                int count = ((Set) m.get(value)).size();
                if (count > 0 && count < totalCount) {
                    buckets.add(new Bucket(this.getClass().getName(), encodeParameter(valueToParameter(value,
                            schemaModel, locale)), valueToLabel(value, schemaModel, locale), count));
                }
            }

            if (buckets.size() > 0) {
                bucketThemes.add(new BucketTheme("", "distinct-values", new ArrayList(buckets)));
            }
        }

        return bucketThemes;
    }

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

            existingBuckets.add(new Bucket(this.getClass().getName(), param, valueToLabel(parameterToNode(params[i]),
                    schemaModel, locale), 0));
        }

        List<BucketTheme> bucketThemes = new ArrayList<BucketTheme>();
        {
            Set<Bucket> buckets = new TreeSet<Bucket>(new Bucket.BucketComparator());
            Set values = projection.getValues();

            Iterator i = values.iterator();
            outer: while (i.hasNext()) {
                Object value = i.next();
                String param = valueToParameter(value, schemaModel, locale);

                for (int j = 0; j < params.length; j++) {
                    if (param.equals(params[j])) {
                        continue outer;
                    }
                }

                buckets.add(new Bucket(this.getClass().getName(), parameter + "," + encodeParameter(param),
                        valueToLabel(value, schemaModel, locale), 0));
            }

            if (buckets.size() > 0) {
                bucketThemes.add(new BucketTheme("", "distinct-values", new ArrayList(buckets)));
            }
        }

        return new BroadeningResult(existingBuckets, bucketThemes);
    }

    protected String individualParameterToDescription(String parameter, String locale) throws RepositoryException {

        return valueToLabel(parameterToNode(parameter), m_profile.getSchemaModel(), locale);
    }

    protected String valueToParameter(Object object, SchemaModel schemaModel, String locale) {
        if (object instanceof Literal) {
            String s = ((Literal) object).getLabel();

            return "l" + s;
        } else if (object instanceof String) {
            return "l" + (String) object;
        } else if (object instanceof URI) {
            return "r" + ((URI) object).toString();
        } else {
            return "null";
        }
    }

    protected String valueToLabel(Object object, SchemaModel schemaModel, String locale) throws RepositoryException {
        ResourceBundle resources = getResources(locale);

        String literalBucketLabelFormat = resources.getString("LiteralBucketLabelFormat");
        String resourceBucketLabelFormat = resources.getString("ResourceBucketLabelFormat");
        String emptyBucketLabel = resources.getString("EmptyBucketLabel");

        if (object instanceof Literal) {
            String s = ((Literal) object).getLabel();

            return MessageFormat.format(literalBucketLabelFormat, new Object[] { s });
        } else if (object instanceof URI) {
            return MessageFormat.format(resourceBucketLabelFormat, new Object[] { schemaModel.getLabel((URI) object, locale) });
        } else if (object instanceof String) {
            return (String) object;
        } else {
            return emptyBucketLabel;
        }
    }

    protected Value parameterToNode(String s) {
        Value node = null;
        if (s.startsWith("l")) {
            node = new LiteralImpl(s.substring(1));
        } else if (s.startsWith("r")) {
            node = new URIImpl(s.substring(1));
        }
        return node;
    }
}
