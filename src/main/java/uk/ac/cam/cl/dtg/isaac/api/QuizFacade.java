/*
 * Copyright 2021 Raspberry Pi Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentCancelledException;
import uk.ac.cam.cl.dtg.isaac.api.managers.AttemptCompletedException;
import uk.ac.cam.cl.dtg.isaac.api.managers.DueBeforeNowException;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateAssignmentException;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAttemptManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ASSIGNMENT_DUEDATE_FK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.GROUP_FK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_HOUR;
import static uk.ac.cam.cl.dtg.segue.api.Constants.QUIZ_ASSIGNMENT_FK;

/**
 * Quiz Facade
 */
@Path("/quiz")
@Api(value = "/quiz")
public class QuizFacade extends AbstractIsaacFacade {
    private final IContentManager contentManager;
    private final QuizManager quizManager;
    private final UserAccountManager userManager;
    private final GroupManager groupManager;
    private final QuizAssignmentManager quizAssignmentManager;
    private final AssignmentService assignmentService;
    private final QuizAttemptManager quizAttemptManager;

    private static final Logger log = LoggerFactory.getLogger(QuizFacade.class);

    /**
     * QuizFacade. For management of quizzes
     * @param properties
     *            - global properties map
     * @param logManager
     *            - for managing logs.
     * @param contentManager
     *            - for the content etag
     * @param quizManager
     *            - for quiz interaction.
     * @param userManager
     *            - for user information.
     * @param groupManager
     *            - for group information.
     * @param quizAssignmentManager
     *            - for managing the assignment of quizzes.
     * @param assignmentService
     *            - for general assignment-related services.
     * @param quizAttemptManager
     *            - for managing attempts at quizzes.
     */
    @Inject
    public QuizFacade(final PropertiesLoader properties, final ILogManager logManager,
                      final IContentManager contentManager, final QuizManager quizManager,
                      final UserAccountManager userManager, final GroupManager groupManager,
                      final QuizAssignmentManager quizAssignmentManager, final AssignmentService assignmentService,
                      final QuizAttemptManager quizAttemptManager) {
        super(properties, logManager);

        this.contentManager = contentManager;
        this.quizManager = quizManager;
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.quizAssignmentManager = quizAssignmentManager;
        this.assignmentService = assignmentService;
        this.quizAttemptManager = quizAttemptManager;
    }

    /**
     * Get quizzes visible to this user.
     *
     * Anonymous users can't see quizzes.
     * Students can see quizzes with the visibleToStudents flag set.
     * Teachers and higher can see all quizzes.
     *
     * @return a Response containing a list of ContentSummaryDTO for the visible quizzes.
     */
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get quizzes visible to this user.")
    public final Response getAvailableQuizzes(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            boolean isStudent = !isUserTeacherOrAbove(userManager, user);

            EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + "");

            ResultsWrapper<ContentSummaryDTO> summary = this.quizManager.getAvailableQuizzes(isStudent, null, null);

