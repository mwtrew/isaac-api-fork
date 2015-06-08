/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.search;

/**
 * An exception that indicates a problem occurred whilst interacting with the third party search provider.
 * 
 * @author Stephen Cummins
 */
public class SegueSearchOperationException extends Exception {
    private static final long serialVersionUID = -2812103839917179690L;

    /**
     * Default constructor.
     */
    public SegueSearchOperationException() {

    }

    /**
     * constructor with message.
     * 
     * @param message
     *            - error message
     */
    public SegueSearchOperationException(final String message) {
        super(message);
    }

    /**
     * constructor with message.
     * 
     * @param message
     *            - error message
     * @param e
     *            - wrapped exception
     */
    public SegueSearchOperationException(final String message, final Exception e) {
        super(message, e);
    }
}
