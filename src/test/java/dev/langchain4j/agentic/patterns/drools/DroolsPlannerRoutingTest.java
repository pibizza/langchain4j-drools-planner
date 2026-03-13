package dev.langchain4j.agentic.patterns.drools;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

/**
 * Example test demonstrating Drools-based agent routing.
 *
 * This test assumes Ollama is running locally with a model available.
 * Run with: mvn test -Dollama.model=llama3.2
 */
class DroolsPlannerRoutingTest {

    private static ChatModel ollamaModel() {
        String modelName = System.getProperty("ollama.model", "llama3.2");
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    public enum RequestCategory {
        LEGAL, MEDICAL, TECHNICAL, UNKNOWN
    }

    public static interface CategoryRouter {

        @UserMessage("""
            Analyze the following user request and categorize it as 'legal', 'medical' or 'technical'.
            In case the request doesn't belong to any of those categories categorize it as 'unknown'.
            Reply with only one of those words and nothing else.
            The user request is: '{{request}}'.
            """)
        @Agent(description = "Categorize a user request", outputKey = "category")
        RequestCategory classify(@V("request") String request);
    }

    public static interface MedicalExpert extends AgentInstance {

        @UserMessage("""
            You are a medical expert.
            Analyze the following user request under a medical point of view and provide the best possible answer.
            The user request is {{request}}.
            """)
        @Agent(description = "A medical expert", outputKey = "response")
        String medical(@V("request") String request);
    }

    public static interface LegalExpert extends AgentInstance {

        @UserMessage("""
            You are a legal expert.
            Analyze the following user request under a legal point of view and provide the best possible answer.
            The user request is {{request}}.
            """)
        @Agent(description = "A legal expert", outputKey = "response")
        String legal(@V("request") String request);
    }

    public static interface TechnicalExpert extends AgentInstance {

        @UserMessage("""
            You are a technical expert.
            Analyze the following user request under a technical point of view and provide the best possible answer.
            The user request is {{request}}.
            """)
        @Agent(description = "A technical expert", outputKey = "response")
        String technical(@V("request") String request);
    }

    public static interface ExpertRouterAgent {
        String ask(String request);
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
            salience 100
            when
                StateEntry(key == "response", value != null, $response: value)
                $decision : AgentDecision()
            then
                $decision.setResult($response);
                $decision.setDone(true);
            end

            rule "Route to Medical Expert"
            when
                not StateEntry(key == "response")
                StateEntry(key == "category", value == "MEDICAL")
                AgentCategory(categoryName == "domain", categoryValue == "medical", $agent : agentName)
                $decision : AgentDecision()
            then
                $decision.setSelectedAgent($agent);
            end

            rule "Route to Legal Expert"
            when
                not StateEntry(key == "response")
                StateEntry(key == "category", value == "LEGAL")
                AgentCategory(categoryName == "domain", categoryValue == "legal", $agent : agentName)
                $decision : AgentDecision()
            then
                $decision.setSelectedAgent($agent);
            end

            rule "Route to Technical Expert"
            when
                not StateEntry(key == "response")
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
        String response = (String) expertRouterAgent.invoke(Map.of("request", "I broke my leg, what should I do?"));

        System.out.println("Response: " + response);

        // Verify we got a response
        assertThat(response).isNotNull().isNotEmpty();

        // Test with a legal question
        String legalResponse = (String) expertRouterAgent.invoke(Map.of("request", "Can I sue my landlord for not fixing the heating?"));

        System.out.println("Legal response: " + legalResponse);
        assertThat(legalResponse).isNotNull().isNotEmpty();

        // Test with a technical question
        String techResponse = (String) expertRouterAgent.invoke(Map.of("request", "My computer won't turn on, what could be wrong?"));

        System.out.println("Technical response: " + techResponse);
        assertThat(techResponse).isNotNull().isNotEmpty();
    }
    

    
}
