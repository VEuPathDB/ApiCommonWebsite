package org.apidb.apicommon.model;

import java.sql.SQLException;

import org.eupathdb.common.model.ProjectMapper;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.Assert;
import org.junit.Test;

public class ProjectMapperTest {

  @Test
  public void testGetRecordUrl() throws WdkModelException {
    try (WdkModel wdkModel = WdkModel.construct("EuPathDB", GusHome.getGusHome())) {
      ProjectMapper mapper = ProjectMapper.getMapper(wdkModel);
      String recordClass = TranscriptUtil.TRANSCRIPT_RECORDCLASS;
      String projectId = "PlasmoDB";
      String sourceId = "_DEFAULT_TRANSCRIPT_";
      String geneSourceId = "PF11_0344";
      String url = mapper.getRecordUrl(recordClass, projectId, sourceId, geneSourceId);
      String expected = "http://plasmodb.org/plasmo/app/record/gene/" + geneSourceId;
      Assert.assertEquals(expected, url);
    }
  }

  @Test
  public void testGetProjectByOrganism() throws WdkModelException, SQLException {
    try (WdkModel wdkModel = WdkModel.construct("EuPathDB", GusHome.getGusHome())) {
      ProjectMapper mapper = ProjectMapper.getMapper(wdkModel);
      Assert.assertEquals("PlasmoDB",
          mapper.getProjectByOrganism("Plasmodium falciparum"));
      Assert.assertEquals("PlasmoDB",
          mapper.getProjectByOrganism("Plasmodium knowlesi strain H"));
      Assert.assertEquals("CryptoDB",
          mapper.getProjectByOrganism("Cryptosporidium muris"));
      Assert.assertEquals("CryptoDB",
          mapper.getProjectByOrganism("Cryptosporidium parvum Chr. 6"));
    }
  }
}
