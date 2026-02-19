package dev.langchain4j.agentic.patterns.drools;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Test agent interfaces for demonstrating Drools planner functionality.
 */
public class TestAgents {

    public enum RequestCategory {
        LEGAL, MEDICAL, TECHNICAL, UNKNOWN
    }

    public interface CategoryRouter {

        @UserMessage("""
            Analyze the following user request and categorize it as 'legal', 'medical' or 'technical'.
            In case the request doesn't belong to any of those categories categorize it as 'unknown'.
            Reply with only one of those words and nothing else.
            The user request is: '{{request}}'.
            """)
        @Agent(description = "Categorize a user request", outputKey = "category")
        RequestCategory classify(@V("request") String request);
    }

    public interface MedicalExpert extends AgentInstance {

        @UserMessage("""
            You are a medical expert.
            Analyze the following user request under a medical point of view and provide the best possible answer.
            The user request is {{request}}.
            """)
        @Agent(description = "A medical expert", outputKey = "response")
        String medical(@V("request") String request);
    }

    public interface LegalExpert extends AgentInstance {

        @UserMessage("""
            You are a legal expert.
            Analyze the following user request under a legal point of view and provide the best possible answer.
            The user request is {{request}}.
            """)
        @Agent(description = "A legal expert", outputKey = "response")
        String legal(@V("request") String request);
    }

    public interface TechnicalExpert extends AgentInstance {

        @UserMessage("""
            You are a technical expert.
            Analyze the following user request under a technical point of view and provide the best possible answer.
            The user request is {{request}}.
            """)
        @Agent(description = "A technical expert", outputKey = "response")
        String technical(@V("request") String request);
    }

    public interface ExpertRouterAgent {
        String ask(String request);
    }
}
