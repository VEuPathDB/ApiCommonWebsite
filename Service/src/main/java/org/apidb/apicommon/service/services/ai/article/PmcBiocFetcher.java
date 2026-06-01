package org.apidb.apicommon.service.services.ai.article;

/**
 * Stage ① article-text resolution for the PubMed path: fetches the PMC BioC
 * JSON document for a PubMed id, keeps passages whose
 * {@code infons.section_type} is one of {FIG, TABLE, RESULTS, CONCL,
 * DISCUSSION, SUPPL}, and concatenates their {@code text}.
 *
 * <p>A non-JSON / 404 / non-open-access response yields a terminal
 * {@code text-unavailable} (never persisted to the cache — retries re-run the
 * fetch). The upload path has no fetch: the front-end-supplied {@code paper_text}
 * is used directly.
 */
public class PmcBiocFetcher {

  public static final String BIOC_URL_BASE =
      "https://www.ncbi.nlm.nih.gov/research/bionlp/RESTful/pmcoa.cgi/BioC_json/";

  /**
   * @return concatenated relevant passage text for the given PubMed id
   * @throws TextUnavailableException if the paper text cannot be resolved
   */
  public String fetch(String pubmedId) throws TextUnavailableException {
    throw new UnsupportedOperationException("PmcBiocFetcher.fetch — deliverable 2");
  }

  /** Signals a terminal {@code text-unavailable} outcome (not cached). */
  public static class TextUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public TextUnavailableException(String reason) {
      super(reason);
    }
  }
}
