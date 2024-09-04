package com.drajer.bsa.controller;

import com.drajer.bsa.model.RestApiBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.drajer.bsa.service.KarParser;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * This controller is a sample controller showing how to receive restful messages from eCRNow App.
 *
 * @author nbashyam
 */
@RestController
public class SampleRestApiReceiver {

  private static final Logger logger = LoggerFactory.getLogger(SampleRestApiReceiver.class);

  @Autowired
  KarParser parser;

  @CrossOrigin
  @PostMapping(value = "/api/receiveEicr")
  public JSONObject receiveEicr(
      @RequestBody RestApiBody body, HttpServletRequest request, HttpServletResponse response) {

    logger.info(
        "Payload received for fhirServerUrl: {}, patientId: {}, encounterId: {}, versionId: {}, requestId: {}, authorizationHeader: {}",
        StringEscapeUtils.escapeJava(body.getFhirServerURL()),
        StringEscapeUtils.escapeJava(body.getPatientId()),
        StringEscapeUtils.escapeJava(body.getEncounterId()),
        body.getSubmittedVersionId(),
        request.getHeader("X-Request-ID"),
        StringEscapeUtils.escapeJava(request.getHeader("Authorization")));

    logger.debug(" Payload is : {}", body.getPayload());

    JSONObject responseObject = new JSONObject();
    responseObject.put("status", "Success");

    return responseObject;
  }

  @CrossOrigin
  @GetMapping(value = "/api/karReload")
  public String reloadKars(){
    parser.loadKars();
    return "done";
  }
}
