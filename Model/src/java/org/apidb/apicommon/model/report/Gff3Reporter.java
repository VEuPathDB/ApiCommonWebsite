/**
 * 
 */
package org.apidb.apicommon.model.report;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
            fasta.append( "##FASTA" + newline );
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
        if ( sbGoTerms.length() > 0 )
            annotation.append( ";Ontology_term=" + sbGoTerms.toString() );
        
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
        if ( sbDbxrefs.length() > 0 )
            annotation.append( ";Dbxref=" + sbDbxrefs.toString() );
        
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
            annotation.append( ";Parent=" );
            readField( row, "attr_parent", annotation );
            
            annotation.append( newline );
            
            String sequence = getValue( row.get( "gff_transcript_sequence" ) );
            if ( hasTranscript && sequence != null ) {
                transcript.append( ">" + rnaId + newline );
                transcript.append( formatSequence(sequence) );
            }
            sequence = getValue( row.get( "gff_protein_sequence" ) );
            if ( hasProtein  && sequence != null ) {
                protein.append( ">" + rnaId + newline );
                protein.append( formatSequence( sequence ) );
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
            annotation.append( ";Parent=" );
            readField( row, "attr_parent", annotation );
            
            annotation.append( newline );
            
            String sequence = getValue( row.get( "gff_coding_sequence" ) );
            if ( hasCodingSequence && sequence != null ) {
                transcript.append( ">" + cdsId + newline );
                transcript.append( formatSequence(sequence) );
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
            annotation.append( ";Parent=" );
            readField( row, "attr_parent", annotation );
            
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
        annotation.append( ";molecule_type=" );
        readField( record, "gff_attr_molecule_type", annotation );
        annotation.append( ";organism_name=" );
        readField( record, "gff_attr_organism_name", annotation );
        annotation.append( ";translation_table=" );
        readField( record, "gff_attr_translation_table", annotation );
        annotation.append( ";topology=" );
        readField( record, "gff_attr_topology", annotation );
        annotation.append( ";localization=" );
        readField( record, "gff_attr_localization", annotation );
        
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
        genomeSequence.append( formatSequence( getValue( record.getAttributeValue( "gff_sequence" ) ) ) );
    }
    
    private void readCommonFields( Object object, StringBuffer buffer )
            throws WdkModelException {
        readField( object, "gff_seqid", buffer );
        buffer.append( '\t' );
        readField( object, "gff_source", buffer );
        buffer.append( '\t' );
        readField( object, "gff_type", buffer );
        buffer.append( '\t' );
        readField( object, "gff_fstart", buffer );
        buffer.append( '\t' );
        readField( object, "gff_fend", buffer );
        buffer.append( '\t' );
        readField( object, "gff_score", buffer );
        buffer.append( '\t' );
        readField( object, "gff_strand", buffer );
        buffer.append( '\t' );
        readField( object, "gff_phase", buffer );
        buffer.append( "\tID=" );
        readField( object, "gff_attr_id", buffer );
        buffer.append( ";Name=" );
        readField( object, "gff_attr_name", buffer );
        buffer.append( ";description=" );
        readField( object, "gff_attr_description", buffer );
        buffer.append( ";locus_tag=" );
        readField( object, "gff_attr_locus_tag", buffer );
        buffer.append( ";size=" );
        readField( object, "gff_attr_size", buffer );
    }
    
    private void readField( Object object, String field, StringBuffer buffer )
            throws WdkModelException {
        Object value;
        if ( object instanceof RecordInstance ) {
            RecordInstance record = ( RecordInstance ) object;
            value = record.getAttributeValue( field );
        } else {
            Map< String, Object > row = ( Map< String, Object > ) object;
            value = row.get( field );
        }
        buffer.append( getValue( value ) );
    }
    
    private String getValue( Object object ) {
        if ( object instanceof AttributeFieldValue ) {
            AttributeFieldValue attrVal = ( AttributeFieldValue ) object;
            return attrVal.getValue().toString();
        } else if ( object == null ) return null;
        else return object.toString();
    }
    
    private String formatSequence( String sequence ) {
        if (sequence == null) return null;
        
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
