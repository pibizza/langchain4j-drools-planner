package dev.langchain4j.agentic.patterns.drools;

import java.util.Map;

import dev.langchain4j.agentic.planner.AgentInstance;

public record DroolsAgent(
	      AgentInstance agentInstance,  // The actual agent (medicalExpert, etc.)
	      Map<String, String> categories,
	      Map<String, Double> attributes
	  ) {

	
}

