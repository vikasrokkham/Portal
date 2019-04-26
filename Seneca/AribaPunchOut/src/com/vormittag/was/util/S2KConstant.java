package com.vormittag.was.util;

import java.util.ArrayList;

public class S2KConstant {
	private String alphaField = "";
	private ArrayList<Double> numericConstants = new ArrayList<Double>();
	private String modsWorkField = "";

	public S2KConstant() {
	}

	public String getAlphaField() {
		return alphaField;
	}
	public void setAlphaField(String alphaField) {
		this.alphaField = alphaField;
	}
	public ArrayList<Double> getNumericConstants() {
		return numericConstants;
	}
	public void setNumericConstants(ArrayList<Double> numericConstants) {
		this.numericConstants = numericConstants;
	}
	public String getModsWorkField() {
		return modsWorkField;
	}
	public void setModsWorkField(String modsWorkField) {
		this.modsWorkField = modsWorkField;
	}
}