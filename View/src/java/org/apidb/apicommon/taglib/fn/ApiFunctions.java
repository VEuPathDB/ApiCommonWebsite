package org.apidb.apicommon.taglib.fn;

public class ApiFunctions {

  public static String defaultBanner(String currentBanner, String project) {
    if (currentBanner != null && !currentBanner.isEmpty()) {
      return currentBanner;
    }
    switch(project) {
      case "EuPathDB":
        return "EuPathDB : The Eukaryotic Pathogen genome resource";
      case "CryptoDB":
        return "CryptoDB : The Cryptosporidium genome resource";
      case "GiardiaDB":
        return "GiardiaDB : The Giardia genome resource";
      case "PiroplasmaDB":
        return "PiroplasmaDB : The Piroplasma genome resource";
      case "PlasmoDB":
        return "PlasmoDB : The Plasmodium genome resource";
      case "ToxoDB":
        return "ToxoDB : The Toxoplasma genome resource";
      case "TrichDB":
        return "TrichDB : The Trichomonas genome resource";
      case "TriTrypDB":
        return "TriTrypDB : The Kinetoplastid genome resource";
      case "AmoebaDB":
        return "AmoebaDB : The Amoeba genome resource";
      case "MicrosporidiaDB":
        return "MicrosporidiaDB : The Microsporidia genome resource";
      case "HostDB":
        return "HostDB";
      default:
        return null;
    }
  }
}
