package dev.langchain4j.agentic.patterns.drools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.InitPlanningContext;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlanningContext;

public class DroolsPlanner implements Planner {

    private final List<DroolsAgent> droolsAgents;                                                                                               
    private final String rulesSource;

    private KieSession kieSession;
    private Map<String, AgentInstance> agentsByName;

    

	public DroolsPlanner(List<DroolsAgent> droolsAgents, String rulesSource) {
		this.rulesSource = rulesSource;
		this.droolsAgents = droolsAgents;
	}

	@Override
    public void init(InitPlanningContext initPlanningContext) {
        // Create KieContainer from rules
        KieContainer kieContainer = createKieContainer(rulesSource);

        // Create persistent session
        this.kieSession = kieContainer.newKieSession();

        // Build agent lookup map
        this.agentsByName = new HashMap<>();

        // Transform DroolsAgent into Drools-friendly fact objects
        for (DroolsAgent droolsAgent : droolsAgents) {
            String agentName = droolsAgent.agentInstance().name();

            // Insert custom attributes as separate facts for better matching
            for (Map.Entry<String, String> attr : droolsAgent.categories().entrySet()) {
                kieSession.insert(new AgentCategory(
                    agentName,
                    attr.getKey(),
                    attr.getValue()
                ));
            }
            
            for (Map.Entry<String, Double> attr : droolsAgent.attributes().entrySet()) {
                kieSession.insert(new AgentAttribute(
                    agentName,
                    attr.getKey(),
                    attr.getValue()
                ));
            }

            agentsByName.put(agentName, droolsAgent.agentInstance());
        }

    }

    private KieContainer createKieContainer(String rulesSource) {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        try {
            String rules = loadRulesFromSource(rulesSource);
            kieFileSystem.write("src/main/resources/rules.drl", rules);

            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                throw new IllegalStateException("Errors building Drools rules: " +
                    kieBuilder.getResults().getMessages(Message.Level.ERROR));
            }

            return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());

        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Drools rules", e);
        }
    }

    private String loadRulesFromSource(String source) throws IOException {
        if (source.startsWith("classpath:")) {
            String path = source.substring("classpath:".length());
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is == null) {
                    throw new IllegalArgumentException("Rules file not found: " + path);
                }
                return new String(is.readAllBytes());
            }
        }
        // Assume it's direct DRL content
        return source;
    }

    @Override
    public Action nextAction(PlanningContext planningContext) {
    	return executeRules(planningContext);
    }

    @Override
    public AgenticSystemTopology topology() {
        return AgenticSystemTopology.SEQUENCE;
    }

    private Action executeRules(PlanningContext planningContext) {
        // Sync state from AgenticScope into the session
        Map<String, Object> state = planningContext.agenticScope().state();

        
        List<FactHandle> handles = new ArrayList<>();
        // Insert state entries as facts
        for (Map.Entry<String, Object> entry : state.entrySet()) {
            handles.add(kieSession.insert(new StateEntry(entry.getKey(), entry.getValue())));
        }

        // Insert a decision holder for rules to populate
        AgentDecision decision = new AgentDecision();
        handles.add(kieSession.insert(decision));

        // Fire all rules
        kieSession.fireAllRules();

        
        // Retract transient facts (keep agent metadata, remove state)
        handles.stream().forEach(x -> kieSession.delete(x));


        
        // Check decision
        if (decision.isDone()) {
            return done(decision.getResult());
        }

        String selectedAgent = decision.getSelectedAgent();
        if (selectedAgent == null) {
            throw new IllegalStateException("No agent selected by Drools rules");
        }

        AgentInstance agent = agentsByName.get(selectedAgent);
        if (agent == null) {
            throw new IllegalStateException("Unknown agent: " + selectedAgent);
        }

        return call(agent);
    }



}
