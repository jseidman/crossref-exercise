package com.fivetran.crossref;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Class encapsulating parameters for making a request to the Crossref API
 * works endpoint.
 */
@Builder
@Getter
@Setter
public class WorksRequest {
  private String query;
  private String filter;
  private String selectFields;
  private int rows;
  private String cursor;
}
