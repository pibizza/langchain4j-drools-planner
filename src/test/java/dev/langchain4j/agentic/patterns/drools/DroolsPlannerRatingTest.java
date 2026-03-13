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
 * Example test demonstrating Drools-based rating of requests.
 *
 * This test assumes Ollama is running locally with a model available.
 * Run with: mvn test -Dollama.model=llama3.2
 */
class DroolsPlannerRatingTest {

    private static ChatModel ollamaModel() {
        String modelName = System.getProperty("ollama.model", "llama3.2");
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    public static interface CategoryRouter {

        @UserMessage("""
            Analyze the following user financial request and evaluate it from 1 to 10. 
            1 is a very simple request, 10 is a very complex request, and numbers between 1 and 10 represent various levels of complexity
            Reply with only a number between 1 and 10.
            The user request is: '{{request}}'.
            """)
        @Agent(description = "Evaluate a user financial request", outputKey = "evaluation")
        int classify(@V("request") String request);
    }

    public static interface JuniorFinancialExpert extends AgentInstance {

        @UserMessage("""
            You are a Junior Financial expert.
            Analyze the following user financial request under a financial point of view and provide the best possible answer.
            You are allowed to analyze only relatively simple financial request.
            The user request is {{request}}.
            """)
        @Agent(description = "A junior financial expert", outputKey = "response")
        String medical(@V("request") String request);
    }

    public static interface SeniorFinancialExpert extends AgentInstance {

        @UserMessage("""
            You are a Senior Financial expert.
            Analyze the following user financial request under a financial point of view and provide the best possible answer.
            You are allowed to analyze any financial request, no matter how complex.
            The user request is {{request}}.
            """)
        @Agent(description = "A legal expert", outputKey = "response")
        String legal(@V("request") String request);
    }


    public static interface ExpertRouterAgent {
        String ask(String request);
    }
    
    @Test
    void drools_planner_evaluate_example() {
        // Step 1: Create the router agent that classifies requests
        CategoryRouter routerAgent = AgenticServices.agentBuilder(CategoryRouter.class)
                .chatModel(ollamaModel())
                .outputKey("evaluation")
                .build();

        // Step 2: Create the expert agents
        JuniorFinancialExpert juniorFinancialExpert = AgenticServices.agentBuilder(JuniorFinancialExpert.class)
                .chatModel(ollamaModel())
                .outputKey("response")
                .build();

        SeniorFinancialExpert seniorFinanciaExpert = AgenticServices.agentBuilder(SeniorFinancialExpert.class)
                .chatModel(ollamaModel())
                .outputKey("response")
                .build();

        // Step 3: Create Drools rules for routing based on state
        String droolsRules = """
            package dev.langchain4j.agentic.patterns.drools;

            import dev.langchain4j.agentic.patterns.drools.AgentAttribute;
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

            rule "Route to Financial Agent"
            when
                not StateEntry(key == "response")
                StateEntry(key == "evaluation", $score: value)
                AgentAttribute(domain == "financialmin", score <= $score, $agent : agentName)
                AgentAttribute(domain == "financialmax", score > $score, $agent == agentName)
                $decision : AgentDecision()
            then
                $decision.setSelectedAgent($agent);
            end
            """;

        // Step 4: Build the Drools-based router
        UntypedAgent droolsRouter = new DroolsPlannerBuilder()
                .agent(juniorFinancialExpert, Map.of(), Map.of("financialmin", 0.0, "financialmax", 5.0))
                .agent(seniorFinanciaExpert, Map.of(), Map.of("financialmin", 5.0, "financialmax", 10.0))
                .rules(droolsRules)
                .build();

        // Step 5: Combine router and Drools planner in a sequence
        UntypedAgent expertRouterAgent = AgenticServices.sequenceBuilder()
                .subAgents(routerAgent, droolsRouter)
                .outputKey("response")
                .build();

        // Step 6: Test the system with a medical question
        String response = (String) expertRouterAgent.invoke(
                java.util.Map.of("request", "I want to become a bit more frugal, what should I do?")
        );

        System.out.println("Expert response: " + response);

        // Verify we got a response
        assertThat(response).isNotNull().isNotEmpty();

        // Test with a legal question
        String legalResponse = (String) expertRouterAgent.invoke(
                java.util.Map.of("request", "I want to develop a diversified portfolio, what should I do?")
        );

        System.out.println("Expert response: " + legalResponse);
        assertThat(legalResponse).isNotNull().isNotEmpty();

    }
    

    
}
