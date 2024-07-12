package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

import java.util.List;

/**
 * This class is the DTO used to transfer user assignment progress information.
 */
public class AssignmentProgressDTO {
    public UserSummaryDTO user;
    public List<Integer> correctPartResults;
    public List<Integer> incorrectPartResults;
    public Constants.GameboardItemState[] results;

    /**
     * Complete AssignmentProgressDTO constructor with all dependencies.
     * @param user
     *            - UserSummaryDTO of the user.
     * @param correctPartResults
     *            - List of correct part results.
     * @param incorrectPartResults
     *            - List of incorrect part results.
     * @param results
     *            - Array of results.
     */
    public AssignmentProgressDTO(UserSummaryDTO user, List<Integer> correctPartResults, List<Integer> incorrectPartResults, Constants.GameboardItemState[] results) {
        this.user = user;
        this.correctPartResults = correctPartResults;
        this.incorrectPartResults = incorrectPartResults;
        this.results = results;
    }

    public AssignmentProgressDTO() {
    }

    public UserSummaryDTO getUser() {
        return user;
    }

    public void setUser(UserSummaryDTO user) {
        this.user = user;
    }

    public List<Integer> getCorrectPartResults() {
        return correctPartResults;
    }

    public void setCorrectPartResults(List<Integer> correctPartResults) {
        this.correctPartResults = correctPartResults;
    }

    public List<Integer> getIncorrectPartResults() {
        return incorrectPartResults;
    }

    public void setIncorrectPartResults(List<Integer> incorrectPartResults) {
        this.incorrectPartResults = incorrectPartResults;
    }

    public Constants.GameboardItemState[] getResults() {
        return results;
    }

    public void setResults(Constants.GameboardItemState[] results) {
        this.results = results;
    }
}
