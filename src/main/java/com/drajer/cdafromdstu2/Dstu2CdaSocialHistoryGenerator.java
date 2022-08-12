package com.drajer.cdafromdstu2;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.primitive.CodeDt;
import com.drajer.cda.utils.CdaGeneratorConstants;
import com.drajer.cda.utils.CdaGeneratorUtils;
import com.drajer.sof.model.Dstu2FhirData;
import com.drajer.sof.model.LaunchDetails;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.StringType;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dstu2CdaSocialHistoryGenerator {

  private static final Logger logger =
      LoggerFactory.getLogger(Dstu2CdaSocialHistoryGenerator.class);

  public static String generateSocialHistorySection(Dstu2FhirData data, LaunchDetails details) {

    StringBuilder sb = new StringBuilder(2000);

    // Will have to wait to discuss with vendors on Travel History, Pregnancy, and Birth Sex
    // Observations.
    // Then we can generte the entries. Till then it will be empty section.
    CodeDt birthSex =
        Dstu2CdaFhirUtilities.getCodeExtension(
            data.getPatient().getUndeclaredExtensions(),
            CdaGeneratorConstants.FHIR_ARGO_BIRTHSEX_EXT_URL);
    List<Observation> pregObs = data.getPregnancyObs();
    List<Observation> travelHistory = data.getTravelObs();
    List<Condition> pregCond = data.getPregnancyConditions();
    List<Observation> occHistory = data.getOccupationObs();

    if (birthSex != null
        || (pregObs != null && !pregObs.isEmpty())
        || (pregCond != null && !pregCond.isEmpty())
        || (occHistory != null && !occHistory.isEmpty())
        || (travelHistory != null && !travelHistory.isEmpty())) {

      sb.append(generateSocialHistorySectionHeader(""));

      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.TEXT_EL_NAME));

      // Create Table Header.
      List<String> list = new ArrayList<String>();
      list.add(CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_1_TITLE);
      list.add(CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_2_TITLE);
      sb.append(
          CdaGeneratorUtils.getXmlForTableHeader(
              list, CdaGeneratorConstants.TABLE_BORDER, CdaGeneratorConstants.TABLE_WIDTH));

      // Add Table Body
      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.TABLE_BODY_EL_NAME));

      String birthSexXml = "";
      String pregObsXml = "";
      StringBuilder pregCondXml = new StringBuilder();
      StringBuilder occHistoryXml = new StringBuilder();
      StringBuilder travelHistoryXml = new StringBuilder();
      int index = 0;
      Map<String, String> bodyvals = new LinkedHashMap<>();

      if (birthSex != null) {

        bodyvals.put(
            CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_1_BODY_CONTENT,
            CdaGeneratorConstants.BIRTH_SEX_DISPLAY);
        bodyvals.put(
            CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_2_BODY_CONTENT,
            Dstu2CdaFhirUtilities.getStringForIDataType(birthSex));

        sb.append(CdaGeneratorUtils.addTableRow(bodyvals, index));
        index++;

        birthSexXml = generateBirthSexEntry(data, details, birthSex);
      }

      if (pregCond != null && !pregCond.isEmpty()) {
        logger.info("Pregnancy Condition Found");

        for (Condition c : pregCond) {

          bodyvals.put(
              CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_1_BODY_CONTENT,
              CdaGeneratorConstants.PREGNANCY_CONDITION_DISPLAY);

          bodyvals.put(
              CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_2_BODY_CONTENT,
              Dstu2CdaFhirUtilities.getCodeableConceptDisplayForCodeSystem(
                      c.getCode(), CdaGeneratorConstants.FHIR_SNOMED_URL, true)
                  .getValue0());

          sb.append(CdaGeneratorUtils.addTableRow(bodyvals, index));
          index++;

          pregCondXml.append(generatePregnancyEntry(c));
        }
      }

      if (pregObs != null && pregObs.size() > 0) {

        logger.error(" Pregnancy Status Observation Found , translation not implemented ");
        // These are not available in FHIR right now reliably, so nothing to process until further
        // discussion with vendors.

        // Setup Table Text Entries

        // Setup XML Entries
      }

      if (occHistory != null && !occHistory.isEmpty()) {
        logger.info("Occupation History Observation Found");

        for (Observation obs : occHistory) {

          if (obs.getValue() != null
              && (obs.getValue() instanceof StringType
                  || obs.getValue() instanceof CodeableConcept)) {
            bodyvals.put(
                CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_1_BODY_CONTENT,
                CdaGeneratorConstants.OCCUPATION_HISTORY_DISPLAY);

            String display = Dstu2CdaFhirUtilities.getStringForType(obs.getValue());

            bodyvals.put(CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_2_BODY_CONTENT, display);

            sb.append(CdaGeneratorUtils.addTableRow(bodyvals, index));
            index++;

            occHistoryXml.append(generateOccHistoryEntry(obs));
          }
        }
      }

      if (travelHistory != null && !travelHistory.isEmpty()) {

        logger.info("Travel History Observation Found ");

        for (Observation obs : travelHistory) {

          bodyvals.put(
              CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_1_BODY_CONTENT,
              CdaGeneratorConstants.TRAVEL_HISTORY_DISPLAY);

          String display =
              Dstu2CdaFhirUtilities.getCombinationStringForCodeSystem(
                  obs.getCode(), obs.getValue(), CdaGeneratorConstants.FHIR_SNOMED_URL, true);

          bodyvals.put(CdaGeneratorConstants.SOC_HISTORY_TABLE_COL_2_BODY_CONTENT, display);

          sb.append(CdaGeneratorUtils.addTableRow(bodyvals, index));
          index++;

          travelHistoryXml.append(generateTravelHistoryEntry(obs, display));
        }
      }

      // Close the Table.
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.TABLE_BODY_EL_NAME));
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.TABLE_EL_NAME));
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.TEXT_EL_NAME));

      // Add entry
      if (!StringUtils.isEmpty(birthSexXml)) {
        sb.append(birthSexXml);
      }

      if (!StringUtils.isEmpty(pregCondXml)) {
        sb.append(pregCondXml);
      }

      if (!StringUtils.isEmpty(pregObsXml)) {
        sb.append(pregObsXml);
      }

      if (!StringUtils.isEmpty(occHistoryXml)) {
        sb.append(occHistoryXml);
      }

      if (!StringUtils.isEmpty(travelHistoryXml)) {
        sb.append(travelHistoryXml);
      }

      sb.append(generateSocialHistorySectionEndHeader());

    } else {
      sb.append(generateEmptySocialHistorySection());
    }

    return sb.toString();
  }

  public static String generateBirthSexEntry(
      Dstu2FhirData data, LaunchDetails details, CodeDt birthSex) {

    StringBuilder sb = new StringBuilder();

    // Generate the entry
    sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.ENTRY_EL_NAME));
    sb.append(
        CdaGeneratorUtils.getXmlForAct(
            CdaGeneratorConstants.OBS_ACT_EL_NAME,
            CdaGeneratorConstants.OBS_CLASS_CODE,
            CdaGeneratorConstants.MOOD_CODE_DEF));

    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(
            CdaGeneratorConstants.BIRTH_SEX_OBS_TEMPLATE_ID,
            CdaGeneratorConstants.BIRTH_SEX_OBS_TEMPLATE_ID_EXT));

    sb.append(CdaGeneratorUtils.getXmlForIIUsingGuid());

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.CODE_EL_NAME,
            CdaGeneratorConstants.BIRTH_SEX_CODE,
            CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
            CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
            CdaGeneratorConstants.BIRTH_SEX_DISPLAY));

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.STATUS_CODE_EL_NAME, CdaGeneratorConstants.COMPLETED_STATUS));

    sb.append(
        CdaGeneratorUtils.getXmlForValueCD(
            Dstu2CdaFhirUtilities.getStringForIDataType(birthSex),
            CdaGeneratorConstants.BIRTH_SEX_CODESYSTEM_OID,
            CdaGeneratorConstants.BIRTH_SEX_CODESYSTEM_NAME,
            ""));

    // End Tag for Entry Relationship
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.OBS_ACT_EL_NAME));
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_EL_NAME));

    return sb.toString();
  }

  public static String generateTravelHistoryEntry(Observation obs, String display) {

    StringBuilder sb = new StringBuilder();

    // Generate the entry
    sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.ENTRY_EL_NAME));
    sb.append(
        CdaGeneratorUtils.getXmlForAct(
            CdaGeneratorConstants.ACT_EL_NAME,
            CdaGeneratorConstants.ACT_CLASS_CODE,
            CdaGeneratorConstants.MOOD_CODE_DEF));

    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(
            CdaGeneratorConstants.TRAVEL_HISTORY_OBS_TEMPLATE_ID,
            CdaGeneratorConstants.TRAVEL_HISTORY_OBS_TEMPLATE_ID_EXT));

    sb.append(CdaGeneratorUtils.getXmlForIIUsingGuid());

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.CODE_EL_NAME,
            CdaGeneratorConstants.TRAVEL_HISTORY_SNOMED_CODE,
            CdaGeneratorConstants.SNOMED_CODESYSTEM_OID,
            CdaGeneratorConstants.SNOMED_CODESYSTEM_NAME,
            CdaGeneratorConstants.TRAVEL_HISTORY_SNOMED_CODE_DISPLAY));

    sb.append(CdaGeneratorUtils.getXmlForText(CdaGeneratorConstants.TEXT_EL_NAME, display));

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.STATUS_CODE_EL_NAME, CdaGeneratorConstants.COMPLETED_STATUS));

    Pair<Date, TimeZone> effDate = Dstu2CdaFhirUtilities.getActualDate(obs.getEffective());

    sb.append(
        CdaGeneratorUtils.getXmlForEffectiveTime(
            CdaGeneratorConstants.EFF_TIME_EL_NAME, effDate.getValue0(), effDate.getValue1()));

    // End Tag for Entry
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ACT_EL_NAME));
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_EL_NAME));

    return sb.toString();
  }

  public static String generateOccHistoryEntry(Observation obs) {

    StringBuilder sb = new StringBuilder();

    // Generate the entry
    sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.ENTRY_EL_NAME));
    sb.append(
        CdaGeneratorUtils.getXmlForAct(
            CdaGeneratorConstants.OBS_ACT_EL_NAME,
            CdaGeneratorConstants.OBS_CLASS_CODE,
            CdaGeneratorConstants.MOOD_CODE_DEF));

    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(
            CdaGeneratorConstants.SOC_HISTORY_OBS_TEMPLATE_ID,
            CdaGeneratorConstants.SOC_HISTORY_OBS_TEMPLATE_ID_EXT));

    sb.append(CdaGeneratorUtils.getXmlForIIUsingGuid());

    sb.append(
        CdaGeneratorUtils.getXmlForCDWithoutEndTag(
            CdaGeneratorConstants.CODE_EL_NAME,
            CdaGeneratorConstants.SNOMED_OCC_HISTORY_CODE,
            CdaGeneratorConstants.SNOMED_CODESYSTEM_OID,
            CdaGeneratorConstants.SNOMED_CODESYSTEM_NAME,
            CdaGeneratorConstants.SNOMED_OCC_HISTORY_CODE_DISPLAY));
    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.TRANSLATION_EL_NAME,
            CdaGeneratorConstants.LOINC_OCC_HISTORY_CODE,
            CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
            CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
            CdaGeneratorConstants.LOINC_OCC_HISTORY_CODE_DISPLAY));

    List<CodeableConceptDt> cds = new ArrayList<>();
    cds.add(obs.getCode());
    sb.append(
        Dstu2CdaFhirUtilities.getCodeableConceptXml(
            cds, CdaGeneratorConstants.TRANSLATION_EL_NAME, false));

    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.CODE_EL_NAME));

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.STATUS_CODE_EL_NAME, CdaGeneratorConstants.COMPLETED_STATUS));

    Pair<Date, TimeZone> effDate = Dstu2CdaFhirUtilities.getActualDate(obs.getEffective());

    sb.append(
        CdaGeneratorUtils.getXmlForEffectiveTime(
            CdaGeneratorConstants.EFF_TIME_EL_NAME, effDate.getValue0(), effDate.getValue1()));

    sb.append(
        Dstu2CdaFhirUtilities.getXmlForType(
            obs.getValue(), CdaGeneratorConstants.VAL_EL_NAME, true));

    // End Tag for Entry
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.OBS_ACT_EL_NAME));
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_EL_NAME));

    return sb.toString();
  }

  public static String generatePregnancyEntry(Condition cond) {

    StringBuilder sb = new StringBuilder();

    // Generate the entry
    sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.ENTRY_EL_NAME));
    sb.append(
        CdaGeneratorUtils.getXmlForAct(
            CdaGeneratorConstants.OBS_ACT_EL_NAME,
            CdaGeneratorConstants.OBS_CLASS_CODE,
            CdaGeneratorConstants.MOOD_CODE_DEF));

    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.PREGNANCY_OBS_TEMPLATE_ID));

    sb.append(CdaGeneratorUtils.getXmlForIIUsingGuid());

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.CODE_EL_NAME,
            CdaGeneratorConstants.OBS_ASSERTION,
            CdaGeneratorConstants.OBS_ASSERTION_CODESYSTEM,
            CdaGeneratorConstants.OBS_ASSERTION_CODESYSTEM_NAME,
            CdaGeneratorConstants.OBS_ASSERTION_DISPLAY));

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.STATUS_CODE_EL_NAME, CdaGeneratorConstants.COMPLETED_STATUS));

    Pair<Date, TimeZone> onset = Dstu2CdaFhirUtilities.getActualDate(cond.getOnset());
    Pair<Date, TimeZone> abatement = Dstu2CdaFhirUtilities.getActualDate(cond.getAbatement());

    sb.append(
        CdaGeneratorUtils.getXmlForIVLWithTS(
            CdaGeneratorConstants.EFF_TIME_EL_NAME, onset, abatement, false));

    List<CodeableConceptDt> cds = new ArrayList<>();
    cds.add(cond.getCode());

    String codeXml =
        Dstu2CdaFhirUtilities.getCodeableConceptXmlForCodeSystem(
            cds,
            CdaGeneratorConstants.VAL_EL_NAME,
            true,
            CdaGeneratorConstants.FHIR_SNOMED_URL,
            false);

    if (!codeXml.isEmpty()) {
      sb.append(codeXml);
    } else {
      sb.append(
          Dstu2CdaFhirUtilities.getCodeableConceptXml(
              cds, CdaGeneratorConstants.VAL_EL_NAME, true));
    }

    // End Tag for Entry Relationship
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.OBS_ACT_EL_NAME));
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_EL_NAME));

    return sb.toString();
  }

  public static String generateSocialHistorySectionHeader(String nf) {

    StringBuilder sb = new StringBuilder();

    // Generate the component and section end tags
    sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.COMP_EL_NAME));

    if (!StringUtils.isEmpty(nf)) {
      sb.append(
          CdaGeneratorUtils.getXmlForNFSection(
              CdaGeneratorConstants.SECTION_EL_NAME, CdaGeneratorConstants.NF_NI));
    } else {
      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.SECTION_EL_NAME));
    }

    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.SOC_HISTORY_SEC_TEMPLATE_ID));
    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(
            CdaGeneratorConstants.SOC_HISTORY_SEC_TEMPLATE_ID,
            CdaGeneratorConstants.SOC_HISTORY_SEC_TEMPLATE_ID_EXT));

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.CODE_EL_NAME,
            CdaGeneratorConstants.SOC_HISTORY_SEC_CODE,
            CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
            CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
            CdaGeneratorConstants.SOC_HISTORY_SEC_NAME));

    // Add Title
    sb.append(
        CdaGeneratorUtils.getXmlForText(
            CdaGeneratorConstants.TITLE_EL_NAME, CdaGeneratorConstants.SOC_HISTORY_SEC_TITLE));

    return sb.toString();
  }

  public static String generateSocialHistorySectionEndHeader() {

    StringBuilder sb = new StringBuilder();

    // Complete the section end tags.
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.SECTION_EL_NAME));
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.COMP_EL_NAME));

    return sb.toString();
  }

  public static String generateEmptySocialHistorySection() {

    StringBuilder sb = new StringBuilder();

    sb.append(generateSocialHistorySectionHeader(CdaGeneratorConstants.NF_NI));

    // Add Narrative Text
    sb.append(
        CdaGeneratorUtils.getXmlForText(
            CdaGeneratorConstants.TEXT_EL_NAME, "No Social History Information"));

    sb.append(generateSocialHistorySectionEndHeader());

    return sb.toString();
  }
}
