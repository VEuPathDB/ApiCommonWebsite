package org.apidb.apicommon.model.migrate;

import java.nio.file.Files;
import java.nio.file.Path;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.fix.table.TableRowInterfaces.RowResult;
import org.gusdb.wdk.model.fix.table.steps.StepData;
import org.json.JSONObject;
import org.junit.Test;

public class TestGus4UnknownFix {

  private static final String PROJECT_ID = "PlasmoDB";

  // each test case is a String[] with [ <question_name>, <display_params> ]
  private static final String[][] TEST_CASES = {
      { "SnpChipQuestions.SnpsByStrain",
        "{\"params\":{\"snp_assay_type\":\"Broad_3k_chip,Broad_barcode,Broad_hd_array\",\"snp_strain_a\":\"7G8\",\"snpchip_strain_meta\":\"{\\\"ignored\\\":[],\\\"values\\\":[\\\"10/10B\\\",\\\"1054\\\",\\\"106/1\\\",\\\"10_54\\\",\\\"207-89\\\",\\\"207_89\\\",\\\"330-89\\\",\\\"330_89\\\",\\\"35_26\\\",\\\"36-89\\\",\\\"365_89\\\",\\\"36_89\\\",\\\"3D7\\\",\\\"3D7_1\\\",\\\"51\\\",\\\"608\\\",\\\"60888\\\",\\\"608_88\\\",\\\"7G8\\\",\\\"87-239\\\",\\\"9-411\\\",\\\"9_411\\\",\\\"A4\\\",\\\"ADA-2\\\",\\\"ADA2\\\",\\\"APO-42\\\",\\\"APO41\\\",\\\"CF04.008_10B\\\",\\\"CF04.008_12G\\\",\\\"CF04.008_1F\\\",\\\"CF04.008_2G\\\",\\\"CF04.008_7H\\\",\\\"CF04.009\\\",\\\"CF04.009_6D\\\",\\\"CF04.010\\\",\\\"CF04.010_10B\\\",\\\"CF08.008\\\",\\\"CH3\\\",\\\"CH4\\\",\\\"D10\\\",\\\"D6\\\",\\\"Dd2\\\",\\\"FCB\\\",\\\"FCC-2\\\",\\\"FCC2\\\",\\\"FCR3\\\",\\\"FCR8\\\",\\\"GA3\\\",\\\"GH2\\\",\\\"HB3\\\",\\\"HB3_1_batch2\\\",\\\"HB3_2_batch2\\\",\\\"HB3_repeat\\\",\\\"IGHCR14\\\",\\\"ITG-2G2\\\",\\\"Indochina I\\\",\\\"Indochina_I\\\",\\\"JDP8\\\",\\\"JST\\\",\\\"K1\\\",\\\"KMWII\\\",\\\"M24\\\",\\\"MT/s1\\\",\\\"MT_S1\\\",\\\"Malawi 2257/10H\\\",\\\"Malawi CF04.008\\\",\\\"Malawi CF04.008 10B\\\",\\\"Malawi CF04.008 12G\\\",\\\"Malawi CF04.008 1F\\\",\\\"Malawi CF04.008 2G\\\",\\\"Malawi CF04.008 7H\\\",\\\"Malawi CF04.009\\\",\\\"Malawi CF04.009 1G\\\",\\\"Malawi CF04.009 6D\\\",\\\"Malawi CF04.010 10B\\\",\\\"Malayan\\\",\\\"Malayan Camp\\\",\\\"Mali PS149\\\",\\\"Mali PS186\\\",\\\"Mali PS189\\\",\\\"Mali PS206\\\",\\\"Muz51.1\\\",\\\"PR145\\\",\\\"PS189\\\",\\\"P_reichenowi\\\",\\\"Preichenowi\\\",\\\"RAJ116\\\",\\\"RO-33\\\",\\\"RO33\\\",\\\"RS1\\\",\\\"SA2\\\",\\\"Santa Lucia\\\",\\\"SantaLucia\\\",\\\"Santa_Lucia\\\",\\\"SenP05.02\\\",\\\"SenP06.02\\\",\\\"SenP08.04\\\",\\\"SenP09.04\\\",\\\"SenP11.02\\\",\\\"SenP18.02\\\",\\\"SenP19.04\\\",\\\"SenP26.04\\\",\\\"SenP27.02\\\",\\\"SenP31.01\\\",\\\"SenP51.02\\\",\\\"SenP60.02\\\",\\\"SenT10.04\\\",\\\"SenT15.04\\\",\\\"SenT26.04\\\",\\\"SenT28.04\\\",\\\"SenThi10.04\\\",\\\"SenV34.04\\\",\\\"SenV35.04\\\",\\\"SenV42.05\\\",\\\"SenV56.04\\\",\\\"Senegal P05.02\\\",\\\"Senegal P06.02\\\",\\\"Senegal P08.04\\\",\\\"Senegal P09.04\\\",\\\"Senegal P11.02\\\",\\\"Senegal P121.05\\\",\\\"Senegal P136.05\\\",\\\"Senegal P137.05\\\",\\\"Senegal P139.05\\\",\\\"Senegal P18.02\\\",\\\"Senegal P19.04\\\",\\\"Senegal P21.05\\\",\\\"Senegal P26.04\\\",\\\"Senegal P27.02\\\",\\\"Senegal P31.01\\\",\\\"Senegal P35.04\\\",\\\"Senegal P47.05\\\",\\\"Senegal P51.02\\\",\\\"Senegal P60.02\\\",\\\"Senegal P69.05\\\",\\\"Senegal P75.05\\\",\\\"Senegal P90.05\\\",\\\"Senegal Thi10.04\\\",\\\"Senegal Thi15.04\\\",\\\"Senegal Thi26.04\\\",\\\"Senegal Thi28.04\\\",\\\"Senegal V127.05\\\",\\\"Senegal V34.04\\\",\\\"Senegal V35.04\\\",\\\"Senegal V42.05\\\",\\\"Senegal V56.04\\\",\\\"Senegal V64.05\\\",\\\"Senegal V83.05\\\",\\\"T116\\\",\\\"T2/C6\\\",\\\"T2_C6\\\",\\\"T9-94\\\",\\\"TD194\\\",\\\"TD203\\\",\\\"TD257\\\",\\\"TM-4C8-2\\\",\\\"TM327\\\",\\\"TM335\\\",\\\"TM336\\\",\\\"TM343\\\",\\\"TM345\\\",\\\"TM346\\\",\\\"TM347\\\",\\\"TM4-C8-2\\\",\\\"TM90C2A\\\",\\\"TM90C6A\\\",\\\"TM90C6A_unknown\\\",\\\"TM90C6B\\\",\\\"TM91C1088\\\",\\\"TM91C235\\\",\\\"TM93C1088\\\",\\\"Thi10.04\\\",\\\"Thi15.04\\\",\\\"Thi26.04\\\",\\\"Thi28.04\\\",\\\"V1/S\\\",\\\"V1_S\\\",\\\"V5.42.05\\\",\\\"VS_1\\\",\\\"W2\\\",\\\"W2mef\\\",\\\"WR87\\\"],\\\"filters\\\":[{\\\"field\\\":\\\"Host\\\",\\\"operation\\\":\\\"membership\\\",\\\"values\\\":[\\\"Human\\\",\\\"Unknown\\\"]}]}\",\"organism\":\"Plasmodium falciparum 3D7\"},\"viewFilters\":[],\"filters\":[]}" },
      { "GeneQuestions.GenesByNgsSnps",
        "{\"params\":{\"ReadFrequencyPercent\":\"80%\",\"MinPercentMinorAlleles\":\"0\",\"WebServicesPath\":\"dflt\",\"snp_density_lower\":\"0\",\"dn_ds_ratio_lower\":\"0\",\"dn_ds_ratio_upper\":\"\",\"organism\":\"Plasmodium falciparum 3D7\",\"occurrences_lower\":\"0\",\"ngsSnp_strain_meta\":\"{\\\"ignored\\\":[],\\\"values\\\":[\\\"SenT001.07\\\",\\\"SenT015.09\\\",\\\"SenT042.09\\\",\\\"SenT044.10\\\",\\\"SenT061.09\\\",\\\"SenT090.08\\\",\\\"SenT101.09\\\",\\\"SenT104.07\\\",\\\"SenT145.08\\\",\\\"SenT175.08\\\",\\\"BM_0008\\\",\\\"BM_0009\\\",\\\"RV_3600\\\",\\\"RV_3610\\\",\\\"RV_3702\\\",\\\"RV_3714\\\",\\\"RV_3737\\\",\\\"TRIPS_504\\\",\\\"T9_94\\\",\\\"SenT002.07\\\",\\\"SenT018.09\\\",\\\"SenT097.09\\\",\\\"SenT110.09\\\",\\\"SenT111.09\\\",\\\"SenT127.09\\\",\\\"SenT128.08\\\",\\\"SenT137.09\\\",\\\"SenT142.09\\\",\\\"SenT151.09\\\",\\\"SenT170.08\\\",\\\"RV_3630\\\",\\\"RV_3672\\\",\\\"RV_3675\\\",\\\"RV_3741\\\",\\\"TRIPS_355\\\",\\\"TRIPS_480\\\",\\\"HB3\\\",\\\"SenT064.10\\\",\\\"SenT094.09\\\",\\\"SenT113.09\\\",\\\"SenT144.08\\\",\\\"SenT230.08\\\",\\\"RV_3703\\\",\\\"RV_3721\\\",\\\"RV_3729\\\",\\\"RV_3735\\\",\\\"RV_3740\\\",\\\"RV_3769\\\",\\\"TRIPS_373\\\",\\\"TRIPS_410\\\",\\\"TRIPS_440\\\",\\\"IT\\\",\\\"SenT002.09\\\",\\\"SenT021.09\\\",\\\"SenT029.09\\\",\\\"SenT033.09\\\",\\\"SenT066.08\\\",\\\"SenT075.10\\\",\\\"SenT084.09\\\",\\\"SenT102.08\\\",\\\"SenT119.09\\\",\\\"SenT123.09\\\",\\\"SenT150.09\\\",\\\"SenT180.08\\\",\\\"SenT197.08\\\",\\\"RV_3611\\\",\\\"RV_3687\\\",\\\"TRIPS_331\\\",\\\"TRIPS_364\\\",\\\"TRIPS_704\\\",\\\"7G8\\\",\\\"CS2\\\",\\\"V1_S\\\",\\\"SenT022.09\\\",\\\"SenT044.08\\\",\\\"SenT047.09\\\",\\\"SenT067.09\\\",\\\"SenT077.08\\\",\\\"SenT106.08\\\",\\\"SenT135.09\\\",\\\"SenT137.08\\\",\\\"SenT139.08\\\",\\\"SenT149.09\\\",\\\"SenT179.08\\\",\\\"SenT235.08\\\",\\\"SenT238.08\\\",\\\"RV_3655\\\",\\\"RV_3701\\\",\\\"RV_3739\\\",\\\"TRIPS_301\\\",\\\"TRIPS_433\\\",\\\"TRIPS_482\\\",\\\"TRIPS_487\\\",\\\"Dd2-1\\\",\\\"3D7\\\",\\\"SenT015.08\\\",\\\"SenT190.08\\\",\\\"SenT227.08\\\",\\\"RV_3606\\\",\\\"RV_3635\\\",\\\"RV_3637\\\",\\\"RV_3736\\\",\\\"TRIPS_461\\\",\\\"TRIPS_474\\\",\\\"TRIPS_499\\\",\\\"TRIPS_700\\\",\\\"TRIPS_708\\\",\\\"SenT024.08\\\",\\\"SenT063.07\\\",\\\"SenT093.09\\\",\\\"SenT140.08\\\",\\\"SenT224.08\\\",\\\"RV_3614\\\",\\\"RV_3650\\\",\\\"RV_3671\\\",\\\"RV_3695\\\",\\\"RV_3696\\\",\\\"RV_3764\\\",\\\"TRIPS_303\\\",\\\"TRIPS_437\\\",\\\"TRIPS_456\\\",\\\"TRIPS_490\\\",\\\"TRIPS_759\\\",\\\"GB4\\\",\\\"SenT001.08\\\",\\\"SenT032.09\\\",\\\"SenT033.08\\\",\\\"SenT037.08\\\",\\\"SenT046.10\\\",\\\"SenT087.08\\\",\\\"SenT090.09\\\",\\\"SenT092.08\\\",\\\"SenT112.09\\\",\\\"RV_3642\\\",\\\"RV_3673\\\",\\\"RV_3708\\\",\\\"RV_3717\\\",\\\"RV_3730\\\",\\\"RV_3731\\\",\\\"RV_3766\\\",\\\"TRIPS_467\\\",\\\"TRIPS_470\\\",\\\"TRIPS_501\\\",\\\"Dd2-2\\\",\\\"707A\\\"],\\\"filters\\\":[{\\\"field\\\":\\\"GeographicLocation\\\",\\\"operation\\\":\\\"membership\\\",\\\"values\\\":[\\\"Brazil\\\",\\\"Gambia\\\",\\\"Ghana\\\",\\\"Honduras\\\",\\\"Indochina/Laos\\\",\\\"Thailand\\\",\\\"Thies,Senegal\\\",\\\"Unknown\\\",\\\"Vietnam\\\"]}]}\",\"MinPercentIsolateCalls\":\"80\",\"ngs_snp_class\":\"All SNPs\",\"snp_density_upper\":\"\",\"occurrences_upper\":\"\"},\"viewFilters\":[],\"filters\":[{\"name\":\"matched_transcript_filter_array\",\"value\":{\"values\":[\"Y\"]},\"disabled\":false}]}" }
  };

  @Test
  public void testFilterFix() throws Exception {
    try (WdkModel model = WdkModel.construct(PROJECT_ID, GusHome.getGusHome())) {
      for (int i = 0; i < TEST_CASES.length; i++) {
        StepData step = getStep(i);
        Gus4StepTableMigrator plugin = new Gus4StepTableMigrator();
        // create temporary empty file
        Path emptyQuestionMappingFile = Files.createTempFile(TestGus4UnknownFix.class.getName(), null);
        plugin.configure(model, new ListBuilder<String>().add(
            emptyQuestionMappingFile.toAbsolutePath().toString()).toList());
        RowResult<StepData> result = plugin.processRecord(step);
        System.out.println("Should write? " + result.shouldWrite());
        System.out.println("Old value: " + new JSONObject(result.getRow().getOrigParamFiltersString()).toString(2));
        System.out.println("New value: " + result.getRow().getParamFilters().toString(2));
      }
    }
  }

  private StepData getStep(int i) {
    StepData step = new StepData();
    step.setStepId((long)i);
    step.setQuestionName(TEST_CASES[i][0]);
    step.setOrigParamFiltersString(TEST_CASES[i][1]);
    step.setParamFilters(new JSONObject(TEST_CASES[i][1]));
    return step;
  }
}
