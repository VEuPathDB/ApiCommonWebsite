package org.apidb.apicommon.model.report.singlegeneformats;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apidb.apicommon.model.report.SingleGeneReporter.Format;

public class Formats {

  public static final Map<String, Supplier<Format>> FORMAT_MAP = new HashMap<>(){{

    // Add new formats here
    // key is the format name clients can specify in SingleGeneReporter reportConfig

    put("apolloGoTerm", () -> new ApolloGoTermFormat());

  }};

}
