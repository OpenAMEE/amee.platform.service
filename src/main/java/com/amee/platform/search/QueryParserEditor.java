package com.amee.platform.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;

import java.beans.PropertyEditorSupport;

public class QueryParserEditor extends PropertyEditorSupport {

    private String field;
    private Analyzer analyzer;
    private boolean doubleValue;

    public QueryParserEditor(String field) {
        setField(field);
        setAnalyzer(SearchService.STANDARD_ANALYZER);
        setDoubleValue(false);
    }

    public QueryParserEditor(String field, Analyzer analyzer) {
        setField(field);
        setAnalyzer(analyzer);
        setDoubleValue(false);
    }

    public QueryParserEditor(String field, Analyzer analyzer, boolean doubleValue) {
        setField(field);
        setAnalyzer(analyzer);
        setDoubleValue(doubleValue);
    }

    /**
     * A factory method to return a tag specific implementation of QueryParserEditor. Uses SearchService.TAG_ANALYZER
     * and a custom  implementation of getModifiedText to replace commas with spaces (to avoid confusing Lucene).
     *
     * @param field
     * @return
     */
    public static QueryParserEditor getTagQueryParserEditor(String field) {
        return new QueryParserEditor(field, SearchService.TAG_ANALYZER) {
            public String getModifiedText(String text) {
                return text.replace(',', ' ');
            }
        };
    }

    @Override
    public void setAsText(String text) {
        if (text != null) {
            try {
                QueryParser parser = getQueryParser();
                setValue(parser.parse(getModifiedText(text)));
            } catch (ParseException e) {
                throw new IllegalArgumentException("Cannot parse query (" + e.getMessage() + ").", e);
            }
        } else {
            setValue(null);
        }
    }

    private QueryParser getQueryParser() {
        return new QueryParser(Version.LUCENE_30, getField(), getAnalyzer()) {

            // TODO: is getField meant to resolve to this.getField or super.getField?
            // Added super as this is how java was resolving the unqualified method call.
            // See: http://findbugs.sourceforge.net/bugDescriptions.html#IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD
            protected Query newTermQuery(Term term) {
                if (isDoubleValue()) {
                    try {
                        return new TermQuery(new Term(super.getField(),
                                NumericUtils.doubleToPrefixCoded(Double.parseDouble(term.text()))));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Cannot parse query (" + e.getMessage() + ").", e);
                    }
                } else {
                    return super.newTermQuery(term);
                }
            }

            // TODO: is getField meant to resolve to this.getField or super.getField?
            // Added super as this is how java was resolving the unqualified method call.
            // See: http://findbugs.sourceforge.net/bugDescriptions.html#IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD
            protected Query newRangeQuery(String field, String part1, String part2, boolean inclusive) {
                if (isDoubleValue()) {
                    try {
                        final NumericRangeQuery query =
                                NumericRangeQuery.newDoubleRange(
                                        super.getField(),
                                        Double.parseDouble(part1),
                                        Double.parseDouble(part2),
                                        inclusive,
                                        inclusive);
                        query.setRewriteMethod(super.getMultiTermRewriteMethod());
                        return query;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Cannot parse query (" + e.getMessage() + ").", e);
                    }
                } else {
                    return super.newRangeQuery(field, part1, part2, inclusive);
                }
            }
        };
    }

    /**
     * Extension point to allow the text to be modified before it is parsed by the QueryParser. The default
     * implementation simply returns the text unmodified.
     *
     * @param text to be modified
     * @return text following modification.
     */
    public String getModifiedText(String text) {
        return text;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public boolean isDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(boolean doubleValue) {
        this.doubleValue = doubleValue;
    }
}