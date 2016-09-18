package org.apidb.apicommon.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.gusdb.fgputil.Timer;

public class LargeFileWriteTest {

  private static final long KILOBYTES_PER_GENE = 100;

  private static final int[] BUFFER_SIZES = {
      8192 /* Java default */,
      32768,
      131072
  };

  private static final boolean CLEAN_UP_FILES = true;
  
  public static void main(String[] args) throws Exception {
    for (int bufferSize : BUFFER_SIZES) {
      System.out.println("\n%%%%%%%%%%% TEST, BUFFER SIZE = " + bufferSize + " %%%%%%%%%%%\n");
      doLargeFileTest(250, bufferSize);
      doLargeFileTest(2500, bufferSize);
      doLargeFileTest(25000, bufferSize);
    }
  }

  public static long getBytes(long numGenes) {
    long numKilobytes = numGenes * KILOBYTES_PER_GENE;
    long numBytes = numKilobytes * 1024;
    return numBytes;
  }

  private static void doLargeFileTest(long numGenes, int bufferSize) throws IOException {
    long numBytes = getBytes(numGenes);
    String testFileName = "/var/tmp/largeFile." + numGenes + "." + bufferSize + ".txt";
    System.out.println("Will dump file '" + testFileName + "' simulating " + numGenes + " genes (" +
        KILOBYTES_PER_GENE + "k each = " + numBytes + " bytes).");
    Timer t = new Timer();
    try (FileWriter fWriter = new FileWriter(testFileName);
         BufferedWriter writer = new BufferedWriter(fWriter, bufferSize)) {
      for (long i = 0; i < numBytes; i++) {
        writer.write((byte)1);
      }
    }
    System.out.println("File written.  Took " + t.getElapsedStringAndRestart());
    try (FileReader fReader = new FileReader(testFileName);
         BufferedReader reader = new BufferedReader(fReader, bufferSize)) {
      while (reader.ready()) {
        reader.read();
      }
    }
    System.out.println("File read.  Took " + t.getElapsedStringAndRestart());
    if (CLEAN_UP_FILES) {
      Files.delete(Paths.get(testFileName));
      System.out.println("File deleted.  Took " + t.getElapsedStringAndRestart());
    }
  }
}
