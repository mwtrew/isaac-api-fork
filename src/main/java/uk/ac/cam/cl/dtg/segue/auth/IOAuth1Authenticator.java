/*
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.auth;

import java.io.IOException;

/**
 * This interface defines the required methods for an oauth 1.0 provider
 * Derived from IOAuth2Authenticator.
 * 
 * @author nr378
 */
public interface IOAuth1Authenticator extends IOAuthAuthenticator {
	/**
	 * Step 1 of OAUTH1 - Contact the OAUTH server to retrieve a request token
	 * which can be used for subsequent operations.
	 * 
	 * @return String - An OAUTH1 token/secret pair.
	 * @throws IOException
	 *             - if there is a problem with the end point.
	 */
	OAuth1Token getRequestToken() throws IOException;
	
	/**
	 * Step 2 of OAUTH1 - Request authorisation URL. Request an authorisation
	 * url which will allow the user to be logged in with their oauth provider.
	 * 
	 * @param token - An OAUTH1 token/secret pair generated by getRequestToken()
	 * @return String - A url which should be fully formed and ready for the
	 *         user to login with - this should result in a callback to a
	 *         prearranged api endpoint if successful.
	 */
	String getAuthorizationUrl(OAuth1Token token);
}
