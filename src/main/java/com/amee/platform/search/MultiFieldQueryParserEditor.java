package com.amee.platform.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;

import java.beans.PropertyEditorSupport;
import java.util.Map;

public class MultiFieldQueryParserEditor extends PropertyEditorSupport {

    private String[] fields;
    private Analyzer analyzer;
    private Map<String, Float> boosts;

    public MultiFieldQueryParserEditor(String[] fields) {
        super();
        setFields(fields);
        setAnalyzer(SearchService.STANDARD_ANALYZER);
    }

    public MultiFieldQueryParserEditor(String[] fields, Analyzer analyzer) {
        super();
        setFields(fields);
        setAnalyzer(analyzer);
    }

    public MultiFieldQueryParserEditor(String[] fields, Analyzer analyzer, Map<String, Float> boosts) {
        super();
        setFields(fields);
        setAnalyzer(analyzer);
        setBoosts(boosts);
    }

    public MultiFieldQueryParserEditor(String[] fields, Map<String, Float> boosts) {
        super();
        setFields(fields);
        setBoosts(boosts);
        setAnalyzer(SearchService.STANDARD_ANALYZER);
    }

    @Override
    public void setAsText(String text) {
        if (text != null) {
            try {
                QueryParser parser = new MultiFieldQueryParser(
                        Version.LUCENE_30, getFields(), getAnalyzer(), getBoosts());
                setValue(parser.parse(text));
            } catch (ParseException e) {
                throw new IllegalArgumentException("Cannot parse query (" + e.getMessage() + ").", e);
            }
        } else {
            setValue(null);
        }
    }

    public String[] getFields() {
        return fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Map<String, Float> getBoosts() {
        return boosts;
    }

    public void setBoosts(Map<String, Float> boosts) {
        this.boosts = boosts;
    }
}