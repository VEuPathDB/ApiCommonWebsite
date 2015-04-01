package org.apidb.apicommon.model;

import java.sql.SQLException;

import org.eupathdb.common.model.ProjectMapper;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.junit.Assert;
import org.junit.Test;

public class ProjectMapperTest {

  private final WdkModel wdkModel;

  public ProjectMapperTest() throws WdkModelException {
    String gusHome = System.getProperty(Utilities.SYSTEM_PROPERTY_GUS_HOME);
    wdkModel = WdkModel.construct("EuPathDB", gusHome);
  }

  @Test
  public void testGetRecordUrl() throws WdkModelException {
    ProjectMapper mapper = ProjectMapper.getMapper(wdkModel);
    String recordClass = "GeneRecordClasses.GeneRecordClass";
    String projectId = "PlasmoDB";
    String sourceId = "PF11_0344";
    String url = mapper.getRecordUrl(recordClass, projectId, sourceId);
    String expected = "http://plasmodb.org/plasmo/showRecord.do?name="
        + recordClass + "&project_id=" + projectId + "&source_id=" + sourceId;
    Assert.assertEquals(expected, url);
  }

  @Test
  public void testGetProjectByOrganism() throws WdkModelException, SQLException {
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
