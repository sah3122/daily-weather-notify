package com.api.main;

public enum Type {
	HOUR("hour"), TEMP("temp"), WFKOR("wfKor");//, TEMPMAX("tmx"), TEMPMIN("tmn");
	
	final private String name;
    
    private Type(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
