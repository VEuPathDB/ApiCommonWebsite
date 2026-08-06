package org.apidb.apicommon.service.services.ai.gene;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link GeneMentionScanner#countSubstrings}, ported from
 * {@code _count_substrings} in {@code PubGene_back_end/helpers.py}:
 * <ul>
 *   <li>plain {@code <letters><digits>} tokens allow an optional hyphen between
 *       the letter and digit parts ({@code Nd6} ↔ {@code Nd-6});</li>
 *   <li>aliases containing {@code _}/{@code -}/space treat each run of separators
 *       as {@code [-_\s]+} ({@code PF3D7_1133400} ↔ {@code PF3D7-1133400} ↔
 *       {@code PF3D7 1133400});</li>
 *   <li>matches require non-alphanumeric boundaries on both sides;</li>
 *   <li>matching is case-insensitive;</li>
 *   <li>regex-special characters in the alias are matched literally.</li>
 * </ul>
 */
public class GeneMentionScannerTest {

  private final GeneMentionScanner scanner = new GeneMentionScanner();

  private int count(String paper, String alias) {
    return scanner.countSubstrings(paper, alias);
  }

  @Test
  public void emptyPaperOrAliasScoresZero() {
    assertEquals(0, count("", "Nd6"));
    assertEquals(0, count(null, "Nd6"));
    assertEquals(0, count("some text", ""));
    assertEquals(0, count("some text", null));
  }

  @Test
  public void lettersDigitsAliasMatchesPlainForm() {
    assertEquals(1, count("the gene Nd6 is studied", "Nd6"));
  }

  @Test
  public void lettersDigitsAliasMatchesOptionalHyphenForm() {
    assertEquals(1, count("the gene Nd-6 is studied", "Nd6"));
  }

  @Test
  public void lettersDigitsAliasMatchingIsCaseInsensitive() {
    assertEquals(3, count("ND6 and nd6 and Nd-6", "Nd6"));
  }

  @Test
  public void lettersDigitsAliasRequiresAlphanumericBoundaries() {
    // embedded in a longer alphanumeric token → no match
    assertEquals(0, count("xNd6 and Nd66 and Nd6x", "Nd6"));
  }

  @Test
  public void lettersDigitsAliasHyphenIsOptionalNotMandatory() {
    // a separator other than a single hyphen (e.g. underscore/space) does NOT match
    assertEquals(0, count("Nd_6 and Nd 6", "Nd6"));
  }

  @Test
  public void separatorAliasMatchesAllSeparatorVariants() {
    String paper = "PF3D7_1133400, PF3D7-1133400 and PF3D7 1133400 here";
    assertEquals(3, count(paper, "PF3D7_1133400"));
  }

  @Test
  public void separatorAliasRequiresASeparator() {
    // collapsing to [-_\s]+ means at least one separator is mandatory
    assertEquals(0, count("PF3D71133400", "PF3D7_1133400"));
  }

  @Test
  public void separatorAliasMatchesMultipleSeparatorChars() {
    assertEquals(1, count("PF3D7   1133400", "PF3D7 1133400"));
  }

  @Test
  public void separatorAliasRespectsBoundaries() {
    assertEquals(0, count("xPF3D7_1133400y", "PF3D7_1133400"));
  }

  @Test
  public void regexSpecialCharactersAreMatchedLiterally() {
    // a dot in the alias must match a literal dot, not any character
    assertEquals(1, count("variant abc.def found", "abc.def"));
    assertEquals(0, count("variant abcXdef found", "abc.def"));
  }

  @Test
  public void countsAllNonOverlappingOccurrences() {
    assertEquals(2, count("Nd6 appears, then Nd6 again", "Nd6"));
  }

  // --- namesMentioned (port of aliases_mentioned_in_paper) ------------------

  private List<String> names(String paper, String geneId, String... aliases) {
    return scanner.namesMentioned(paper, geneId, Arrays.asList(aliases));
  }

  @Test
  public void namesMentionedListsGeneFirstThenAliasesByFrequency() {
    String paper = "alpha alpha alpha. beta beta. PF3D7_1133400 is the gene.";
    assertEquals(Arrays.asList("PF3D7_1133400", "alpha", "beta"),
        names(paper, "PF3D7_1133400", "alpha", "beta"));
  }

  @Test
  public void namesMentionedFindsGeneViaUnderscoreToHyphenFallback() {
    // gene id uses an underscore but the paper writes the hyphen form
    assertEquals(Collections.singletonList("PF3D7_1133400"),
        names("the locus PF3D7-1133400 was assayed", "PF3D7_1133400"));
  }

  @Test
  public void namesMentionedOmitsGeneWhenAbsentButKeepsAliases() {
    assertEquals(Collections.singletonList("alpha"),
        names("only alpha is here", "PF3D7_1133400", "alpha"));
  }

  @Test
  public void namesMentionedReturnsEmptyWhenNothingMatches() {
    assertEquals(Collections.emptyList(),
        names("unrelated prose", "PF3D7_1133400", "alpha", "beta"));
  }

  @Test
  public void namesMentionedCapsAliasesAtTopThree() {
    String paper = "a a a a. b b b. c c. d. PF3D7_1133400.";
    // gene + top-3 aliases by count desc (a,b,c); d is dropped
    assertEquals(Arrays.asList("PF3D7_1133400", "a", "b", "c"),
        names(paper, "PF3D7_1133400", "a", "b", "c", "d"));
  }

  @Test
  public void namesMentionedBreaksFrequencyTiesAlphabetically() {
    String paper = "zeta zeta. alpha alpha. beta.";
    assertEquals(Arrays.asList("alpha", "zeta", "beta"),
        names(paper, "GENE_NOT_HERE", "zeta", "alpha", "beta"));
  }

  @Test
  public void namesMentionedCapsAtTopThreeBeforeDedupingGeneWithoutBackfill() {
    // gene 'Nd6' and alias 'ND6' match the same four tokens; the top-3-by-count
    // are [ND6, alpha, beta], and ND6 is then dropped as a case dupe of the gene.
    // Python caps BEFORE the de-dup, so 'gamma' is NOT backfilled into the gap.
    String paper = "Nd6 Nd6 Nd6 Nd6. alpha alpha alpha. beta beta. gamma.";
    assertEquals(Arrays.asList("Nd6", "alpha", "beta"),
        names(paper, "Nd6", "ND6", "alpha", "beta", "gamma"));
  }

  @Test
  public void namesMentionedDedupesAliasMatchingGeneCaseInsensitively() {
    String paper = "Nd6 Nd6 Nd6 here";
    // alias "ND6" duplicates the gene id case-insensitively → dropped
    assertEquals(Collections.singletonList("Nd6"),
        names(paper, "Nd6", "ND6"));
  }

  @Test
  public void namesMentionedReturnsEmptyForEmptyPaperOrGene() {
    assertEquals(Collections.emptyList(), names("", "Nd6", "Nd6"));
    assertEquals(Collections.emptyList(), names("Nd6 here", "", "Nd6"));
  }
}
