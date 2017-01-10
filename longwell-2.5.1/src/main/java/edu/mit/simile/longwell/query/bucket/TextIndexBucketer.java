package edu.mit.simile.longwell.query.bucket;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.query.project.IProjection;
import edu.mit.simile.longwell.query.project.TextIndexProjector.TextIndexProjection;

public class TextIndexBucketer extends BucketerBase {

    public TextIndexBucketer(Profile profile) {
        super(profile);
    }

    protected Set internalGetBucket(IProjection projection, String parameter) {
        TextIndexProjection tiProjection = (TextIndexProjection) projection;

        return tiProjection.search(parameter);
    }

    protected List internalSuggestNarrowingBuckets(IProjection projection, float desirable) {
        return new ArrayList();
    }

    protected BroadeningResult internalSuggestBroadeningBuckets(IProjection projection, String parameter) throws QueryEvaluationException, RepositoryException {

        List<Bucket> existingBuckets = new ArrayList<Bucket>();

        existingBuckets.add(new Bucket(this.getClass().getName(), parameter, parameterToLabel(parameter, projection
                .getLocale()), 0));

        return new BroadeningResult(existingBuckets, new ArrayList());
    }

    protected String individualParameterToDescription(String parameter, String locale) {

        return parameterToLabel(parameter, locale);
    }

    protected String parameterToLabel(String parameter, String locale) {
        ResourceBundle resources = ResourceBundle.getBundle(TextIndexBucketer.class.getName());

        return MessageFormat.format(resources.getString("ParameterLabelFormat"), new Object[] { parameter });
    }
}
