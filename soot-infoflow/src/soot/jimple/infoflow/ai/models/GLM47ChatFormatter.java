package soot.jimple.infoflow.ai.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.agentscope.core.formatter.openai.GLMFormatter;
import io.agentscope.core.formatter.openai.dto.OpenAIResponse;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatResponse;

/**
 * Formatter implementation for supporting tool calling for GLM-4.7 and similar
 * models
 * 
 * @author Steven Arzt
 */
public class GLM47ChatFormatter extends GLMFormatter {

	protected static Pattern TOOL_PATTERN = Pattern.compile("<tool_call>([^<]+)</tool_call>", Pattern.DOTALL);

	@Override
	public ChatResponse parseResponse(OpenAIResponse response, Instant startTime) {
		ChatResponse base = super.parseResponse(response, startTime);

		List<ContentBlock> newBlocks = new ArrayList<>();
		for (ContentBlock block : base.getContent()) {
			if (block instanceof TextBlock text) {
				String t = text.getText();

				ToolUseBlock tool = tryParseToolCall(t);
				if (tool != null) {
					newBlocks.add(tool);
				} else {
					newBlocks.add(block);
				}
			} else {
				newBlocks.add(block);
			}
		}

		return ChatResponse.builder().content(newBlocks).build();
	}

	protected ToolUseBlock tryParseToolCall(String text) {
		Matcher m = TOOL_PATTERN.matcher(text);
		if (!m.find())
			return null;

		String toolName = m.group(1).trim();
		return ToolUseBlock.builder().id(UUID.randomUUID().toString()).name(toolName).build();
	}

}
