/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services.helper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.PfsParameter;

/**
 * Performs utility functions relating to Lucene indexes and Hibernate Search.
 */
public class IndexUtility {

  /**
   * Simple wrapper for search results, replacing the old FullTextQuery return
   * type. Contains total count and result list with [score, entity] pairs.
   */
  public static class FullTextQueryResult {

    /** The total count. */
    private final long totalCount;

    /** The results as [Float score, Object entity] arrays. */
    private final List<Object[]> results;

    /**
     * Instantiates a {@link FullTextQueryResult}.
     *
     * @param totalCount the total count
     * @param results the results
     */
    public FullTextQueryResult(long totalCount, List<Object[]> results) {
      this.totalCount = totalCount;
      this.results = results;
    }

    /**
     * Returns the result size (total count across all pages).
     *
     * @return the result size
     */
    public int getResultSize() {
      return (int) totalCount;
    }

    /**
     * Returns the result list. Each element is Object[] with [Float score,
     * Object entity].
     *
     * @return the result list
     */
    public List<Object[]> getResultList() {
      return results;
    }
  }

  /** The sort field analyzed map. */
  public static Map<String, Map<String, Boolean>> sortFieldAnalyzedMap = new HashMap<>();

  /** The string field names map. */
  private static Map<Class<?>, Set<String>> stringFieldNames = new HashMap<>();

  /** The field names map. */
  private static Map<Class<?>, Set<String>> allFieldNames = new HashMap<>();

  /** The all fields map. */
  private static Map<Class<?>, java.lang.reflect.Field[]> allFields = new HashMap<>();

  /** The all column methods. */
  private static Map<Class<?>, List<Method>> allColumnGetMethods = new HashMap<>();

  /** The all column set methods. */
  private static Map<Class<?>, List<Method>> allColumnSetMethods = new HashMap<>();

  /** The all @OneToMany methods. */
  private static Map<Class<?>, List<Method>> allOneToManyGetMethods = new HashMap<>();

  // Initialize the field names maps
  static {
    try {
      final Map<String, Class<?>> reindexMap = new HashMap<>();
      final String indexProp = ConfigUtility.getConfigProperties().getProperty("index.packages");
      final String[] packages = indexProp != null ? indexProp.split(";") : new String[] {
          "com.wci.umls.server"
      };
      final Reflections reflections =
          new Reflections(new ConfigurationBuilder().forPackages(packages));
      for (final Class<?> clazz : reflections.getTypesAnnotatedWith(Indexed.class)) {
        reindexMap.put(clazz.getSimpleName(), clazz);
      }
      final Class<?>[] classes = reindexMap.values().toArray(new Class<?>[0]);

      for (final Class<?> clazz : classes) {
        stringFieldNames.put(clazz, IndexUtility.getIndexedFieldNames(clazz, true));
        allFieldNames.put(clazz, IndexUtility.getIndexedFieldNames(clazz, false));
      }
    } catch (Exception e) {
      e.printStackTrace();
      stringFieldNames = null;
    }
  }

