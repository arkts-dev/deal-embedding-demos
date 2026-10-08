import deal.ui.*;
import java.nio.file.*;
import java.util.*;

/** Compile framework regression fixtures through Android's exact session-generation path. */
public class CheckUiLifecycle {
    public static void main(String[] args) throws Exception {
        Path fixture = Path.of("dependencies/deal-ui/src/test/fixtures/js-session");
        Path output = Path.of("build/ui-lifecycle");
        Path sources = output.resolve("src");
        Files.createDirectories(sources);
        Path app = sources.resolve("app.deal"), view = sources.resolve("app.dealui");
        String source = Files.readString(fixture.resolve("app.deal"));
        Path packFile = Path.of("dependencies/deal-embedding/android/assets/embedding/platform.dealui-pack");
        var pack = UiParser.parsePack(packFile, Files.readString(packFile));
        var generated = UiSourceGenerator.generate(view, Files.readString(fixture.resolve("app.dealui")), app, source,
            Map.of("./platform.dealui-pack", pack), Path.of("dependencies/deal-ui"));
        Files.writeString(app, UiSourceGenerator.compilerSource(source) + generated.augmentation());
        Path entry = sources.resolve("experience_entry.deal");
        Files.writeString(entry, generated.source());
        Files.writeString(output.resolve("work.d.deal"), "export async function run(value: int): int;\n");
        Files.writeString(output.resolve("deal.json"), "{\"languageVersion\":\"1.2\",\"backend\":\"js\",\"moduleRoots\":[\"src\"],\"externals\":{\"host/work\":{\"declaration\":\"work.d.deal\"}}}");
        System.setProperty("deal.home", Path.of("dependencies/deal").toAbsolutePath().toString());
        if (deal.Main.run(new String[]{"compile", entry.toString(), "--backend", "js", "--output", output.resolve("js").toString()}) != 0)
            throw new IllegalStateException("UI lifecycle fixture rejected");
    }
}
