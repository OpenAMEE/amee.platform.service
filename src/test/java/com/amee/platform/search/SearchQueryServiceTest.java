package com.amee.platform.search;

import com.amee.base.domain.ResultsWrapper;
import com.amee.domain.ObjectType;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertTrue;

// TODO: PL-6618
// @RunWith(SpringJUnit4ClassRunner.class)
public class SearchQueryServiceTest { //} extends ServiceTest {

    @Test
    public void x() {

    }

    // @Autowired
    private SearchQueryService searchQueryService;

    // @Autowired
    private LuceneServiceMock luceneService;

    // Mock Data Category Documents.
    private final static Document DC_AA = getMockDocument(ObjectType.DC, "AAAAAAAAAAAA", null); // Page A1.
    private final static Document DC_AB = getMockDocument(ObjectType.DC, "AAAAAAAAAAAB", null); // Page A1.
    private final static Document DC_AC = getMockDocument(ObjectType.DC, "AAAAAAAAAAAC", null); // Page A2.
    private final static Document DC_AD = getMockDocument(ObjectType.DC, "AAAAAAAAAAAD", null); // Page A2.
    private final static Document DC_AE = getMockDocument(ObjectType.DC, "AAAAAAAAAAAE", null); // Page A3.
    private final static Document DC_AF = getMockDocument(ObjectType.DC, "AAAAAAAAAAAF", null); // Page A3Alt.
    private final static Document DC_BA = getMockDocument(ObjectType.DC, "BBBBBBBBBBBA", null);
    private final static Document DC_BB = getMockDocument(ObjectType.DC, "BBBBBBBBBBBB", null);
    private final static Document DC_BC = getMockDocument(ObjectType.DC, "BBBBBBBBBBBC", null);
    private final static Document DC_BD = getMockDocument(ObjectType.DC, "BBBBBBBBBBBD", null);

    // Mock Data Item Documents.
    private final static Document DI_1 = getMockDocument(ObjectType.NDI, "111111111111", "BBBBBBBBBBBA"); // Page A3 / A4Alt / B1.
    private final static Document DI_2 = getMockDocument(ObjectType.NDI, "111111111112", "BBBBBBBBBBBA"); // DuplicateAB.
    private final static Document DI_3 = getMockDocument(ObjectType.NDI, "111111111113", "BBBBBBBBBBBB"); // Page A4 / A4Alt / B1.
    private final static Document DI_4 = getMockDocument(ObjectType.NDI, "111111111114", "BBBBBBBBBBBB"); // DuplicateAB.
    private final static Document DI_5 = getMockDocument(ObjectType.NDI, "111111111115", "AAAAAAAAAAAB"); // DuplicateA / Page B2.
    private final static Document DI_6 = getMockDocument(ObjectType.NDI, "111111111116", "BBBBBBBBBBBC"); // Page A4 / B2.
    private final static Document DI_7 = getMockDocument(ObjectType.NDI, "111111111117", "BBBBBBBBBBBD"); // Page A5 / B3.

    private final static List<Document> ALL_DOCUMENTS = new ArrayList<Document>(
            Arrays.asList(
                    DC_AA, DC_AB, DC_AC, DC_AD, DC_AE, DC_AF, DC_BA, DC_BB, DC_BC, DC_BD,
                    DI_1, DI_2, DI_3, DI_4, DI_5, DI_6, DI_7));

    // DC: results = 5, truncated = false
    private final static ResultsWrapper<Document> ALL_DC_RESULTS =
            new ResultsWrapper<Document>(
                    new ArrayList<Document>(
                            Arrays.asList(
                                    DC_AA,
                                    DC_AB,
                                    DC_AC,
                                    DC_AD,
                                    DC_AE)),
                    false);

    // DC: results = 6, truncated = false
    private final static ResultsWrapper<Document> ALL_DC_RESULTS_ALT =
            new ResultsWrapper<Document>(
                    new ArrayList<Document>(
                            Arrays.asList(
                                    DC_AA,
                                    DC_AB,
                                    DC_AC,
                                    DC_AD,
                                    DC_AE,
                                    DC_AF)),
                    false);

