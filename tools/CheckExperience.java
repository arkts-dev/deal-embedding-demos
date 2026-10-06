import deal.ui.*;
import java.nio.file.*;
import java.util.*;

/** Exercises the exact in-process source generation entry used by Android. */
public class CheckExperience {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("android/apps/calendar/experiences/departure");
        Path output = Path.of("build/checked-experience");
        Files.createDirectories(output);
        Path sources = output.resolve("src");
        Files.createDirectories(sources);
        Path app = sources.resolve("departure.deal");
        Path view = sources.resolve("departure.dealui");
        String source = Files.readString(root.resolve("departure.deal"));
        String views = Files.readString(root.resolve("departure.dealui"));
        Path packFile = Path.of("dependencies/deal-embedding/android/assets/embedding/platform.dealui-pack");
        String pack = Files.readString(packFile);
        var result = UiSourceGenerator.generate(view, views, app, source,
            Map.of("./platform.dealui-pack", UiParser.parsePack(packFile, pack)), Path.of("dependencies/deal-ui"));
        Files.writeString(app, UiSourceGenerator.compilerSource(source) + result.augmentation());
        Files.writeString(sources.resolve("experience.deal"), result.source());
        // Contracts/declarations are receipts from actual on-device provider discovery.
        Path declarations = Path.of("build/discovered-declarations");
        var externals = new ArrayList<String>();
        try (var files = Files.list(declarations)) {
            for (Path file : files.sorted().toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".d.deal")) continue;
                Files.copy(file, output.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                String module = name.substring(0, name.length() - ".d.deal".length());
                externals.add("\"host/" + module + "\":{\"declaration\":\"" + name + "\"}");
            }
        }
        if (externals.isEmpty()) throw new IllegalStateException("Capture discovered provider declarations first");
        Files.writeString(output.resolve("deal.json"), "{\"languageVersion\":\"1.2\",\"backend\":\"js\",\"moduleRoots\":[\"src\"],\"externals\":{" + String.join(",", externals) + "}}");
        System.setProperty("deal.home", Path.of("dependencies/deal").toAbsolutePath().toString());
        int status = deal.Main.run(new String[]{"compile", sources.resolve("experience.deal").toString(), "--backend", "js", "--output", output.resolve("js").toString()});
        if (status != 0) throw new IllegalStateException("Compilation rejected");
    }
}
