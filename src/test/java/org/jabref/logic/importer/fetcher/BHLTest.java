package org.jabref.logic.importer.fetcher;

import kong.unirest.json.JSONObject;
import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BHLTest {

    BHLFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new BHLFetcher();
    }

    @Test
    void testSearchByQueryFindsEntry() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article);
        expected.setField(StandardField.AUTHOR, "Frost, C A");
        expected.setField(StandardField.DOI, "10.1155/1929/48919");
        expected.setField(StandardField.PUBLISHER, "Cambridge Entomological Club");
        expected.setField(StandardField.TITLE, "The Unexpected Acid Test");
        expected.setField(StandardField.URL, "https://www.biodiversitylibrary.org/part/182767");
        expected.setField(StandardField.VOLUME, "36");
        expected.setField(StandardField.YEAR, "1929");

        List<BibEntry> fetchedEntries = fetcher.performSearch("The Unexpected Acid Test");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }

    @Test
    void testSearchByEmptyQuery() throws FetcherException {
        assertEquals(Collections.emptyList(), fetcher.performSearch(""));
    }

    @Test
    void testGetURLForQueryWithLucene() throws QueryNodeParseException, MalformedURLException, FetcherException,
            URISyntaxException {
        String query = "The Unexpected Acid Test";
        SyntaxParser parser = new StandardSyntaxParser();
        URL url = fetcher.getURLForQuery(parser.parse(query, "default"));
        assertEquals("https://www.biodiversitylibrary.org/api3?op=PublicationSearch&searchtype=C&searchterm=The" +
                "+Unexpected+Acid+Test&pageSize=30&apikey=70e68188-5d42-4bad-b3a8-8e875f1d6be4&format=json", url.toString());
    }

    @Test
    void testGetURLForParty() throws QueryNodeParseException, MalformedURLException, FetcherException,
            URISyntaxException {
        URL url = fetcher.getURLForPart("182767");
        assertEquals("https://www.biodiversitylibrary.org/api3?op=GetPartMetadata&id=182767&apikey=70e68188-5d42-4bad-b3a8-8e875f1d6be4&format=json", url.toString());
    }

    @Test
    void testBibJSONConverter() {
        String jsonString = "{\"PartUrl\":\"https://www.biodiversitylibrary.org/part/182767\",\"PartID\":182767,\"ItemID\":\"207077\",\"StartPageID\":\"50913436\",\"SequenceOrder\":\"11\",\"Genre\":\"Article\",\"Title\":\"The Unexpected Acid Test\",\"ContainerTitle\":\"Psyche.\",\"PublicationDetails\":\"Cambridge, Mass. : Cambridge Entomological Club\",\"PublisherName\":\"Cambridge Entomological Club\",\"PublisherPlace\":\"Cambridge, Mass. :\",\"Volume\":\"36\",\"Date\":\"1929\",\"PageRange\":\"59--59\",\"StartPageNumber\":\"59\",\"EndPageNumber\":\"59\",\"Language\":\"English\",\"RightsStatus\":\"Public domain. The BHL considers that this work is no longer under copyright protection.\",\"Doi\":\"10.1155/1929/48919\",\"CreationDate\":\"2016/06/25 00:36:09\",\"Authors\":[{\"AuthorID\":\"82615\",\"Name\":\"Frost, C A\"}],\"Contributors\":[{\"ContributorName\":\"BioStor\"}],\"Subjects\":[],\"Identifiers\":[{\"IdentifierName\":\"BioStor\",\"IdentifierValue\":\"173631\"},{\"IdentifierName\":\"DOI\",\"IdentifierValue\":\"10.1155/1929/48919\"}],\"RelatedParts\":[]}";
        JSONObject jsonObject = new JSONObject(jsonString);
        BibEntry bibEntry = fetcher.parseBibJSONtoBibtex(jsonObject);

        assertEquals(StandardEntryType.Article, bibEntry.getType());
        assertEquals(Optional.of("10.1155/1929/48919"), bibEntry.getField(StandardField.DOI));
        assertEquals(Optional.of("Frost, C A"), bibEntry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("The Unexpected Acid Test"), bibEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("1929"), bibEntry.getField(StandardField.YEAR));
    }

    @Test
    void testGetBibEntriesFromMetadata() throws ParseException {
        String partID = "182767";
        BibEntry bibEntry = fetcher.getBibEntriesFromMetadata(partID);

        assertEquals(StandardEntryType.Article, bibEntry.getType());
        assertEquals(Optional.of("10.1155/1929/48919"), bibEntry.getField(StandardField.DOI));
        assertEquals(Optional.of("Frost, C A"), bibEntry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("The Unexpected Acid Test"), bibEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("1929"), bibEntry.getField(StandardField.YEAR));
    }
}
