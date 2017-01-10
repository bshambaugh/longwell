package edu.mit.simile.longwell.query.bucket;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.DoubleProjector.DoubleProjection;

public class DoubleBucketer extends BucketerBase {

    final static protected int s_mode_exact = 0;
    final static protected int s_mode_range = 1;
    final static protected int s_mode_lessThan = 2;
    final static protected int s_mode_lessThanOrEqual = 3;
    final static protected int s_mode_greaterThan = 4;
    final static protected int s_mode_greaterThanOrEqual = 5;

    final static protected String[] s_modePrefixes = new String[] { "==", "<>", "<<", "<=", ">>", ">=" };

    final static private Logger s_logger = Logger.getLogger(DoubleBucketer.class);

    public DoubleBucketer(Profile profile) {
        super(profile);
    }

    public boolean matchesOneValue(String parameter) {
        String[] params = splitParameter(parameter);
        if (params.length > 1) {
            return false;
        }
        return params[0].startsWith(s_modePrefixes[s_mode_exact]);
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
        DoubleProjection dProjection = (DoubleProjection) projection;
        double[] params = parseParameter(parameter);

        Set objects2 = null;
        if ((int) params[0] < s_modePrefixes.length) {
            switch ((int) params[0]) {
                case s_mode_exact:
                    objects2 = dProjection.getObjects(params[1], params[1] + Double.MIN_VALUE);
                    break;
                case s_mode_range:
                    objects2 = dProjection.getObjects(params[1], params[2]);
                    break;
                case s_mode_lessThan:
                    objects2 = dProjection.getObjects(Double.MIN_VALUE, params[1]);
                    break;
                case s_mode_lessThanOrEqual:
                    objects2 = dProjection.getObjects(Double.MIN_VALUE, params[1] + Double.MIN_VALUE);
                    break;
                case s_mode_greaterThan:
                    objects2 = dProjection.getObjects(params[1] + Double.MIN_VALUE, Double.MAX_VALUE);
                    break;
                case s_mode_greaterThanOrEqual:
                    objects2 = dProjection.getObjects(params[1], Double.MAX_VALUE);
                    break;
            }
        }
        if (objects2 != null) {
            builder.addAll(objects2);
        }
    }

    protected List internalSuggestNarrowingBuckets(IProjection projection, float desirable) throws QueryEvaluationException, RepositoryException {
        List<BucketTheme> bucketThemes = new ArrayList<BucketTheme>();

        Set values = projection.getValues();

        // Only 1 value, nothing to refine by
        if (desirable < 1 && values.size() < 2) {
            return bucketThemes;
        }

        // Few values, just refine by distinct values
        if (values.size() < 20) {
            suggestDistinctValues(projection, values, bucketThemes, null, "");
        } else {
            suggestRanges(projection, values, bucketThemes, null, "");
        }

        return bucketThemes;
    }

    static protected class DoubleBucket extends Bucket {
        final double m_value;

        public DoubleBucket(String bucketerName, String bucketerParameter, String label, int count, double value) {
            super(bucketerName, bucketerParameter, label, count);
            m_value = value;
        }
    }

    static protected class DoubleBucketNarrowerComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            DoubleBucket b1 = (DoubleBucket) o1;
            DoubleBucket b2 = (DoubleBucket) o2;

