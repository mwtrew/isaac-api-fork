package uk.ac.cam.cl.dtg.segue.dto;

import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.isaac.models.JsonType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Figure DTO
 * To be used anywhere that a figure should be displayed in the CMS.
 *
 */
@JsonType("figure")
public class Figure extends Content {
	
	protected String src;
	protected String altText;
	
	public Figure(){
		
	}
	
	@JsonCreator
	public Figure(@JsonProperty("_id") String _id,
			       @JsonProperty("id") String id, 
				   @JsonProperty("title") String title, 
				   @JsonProperty("type") String type, 
				   @JsonProperty("author") String author,
				   @JsonProperty("encoding") String encoding,
				   @JsonProperty("canonicalSourceFile") String canonicalSourceFile,
				   @JsonProperty("layout") String layout,
				   @JsonProperty("contentReferenced") List<ContentBase> children,
				   @JsonProperty("value") String value,
				   @JsonProperty("attribution") String attribution,
				   @JsonProperty("relatedContent") List<String> relatedContent,
				   @JsonProperty("version") boolean published,
				   @JsonProperty("tags") Set<String> tags,
				   @JsonProperty("src") String src,
				   @JsonProperty("altText") String altText) {
		super(_id, 
		      id, 
		      title, 
		      type, 
		      author, 
		      encoding, 
		      canonicalSourceFile,
		      layout, 
		      children, 
		      value, 
		      attribution, 
		      relatedContent, 
		      published,
		      tags);
		this.src = src;
		this.altText = altText;
	}
	
	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public String getAltText() {
		return altText;
	}

	public void setAltText(String altText) {
		this.altText = altText;
	}
}
