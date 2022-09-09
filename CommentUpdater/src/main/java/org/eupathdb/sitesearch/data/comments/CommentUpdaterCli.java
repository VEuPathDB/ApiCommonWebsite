package org.eupathdb.sitesearch.data.comments;

public class CommentUpdaterCli {

  protected static void validateEnv(String[] envVarKeys) {
    final var env = System.getenv();
    String msg = "Error. Missing at least one required environment variable: " + String.join(", ", envVarKeys);
    for (String key : envVarKeys) {
      if (!env.containsKey(key)) {
        System.err.println(msg);
        System.exit(1);
      }
    }
  }
}
