package dev.langchain4j.agentic.patterns.drools;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.drools.TestAgents.CategoryRouter;
import dev.langchain4j.agentic.patterns.drools.TestAgents.LegalExpert;
import dev.langchain4j.agentic.patterns.drools.TestAgents.MedicalExpert;
import dev.langchain4j.agentic.patterns.drools.TestAgents.TechnicalExpert;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example test demonstrating Drools-based agent routing.
 *
 * This test assumes Ollama is running locally with a model available.
 * Run with: mvn test -Dollama.model=llama3.2
 */
class DroolsPlannerExampleTest {

    private static ChatModel ollamaModel() {
        String modelName = System.getProperty("ollama.model", "llama3.2");
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    @Test
    void drools_planner_routing_example() {
        // Step 1: Create the router agent that classifies requests
        CategoryRouter routerAgent = AgenticServices.agentBuilder(CategoryRouter.class)
                .chatModel(ollamaModel())
                .outputKey("category")
                .build();

        // Step 2: Create the expert agents
        MedicalExpert medicalExpert = AgenticServices.agentBuilder(MedicalExpert.class)
                .chatModel(ollamaModel())
                .outputKey("response")
                .build();

        LegalExpert legalExpert = AgenticServices.agentBuilder(LegalExpert.class)
                .chatModel(ollamaModel())
                .outputKey("response")
                .build();

        TechnicalExpert technicalExpert = AgenticServices.agentBuilder(TechnicalExpert.class)
                .chatModel(ollamaModel())
                .outputKey("response")
                .build();

        // Step 3: Create Drools rules for routing based on state
        String droolsRules = """
            package dev.langchain4j.agentic.patterns.drools;

            import dev.langchain4j.agentic.patterns.drools.AgentCategory;
            import dev.langchain4j.agentic.patterns.drools.StateEntry;
            import dev.langchain4j.agentic.patterns.drools.AgentDecision;


            rule "Final decision"
            when
                StateEntry(key == "response", $response: value)
                $decision : AgentDecision()
            then
                $decision.setResult($response);
                $decision.setDone(true);
            end
            
            rule "Route to Medical Expert"
            when
                StateEntry(key == "category", value == "MEDICAL")
                AgentCategory(categoryName == "domain", categoryValue == "medical", $agent : agentName)
                $decision : AgentDecision()
            then
                $decision.setSelectedAgent($agent);
            end

            rule "Route to Legal Expert"
            when
                StateEntry(key == "category", value == "LEGAL")
                AgentCategory(categoryName == "domain", categoryValue == "legal", $agent : agentName)
                $decision : AgentDecision()
            then
                $decision.setSelectedAgent($agent);
            end

            rule "Route to Technical Expert"
            when
                StateEntry(key == "category", value == "TECHNICAL")
                AgentCategory(categoryName == "domain", categoryValue == "technical", $agent : agentName)
                $decision : AgentDecision()
            then
                $decision.setSelectedAgent($agent);
            end
            """;

        // Step 4: Build the Drools-based router
        UntypedAgent droolsRouter = new DroolsPlannerBuilder()
                .agent(medicalExpert, "domain", "medical")
                .agent(legalExpert, "domain", "legal")
                .agent(technicalExpert, "domain", "technical")
                .rules(droolsRules)
                .build();

        // Step 5: Combine router and Drools planner in a sequence
        UntypedAgent expertRouterAgent = AgenticServices.sequenceBuilder()
                .subAgents(routerAgent, droolsRouter)
                .outputKey("response")
                .build();

        // Step 6: Test the system with a medical question
        String response = (String) expertRouterAgent.invoke(
                java.util.Map.of("request", "I broke my leg, what should I do?")
        );

        System.out.println("Response: " + response);

        // Verify we got a response
        assertThat(response).isNotNull().isNotEmpty();

        // Test with a legal question
        String legalResponse = (String) expertRouterAgent.invoke(
                java.util.Map.of("request", "Can I sue my landlord for not fixing the heating?")
        );

        System.out.println("Legal response: " + legalResponse);
        assertThat(legalResponse).isNotNull().isNotEmpty();

        // Test with a technical question
        String techResponse = (String) expertRouterAgent.invoke(
                java.util.Map.of("request", "My computer won't turn on, what could be wrong?")
        );

        System.out.println("Technical response: " + techResponse);
        assertThat(techResponse).isNotNull().isNotEmpty();
    }
}
