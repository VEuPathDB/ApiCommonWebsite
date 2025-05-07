package org.apidb.apicommon.service.services.jbrowse;

import static org.gusdb.wdk.service.FileRanges.getFileChunkResponse;
import static org.gusdb.wdk.service.FileRanges.parseRangeHeaderValue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkRuntimeException;
import org.gusdb.wdk.service.service.AbstractWdkService;
import org.json.JSONObject;

@Path("/jbrowse")
public class JBrowseService extends AbstractWdkService {

    public static String appType = "jbrowse";

    @GET
    @Path("stats/global")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseGlobalStats(@SuppressWarnings("unused") @QueryParam("feature") String feature) {
        return Response.ok(new JSONObject().put("featureDensity", 0.0002).toString()).build();
    }

    @GET
    @Path("stats/regionFeatureDensities/{refseq_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseRegionFeatureDensities(@PathParam("refseq_name") String refseqName, 
                                                     @Context UriInfo uriInfo,
                                                     @QueryParam("feature") String feature,
                                                     @QueryParam("start") String start,
                                                     @QueryParam("end") String end) {
        return new JBrowseFeatureDataFactory(getWdkModel())
            .featuresAndRegionStats(refseqName, uriInfo, feature, Long.valueOf(start), Long.valueOf(end));
    }

    @GET
    @Path("features/{refseq_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseFeatures(@PathParam("refseq_name") String refseqName, 
                                       @Context UriInfo uriInfo,
                                       @QueryParam("feature") String feature,
                                       @QueryParam("start") String start,
                                       @QueryParam("end") String end) {
      return new JBrowseFeatureDataFactory(getWdkModel())
          .featuresAndRegionStats(refseqName, uriInfo, feature, Long.valueOf(start), Long.valueOf(end));
    }

    @GET
    @Path("dnaseq/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseDNASeqTracks(@PathParam("organismAbbrev") String organismAbbrev) throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();
        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseDNASeqTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(buildNumber);
        command.add(webservicesDir);
        command.add(appType);

        return responseFromCommand(command);
    }

    @GET
    @Path("rnaseqJunctions/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseRNASeqJunctionTracks(@PathParam("organismAbbrev") String organismAbbrev, 
                                                   @DefaultValue("0") @QueryParam("isApollo") String isApollo
                                                   )  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseRNASeqJunctionTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(isApollo);
        command.add(appType);

