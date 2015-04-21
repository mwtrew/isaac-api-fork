/**
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import java.util.Date;

import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

/**
 * @author sac92
 *
 */
public class EventBookingDTO {
	private Long bookingId;
	
	private UserSummaryDTO userBooked;
	
	private String eventId;
	
	private String eventTitle;
	
	private Date eventDate;
	
	private Date bookingDate;

	/**
	 * 
	 */
	public EventBookingDTO() {
		
	}
	
	/**
	 * @param bookingId 
	 * @param userBooked 
	 * @param eventId 
	 * @param eventTitle 
	 * @param eventDate 
	 * @param bookingDate 
	 */
	public EventBookingDTO(final Long bookingId, final UserSummaryDTO userBooked, final String eventId,
			final String eventTitle, final Date eventDate, final Date bookingDate) {
		this.bookingId = bookingId;
		this.userBooked = userBooked;
		this.eventId = eventId;
		this.eventTitle = eventTitle;
		this.eventDate = eventDate;
		this.bookingDate = bookingDate;
	}

	/**
	 * Gets the bookingId.
	 * @return the bookingId
	 */
	public Long getBookingId() {
		return bookingId;
	}

	/**
	 * Sets the bookingId.
	 * @param bookingId the bookingId to set
	 */
	public void setBookingId(final Long bookingId) {
		this.bookingId = bookingId;
	}

	/**
	 * Gets the userBooked.
	 * @return the userBooked
	 */
	public UserSummaryDTO getUserBooked() {
		return userBooked;
	}

	/**
	 * Sets the userBooked.
	 * @param userBooked the userBooked to set
	 */
	public void setUserBooked(final UserSummaryDTO userBooked) {
		this.userBooked = userBooked;
	}

	/**
	 * Gets the eventId.
	 * @return the eventId
	 */
	public String getEventId() {
		return eventId;
	}

	/**
	 * Sets the eventId.
	 * @param eventId the eventId to set
	 */
	public void setEventId(final String eventId) {
		this.eventId = eventId;
	}

	/**
	 * Gets the eventTitle.
	 * @return the eventTitle
	 */
	public String getEventTitle() {
		return eventTitle;
	}

	/**
	 * Sets the eventTitle.
	 * @param eventTitle the eventTitle to set
	 */
	public void setEventTitle(final String eventTitle) {
		this.eventTitle = eventTitle;
	}

	/**
	 * Gets the eventDate.
	 * @return the eventDate
	 */
	public Date getEventDate() {
		return eventDate;
	}

	/**
	 * Sets the eventDate.
	 * @param eventDate the eventDate to set
	 */
	public void setEventDate(final Date eventDate) {
		this.eventDate = eventDate;
	}

	/**
	 * Gets the bookingDate.
	 * @return the bookingDate
	 */
	public Date getBookingDate() {
		return bookingDate;
	}

	/**
	 * Sets the bookingDate.
	 * @param bookingDate the bookingDate to set
	 */
	public void setBookingDate(final Date bookingDate) {
		this.bookingDate = bookingDate;
	}	
}
