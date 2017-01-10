package edu.mit.simile.longwell.query.bucket;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Cache;
import edu.mit.simile.longwell.CacheFactory;
import edu.mit.simile.longwell.FixedSetBuilder;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.DateTimeProjector.DateTimeProjection;

public class DateTimeBucketer extends BucketerBase {

    final static private Logger s_logger = Logger.getLogger(DateTimeBucketer.class);

    final static protected int s_mode_byHour = 0;
    final static protected int s_mode_byDay = 1;
    final static protected int s_mode_byWeek = 2;
    final static protected int s_mode_byMonth = 3;
    final static protected int s_mode_byYear = 4;
    final static protected int s_mode_byDecade = 5;
    final static protected int s_mode_byCentury = 6;
    final static protected int s_mode_after = 7;
    final static protected int s_mode_before = 8;
    final static protected int s_mode_relative = 9;

    final static protected String[] s_modePrefixes = new String[] { "byHour:", "byDay:", "byWeek:", "byMonth:",
            "byYear:", "byDecade:", "byCentury:", "byAfter:", "byBefore:", "byRelative:" };

    final static protected int s_relativeMode_today = 0;
    final static protected int s_relativeMode_thisWeek = 1;
    final static protected int s_relativeMode_thisMonth = 2;
    final static protected int s_relativeMode_thisYear = 3;
    final static protected int s_relativeMode_withinAWeek = 4;
    final static protected int s_relativeMode_withinAMonth = 5;
    final static protected int s_relativeMode_within3Months = 6;
    final static protected int s_relativeMode_withinHalfAYear = 7;
    final static protected int s_relativeMode_withinAYear = 8;
    final static protected int s_relativeMode_yesterday = 9;
    final static protected int s_relativeMode_lastWeek = 10;
    final static protected int s_relativeMode_lastMonth = 11;
    final static protected int s_relativeMode_lastYear = 12;
    final static protected int s_relativeMode_before1Week = 13;
    final static protected int s_relativeMode_before1Month = 14;
    final static protected int s_relativeMode_before3Months = 15;
    final static protected int s_relativeMode_beforeHalfAYear = 16;
    final static protected int s_relativeMode_beforeAYear = 17;
    final static protected int s_relativeMode_tomorrow = 18;
    final static protected int s_relativeMode_nextWeek = 19;
    final static protected int s_relativeMode_nextMonth = 20;
    final static protected int s_relativeMode_nextYear = 21;
    final static protected int s_relativeMode_max = 22;

    final static protected String[] s_relativeModePrefixes = new String[] { "Today", "ThisWeek", "ThisMonth",
            "ThisYear", "WithinAWeek", "WithinAMonth", "Within3Months", "WithinHalfAYear", "WithinAYear",

            "Yesterday", "LastWeek", "LastMonth", "LastYear", "BeforeAWeek", "BeforeAMonth", "Before3Months",
            "BeforeHalfAYear", "BeforeAYear",

            "Tomorrow", "NextWeek", "NextMonth", "NextYear" };

    final protected Cache m_dateRangeParamToProjectionToResult;
    final protected Cache m_fixedNarrowingSuggestionCache;

    final protected Date[] m_relativeStartDates = new Date[s_relativeMode_max];
    final protected Date[] m_relativeEndDates = new Date[s_relativeMode_max];
    final protected Cache[] m_relativeCaches = new Cache[s_relativeMode_max];

    public DateTimeBucketer(Profile profile) {
        super(profile);

        CacheFactory f = profile.getCacheFactory();
        m_dateRangeParamToProjectionToResult = f.getCache("date-range-param", "projection", false);
        m_fixedNarrowingSuggestionCache = f.getCache("fixed-narrowing-suggestion", false);

        for (int i = 0; i < m_relativeCaches.length; i++) {
            m_relativeCaches[i] = f.getCache("relative-cache", true);
        }
    }

    public boolean matchesOneValue(String parameter) {
        return false;
    }

