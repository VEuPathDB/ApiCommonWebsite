/**
 * 
 */
package org.apidb.apicommon.model.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.Answer;
import org.gusdb.wdk.model.Field;
import org.gusdb.wdk.model.Question;
import org.gusdb.wdk.model.QuestionSet;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.report.FullRecordReporter;
import org.gusdb.wdk.model.report.Reporter;

/**
 * @author xingao
 * 
 */
public class RecordDumper {
    
    private static final Logger logger = Logger.getLogger(RecordDumper.class);
    
    /**
     * @param args
     * @throws WdkModelException
     * @throws WdkUserException
     * @throws IOException
     */
    public static void main( String[ ] args ) throws WdkModelException,
            WdkUserException, IOException {
        if ( args.length != 6 && args.length != 8 ) {
            System.err.println( "Invalid parameters." );
            printUsage();
            System.exit( -1 );
        }
        Map< String, String > cmdArgs = new HashMap< String, String >();
        for ( int i = 0; i < args.length - 1; i += 2 ) {
            cmdArgs.put( args[ i ].trim().toLowerCase(), args[ i + 1 ].trim() );
        }
        
        // get params
        String modelName = cmdArgs.get( "-model" );
        String organismArg = cmdArgs.get( "-organism" );
        String typeArg = cmdArgs.get( "-type" );
        String baseDir = cmdArgs.get( "-dir" );
        if ( modelName == null || organismArg == null ) {
            System.err.println( "Missing parameters." );
            printUsage();
            System.exit( -1 );
        }
        if ( baseDir == null || baseDir.length() == 0 ) baseDir = ".";
        
        // TEST
        System.out.println( "Initializing...." );
        
        // construct wdkModel
        WdkModel model = WdkModel.construct( modelName );
        QuestionSet qset = model.getQuestionSet( "DataDumpQuestions" );
        
        // get type list
        String[ ] types = typeArg.split( "," );
        for ( String type : types ) {
            type = type.trim();
            if ( !type.equalsIgnoreCase( "sequence" ) && !type.equalsIgnoreCase( "gene" ) ) {
                System.err.println( "Unsupported record type: " + type );
                printUsage();
                System.exit( -1 );
            }
        }
        
        String[ ] organisms = organismArg.split( "," );
        for ( String organism : organisms ) {
            for ( String type : types ) {
                dumpOrganism( qset, organism.trim(), type.trim(), baseDir );
            }
        }
        System.out.println( "Finished." );
    }
    
    private static void dumpOrganism( QuestionSet qset, String organism,

    String type, String baseDir ) throws WdkUserException, WdkModelException,
            IOException {
        long start = System.currentTimeMillis();
        
        // TEST
        logger.info( "Dumping " + type + " records for " + organism
                + "..." );
        
        // decide which question to use, and the name of the parameter.
        Question question = null;
        String organismParam = "organism";
        String reporterName = null;
        if ( type.equalsIgnoreCase( "gene" ) ) {
            question = qset.getQuestion( "GeneGffQuestion" );
            reporterName = "fullRecordDump";
        } else if ( type.equalsIgnoreCase( "sequence" ) ) {
            question = qset.getQuestion( "SequenceGffQuestion" );
            reporterName = "fullRecord";
        } else { // something wrong here, not supported record type
            throw new WdkModelException( "Unsupported Record Type: " + type );
        }
        
        // get report maker attributes and tables
        Map< String, Field > fields = question.getReportMakerFields();
        StringBuffer sbFields = new StringBuffer();
        for ( String fieldName : fields.keySet() ) {
            if ( sbFields.length() > 0 ) sbFields.append( "," );
            sbFields.append( fieldName );
        }
        
        // make the configuration for the reporter
        Map< String, String > config = new LinkedHashMap< String, String >();
        config.put( Reporter.FIELD_FORMAT, "text" );
        config.put( FullRecordReporter.FIELD_SELECTED_COLUMNS,
                sbFields.toString() );
        config.put( FullRecordReporter.FIELD_HAS_EMPTY_TABLE, "yes" );
        
        // ask the question
        Map< String, Object > params = new LinkedHashMap< String, Object >();
        params.put( organismParam, organism );
        Answer sqlAnswer = question.makeAnswer( params, 1, 1 );
        
        // decide the path-file name
        File dir = new File( baseDir, organism.replace( ' ', '_' ) );
        if ( !dir.exists() || !dir.isDirectory() ) dir.mkdirs();
        int pos = organism.indexOf( " " );
        String fileName = organism;
        if ( pos >= 0 )
            fileName = organism.substring( 0, 1 ).toLowerCase() + "_"
                    + organism.substring( pos + 1 );
        fileName += "_" + type + ".txt";
        File file = new File( dir, fileName );
        
        // output the result
        OutputStream out = new FileOutputStream( file );
        Reporter seqReport = sqlAnswer.createReport( reporterName, config );
        seqReport.write( out );
        out.close();
        
        // TEST
        System.out.println( "Dump file saved at " + file.getAbsolutePath()
                + "." );
        
        long end = System.currentTimeMillis();
        System.out.println( "Time spent " + ( ( end - start ) / 1000.0 )
                + " seconds." );
    }
    
    public static void printUsage() {
        System.out.println();
        System.out.println( "Usage: wdkRecordDump -model <model_name> -organism "
                + "<organism_list> -type <record_type_list> [-dir <base_dir>]" );
        System.out.println();
        System.out.println( "\t\t<model_name>:\tThe name of WDK supported model" );
        System.out.println( "\t\t<organism_list>: a list of organism names, "
                + "delimited by a comma;" );
        System.out.println( "\t\t<record_type_list>: a list of record types, "
                + "current support: gene, sequence." );
        System.out.println( "\t\t<base_dir>: Optional, the base directory for "
                + "the output files. If not specified, the current directory "
                + "will be used." );
        System.out.println();
    }
}
