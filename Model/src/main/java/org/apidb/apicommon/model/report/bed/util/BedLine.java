package org.apidb.apicommon.model.report.bed.util;
import java.util.List;
import java.util.stream.Collectors;
import org.apidb.apicommon.model.report.bed.util.StrandDirection;
import org.apidb.apicommon.model.report.bed.util.DeflineBuilder;

public class BedLine {

  public static List<String> bed6(String featureId, Integer start, Integer end, DeflineBuilder defline, StrandDirection strand){

    return List.of(featureId, start.toString(), end.toString(), defline.toString(), ".", strand.getSign());

  }

  public static List<String> bed12(String featureId, Integer start, Integer end, DeflineBuilder defline, StrandDirection strand, List<Integer> blockSizes, List<Integer> blockStarts){

    String blockSizesStr = blockSizes.stream().map(i -> i.toString()).collect(Collectors.joining(","));
    String blockStartsStr = blockStarts.stream().map(i -> i.toString()).collect(Collectors.joining(","));

    return List.of(featureId, start.toString(), end.toString(), defline.toString(), ".", strand.getSign(), ".", ".", ".", ""+ blockSizes.size(), blockSizesStr, blockStartsStr);

  }
}
