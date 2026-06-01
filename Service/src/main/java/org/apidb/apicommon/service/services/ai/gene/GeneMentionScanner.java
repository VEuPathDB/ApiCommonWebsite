package org.apidb.apicommon.service.services.ai.gene;

import java.util.List;
import java.util.Map;

/**
 * Deterministic regex matcher that counts gene-synonym mentions in the paper
 * text. Port of {@code _count_substrings} from
 * {@code PubGene_back_end/helpers.py}: case-insensitive, tolerant of
 * separator variants ({@code Nd6} ↔ {@code Nd-6}, {@code PF3D7_1133400} ↔
 * {@code PF3D7-1133400}), anchored on non-alphanumeric boundaries.
 *
 * <p>If the gene id and all aliases score zero, the pipeline short-circuits to
 * a deterministic {@code gene-not-mentioned} terminal; otherwise the top-3
 * aliases by frequency are passed into the summary prompt.
 */
public class GeneMentionScanner {

  /** Count boundary-anchored, separator-tolerant occurrences of {@code needle}. */
  public int countSubstrings(String haystack, String needle) {
    throw new UnsupportedOperationException("GeneMentionScanner.countSubstrings — deliverable 3");
  }

  /** @return per-synonym hit counts over {@code paperText}. */
  public Map<String, Integer> scan(String paperText, List<String> synonyms) {
    throw new UnsupportedOperationException("GeneMentionScanner.scan — deliverable 3");
  }
}
