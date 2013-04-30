package org.apidb.apicomment.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stand-alone program used to parse GBrowse performance logs.  Input file
 * should be a portion of the Apache error log representing the time over which
 * a request is handled, grepped for 'TIMETEST'.
 * 
 * @author rdoherty
 */
public class GBrowseTimingReport {

  private static final String NL = System.getProperty("line.separator");
  
  public static void main(String[] args) {
    File inputFile = getInputFile(args);
    BufferedReader br = null;
    try {
      Map<String, Pair> events = new LinkedHashMap<>();
      br = new BufferedReader(new FileReader(inputFile));
      while (br.ready()) {
        LogLine line = new LogLine(br.readLine());
        switch (line.type) {
          case BEGIN:
            events.put(line.id, new Pair(line));
            break;
          case END:
            events.get(line.id).addEnd(line);
            break;
        }
      }
      processEvents(events);
    }
    catch (Exception e) {
      throw new RuntimeException("Error running report", e);
    }
    finally {
      if (br != null) { try { br.close(); } catch(Exception e) { } }
    }
  }

  private static File getInputFile(String[] args) {
    if (args.length == 1) {
      return new File(args[0]);
    }
    System.err.println(new StringBuilder("USAGE: java ")
        .append(GBrowseTimingReport.class.getName())
        .append(" <input_file>").append(NL)
        .append("  Input file should be a portion of the Apache error log,")
        .append(" representing the time over which a request is handled,")
        .append(" grepped for 'TIMETEST'.").toString());
    System.exit(1);
    return null; // will never get this far
  }

  private static void processEvents(Map<String, Pair> events) {
    Stats stats = new Stats();
    for (Pair p : events.values()) {
      stats.registerStats(p);
      System.out.println(new StringBuilder()
        .append(p.getId()).append(" ")
        .append(p.isComplete() ? p.getDuration() : "unfinished").toString());
    }
    stats.printStats();
  }
  
  private static class Stats {

    private static String[] EVENT_PREFIXES = { 
      "CGI_Main_",
      "Session_Connect_",
      "Session_Create_",
      "DB_Connect_UserDB_",
      "GBrowse_Login_",
      "ACTION_plugin_authenticate:plugin",
      "ACTION_plugin_authenticate:other",
      "ACTION_authorize_login"
    };
    
    private Double _sessionBeginTime;
    private Double _sessionEndTime;
    Map<String, EventSummary> _statMap = new LinkedHashMap<>();
    
    public void registerStats(Pair p) {
      if (_sessionBeginTime == null) {
        _sessionEndTime = _sessionBeginTime = p.getStart();
      }
      if (p.getEnd() != null && p.getEnd() > _sessionEndTime) {
        _sessionEndTime = p.getEnd();
      }
      String prefix = findPrefix(p);
      EventSummary eventData = _statMap.get(prefix);
      if (eventData == null) {
        eventData = new EventSummary(prefix);
        _statMap.put(prefix, eventData);
      }
      eventData.addEvent(p);
    }  
    
    private String findPrefix(Pair p) {
      for (String prefix : EVENT_PREFIXES) {
        if (p.getId().startsWith(prefix)) {
          return prefix;
        }
      }
      throw new RuntimeException("Misspelled prefix in log!! " + p.getId());
    }

    public void printStats() {
      Double sessionDuration = _sessionEndTime - _sessionBeginTime;
      System.out.println("Entire session took " + sessionDuration + " seconds.");
      System.out.println(EventSummary.getHeader());
      for (String code : _statMap.keySet()) {
        System.out.println(_statMap.get(code));
      }
    }
  }
  
  private static class EventSummary {
    
    private String _prefix;
    private int _numComplete = 0;
    private int _numIncomplete = 0;
    private Double _shortest;
    private Double _longest;
    private Double _totalDuration = 0.0;
    
    public EventSummary(String prefix) {
      _prefix = prefix;
    }
    
    public void addEvent(Pair p) {
      if (p.isComplete()) {
        _numComplete++;
        if (_shortest == null || _shortest > p.getDuration()) {
          _shortest = p.getDuration();
        }
        if (_longest == null || _longest < p.getDuration()) {
          _longest = p.getDuration();
        }
        _totalDuration += p.getDuration();
      }
      else {
        _numIncomplete++;
      }
    }

    public static String getHeader() {
      return "Prefix  #Complete #Incomplete  Shortest  Longest  MeanDuration";
    }
    
    @Override
    public String toString() {
      return _prefix + " " + _numComplete + " " + _numIncomplete + " " +
          _shortest + " " + _longest + " " + (_totalDuration / _numComplete);
    }
  }
    
  private static class Pair {
    private LogLine _begin;
    private LogLine _end;
    public Pair(LogLine begin) {
      _begin = begin;
    }
    public void addEnd(LogLine end) {
      _end = end;
    }
    public Double getStart() { return _begin.startTime; }
    public Double getEnd() { return _end == null ? null : _end.endTime; }
    public String getId() { return _begin.id; }
    public Double getDuration() { return _end.duration; }
    public boolean isComplete() { return _end != null; }
  }
  
  /* Example from log
[Thu Feb 21 15:31:22 2013] [error] [client 74.99.141.48] TIMETEST::Begin Session_Connect_2381 1361478682.73066, referer: http://rdoherty.plasmodb.org/plasmo.rdoherty/?auth_tkt=MjUxZDY5M2M5M2ZiMjRiMzc0MzJjNWVlZGQ3ODYzNTc1MTI0YjMzMmFwaWRiIWFwaWRiITEzNjEzNTk2NjY6MC4wLjAuMA==
[Thu Feb 21 15:31:22 2013] [error] [client 74.99.141.48] TIMETEST::End Session_Connect_2381 1361478682.73066 to 1361478682.7942 = 0.0635359287261963, referer: http://rdoherty.plasmodb.org/plasmo.rdoherty/?auth_tkt=MjUxZDY5M2M5M2ZiMjRiMzc0MzJjNWVlZGQ3ODYzNTc1MTI0YjMzMmFwaWRiIWFwaWRiITEzNjEzNTk2NjY6MC4wLjAuMA==
   */
  private static class LogLine {
    
    public enum EventType { BEGIN, END; }
    
    public EventType type;
    public String id;
    public Double startTime;
    public Double endTime;
    public Double duration;
    
    public LogLine(String line) {
      String[] parts = line.split(" ");
      type = parts[8].contains("Begin") ? EventType.BEGIN : EventType.END;
      id = parts[9];
      startTime = Double.parseDouble(clean(parts[10]));
      if (type.equals(EventType.END)) {
        endTime = Double.parseDouble(parts[12]);
        duration = Double.parseDouble(clean(parts[14]));
      }
    }
    
    @Override
    public String toString() {
      return type + " " + id + ": " + startTime + ", " + endTime + ", " + duration;
    }

    private String clean(String str) {
      return (!str.endsWith(",") ? str : str.substring(0, str.length() - 1));
    }
  }
}
