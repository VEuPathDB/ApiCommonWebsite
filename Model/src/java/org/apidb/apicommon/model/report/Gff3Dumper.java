/**
 * 
 */
package org.apidb.apicommon.model.report;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.wdk.model.Answer;
import org.gusdb.wdk.model.Question;
import org.gusdb.wdk.model.QuestionSet;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
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
    public static void main(String[] args) throws WdkModelException,
            WdkUserException, IOException {
        if (args.length != 4 && args.length != 6) {
            System.err.println("Invalid parameters.");
            printUsage();
            System.exit(-1);
        }
        Map<String, String> cmdArgs = new HashMap<String, String>();
        for (int i = 0; i < args.length - 1; i += 2) {
            cmdArgs.put(args[i].trim().toLowerCase(), args[i + 1].trim());
        }

        // get params
        String modelName = cmdArgs.get("-model");
        String organismArg = cmdArgs.get("-organism");
        String baseDir = cmdArgs.get("-dir");
        if (modelName == null || organismArg == null) {
            System.err.println("Missing parameters.");
            printUsage();
            System.exit(-1);
        }
        if (baseDir == null || baseDir.length() == 0) baseDir = ".";

        // TEST
        System.out.println("Initializing....");

        // load config
        Map<String, String> config = new LinkedHashMap<String, String>();
        config.put(Reporter.FIELD_FORMAT, "text");

        // construct wdkModel
        WdkModel wdkModel = WdkModel.construct(modelName);
        QuestionSet qset = wdkModel.getQuestionSet("DataDumpQuestions");

        String[] organisms = organismArg.split(",");
        for (String organism : organisms) {
            dumpOrganism(qset, organism.trim(), config, baseDir);
        }
        System.out.println("Finished.");
    }

    private static void dumpOrganism(QuestionSet qset, String organism,
            Map<String, String> config, String baseDir)
            throws WdkUserException, WdkModelException, IOException {

        long start = System.currentTimeMillis();
        // TEST
        System.out.println("Collecting sequence data....");

        // ask sequence dumper question
        Question seqQuestion = qset.getQuestion("SequenceGffQuestion");
        Map<String, Object> seqParams = new LinkedHashMap<String, Object>();
        seqParams.put("organism_with_sequences", organism);
        Answer sqlAnswer = seqQuestion.makeAnswer(seqParams, 1, 1);

        ByteArrayOutputStream seqOut = new ByteArrayOutputStream();
        Reporter seqReport = sqlAnswer.createReport("gff3Dump", config);
        seqReport.write(seqOut);
        byte[] seqBuffer = seqOut.toByteArray();

        // TEST
        System.out.println("Collecting gene data....");

        // ask gene dumper question
        Question geneQuestion = qset.getQuestion("GeneGffQuestion");
        Map<String, Object> geneParams = new LinkedHashMap<String, Object>();
        geneParams.put("organism", organism);
        Answer geneAnswer = geneQuestion.makeAnswer(geneParams, 1, 1);

        ByteArrayOutputStream geneOut = new ByteArrayOutputStream();
        config.put(Gff3Reporter.FIELD_HAS_PROTEIN, "yes");
        Reporter geneReport = geneAnswer.createReport("gff3Dump", config);
        geneReport.write(geneOut);
        byte[] geneBuffer = geneOut.toByteArray();

        // decide the path-file name
        File dir = new File(baseDir, organism.replace(' ', '_'));
        if (!dir.exists() || !dir.isDirectory()) dir.mkdirs();
        int pos = organism.indexOf(" ");
        String fileName = organism;
        if (pos >= 0)
            fileName = organism.substring(0, 1).toLowerCase() + "_"
                    + organism.substring(pos + 1);
        fileName += ".gff";
        File gffFile = new File(dir, fileName);

        // merge the result
        BufferedReader seqIn = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(seqBuffer)));
        BufferedReader geneIn = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(geneBuffer)));
        PrintWriter gffOut = new PrintWriter(new FileWriter(gffFile));
        String line;

        // read headers, and annotations from sequence gff
        while ((line = seqIn.readLine()) != null) {
            line = line.trim();
            if (line.equalsIgnoreCase("##FASTA")) break;
            gffOut.println(line);
        }
        gffOut.flush();

        // read annotations from gene gff
        while ((line = geneIn.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("##") && !line.equalsIgnoreCase("##FASTA"))
                continue;
            gffOut.println(line);
        }
        gffOut.flush();

        // append genomic sequence
        while ((line = seqIn.readLine()) != null) {
            line = line.trim();
            gffOut.println(line);
        }
        gffOut.flush();
        gffOut.close();

        // TEST
        System.out.println("GFF3 file saved at " + gffFile.getAbsolutePath()
                + ".");

        long end = System.currentTimeMillis();
        System.out.println("Time spent " + ((end - start) / 1000.0)
                + " seconds.");
    }

    public static void printUsage() {
        System.out.println();
        System.out.println("Usage: gff3Dump -model <model_name> -organism "
                + "<organism_list> [-dir <base_dir>]");
        System.out.println();
        System.out.println("\t\t<model_name>:\tThe name of WDK supported model;");
        System.out.println("\t\t<organism_list>: a list of organism names, "
                + "delimited by a comma;");
        System.out.println("\t\t<base_dir>: Optional, the base directory for "
                + "the output files. If not specified, the current directory "
                + "will be used.");
        System.out.println();
    }
}