    // DI: results = 6, truncated = false
    private final static ResultsWrapper<Document> ALL_DI_RESULTS =
            new ResultsWrapper<Document>(
                    new ArrayList<Document>(
                            Arrays.asList(
                                    DI_1,
                                    DI_2,
                                    DI_3,
                                    DI_4,
                                    DI_5,
                                    DI_6,
                                    DI_7)),
                    false);

    // @Test
    public void hasNoResultsPageA1() {
        // Create mock results.
        luceneService.setAllResults(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        0,
                        2,
                        0));
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        0,
                        2,
                        0));
        luceneService.setResultsWrapperB(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        0,
                        2,
                        0));
        luceneService.setAllDocuments(null);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(0);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have no results.", resultsWrapper.getResults().isEmpty());
        assertTrue("Should not be truncated.", !resultsWrapper.isTruncated());
        assertTrue("Should have 0 hits.", resultsWrapper.getHits() == 0);
        assertTrue("Should have resultStart of 0.", resultsWrapper.getResultStart() == 0);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasOnlyPrimaryResultsPageA1() {
        // Create mock results.
        luceneService.setAllResults(ALL_DC_RESULTS);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(
                                Arrays.asList(
                                        DC_AA,
                                        DC_AB)),
                        true,
                        0,
                        2,
                        5));
        luceneService.setResultsWrapperB(null);
        luceneService.setAllDocuments(null);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(0);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 2 results.", resultsWrapper.getResults().size() == 2);
        assertTrue("Should be truncated.", resultsWrapper.isTruncated());
        assertTrue("Should have 5 hits.", resultsWrapper.getHits() == 5);
        assertTrue("Should have resultStart of 0.", resultsWrapper.getResultStart() == 0);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasOnlyPrimaryResultsPageA2() {
        // Create mock results.
        luceneService.setAllResults(ALL_DC_RESULTS);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(
                                Arrays.asList(
                                        DC_AC,
                                        DC_AD)),
                        true,
                        2,
                        2,
                        5));
        luceneService.setResultsWrapperB(null);
        luceneService.setAllDocuments(null);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(2);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 2 results.", resultsWrapper.getResults().size() == 2);
        assertTrue("Should be truncated.", resultsWrapper.isTruncated());
        assertTrue("Should have 5 hits.", resultsWrapper.getHits() == 5);
        assertTrue("Should have resultStart of 2.", resultsWrapper.getResultStart() == 2);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasPrimaryAndSecondaryResultsPageA3() {
        // Create mock results.
        luceneService.setAllResults(ALL_DC_RESULTS);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(
                                Arrays.asList(
                                        DC_AE)),
                        false,
                        4,
                        2,
                        5));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(4);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 2 results.", resultsWrapper.getResults().size() == 2);
        assertTrue("Should be truncated.", resultsWrapper.isTruncated());
        assertTrue("Should have 5 hits.", resultsWrapper.getHits() == 5);
        assertTrue("Should have resultStart of 4.", resultsWrapper.getResultStart() == 4);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasOnlyPrimaryResultsPageA3Alt() {
        // Create mock results.
        luceneService.setAllResults(ALL_DC_RESULTS_ALT);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(
                                Arrays.asList(
                                        DC_AE,
                                        DC_AF)),
                        false,
                        4,
                        2,
                        6));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(4);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 2 results.", resultsWrapper.getResults().size() == 2);
        assertTrue("Should be truncated.", resultsWrapper.isTruncated());
        assertTrue("Should have 6 hits.", resultsWrapper.getHits() == 6);
        assertTrue("Should have resultStart of 4.", resultsWrapper.getResultStart() == 4);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasSecondaryResultsPageA4() {
        // Create mock results.
        luceneService.setAllResults(ALL_DC_RESULTS);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        6,
                        2,
                        5));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(6);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 2 results.", resultsWrapper.getResults().size() == 2);
        assertTrue("Should be truncated.", resultsWrapper.isTruncated());
        assertTrue("Should have 5 hits.", resultsWrapper.getHits() == 5);
        assertTrue("Should have resultStart of 6.", resultsWrapper.getResultStart() == 6);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasSecondaryResultsPageA4Alt() {
        // Create mock results.
        luceneService.setAllResults(ALL_DC_RESULTS_ALT);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        6,
                        2,
                        6));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(6);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 2 results.", resultsWrapper.getResults().size() == 2);
        assertTrue("Should be truncated.", resultsWrapper.isTruncated());
        assertTrue("Should have 6 hits.", resultsWrapper.getHits() == 6);
        assertTrue("Should have resultStart of 6.", resultsWrapper.getResultStart() == 6);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasSecondaryResultsPageA5() {
        // Create mock results.
        luceneService.setAllResults(ALL_DC_RESULTS);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        8,
                        2,
                        5));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(8);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 1 result.", resultsWrapper.getResults().size() == 1);
        assertTrue("Should not be truncated.", !resultsWrapper.isTruncated());
        assertTrue("Should have 5 hits.", resultsWrapper.getHits() == 5);
        assertTrue("Should have resultStart of 8.", resultsWrapper.getResultStart() == 8);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasNoResultsPageA6() {
        // Create mock results.
        luceneService.setAllResults(ALL_DC_RESULTS);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        10,
                        2,
                        5));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(10);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should not have results.", resultsWrapper.getResults().isEmpty());
        assertTrue("Should not be truncated.", !resultsWrapper.isTruncated());
        assertTrue("Should have 5 hits.", resultsWrapper.getHits() == 5);
        assertTrue("Should have resultStart of 10.", resultsWrapper.getResultStart() == 10);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasOnlySecondaryResultsPageB1() {
        // Create mock results.
        luceneService.setAllResults(null);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        0,
                        2,
                        0));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(0);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 2 result.", resultsWrapper.getResults().size() == 2);
        assertTrue("Should be truncated.", resultsWrapper.isTruncated());
        assertTrue("Should have 0 hits.", resultsWrapper.getHits() == 0);
        assertTrue("Should have resultStart of 0.", resultsWrapper.getResultStart() == 0);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasOnlySecondaryResultsPageB2() {
        // Create mock results.
        luceneService.setAllResults(null);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        2,
                        2,
                        0));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(2);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 2 result.", resultsWrapper.getResults().size() == 2);
        assertTrue("Should be truncated.", resultsWrapper.isTruncated());
        assertTrue("Should have 0 hits.", resultsWrapper.getHits() == 0);
        assertTrue("Should have resultStart of 2.", resultsWrapper.getResultStart() == 2);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasOnlySecondaryResultsPageB3() {
        // Create mock results.
        luceneService.setAllResults(null);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        4,
                        2,
                        0));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(4);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have results.", !resultsWrapper.getResults().isEmpty());
        assertTrue("Should have 1 result.", resultsWrapper.getResults().size() == 1);
        assertTrue("Should not be truncated.", !resultsWrapper.isTruncated());
        assertTrue("Should have 0 hits.", resultsWrapper.getHits() == 0);
        assertTrue("Should have resultStart of 4.", resultsWrapper.getResultStart() == 4);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    // @Test
    public void hasNoResultsPageB4() {
        // Create mock results.
        luceneService.setAllResults(null);
        luceneService.setResultsWrapperA(
                new ResultsWrapper<Document>(
                        new ArrayList<Document>(),
                        false,
                        6,
                        2,
                        0));
        luceneService.setResultsWrapperB(ALL_DI_RESULTS);
        luceneService.setAllDocuments(ALL_DOCUMENTS);
        luceneService.setCount(0);
        // Create SearchFilter.
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.getTypes().add(ObjectType.DC);
        searchFilter.setResultStart(6);
        searchFilter.setResultLimit(2);
        // Get results.
        ResultsWrapper<Document> resultsWrapper = searchQueryService.doSearch(searchFilter);
        assertTrue("Should have no results.", resultsWrapper.getResults().isEmpty());
        assertTrue("Should not be truncated.", !resultsWrapper.isTruncated());
        assertTrue("Should have 0 hits.", resultsWrapper.getHits() == 0);
        assertTrue("Should have resultStart of 6.", resultsWrapper.getResultStart() == 6);
        assertTrue("Should have resultLimit of 2.", resultsWrapper.getResultLimit() == 2);
    }

    private static Document getMockDocument(ObjectType entityType, String uid, String categoryUid) {
        Document document = new Document();
        document.add(new Field("entityUid", uid, Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("entityType", entityType.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        if (categoryUid != null) {
            document.add(new Field("categoryUid", categoryUid, Field.Store.YES, Field.Index.NOT_ANALYZED));
        }
        return document;
    }
}