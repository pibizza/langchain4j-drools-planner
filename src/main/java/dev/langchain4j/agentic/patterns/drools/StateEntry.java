package dev.langchain4j.agentic.patterns.drools;

public class StateEntry {
    private final String key;
    private final String value;

    public StateEntry(String key, Object value) {
        this.key = key;
        this.value = value.toString();
    }

    public String getKey() { 
    	return key; 
    }
    public String getValue() { 
    	return value; 
    	
    }
}

