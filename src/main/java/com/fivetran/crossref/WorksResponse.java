package com.fivetran.crossref;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Class encapsulating a response from the Crossref API works endpoint.
 */
@Builder
@Getter
public class WorksResponse {
  private List<Work> works;
  private int totalResults;
  private String nextCursor;
}
