package com.drajer.sof.launch;

import com.drajer.sof.model.ClientDetails;
import com.drajer.sof.service.ClientDetailsService;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ClientDetailsController {

  public static final String ERROR_IN_PROCESSING_THE_REQUEST = "Error in Processing the Request";

  @Autowired ClientDetailsService clientDetailsService;

  private final Logger logger = LoggerFactory.getLogger(ClientDetailsController.class);

  @CrossOrigin
  @RequestMapping("/api/clientDetails/{clientId}")
  public ClientDetails getClientDetailsById(@PathVariable("clientId") Integer clientId) {
    return clientDetailsService.getClientDetailsById(clientId);
  }

  // POST method to create a Client
  @CrossOrigin
  @RequestMapping(value = "/api/clientDetails", method = RequestMethod.POST)
  public ResponseEntity<?> createClientDetails(@RequestBody ClientDetails clientDetails) {
    ClientDetails checkClientDetails =
        clientDetailsService.getClientDetailsByUrl(clientDetails.getFhirServerBaseURL());
    if (checkClientDetails == null) {
      logger.info("Saving the Client Details");
      clientDetailsService.saveOrUpdate(clientDetails);
      return new ResponseEntity<>(clientDetails, HttpStatus.OK);
    } else {
      logger.error("FHIR Server URL is already registered");
      JSONObject responseObject = new JSONObject();
      responseObject.put("status", "error");
      responseObject.put("message", "URL is already registered");
      return new ResponseEntity<>(responseObject, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @CrossOrigin
  @RequestMapping(value = "/api/clientDetails", method = RequestMethod.PUT)
  public ResponseEntity<?> updateClientDetails(@RequestBody ClientDetails clientDetail) {
    ClientDetails checkClientDetails =
        clientDetailsService.getClientDetailsByUrl(clientDetail.getFhirServerBaseURL());
    if (checkClientDetails == null || (checkClientDetails.getId().equals(clientDetail.getId()))) {
      logger.info("Saving the Client Details");
      clientDetailsService.saveOrUpdate(clientDetail);
      return new ResponseEntity<>(clientDetail, HttpStatus.OK);
    } else {
      logger.error("FHIR Server URL is already registered");
      JSONObject responseObject = new JSONObject();
      responseObject.put("status", "error");
      responseObject.put("message", "URL is already registered");
      return new ResponseEntity<>(responseObject, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @CrossOrigin
  @RequestMapping("/api/clientDetails")
  public ClientDetails getClientDetailsByUrl(@RequestParam(value = "url") String url) {
    return clientDetailsService.getClientDetailsByUrl(url);
  }

  @CrossOrigin
  @RequestMapping("/api/clientDetails/")
  public List<ClientDetails> getAllClientDetails() {
    return clientDetailsService.getAllClientDetails();
  }

  @CrossOrigin
  @RequestMapping(value = "/api/clientDetails", method = RequestMethod.DELETE)
  public ResponseEntity<Object> deleteClientDetails(
      @RequestParam String fhirUrl, HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    try {
      if (fhirUrl == null || fhirUrl.isEmpty()) {
        return new ResponseEntity("Requested FHIR Url is missing or empty", HttpStatus.BAD_REQUEST);
      }
      ClientDetails checkClientDetails = clientDetailsService.getClientDetailsByUrl(fhirUrl);
      if (checkClientDetails != null) {
        clientDetailsService.delete(checkClientDetails);
        return new ResponseEntity("ClientDetails deleted successfully.", HttpStatus.OK);
      }
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "Client Details Not found");
      return new ResponseEntity("Client Details Not found", HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      logger.error(ERROR_IN_PROCESSING_THE_REQUEST, e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, ERROR_IN_PROCESSING_THE_REQUEST);
    }
  }
}
