package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkedExample;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacLLMFreeTextQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;

@DTOMapping(IsaacLLMFreeTextQuestionDTO.class)
@JsonContentType("isaacLLMFreeTextQuestion")
@ValidatesWith(IsaacLLMFreeTextValidator.class)
public class IsaacLLMFreeTextQuestion extends Question {
    private String promptInstructionOverride;
    private List<LLMFreeTextMarkSchemeEntry> markScheme;
    private Integer maxMarks;
    private String additionalMarkingInstructions;
    private String markCalculationInstructions;
    private List<LLMFreeTextMarkedExample> markedExamples;

    public IsaacLLMFreeTextQuestion() {
    }

    public String getPromptInstructionOverride() {
        return promptInstructionOverride;
    }
    public void setPromptInstructionOverride(String promptInstructionOverride) {
        this.promptInstructionOverride = promptInstructionOverride;
    }

    public List<LLMFreeTextMarkSchemeEntry> getMarkScheme() {
        return markScheme;
    }
    public void setMarkScheme(List<LLMFreeTextMarkSchemeEntry> markScheme) {
        this.markScheme = markScheme;
    }

    public Integer getMaxMarks() {
        return maxMarks;
    }
    public void setMaxMarks(Integer maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getAdditionalMarkingInstructions() {
        return additionalMarkingInstructions;
    }
    public void setAdditionalMarkingInstructions(String additionalMarkingInstructions) {
        this.additionalMarkingInstructions = additionalMarkingInstructions;
    }

    public String getMarkCalculationInstructions() {
        return markCalculationInstructions;
    }
    public void setMarkCalculationInstructions(String markCalculationInstructions) {
        this.markCalculationInstructions = markCalculationInstructions;
    }

    public List<LLMFreeTextMarkedExample> getMarkedExamples() {
        return markedExamples;
    }
    public void setMarkedExamples(List<LLMFreeTextMarkedExample> markedExamples) {
        this.markedExamples = markedExamples;
    }
}

