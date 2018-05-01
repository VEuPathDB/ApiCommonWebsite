package org.apidb.apicommon.service.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.gusdb.wdk.service.service.WdkService;
import org.json.JSONObject;

@Path("/jbrowse")
public class JBrowseService extends WdkService {

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(JBrowseService.class);

    @GET
    @Path("stats/global")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseFeatures(@SuppressWarnings("unused") @QueryParam("feature") String feature) {
        return Response.ok(new JSONObject().put("featureDensity", 0.0002).toString()).build();
    }


    @GET
    @Path("features/{refseq_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJbrowseFeatures(@PathParam("refseq_name") String refseqName, 
                                       @QueryParam("feature") String feature,
                                       @QueryParam("start") String start,
                                       @QueryParam("end") String end) throws IOException, InterruptedException {

        String gusHome = getWdkModel().getGusHome();


        List<String> command = new ArrayList<String>();
        command.add(gusHome + "/bin/jbrowseFeatures");
        command.add(gusHome);
        command.add(refseqName);
        command.add(start);
        command.add(end);
        command.add(feature);

        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();
        env.put("GUS_HOME", gusHome);

        pb.redirectErrorStream(true);

        Process p = pb.start();

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));


        String result = "";
        String line;
        while ((line = br.readLine()) != null) {
            result += line;
        }

        p.waitFor();

        if(p.exitValue() != 0) {
            throw new RuntimeException(result);
        }

        if(result.equals("")) {
            result = "{}";
        }


        return Response.ok(result).build();
    }
}
