package org.apidb.apicommon.service.services.ai.gene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  /** Plain {@code <letters><digits>} alias, e.g. {@code Nd6} or {@code DHHC16}. */
  private static final Pattern LETTERS_DIGITS = Pattern.compile("([A-Za-z]+)([0-9]+)");

  /**
   * Count boundary-anchored, separator-tolerant, case-insensitive occurrences of
   * {@code needle} in {@code haystack}. Direct port of {@code _count_substrings}.
   */
  public int countSubstrings(String haystack, String needle) {
    if (haystack == null || haystack.isEmpty() || needle == null || needle.isEmpty())
      return 0;

    String inner;
    Matcher ld = LETTERS_DIGITS.matcher(needle);
    if (ld.matches()) {
      // Case 1: letters+digits only → allow an optional hyphen before the digits.
      inner = Pattern.quote(ld.group(1)) + "-?" + Pattern.quote(ld.group(2));
    }
    else {
      // Case 2: collapse each run of _/-/space into a flexible [-_\s]+ matcher;
      // match every other character literally.
      StringBuilder sb = new StringBuilder();
      int i = 0;
      while (i < needle.length()) {
        char ch = needle.charAt(i);
        if (ch == '_' || ch == '-' || ch == ' ') {
          while (i < needle.length() && isSeparator(needle.charAt(i)))
            i++;
          sb.append("[-_\\s]+");
          continue;
        }
        sb.append(Pattern.quote(String.valueOf(ch)));
        i++;
      }
      inner = sb.toString();
    }

    Pattern pattern = Pattern.compile(
        "(?<![A-Za-z0-9])" + inner + "(?![A-Za-z0-9])", Pattern.CASE_INSENSITIVE);
    Matcher m = pattern.matcher(haystack);
    int count = 0;
    while (m.find())
      count++;
    return count;
  }

  private static boolean isSeparator(char ch) {
    return ch == '_' || ch == '-' || ch == ' ';
  }

  /**
   * Port of {@code aliases_mentioned_in_paper}: the ordered list of names
   * actually mentioned in {@code paperText} —
   * <ul>
   *   <li>{@code geneId} first, iff mentioned (underscore↔hyphen tolerant);</li>
   *   <li>then the aliases that occur in the text, by frequency descending and
   *       alphabetically within a tie, capped at the top three;</li>
   *   <li>de-duplicated case-insensitively, preserving the order above.</li>
   * </ul>
   * An empty list means neither the gene id nor any alias was found, which the
   * pipeline maps to the deterministic {@code gene-not-mentioned} terminal.
   */
  public List<String> namesMentioned(String paperText, String geneId, List<String> aliases) {
    List<String> names = new ArrayList<>();
    if (paperText == null || paperText.isEmpty() || geneId == null || geneId.isEmpty())
      return names;

    // gene id first, iff mentioned (underscore↔hyphen tolerant)
    int geneHits = countSubstrings(paperText, geneId);
    if (geneHits == 0 && geneId.indexOf('_') >= 0)
      geneHits = countSubstrings(paperText, geneId.replace('_', '-'));
    if (geneHits > 0)
      names.add(geneId);

    // aliases actually mentioned, by count desc then alphabetical, capped at 3
    List<AliasCount> mentioned = new ArrayList<>();
    if (aliases != null) {
      for (String alias : aliases) {
        int count = countSubstrings(paperText, alias);
        if (count > 0)
          mentioned.add(new AliasCount(alias, count));
      }
    }
    mentioned.sort(Comparator
        .comparingInt((AliasCount ac) -> ac.count).reversed()
        .thenComparing(ac -> ac.alias));

    // Python caps at the top-3-by-count FIRST, then de-dups those against the
    // gene id (and among themselves) — it does not backfill a 4th alias into a
    // gap left by a case-insensitive duplicate. Match that ordering exactly.
    Set<String> seen = new HashSet<>();
    for (String n : names)
      seen.add(n.toLowerCase());
    int top = Math.min(3, mentioned.size());
    for (int i = 0; i < top; i++) {
      String alias = mentioned.get(i).alias;
      if (seen.add(alias.toLowerCase()))
        names.add(alias);
    }
    return names;
  }

  /** An alias paired with its mention count, for frequency ranking. */
  private static final class AliasCount {
    final String alias;
    final int count;
    AliasCount(String alias, int count) {
      this.alias = alias;
      this.count = count;
    }
  }
}
