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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.api.ResponseWrapper;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class QuizQuestionManager {
    private final QuestionManager questionManager;
    private final ContentMapper mapper;
    private final IQuizQuestionAttemptPersistenceManager quizQuestionAttemptManager;

    private static final Logger log = LoggerFactory.getLogger(QuizQuestionManager.class);

    /**
     * Manage questions on quizzes.
     *
     * Delegates some behaviour to QuestionManager for the safety of consumers who msut not confuse questions with
     * quiz questions.
     *
     * @param questionManager
     *            - for parsing and validating question answers.
     * @param mapper
     *            - an auto mapper to allow us to convert to and from QuestionValidationResponseDOs and DTOs.
     * @param quizQuestionAttemptManager
     *            - for quiz question attempt persistence.
     */
    @Inject
    public QuizQuestionManager(final QuestionManager questionManager, final ContentMapper mapper, final IQuizQuestionAttemptPersistenceManager quizQuestionAttemptManager) {
        this.questionManager = questionManager;
        this.mapper = mapper;
        this.quizQuestionAttemptManager = quizQuestionAttemptManager;
    }

    public ChoiceDTO convertJsonAnswerToChoice(String jsonAnswer) throws ResponseWrapper {
        return questionManager.convertJsonAnswerToChoice(jsonAnswer);
    }

    public QuestionValidationResponseDTO validateAnswer(Question question, ChoiceDTO answerFromClientDTO) throws ResponseWrapper {
        Response response = questionManager.validateAnswer(question, answerFromClientDTO);
        if (response.getEntity() instanceof QuestionValidationResponseDTO) {
            return (QuestionValidationResponseDTO) response.getEntity();
        } else if (response.getEntity() instanceof SegueErrorResponse) {
            throw new ResponseWrapper((SegueErrorResponse) response.getEntity());
        } else {
            throw new ResponseWrapper(new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, response.getEntity().toString()));
        }
    }

    public void recordQuestionAttempt(QuizAttemptDTO quizAttempt, QuestionValidationResponseDTO questionResponse) throws SegueDatabaseException {
        QuestionValidationResponse questionResponseDO = this.mapper.getAutoMapper().map(questionResponse, QuestionValidationResponse.class);

        this.quizQuestionAttemptManager.registerQuestionAttempt(quizAttempt.getId(), questionResponseDO);
    }
}
