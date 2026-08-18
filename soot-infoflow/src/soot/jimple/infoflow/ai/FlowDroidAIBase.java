package soot.jimple.infoflow.ai;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import soot.jimple.infoflow.InfoflowConfiguration.AIConfiguration;

/**
 * AI integration class for FlowDroid
 * 
 * @author Steven Arzt
 */
public abstract class FlowDroidAIBase {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ReActAgent agent;
	protected Memory memory = new InMemoryMemory();
	protected final AIConfiguration configuration;

	/**
	 * Simple memory class that has no memory and instantaneously forgets all
	 * messages
	 * 
	 * @author Steven Arzt
	 */
	protected static class ForgetEverythingMemory implements Memory {

		public static final ForgetEverythingMemory INSTANCE = new ForgetEverythingMemory();

		private ForgetEverythingMemory() {
		}

		@Override
		public void addMessage(Msg message) {
			System.out.println("FORGETTING: " + message.toString());
		}

		@Override
		public List<Msg> getMessages() {
			return Collections.emptyList();
		}

		@Override
		public void deleteMessage(int index) {
		}

		@Override
		public void clear() {
		}

	}

	/**
	 * Creates a new instance of the {@link FlowDroidAIBase} class and establishes a
	 * connection to an LLM
	 * 
	 * @param config       The configuration of the AI integration
	 * @param systemPrompt The system prompt with which to provide the LLM
	 */
	protected FlowDroidAIBase(AIConfiguration config, String systemPrompt) {
		this.configuration = config;

		// Configure the code analysis tools
		Toolkit toolkit = new Toolkit();

		// Link to the model that we want to use
		Model model = OpenAIChatModel.builder().baseUrl(config.getLlmEndpoint()).modelName(config.getModelName())
				.apiKey(config.getApiKey()).stream(true).build();

		// Create the agent
		this.agent = ReActAgent.builder().name("FlowDroid AI Integration").sysPrompt(systemPrompt).model(model)
				.memory(memory).longTermMemoryMode(LongTermMemoryMode.AGENT_CONTROL).toolkit(toolkit).build();
	}

	protected synchronized Msg callWithAgent(Msg request) {
		long nanosBefore = System.nanoTime();
		try {
			memory.clear();
			return agent.call(request).block();
		} finally {
			logger.info("LLM query took {}s", (System.nanoTime() - nanosBefore) / 1e9);
		}
	}

}
