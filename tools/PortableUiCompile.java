import deal.ui.*;
import java.nio.file.*;
import java.util.*;

/** Integration fixture using the same in-process Deal UI lowering as Android. */
public final class PortableUiCompile {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]), framework=Path.of(args[1]), pack=Path.of(args[2]);
        Path app=root.resolve("src/experience.deal"), view=root.resolve("src/experience.dealui");
        var result=UiSourceGenerator.generate(view,Files.readString(view),app,Files.readString(app),
            Map.of("./platform.dealui-pack",UiParser.parsePack(pack,Files.readString(pack))),framework);
        Files.writeString(app,UiSourceGenerator.compilerSource(Files.readString(app))+result.augmentation());
        Files.writeString(root.resolve("src/experience_entry.deal"),result.source());
    }
}
