package com.drajer.sof.utils;

public class QueryConstants {

  private QueryConstants() {}

  public static final String LOINC_CODE_SYSTEM = "http://loinc.org";
  public static final String PREGNANCY_CODE = "90767-5";
  public static final String TRAVEL_CODE = "29762-2";

  public static final String[] TRAVEL_HISTORY_SNOMED_CODES = {
    "161085007", "443846001", "34831000175105"
  };
  public static final String SNOMED_CODE_SYSTEM = "http://snomed.info/sct";

  public static final String[] OCCUPATION_SNOMED_CODES = {"224362002"};
  public static final String[] PREGNANCY_SNOMED_CODES = {"77386006"};
}