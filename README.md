# LangChain4j Drools Planner

Drools-based planner for the LangChain4j agentic framework.

## Overview

This library integrates the Drools rules engine with LangChain4j's agentic framework, enabling rule-based agent orchestration. Agents can be annotated with metadata (categories and numerical attributes), and Drools rules determine which agent to execute based on the current agentic state.

## Features

- Attach metadata to agents (categories and numerical attributes)
- Define agent selection logic using Drools DRL rules
- Access current AgenticScope state within rules
- Persistent Drools session across invocations

## Usage

```java
// Create agents with metadata
UntypedAgent planner = new DroolsPlannerBuilder()
    .agent(agentA, "domain", "medical")              // category
    .agent(agentB, "confidence", 0.9)                // numerical attribute
    .agent(agentC,
           Map.of("specialty", "cardiology"),        // categories
           Map.of("experience", 10.0))               // attributes
    .rules("classpath:/agent-rules.drl")             // or inline DRL string
    .build();
```

## Drools Rules

Rules can access:
- **StateEntry**: Current state from AgenticScope
- **AgentCategory**: Categorical metadata about agents
- **AgentAttribute**: Numerical metadata about agents
- **AgentDecision**: Object to set the selected agent

Example rule:
```drools
rule "Example"
when
    StateEntry(key == "userType", value == "premium")
    AgentAttribute(attributeName == "tier", attributeValue > 5, $agent : agentName)
    $decision : AgentDecision()
then
    $decision.setSelectedAgent($agent);
end
```

## Installation

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-drools-planner</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Requirements

- Java 17+
- LangChain4j agentic framework 1.11.0-beta19-SNAPSHOT+
- Drools 10.1.0

## License

Apache License 2.0