        return responseFromCommand(command);
    }


    @GET
    @Path("{tracks}/{publicOrganismAbbrev}/aa/trackList.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseAaTracks(@PathParam("publicOrganismAbbrev") String publicOrganismAbbrev, 
                                       @PathParam("tracks") String tracks)  throws IOException {

        //NOTE:  this service call is public facing so we use the publicOrganismAbbrev here

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        boolean isPbrowse = true;

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseTracks");
        command.add(publicOrganismAbbrev);
        command.add(projectId);
        command.add(String.valueOf(isPbrowse));
        command.add(tracks);
        command.add(appType);

        return responseFromCommand(command);
    }

    @GET
    @Path("{tracks}/{publicOrganismAbbrev}/trackList.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseTracks(@PathParam("publicOrganismAbbrev") String publicOrganismAbbrev,
                                     @PathParam("tracks") String tracks)  throws IOException {

        //NOTE:  this service call is public facing so we use the publicOrganismAbbrev here

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        boolean isPbrowse = false;

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseTracks");
        command.add(publicOrganismAbbrev);
        command.add(projectId);
        command.add(String.valueOf(isPbrowse));
        command.add(tracks);
        command.add(appType);
        return responseFromCommand(command);
    }

    @GET
    @Path("organismSpecific/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseOrganismSpecificTracks(@PathParam("organismAbbrev") String organismAbbrev,
                                                     @DefaultValue("0") @QueryParam("isApollo") String isApollo)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();
        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");


        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseOrganismSpecificTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(isApollo);
        command.add(buildNumber);
        command.add(webservicesDir);
        command.add(appType);
        return responseFromCommand(command);
    }

    @GET
    @Path("organismSpecificPbrowse/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseOrganismSpecificPbrowseTracks(@PathParam("organismAbbrev") String organismAbbrev)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseOrganismSpecificPbrowseTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(appType);
        return responseFromCommand(command);
    }

    @GET
    @Path("organismList")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrganismList() throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseOrganismList");
        command.add(projectId);

        return responseFromCommand(command);
    }

    @GET
    @Path("rnaseq/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseRNASeqTracks(@PathParam("organismAbbrev") String organismAbbrev) throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();
        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseRnaAndChipSeqTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(buildNumber);
        command.add(webservicesDir);
        command.add("RNASeq");
        command.add(appType);
        return responseFromCommand(command);
    }

    @GET
    @Path("chipseq/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseChIPSeqTracks(@PathParam("organismAbbrev") String organismAbbrev) throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();
        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseRnaAndChipSeqTracks");
        command.add(organismAbbrev);
        command.add(projectId);
        command.add(buildNumber);
        command.add(webservicesDir);
        command.add("ChIPSeq");
        command.add(appType);
        return responseFromCommand(command);
    }

    @GET
    @Path("store")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getJBrowseStore(@QueryParam("data") String data,
                                    @HeaderParam("Range") String fileRange) throws WdkModelException {

        String projectId = getWdkModel().getProjectId();
        String buildNumber = getWdkModel().getBuildNumber();
        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");

        String path = checkPath(
            webservicesDir + "/" +
            projectId + "/" +
            "build-" + buildNumber + "/" +
            data);

        return getFileChunkResponse(Paths.get(path), parseRangeHeaderValue(fileRange));
    }

    @GET
    @Path("auxiliary")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getAuxiliaryFile(@QueryParam("data") String data,
                                    @HeaderParam("Range") String fileRange) throws WdkModelException {

        String webservicesDir = getWdkModel().getProperties().get("WEBSERVICEMIRROR");

        String auxPath = webservicesDir.replaceAll("webServices", "auxiliary");

        String path = checkPath(
            auxPath + "/" +
            data);

        return getFileChunkResponse(Paths.get(path), parseRangeHeaderValue(fileRange));
    }

    private static String checkPath(String fileSystemPath) {
      // TODO: think about whether other checks belong here
      if (fileSystemPath.contains("..") || fileSystemPath.contains("$")) {
        throw new NotFoundException(formatNotFound("*"));
      }
      return fileSystemPath;
    }


    // THIS SHOULD BE REPLACED BY INDEXED FASTA IN WEBSERVICES
    // @GET
    // @Path("seq/{organismAbbrev}")
    // @Produces(MediaType.APPLICATION_JSON)
    // public Response getJbrowseRefSeqs(@PathParam("organismAbbrev") String organismAbbrev )  throws IOException {

    //     String gusHome = getWdkModel().getGusHome();
    //     String projectId = getWdkModel().getProjectId();

    //     List<String> command = new ArrayList<String>();
    //     command.add(gusHome + "/bin/jbrowseRefSeqs");
    //     command.add(gusHome);
    //     command.add(projectId);
    //     command.add(organismAbbrev);
    //     command.add("genomic");

    //     return responseFromCommand(command);
    // }

    // THIS SHOULD BE REPLACED BY INDEXED FASTA IN WEBSERVICES
    // @GET
    // @Path("aaseq/{organismAbbrev}")
    // @Produces(MediaType.APPLICATION_JSON)
    // public Response getJbrowseAaRefSeqs(@PathParam("organismAbbrev") String organismAbbrev )  throws IOException {

    //     String gusHome = getWdkModel().getGusHome();
    //     String projectId = getWdkModel().getProjectId();

    //     List<String> command = new ArrayList<String>();
    //     command.add(gusHome + "/bin/jbrowseRefSeqs");
    //     command.add(gusHome);
    //     command.add(projectId);
    //     command.add(organismAbbrev);
    //     command.add("protein");

    //     return responseFromCommand(command);
    // }

    @GET
    @Path("names/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseNames(@PathParam("organismAbbrev") String organismAbbrev, 
                                    @QueryParam("equals") String eq, 
                                    @QueryParam("startswith") String startsWith)  throws IOException {

        if ((startsWith == null || startsWith.isBlank()) && (eq == null || eq.isBlank())) {
          throw new BadRequestException("Request must include one of the following query parameters: ['startswith', 'equals']");
        }

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        boolean isPartial = true;
        String sourceId = startsWith;

        if (eq != null && !eq.isBlank()) {
            isPartial = false;
            sourceId = eq;
        }

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseNames");
        command.add(gusHome);
        command.add(projectId);
        command.add(organismAbbrev);
        command.add("genomic");
        command.add(String.valueOf(isPartial));
        command.add(appType);
        command.add(sourceId);
        return responseFromCommand(command);
    }

    @GET
    @Path("aanames/{organismAbbrev}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseAaNames(@PathParam("organismAbbrev") String organismAbbrev, 
                                      @QueryParam("equals") String eq, 
                                      @QueryParam("startswith") String startsWith)  throws IOException {

        String gusHome = getWdkModel().getGusHome();
        String projectId = getWdkModel().getProjectId();

        boolean isPartial = true;
        String sourceId = startsWith;

        if(eq != null && !eq.equals("")) {
            isPartial = false;
            sourceId = eq;
        }

        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseNames");
        command.add(gusHome);
        command.add(projectId);
        command.add(organismAbbrev);
        command.add("protein");
        command.add(String.valueOf(isPartial));
        command.add(appType);
        command.add(sourceId);

        return responseFromCommand(command);
    }

    public Response responseFromCommand(List<String> command) throws IOException {
        for (int i = 0; i < command.size(); i++) {
          if (command.get(i) == null)
            throw new WdkRuntimeException(
                "Command part at index " + i + " is null.  Could be due to unchecked user input.");
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();
        env.put("GUS_HOME", getWdkModel().getGusHome());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        return Response.ok(getStreamingStandardOutput(p, String.join(" ", command))).build();
    }
}
