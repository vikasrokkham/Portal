package com.vormittag.was.util;

public class ItemCustomizationSetting {
	private String templateName = "";
	private String templateClass = "";
	private String sizing = "N";
	private String personalization = "N";
	private String quantityOne = "N";
	private String classRequired = "N";
	private String errorCode = "";
	private String message = "";
	
	public String getTemplateName() {
		return templateName;
	}
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	public String getTemplateClass() {
		return templateClass;
	}
	public void setTemplateClass(String templateClass) {
		this.templateClass = templateClass;
	}	
	public String getSizing() {
		return sizing;
	}
	public void setSizing(String sizing) {
		this.sizing = sizing;
	}
	public String getPersonalization() {
		return personalization;
	}
	public void setPersonalization(String personalization) {
		this.personalization = personalization;
	}
	public String getQuantityOne() {
		return quantityOne;
	}
	public void setQuantityOne(String quantityOne) {
		this.quantityOne = quantityOne;
	}
	public String getClassRequired() {
		return classRequired;
	}
	public void setClassRequired(String classRequired) {
		this.classRequired = classRequired;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