  /**
   * Returns the indexed field names for a given class.
   *
   * @param clazz the clazz
   * @param stringOnly the string only flag
   * @return the indexed field names
   * @throws Exception the exception
   */
  public static Set<String> getIndexedFieldNames(Class<?> clazz, boolean stringOnly)
    throws Exception {

    // If already initialized, return computed values
    if (stringOnly && stringFieldNames != null && stringFieldNames.containsKey(clazz)) {
      return stringFieldNames.get(clazz);
    }
    if (!stringOnly && allFieldNames.containsKey(clazz)) {
      return allFieldNames.get(clazz);
    }

    // Avoid ngram and sort fields (these have special uses)
    Set<String> exclusions = new HashSet<>();
    exclusions.add("nGram");
    exclusions.add("NGram");

    // When looking for default fields, exclude definitions and branches
    Set<String> stringExclusions = new HashSet<>();
    stringExclusions.add("definitions");
    stringExclusions.add("branch");

    final Set<String> fieldNames = new HashSet<>();

    // first cycle over all methods
    for (final Method m : clazz.getMethods()) {

      // if no annotations, skip
      if (m.getAnnotations().length == 0) {
        continue;
      }

      // check for @IndexedEmbedded on methods
      if (m.isAnnotationPresent(IndexedEmbedded.class)) {
        throw new Exception("Unable to handle @IndexedEmbedded on methods, specify on field");
      }

      // for non-embedded fields, only process strings (unless has a bridge)
      if (stringOnly && !m.getReturnType().equals(String.class)) {
        continue;
      }

      // check for search field annotations and collect name/analyzed pairs
      if (doesMethodHaveFieldAnnotation(m)) {
        final Map<String, Boolean> nameAnalyzedPairs = new HashMap<>();
        for (final Object annotationField : getMethodAnnotations(m)) {
          final String fieldName = getFieldNameFromMethod(m, annotationField);
          fieldNames.add(fieldName);
          nameAnalyzedPairs.put(getAnnotationFieldName(annotationField),
              isFieldAnalyzed(annotationField));
        }
        // Cache the analyzed pairs for sort field lookup
        String methodFieldName = m.getName().substring(3);
        methodFieldName = methodFieldName.substring(0, 1).toLowerCase() + methodFieldName.substring(1);
        final String key = clazz.getName() + "." + methodFieldName;
        sortFieldAnalyzedMap.put(key, nameAnalyzedPairs);
      }
    }

    // second cycle over all fields
    for (final java.lang.reflect.Field f : getAllFields(clazz)) {
      // check for @IndexedEmbedded
      if (f.isAnnotationPresent(IndexedEmbedded.class)) {

        // Assumes field is a collection, and has a OneToMany, ManyToMany, or
        // ManyToOne annotation
        Class<?> jpaType = null;
        if (f.isAnnotationPresent(OneToMany.class)) {
          jpaType = f.getAnnotation(OneToMany.class).targetEntity();
        } else if (f.isAnnotationPresent(ManyToMany.class)) {
          jpaType = f.getAnnotation(ManyToMany.class).targetEntity();
        } else if (f.isAnnotationPresent(ManyToOne.class)) {
          jpaType = f.getAnnotation(ManyToOne.class).targetEntity();
        } else if (f.isAnnotationPresent(OneToOne.class)) {
          jpaType = f.getAnnotation(OneToOne.class).targetEntity();
        } else {
          throw new Exception("Unable to determine jpa type, @IndexedEmbedded must be used with "
              + "@OneToOne, @OneToMany, @ManyToOne, or @ManyToMany ");

        }

        for (final String embeddedField : getIndexedFieldNames(jpaType, stringOnly)) {
          fieldNames.add(f.getName() + "." + embeddedField);
        }
      }

      // for non-embedded fields, only process strings
      if (stringOnly && !f.getType().equals(String.class))
        continue;

      // check for search field annotations and collect name/analyzed pairs
      if (doesFieldHaveFieldAnnotation(f)) {
        final Map<String, Boolean> nameAnalyzedPairs = new HashMap<>();
        for (final Object annotationField : getFieldAnnotations(f)) {
          final String fieldName = getFieldNameFromField(f, annotationField);
          fieldNames.add(fieldName);
          nameAnalyzedPairs.put(getAnnotationFieldName(annotationField),
              isFieldAnalyzed(annotationField));
        }
        final String key = clazz.getName() + "." + f.getName();
        sortFieldAnalyzedMap.put(key, nameAnalyzedPairs);
      }

    }

    // Apply filters
    Set<String> filteredFieldNames = new HashSet<>();
    OUTER: for (final String fieldName : fieldNames) {
      for (final String exclusion : exclusions) {
        if (fieldName.contains(exclusion)) {
          continue OUTER;
        }
      }
      for (final String exclusion : stringExclusions) {
        if (stringOnly && fieldName.contains(exclusion)) {
          continue OUTER;
        }
      }
      filteredFieldNames.add(fieldName);
    }

    return filteredFieldNames;
  }

  /**
   * Returns whether a method has a Hibernate Search field annotation.
   *
   * @param method the method
   * @return true if annotated
   */
  private static boolean doesMethodHaveFieldAnnotation(final Method method) {
    return (method.isAnnotationPresent(FullTextField.class)
        || method.isAnnotationPresent(GenericField.class)
        || method.isAnnotationPresent(KeywordField.class));
  }

  /**
   * Returns whether a field has a Hibernate Search field annotation.
   *
   * @param field the field
   * @return true if annotated
   */
  private static boolean doesFieldHaveFieldAnnotation(final java.lang.reflect.Field field) {
    return (field.isAnnotationPresent(FullTextField.class)
        || field.isAnnotationPresent(GenericField.class)
        || field.isAnnotationPresent(KeywordField.class));
  }

