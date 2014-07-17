package uk.ac.cam.cl.dtg.isaac.quiz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

/**
 * Validator that only provides functionality to validate Numeric questions.
 * 
 * 
 */
public class IsaacNumericValidator implements IValidator {
	private static final Logger log = LoggerFactory
			.getLogger(IsaacNumericValidator.class);

	@Override
	public final QuestionValidationResponse validateQuestionResponse(
			final Question question, final Choice answer) {
		if (question instanceof IsaacNumericQuestion) {

			IsaacNumericQuestion choiceQuestion = (IsaacNumericQuestion) question;

			if (null == choiceQuestion.getChoices() || choiceQuestion.getChoices().isEmpty()) {
				log.warn("Question does not have any answers. " 
						+ question.getId() + " src: " + question.getCanonicalSourceFile());
				
				return new QuantityValidationResponse(
						question.getId(), null,
						false, new Content(""),
						false, false);
			}
			
			if (answer instanceof Quantity) {
				Quantity answerFromUser = (Quantity) answer;
				QuantityValidationResponse bestResponse = null;
				for (Choice c : choiceQuestion.getChoices()) {
					if (c instanceof Quantity) {
						Quantity quantityChoice = (Quantity) c;

						if (null == answerFromUser.getValue()) {
							bestResponse = new QuantityValidationResponse(
									question.getId(), null
											+ " " + answerFromUser.getUnits(),
									false, new Content("You did not provide a complete answer."),
									false, false);
							break;
						}
						
						// match known choices
						if (answerFromUser.getValue().equals(
								quantityChoice.getValue())
								&& answerFromUser.getUnits().equals(
										quantityChoice.getUnits())) {
							bestResponse = new QuantityValidationResponse(
									question.getId(), answerFromUser.getValue()
											+ " " + answerFromUser.getUnits(),
									quantityChoice.isCorrect(),
									(Content) quantityChoice.getExplanation(),
									quantityChoice.isCorrect(),
									quantityChoice.isCorrect());
							// exact match so we can break
							break;
						} else if (answerFromUser.getValue().equals(
								quantityChoice.getValue())
								&& !answerFromUser.getUnits().equals(
										quantityChoice.getUnits())) {
							bestResponse = new QuantityValidationResponse(
									question.getId(), answerFromUser.getValue()
											+ " " + answerFromUser.getUnits(),
									false, new Content("Check your units."),
									true, false);
						} else if (!answerFromUser.getValue().equals(
								quantityChoice.getValue())
								&& answerFromUser.getUnits().equals(
										quantityChoice.getUnits())) {
							bestResponse = new QuantityValidationResponse(
									question.getId(), answerFromUser.getValue()
											+ " " + answerFromUser.getUnits(),
									false, new Content("Check your working."),
									false, true);
						}
					}
				}

				if (null == bestResponse) {
					// tell them they got it wrong but we cannot find an
					// feedback for them.
					return new QuestionValidationResponse(question.getId(),
							answerFromUser.getValue() + " "
									+ answerFromUser.getUnits(), false, null);
				} else {
					return bestResponse;
				}
			} else {
				log.error("Incorrect answer type received. Expected Quantity. Received: "
						+ answer.getClass());
				throw new IllegalArgumentException(
						"This type of question requires a quantity object instead of a choice");
			}
		} else {
			log.error("Incorrect validator used for question: "
					+ question.getId());
			throw new IllegalArgumentException(
					"This validator only works with Isaac Numeric Questions...");
		}
	}
}
