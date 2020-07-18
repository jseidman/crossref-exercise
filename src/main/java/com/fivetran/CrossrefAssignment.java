package com.fivetran;

import com.fivetran.crossref.CrossrefClient;
import com.fivetran.crossref.Work;
import com.fivetran.crossref.WorksRequest;
import com.fivetran.crossref.WorksResponse;

import com.sun.org.apache.xpath.internal.operations.Quo;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Fetch records from the Crossref service using the Crossref REST API and
 * persist those records to a CSV file. The persisted records will contain
 * the following fields:
 * <p><ul>
 *   <li>Digital Object Identifier (DOI)
 *   <li>Title(s)
 *   <li>Author(s)
 *   <li>Publisher
 *   <li>Created timestamp
 * </ul><p>
 *
 * Note that in some cases one or more of the above fields will be blank in
 * the returned records. In these cases the records will be persisted with
 * the available fields.
 *
 * In addition to the above, this code will do some basic data cleansing.
 * Currently this cleansing consists of removing embedded newlines in fields
 * and converting multiple spaces into a single space.
 *
 * Records are fetched via the CrossrefClient class, and are fetched based
 * on the following criteria:
 * <p><ul>
 *   <li>Records related to the term "animal".
 *   <li>Records where the published date is within the past year.
 * </ul><p>
 *
 * Note that the Crossref API provides functionality to "page" through large
 * result sets via a cursor value. This functionality is utilized here to
 * fetch and save batches of records until the entire data set is returned.
 */
@Slf4j
public class CrossrefAssignment {
  /**
   * CSV file header.
   */
  private static String HEADER[] = {"DOI", "Title", "Author", "Publisher", "Created"};
  /**
   * Number of records to fetch with each request.
   */
  private static int NUM_ROWS_TO_FETCH = 100;
  /**
   * Path to persist records to.
   */
  private static String CSV_PATH = "/tmp/xref-assignment.csv";

  /**
   * Fetch records from the Crossref API and persist to a CSV file.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    // Construct a string representing the date one year ago from today:
    String fromDate =
        ZonedDateTime
            .now()
            .minusYears(1)
            .format(DateTimeFormatter
            .ofPattern("yyyy-MM-dd"));
    CrossrefClient client = new CrossrefClient();
    FileWriter fileWriter = null;
    CSVPrinter csvPrinter = null;
    boolean done = false;
    int rowsFetched = 0;
    WorksRequest request = WorksRequest.builder()
        .query("animal")
        .filter("from-pub-date:" + fromDate)
        .selectFields("DOI,title,author,created,publisher")
        .rows(NUM_ROWS_TO_FETCH)
        // The '*' tells the API to use the "deep paging" functionality.
        // Subsequent responses will include a next cursor parameter, which
        // we can use to fetch the next set of records.
        .cursor("*")
        .build();

    // Fetch the records and write each batch to a CSV file. We'll get the
    // first set of records, and then use the cursor returned as part of
    // that response to fetch the next set of records. Repeat until we've
    // received the entire result set.
    try {
      fileWriter = new FileWriter(CSV_PATH);
      csvPrinter =
          new CSVPrinter(
              fileWriter,
              CSVFormat.DEFAULT
                  .withHeader(HEADER)
                  .withQuoteMode(QuoteMode.ALL));

      WorksResponse response = client.works(request);
      log.info("Number of records returned=" + response.getWorks().size() +
          ", total result count=" + response.getTotalResults() +
          ", next cursor=" + response.getNextCursor());
      for (Work work : response.getWorks()) {
        writeCsv(csvPrinter, work);
      }
      rowsFetched = response.getWorks().size();
      while (response.getWorks().size() == NUM_ROWS_TO_FETCH &&
          response.getNextCursor() != null) {
        request.setCursor(response.getNextCursor());
        response = client.works(request);
        rowsFetched += response.getWorks().size();
        log.info("Number of records returned=" + response.getWorks().size() +
            ", total result size=" + response.getTotalResults() +
            ", total results fetched=" + rowsFetched +
            ", next cursor=" + response.getNextCursor());
        for (Work work : response.getWorks()) {
          writeCsv(csvPrinter, work);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        csvPrinter.close();
        fileWriter.close();
      } catch (IOException e) {
      }
    }
  }

  /**
   * Write CSV formatted records to a file.
   *
   * @param csvPrinter Object that supports writing CSV formatted records to a
   *                   file.
   * @param work Object representing values to be persisted.
   * @throws IOException Thrown if error occurs writing record.
   */
  private static void writeCsv(CSVPrinter csvPrinter, Work work)
      throws  IOException {
    csvPrinter.printRecord(
        work.getDOI(),
        prepareStrings(work.getTitles()),
        prepareStrings(work.getAuthors()),
        work.getPublisher(),
        work.getTimestamp());
  }

  /**
   * Convert a collection of Strings into a formatted string delimited
   * by ';'. This will also remove any embedded newlines and ensure that
   * multiple spaces are converted into a single space.
   *
   * @param fields A collection of Strings.
   * @return Formatted String constructed from input collection.
   */
  private static String prepareStrings(List<String> fields) {
    return String.join(";", fields)
        .replaceAll("\\R", " ")
        .replaceAll("\\s+", " ");
  }
}
