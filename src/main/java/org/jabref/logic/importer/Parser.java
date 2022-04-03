package org.jabref.logic.importer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jabref.model.entry.BibEntry;

/**
 * A parser converts an {@link InputStream} into a list of {@link BibEntry}.
 */
public interface Parser {

    List<BibEntry> parseEntries(InputStream inputStream) throws ParseException, FetcherException;

    default List<BibEntry> parseEntries(String dataString) throws ParseException {
        try {
            return parseEntries(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8)));
        } catch (FetcherException e) {
            throw new ParseException("Error", e);
        }
    }
}
