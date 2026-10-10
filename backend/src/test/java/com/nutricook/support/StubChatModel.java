package com.nutricook.support;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Deterministic stand-in for the Groq chat model.
 *
 * <p>Script responses in order with {@link #reply}, {@link #fail} and {@link #slowReply}; once the
 * script is exhausted every call returns {@link #DEFAULT_REPLY}. Every prompt received is recorded
 * so tests can assert what was sent to the model (e.g. the candidate food list, BR-SAF-07).
 */
public class StubChatModel implements ChatModel {

  public static final String DEFAULT_REPLY = "Stub AI reply.";

  private final Deque<Supplier<String>> script = new ArrayDeque<>();
  private final List<Prompt> prompts = new ArrayList<>();

  /** Next call returns {@code text}. */
  public synchronized StubChatModel reply(String text) {
    script.add(() -> text);
    return this;
  }

  /** Next call throws {@code error} (e.g. a timeout or HTTP 429 from the provider). */
  public synchronized StubChatModel fail(RuntimeException error) {
    script.add(
        () -> {
          throw error;
        });
    return this;
  }

  /** Next call sleeps for {@code latency} and then returns {@code text}. */
  public synchronized StubChatModel slowReply(Duration latency, String text) {
    script.add(
        () -> {
          try {
            Thread.sleep(latency.toMillis());
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return text;
        });
    return this;
  }

  /** Clears the script and the recorded prompts. Call from {@code @BeforeEach}. */
  public synchronized void reset() {
    script.clear();
    prompts.clear();
  }

  public synchronized List<Prompt> prompts() {
    return List.copyOf(prompts);
  }

  @Override
  public ChatResponse call(Prompt prompt) {
    Supplier<String> next;
    synchronized (this) {
      prompts.add(prompt);
      next = script.isEmpty() ? () -> DEFAULT_REPLY : script.poll();
    }
    return new ChatResponse(List.of(new Generation(new AssistantMessage(next.get()))));
  }
}
