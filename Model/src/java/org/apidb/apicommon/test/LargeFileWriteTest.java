package org.apidb.apicommon.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.gusdb.fgputil.Timer;

public class LargeFileWriteTest {

  private static final long KILOBYTES_PER_GENE = 100;

  public static void main() throws Exception {
    doLargeFileTest(250);
    doLargeFileTest(2500);
    doLargeFileTest(25000);
  }

  public static long getBytes(long numGenes) {
    long numKilobytes = numGenes * KILOBYTES_PER_GENE;
    long numBytes = numKilobytes * 1024;
    return numBytes;
  }

  private static void doLargeFileTest(long numGenes) throws IOException, InterruptedException {
    long numBytes = getBytes(numGenes);
    System.out.println("Will dump file simulating " + numGenes + " genes (" +
        KILOBYTES_PER_GENE + "k each = " + numBytes + " bytes)");
    Timer t = new Timer();
    String testFileName = "/var/tmp/largeFile.txt";
    System.out.println("Writing...");
    try (FileWriter fWriter = new FileWriter(testFileName);
         BufferedWriter writer = new BufferedWriter(fWriter)) {
      for (long i = 0; i < numBytes; i++) {
        writer.write((byte)1);
      }
    }
    System.out.println("File written.  Took " + t.getElapsedStringAndRestart());
    System.out.println("Reading...");
    try (FileReader fReader = new FileReader(testFileName);
         BufferedReader reader = new BufferedReader(fReader)) {
      while (reader.ready()) {
        reader.read();
      }
    }
    System.out.println("File read.  Took " + t.getElapsedStringAndRestart());
    System.out.println("Checking file size in kilobytes:");
    dumpProcessResults("du -k " + testFileName);
    dumpProcessResults("du -h " + testFileName);
    System.out.println("Removing test file...");
    Files.delete(Paths.get(testFileName));
    System.out.println("Done.  Delete took " + t.getElapsedStringAndRestart());
  }

  private static void dumpProcessResults(String execLine) throws IOException, InterruptedException {
    Process p = Runtime.getRuntime().exec(execLine);
    BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
    BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    // read stdout; less likely to blow buffer with stderr
    while (stdout.ready()){
      System.out.println(stdout.readLine());
    }
    while (stderr.ready()) {
      System.err.println(stderr.readLine());
    }
    p.waitFor();
  }
}
