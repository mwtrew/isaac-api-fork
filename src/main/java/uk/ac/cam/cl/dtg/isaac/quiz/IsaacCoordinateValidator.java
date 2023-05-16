package uk.ac.cam.cl.dtg.isaac.quiz;

import com.google.common.collect.Streams;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCoordinateQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.CoordinateChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.CoordinateItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class IsaacCoordinateValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacCoordinateValidator.class);

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacCoordinateQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacCoordinateQuestion (%s is not IsaacCoordinateQuestion)", question.getId()));
        }

        if (!(answer instanceof CoordinateChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected CoordinateChoice for IsaacCoordinateQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        IsaacCoordinateQuestion coordinateQuestion = (IsaacCoordinateQuestion) question;
        CoordinateChoice submittedChoice = (CoordinateChoice) answer;
        // Potentially unsafe cast, but we trust the front end to have sent us the right type of items.
        List<CoordinateItem> submittedItems = submittedChoice.getItems().stream().map(i -> (CoordinateItem) i).collect(Collectors.toList());

        // Check if any coordinates are missing
        if (submittedItems.stream().anyMatch(i -> null == i.getX() || null == i.getY())) {
            feedback = new Content("You did not provide a complete answer.");
        }

        // Extract significant figure bounds, defaulting to NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES either are missing
        int significantFiguresMax = Objects.requireNonNullElse(coordinateQuestion.getSignificantFiguresMax(), ValidationUtils.NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES);
        int significantFiguresMin = Objects.requireNonNullElse(coordinateQuestion.getSignificantFiguresMin(), ValidationUtils.NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES);

        // STEP 0: Is it even possible to answer this question?

        if (null == coordinateQuestion.getChoices() || coordinateQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any correct answers!");
        }

        // STEP 1: Did they provide a valid answer?

        if (null == feedback && (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty())) {
            feedback = new Content("You did not provide an answer.");
        }

        if (null != coordinateQuestion.getNumberOfCoordinates() && submittedChoice.getItems().size() != coordinateQuestion.getNumberOfCoordinates()) {
            feedback = new Content("You did not provide the required number of coordinates.");
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback) {
            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(coordinateQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the CoordinateChoice type, ...
                if (!(c instanceof CoordinateChoice)) {
                    log.error(String.format(
                            "Validator for question (%s) expected there to be an CoordinateChoice. Instead it found a %s.",
                            coordinateQuestion.getId(), c.getClass().toString()));
                    continue;
                }

                CoordinateChoice coordinateChoice = (CoordinateChoice) c;

                // ... and that contain items ...
                if (null == coordinateChoice.getItems() || coordinateChoice.getItems().isEmpty()) {
                    log.error("Expected list of CoordinateItems, but none found in choice for question id: "
                            + coordinateQuestion.getId());
                    continue;
                }

                // ... look for a match to the submitted answer.
                if (coordinateChoice.getItems().size() != submittedChoice.getItems().size()) {
                    // We know that we don't have a match if the number of items is different.
                    continue;
                }

                // Ensure that all items in the choice are CoordinateItems.
                boolean allCoordinateItems = coordinateChoice.getItems().stream().allMatch(i -> i instanceof CoordinateItem);
                if (!allCoordinateItems) {
                    log.error("Expected list of CoordinateItems, but found a different type of item in choice for question id: "
                            + coordinateQuestion.getId());
                    // Hopefully another choice will be better...
                    continue;
                }
                List<CoordinateItem> choiceItems = coordinateChoice.getItems().stream().map(i -> (CoordinateItem) i).collect(Collectors.toList());

                // Check that the items in the submitted answer match the items in the choice numerically

                // If the question is unordered, then we need to (paradoxically) order the items to compare them.
                if (null == coordinateQuestion.getOrdered() || !coordinateQuestion.getOrdered()) {
                    choiceItems = orderCoordinates(choiceItems);
                    submittedItems = orderCoordinates(submittedItems);
                }

                // Zip the two lists together and check that the items match.
                boolean allItemsMatch = Streams.zip(
                        choiceItems.stream(),
                        submittedItems.stream(),
                        (choiceItem, submittedItem) -> {
                            Integer xSigFigs = ValidationUtils.numberOfSignificantFiguresToValidateWith(
                                submittedItem.getX(),
                                significantFiguresMin,
                                significantFiguresMax,
                                log
                            );
                            Integer ySigFigs = ValidationUtils.numberOfSignificantFiguresToValidateWith(
                                submittedItem.getY(),
                                significantFiguresMin,
                                significantFiguresMax,
                                log
                            );
                            return ValidationUtils.numericValuesMatch(
                                choiceItem.getX(),
                                submittedItem.getX(),
                                xSigFigs,
                                log
                            ) && ValidationUtils.numericValuesMatch(
                                choiceItem.getY(),
                                submittedItem.getY(),
                                ySigFigs,
                                log
                            );
                        }
                    ).allMatch(b -> b);

                if (allItemsMatch) {
                    responseCorrect = coordinateChoice.isCorrect();
                    feedback = (Content) coordinateChoice.getExplanation();
                    break;
                }
            }
        }

        // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != coordinateQuestion.getDefaultFeedback()) {
            feedback = coordinateQuestion.getDefaultFeedback();
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
    }

    /**
     * Lexicographically order the items in the list by their x coordinate, then by their y coordinate.
     * @param items     The list of coordinate items to order
     * @return          The (lexicographically) ordered list of coordinate items
     */
    private List<CoordinateItem> orderCoordinates(final List<CoordinateItem> items) {
        return items.stream().sorted((a, b) -> {
            if (a.getX().equals(b.getX())) {
                return a.getY().compareTo(b.getY());
            } else {
                return a.getX().compareTo(b.getX());
            }
        }).collect(Collectors.toList());
    }
}
