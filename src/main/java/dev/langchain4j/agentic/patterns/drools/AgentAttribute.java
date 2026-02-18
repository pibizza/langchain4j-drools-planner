package dev.langchain4j.agentic.patterns.drools;

public class AgentAttribute {
    public AgentAttribute(String agentName, String domain, double score) {
		this.agentName = agentName;
		this.domain = domain;
		this.score = score;
	}
	private String agentName;
	private String domain;
    private double score;

    public String getAgentName() {
		return agentName;
	}
	public String getDomain() {
		return domain;
	}
	public double getScore() {
		return score;
	}
}