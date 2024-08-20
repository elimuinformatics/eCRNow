package com.drajer.cdafromr4;

import com.drajer.cda.utils.CdaGeneratorConstants;
import com.drajer.cda.utils.CdaGeneratorUtils;
import com.drajer.eca.model.MatchedTriggerCodes;
import com.drajer.eca.model.PatientExecutionState;
import com.drajer.ecrapp.util.ApplicationUtils;
import com.drajer.sof.model.LaunchDetails;
import com.drajer.sof.model.R4FhirData;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.codesystems.ConditionClinical;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class CdaProblemGenerator {

  private CdaProblemGenerator() {}

  private static final Logger logger = LoggerFactory.getLogger(CdaProblemGenerator.class);

  public static String generateProblemSection(R4FhirData data, LaunchDetails details) {

    StringBuilder sb = new StringBuilder(2000);

    List<Condition> conds = data.getConditions();

    if (conds != null && !conds.isEmpty()) {

      // Generate the component and section end tags
      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.COMP_EL_NAME));
      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.SECTION_EL_NAME));

      sb.append(CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.PROB_SEC_TEMPLATE_ID));
      sb.append(
          CdaGeneratorUtils.getXmlForTemplateId(
              CdaGeneratorConstants.PROB_SEC_TEMPLATE_ID,
              CdaGeneratorConstants.PROB_SEC_TEMPLATE_ID_EXT));

      sb.append(
          CdaGeneratorUtils.getXmlForCD(
              CdaGeneratorConstants.CODE_EL_NAME,
              CdaGeneratorConstants.PROB_SEC_CODE,
              CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
              CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
              CdaGeneratorConstants.PROB_SEC_NAME));

      // Add Title
      sb.append(
          CdaGeneratorUtils.getXmlForText(
              CdaGeneratorConstants.TITLE_EL_NAME, CdaGeneratorConstants.PROB_SEC_TITLE));

      // Add Narrative Text
      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.TEXT_EL_NAME));

      // Create Table Header.
      List<String> list = new ArrayList<>();
      list.add(CdaGeneratorConstants.PROB_TABLE_COL_1_TITLE);
      list.add(CdaGeneratorConstants.PROB_TABLE_COL_2_TITLE);
      list.add(CdaGeneratorConstants.PROB_TABLE_COL_3_TITLE);
      sb.append(
          CdaGeneratorUtils.getXmlForTableHeader(
              list, CdaGeneratorConstants.TABLE_BORDER, CdaGeneratorConstants.TABLE_WIDTH));

      // Add Table Body
      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.TABLE_BODY_EL_NAME));

      // Add Body Rows
      int rowNum = 1;
      for (Condition prob : conds) {

        String probDisplayName = CdaFhirUtilities.getStringForCodeableConcept(prob.getCode());
        if (StringUtils.isEmpty(probDisplayName))
          probDisplayName = CdaGeneratorConstants.UNKNOWN_VALUE;

        Map<String, String> bodyvals = new LinkedHashMap<>();
        bodyvals.put(CdaGeneratorConstants.PROB_TABLE_COL_1_BODY_CONTENT, probDisplayName);

        if (prob.hasClinicalStatus()
            && prob.getClinicalStatus().hasCoding()
            && prob.getClinicalStatus().getCodingFirstRep() != null
            && !StringUtils.isEmpty(prob.getClinicalStatus().getCodingFirstRep().getCode())
            && (prob.getClinicalStatus()
                    .getCodingFirstRep()
                    .getCode()
                    .contentEquals(ConditionClinical.RESOLVED.toCode())
                || prob.getClinicalStatus()
                    .getCodingFirstRep()
                    .getCode()
                    .contentEquals(ConditionClinical.INACTIVE.toCode())
                || prob.getClinicalStatus()
                    .getCodingFirstRep()
                    .getCode()
                    .contentEquals(ConditionClinical.REMISSION.toCode()))) {
          bodyvals.put(
              CdaGeneratorConstants.PROB_TABLE_COL_2_BODY_CONTENT,
              CdaGeneratorConstants.TABLE_RESOLVED_STATUS);
        } else {
          bodyvals.put(
              CdaGeneratorConstants.PROB_TABLE_COL_2_BODY_CONTENT,
              CdaGeneratorConstants.TABLE_ACTIVE_STATUS);
        }

        // Add dates
        Pair<Date, TimeZone> onset = CdaFhirUtilities.getActualDate(prob.getOnset());
        Pair<Date, TimeZone> abatement = CdaFhirUtilities.getActualDate(prob.getAbatement());
        Pair<Date, TimeZone> recordedDate = null;
        if (prob.hasRecordedDateElement()) {
          recordedDate =
              new Pair<>(prob.getRecordedDate(), prob.getRecordedDateElement().getTimeZone());
        }

        String probDates = CdaFhirUtilities.getStringForDates(onset, abatement, recordedDate);
        bodyvals.put(CdaGeneratorConstants.PROB_TABLE_COL_3_BODY_CONTENT, probDates);

        sb.append(CdaGeneratorUtils.addTableRow(bodyvals, rowNum));
        ++rowNum;
      }

      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.TABLE_BODY_EL_NAME));

      // End Table.
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.TABLE_EL_NAME));
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.TEXT_EL_NAME));

      for (Condition pr : conds) {
        // Add the Entries.
        sb.append(CdaGeneratorUtils.getXmlForActEntry(CdaGeneratorConstants.TYPE_CODE_DEF));

        // Add the problem Concern Act
        sb.append(
            CdaGeneratorUtils.getXmlForAct(
                CdaGeneratorConstants.ACT_EL_NAME,
                CdaGeneratorConstants.ACT_CLASS_CODE,
                CdaGeneratorConstants.MOOD_CODE_DEF));

        sb.append(
            CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.PROB_CONCERN_TEMPLATE_ID));
        sb.append(
            CdaGeneratorUtils.getXmlForTemplateId(
                CdaGeneratorConstants.PROB_CONCERN_TEMPLATE_ID,
                CdaGeneratorConstants.PROB_CONCERN_TEMPLATE_ID_EXT));

        sb.append(CdaGeneratorUtils.getXmlForIIUsingGuid());
        sb.append(
            CdaGeneratorUtils.getXmlForCD(
                CdaGeneratorConstants.CODE_EL_NAME,
                CdaGeneratorConstants.PROB_CONC_ACT_CODE,
                CdaGeneratorConstants.HL7_ACT_CLASS_OID,
                CdaGeneratorConstants.HL7_ACT_CLASS_NAME,
                CdaGeneratorConstants.PROB_CONC_ACT_NAME));

        if (pr.hasClinicalStatus()
            && pr.getClinicalStatus().hasCoding()
            && pr.getClinicalStatus().getCodingFirstRep() != null
            && !StringUtils.isEmpty(pr.getClinicalStatus().getCodingFirstRep().getCode())
            && (pr.getClinicalStatus()
                    .getCodingFirstRep()
                    .getCode()
                    .contentEquals(ConditionClinical.RESOLVED.toCode())
                || pr.getClinicalStatus()
                    .getCodingFirstRep()
                    .getCode()
                    .contentEquals(ConditionClinical.INACTIVE.toCode())
                || pr.getClinicalStatus()
                    .getCodingFirstRep()
                    .getCode()
                    .contentEquals(ConditionClinical.REMISSION.toCode()))) {
          sb.append(
              CdaGeneratorUtils.getXmlForCD(
                  CdaGeneratorConstants.STATUS_CODE_EL_NAME,
                  CdaGeneratorConstants.COMPLETED_STATUS));
        } else {
          sb.append(
              CdaGeneratorUtils.getXmlForCD(
                  CdaGeneratorConstants.STATUS_CODE_EL_NAME, CdaGeneratorConstants.ACTIVE_STATUS));
        }

        Pair<Date, TimeZone> onset = CdaFhirUtilities.getActualDate(pr.getOnset());
        Pair<Date, TimeZone> abatement = CdaFhirUtilities.getActualDate(pr.getAbatement());
        Pair<Date, TimeZone> recordedDate = null;
        if (pr.hasRecordedDateElement()) {
          recordedDate =
              new Pair<>(pr.getRecordedDate(), pr.getRecordedDateElement().getTimeZone());
        }

        sb.append(
            CdaGeneratorUtils.getXmlForIVLWithTS(
                CdaGeneratorConstants.EFF_TIME_EL_NAME, recordedDate, abatement, true));

        // Add the Problem Observation
        sb.append(
            CdaGeneratorUtils.getXmlForEntryRelationship(
                CdaGeneratorConstants.ENTRY_REL_SUBJ_CODE));
        sb.append(
            CdaGeneratorUtils.getXmlForAct(
                CdaGeneratorConstants.OBS_ACT_EL_NAME,
                CdaGeneratorConstants.OBS_CLASS_CODE,
                CdaGeneratorConstants.MOOD_CODE_DEF));

        sb.append(
            CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.PROB_OBS_TEMPLATE_ID));
        sb.append(
            CdaGeneratorUtils.getXmlForTemplateId(
                CdaGeneratorConstants.PROB_OBS_TEMPLATE_ID,
                CdaGeneratorConstants.PROB_OBS_TEMPALTE_ID_EXT));

        sb.append(
            CdaGeneratorUtils.getXmlForII(
                details.getAssigningAuthorityId(), pr.getIdElement().getIdPart()));

        sb.append(
            CdaGeneratorUtils.getXmlForCDWithoutEndTag(
                CdaGeneratorConstants.CODE_EL_NAME,
                CdaGeneratorConstants.DIAGNOSIS_SNOMED,
                CdaGeneratorConstants.SNOMED_CODESYSTEM_OID,
                CdaGeneratorConstants.SNOMED_CODESYSTEM_NAME,
                CdaGeneratorConstants.DIAGNOSIS_DISPLAY_NAME));
        sb.append(
            CdaGeneratorUtils.getXmlForCD(
                CdaGeneratorConstants.TRANSLATION_EL_NAME,
                CdaGeneratorConstants.DIAGNOSIS_LOINC,
                CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
                CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
                CdaGeneratorConstants.DIAGNOSIS_DISPLAY_NAME));
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.CODE_EL_NAME));

        sb.append(
            CdaGeneratorUtils.getXmlForCD(
                CdaGeneratorConstants.STATUS_CODE_EL_NAME, CdaGeneratorConstants.COMPLETED_STATUS));

        sb.append(
            CdaGeneratorUtils.getXmlForIVLWithTS(
                CdaGeneratorConstants.EFF_TIME_EL_NAME, onset, abatement, true));

        List<CodeableConcept> cds = new ArrayList<>();
        cds.add(pr.getCode());

        String codeXml =
            CdaFhirUtilities.getCodeableConceptXmlForCodeSystem(
                cds,
                CdaGeneratorConstants.VAL_EL_NAME,
                true,
                CdaGeneratorConstants.FHIR_SNOMED_URL,
                false);

        if (!codeXml.isEmpty()) {
          sb.append(codeXml);
        } else {
          sb.append(
              CdaFhirUtilities.getCodeableConceptXml(cds, CdaGeneratorConstants.VAL_EL_NAME, true));
        }

        // End Tag for Entry Relationship
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.OBS_ACT_EL_NAME));
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_REL_EL_NAME));

        logger.debug(
            "Add Trigger Codes to Problem Observation if applicable {}",
            pr.getIdElement().getIdPart());
        sb.append(
            addTriggerCodes(
                details, pr.getCode(), onset, abatement, pr.getIdElement().getIdPart()));

        logger.debug("Completed adding Trigger Codes ");

        // End Tags for Entries
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ACT_EL_NAME));
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_EL_NAME));
      }

      // Complete the section end tags.
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.SECTION_EL_NAME));
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.COMP_EL_NAME));

    } else {

      sb.append(generateEmptyProblemSection());
    }

    return sb.toString();
  }

  public static String addTriggerCodes(
      LaunchDetails details,
      CodeableConcept cd,
      Pair<Date, TimeZone> onset,
      Pair<Date, TimeZone> abatement,
      String id) {

    StringBuilder sb = new StringBuilder();

    logger.debug("Adding Trigger Code Reason for Problem Observation");

    PatientExecutionState state = null;

    state = ApplicationUtils.getDetailStatus(details);

    List<MatchedTriggerCodes> mtcs = state.getMatchTriggerStatus().getMatchedCodes();

    for (MatchedTriggerCodes mtc : mtcs) {

      // Add each code as an entry relationship observation
      List<CodeableConcept> cds = new ArrayList<>();
      if (cd != null) cds.add(cd);

      Set<String> matchedCodesFromCc =
          mtc.hasMatchedTriggerCodesFromCodeableConcept("Condition", cds);

      Set<String> encMatchedCodes = mtc.hasMatchedTriggerCodesFromCodeableConcept("Encounter", cds);

      if (encMatchedCodes != null && !encMatchedCodes.isEmpty()) {

        if (matchedCodesFromCc != null) {
          matchedCodesFromCc.addAll(encMatchedCodes);
        } else {
          matchedCodesFromCc = new HashSet<>();
          matchedCodesFromCc.addAll(encMatchedCodes);
        }
      }

      if (matchedCodesFromCc != null && !matchedCodesFromCc.isEmpty()) {

        logger.debug(
            " Matched Codes from Codeable Concept is not empty, size = {}",
            matchedCodesFromCc.size());

        // Add the Problem Observation
        sb.append(
            CdaGeneratorUtils.getXmlForEntryRelationship(
                CdaGeneratorConstants.ENTRY_REL_RSON_CODE));
        sb.append(
            CdaGeneratorUtils.getXmlForActWithNegationInd(
                CdaGeneratorConstants.OBS_ACT_EL_NAME,
                CdaGeneratorConstants.OBS_CLASS_CODE,
                CdaGeneratorConstants.MOOD_CODE_DEF,
                "false",
                true));

        sb.append(
            CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.PROB_OBS_TEMPLATE_ID));
        sb.append(
            CdaGeneratorUtils.getXmlForTemplateId(
                CdaGeneratorConstants.PROB_OBS_TEMPLATE_ID,
                CdaGeneratorConstants.PROB_OBS_TEMPALTE_ID_EXT));
        sb.append(
            CdaGeneratorUtils.getXmlForTemplateId(
                CdaGeneratorConstants.TRIGGER_CODE_PROB_OBS_TEMPLATE_ID,
                CdaGeneratorConstants.TRIGGER_CODE_PROB_OBS_TEMPLATE_ID_EXT));

        sb.append(CdaGeneratorUtils.getXmlForII(details.getAssigningAuthorityId(), id));

        sb.append(
            CdaGeneratorUtils.getXmlForCDWithoutEndTag(
                CdaGeneratorConstants.CODE_EL_NAME,
                CdaGeneratorConstants.DIAGNOSIS_SNOMED,
                CdaGeneratorConstants.SNOMED_CODESYSTEM_OID,
                CdaGeneratorConstants.SNOMED_CODESYSTEM_NAME,
                CdaGeneratorConstants.DIAGNOSIS_DISPLAY_NAME));
        sb.append(
            CdaGeneratorUtils.getXmlForCD(
                CdaGeneratorConstants.TRANSLATION_EL_NAME,
                CdaGeneratorConstants.DIAGNOSIS_LOINC,
                CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
                CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
                CdaGeneratorConstants.DIAGNOSIS_DISPLAY_NAME));
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.CODE_EL_NAME));

        sb.append(
            CdaGeneratorUtils.getXmlForCD(
                CdaGeneratorConstants.STATUS_CODE_EL_NAME, CdaGeneratorConstants.COMPLETED_STATUS));

        sb.append(
            CdaGeneratorUtils.getXmlForIVLWithTS(
                CdaGeneratorConstants.EFF_TIME_EL_NAME, onset, abatement, true));

        // Split the system and code.
        matchedCodesFromCc
            .stream()
            .filter(Objects::nonNull)
            .findFirst()
            .ifPresent(
                matchCode -> {
                  logger.debug("Starting to add trigger code that was matched {}", matchCode);

                  String[] parts = matchCode.split("\\|");

                  logger.debug("Parts [0] = {}", parts[0]);

                  Pair<String, String> csd = CdaGeneratorConstants.getCodeSystemFromUrl(parts[0]);

                  logger.debug("Retrieved CSD Values");
                  logger.debug("Retrieved CSD Values {}, {}", csd.getValue0(), csd.getValue1());

                  sb.append(
                      CdaGeneratorUtils.getXmlForValueCDWithValueSetAndVersion(
                          parts[1],
                          csd.getValue0(),
                          csd.getValue1(),
                          details.getRctcOid(),
                          details.getRctcVersion(),
                          ""));
                  logger.debug("Constructed Value CD");
                });

        // End Tag for Entry Relationship
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.OBS_ACT_EL_NAME));
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_REL_EL_NAME));
      } else {

        logger.debug("Not adding matched Trigger codes as they are not present");
      }
    }

    return sb.toString();
  }

  public static String generateEmptyProblemSection() {

    StringBuilder sb = new StringBuilder();

    // Generate the component and section end tags
    sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.COMP_EL_NAME));
    sb.append(
        CdaGeneratorUtils.getXmlForNFSection(
            CdaGeneratorConstants.SECTION_EL_NAME, CdaGeneratorConstants.NF_NI));

    sb.append(CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.PROB_SEC_TEMPLATE_ID));
    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(
            CdaGeneratorConstants.PROB_SEC_TEMPLATE_ID,
            CdaGeneratorConstants.PROB_SEC_TEMPLATE_ID_EXT));

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.CODE_EL_NAME,
            CdaGeneratorConstants.PROB_SEC_CODE,
            CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
            CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
            CdaGeneratorConstants.PROB_SEC_NAME));

    // Add Title
    sb.append(
        CdaGeneratorUtils.getXmlForText(
            CdaGeneratorConstants.TITLE_EL_NAME, CdaGeneratorConstants.PROB_SEC_TITLE));

    // Add Narrative Text
    sb.append(
        CdaGeneratorUtils.getXmlForText(
            CdaGeneratorConstants.TEXT_EL_NAME, "No Problem Information"));

    // Complete the section end tags.
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.SECTION_EL_NAME));
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.COMP_EL_NAME));

    return sb.toString();
  }
}
