package com.drajer.bsa.ehr.service.impl;

import com.drajer.bsa.ehr.service.EhrAuthorizationService;
import com.drajer.bsa.model.BsaTypes;
import com.drajer.bsa.model.HealthcareSetting;
import com.drajer.bsa.model.KarProcessingData;
import com.drajer.sof.model.Response;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 *
 * <h1>EhrAuthorizationServiceImpl</h1>
 *
 * This class defines the implementation methods to get authorized with the EHRs.
 *
 * @author nbashyam
 */
@Service("ehrauth")
@Transactional
public class EhrAuthorizationServiceImpl implements EhrAuthorizationService {

  public static final String AUTHORIZATION_IS_CONFIGURED_FOR_EHR =
      " System Launch authorization is configured for EHR ";
  public static final String RECEIVED_ACCESS_TOKEN = "Received AccessToken: {}";
  private final Logger logger = LoggerFactory.getLogger(EhrAuthorizationServiceImpl.class);

  private static final String GRANT_TYPE = "grant_type";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String EXPIRES_IN = "expires_in";

  @Override
  public void getAuthorizationToken(KarProcessingData kd) {

    JSONObject tokenResponse = null;
    logger.info(
        "Getting AccessToken for EHR FHIR URL : {}",
        kd.getHealthcareSetting().getFhirServerBaseURL());
    logger.info("Getting AccessToken for Client Id : {}", kd.getHealthcareSetting().getClientId());

    try {

      if (kd.getHealthcareSetting()
          .getAuthType()
          .equals(BsaTypes.getString(BsaTypes.AuthenticationType.SYSTEM))) {

        logger.info(AUTHORIZATION_IS_CONFIGURED_FOR_EHR);

        RestTemplate resTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);

        // Setup the Authorization Header.
        String authValues =
            kd.getHealthcareSetting().getClientId()
                + ":"
                + kd.getHealthcareSetting().getClientSecret();
        String base64EncodedString =
            Base64.getEncoder().encodeToString(authValues.getBytes(StandardCharsets.UTF_8));
        headers.add("Authorization", "Basic " + base64EncodedString);

        // Setup the OAuth Type flow.
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(GRANT_TYPE, "client_credentials");
        map.add("scope", kd.getHealthcareSetting().getScopes());

        if (Boolean.TRUE.equals(kd.getHealthcareSetting().getRequireAud()))
          map.add("aud", kd.getHealthcareSetting().getFhirServerBaseURL());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
        ResponseEntity<?> response =
            resTemplate.exchange(
                kd.getHealthcareSetting().getTokenUrl(), HttpMethod.POST, entity, Response.class);
        tokenResponse = new JSONObject(response.getBody());

        logger.info(RECEIVED_ACCESS_TOKEN, tokenResponse);

        kd.getNotificationContext().setEhrAccessToken(tokenResponse.getString(ACCESS_TOKEN));
        kd.getNotificationContext()
            .setEhrAccessTokenExpiryDuration(tokenResponse.getInt(EXPIRES_IN));

        Integer expiresInSec = (Integer) tokenResponse.get(EXPIRES_IN);
        Instant expireInstantTime = new Date().toInstant().plusSeconds(expiresInSec);
        kd.getNotificationContext().setEhrAccessTokenExpirationTime(Date.from(expireInstantTime));
      }

    } catch (Exception e) {
      logger.error(
          "Error in Getting the AccessToken for the client: {}",
          kd.getHealthcareSetting().getFhirServerBaseURL(),
          e);
    }
  }

  @Override
  public JSONObject getAuthorizationToken(HealthcareSetting hs) {

    JSONObject tokenResponse = null;
    logger.info("Getting AccessToken for EHR FHIR URL : {}", hs.getFhirServerBaseURL());
    logger.info("Getting AccessToken for Client Id : {}", hs.getClientId());

    // Before getting a new access token, check to see if the old one is expired.
    if (hs.getAuthType().equals(BsaTypes.getString(BsaTypes.AuthenticationType.SYSTEM))) {

      logger.info(AUTHORIZATION_IS_CONFIGURED_FOR_EHR);

      tokenResponse =
          getAccessTokenForSystemLaunch(
              hs.getClientId(),
              hs.getClientSecret(),
              hs.getScopes(),
              hs.getRequireAud(),
              hs.getFhirServerBaseURL(),
              hs.getTokenUrl());

      if (tokenResponse != null) {

        logger.info(RECEIVED_ACCESS_TOKEN, tokenResponse);
        updateTokenDetailsInHealthcareSetting(tokenResponse, hs);

      } else {

        logger.error(" Error in retrieving access token, cannot proceed. ");
      }

    } else {

      // Handle Other AuthTypes.
    }

    return tokenResponse;
  }

  private void updateTokenDetailsInHealthcareSetting(
      JSONObject tokenResponse, HealthcareSetting hs) {

    hs.setEhrAccessToken(tokenResponse.getString(ACCESS_TOKEN));
    hs.setEhrAccessTokenExpiryDuration(tokenResponse.getInt(EXPIRES_IN));

    Integer expiresInSec = (Integer) tokenResponse.get(EXPIRES_IN);
    Instant expireInstantTime = new Date().toInstant().plusSeconds(expiresInSec);
    hs.setEhrAccessTokenExpirationTime(new Date().from(expireInstantTime));
  }

  public JSONObject getAccessTokenForSystemLaunch(
      String clientId,
      String clientSecret,
      String scopes,
      Boolean requireAud,
      String fhirServerUrl,
      String tokenUrl) {

    JSONObject tokenResponse = null;

    try {

      logger.debug(AUTHORIZATION_IS_CONFIGURED_FOR_EHR);

      RestTemplate resTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();

      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);

      // Setup the Authorization Header.
      String authValues = clientId + ":" + clientSecret;
      String base64EncodedString =
          Base64.getEncoder().encodeToString(authValues.getBytes(StandardCharsets.UTF_8));
      headers.add("Authorization", "Basic " + base64EncodedString);

      // Setup the OAuth Type flow.
      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add(GRANT_TYPE, "client_credentials");
      map.add("scope", scopes);

      if (Boolean.TRUE.equals(requireAud)) {
        map.add("aud", fhirServerUrl);
      }

      HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
      ResponseEntity<?> response =
          resTemplate.exchange(tokenUrl, HttpMethod.POST, entity, Response.class);
      tokenResponse = new JSONObject(response.getBody());

      logger.info(RECEIVED_ACCESS_TOKEN, tokenResponse);

    } catch (Exception e) {
      logger.error("Error in Getting the AccessToken for the client: {}", fhirServerUrl, e);
    }

    return tokenResponse;
  }
}
