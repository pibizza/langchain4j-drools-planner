package dev.langchain4j.agentic.patterns.drools;

public class StateEntry {
    private final String key;
    private final Object value;

    public StateEntry(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { 
    	return key; 
    }
    public Object getValue() { 
    	return value; 
    	
    }
}