            return (int) Math.ceil(b1.m_value - b2.m_value);
        }

    }

    protected void suggestDistinctValues(IProjection projection, Set values, List<BucketTheme> bucketThemes, String[] excepts,
            String concatWith) throws QueryEvaluationException, RepositoryException {

        Set<DoubleBucket> buckets = new TreeSet<DoubleBucket>(new DoubleBucketNarrowerComparator());

        Iterator i = values.iterator();
        outer: while (i.hasNext()) {
            Object o = i.next();

            if (o instanceof Double) {
                Double d = (Double) o;

                String label = d.toString();
                String parameter = s_modePrefixes[s_mode_exact] + label;

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            continue outer;
                        }
                    }
                }

                buckets.add(new DoubleBucket(this.getClass().getName(), concatWith + encodeParameter(parameter), label,
                        projection.countObjects(o), d.doubleValue()));
            }
        }

        bucketThemes.add(new BucketTheme("", "distinct-values", new ArrayList(buckets)));
    }

    protected void suggestRanges(IProjection projection, Set values, List<BucketTheme> bucketThemes, String[] excepts,
            String concatWith) throws QueryEvaluationException, RepositoryException {
        DoubleProjection dProjection = (DoubleProjection) projection;
        Set<DoubleBucket> buckets = new TreeSet<DoubleBucket>(new DoubleBucketNarrowerComparator());
        double min = dProjection.getMin();
        double max = dProjection.getMax();

        double diff = max - min;
        double interval = 10;

        while (diff / interval > 10) {
            interval *= 10;
        }
        while (diff / interval < 2) {
            interval /= 10;
        }

        String format = getResources(projection.getLocale()).getString("RangeFormat");

        min = Math.floor(min / interval) * interval;
        max = Math.ceil(max / interval) * interval;

        int intervalCount = (int) Math.ceil((max - min) / interval);
        outer: for (int j = 0; j < intervalCount; j++) {
            double from = (min + j * interval);
            double to = (min + j * interval + interval);
            int count = dProjection.countObjects(from, to);

            if (count > 0) {
                String label = MessageFormat.format(format, new Object[] { new Double(from), new Double(to) });

                String parameter = s_modePrefixes[s_mode_range] + from + "," + to;

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            continue outer;
                        }
                    }
                }

                buckets.add(new DoubleBucket(this.getClass().getName(), concatWith + encodeParameter(parameter), label,
                        count, from));
            }
        }

        bucketThemes.add(new BucketTheme("", "distinct-values", new ArrayList(buckets)));
    }

    protected BroadeningResult internalSuggestBroadeningBuckets(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {
        String locale = projection.getLocale();
        String[] params = splitParameter(parameter);

        int mode = s_mode_exact;

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

            double[] doubles = parseParameter(params[i]);
            if ((int) doubles[0] < s_modePrefixes.length) {
                mode = (int) doubles[0];
                existingBuckets.add(new Bucket(this.getClass().getName(), param, paramToLabel(doubles, locale), 0));
            }
        }

        List<BucketTheme> bucketThemes = new ArrayList<BucketTheme>();

        DoubleProjection dProjection = (DoubleProjection) projection;
        Set values = dProjection.getValues();

        if (mode == s_mode_range) {
            suggestRanges(dProjection, values, bucketThemes, params, parameter + ",");
        } else {
            suggestDistinctValues(dProjection, values, bucketThemes, params, parameter + ",");
        }

        return new BroadeningResult(existingBuckets, bucketThemes);
    }

    protected String individualParameterToDescription(String parameter, String locale) {
        return paramToLabel(parseParameter(parameter), locale);
    }

    protected double[] parseParameter(String parameter) {
        int mode = s_mode_exact;
        double[] results = null;

        if (parameter != null) {
            while (mode < s_modePrefixes.length) {
                if (parameter.startsWith(s_modePrefixes[mode])) {
                    parameter = parameter.substring(s_modePrefixes[mode].length());
                    break;
                }
                mode++;
            }

            String[] doubles = StringUtils.splitPreserveAllTokens(parameter, ',');

            results = new double[Math.max(2, doubles.length + 1)];
            results[0] = mode;

            for (int i = 0; i < doubles.length; i++) {
                try {
                    results[i + 1] = Double.parseDouble(doubles[i]);
                } catch (NumberFormatException e) {
                    s_logger.error(e);
                }
            }
        }

        if (results == null) {
            results = new double[2];
            results[0] = mode;
        }

        return results;
    }

    protected String paramToLabel(double[] doubles, String locale) {
        ResourceBundle resources = getResources(locale);
        String label = "";

        switch ((int) doubles[0]) {
            case s_mode_exact:
                label = MessageFormat.format(resources.getString("ExactTermFormat"), new Object[] { new Double(
                        doubles[1]) });
                break;
            case s_mode_range:
                label = MessageFormat.format(resources.getString("RangeTermFormat"), new Object[] {
                        new Double(doubles[1]), new Double(doubles[2]) });
                break;
            case s_mode_lessThan:
                label = MessageFormat.format(resources.getString("LessThanTermFormat"), new Object[] { new Double(
                        doubles[1]) });
                break;
            case s_mode_lessThanOrEqual:
                label = MessageFormat.format(resources.getString("LessThanOrEqualTermFormat"),
                        new Object[] { new Double(doubles[1]) });
                break;
            case s_mode_greaterThan:
                label = MessageFormat.format(resources.getString("GreaterThanTermFormat"), new Object[] { new Double(
                        doubles[1]) });
                break;
            case s_mode_greaterThanOrEqual:
                label = MessageFormat.format(resources.getString("GreaterThanOrEqualTermFormat"),
                        new Object[] { new Double(doubles[1]) });
                break;
        }

        return label;
    }
}
