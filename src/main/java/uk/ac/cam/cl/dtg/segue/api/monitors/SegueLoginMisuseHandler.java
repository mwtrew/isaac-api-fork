/*
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.api.monitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import com.google.inject.Inject;

/**
 * Handler to detect bruteforce login attempts.
 * 
 * Preventing users from overusing this endpoint is important as they may be trying to brute force someones password.
 *
 */
public class SegueLoginMisuseHandler implements IMisuseHandler {
    private static final Logger log = LoggerFactory.getLogger(SegueLoginMisuseHandler.class);

    public static final Integer SOFT_THRESHOLD = 5;
    public static final Integer HARD_THRESHOLD = 10;
    public static final Integer ACCOUNTING_INTERVAL = Constants.NUMBER_SECONDS_IN_TEN_MINUTES;

    private AbstractConfigLoader properties;
    private EmailManager emailManager;

    /**
     * @param emailManager
     *            - so we can send e-mails if the threshold limits have been reached.
     * @param properties
     *            - so that we can look up properties set.
     */
    @Inject
    public SegueLoginMisuseHandler(final EmailManager emailManager, final AbstractConfigLoader properties) {
        this.properties = properties;
        this.emailManager = emailManager;
    }

    @Override
    public Integer getSoftThreshold() {
        return SOFT_THRESHOLD;
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.segue.api.managers.IMisuseEvent#getHardThreshold()
     */
    @Override
    public Integer getHardThreshold() {
        return HARD_THRESHOLD;
    }

    @Override
    public Integer getAccountingIntervalInSeconds() {
        return ACCOUNTING_INTERVAL;
    }

    @Override
    public void executeSoftThresholdAction(final String message) {
        log.warn("Soft threshold limit: " + message);
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        log.warn("Hard threshold limit: " + message);
    }
}