  /**
   * Returns all search field annotations from a method.
   *
   * @param method the method
   * @return set of annotation objects
   */
  private static Set<Object> getMethodAnnotations(final Method method) {
    final Set<Object> annotations = new HashSet<>();
    if (method.isAnnotationPresent(FullTextField.class)) {
      annotations.add(method.getAnnotation(FullTextField.class));
    }
    if (method.isAnnotationPresent(KeywordField.class)) {
      annotations.add(method.getAnnotation(KeywordField.class));
    }
    if (method.isAnnotationPresent(GenericField.class)) {
      annotations.add(method.getAnnotation(GenericField.class));
    }
    return annotations;
  }

  /**
   * Returns all search field annotations from a field.
   *
   * @param field the field
   * @return set of annotation objects
   */
  private static Set<Object> getFieldAnnotations(final java.lang.reflect.Field field) {
    final Set<Object> annotations = new HashSet<>();
    if (field.isAnnotationPresent(FullTextField.class)) {
      annotations.add(field.getAnnotation(FullTextField.class));
    }
    if (field.isAnnotationPresent(KeywordField.class)) {
      annotations.add(field.getAnnotation(KeywordField.class));
    }
    if (field.isAnnotationPresent(GenericField.class)) {
      annotations.add(field.getAnnotation(GenericField.class));
    }
    return annotations;
  }

  /**
   * Returns the name from a search field annotation.
   *
   * @param annotationField the annotation (FullTextField, KeywordField, or
   *          GenericField)
   * @return the field name
   */
  private static String getAnnotationFieldName(final Object annotationField) {
    if (annotationField instanceof FullTextField) {
      return ((FullTextField) annotationField).name();
    } else if (annotationField instanceof KeywordField) {
      return ((KeywordField) annotationField).name();
    } else if (annotationField instanceof GenericField) {
      return ((GenericField) annotationField).name();
    }
    return "";
  }

  /**
   * Returns whether a search field annotation represents an analyzed field.
   * FullTextField = analyzed, KeywordField/GenericField = not analyzed.
   *
   * @param annotationField the annotation
   * @return true if analyzed
   */
  private static boolean isFieldAnalyzed(final Object annotationField) {
    return (annotationField instanceof FullTextField);
  }

  /**
   * Helper function to get a field name from a method and annotation.
   *
   * @param m the reflected, annotated method, assumed to be of form
   *          getFieldName()
   * @param annotationField the annotation field (FullTextField, KeywordField,
   *          GenericField, or null)
   * @return the indexed field name
   */
  public static String getFieldNameFromMethod(Method m, Object annotationField) {
    // If annotation has a specified name, use that
    if (annotationField != null) {
      final String annotationName = getAnnotationFieldName(annotationField);
      if (annotationName != null && !annotationName.isEmpty()) {
        return annotationName;
      }
    }

    // otherwise, assume method name of form getFieldName
    // where the desired value is fieldName
    if (m.getName().startsWith("get")) {
      return StringUtils.uncapitalize(m.getName().substring(3));
    } else if (m.getName().startsWith("is")) {
      return StringUtils.uncapitalize(m.getName().substring(2));
    } else if (m.getName().startsWith("set")) {
      return StringUtils.uncapitalize(m.getName().substring(3));
    } else
      return m.getName();

  }

  /**
   * Helper function get a field name from reflected Field and annotation.
   *
   * @param annotatedField the reflected, annotated field
   * @param annotationField the field annotation (FullTextField, KeywordField,
   *          GenericField, or null)
   * @return the indexed field name
   */
  private static String getFieldNameFromField(java.lang.reflect.Field annotatedField,
    Object annotationField) {
    if (annotationField != null) {
      final String annotationName = getAnnotationFieldName(annotationField);
      if (annotationName != null && !annotationName.isEmpty()) {
        return annotationName;
      }
    }

    return annotatedField.getName();
  }

  /**
   * Returns the all fields.
   *
   * @param type the type
   * @return the all fields
   */
  public static java.lang.reflect.Field[] getAllFields(Class<?> type) {

    // If already initialized, return computed values
    if (allFields.containsKey(type)) {
      return allFields.get(type);
    }

    if (type.getSuperclass() != null) {
      java.lang.reflect.Field[] allFieldsArray =
          ArrayUtils.addAll(getAllFields(type.getSuperclass()), type.getDeclaredFields());
      allFields.put(type, allFieldsArray);
      return allFieldsArray;
    }
    java.lang.reflect.Field[] allFieldsArray = type.getDeclaredFields();
    allFields.put(type, allFieldsArray);
    return allFieldsArray;
  }

