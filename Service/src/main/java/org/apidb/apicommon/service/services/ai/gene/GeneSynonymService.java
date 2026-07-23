package org.apidb.apicommon.service.services.ai.gene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import javax.ws.rs.NotFoundException;

import org.apache.log4j.Logger;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.PrimaryKeyDefinition;
import org.gusdb.wdk.model.record.PrimaryKeyValue;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;

/**
 * Resolves a gene stable id to its aliases/synonyms via the WDK
 * {@code GeneRecordClasses.GeneRecordClass}, in-process. Mirrors the Python
 * pipeline's {@code get_vpdb_alias} (which POSTs to the public
 * {@code /service/record-types/gene/records} endpoint requesting the
 * {@code Alias} table) — but reads the same {@code Alias} table directly off the
 * record instance, since we are that service.
 *
 * <p>Gene aliases are public, non-user-owned data, so resolution runs as the
 * WDK system user. The returned list excludes the queried id itself and is
 * sorted/de-duplicated — the canonical synonym set folded into the {@code jobId}
 * digest and scanned against the paper text.
 */
public class GeneSynonymService {

  private static final Logger LOG = Logger.getLogger(GeneSynonymService.class);

  public static final String GENE_URL_SEGMENT = "gene";
  public static final String ALIAS_TABLE = "Alias";
  public static final String ALIAS_COLUMN = "alias";
  public static final String SOURCE_ID_COLUMN = "source_id";
  public static final String PROJECT_ID_COLUMN = "project_id";
  public static final String ORGANISM_ATTR = "organism";
  public static final String GENE_NAME_ATTR = "name";

  private final WdkModel _wdkModel;

  public GeneSynonymService(WdkModel wdkModel) {
    _wdkModel = wdkModel;
  }

  /**
   * @return the gene's aliases (canonical, sorted, de-duplicated, excluding the
   *         queried id). Empty if the gene has no aliases.
   * @throws NotFoundException if the stable id maps to no gene record (→ 404)
   * @throws WdkModelException on model/lookup failure
   */
  public List<String> resolve(String geneId) throws WdkModelException {
    RecordClass geneClass = _wdkModel.getRecordClassByUrlSegment(GENE_URL_SEGMENT)
        .orElseThrow(() -> new WdkModelException(
            "Gene record class '" + GENE_URL_SEGMENT + "' is not present in the model"));

    PrimaryKeyValue pkValue = buildPrimaryKey(geneClass.getPrimaryKeyDefinition(), geneId);

    List<RecordInstance> records = RecordClass.getRecordInstances(_wdkModel.getSystemUser(), pkValue);
    if (records.isEmpty()) {
      throw new NotFoundException("No gene found for stable id '" + geneId + "'");
    }

    // TODO: propagate ambiguous-id options to caller as a 300 Multiple Choices (see RecordService)
    if (records.size() > 1) {
      LOG.warn("Gene id '" + geneId + "' resolved to " + records.size() + " records; using first");
    }

    return readAliases(records.get(0), geneId);
  }

  /**
   * Looks up the organism for a gene from its record's {@code organism} attribute.
   * Returns {@link Optional#empty()} if the gene has no such attribute, the attribute
   * value is blank, or any lookup error occurs — organism is optional for comment
   * creation and must never prevent publishing.
   */
  public Optional<String> resolveOrganism(String geneId) throws WdkModelException {
    RecordClass geneClass = _wdkModel.getRecordClassByUrlSegment(GENE_URL_SEGMENT)
        .orElseThrow(() -> new WdkModelException(
            "Gene record class '" + GENE_URL_SEGMENT + "' is not present in the model"));

    PrimaryKeyValue pkValue = buildPrimaryKey(geneClass.getPrimaryKeyDefinition(), geneId);

    List<RecordInstance> records = RecordClass.getRecordInstances(_wdkModel.getSystemUser(), pkValue);
    if (records.isEmpty()) {
      LOG.warn("No gene record found for '" + geneId + "' while resolving organism; organism will be null");
      return Optional.empty();
    }

    try {
      AttributeValue attrVal = records.get(0).getAttributeValue(ORGANISM_ATTR);
      if (attrVal == null) return Optional.empty();
      String value = attrVal.getValue();
      return (value == null || value.trim().isEmpty()) ? Optional.empty() : Optional.of(value);
    }
    catch (Exception e) {
      LOG.warn("Could not read organism attribute for gene '" + geneId + "': " + e.getMessage());
      return Optional.empty();
    }
  }

  private PrimaryKeyValue buildPrimaryKey(PrimaryKeyDefinition pkDef, String geneId)
      throws WdkModelException {
    Map<String, Object> pk = new java.util.HashMap<>();
    for (String column : pkDef.getColumnRefs()) {
      if (SOURCE_ID_COLUMN.equalsIgnoreCase(column)) {
        pk.put(column, geneId);
      }
      else if (PROJECT_ID_COLUMN.equalsIgnoreCase(column)) {
        pk.put(column, _wdkModel.getProjectId());
      }
      else {
        throw new WdkModelException(
            "Unexpected gene primary-key column '" + column + "'; cannot resolve synonyms");
      }
    }
    return new PrimaryKeyValue(pkDef, pk);
  }

  private List<String> readAliases(RecordInstance gene, String geneId) throws WdkModelException {
    TreeSet<String> aliases = new TreeSet<>();

    try {
      // gene name
      AttributeValue nameAttr = gene.getAttributeValue(GENE_NAME_ATTR);
      if (nameAttr != null) {
        String name = nameAttr.getValue();
        if (name != null && !name.isEmpty() && !name.equals(geneId)) {
          aliases.add(name);
        }
      }

      // aliases
      TableValue table = gene.getTableValue(ALIAS_TABLE);
      for (Map<String, AttributeValue> row : table) {
        AttributeValue cell = row.get(ALIAS_COLUMN);
        if (cell == null) continue;
        String value = cell.getValue();
        if (value != null && !value.isEmpty() && !value.equals(geneId)) {
          aliases.add(value);
        }
      }
    }
    catch (WdkUserException e) {
      throw new WdkModelException(
          "Failed to read gene attributes/aliases for gene '" + geneId + "'", e);
    }
    return new ArrayList<>(aliases);
  }
}
