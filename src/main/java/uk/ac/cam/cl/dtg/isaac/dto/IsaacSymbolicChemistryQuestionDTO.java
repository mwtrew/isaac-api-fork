/*
 * Copyright 2016 Ian Davies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacOldSymbolicChemistryValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

@JsonContentType("isaacSymbolicChemistryQuestion")
@ValidatesWith(IsaacOldSymbolicChemistryValidator.class)
public class IsaacSymbolicChemistryQuestionDTO extends IsaacSymbolicQuestionDTO {
    @JsonProperty("isNuclear")
    private boolean isNuclear;

    /**
     * @return whether the question is a nuclear question or not
     */
    public boolean isNuclear() {
        return isNuclear;
    }

    /**
     * @param nuclear set whether the question is a nuclear question or not
     */
    public void setNuclear(boolean nuclear) {
        isNuclear = nuclear;
    }
}
