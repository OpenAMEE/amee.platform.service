package com.amee.platform.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;

import java.beans.PropertyEditorSupport;

public class QueryParserEditor extends PropertyEditorSupport {

    private String field;
    private Analyzer analyzer;

    public QueryParserEditor(String field) {
        setField(field);
        setAnalyzer(SearchService.STANDARD_ANALYZER);
    }

    public QueryParserEditor(String field, Analyzer analyzer) {
        setField(field);
        setAnalyzer(analyzer);
    }

    @Override
    public void setAsText(String text) {
        if (text != null) {
            try {
                QueryParser parser = new QueryParser(Version.LUCENE_30, getField(), getAnalyzer());
                setValue(parser.parse(text));
            } catch (ParseException e) {
                throw new IllegalArgumentException("Cannot parse query (" + e.getMessage() + ").", e);
            }
        } else {
            setValue(null);
        }
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
}