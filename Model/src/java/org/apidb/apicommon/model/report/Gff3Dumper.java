/**
 * 
 */
package org.apidb.apicommon.model.report;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.wdk.model.Answer;
import org.gusdb.wdk.model.Question;
import org.gusdb.wdk.model.QuestionSet;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.implementation.Oracle;
import org.gusdb.wdk.model.report.Reporter;

/**
 * @author xingao
 * 
 */
public class Gff3Dumper {
    
    /**
     * @param args
     * @throws WdkModelException
     * @throws WdkUserException
     * @throws IOException
     */
    public static void main( String[ ] args ) throws WdkModelException,
            WdkUserException, IOException {
        if ( args.length != 4 && args.length != 6 ) {
            System.err.println( "Invalid parameters." );
            printUsage();
            System.exit( -1 );
        }
        Map< String, String > cmdArgs = new HashMap< String, String >();
        cmdArgs.put( args[ 0 ].trim().toLowerCase(), args[ 1 ].trim() );
        cmdArgs.put( args[ 2 ].trim().toLowerCase(), args[ 3 ].trim() );
        if ( args.length == 6 )
            cmdArgs.put( args[ 4 ].trim().toLowerCase(), args[ 5 ].trim() );
        
        // get params
        String modelName = cmdArgs.get( "-model" );
        String organismArg = cmdArgs.get( "-organism" );
        String configFile = cmdArgs.get( "-config" );
        if ( modelName == null || organismArg == null ) {
            System.err.println( "Missing parameters." );
            printUsage();
            System.exit( -1 );
        }
        
        // TEST
        System.out.println( "Initializing...." );
        
        // load config
        Map< String, String > config = loadConfiguration( configFile );
        
        // construct wdkModel
        WdkModel wdkModel = WdkModel.construct( modelName );
        QuestionSet qset = wdkModel.getQuestionSet( "DataDumpQuestions" );
        
        String[ ] organisms = organismArg.split( "," );
        for ( String organism : organisms ) {
            dumpOrganism( qset, organism.trim(), config );
        }
        System.out.println("Finished.");
    }
    
    private static void dumpOrganism( QuestionSet qset, String organism,
            Map< String, String > config ) throws WdkUserException,
            WdkModelException, IOException {
        
        long start = System.currentTimeMillis();
        // TEST
        System.out.println( "Collecting sequence data...." );
        
        // ask sequence dumper question
        Question seqQuestion = qset.getQuestion( "SequenceGffQuestion" );
        Map< String, Object > seqParams = new LinkedHashMap< String, Object >();
        seqParams.put( "organism_with_sequences", organism );
        Answer sqlAnswer = seqQuestion.makeAnswer( seqParams, 1, 1 );
        
        ByteArrayOutputStream seqOut = new ByteArrayOutputStream();
        Reporter seqReport = sqlAnswer.createReport( "gff3", config );
        seqReport.write( seqOut );
        byte[ ] seqBuffer = seqOut.toByteArray();
        
        // TEST
        System.out.println( "Collecting gene data...." );
        
        // ask gene dumper question
        Question geneQuestion = qset.getQuestion( "GeneGffQuestion" );
        Map< String, Object > geneParams = new LinkedHashMap< String, Object >();
        geneParams.put( "organism", organism );
        Answer geneAnswer = geneQuestion.makeAnswer( geneParams, 1, 1 );
        
        ByteArrayOutputStream geneOut = new ByteArrayOutputStream();
        config.put( Gff3Reporter.FIELD_HAS_PROTEIN, "yes" );
        Reporter geneReport = geneAnswer.createReport( "gff3", config );
        geneReport.write( geneOut );
        byte[ ] geneBuffer = geneOut.toByteArray();
        
        // merge the result
        StringBuffer name = new StringBuffer();
        int pos = organism.indexOf( " " );
        String fileName = ( pos < 0 ) ? organism
                : organism.substring( 0, 1 ).toLowerCase()
                        + organism.substring( pos );
        fileName = fileName.replaceAll( "\\s+", "_" );
        File gffFile = new File( fileName + ".gff" );
        if ( !gffFile.exists() ) gffFile.createNewFile();
        BufferedReader seqIn = new BufferedReader( new InputStreamReader(
                new ByteArrayInputStream( seqBuffer ) ) );
        BufferedReader geneIn = new BufferedReader( new InputStreamReader(
                new ByteArrayInputStream( geneBuffer ) ) );
        PrintWriter gffOut = new PrintWriter( new FileWriter( gffFile ) );
        String line;
        
        // read headers, and annotations from sequence gff
        while ( ( line = seqIn.readLine() ) != null ) {
            line = line.trim();
            if ( line.equalsIgnoreCase( "##FASTA" ) ) break;
            gffOut.println( line );
        }
        gffOut.flush();
        
        // read annotations from gene gff
        while ( ( line = geneIn.readLine() ) != null ) {
            line = line.trim();
            if ( line.startsWith( "##" ) && !line.equalsIgnoreCase( "##FASTA" ) )
                continue;
            gffOut.println( line );
        }
        gffOut.flush();
        
        // append genomic sequence
        while ( ( line = seqIn.readLine() ) != null ) {
            line = line.trim();
            gffOut.println( line );
        }
        gffOut.flush();
        gffOut.close();
        
        // TEST
        System.out.println( "GFF3 file saved at " + gffFile.getAbsolutePath()
                + "." );
        
        long end = System.currentTimeMillis();
        System.out.println( "Time spent " + ( ( end - start ) / 1000.0 )
                + " seconds." );
    }
    
    public static void printUsage() {
        System.out.println();
        System.out.println( "Usage: gff3Dump -model <model_name> -organism <organism_list>" );
        System.out.println();
        System.out.println( "\t\t<model_name>:\tThe name of WDK supported model" );
        System.out.println( "\t\t<organism_list>: a list of organism names, delimited by a comma" );
        System.out.println();
    }
    
    private static Map< String, String > loadConfiguration(
            String configFileName ) throws IOException {
        Map< String, String > config = new LinkedHashMap< String, String >();
        
        if ( configFileName == null || configFileName.length() == 0 )
            return config;
        
        File configFile = new File( configFileName );
        BufferedReader in = new BufferedReader( new InputStreamReader(
                new FileInputStream( configFile ) ) );
        String line;
        while ( ( line = in.readLine() ) != null ) {
            if ( line.trim().length() == 0 ) continue;
            if ( line.charAt( 0 ) == '#' ) continue;
            int pos = line.indexOf( "=" );
            if ( pos < 0 ) config.put( line, null );
            else config.put( line.substring( 0, pos ), line.substring( pos + 1 ) );
        }
        config.put( Reporter.FIELD_FORMAT, "text" );
        return config;
    }
    
}
