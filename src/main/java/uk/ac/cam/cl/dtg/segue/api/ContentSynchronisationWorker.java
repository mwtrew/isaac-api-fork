package uk.ac.cam.cl.dtg.segue.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentSynchronisationWorker implements Runnable{
	private static final Logger log = LoggerFactory.getLogger(ContentSynchronisationWorker.class);
	
	private ContentVersionController contentVersionController;
	private String version; // null for latest, set for a particular version	

	public ContentSynchronisationWorker(ContentVersionController contentVersionController, String version){
		this.contentVersionController = contentVersionController;
		this.version = version;
	}
	
	public ContentSynchronisationWorker(ContentVersionController contentVersionController){
		this(contentVersionController, null);
	}
	
	@Override
	public void run() {		
		// Verify with Content manager that we can sync to the version requested / get the latest one
		log.info("Starting synchronisation task for the content repository.");
		
		if(null == version){
			// assume we are just trying to get the latest version when we have a null version field.
			version = contentVersionController.getContentManager().getLatestVersionId();
		}
		
		if(contentVersionController.getContentManager().isValidVersion(version)){
			// trigger index operation with content Manager.
			log.info("Triggering index operation as a result of Content Synchronisation job.");
			
			if(this.contentVersionController.getContentManager().ensureCache(version)){
				// successful indexing complete.
				// Call the content controller to tell them we have finished our job and they may like to do something.
				contentVersionController.syncJobCompleteCallback(version, true);
				
				log.info("Content synchronisation job complete");
				return;
			}
		}

		log.error("Error while trying to run index operation for version: " +version +" . Terminating Sync Job.");
		// call the content controller to tell them we failed to sync the version requested
		contentVersionController.syncJobCompleteCallback(version, false);
		
		return;
	}
	
}