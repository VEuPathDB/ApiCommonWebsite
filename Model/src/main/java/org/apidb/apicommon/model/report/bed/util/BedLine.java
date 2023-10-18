package org.apidb.apicommon.model.report.bed.util;

import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/* 
 * fields documented in:
 * https://en.wikipedia.org/wiki/BED_(file_format)
 */
public class BedLine {

  /*
   * Needs to be parseable as a float
   * see: htsjdk.tribble.bed.BEDCodec::decode
   */
  private static final String SCORE_FIELD = "0";

  private static Integer locationToZeroBased(Integer loc) {
    return new Integer(loc.intValue() - 1);
  }

  public static List<String> bed6(String featureId, Integer start, Integer end, DeflineBuilder defline, StrandDirection strand){

    Integer zeroBasedStart = locationToZeroBased(start);

    return List.of(featureId, zeroBasedStart.toString(), end.toString(), defline.toString(), SCORE_FIELD, strand.getSign());
  }


  public static List<String> bed12(String featureId, DeflineBuilder defline, StrandDirection strand, List<Integer> subfeatureStarts, List<Integer> subfeatureEnds){
    Integer start = Collections.min(subfeatureStarts);
    Integer end = Collections.max(subfeatureEnds);

    String color = ".";

    Integer numBlocks = subfeatureStarts.size();

    // subfeature coords are 1 based closed here (genomic coords).  when getting length need to add 1
    String blockSizesStr =
      IntStream.range(0, numBlocks)
      .mapToObj(i -> Integer.valueOf(subfeatureEnds.get(i) - subfeatureStarts.get(i) + 1).toString())
      .collect(Collectors.joining(","));

    String blockStartsStr =
      IntStream.range(0, numBlocks)
      .mapToObj(i -> Integer.valueOf(subfeatureStarts.get(i) - start).toString())
      .collect(Collectors.joining(","));

    Integer zeroBasedStart = locationToZeroBased(start);

    Integer cdStart = zeroBasedStart;
    Integer cdEnd = end;

    return List.of(featureId, zeroBasedStart.toString(), end.toString(), defline.toString(), SCORE_FIELD, strand.getSign(), cdStart.toString(), cdEnd.toString(), color, numBlocks.toString(), blockSizesStr, blockStartsStr);

  }
}