  /**
   * Returns the getXXX methods for @Column annotated fields.
   *
   * @param clazz the clazz
   * @return the all methods
   * @throws Exception the exception
   */
  public static List<Method> getAllColumnGetMethods(Class<?> clazz) throws Exception {

    // If already initialized, return computed values
    if (allColumnGetMethods.containsKey(clazz)) {
      return allColumnGetMethods.get(clazz);
    }

    final List<Method> allClassMethods = new ArrayList<Method>();

    // exclude fields that can't be modified directly from the UI
    final Set<String> excludedFields = new HashSet<>();
    excludedFields.add("id");
    excludedFields.add("timestamp");
    excludedFields.add("lastModified");
    excludedFields.add("lastModifiedBy");
    excludedFields.add("lastApproved");
    excludedFields.add("lastApprovedBy");
    excludedFields.add("terminology");
    excludedFields.add("branch");
    excludedFields.add("branchedTo");

    for (final java.lang.reflect.Field field : getAllFields(clazz)) {

      if (excludedFields.contains(field.getName())) {
        continue;
      }
      if (!field.isAnnotationPresent(Column.class)) {
        continue;
      }

      // Try get first - find a getXXX method that takes no parameters
      final String accessorName1 =
          "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
      Method getMethod;
      try {
        getMethod = clazz.getMethod(accessorName1, new Class<?>[] {});
      } catch (Exception e) {
        getMethod = null;
      }
      try {
        getMethod = clazz.getMethod(accessorName1, new Class<?>[] {});
      } catch (Exception e) {
        getMethod = null;
      }

      if (getMethod != null) {
        allClassMethods.add(getMethod);
        continue;
      }
      // Otherwise, use is - find an isXXX method that takes no parameters
      final String accessorName2 =
          "is" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
      Method isMethod;
      try {
        isMethod = clazz.getMethod(accessorName2, new Class<?>[] {});
      } catch (Exception e) {
        isMethod = null;
      }

      if (isMethod != null) {
        allClassMethods.add(isMethod);
        continue;
      }
    }

    // Cache for later runs
    allColumnGetMethods.put(clazz, allClassMethods);
    return allClassMethods;

  }

  /**
   * Returns the setXXX methods for @Column annotated fields.
   *
   * @param clazz the clazz
   * @return the all methods
   * @throws Exception the exception
   */
  public static List<Method> getAllColumnSetMethods(Class<?> clazz) throws Exception {

    // If already initialized, return computed values
    if (allColumnSetMethods.containsKey(clazz)) {
      return allColumnSetMethods.get(clazz);
    }

    final List<Method> allClassMethods = new ArrayList<Method>();

    // exclude fields that can't be modified directly from the UI
    final Set<String> excludedFields = new HashSet<>();
    excludedFields.add("id");
    excludedFields.add("timestamp");
    excludedFields.add("lastModified");
    excludedFields.add("lastModifiedBy");
    excludedFields.add("lastApproved");
    excludedFields.add("lastApprovedBy");
    excludedFields.add("terminology");
    excludedFields.add("branch");
    excludedFields.add("branchedTo");

    for (final java.lang.reflect.Field field : getAllFields(clazz)) {
      if (excludedFields.contains(field.getName())) {
        continue;
      }
      if (!field.isAnnotationPresent(Column.class)) {
        continue;
      }

      // Try get first - find a setXXX method that takes 1 parameter
      final String accessorName1 =
          "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
      Method setMethod = null;
      // Iterate through methods
      for (final Method m : clazz.getMethods()) {
        // Find matching name with 1 paramter
        if (m.getName().equals(accessorName1) && m.getParameterTypes().length == 1) {
          setMethod = m;
          break;
        }
      }

      if (setMethod != null) {
        allClassMethods.add(setMethod);
      }
    }

    // Cache for later runs
    allColumnSetMethods.put(clazz, allClassMethods);
    return allClassMethods;

  }

