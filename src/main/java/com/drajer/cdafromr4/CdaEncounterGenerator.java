package com.drajer.cdafromr4;

import com.drajer.cda.utils.CdaGeneratorConstants;
import com.drajer.cda.utils.CdaGeneratorUtils;
import com.drajer.sof.model.LaunchDetails;
import com.drajer.sof.model.R4FhirData;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdaEncounterGenerator {

  private static final Logger logger = LoggerFactory.getLogger(CdaEncounterGenerator.class);

  private CdaEncounterGenerator() {}

  public static String generateEncounterSection(R4FhirData data, LaunchDetails details) {

    StringBuilder sb = new StringBuilder(2000);
    Encounter encounter = data.getEncounter();

    if (encounter != null) {

      logger.debug("Generating Encounter section");

      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.COMP_EL_NAME));
      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.SECTION_EL_NAME));

      sb.append(CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.ENC_SEC_TEMPLATE_ID));
      sb.append(
          CdaGeneratorUtils.getXmlForTemplateId(
              CdaGeneratorConstants.ENC_SEC_TEMPLATE_ID,
              CdaGeneratorConstants.ENC_SEC_TEMPLATE_ID_EXT));

      sb.append(
          CdaGeneratorUtils.getXmlForCD(
              CdaGeneratorConstants.CODE_EL_NAME,
              CdaGeneratorConstants.ENC_SEC_CODE,
              CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
              CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
              CdaGeneratorConstants.ENC_SEC_NAME));

      // Add Title
      sb.append(
          CdaGeneratorUtils.getXmlForText(
              CdaGeneratorConstants.TITLE_EL_NAME, CdaGeneratorConstants.ENC_SEC_TITLE));

      // Add Narrative Text
      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.TEXT_EL_NAME));

      // Create Table Header.
      List<String> list = new ArrayList<>();
      list.add(CdaGeneratorConstants.ENC_TABLE_COL_1_TITLE);
      list.add(CdaGeneratorConstants.ENC_TABLE_COL_2_TITLE);

      sb.append(
          CdaGeneratorUtils.getXmlForTableHeader(
              list, CdaGeneratorConstants.TABLE_BORDER, CdaGeneratorConstants.TABLE_WIDTH));

      // Add Body
      sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.TABLE_BODY_EL_NAME));

      String actDisplayName = CdaGeneratorConstants.UNKNOWN_VALUE;

      if (encounter.getClass_() != null
          && encounter.getClass_().getCode() != null
          && !StringUtils.isEmpty(encounter.getClass_().getDisplay())) {
        logger.debug("Found Encounter Class value, so using it for display");
        actDisplayName = encounter.getClass_().getDisplay();
      } else if (encounter.getTypeFirstRep() != null
          && encounter.getTypeFirstRep().getCodingFirstRep() != null
          && !StringUtils.isEmpty(encounter.getTypeFirstRep().getCodingFirstRep().getDisplay())) {

        logger.debug("Did not find Encounter class, but Found Encounter Type value, so using it");
        actDisplayName = encounter.getTypeFirstRep().getCodingFirstRep().getDisplay();
      }

      String dt = CdaGeneratorConstants.UNKNOWN_VALUE;
      if (encounter.getPeriod() != null && encounter.getPeriod().getStartElement() != null) {

        dt =
            CdaFhirUtilities.getDisplayStringForDateTimeType(
                encounter.getPeriod().getStartElement());

      } else {
        logger.error(
            " Period is either null or the Period.DateTime has a null value or null timezone value ");
      }

      Map<String, String> bodyvals = new LinkedHashMap<>();
      bodyvals.put(CdaGeneratorConstants.ENC_TABLE_COL_1_BODY_CONTENT, actDisplayName);
      bodyvals.put(CdaGeneratorConstants.ENC_TABLE_COL_2_BODY_CONTENT, dt);

      String contentRef = CdaGeneratorConstants.ENC_TABLE_COL_1_BODY_CONTENT + Integer.toString(1);

      sb.append(CdaGeneratorUtils.addTableRow(bodyvals, 1));

      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.TABLE_BODY_EL_NAME));

      // End Table.
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.TABLE_EL_NAME));
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.TEXT_EL_NAME));

      // Add the Entries.
      sb.append(CdaGeneratorUtils.getXmlForActEntry(CdaGeneratorConstants.TYPE_CODE_DEF));
      sb.append(
          CdaGeneratorUtils.getXmlForAct(
              CdaGeneratorConstants.ENC_ACT_EL_NAME,
              CdaGeneratorConstants.ENC_CLASS_CODE,
              CdaGeneratorConstants.MOOD_CODE_DEF));

      sb.append(CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.ENC_ENTRY_TEMPLATE_ID));
      sb.append(
          CdaGeneratorUtils.getXmlForTemplateId(
              CdaGeneratorConstants.ENC_ENTRY_TEMPLATE_ID,
              CdaGeneratorConstants.ENC_ENTRY_TEMPLATE_ID_EXT));

      sb.append(
          CdaGeneratorUtils.getXmlForII(
              details.getAssigningAuthorityId(), encounter.getIdElement().getIdPart()));

      // Add Identifiers
      List<Identifier> ids = encounter.getIdentifier();
      if (ids != null) {

        for (Identifier id : ids) {

          if (id.getSystem() != null && id.getValue() != null) {

            sb.append(
                CdaGeneratorUtils.getXmlForII(
                    CdaGeneratorUtils.getRootOid(id.getSystem(), details.getAssigningAuthorityId()),
                    id.getValue()));
          }
        }
      }

      sb.append(getEncounterCodeXml(encounter, contentRef));

      sb.append(
          CdaFhirUtilities.getPeriodXml(
              encounter.getPeriod(), CdaGeneratorConstants.EFF_TIME_EL_NAME, false));

      if (encounter.getHospitalization() != null
          && encounter.getHospitalization().getDischargeDisposition() != null) {

        List<CodeableConcept> dds = new ArrayList<>();
        dds.add(encounter.getHospitalization().getDischargeDisposition());

        String ddxml =
            CdaFhirUtilities.getCodeableConceptXml(
                dds, CdaGeneratorConstants.SDTC_DISCHARGE_DISPOSITION, false);

        if (ddxml != null && !ddxml.isEmpty()) sb.append(ddxml);
      }

      String encDiagXml = generateEncounterDiagnosisXml(data, details);

      if (encDiagXml != null && !encDiagXml.isEmpty()) {

        logger.debug("Adding Encounter Diagnosis to the Encounter Section");
        sb.append(encDiagXml);
      }

      String reasonDiagXml = generateEncounterReasonXml(data, details, encounter);

      if (reasonDiagXml != null && !reasonDiagXml.isEmpty()) {

        logger.debug("Adding Encounter Reason Code Xml to the Encounter Section");
        sb.append(reasonDiagXml);
      }

      // End Entry Tags
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENC_ACT_EL_NAME));
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_EL_NAME));

      // Complete the section end tags.
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.SECTION_EL_NAME));
      sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.COMP_EL_NAME));

    } else {

      sb.append(generateEmptyEncounterSection());
    }

    return sb.toString();
  }

  public static String generateEncounterDiagnosisXml(R4FhirData data, LaunchDetails details) {

    StringBuilder sb = new StringBuilder();

    if (data.getEncounterDiagnosisConditions() != null
        && !data.getEncounterDiagnosisConditions().isEmpty()) {

      List<Condition> encDiagnosis = data.getEncounterDiagnosisConditions();

      for (Condition c : encDiagnosis) {

        sb.append(
            CdaGeneratorUtils.getXmlForEntryRelationship(
                CdaGeneratorConstants.ENTRY_REL_RSON_CODE));
        sb.append(
            CdaGeneratorUtils.getXmlForAct(
                CdaGeneratorConstants.ACT_EL_NAME,
                CdaGeneratorConstants.ACT_CLASS_CODE,
                CdaGeneratorConstants.MOOD_CODE_DEF));

        sb.append(
            CdaGeneratorUtils.getXmlForTemplateId(
                CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_TEMPLATE_ID));
        sb.append(
            CdaGeneratorUtils.getXmlForTemplateId(
                CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_TEMPLATE_ID,
                CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_TEMPLATE_ID_EXT));

        sb.append(
            CdaGeneratorUtils.getXmlForCD(
                CdaGeneratorConstants.CODE_EL_NAME,
                CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_CODE,
                CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
                CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
                CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_CODE_DISPLAY_NAME));

        sb.append(
            CdaGeneratorUtils.getXmlForCD(
                CdaGeneratorConstants.STATUS_CODE_EL_NAME, CdaGeneratorConstants.ACTIVE_STATUS));

        // Add problem observation
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
        sb.append(CdaGeneratorUtils.getXmlForII(CdaGeneratorUtils.getGuid()));

        // Add Code.
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

        Pair<Date, TimeZone> onset = CdaFhirUtilities.getActualDate(c.getOnset());
        Pair<Date, TimeZone> abatement = CdaFhirUtilities.getActualDate(c.getAbatement());

        sb.append(
            CdaGeneratorUtils.getXmlForIVLWithTS(
                CdaGeneratorConstants.EFF_TIME_EL_NAME, onset, abatement, true));

        List<CodeableConcept> cds = new ArrayList<>();
        cds.add(c.getCode());

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

        sb.append(
            CdaProblemGenerator.addTriggerCodes(
                details, c.getCode(), onset, abatement, c.getIdElement().getIdPart()));

        // End Tag for Entry Relationship
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.OBS_ACT_EL_NAME));
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_REL_EL_NAME));

        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ACT_EL_NAME));
        sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_REL_EL_NAME));
      }
    }

    return sb.toString();
  }

  public static String generateEncounterReasonXml(
      R4FhirData data, LaunchDetails details, Encounter encounter) {

    StringBuilder sb = new StringBuilder();
    CodeableConcept primaryCd = null;

    if (encounter.hasReasonCode()) {

      List<CodeableConcept> codes = encounter.getReasonCode();
      List<Coding> codings = new ArrayList<>();

      if (codes != null && !codes.isEmpty()) {

        for (CodeableConcept cd : codes) {

          Coding code =
              CdaFhirUtilities.getCodingForCodeSystem(cd, CdaGeneratorConstants.FHIR_SNOMED_URL);
          Coding code1 =
              CdaFhirUtilities.getCodingForCodeSystem(cd, CdaGeneratorConstants.FHIR_ICD10_CM_URL);
          Coding code2 =
              CdaFhirUtilities.getCodingForCodeSystem(cd, CdaGeneratorConstants.FHIR_ICD9_CM_URL);

          if (code != null) {
            primaryCd = cd;
            codings.add(code);
            break;
          }

          if (code1 != null) {
            primaryCd = cd;
            codings.add(code1);
            break;
          }

          if (code2 != null) {
            primaryCd = cd;
            codings.add(code2);
            break;
          }
        } // for all codeable concepts
      } // if codeable concept exists

      if (!codings.isEmpty()) {
        sb.append(
            addDiagnosisActAndObsXml(
                details, codings, primaryCd, encounter.getIdElement().getIdPart()));
      }
    } // if reason code is present

    return sb.toString();
  }

  public static String addDiagnosisActAndObsXml(
      LaunchDetails details, List<Coding> codes, CodeableConcept cd, String id) {

    StringBuilder sb = new StringBuilder(200);

    sb.append(
        CdaGeneratorUtils.getXmlForEntryRelationship(CdaGeneratorConstants.ENTRY_REL_RSON_CODE));
    sb.append(
        CdaGeneratorUtils.getXmlForAct(
            CdaGeneratorConstants.ACT_EL_NAME,
            CdaGeneratorConstants.ACT_CLASS_CODE,
            CdaGeneratorConstants.MOOD_CODE_DEF));

    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_TEMPLATE_ID));
    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(
            CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_TEMPLATE_ID,
            CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_TEMPLATE_ID_EXT));

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.CODE_EL_NAME,
            CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_CODE,
            CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
            CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
            CdaGeneratorConstants.ENC_DIAGNOSIS_ACT_CODE_DISPLAY_NAME));

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.STATUS_CODE_EL_NAME, CdaGeneratorConstants.ACTIVE_STATUS));

    // Add problem observation
    sb.append(
        CdaGeneratorUtils.getXmlForEntryRelationship(CdaGeneratorConstants.ENTRY_REL_SUBJ_CODE));

    sb.append(
        CdaGeneratorUtils.getXmlForAct(
            CdaGeneratorConstants.OBS_ACT_EL_NAME,
            CdaGeneratorConstants.OBS_CLASS_CODE,
            CdaGeneratorConstants.MOOD_CODE_DEF));

    sb.append(CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.PROB_OBS_TEMPLATE_ID));
    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(
            CdaGeneratorConstants.PROB_OBS_TEMPLATE_ID,
            CdaGeneratorConstants.PROB_OBS_TEMPALTE_ID_EXT));
    sb.append(CdaGeneratorUtils.getXmlForII(CdaGeneratorUtils.getGuid()));

    // Add Code.
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

    Pair<Date, TimeZone> onset = null;
    Pair<Date, TimeZone> abatement = null;

    sb.append(
        CdaGeneratorUtils.getXmlForIVLWithTS(
            CdaGeneratorConstants.EFF_TIME_EL_NAME, onset, abatement, true));

    String codeXml =
        CdaFhirUtilities.getCodingXmlForValue(codes, CdaGeneratorConstants.VAL_EL_NAME, "");

    if (!codeXml.isEmpty()) sb.append(codeXml);

    sb.append(CdaProblemGenerator.addTriggerCodes(details, cd, onset, abatement, id));

    // End Tag for Entry Relationship
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.OBS_ACT_EL_NAME));
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_REL_EL_NAME));

    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ACT_EL_NAME));
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.ENTRY_REL_EL_NAME));

    return sb.toString();
  }

  public static String generateEmptyEncounterSection() {
    StringBuilder sb = new StringBuilder();

    // Generate the component and section end tags
    sb.append(CdaGeneratorUtils.getXmlForStartElement(CdaGeneratorConstants.COMP_EL_NAME));
    sb.append(
        CdaGeneratorUtils.getXmlForNFSection(
            CdaGeneratorConstants.SECTION_EL_NAME, CdaGeneratorConstants.NF_NI));

    sb.append(CdaGeneratorUtils.getXmlForTemplateId(CdaGeneratorConstants.ENC_SEC_TEMPLATE_ID));
    sb.append(
        CdaGeneratorUtils.getXmlForTemplateId(
            CdaGeneratorConstants.ENC_SEC_TEMPLATE_ID,
            CdaGeneratorConstants.ENC_SEC_TEMPLATE_ID_EXT));

    sb.append(
        CdaGeneratorUtils.getXmlForCD(
            CdaGeneratorConstants.CODE_EL_NAME,
            CdaGeneratorConstants.ENC_SEC_CODE,
            CdaGeneratorConstants.LOINC_CODESYSTEM_OID,
            CdaGeneratorConstants.LOINC_CODESYSTEM_NAME,
            CdaGeneratorConstants.ENC_SEC_NAME));

    // Add Title
    sb.append(
        CdaGeneratorUtils.getXmlForText(
            CdaGeneratorConstants.TITLE_EL_NAME, CdaGeneratorConstants.ENC_SEC_TITLE));

    // Add Narrative Text
    sb.append(
        CdaGeneratorUtils.getXmlForText(
            CdaGeneratorConstants.TEXT_EL_NAME, "No Encounter Information"));

    // Complete the section end tags.
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.SECTION_EL_NAME));
    sb.append(CdaGeneratorUtils.getXmlForEndElement(CdaGeneratorConstants.COMP_EL_NAME));

    return sb.toString();
  }

  public static String getEncounterCodeXml(Encounter encounter, String contentRef) {
    String codeXml = "";

    if (encounter != null) {
      if (encounter.getClass_() != null) {
        Coding encClass = encounter.getClass_();
        List<Coding> codes = new ArrayList<>();
        codes.add(encClass);
        codeXml =
            CdaFhirUtilities.getCodingXmlForCodeSystem(
                codes,
                CdaGeneratorConstants.CODE_EL_NAME,
                CdaGeneratorConstants.FHIR_ENCOUNTER_CLASS_URL,
                true,
                contentRef);
      }

      if (!codeXml.isEmpty()) {
        logger.debug("Found Encounter class information, so use it ");
        return codeXml;
      } else {

        List<CodeableConcept> cds = encounter.getType();

        // Try to see if there is a CPT code first, if not try ICD-10, if not try SNOMED.
        codeXml =
            CdaFhirUtilities.getCodeableConceptXmlForCodeSystem(
                cds,
                CdaGeneratorConstants.CODE_EL_NAME,
                false,
                CdaGeneratorConstants.FHIR_CPT_URL,
                true);

        if (!codeXml.isEmpty()) {
          logger.debug("Found Encounter Information using CPT, so using it ");
          return codeXml;
        } else {
          codeXml =
              CdaFhirUtilities.getCodeableConceptXmlForCodeSystem(
                  cds,
                  CdaGeneratorConstants.CODE_EL_NAME,
                  false,
                  CdaGeneratorConstants.FHIR_ICD10_CM_URL,
                  true);

          if (!codeXml.isEmpty()) {
            logger.debug("Found Encounter Information using ICD10, so using it ");
            return codeXml;
          } else {

            codeXml =
                CdaFhirUtilities.getCodeableConceptXmlForCodeSystem(
                    cds,
                    CdaGeneratorConstants.CODE_EL_NAME,
                    false,
                    CdaGeneratorConstants.FHIR_SNOMED_URL,
                    true);

            if (!codeXml.isEmpty()) {
              logger.debug("Found Encounter Information using SNOMED, so using it ");
              return codeXml;
            } else {
              logger.debug(
                  "Append Encounter Information using other code systems if found or append null");
              codeXml =
                  CdaFhirUtilities.getCodeableConceptXml(
                      cds, CdaGeneratorConstants.CODE_EL_NAME, false);
              return codeXml;
            }
          }
        }
      }
    }
    return codeXml;
  }
}
