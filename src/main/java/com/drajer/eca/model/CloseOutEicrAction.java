package com.drajer.eca.model;

import com.drajer.eca.model.EventTypes.EcrActionTypes;
import com.drajer.eca.model.EventTypes.JobStatus;
import com.drajer.eca.model.EventTypes.WorkflowEvent;
import com.drajer.ecrapp.model.Eicr;
import com.drajer.ecrapp.service.WorkflowService;
import com.drajer.ecrapp.util.ApplicationUtils;
import com.drajer.sof.model.LaunchDetails;
import com.drajer.sof.model.LaunchDetails.ProcessingStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;
import org.hibernate.ObjectDeletedException;
import org.hl7.fhir.r4.model.PlanDefinition.ActionRelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseOutEicrAction extends AbstractAction {

  private final Logger logger = LoggerFactory.getLogger(CloseOutEicrAction.class);

  @Override
  public void execute(Object obj, WorkflowEvent launchType, String taskInstanceId) {

    logger.info(" **** START Executing Close Out Eicr Action **** ");

    if (obj instanceof LaunchDetails) {

      LaunchDetails details = (LaunchDetails) obj;
      ObjectMapper mapper = new ObjectMapper();
      PatientExecutionState state = null;

      state = ApplicationUtils.getDetailStatus(details);
      state.getCloseOutEicrStatus().setActionId(getActionId());

      logger.info(
          " Executing Close Out Eicr Action , Prior Execution State : = {}",
          StringEscapeUtils.escapeJava(details.getStatus()));

      // Handle Conditions
      Boolean conditionsMet = true;
      conditionsMet = matchCondition(details);
      boolean encounterClosed = EcaUtils.checkEncounterClose(details);
      logger.info(" Encounter is closed = {}", encounterClosed);

      // PreConditions Met, then process related actions.
      Boolean relatedActsDone = true;
      if (Boolean.TRUE.equals(conditionsMet) && encounterClosed) {

        logger.info(" PreConditions have been Met, evaluating Related Actions. ");

        if (getRelatedActions() != null && !getRelatedActions().isEmpty()) {

          List<RelatedAction> racts = getRelatedActions();

          for (RelatedAction ract : racts) {

            if (ract.getRelationship() == ActionRelationshipType.AFTER) {

              // check if the action is completed.
              String actionId = ract.getRelatedAction().getActionId();

              if (Boolean.FALSE.equals(state.hasActionCompleted(actionId))) {

                logger.info(
                    " Action {} is not completed , hence this action has to wait ", actionId);
                relatedActsDone = false;
              } else {
                logger.info(" Related Action that has been completed : {}", actionId);

                // Check if there is any timing constraint that needs to be handled.
                if (ract.getDuration() != null
                    && state.getCloseOutEicrStatus().getJobStatus() == JobStatus.NOT_STARTED) {

                  // Duration is not null, meaning that the close out action has to be delayed by
                  // the
                  // duration.
                  logger.info(" Schedule the job for Close Out EICR Action based on the duration.");

                  scheduleJob(details, state, ract, mapper, taskInstanceId);
                  // No need to continue as the job will take over execution.

                  logger.info(" **** END Executing Close Out Eicr Action **** ");
                  return;

                } else {

                  logger.info(
                      " No need to schedule job as it has already been scheduled or completed. ");
                }
              }
            } else {
              logger.info(
                  " Action {} is related via {}",
                  ract.getRelatedAction().getActionId(),
                  ract.getRelationship());
            }
          }
        }

        // Check Timing Data , Dont check if the state is already scheduled meaning the
        // job was scheduled already.
        if (Boolean.TRUE.equals(relatedActsDone)) {

          logger.info(" All Related Actions are completed ");

          if (state.getCloseOutEicrStatus().getJobStatus() == JobStatus.NOT_STARTED) {

            logger.info(" Related Actions Done and this action has not started ");

            if (getTimingData() != null && !getTimingData().isEmpty()) {

              logger.info(" Timing Data is present , so create a job based on timing data.");
              List<TimingSchedule> tsjobs = getTimingData();

              for (TimingSchedule ts : tsjobs) {

                WorkflowService.scheduleJob(
                    details.getId(),
                    ts,
                    EcrActionTypes.CLOSE_OUT_EICR,
                    details.getStartDate(),
                    taskInstanceId);
                state.getCloseOutEicrStatus().setJobStatus(JobStatus.SCHEDULED);
                EcaUtils.updateDetailStatus(details, state);

                logger.info(" **** END Executing Close Out Eicr Action **** ");
              }
            } else {
              logger.info(" Job Not Scheduled since there is no timing data ");
            }

          } else if (state.getCloseOutEicrStatus().getJobStatus() == JobStatus.SCHEDULED
              && launchType == WorkflowEvent.SCHEDULED_JOB) {

            logger.info(" Creating the Close Out EICR since the job has been scheduled ");

            // Check Trigger Codes again in case the data has changed.
            PatientExecutionState newState = EcaUtils.recheckTriggerCodes(details, launchType);

            if (Boolean.TRUE.equals(newState.getMatchTriggerStatus().getTriggerMatchStatus())
                && newState.getMatchTriggerStatus().getMatchedCodes() != null
                && !newState.getMatchTriggerStatus().getMatchedCodes().isEmpty()) {

              logger.info(
                  "Creating the EICR for {} action as new trigger code is matched",
                  EcrActionTypes.CLOSE_OUT_EICR);
              // Since the job has started, Execute the job.
              // Call the Loading Queries and create eICR.
              Eicr ecr = EcaUtils.createEicr(details);
              logger.info(
                  " EICR created successfully for {} with eICRDocID: {} version: {}",
                  EcrActionTypes.CLOSE_OUT_EICR,
                  ecr.getEicrDocId(),
                  ecr.getDocVersion());

              newState.getCloseOutEicrStatus().setEicrClosed(true);
              newState.getCloseOutEicrStatus().seteICRId(ecr.getId().toString());
              newState.getCloseOutEicrStatus().setJobStatus(JobStatus.COMPLETED);
              details.setProcessingState(LaunchDetails.getString(ProcessingStatus.Completed));

              EcaUtils.updateDetailStatus(details, newState);

              logger.debug(" **** Printing Eicr from CLOSE OUT EICR ACTION **** ");

              String fileName =
                  ActionRepo.getInstance().getLogFileDirectory()
                      + "/"
                      + details.getLaunchPatientId()
                      + "_CloseOutEicrAction"
                      + LocalDateTime.now().getHour()
                      + LocalDateTime.now().getMinute()
                      + LocalDateTime.now().getSecond()
                      + ".xml";
              ApplicationUtils.saveDataToFile(ecr.getEicrData(), fileName);

              logger.debug(" **** End Printing Eicr from CLOSE OUT EICR ACTION **** ");

            } // Check if Trigger Code Match found
            else {

              logger.info(" **** Trigger Code did not match, hence not creating EICR **** ");

              newState.getCloseOutEicrStatus().setEicrClosed(false);
              newState.getCloseOutEicrStatus().seteICRId("0");
              newState.getCloseOutEicrStatus().setJobStatus(JobStatus.COMPLETED);
              details.setProcessingState(LaunchDetails.getString(ProcessingStatus.Completed));

              EcaUtils.updateDetailStatus(details, newState);
            }
          } else {
            logger.info(
                " Close Out Eicr Action not creating Eicr because state = {}",
                state.getCloseOutEicrStatus().getJobStatus());
          }
        } else {
          logger.info(" Related Actions are not completed, hence EICR will not be created.");
        }

      } else if (encounterClosed) {

        logger.info(" Conditions not met, hence Close Out Action will have to be rescheduled . ");
        List<RelatedAction> racts = getRelatedActions();

        for (RelatedAction ract : racts) {

          if (ract.getRelationship() == ActionRelationshipType.AFTER) {
            logger.info(" Scheduling the job using related actions ");
            scheduleJob(details, state, ract, mapper, taskInstanceId);
          }
        }
      } else {
        logger.info(" Encounter is not closed, hence close out action will not be scheduled. ");
      }
    } else {

      String msg =
          "Invalid Object passed to Execute method, Launch Details expected, found : "
              + (obj != null ? obj.getClass().getName() : null);
      logger.error(msg);

      throw new ObjectDeletedException(msg, "0", "launchDetails");
    }

    logger.info(
        " **** END Executing Close Out Eicr Action after completing normal execution. **** ");
  }

  public void scheduleJob(
      LaunchDetails details,
      PatientExecutionState state,
      RelatedAction ract,
      ObjectMapper mapper,
      String taskInstanceId) {

    try {
      logger.info(" **** Start Scheduling Close Out Eicr Action Job **** ");
      List<TimingSchedule> tsjobs = getTimingData();

      if (tsjobs != null) {
        for (TimingSchedule ts : tsjobs) {

          // TBD : Setup job using TS Timing after testing so that we can test faster.
          // For now setup a default job with 10 seconds.

          WorkflowService.scheduleJob(
              details.getId(),
              ts,
              EcrActionTypes.CLOSE_OUT_EICR,
              details.getStartDate(),
              taskInstanceId);
        }
      } else {
        WorkflowService.scheduleJob(
            details.getId(),
            ract.getDuration(),
            EcrActionTypes.CLOSE_OUT_EICR,
            details.getStartDate(),
            taskInstanceId);
      }

      state.getCloseOutEicrStatus().setJobStatus(JobStatus.SCHEDULED);
      details.setStatus(mapper.writeValueAsString(state));

      // No need to continue as the job will take over execution.

      logger.info(" **** Finished Scheduling Close Out Eicr Action Job **** ");
    } catch (JsonProcessingException e) {
      String msg = "Unable to read/write execution state";
      logger.error(msg, e);
      throw new RuntimeException(msg);
    }
  }

  @Override
  public void print() {

    logger.info(" **** Printing CloseOutEicrAction **** ");
    printBase();
    logger.info(" **** End Printing CloseOutEicrAction **** ");
  }
}