            return ok(summary).tag(etag)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, isStudent))
                .build();
        } catch (ContentManagerException e) {
            String message = "ContentManagerException whilst getting available quizzes";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }


    /**
     * Get quizzes assigned to this user.
     *
     * Shows a content summary, so we can track when a user actually attempts a quiz.
     *
     * @return a Response containing a list of QuizAssignmentDTO for the assigned quizzes.
     */
    @GET
    @Path("assignments")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get quizzes assigned to this user.")
    public final Response getAssignedQuizzes(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            List<QuizAssignmentDTO> quizzes = this.quizAssignmentManager.getAssignedQuizzes(user);

            this.assignmentService.augmentAssignerSummaries(quizzes);

            for (QuizAssignmentDTO quiz: quizzes) {
                quiz.setQuiz(this.contentManager.extractContentSummary(this.quizManager.findQuiz(quiz.getQuizId())));
            }

            return Response.ok(quizzes)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst getting available quizzes";
            log.error(message, e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            String message = "ContentManagerException whilst getting available quizzes";
            log.error(message, e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Preview a quiz. Only available to teachers and above.
     *
     * @param request
     *            - so we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizId
     *            as a string
     * @return a Response containing a list of ContentSummaryDTO for the visible quizzes.
     */
    @GET
    @Path("/{quizId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Preview an individual quiz.")
    public final Response previewQuiz(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
                                      @PathParam("quizId") final String quizId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            if (null == quizId || quizId.isEmpty()) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz id.").toResponse();
            }

            EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + quizId.hashCode() + "");

            Response cachedResponse = generateCachedResponse(request, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            IsaacQuizDTO quiz = this.quizManager.findQuiz(quizId);

            return ok(quiz)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, false)).tag(etag).build();
        } catch (ContentManagerException e) {
            String message = "ContentManagerException whilst previewing a quiz";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Start a quiz attempt for a particular quiz assignment.
     *
     * For a student, indicates the quiz is being attempted and creates a QuizAttemptDO in the DB
     * if they are a member of the group for the assignment.
     * If an attempt has already begun, return the already begun attempt.
     *
     * TODO: @see startFreeQuizAttempt An endpoint to allow a quiz that is visibleToStudents to be taken by students.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAssignmentId
     *            - the ID of the quiz assignment for this user.
     * @return a QuizAttemptDTO
     */
    @POST
    @Path("/startQuizAttempt")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Start a quiz attempt.")
    public final Response startQuizAttempt(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
                                      @FormParam("quizAssignmentId") final Long quizAssignmentId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (null == quizAssignmentId) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz assignment id.").toResponse();
            }

            // Get the quiz assignment
            QuizAssignmentDTO quizAssignment = this.quizAssignmentManager.getById(quizAssignmentId);

            // Check the user is an active member of the relevant group
            UserGroupDTO group = groupManager.getGroupById(quizAssignment.getGroupId());
            if (!groupManager.isUserInGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not a member of a group assigned this quiz.").toResponse();
            }

            // Check the due date hasn't passed
            if (quizAssignment.getDueDate() != null && new Date().after(quizAssignment.getDueDate())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "The due date for this quiz has passed.").toResponse();
            }

            // Create a quiz attempt
            QuizAttemptDTO quizAttempt = this.quizAttemptManager.fetchOrCreate(quizAssignment, user);

            // TODO: Might as well augment the result to avoid a round-trip

            return Response.ok(quizAttempt).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst starting a quiz attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AttemptCompletedException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "You have already completed your attempt at this quiz.").toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.GONE, "This quiz assignment has been cancelled.").toResponse();
        }
    }

    /**
     * Start a quiz attempt for a free quiz (one that is visibleToStudents).
     *
     * This checks that quiz has not already been assigned. (When a quiz has been set to a student,
     * they are locked out of all previous feedback for that quiz and prevented from starting the
     * quiz freely in order to prevent some ways of cheating.)
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizId
     *            - the ID of the quiz the user wishes to attempt.
     * @return a QuizAttemptDTO
     */
    @POST
    @Path("/startFreeQuizAttempt")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Start a free quiz attempt.")
    public final Response startFreeQuizAttempt(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
                                           @FormParam("quizId") final String quizId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (null == quizId || quizId.isEmpty()) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz id.").toResponse();
            }

            // Get the quiz
            IsaacQuizDTO quiz = quizManager.findQuiz(quizId);

            // Check it is visibleToStudents
            if (!quiz.getVisibleToStudents()) {
                return new SegueErrorResponse(Status.FORBIDDEN, "This quiz is not available for students to attempt freely.").toResponse();
            }

            // Check if there is an active assignment of this quiz
            List<QuizAssignmentDTO> activeQuizAssignments = this.quizAssignmentManager.getActiveQuizAssignments(quiz, user);

            if (!activeQuizAssignments.isEmpty()) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are currently set this quiz so you cannot attempt it freely.").toResponse();
            }

            // Create a quiz attempt
            QuizAttemptDTO quizAttempt = this.quizAttemptManager.fetchOrCreateFreeQuiz(quiz, user);

            // TODO: Might as well augment the result to avoid a round-trip

            return Response.ok(quizAttempt).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst starting a free quiz attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            String message = "ContentManagerException whilst starting a free quiz attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /**
     * Allows a teacher to set a quiz to a group.
     *
     * Sends emails to the members of the group informing them of the quiz.
     * Takes a quiz id, a group id, an optional end datetime for completion, and the feedbackMode.
     *
     * @param request so that we can extract user information.
     * @param clientQuizAssignment the partially constructed quiz assignment
     *
     * @return the quiz assignment that has been created, or an error.
     */
    @POST
    @Path("/createQuizAssignment")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Set a quiz to a group, with an optional due date.")
    public final Response createQuizAssignment(@Context final HttpServletRequest request,
                                               final QuizAssignmentDTO clientQuizAssignment) {

        if (   clientQuizAssignment.getQuizId() == null
            || clientQuizAssignment.getGroupId() == null
            || clientQuizAssignment.getQuizFeedbackMode() == null) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "A required field was missing. Must provide group and quiz ids and a quiz feedback mode.").toResponse();
        }

        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            if (!(isUserTeacherOrAbove(userManager, currentlyLoggedInUser))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            UserGroupDTO assigneeGroup = groupManager.getGroupById(clientQuizAssignment.getGroupId());
            if (null == assigneeGroup) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The group id specified does not exist.")
                    .toResponse();
            }

            if (!GroupManager.isOwnerOrAdditionalManager(assigneeGroup, currentlyLoggedInUser.getId())
                && !isUserAnAdmin(userManager, currentlyLoggedInUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only set assignments to groups you own or manage.").toResponse();
            }

            IsaacQuizDTO quiz = this.quizManager.findQuiz(clientQuizAssignment.getQuizId());
            if (null == quiz) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The quiz id specified does not exist.")
                    .toResponse();
            }

            clientQuizAssignment.setOwnerUserId(currentlyLoggedInUser.getId());
            clientQuizAssignment.setCreationDate(null);
            clientQuizAssignment.setId(null);

            // modifies assignment passed in to include an id.
            QuizAssignmentDTO assignmentWithID = this.quizAssignmentManager.createAssignment(clientQuizAssignment);

            Map<String, Object> eventDetails = ImmutableMap.of(
                Constants.QUIZ_ID_FKEY, assignmentWithID.getQuizId(),
                GROUP_FK, assignmentWithID.getGroupId(),
                QUIZ_ASSIGNMENT_FK, assignmentWithID.getId(),
                ASSIGNMENT_DUEDATE_FK, assignmentWithID.getDueDate() == null ? "NO_DUE_DATE" : assignmentWithID.getDueDate()
            );

            this.getLogManager().logEvent(currentlyLoggedInUser, request, Constants.IsaacServerLogType.SET_NEW_QUIZ_ASSIGNMENT, eventDetails);

            return Response.ok(assignmentWithID).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (DuplicateAssignmentException|DueBeforeNowException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst setting quiz";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            String message = "Content error whilst setting quiz";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /**
     * Allows a teacher to set a quiz to a group.
     *
     * Sends emails to the members of the group informing them of the quiz.
     * Takes a quiz id, a group id, an optional end datetime for completion, and the feedbackMode.
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment to cancel.
     *
     * @return Confirmation or error.
     */
    @DELETE
    @Path("/quizAssignment/{quizAssignmentId}")
    @ApiOperation(value = "Cancel a quiz assignment.")
    public final Response cancelQuizAssignment(@Context final HttpServletRequest httpServletRequest,
                                               @PathParam("quizAssignmentId") Long quizAssignmentId) {

        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz assignment id.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            UserGroupDTO assigneeGroup = groupManager.getGroupById(assignment.getGroupId());

            if (!GroupManager.isOwnerOrAdditionalManager(assigneeGroup, user.getId())
                && !isUserAnAdmin(userManager, user)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only cancel assignments to groups you own or manage.").toResponse();
            }

            quizAssignmentManager.cancelAssignment(assignment);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment is already cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst cancelling quiz assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }
}
