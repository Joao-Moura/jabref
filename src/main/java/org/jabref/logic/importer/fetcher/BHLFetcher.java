package org.jabref.logic.importer.fetcher;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fetches data from Biodiversity Heritage Library (BHL)
 *
 * @implNote <a href="https://www.biodiversitylibrary.org/docs/api3.html">API documentation</a>
 */
public class BHLFetcher implements SearchBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BHLFetcher.class);

    private static final String SEARCH_URL = "https://www.biodiversitylibrary.org/api3";
    private static final String API_KEY = "70e68188-5d42-4bad-b3a8-8e875f1d6be4";

    public static BibEntry parseBibJSONtoBibtex(JSONObject bibJsonEntry) {
        BibEntry entry = new BibEntry(StandardEntryType.Article);

        // Authors
        if (bibJsonEntry.has("Authors")) {
            JSONArray authors = bibJsonEntry.getJSONArray("Authors");
            List<String> authorList = new ArrayList<>();
            for (int i = 0; i < authors.length(); i++) {
                if (authors.getJSONObject(i).has("Name")) {
                    authorList.add(authors.getJSONObject(i).getString("Name"));
                } else {
                    LOGGER.info("Empty author name.");
                }
            }
            entry.setField(StandardField.AUTHOR, String.join(" and ", authorList));
        } else {
            LOGGER.info("No author found.");
        }

        // Year
        if (bibJsonEntry.has("Date")) {
            entry.setField(StandardField.YEAR, bibJsonEntry.getString("Date"));
        }

        // DOI
        if (bibJsonEntry.has("Doi")) {
            entry.setField(StandardField.DOI, bibJsonEntry.getString("Doi"));
        }

        // Publisher
        if (bibJsonEntry.has("PublisherName")) {
            entry.setField(StandardField.PUBLISHER, bibJsonEntry.getString("PublisherName"));
        }

        // Title
        if (bibJsonEntry.has("Title")) {
            entry.setField(StandardField.TITLE, bibJsonEntry.getString("Title"));
        }

        // URL
        if (bibJsonEntry.has("PartUrl")) {
            entry.setField(StandardField.URL, bibJsonEntry.getString("PartUrl"));
        }

        // Volume
        if (bibJsonEntry.has("Volume")) {
            entry.setField(StandardField.VOLUME, bibJsonEntry.getString("Volume"));
        }

        return entry;
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("op", "PublicationSearch");
        uriBuilder.addParameter("searchtype", "C");
        uriBuilder.addParameter("searchterm", new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        // Max results per page
        uriBuilder.addParameter("pageSize", "30");
        uriBuilder.addParameter("apikey", API_KEY);
        uriBuilder.addParameter("format", "json");
        return uriBuilder.build().toURL();
    }

    public URL getURLForPart(String id) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("op", "GetPartMetadata");
        uriBuilder.addParameter("id", id);
        uriBuilder.addParameter("apikey", API_KEY);
        uriBuilder.addParameter("format", "json");
        return uriBuilder.build().toURL();
    }

    @Override
    public String getName() {
        return "BHL";
    }

    public BibEntry getBibEntriesFromMetadata(String partID) throws ParseException {
        URL urlForQuery;
        try {
            urlForQuery = getURLForPart(partID);
        } catch (MalformedURLException e) {
            throw new ParseException("A URL error occurred while fetching with " + partID, e);
        } catch (URISyntaxException e) {
            throw new ParseException("A URL error occurred while fetching with " + partID, e);
        }

        try (InputStream stream = getUrlDownload(urlForQuery).asInputStream()) {
            String response = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining(OS.NEWLINE));
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.has("Result")) {
                JSONObject bibJsonEntry = jsonObject.getJSONArray("Result").getJSONObject(0);
                return parseBibJSONtoBibtex(bibJsonEntry);
            }
            return null;
        } catch (IOException e) {
            throw new ParseException("A network error occurred while fetching from " + urlForQuery, e);
        }
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            JSONObject jsonObject = new JSONObject(response);

            List<BibEntry> entries = new ArrayList<>();
            if (jsonObject.has("Result")) {
                JSONArray results = jsonObject.getJSONArray("Result");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject bibJsonEntry = results.getJSONObject(i);
                    if (bibJsonEntry.getString("BHLType").equals("Part")) {
                        BibEntry entry = getBibEntriesFromMetadata(bibJsonEntry.getString("PartID"));
                        entries.add(entry);
                    }
                }
            }
            return entries;
        };
    }
}
