package org.apidb.apicommon.model;

import org.eupathdb.common.model.ProjectMapper;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
      Assertions.assertEquals(expected, url);
    }
  }

  @Test
  public void testGetProjectByOrganism() throws WdkModelException {
    try (WdkModel wdkModel = WdkModel.construct("EuPathDB", GusHome.getGusHome())) {
      ProjectMapper mapper = ProjectMapper.getMapper(wdkModel);
      Assertions.assertEquals("PlasmoDB",
          mapper.getProjectByOrganism("Plasmodium falciparum"));
      Assertions.assertEquals("PlasmoDB",
          mapper.getProjectByOrganism("Plasmodium knowlesi strain H"));
      Assertions.assertEquals("CryptoDB",
          mapper.getProjectByOrganism("Cryptosporidium muris"));
      Assertions.assertEquals("CryptoDB",
          mapper.getProjectByOrganism("Cryptosporidium parvum Chr. 6"));
    }
  }
}
