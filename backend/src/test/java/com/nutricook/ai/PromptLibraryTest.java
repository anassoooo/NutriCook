package com.nutricook.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** BR-AI-08: prompts are versioned files, never strings in Java. */
class PromptLibraryTest {

  private final PromptLibrary library = new PromptLibrary();

  @Test
  void BR_AI_08_everyPromptFileHasAVersionHeader() throws IOException {
    try (Stream<Path> files = Files.list(Path.of("src/main/resources/prompts"))) {
      List<Path> prompts = files.filter(p -> p.toString().endsWith(".st")).toList();
      assertThat(prompts).isNotEmpty();
      for (Path p : prompts) {
        String name = p.getFileName().toString().replace(".st", "");
        assertThat(library.get(name).version()).as(name).isPositive();
      }
    }
  }

  @Test
  void BR_AI_08_noPromptTextInJavaSources() throws IOException {
    try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
      List<Path> offenders =
          files
              .filter(p -> p.toString().endsWith(".java"))
              .filter(
                  p -> {
                    try {
                      return Files.readString(p).contains("You are");
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .toList();
      assertThat(offenders).as("Java files containing prompt text").isEmpty();
    }
  }

  @Test
  void commentLinesAreNeverSentToTheModel() {
    PromptLibrary.Template chat = library.get("chat");
    assertThat(chat.text()).doesNotContain("# version").startsWith("You are the NutriCook");
  }

  @Test
  void BR_AI_06_theUserMessageIsWrappedInDelimiters() {
    String rendered =
        library.render(
            "chat",
            Map.of(
                "date",
                "2026-10-11",
                "plan",
                "-",
                "goal",
                "MAINTAIN_WEIGHT",
                "targets",
                "-",
                "restrictions",
                "none",
                "conditions",
                "none",
                "progress",
                "nothing yet",
                "candidates",
                "1: Lentils, protein",
                "message",
                "Can I have rice?"));
    assertThat(rendered).contains("<user_message>\nCan I have rice?\n</user_message>");
  }

  @Test
  void aPromptWithAMissingVariableIsNeverSent() {
    assertThatThrownBy(() -> library.render("chat", Map.of("message", "hi")))
        .isInstanceOf(IllegalStateException.class);
  }
}
