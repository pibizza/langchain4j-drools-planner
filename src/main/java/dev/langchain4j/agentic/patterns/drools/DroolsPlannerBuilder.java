package dev.langchain4j.agentic.patterns.drools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.planner.AgentInstance;

public class DroolsPlannerBuilder {

    private final List<DroolsAgent> droolsAgents = new ArrayList<>();
    private String rulesSource;
    private String name;
    private String outputKey;

    /**
     * Specify the Drools rules source (e.g., "classpath:/routing-rules.drl")
     */
    public DroolsPlannerBuilder rules(String rulesSource) {
        this.rulesSource = rulesSource;
        return this;
    }

    /**
     * Add an agent just an category
     */
    public DroolsPlannerBuilder agent(AgentInstance agent, String category, String value) {
        return agent(agent, Map.of(category, value), Map.of());
    }
    
    /**
     * Add an agent just an an attribute
     */
    public DroolsPlannerBuilder agent(AgentInstance agent, String attribute, double score) {
        return agent(agent, Map.of(), Map.of(attribute, score));
    }
    
    /**
     * Convenience method to add agent with domain and minScore
     */
    public DroolsPlannerBuilder agent(AgentInstance agent, Map<String, String> categories, Map<String, Double> attributes) {
        this.droolsAgents.add(new DroolsAgent(agent, categories, attributes));
        return this;    
    }

    /**
     * Optional: set output key
     */
    public DroolsPlannerBuilder outputKey(String outputKey) {
        this.outputKey = outputKey;
        return this;
    }

    /**
     * Build the complete agent system with DroolsPlanner
     */
    public UntypedAgent build() {
        if (rulesSource == null || rulesSource.isBlank()) {
            throw new IllegalStateException("Rules source must be specified");
        }
        if (droolsAgents.isEmpty()) {
            throw new IllegalStateException("At least one agent must be specified");
        }

        // Create the DroolsPlanner
        DroolsPlanner planner = new DroolsPlanner(droolsAgents, rulesSource);

        // Extract agent instances for the planner system
        List<AgentInstance> agentInstances = droolsAgents.stream()
                .map(DroolsAgent::agentInstance)
                .toList();

        // Build using AgenticServices.plannerBuilder()
        var builder = AgenticServices.plannerBuilder()
                .subAgents(agentInstances)
                .planner(() -> planner);

        if (name != null) {
            builder.name(name);
        }
        if (outputKey != null) {
            builder.outputKey(outputKey);
        }

        return builder.build();
    }

}

