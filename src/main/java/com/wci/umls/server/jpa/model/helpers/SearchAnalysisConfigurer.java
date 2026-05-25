/*
 *    Copyright 2024 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

/**
 * Programmatic analyzer configuration for Hibernate Search 7.x (Lucene
 * backend).
 *
 * <pre>
 * hibernate.search.backend.analysis.configurer=class:com.wci.umls.server.jpa.model.helpers.SearchAnalysisConfigurer
 * </pre>
 */
public class SearchAnalysisConfigurer implements LuceneAnalysisConfigurer {

  /* see superclass */
  @Override
  public void configure(LuceneAnalysisConfigurationContext context) {

    // "noStopWord" analyzer - standard tokenizer, lowercase, no stop words
    context.analyzer("noStopWord").custom()
        .tokenizer(StandardTokenizerFactory.class)
        .tokenFilter(LowerCaseFilterFactory.class);

    // "whitespace" analyzer - standard tokenizer, lowercase
    context.analyzer("whitespace").custom()
        .tokenizer(StandardTokenizerFactory.class)
        .tokenFilter(LowerCaseFilterFactory.class);

    // "autocompleteEdgeAnalyzer" - keyword tokenizer with edge n-gram
    context.analyzer("autocompleteEdgeAnalyzer").custom()
        .tokenizer(KeywordTokenizerFactory.class)
        .tokenFilter(PatternReplaceFilterFactory.class)
            .param("pattern", "([^a-zA-Z0-9\\.])")
            .param("replacement", " ")
            .param("replace", "all")
        .tokenFilter(LowerCaseFilterFactory.class)
        .tokenFilter(StopFilterFactory.class)
        .tokenFilter(EdgeNGramFilterFactory.class)
            .param("minGramSize", "3")
            .param("maxGramSize", "50");

    // "autocompleteNGramAnalyzer" - standard tokenizer with n-gram
    context.analyzer("autocompleteNGramAnalyzer").custom()
        .tokenizer(StandardTokenizerFactory.class)
        .tokenFilter(WordDelimiterFilterFactory.class)
        .tokenFilter(LowerCaseFilterFactory.class)
        .tokenFilter(NGramFilterFactory.class)
            .param("minGramSize", "3")
            .param("maxGramSize", "5")
        .tokenFilter(PatternReplaceFilterFactory.class)
            .param("pattern", "([^a-zA-Z0-9\\.])")
            .param("replacement", " ")
            .param("replace", "all");
  }
}