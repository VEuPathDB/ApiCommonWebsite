package org.apidb.apicommon.service.services;

import static org.gusdb.fgputil.FormatUtil.NL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.answer.stream.RecordStream;
import org.gusdb.wdk.model.answer.stream.RecordStreamFactory;
import org.gusdb.wdk.model.record.PrimaryKeyDefinition;
import org.gusdb.wdk.model.record.PrimaryKeyValue;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.attribute.AttributeField;
import org.gusdb.wdk.model.user.StepContainer;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.service.service.AbstractWdkService;

import io.prometheus.client.Counter;

@Path("system/metrics/organism")
public class OrganismMetricsService extends AbstractWdkService {

  private static final Logger LOG = Logger.getLogger(OrganismMetricsService.class);

  private static final String ALL_ORGS_QUESTION = "OrganismQuestions.AllOrganisms";

  private static final String ORGANISM_VALIDATION_ATTRIBUTE = "organism_full";

  private static final String ORGANISM_ATTRIBUTE = "organism_full";

  private static final Counter ORGANISM_COUNTER = Counter.build()
      .name("page_access_by_organism")
      .help("Times a page related to each organism is accessed.")
      .labelNames("organism")
      .register();

  // lazy loaded constant list of valid org names
  private static List<String> VALID_ORGANISM_NAMES;

  @GET
  public Response incrementOrganismCount(
      @QueryParam("recordType") String recordClassUrlSegment,
      @QueryParam("primaryKey") String primaryKeyValues,
      @QueryParam("organism") String organism) throws WdkModelException {

    // caller can specify organism directly or PK of a recordclass; if latter, look up organism for that record
    if (organism == null) {
      organism = resolveOrganism(recordClassUrlSegment, primaryKeyValues);
      LOG.info("Found organism '" + organism + "' for " + recordClassUrlSegment + " record with PK " + primaryKeyValues);
    }

    // validate submitted or fetched organism name
    if (!getValidOrganismNames(getWdkModel()).contains(organism)) {
      throw new BadRequestException("Invalid organism name: " + organism);
    }

    // found a valid organism name in this request; increment counter for that org
    ORGANISM_COUNTER.labels(organism).inc();

    return Response.noContent().build();
  }

  private static synchronized List<String> getValidOrganismNames(WdkModel wdkModel) throws WdkModelException {
    if (VALID_ORGANISM_NAMES == null) {
      User user = wdkModel.getSystemUser();
      AnswerValue answer = AnswerValueFactory.makeAnswer(user, AnswerSpec.builder(wdkModel)
          .setQuestionFullName(ALL_ORGS_QUESTION)
          .buildRunnable(user, StepContainer.emptyContainer()));
      AttributeField orgNameAttribute = answer.getAnswerSpec().getQuestion()
          .getRecordClass().getAttributeField(ORGANISM_VALIDATION_ATTRIBUTE).orElseThrow();
      try (RecordStream records = RecordStreamFactory.getRecordStream(answer, List.of(orgNameAttribute), Collections.emptyList())) {
        List<String> validOrgNames = new ArrayList<>();
        for (RecordInstance record : records) {
          validOrgNames.add(record.getAttributeValue(ORGANISM_VALIDATION_ATTRIBUTE).getValue());
        }
        VALID_ORGANISM_NAMES = validOrgNames;
        LOG.info("Loaded list of valid organisms for organism metrics: " + NL + String.join(NL, VALID_ORGANISM_NAMES));
      }
      catch (WdkUserException e) {
        throw new WdkModelException("Could not look up attribute " + ORGANISM_VALIDATION_ATTRIBUTE + " on organism record instance.", e);
      }
    }
    return VALID_ORGANISM_NAMES;
  }

  private String resolveOrganism(String recordClassUrlSegment, String primaryKeyValues) throws WdkModelException {
    if (recordClassUrlSegment == null || primaryKeyValues == null) {
      throw new BadRequestException("Request must include either 'organism' or both 'recordType' and 'primaryKey' query parameters.");
    }
    RecordClass recordClass = getWdkModel().getRecordClassByUrlSegment(recordClassUrlSegment)
        .orElseThrow(() -> new BadRequestException("No record type found with url segment '" + recordClassUrlSegment + "'"));
    if (!recordClass.getAttributeFieldMap().containsKey(ORGANISM_ATTRIBUTE)) {
      throw new BadRequestException("Record type with url segment '" + recordClassUrlSegment + "' does not contain an '" + ORGANISM_ATTRIBUTE + "' attribute.");
    }
    PrimaryKeyValue pkValue = getPkValue(recordClass, primaryKeyValues);
    List<RecordInstance> records = RecordClass.getRecordInstances(getRequestingUser(), pkValue);
    if (records.isEmpty()) {
      throw new BadRequestException("Primary Key '" + primaryKeyValues + "' does not map to any records of type '" + recordClassUrlSegment + "'.");
    }
    try {
      return records.get(0).getAttributeValue(ORGANISM_ATTRIBUTE).getValue();
    }
    catch (WdkUserException e) {
      throw new WdkModelException("Could not look up organism of PK '" + primaryKeyValues + "' of type " + recordClassUrlSegment, e);
    }
  }

  private PrimaryKeyValue getPkValue(RecordClass recordClass, String primaryKeyValues) throws WdkModelException {
    PrimaryKeyDefinition pkDef = recordClass.getPrimaryKeyDefinition();
    String[] pkColNames = pkDef.getColumnRefs();
    String[] pkValueNames = primaryKeyValues.split(",");
    if (pkColNames.length != pkValueNames.length) {
      throw new BadRequestException("Primary key for record type '" + recordClass.getUrlSegment() +
          "' has " + pkColNames.length + " fields: " + String.join(", ", pkColNames));
    }
    return new PrimaryKeyValue(pkDef,
      IntStream.range(0, pkColNames.length).boxed()
        .collect(Collectors.toMap(i -> pkColNames[i], i -> pkValueNames[i])));
  }

}
