/**
 * 
 */
package org.apidb.apicommon.model.report;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.Answer;
import org.gusdb.wdk.model.AttributeFieldValue;
import org.gusdb.wdk.model.RecordInstance;
import org.gusdb.wdk.model.TableFieldValue;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.report.Reporter;

/**
 * @author xingao
 * 
 */
public class Gff3Reporter extends Reporter {
    
    public final static String FIELD_HAS_TRANSCRIPT = "hasTranscript";
    public final static String FIELD_HAS_PROTEIN = "hasProtein";
    public final static String FIELD_HAS_CODING_SEQUENCE = "hasCodingSequence";
    
    private static final String newline = System.getProperty( "line.separator" );
    
    private static Logger logger = Logger.getLogger( Gff3Reporter.class );
    
    private boolean hasTranscript = false;
    private boolean hasProtein = false;
    private boolean hasCodingSequence = false;
    
    public Gff3Reporter( Answer answer ) {
        super( answer );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.Reporter#configure(java.util.Map)
     */
    @Override
    public void configure( Map< String, String > config ) {
        super.configure( config );
        
        // include transcript
        if ( config.containsKey( FIELD_HAS_TRANSCRIPT ) ) {
            String value = config.get( FIELD_HAS_TRANSCRIPT );
            hasTranscript = ( value.equalsIgnoreCase( "yes" ) || value.equalsIgnoreCase( "true" ) ) ? true
                    : false;
        }
        
        // include protein
        if ( config.containsKey( FIELD_HAS_PROTEIN ) ) {
            String value = config.get( FIELD_HAS_PROTEIN );
            hasProtein = ( value.equalsIgnoreCase( "yes" ) || value.equalsIgnoreCase( "true" ) ) ? true
                    : false;
        }
        
        // include coding sequence
        if ( config.containsKey( FIELD_HAS_CODING_SEQUENCE ) ) {
            String value = config.get( FIELD_HAS_CODING_SEQUENCE );
            hasCodingSequence = ( value.equalsIgnoreCase( "yes" ) || value.equalsIgnoreCase( "true" ) ) ? true
                    : false;
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.Reporter#getHttpContentType()
     */
    @Override
    public String getHttpContentType() {
        if ( format.equalsIgnoreCase( "text" ) ) {
            return "text/plain";
        } else { // use the default content type defined in the parent class
            return super.getHttpContentType();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.Reporter#getDownloadFileName()
     */
    @Override
    public String getDownloadFileName() {
        logger.info( "Internal format: " + format );
        String name = answer.getQuestion().getName();
        if ( format.equalsIgnoreCase( "text" ) ) {
            return name + ".gff";
        } else { // use the defaul file name defined in the parent
            return super.getDownloadFileName();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.report.IReporter#format(org.gusdb.wdk.model.Answer)
     */
    public void write( OutputStream out ) throws WdkModelException {
        StringBuffer header = new StringBuffer();
        
        // output the header
        header.append( "##gff-version\t3" + newline );
        header.append( "##feature-ontology\tso.obo" + newline );
        header.append( "##attribute-ontology\tgff3_attributes.obo" + newline );
        
        // iterate on each record, and format the result
        StringBuffer annotation = new StringBuffer();
        StringBuffer fasta = new StringBuffer();
        
        PrintWriter writer = new PrintWriter( new OutputStreamWriter( out ) );
        
        String rcName = answer.getQuestion().getRecordClass().getFullName();
        if ( rcName.equals( "GeneRecordClasses.GeneRecordClass" ) ) {
            StringBuffer transcript = new StringBuffer();
            StringBuffer protein = new StringBuffer();
            StringBuffer codingSequence = new StringBuffer();
            Map< String, int[ ] > sequenceIds = new LinkedHashMap< String, int[ ] >();
           
            while ( answer.hasMoreRecordInstances() ) {
                RecordInstance record = answer.getNextRecordInstance();
                formatGeneRecord( record, annotation, transcript, protein,
                        codingSequence, sequenceIds );
            }
            
            // put sequence id into the header
            for ( String seqId : sequenceIds.keySet() ) {
                int[ ] region = sequenceIds.get( seqId );
                header.append( "##sequence-region\t" + seqId + "\t"
                        + region[ 0 ] + "\t" + region[ 1 ] + newline );
            }
            
            // put sequences together
            if ( hasTranscript || hasProtein || hasCodingSequence )
                fasta.append( "##FASTA" + newline );
            if ( hasTranscript ) fasta.append( transcript );
            if ( hasProtein ) fasta.append( protein );
            if ( hasCodingSequence ) fasta.append( codingSequence );
        } else {
            while ( answer.hasMoreRecordInstances() ) {
                RecordInstance record = answer.getNextRecordInstance();
                formatSequenceRecord( record, header, annotation, fasta );
            }
            annotation.append( "##FASTA" + newline );
        }
        writer.print( header );
        writer.flush();
        writer.print( annotation );
        writer.flush();
        writer.print( fasta );
        writer.flush();
    }
    
    private void formatGeneRecord( RecordInstance record,
            StringBuffer annotation, StringBuffer transcript,
            StringBuffer protein, StringBuffer codingSequence,
            Map< String, int[ ] > sequenceIds ) throws WdkModelException {
        // get sequence id
        String seqId = getValue( record.getAttributeValue( "gff_seqid" ) );
        int start = Integer.parseInt( getValue( record.getAttributeValue( "gff_fstart" ) ) );
        int stop = Integer.parseInt( getValue( record.getAttributeValue( "gff_fend" ) ) );
        if ( sequenceIds.containsKey( seqId ) ) {
            int[ ] region = sequenceIds.get( seqId );
            if ( region[ 0 ] > start ) region[ 0 ] = start;
            if ( region[ 1 ] < stop ) region[ 1 ] = stop;
            sequenceIds.put( seqId, region );
        } else {
            int[ ] region = { start, stop };
            sequenceIds.put( seqId, region );
        }
        
        // print the attributes for the record
        readCommonFields( record, annotation );
        
        // get GO terms
        TableFieldValue goTerms = record.getTableValue( "GoTerms" );
        Iterator it = goTerms.getRows();
        StringBuffer sbGoTerms = new StringBuffer();
        while ( it.hasNext() ) {
            Map< String, Object > row = ( Map< String, Object > ) it.next();
            String goTerm = getValue( row.get( "go_id" ) ).trim();
            if ( sbGoTerms.length() > 0 ) sbGoTerms.append( "," );
            sbGoTerms.append( goTerm );
        }
        goTerms.getClose();
        
        // get aliases
        TableFieldValue alias = record.getTableValue( "Aliases" );
        StringBuffer sbAlias = new StringBuffer();
        it = alias.getRows();
        while ( it.hasNext() ) {
            Map< String, Object > row = ( Map< String, Object > ) it.next();
            String alias_value = getValue( row.get( "alias" ) ).trim();
            if ( sbAlias.length() > 0 ) sbAlias.append( "," );
            sbAlias.append( alias_value );
        }
        alias.getClose();
        if ( sbAlias.length() > 0 )
            annotation.append( ";Alias=" + sbAlias.toString() );
        
        // get dbxref terms
        TableFieldValue dbxrefs = record.getTableValue( "gff_GeneDbxrefs" );
        StringBuffer sbDbxrefs = new StringBuffer();
        it = dbxrefs.getRows();
        while ( it.hasNext() ) {
            Map< String, Object > row = ( Map< String, Object > ) it.next();
            String dbxref_value = getValue( row.get( "gff_dbxref" ) ).trim();
            if ( sbDbxrefs.length() > 0 ) sbDbxrefs.append( "," );
            sbDbxrefs.append( dbxref_value );
        }
        dbxrefs.getClose();
        
        annotation.append( newline );
        
        // print RNAs
        TableFieldValue rnas = record.getTableValue( "gff_GeneRnas" );
        it = rnas.getRows();
        while ( it.hasNext() ) {
            Map< String, Object > row = ( Map< String, Object > ) it.next();
            String rnaId = getValue( row.get( "gff_attr_id" ) );
            
            // read common fields
            readCommonFields( row, annotation );
            // read other fields
            annotation.append( ";Parent=" + readField( row, "gff_attr_parent" ) );
            
            // add GO terms in mRNA
            if ( sbGoTerms.length() > 0 )
                annotation.append( ";Ontology_term=" + sbGoTerms.toString() );
            
            // add dbxref in mRNA
            if ( sbDbxrefs.length() > 0 )
                annotation.append( ";Dbxref=" + sbDbxrefs.toString() );
            
            annotation.append( newline );
            
            String sequence = getValue( row.get( "gff_transcript_sequence" ) );
            if ( hasTranscript && sequence != null ) {
                transcript.append( ">" + rnaId + newline );
                transcript.append( formatSequence( sequence ) );
            }
        }
        rnas.getClose();
        
        // print CDSs
        TableFieldValue cdss = record.getTableValue( "gff_GeneCdss" );
        it = cdss.getRows();
        while ( it.hasNext() ) {
            Map< String, Object > row = ( Map< String, Object > ) it.next();
            String cdsId = getValue( row.get( "gff_attr_id" ) );
            
            // read common fields
            readCommonFields( row, annotation );
            // read other fields
            annotation.append( ";Parent=" + readField( row, "gff_attr_parent" ) );
            
            annotation.append( newline );
            
            String sequence = getValue( row.get( "gff_coding_sequence" ) );
            if ( hasCodingSequence && sequence != null ) {
                codingSequence.append( ">" + cdsId + newline );
                codingSequence.append( formatSequence( sequence ) );
            }
            sequence = getValue( row.get( "gff_protein_sequence" ) );
            if ( hasProtein && sequence != null ) {
                protein.append( ">" + cdsId + newline );
                protein.append( formatSequence( sequence ) );
            }
        }
        cdss.getClose();
        
        // print EXONs
        TableFieldValue exons = record.getTableValue( "gff_GeneExons" );
        it = exons.getRows();
        while ( it.hasNext() ) {
            Map< String, Object > row = ( Map< String, Object > ) it.next();
            
            // read common fields
            readCommonFields( row, annotation );
            // read other fields
            annotation.append( ";Parent=" + readField( row, "gff_attr_parent" ) );
            
            annotation.append( newline );
        }
        exons.getClose();
    }
    
    private void formatSequenceRecord( RecordInstance record,
            StringBuffer header, StringBuffer annotation,
            StringBuffer genomeSequence ) throws WdkModelException {
        // print the attributes for the record
        String seqId = getValue( record.getAttributeValue( "gff_seqid" ) );
        String fstart = getValue( record.getAttributeValue( "gff_fstart" ) );
        String fend = getValue( record.getAttributeValue( "gff_fend" ) );
        
        // output header
        header.append( "##sequence-region\t" + seqId + "\t" + fstart + "\t"
                + fend + newline );
        
        // read common fields
        readCommonFields( record, annotation );
        
        // read other fields
        annotation.append( ";molecule_type="
                + readField( record, "gff_attr_molecule_type" ) );
        annotation.append( ";organism_name="
                + readField( record, "gff_attr_organism_name" ) );
        annotation.append( ";translation_table="
                + readField( record, "gff_attr_translation_table" ) );
        annotation.append( ";topology="
                + readField( record, "gff_attr_topology" ) );
        annotation.append( ";localization="
                + readField( record, "gff_attr_localization" ) );
        
        // get dbxref terms
        TableFieldValue dbxrefs = record.getTableValue( "gff_SequenceDbxrefs" );
        StringBuffer sbDbxrefs = new StringBuffer();
        Iterator it = dbxrefs.getRows();
        while ( it.hasNext() ) {
            Map< String, Object > row = ( Map< String, Object > ) it.next();
            String dbxref_value = getValue( row.get( "gff_dbxref" ) ).trim();
            if ( sbDbxrefs.length() > 0 ) sbDbxrefs.append( "," );
            sbDbxrefs.append( dbxref_value );
        }
        dbxrefs.getClose();
        if ( sbDbxrefs.length() > 0 )
            annotation.append( ";Dbxref=" + sbDbxrefs.toString() );
        
        annotation.append( newline );
        
        // output genome sequence in fasta
        genomeSequence.append( ">" + seqId + newline );
        genomeSequence.append( formatSequence( getValue( record.getAttributeValue( "gff_sequence" ) ) ) );
    }
    
    private void readCommonFields( Object object, StringBuffer buffer )
            throws WdkModelException {
        buffer.append( readField( object, "gff_seqid" ) + "\t" );
        buffer.append( readField( object, "gff_source" ) + "\t" );
        buffer.append( readField( object, "gff_type" ) + "\t" );
        buffer.append( readField( object, "gff_fstart" ) + "\t" );
        buffer.append( readField( object, "gff_fend" ) + "\t" );
        buffer.append( readField( object, "gff_score" ) + "\t" );
        buffer.append( readField( object, "gff_strand" ) + "\t" );
        buffer.append( readField( object, "gff_phase" ) + "\t" );
        buffer.append( "ID=" + readField( object, "gff_attr_id" ) );
        
        String name = readField( object, "gff_attr_name" );
        if ( name != null ) try {
            buffer.append( ";Name=" + URLEncoder.encode( name, "utf-8" ) );
        } catch ( UnsupportedEncodingException ex ) {
            ex.printStackTrace();
        }
        String description = readField( object, "gff_attr_description" );
        if ( description != null )
            try {
                buffer.append( ";description="
                        + URLEncoder.encode( description, "utf-8" ) );
            } catch ( UnsupportedEncodingException ex ) {
                ex.printStackTrace();
            }
        String locusTag = readField( object, "gff_attr_locus_tag" );
        if ( locusTag != null ) buffer.append( ";locus_tag=" + locusTag );
        String size = readField( object, "gff_attr_size" );
        if ( size != null ) buffer.append( ";size=" + size );
    }
    
    private String readField( Object object, String field )
            throws WdkModelException {
        Object value;
        if ( object instanceof RecordInstance ) {
            RecordInstance record = ( RecordInstance ) object;
            value = record.getAttributeValue( field );
        } else {
            Map< String, Object > row = ( Map< String, Object > ) object;
            value = row.get( field );
        }
        return getValue( value );
    }
    
    private String getValue( Object object ) {
        String value;
        if ( object instanceof AttributeFieldValue ) {
            AttributeFieldValue attrVal = ( AttributeFieldValue ) object;
            value = attrVal.getValue().toString();
        } else if ( object == null ) return null;
        else {
            value = object.toString();
        }
        value = value.trim();
        if ( value.length() == 0 ) return null;
        return value;
    }
    
    private String formatSequence( String sequence ) {
        if ( sequence == null ) return null;
        
        StringBuffer buffer = new StringBuffer();
        int offset = 0;
        while ( offset < sequence.length() ) {
            int endp = offset + Math.min( 60, sequence.length() - offset );
            buffer.append( sequence.substring( offset, endp ) + newline );
            offset = endp;
        }
        return buffer.toString();
    }
}
