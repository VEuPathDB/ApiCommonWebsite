package org.apidb.apicommon.service.services;

import java.util.Arrays;
import java.util.List;

import org.eupathdb.common.model.ProjectMapper;
import org.gusdb.fgputil.validation.ValidObjectFactory.DisplayablyValid;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.query.param.AbstractEnumParam;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.service.service.ProjectService;
import org.gusdb.wdk.service.service.QuestionService;
import org.json.JSONObject;

public class ApiProjectService extends ProjectService {

  private static final String PROJECT_URLS_KEY = "projectUrls";
  private static final String ORG_TO_PROJECT_KEY = "organismToProject";

  private static final String TAXON_QUESTION_NAME = "GenesByTaxon";
  private static final String ORGANISM_PARAM_NAME = "organism";

  private static final String[] PORTAL_PROJECT_IDS = new String[] { "EuPathDB", "VEuPathDB" };

  @Override
  protected JSONObject addSupplementalProjectInfo(JSONObject projectJson) throws WdkModelException {
    WdkModel model = getWdkModel();
    if (Arrays.asList(PORTAL_PROJECT_IDS).contains(model.getProjectId())) {
      ProjectMapper mapper = ProjectMapper.getMapper(model);

      // add mapping of project ID to webapp URL
      JSONObject mappedProjects = new JSONObject();
      for (String projectId : mapper.getFederatedProjects()) {
        mappedProjects.put(projectId, mapper.getWebAppUrl(projectId));
      }
      projectJson.put(PROJECT_URLS_KEY, mappedProjects);

      // get all organisms in vocabulary of org param of taxon question
      Question taxonQuestion = model.getQuestionByName(TAXON_QUESTION_NAME).get();
      DisplayablyValid<AnswerSpec> spec = QuestionService.getDisplayableAnswerSpec(
          TAXON_QUESTION_NAME, model, getSessionUser(), name -> taxonQuestion);
      AbstractEnumParam orgParam = (AbstractEnumParam)taxonQuestion.getParamMap().get(ORGANISM_PARAM_NAME);
      List<String> organisms = orgParam.getVocabInstance(AnswerSpec.getValidQueryInstanceSpec(spec)).getVocabTreeLeafTerms();

      // add mapping of organism to project ID
      JSONObject mappedOrganisms = new JSONObject();
      for (String organism : organisms) {
        mappedOrganisms.put(organism, mapper.getProjectByOrganism(organism));
      }
      projectJson.put(ORG_TO_PROJECT_KEY, mappedOrganisms);
    }
    return projectJson;
  }

}
