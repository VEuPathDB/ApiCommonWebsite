package org.apidb.apicommon.model.report.bed.util;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;

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

  public static List<String> bed6(String featureId, Integer start, Integer end, DeflineBuilder defline, StrandDirection strand){

    return List.of(featureId, start.toString(), end.toString(), defline.toString(), SCORE_FIELD, strand.getSign());
  }

  public static List<String> bed12(String featureId, DeflineBuilder defline, StrandDirection strand, List<Integer> subfeatureStarts, List<Integer> subfeatureEnds){
    Integer start = Collections.min(subfeatureStarts);
    Integer end = Collections.max(subfeatureEnds);

    Integer cdStart = start;
    Integer cdEnd = end;
    String color = ".";

    Integer numBlocks = subfeatureStarts.size();
    
    String blockSizesStr =
      IntStream.range(0, numBlocks)
      .mapToObj(i -> new Integer(subfeatureEnds.get(i) - subfeatureStarts.get(i)).toString())
      .collect(Collectors.joining(","));

    String blockStartsStr =
      IntStream.range(0, numBlocks)
      .mapToObj(i -> new Integer(subfeatureStarts.get(i) - start).toString())
      .collect(Collectors.joining(","));


    return List.of(featureId, start.toString(), end.toString(), defline.toString(), SCORE_FIELD, strand.getSign(), cdStart.toString(), cdEnd.toString(), color, numBlocks.toString(), blockSizesStr, blockStartsStr);

  }
}