    public Set getBucket(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {
        Set oldObjects = (Set) m_projectionToParameterToBucket.get(projection, parameter);
        Set newObjects = internalGetBucket(projection, parameter);

        if (newObjects.equals(oldObjects)) {
            return oldObjects;
        }

        m_projectionToParameterToBucket.put(projection, parameter, newObjects);
        return newObjects;
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
        DateTimeProjection dtProjection = (DateTimeProjection) projection;
        Set newObjects = null;

        int mode = parameterToMode(parameter);
        if (mode == s_modePrefixes.length) {
            return;
        }

        String arguments = parameter.substring(s_modePrefixes[mode].length());
        if (mode != s_mode_relative) {
            newObjects = (Set) m_dateRangeParamToProjectionToResult.get(arguments, projection);
            if (newObjects == null) {
                String[] dateStrings = StringUtils.splitPreserveAllTokens(arguments, ',');

                Date start = dateStrings[0].equals("null") ? null : new Date(Long.parseLong(dateStrings[0]));
                Date end = dateStrings[1].equals("null") ? null : new Date(Long.parseLong(dateStrings[1]));

                newObjects = dtProjection.getObjects(start, end);

                m_dateRangeParamToProjectionToResult.put(arguments, projection, newObjects);
            }
        } else {
            int relativeMode = parameterToRelativeMode(arguments);
            if (relativeMode == s_relativeModePrefixes.length) {
                return;
            }

            Date[] dates = relativeModeToDates(relativeMode);
            if (datesEqual(dates[0], m_relativeStartDates[relativeMode])
                    && datesEqual(dates[1], m_relativeEndDates[relativeMode])) {

                newObjects = (Set) m_relativeCaches[relativeMode].get(projection);
            } else {
                m_relativeStartDates[relativeMode] = dates[0];
                m_relativeEndDates[relativeMode] = dates[1];
                m_relativeCaches[relativeMode].clear();
            }

            if (newObjects == null) {
                newObjects = dtProjection.getObjects(dates[0], dates[1]);
                m_relativeCaches[relativeMode].put(projection, newObjects);
            }
        }

        if (newObjects != null) {
            builder.addAll(newObjects);
        }
    }

    final static protected long s_day = 24 * 60 * 60 * 1000;
    final static protected long s_week = s_day * 7;
    final static protected long s_month = s_day * 30;
    final static protected long s_year = s_day * 365;
    final static protected long s_decade = s_year * 10;
    final static protected long s_century = s_year * 100;

    public List suggestNarrowingBuckets(IProjection projection, float desirable) throws QueryEvaluationException, RepositoryException {
        return internalSuggestNarrowingBuckets(projection, desirable);
    }

    protected List internalSuggestNarrowingBuckets(IProjection projection, float desirable) throws QueryEvaluationException, RepositoryException {
        DateTimeProjection dtProjection = (DateTimeProjection) projection;
        List<BucketTheme> bucketThemes = new ArrayList<BucketTheme>();

        List<BucketTheme> fixedNarrowingSuggestions = (List) m_fixedNarrowingSuggestionCache.get(projection);

        if (fixedNarrowingSuggestions == null || desirable == 1) {
            fixedNarrowingSuggestions = internalSuggestBuckets(dtProjection, desirable, null, "");
            m_fixedNarrowingSuggestionCache.put(projection, fixedNarrowingSuggestions);
        }

        bucketThemes.addAll(fixedNarrowingSuggestions);

        suggestRelative(dtProjection, desirable, bucketThemes, null, "");

        return bucketThemes;
    }

    protected List<BucketTheme> internalSuggestBuckets(DateTimeProjection projection, float desirable, String[] excepts,
            String concatWith) throws QueryEvaluationException, RepositoryException {

        List<BucketTheme> bucketThemes = new ArrayList<BucketTheme>();

        Date earliest = projection.getEarliest();
        Date latest = projection.getLatest();
        long diff = latest.getTime() - earliest.getTime();

        if (diff > s_century * 5) {
            suggestNarrowingByCentury(projection, desirable, bucketThemes, excepts, concatWith);
        } else if (diff > s_decade * 5) {
            suggestNarrowingByDecade(projection, desirable, bucketThemes, excepts, concatWith);
        } else if (diff > s_year * 5) {
            suggestNarrowingByYear(projection, desirable, bucketThemes, excepts, concatWith);
        } else if (diff > s_month * 3) {
            suggestNarrowingByMonth(projection, desirable, bucketThemes, excepts, concatWith);
        } else if (diff > s_week * 3) {
            suggestNarrowingByWeek(projection, desirable, bucketThemes, excepts, concatWith);
        } else if (diff > s_day) {
            suggestNarrowingByDay(projection, desirable, bucketThemes, excepts, concatWith);
        } else {
            suggestNarrowingByHour(projection, desirable, bucketThemes, excepts, concatWith);
        }

        return bucketThemes;
    }

    protected void suggestNarrowingByCentury(DateTimeProjection projection, float desirable, List<BucketTheme> bucketThemes,
            String[] excepts, String concatWith) throws QueryEvaluationException, RepositoryException {

        int totalCount = projection.countObjects();

        DateFormat df = new SimpleDateFormat("yyyy");

        Calendar earliest = Calendar.getInstance();
        Calendar latest = Calendar.getInstance();

        earliest.setTime(projection.getEarliest());
        latest.setTime(projection.getLatest());

        int year = earliest.get(Calendar.YEAR);

        Calendar earliestYear = Calendar.getInstance();
        earliestYear.clear();
        earliestYear.set(Calendar.YEAR, year);
        earliestYear.add(Calendar.YEAR, -(year % 100));

        List<DateBucket> buckets = new ArrayList<DateBucket>();

        outer: while (earliest.before(latest)) {
            Date start = earliestYear.getTime();
            earliestYear.add(Calendar.YEAR, 100);
            Date end = earliestYear.getTime();

            int count = projection.countObjects(start, end);
            if (count > 0 && count < totalCount) {
                String parameter = s_modePrefixes[s_mode_byCentury] + start.getTime() + "," + end.getTime();

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            continue outer;
                        }
                    }
                }

                buckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter), format(
                        df, start), count, start));
            }
        }

        if (buckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("ByCentury"),
                    "distinct-values", buckets));
        }
    }

    protected void suggestNarrowingByDecade(DateTimeProjection projection, float desirable, List<BucketTheme> bucketThemes,
            String[] excepts, String concatWith) throws QueryEvaluationException, RepositoryException {

        int totalCount = projection.countObjects();

        DateFormat df = new SimpleDateFormat("yyyy");

        Calendar earliest = Calendar.getInstance();
        Calendar latest = Calendar.getInstance();

        earliest.setTime(projection.getEarliest());
        latest.setTime(projection.getLatest());

        int year = earliest.get(Calendar.YEAR);

        Calendar earliestYear = Calendar.getInstance();
        earliestYear.clear();
        earliestYear.set(Calendar.YEAR, year);
        earliestYear.add(Calendar.YEAR, -(year % 10));

        List<DateBucket> buckets = new ArrayList<DateBucket>();

        outer: while (earliestYear.before(latest)) {
            Date start = earliestYear.getTime();
            earliestYear.add(Calendar.YEAR, 10);
            Date end = earliestYear.getTime();

            int count = projection.countObjects(start, end);
            if (count > 0 && count < totalCount) {
                String parameter = s_modePrefixes[s_mode_byDecade] + start.getTime() + "," + end.getTime();

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            continue outer;
                        }
                    }
                }

                buckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter), format(
                        df, start), count, start));
            }
        }

        if (buckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("ByDecade"),
                    "distinct-values", buckets));
        }
    }

    protected void suggestNarrowingByYear(DateTimeProjection projection, float desirable, List<BucketTheme> bucketThemes,
            String[] excepts, String concatWith) throws QueryEvaluationException, RepositoryException {

        int totalCount = projection.countObjects();

        DateFormat df = new SimpleDateFormat("yyyy");

        Calendar earliest = Calendar.getInstance();
        Calendar latest = Calendar.getInstance();

        earliest.setTime(projection.getEarliest());
        latest.setTime(projection.getLatest());

        Calendar earliestYear = Calendar.getInstance();
        earliestYear.clear();
        earliestYear.set(Calendar.YEAR, earliest.get(Calendar.YEAR));

        List<DateBucket> buckets = new ArrayList<DateBucket>();

        outer: while (earliestYear.before(latest)) {
            Date start = earliestYear.getTime();
            earliestYear.add(Calendar.YEAR, 1);
            Date end = earliestYear.getTime();

            int count = projection.countObjects(start, end);
            if (count > 0 && count < totalCount) {
                String parameter = s_modePrefixes[s_mode_byYear] + start.getTime() + "," + end.getTime();

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            continue outer;
                        }
                    }
                }

                buckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter), format(
                        df, start), count, start));
            }
        }

        if (buckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("ByYear"),
                    "distinct-values", buckets));
        }
    }

    protected void suggestNarrowingByMonth(DateTimeProjection projection, float desirable, List<BucketTheme> bucketThemes,
            String[] excepts, String concatWith) throws QueryEvaluationException, RepositoryException {
        DateFormat df = new SimpleDateFormat("MMMM yyyy");

        int totalCount = projection.countObjects();

        Calendar earliest = Calendar.getInstance();
        Calendar latest = Calendar.getInstance();

        earliest.setTime(projection.getEarliest());
        latest.setTime(projection.getLatest());

        Calendar earliestMonth = Calendar.getInstance();
        earliestMonth.clear();
        earliestMonth.set(Calendar.YEAR, earliest.get(Calendar.YEAR));
        earliestMonth.set(Calendar.MONTH, earliest.get(Calendar.MONTH));

        List<DateBucket> buckets = new ArrayList<DateBucket>();

        outer: while (earliestMonth.before(latest)) {
            Date start = (Date) earliestMonth.getTime().clone();
            earliestMonth.add(Calendar.MONTH, 1);
            Date end = earliestMonth.getTime();

            int count = projection.countObjects(start, end);
            if (count > 0 && count < totalCount) {
                String parameter = s_modePrefixes[s_mode_byMonth] + start.getTime() + "," + end.getTime();

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            continue outer;
                        }
                    }
                }

                buckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter), format(
                        df, start), count, start));
            }
        }

        if (buckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("ByMonth"),
                    "distinct-values", buckets));
        }
    }

    protected void suggestNarrowingByWeek(DateTimeProjection projection, float desirable, List<BucketTheme> bucketThemes,
            String[] excepts, String concatWith) throws QueryEvaluationException, RepositoryException {

        String weekOfFormat = getResources(projection.getLocale()).getString("WeekOfFormat");

        int totalCount = projection.countObjects();

        Calendar earliest = Calendar.getInstance();
        Calendar latest = Calendar.getInstance();

        earliest.setTime(projection.getEarliest());
        latest.setTime(projection.getLatest());

        Calendar earliestWeek = (Calendar) earliest.clone();
        clearThingsInDay(earliestWeek);
        earliestWeek.clear(Calendar.DAY_OF_WEEK);
        earliestWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        List<DateBucket> buckets = new ArrayList<DateBucket>();

        outer: while (earliestWeek.before(latest)) {
            Date start = (Date) earliestWeek.getTime().clone();
            earliestWeek.add(Calendar.DAY_OF_MONTH, 7);
            Date end = earliestWeek.getTime();

            int count = projection.countObjects(start, end);
            if (count > 0 && count < totalCount) {
                String parameter = s_modePrefixes[s_mode_byWeek] + start.getTime() + "," + end.getTime();

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            continue outer;
                        }
                    }
                }

                buckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter), format(
                        weekOfFormat, new Object[] { start }), count, start));
            }
        }

        if (buckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("ByWeek"),
                    "distinct-values", buckets));
        }
    }

    protected void suggestNarrowingByDay(DateTimeProjection projection, float desirable, List<BucketTheme> bucketThemes,
            String[] excepts, String concatWith) throws QueryEvaluationException, RepositoryException {

        String dayOfFormat = getResources(projection.getLocale()).getString("DayOfFormat");

        int totalCount = projection.countObjects();

        Calendar earliest = Calendar.getInstance();
        Calendar latest = Calendar.getInstance();

        earliest.setTime(projection.getEarliest());
        latest.setTime(projection.getLatest());

        Calendar earliestDay = (Calendar) earliest.clone();
        clearThingsInDay(earliestDay);
        earliestDay.set(Calendar.HOUR_OF_DAY, 0);

        List<DateBucket> buckets = new ArrayList<DateBucket>();

        outer: while (earliestDay.before(latest)) {
            Date start = (Date) earliestDay.getTime().clone();
            earliestDay.add(Calendar.DATE, 1);
            Date end = earliestDay.getTime();

            int count = projection.countObjects(start, end);
            if (count > 0 && count < totalCount) {
                String parameter = s_modePrefixes[s_mode_byDay] + start.getTime() + "," + end.getTime();

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            continue outer;
                        }
                    }
                }

                buckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter), format(
                        dayOfFormat, new Object[] { start }), count, start));
            }
        }

        if (buckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("ByDay"),
                    "distinct-values", buckets));
        }
    }

    protected void suggestNarrowingByHour(DateTimeProjection projection, float desirable, List<BucketTheme> bucketThemes,
            String[] excepts, String concatWith) throws QueryEvaluationException, RepositoryException {

        String hourOfFormat = getResources(projection.getLocale()).getString("HourOfFormat");
        String afterFormat = getResources(projection.getLocale()).getString("AfterFormat");
        String beforeFormat = getResources(projection.getLocale()).getString("BeforeFormat");

        int totalCount = projection.countObjects();

        Calendar earliest = Calendar.getInstance();
        Calendar latest = Calendar.getInstance();

        earliest.setTime(projection.getEarliest());
        latest.setTime(projection.getLatest());

        earliest.clear(Calendar.MILLISECOND);
        earliest.clear(Calendar.SECOND);
        earliest.clear(Calendar.MINUTE);

        List<DateBucket> hourBuckets = new ArrayList<DateBucket>();
        List<DateBucket> afterBuckets = new ArrayList<DateBucket>();
        List<DateBucket> beforeBuckets = new ArrayList<DateBucket>();

        while (earliest.before(latest)) {
            Date start = (Date) earliest.getTime().clone();
            earliest.add(Calendar.HOUR, 1);
            Date end = earliest.getTime();

            int count = projection.countObjects(start, end);
            byHour: if (count > 0 && count < totalCount) {
                String parameter = s_modePrefixes[s_mode_byHour] + start.getTime() + "," + end.getTime();

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            break byHour;
                        }
                    }
                }

                hourBuckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter),
                        format(hourOfFormat, new Object[] { start }), count, start));
            }

            count = projection.countObjects(start, null);
            after: if (count > 0 && count < totalCount) {
                String parameter = s_modePrefixes[s_mode_after] + start.getTime() + ",null";

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            break after;
                        }
                    }
                }

                afterBuckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter),
                        format(afterFormat, new Object[] { start }), count, start));
            }

            count = projection.countObjects(null, start);
            before: if (count > 0 && count < totalCount) {
                String parameter = s_modePrefixes[s_mode_before] + "null," + start.getTime();

                if (excepts != null) {
                    for (int n = 0; n < excepts.length; n++) {
                        if (parameter.equals(excepts[n])) {
                            break before;
                        }
                    }
                }

                beforeBuckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter),
                        format(beforeFormat, new Object[] { start }), count, start));
            }
        }

        if (hourBuckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("ByHour"),
                    "distinct-values", hourBuckets));
        }
        if (afterBuckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("After"),
                    "distinct-values", afterBuckets));
        }
        if (beforeBuckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("Before"),
                    "distinct-values", beforeBuckets));
        }
    }

    protected void suggestRelative(DateTimeProjection projection, float desirable, List<BucketTheme> bucketThemes,
            String[] excepts, String concatWith) throws QueryEvaluationException, RepositoryException {

        Date start, end;

        Calendar c = null;
        List<DateBucket> buckets = null;

        // Present
        buckets = new ArrayList<DateBucket>();

        c = Calendar.getInstance();
        clearThingsInDay(c);
        start = (Date) c.getTime().clone();
        c.add(Calendar.DATE, 1);
        end = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_today, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.clear(Calendar.DAY_OF_WEEK);
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        start = (Date) c.getTime().clone();
        c.add(Calendar.WEEK_OF_YEAR, 1);
        end = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_thisWeek, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.clear(Calendar.DAY_OF_MONTH);
        c.set(Calendar.DAY_OF_MONTH, 1);
        start = (Date) c.getTime().clone();
        c.add(Calendar.MONTH, 1);
        end = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_thisMonth, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.clear(Calendar.DAY_OF_YEAR);
        c.set(Calendar.DAY_OF_YEAR, 1);
        start = (Date) c.getTime().clone();
        c.add(Calendar.YEAR, 1);
        end = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_thisYear, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        end = (Date) c.getTime().clone();
        clearThingsInDay(c);
        c.add(Calendar.DATE, -7);
        start = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_withinAWeek, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        end = (Date) c.getTime().clone();
        clearThingsInDay(c);
        c.add(Calendar.MONTH, -1);
        start = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_withinAMonth, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        end = (Date) c.getTime().clone();
        clearThingsInDay(c);
        c.add(Calendar.MONTH, -3);
        start = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_within3Months, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        end = (Date) c.getTime().clone();
        clearThingsInDay(c);
        c.add(Calendar.MONTH, -6);
        start = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_withinHalfAYear, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        end = (Date) c.getTime().clone();
        clearThingsInDay(c);
        c.add(Calendar.YEAR, -1);
        start = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_withinAYear, buckets, excepts, concatWith);

        if (buckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("Present"),
                    "distinct-values", buckets));
        }

        // Past
        buckets = new ArrayList<DateBucket>();

        c = Calendar.getInstance();
        clearThingsInDay(c);
        end = (Date) c.getTime().clone();
        c.add(Calendar.DATE, -1);
        start = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_yesterday, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.clear(Calendar.DAY_OF_WEEK);
        end = (Date) c.getTime().clone();
        c.add(Calendar.WEEK_OF_YEAR, -1);
        start = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_lastWeek, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.clear(Calendar.DAY_OF_MONTH);
        end = (Date) c.getTime().clone();
        c.add(Calendar.MONTH, -1);
        start = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_lastMonth, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.clear(Calendar.DAY_OF_YEAR);
        end = (Date) c.getTime().clone();
        c.add(Calendar.YEAR, -1);
        start = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_lastYear, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.add(Calendar.WEEK_OF_YEAR, -1);
        end = c.getTime();
        start = null;
        addRelativeBucket(projection, start, end, s_relativeMode_before1Week, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.add(Calendar.MONTH, -1);
        end = c.getTime();
        start = null;
        addRelativeBucket(projection, start, end, s_relativeMode_before1Month, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.add(Calendar.MONTH, -3);
        end = c.getTime();
        start = null;
        addRelativeBucket(projection, start, end, s_relativeMode_before3Months, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.add(Calendar.MONTH, -6);
        end = c.getTime();
        start = null;
        addRelativeBucket(projection, start, end, s_relativeMode_beforeHalfAYear, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.add(Calendar.YEAR, -1);
        end = c.getTime();
        start = null;
        addRelativeBucket(projection, start, end, s_relativeMode_beforeAYear, buckets, excepts, concatWith);

        if (buckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("Past"), "distinct-values",
                    buckets));
        }

        // Future
        buckets = new ArrayList<DateBucket>();

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.add(Calendar.DATE, 1);
        start = (Date) c.getTime().clone();
        c.add(Calendar.DATE, 1);
        end = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_tomorrow, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.clear(Calendar.DAY_OF_WEEK);
        c.add(Calendar.WEEK_OF_YEAR, 1);
        start = (Date) c.getTime().clone();
        c.add(Calendar.WEEK_OF_YEAR, 1);
        end = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_nextWeek, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.clear(Calendar.DAY_OF_MONTH);
        c.add(Calendar.MONTH, 1);
        start = (Date) c.getTime().clone();
        c.add(Calendar.MONTH, 1);
        end = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_nextMonth, buckets, excepts, concatWith);

        c = Calendar.getInstance();
        clearThingsInDay(c);
        c.clear(Calendar.DAY_OF_YEAR);
        c.add(Calendar.YEAR, 1);
        start = (Date) c.getTime().clone();
        c.add(Calendar.YEAR, 1);
        end = c.getTime();
        addRelativeBucket(projection, start, end, s_relativeMode_nextYear, buckets, excepts, concatWith);

        if (buckets.size() > 0) {
            bucketThemes.add(new BucketTheme(getResources(projection.getLocale()).getString("Future"),
                    "distinct-values", buckets));
        }
    }

    protected void addRelativeBucket(DateTimeProjection projection, Date start, Date end, int relativeMode,
            List<DateBucket> buckets, String[] excepts, String concatWith) throws QueryEvaluationException, RepositoryException {

        int totalCount = projection.countObjects();

        ResourceBundle resources = getResources(projection.getLocale());

        int count = projection.countObjects(start, end);
        if (count > 0 && count < totalCount) {
            String key = s_relativeModePrefixes[relativeMode];
            String parameter = s_modePrefixes[s_mode_relative] + key;

            if (excepts != null) {
                for (int n = 0; n < excepts.length; n++) {
                    if (parameter.equals(excepts[n])) {
                        return;
                    }
                }
            }

            buckets.add(new DateBucket(this.getClass().getName(), concatWith + encodeParameter(parameter), resources
                    .getString(key), count, start));
        }
    }

    protected BroadeningResult internalSuggestBroadeningBuckets(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {

        ResourceBundle resources = getResources(projection.getLocale());
        List<Bucket> existingBuckets = new ArrayList<Bucket>();

        String[] params = splitParameter(parameter);
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

            int mode = parameterToMode(params[i]);
            if (mode < s_modePrefixes.length) {
                String label = modeToLabel(mode, params[i], resources);

                existingBuckets.add(new Bucket(this.getClass().getName(), param, label, 0));
            }
        }

        DateTimeProjection dtProjection = (DateTimeProjection) projection;

        List<BucketTheme> bucketThemes = internalSuggestBuckets(dtProjection, 1, params, parameter + ",");

        suggestRelative(dtProjection, 1, bucketThemes, params, parameter + ",");

        return new BroadeningResult(existingBuckets, bucketThemes);
    }

    protected String individualParameterToDescription(String parameter, String locale) {

        ResourceBundle resources = getResources(locale);
        int mode = parameterToMode(parameter);

        if (mode < s_modePrefixes.length) {
            return modeToLabel(mode, parameter, resources);
        }

        return "";
    }

    protected int parameterToMode(String parameter) {
        int mode;
        for (mode = 0; mode < s_modePrefixes.length; mode++) {
            if (parameter.startsWith(s_modePrefixes[mode])) {
                break;
            }
        }
        return mode;
    }

    protected int parameterToRelativeMode(String parameter) {
        int relativeMode;
        for (relativeMode = 0; relativeMode < s_relativeModePrefixes.length; relativeMode++) {
            if (parameter.equals(s_relativeModePrefixes[relativeMode])) {
                break;
            }
        }
        return relativeMode;
    }

    protected String modeToLabel(int mode, String param, ResourceBundle resources) {
        String arguments = param.substring(s_modePrefixes[mode].length());
        String label = "";

        if (mode == s_mode_relative) {
            int relativeMode = parameterToRelativeMode(arguments);
            label = resources.getString(s_relativeModePrefixes[relativeMode]);
        } else {
            String[] dateStrings = StringUtils.splitPreserveAllTokens(arguments, ',');
            Date startDate = null;
            Date endDate = null;

            // TODO(SM): we should use locale + timezone info here instead
            Calendar start = Calendar.getInstance();

            try {
                start.setTime(new Date(Long.parseLong(dateStrings[0])));

                startDate = start.getTime();
            } catch (Exception e) {
                // ignore
            }
            try {
                Calendar end = Calendar.getInstance();
                end.setTime(new Date(Long.parseLong(dateStrings[1])));

                endDate = end.getTime();
            } catch (Exception e) {
                // ignore
            }

            switch (mode) {
                case s_mode_byCentury:
                    label = format(resources.getString("ByCenturyFormat"), new Object[] { Integer.toString(start
                            .get(Calendar.YEAR)) });
                    break;
                case s_mode_byDecade:
                    label = format(resources.getString("ByDecadeFormat"), new Object[] { Integer.toString(start
                            .get(Calendar.YEAR)) });
                    break;
                case s_mode_byYear:
                    label = format(resources.getString("ByYearFormat"), new Object[] { Integer.toString(start
                            .get(Calendar.YEAR)) });
                    break;
                case s_mode_byMonth:
                    label = format(resources.getString("ByMonthFormat"), new Object[] { startDate });
                    break;
                case s_mode_byWeek:
                    label = format(resources.getString("ByWeekFormat"), new Object[] { startDate });
                    break;
                case s_mode_byDay:
                    label = format(resources.getString("ByDayFormat"), new Object[] { startDate });
                    break;
                case s_mode_byHour:
                    label = format(resources.getString("ByHourFormat"), new Object[] { startDate });
                    break;
                case s_mode_after:
                    label = format(resources.getString("AfterFormat"), new Object[] { startDate });
                    break;
                case s_mode_before:
                    label = format(resources.getString("BeforeFormat"), new Object[] { endDate });
                    break;
            }
        }

        return label;
    }

    protected Date[] relativeModeToDates(int relativeMode) {
        Date start = null;
        Date end = null;

        Calendar now = Calendar.getInstance();
        clearThingsInDay(now);

        switch (relativeMode) {
            case s_relativeMode_today:
                start = (Date) now.getTime().clone();
                now.add(Calendar.DATE, 1);
                end = now.getTime();
                break;
            case s_relativeMode_thisWeek:
                now.clear(Calendar.DAY_OF_WEEK);
                now.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                start = (Date) now.getTime().clone();
                now.add(Calendar.WEEK_OF_YEAR, 1);
                end = now.getTime();
                break;
            case s_relativeMode_thisMonth:
                now.clear(Calendar.DAY_OF_MONTH);
                now.set(Calendar.DAY_OF_MONTH, 1);
                start = (Date) now.getTime().clone();
                now.add(Calendar.MONTH, 1);
                end = now.getTime();
                break;
            case s_relativeMode_thisYear:
                now.clear(Calendar.MONTH);
                now.set(Calendar.DAY_OF_YEAR, 1);
                start = (Date) now.getTime().clone();
                now.add(Calendar.YEAR, 1);
                end = now.getTime();
                break;

            case s_relativeMode_withinAWeek:
                end = (Date) now.getTime().clone();
                now.add(Calendar.DATE, -7);
                start = now.getTime();
                break;
            case s_relativeMode_withinAMonth:
                end = (Date) now.getTime().clone();
                now.add(Calendar.MONTH, -1);
                start = now.getTime();
                break;
            case s_relativeMode_within3Months:
                end = (Date) now.getTime().clone();
                now.add(Calendar.MONTH, -3);
                start = now.getTime();
                break;
            case s_relativeMode_withinHalfAYear:
                end = (Date) now.getTime().clone();
                now.add(Calendar.MONTH, -6);
                start = now.getTime();
                break;
            case s_relativeMode_withinAYear:
                end = (Date) now.getTime().clone();
                now.add(Calendar.YEAR, -1);
                start = now.getTime();
                break;

            case s_relativeMode_yesterday:
                end = (Date) now.getTime().clone();
                now.add(Calendar.DATE, -1);
                start = now.getTime();
                break;
            case s_relativeMode_lastWeek:
                now.clear(Calendar.DAY_OF_WEEK);
                end = (Date) now.getTime().clone();
                now.add(Calendar.WEEK_OF_YEAR, -1);
                start = now.getTime();
                break;
            case s_relativeMode_lastMonth:
                now.clear(Calendar.DAY_OF_MONTH);
                end = (Date) now.getTime().clone();
                now.add(Calendar.MONTH, -1);
                start = now.getTime();
                break;
            case s_relativeMode_lastYear:
                now.clear(Calendar.DAY_OF_YEAR);
                end = (Date) now.getTime().clone();
                now.add(Calendar.YEAR, -1);
                start = now.getTime();
                break;

            case s_relativeMode_before1Week:
                now.add(Calendar.WEEK_OF_YEAR, -1);
                end = now.getTime();
                break;
            case s_relativeMode_before1Month:
                now.add(Calendar.MONTH, -1);
                end = now.getTime();
                break;
            case s_relativeMode_before3Months:
                now.add(Calendar.MONTH, -3);
                end = now.getTime();
                break;
            case s_relativeMode_beforeHalfAYear:
                now.add(Calendar.MONTH, -6);
                end = now.getTime();
                break;
            case s_relativeMode_beforeAYear:
                now.add(Calendar.YEAR, -1);
                end = now.getTime();
                break;

            case s_relativeMode_tomorrow:
                now.add(Calendar.DATE, 1);
                start = now.getTime();
                break;
            case s_relativeMode_nextWeek:
                now.clear(Calendar.DAY_OF_WEEK);
                now.add(Calendar.WEEK_OF_YEAR, 1);
                start = now.getTime();
                break;
            case s_relativeMode_nextMonth:
                now.clear(Calendar.DAY_OF_MONTH);
                now.add(Calendar.MONTH, 1);
                start = now.getTime();
                break;
            case s_relativeMode_nextYear:
                now.clear(Calendar.DAY_OF_YEAR);
                now.add(Calendar.YEAR, 1);
                start = now.getTime();
                break;
        }

        return new Date[] { start, end };
    }

    protected boolean datesEqual(Date d1, Date d2) {
        if (d1 == null) {
            return d2 == null;
        }
        return d1.equals(d2);
    }

    protected void clearThingsInDay(Calendar c) {
        c.clear(Calendar.HOUR_OF_DAY);
        c.clear(Calendar.MINUTE);
        c.clear(Calendar.SECOND);
        c.clear(Calendar.MILLISECOND);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    protected String format(String format, Object[] objects) {
        try {
            return MessageFormat.format(format, objects);
        } catch (Exception e) {
            String s = "Error formatting with pattern " + format;
            for (int i = 0; i < objects.length; i++) {
                s += "\n\t" + objects[i];
            }
            s_logger.debug(s, e);
            return "ERROR";
        }
    }

    protected String format(DateFormat df, Date d) {
        try {
            return df.format(d);
        } catch (Exception e) {
            s_logger.debug("Error formatting date " + d + " with formatter " + df, e);
            return "ERROR";
        }
    }
}

class DateBucket extends Bucket {
    final Date m_date;

    public DateBucket(String bucketerName, String bucketerParameter, String label, int count, Date date) {
        super(bucketerName, bucketerParameter, label, count);
        m_date = date;
    }
}

class DateBucketComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        DateBucket d1 = (DateBucket) o1;
        DateBucket d2 = (DateBucket) o2;

        return d1.m_date.compareTo(d2.m_date);
    }
}