  /**
   * Returns the name analyzed pairs from annotation.
   *
   * @param clazz the clazz
   * @param sortField the sort field
   * @return the name analyzed pairs from annotation
   * @throws Exception the exception
   */
  public static Map<String, Boolean> getNameAnalyzedPairsFromAnnotation(Class<?> clazz,
    String sortField) throws Exception {
    final String key = clazz.getName() + "." + sortField;
    if (sortFieldAnalyzedMap.containsKey(key)) {
      return sortFieldAnalyzedMap.get(key);
    }
    return new HashMap<>();
  }

  /**
   * Apply pfs to lucene query. Uses Hibernate Search 7.x SearchSession API
   * with LuceneExtension for query wrapping.
   *
   * @param clazz the clazz
   * @param query the query
   * @param pfs the pfs
   * @param manager the manager
   * @return the search result wrapper
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
  public static FullTextQueryResult applyPfsToLuceneQuery(final Class<?> clazz, final String query,
    final PfsParameter pfs, final EntityManager manager) throws Exception {

    final SearchSession searchSession = Search.session(manager);
    final SearchMapping mapping = Search.mapping(manager.getEntityManagerFactory());

    // Build up the query
    final StringBuilder pfsQuery = new StringBuilder();
    pfsQuery.append(query);
    if (pfs != null) {
      if (pfs.getActiveOnly()) {
        pfsQuery.append(" AND obsolete:false");
      }
      if (pfs.getInactiveOnly()) {
        pfsQuery.append(" AND obsolete:true");
      }
      if (pfs.getQueryRestriction() != null && !pfs.getQueryRestriction().isEmpty()) {
        pfsQuery.append(" AND " + pfs.getQueryRestriction());
      }
    }

    // Set max clause count
    IndexSearcher.setMaxClauseCount(ConfigUtility.getLuceneMaxClauseCount());

    // Set up query parser with class analyzer from the Lucene index manager
    final QueryParser queryParser = new MultiFieldQueryParser(
        IndexUtility.getIndexedFieldNames(clazz, true).toArray(new String[] {}),
        mapping.indexedEntity(clazz).indexManager().unwrap(LuceneIndexManager.class)
            .searchAnalyzer());
    queryParser.setAllowLeadingWildcard(true);

    // construct the query
    final String finalQuery = (pfsQuery.toString().startsWith(" AND "))
        ? pfsQuery.toString().substring(5) : pfsQuery.toString();

    // ONLY log this if in dev mode
    if ("DEV".equals(ConfigUtility.getConfigProperties().getProperty("deploy.mode"))) {
      Logger.getLogger(IndexUtility.class).info("  query = " + finalQuery + ", " + pfs);
    }

    // Parse the Lucene query
    org.apache.lucene.search.Query luceneQuery;
    try {
      luceneQuery = queryParser.parse(finalQuery);
    } catch (ParseException e) {
      throw new LocalException("Unable to parse query");
    }

    // Build HS7 predicate from Lucene query using LuceneExtension
    final SearchScope<?> scope = searchSession.scope(clazz);
    final SearchPredicateFactory predicateFactory = scope.predicate();
    final SearchPredicate predicate = predicateFactory
        .extension(LuceneExtension.get())
        .fromLuceneQuery(luceneQuery)
        .toPredicate();

    // Build sort
    final List<SearchSort> sortFields = new ArrayList<>();
    boolean randomSort = false;

    if (pfs != null) {
      if (pfs.getSortField() != null && !pfs.getSortField().isEmpty()
          && pfs.getSortField().equals("RANDOM")) {

        // Random sort - will be handled after query execution by shuffling
        randomSort = true;

      } else if ((pfs.getSortFields() != null && !pfs.getSortFields().isEmpty())
          || (pfs.getSortField() != null && !pfs.getSortField().isEmpty())) {

        // convenience container for sort field names (from either method)
        List<String> sortFieldNames = null;

        // use multiple-field sort before backwards-compatible single-field sort
        if (pfs.getSortFields() != null && !pfs.getSortFields().isEmpty()) {
          sortFieldNames = pfs.getSortFields();
        } else {
          sortFieldNames = new ArrayList<>();
          sortFieldNames.add(pfs.getSortField());
        }

        for (final String sortFieldName : sortFieldNames) {

          // the computed string name of the indexed field to sort by
          String sortFieldStr = null;

          // if a subfield search (e.g. FIELD1.FIELD2) skip preconditions
          if (sortFieldName.contains(".")) {
            sortFieldStr = sortFieldName;
          }

          // otherwise, check preconditions
          else {

            final Map<String, Boolean> nameToAnalyzedMap =
                IndexUtility.getNameAnalyzedPairsFromAnnotation(clazz, sortFieldName);

            // check existence of the annotated get[SortFieldName]() method
            if (nameToAnalyzedMap.size() == 0) {
              throw new Exception(clazz.getName()
                  + " does not have declared, annotated method for field " + sortFieldName);
            }

            // first, check explicit [SortFieldName]Sort index
            if (nameToAnalyzedMap.get(sortFieldName + "Sort") != null
                && !nameToAnalyzedMap.get(sortFieldName + "Sort")) {
              sortFieldStr = sortFieldName + "Sort";
            }

            // next check the default name (rendered as ""), if not analyzed, use
            // this as sort
            else if (nameToAnalyzedMap.get("") != null && nameToAnalyzedMap.get("").equals(false)) {
              sortFieldStr = sortFieldName;
            }

            // if an indexed sort field could not be found, throw exception
            if (sortFieldStr == null) {
              throw new Exception(
                  "Could not retrieve a non-analyzed Field annotation for get method for variable name "
                      + sortFieldName);
            }
          }

          // Build HS7 sort using the sort DSL
          SearchSort searchSort;
          if (pfs.isAscending()) {
            searchSort = scope.sort().field(sortFieldStr).asc().toSort();
          } else {
            searchSort = scope.sort().field(sortFieldStr).desc().toSort();
          }

          // add the field
          sortFields.add(searchSort);
        }
      }
    }

    // Build and execute query with entity + score projection
    final SearchQuery<?> searchQuery = searchSession.search(scope)
        .select(f -> f.composite().from(f.entity(), f.score()).asList())
        .where(predicate)
        .sort(f -> f.composite(sortBuilder -> {
          for (final SearchSort sortField : sortFields) {
            sortBuilder.add(sortField);
          }
        }))
        .toQuery();

    SearchResult<?> result;
    // if start index and max results are set, set paging
    if (pfs != null && pfs.getStartIndex() >= 0 && pfs.getMaxResults() >= 0) {
      result = searchQuery.fetch(pfs.getStartIndex(), pfs.getMaxResults());
    } else {
      result = searchQuery.fetch(0, 500000);
    }

    // Convert SearchResult hits to [score, entity] Object[] arrays
    final List<Object[]> resultList = new ArrayList<>();
    for (final Object hit : result.hits()) {
      final List<?> composite = (List<?>) hit;
      final Object entity = composite.get(0);
      final Float score = (Float) composite.get(1);
      resultList.add(new Object[] {
          score, entity
      });
    }

    // Handle random sort by shuffling results
    if (randomSort) {
      Collections.shuffle(resultList);
    }

    return new FullTextQueryResult(result.total().hitCount(), resultList);
  }

  /**
   * Returns the methods for @OneToMany annotated fields.
   *
   * @param clazz the clazz
   * @return the all methods
   * @throws Exception the exception
   */
  public static List<Method> getAllCollectionGetMethods(Class<?> clazz) throws Exception {

    // If already initialized, return computed values
    if (allOneToManyGetMethods.containsKey(clazz)) {
      return allOneToManyGetMethods.get(clazz);
    }

    final List<Method> allClassMethods = new ArrayList<Method>();

    // exclude fields that can't be modified directly from the UI
    final Set<String> excludedFields = new HashSet<>();
    excludedFields.add("inverseRelationships");

    for (final java.lang.reflect.Field field : getAllFields(clazz)) {

      if (excludedFields.contains(field.getName())) {
        continue;
      }
      if (!field.isAnnotationPresent(OneToMany.class)
          && !field.isAnnotationPresent(ManyToMany.class)) {
        continue;
      }

      // Try get first - find a getXXX method that takes no parameters
      final String accessorName1 =
          "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
      Method getMethod;
      try {
        getMethod = clazz.getMethod(accessorName1, new Class<?>[] {});
      } catch (Exception e) {
        getMethod = null;
      }
      try {
        getMethod = clazz.getMethod(accessorName1, new Class<?>[] {});
      } catch (Exception e) {
        getMethod = null;
      }

      if (getMethod != null) {
        allClassMethods.add(getMethod);
        continue;
      }

      // No need to worry about "is" here because these are collection methods

    }

    // Cache for later runs
    allOneToManyGetMethods.put(clazz, allClassMethods);
    return allClassMethods;

  }

}
