package org.apidb.apicommon.service.filter;

import java.util.List;

import javax.annotation.Priority;

import org.gusdb.wdk.service.filter.CheckLoginFilter;

@Priority(30)
public class ApiCheckLoginFilter extends CheckLoginFilter {

  private static final List<String> OPEN_FULL_PATHS = List.of(
      "",
      "login",
      "ontologies/Categories",
      "site-messages",
      "user-profile-vocabularies",
      "subscription-groups",
      "oauth/state-token",
      "users",
      "user-password-reset",
      "users/current",
      "users/current/preferences",
      "client-errors",
      "record-types",
      "record-types/dataset/searches/AllDatasets/reports/standard",
      "record-types/organism/searches/GenomeDataTypes/reports/standard",
      "record-types/genomic-sequence/searches/SequencesByTaxon",
      "record-types/transcript/searches/GeneByLocusTag",
      "record-types/transcript/searches/GenesByText"
  );

  private static final List<String> OPEN_PATH_PREFIXES = List.of(
      "system/metrics/count-page-view",
      "temporary-files",
      "temporary-results"
  );

  private boolean isOpenPath(String path) {
    if (OPEN_FULL_PATHS.contains(path)) return true;
    for(String openPrefix : OPEN_PATH_PREFIXES) {
      if (path.startsWith(openPrefix)) return true;
    }
    return false;
  }

  @Override
  protected boolean isValidTokenRequired(String path) {
    return !isOpenPath(path);
  }

  @Override
  protected boolean isGuestUserAllowed(String path) {
    return isOpenPath(path);
  }
}
