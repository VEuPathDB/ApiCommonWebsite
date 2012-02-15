/**
 * 
 */
package org.apidb.apicommon.model.view;

/**
 * @author jerric
 * 
 */
public class DynamicSpanGenomeViewHandler extends GenomeViewHandler {

    private static final String ATTRIBUTE_START = "start_min";
    private static final String ATTRIBUTE_END = "end_max";
    private static final String ATTRIBUTE_SOURCE_ID = "source_id";
    private static final String ATTRIBUTE_SEQUENCE_SOURCE_ID = "seq_source_id";
    private static final String ATTRIBUTE_SEQUENCE_LENGTH = "sequence_length";
    private static final String ATTRIBUTE_STRAND = "strand";

    /**
     * 
     */
    public DynamicSpanGenomeViewHandler() {
        super(ATTRIBUTE_SOURCE_ID, ATTRIBUTE_SEQUENCE_SOURCE_ID,
                ATTRIBUTE_SEQUENCE_LENGTH, ATTRIBUTE_START, ATTRIBUTE_END,
                ATTRIBUTE_STRAND);
    }

}
