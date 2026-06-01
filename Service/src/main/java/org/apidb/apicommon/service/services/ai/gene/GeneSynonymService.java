package org.apidb.apicommon.service.services.ai.gene;

import java.util.List;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;

/**
 * Resolves a gene stable id to its canonical id plus aliases/synonyms via the
 * WDK {@code GeneRecordClasses.GeneRecordClass}, in-process (no external HTTP).
 * Used by the sync prelude both to feed the gene-mention scan and to bake the
 * sorted synonym set into the {@code jobId} digest.
 */
public class GeneSynonymService {

  public static final String GENE_RECORD_CLASS = "GeneRecordClasses.GeneRecordClass";

  private final WdkModel _wdkModel;

  public GeneSynonymService(WdkModel wdkModel) {
    _wdkModel = wdkModel;
  }

  /**
   * @return the gene id followed by its resolved synonyms (canonicalised)
   * @throws org.gusdb.wdk.model.WdkModelException if resolution fails
   *         (a 404 for an unknown stable id is raised by the caller)
   */
  public List<String> resolve(String geneId) throws WdkModelException {
    throw new UnsupportedOperationException("GeneSynonymService.resolve — deliverable 1");
  }
}
