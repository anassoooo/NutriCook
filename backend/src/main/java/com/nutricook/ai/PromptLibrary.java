package com.nutricook.ai;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Versioned prompt templates from {@code resources/prompts/*.st} (BR-AI-08). Every file starts with
 * {@code # version: N}; lines starting with {@code #} are comments and never sent to the model.
 * Rendering fails if a template variable is missing, so a prompt can never go out half-filled.
 */
@Component
public class PromptLibrary {

  private static final Pattern VERSION = Pattern.compile("^# version: (\\d+)\\s*$");

  public record Template(String name, int version, String text) {}

  private final Map<String, Template> cache = new ConcurrentHashMap<>();

  public Template get(String name) {
    return cache.computeIfAbsent(name, PromptLibrary::load);
  }

  public String render(String name, Map<String, Object> variables) {
    // StringTemplate writes the OS line separator; normalize so prompts are identical everywhere
    return new PromptTemplate(get(name).text()).render(variables).replace(CRLF, LF);
  }

  private static final String CRLF = "\r\n";
  private static final String LF = "\n";

  static Template load(String name) {
    String raw;
    try {
      raw =
          new ClassPathResource("prompts/" + name + ".st")
              .getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Missing prompt " + name, e);
    }
    String firstLine = raw.lines().findFirst().orElse("");
    Matcher m = VERSION.matcher(firstLine);
    if (!m.matches()) {
      throw new IllegalStateException(
          "Prompt " + name + " must start with '# version: N' (BR-AI-08)");
    }
    String text =
        raw.lines().filter(l -> !l.startsWith("#")).collect(Collectors.joining("\n")).strip();
    return new Template(name, Integer.parseInt(m.group(1)), text);
  }
}
