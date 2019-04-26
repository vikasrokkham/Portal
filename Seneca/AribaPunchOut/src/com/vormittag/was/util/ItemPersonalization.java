package com.vormittag.was.util;

public class ItemPersonalization {
	private String templateName = "";
	private int personalizationId = 0;
	private String personalizationDescription = "";
	private String personalizationValue = "";
	private String defaultWasChanged = "N";
	private String templateClass = "";
	
	public String getTemplateName() {
		return templateName;
	}
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	public int getPersonalizationId() {
		return personalizationId;
	}
	public void setPersonalizationId(int personalizationId) {
		this.personalizationId = personalizationId;
	}
	public String getPersonalizationDescription() {
		return personalizationDescription;
	}
	public void setPersonalizationDescription(String personalizationDescription) {
		this.personalizationDescription = personalizationDescription;
	}
	public String getPersonalizationValue() {
		return personalizationValue;
	}
	public void setPersonalizationValue(String personalizationValue) {
		this.personalizationValue = personalizationValue;
	}	
	public String getDefaultWasChanged() {
		return defaultWasChanged;
	}
	public void setDefaultWasChanged(String defaultWasChanged) {
		this.defaultWasChanged = defaultWasChanged;
	}
	public String getTemplateClass() {
		return templateClass;
	}
	public void setTemplateClass(String templateClass) {
		this.templateClass = templateClass;
	}
}
