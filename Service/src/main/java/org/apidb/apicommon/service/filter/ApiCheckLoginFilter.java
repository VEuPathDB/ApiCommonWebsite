package org.apidb.apicommon.service.filter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Priority;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.wdk.service.filter.CheckLoginFilter;

@Priority(30)
public class ApiCheckLoginFilter extends CheckLoginFilter {

  private static final List<String> OPEN_FULL_PATHS = new ListBuilder<String>()
    .addAll(List.of(
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
      "users/current/favorites/query",
      "client-errors",
      "system/metrics/organism",
      "record-types",
      "record-types/dataset/records",
      "record-types/dataset/searches/AllDatasets/reports/standard",
      "record-types/organism/searches/GenomeDataTypes/reports/standard",
      "record-types/genomic-sequence/searches/SequencesByTaxon",
      "record-types/transcript/searches/GeneByLocusTag",
      "record-types/transcript/searches/GenesByText"
    ))
    .addAll(List.of(
      "transcript","gene","organism","genomic-sequence","genomic-segment","est",
      "pathway","compound","blast-est-ontology","blast-orf-ontology","file","build",
      "dfile","metrics","jbrowse-gene","dataset-release-notes","legacy-dataset",
      "dataset","datasource","userdataset"
    ).stream().map(s -> "record-types/" + s).collect(Collectors.toList()))
    .toList();

  private static final List<String> OPEN_PATH_PREFIXES = List.of(
      "system/metrics/count-page-view",
      "temporary-files",
      "temporary-results"
  );

  private static final String ADDITIONAL_MESSAGE_TEMPLATE =
      "\n\nRegistered users can obtain their API key here: %s/user/profile#serviceAccess" +
      "\n\nFor instructions on how to include the API key in your request, see: %s/static-content/content/PlasmoDB/webServices.html";

  @Override
  protected String getAdditionalUnauthorizedMessage() {
    Map<String,String> props = _wdkModel.getProperties();
    String clientBaseUrl = props.get("LOCALHOST") + props.get("WEBAPP_BASE_URL");
    return ADDITIONAL_MESSAGE_TEMPLATE.formatted(clientBaseUrl, clientBaseUrl);
  }

  private boolean isOpenPath(String path) {
    // get rid of trailing slashes
    if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

    // check if path matches any open full paths
    if (OPEN_FULL_PATHS.contains(path)) return true;

    // check if path prefix matches any open prefixes
    for(String openPrefix : OPEN_PATH_PREFIXES) {
      if (path.startsWith(openPrefix)) return true;
    }

    // deny access to any other path
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

  @Override
  protected boolean isPathToSkip(String path) {
    return path.startsWith("profileSet") || super.isPathToSkip(path);
  }

}
