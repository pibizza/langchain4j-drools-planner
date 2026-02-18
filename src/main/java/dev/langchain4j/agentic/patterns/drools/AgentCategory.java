package dev.langchain4j.agentic.patterns.drools;

public class AgentCategory {
    public AgentCategory(String agentName, String categoryName, Object categoryValue) {
		this.agentName = agentName;
		this.categoryName = categoryName;
		this.categoryValue = categoryValue;
	}
	private final String agentName;
	private final String categoryName;
    private final Object categoryValue;
    
    public String getAgentName() {
		return agentName;
	}
	public String getCategoryName() {
		return categoryName;
	}
	public Object getCategoryValue() {
		return categoryValue;
	}

}