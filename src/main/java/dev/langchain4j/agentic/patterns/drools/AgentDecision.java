package dev.langchain4j.agentic.patterns.drools;

public class AgentDecision {

	private String selectedAgent;
	private boolean done;
	private Object result;
	
	public String getSelectedAgent() {
		return selectedAgent;
	}
	
	public void setSelectedAgent(String selectedAgent) {
		this.selectedAgent = selectedAgent;
	}
	
	public boolean isDone() {
		return done;
	}
	
	public void setDone(boolean done) {
		this.done = done;
	}
	public Object getResult() {
		return result;
	}
	public void setResult(Object result) {
		this.result = result;
	}

    
		
		

    // getters and setters
}
