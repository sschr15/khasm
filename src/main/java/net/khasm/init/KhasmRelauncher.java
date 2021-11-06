package net.khasm.init;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import net.khasm.instrumentation.KhasmInstrumentationApi;
import net.khasm.util.KhasmUtilitiesKt;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class KhasmRelauncher implements LanguageAdapter {
    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) {
        throw new RuntimeException();
    }

    static {
        Field field = KhasmUtilitiesKt.rethrowIfException(() -> Class.forName(
                "net.khasm.instrumentation.InstrumentationAgent",
                false,
                FabricLoader.class.getClassLoader()
        ).getDeclaredField("instrumentation"));

        field.setAccessible(true);

        Instrumentation instrumentation = KhasmUtilitiesKt.rethrowIfException(() -> (Instrumentation) field.get(null));

        if (instrumentation == null) {
            KhasmUtilitiesKt.getLogger().info("Khasm instrumentation was not already loaded, trying again");

            // we need to add our agent ourselves
            Path agentJar = Path.of(".", "khasm-agent.jar");
            KhasmUtilitiesKt.rethrowIfException(() -> Files.deleteIfExists(agentJar));
            Path path = FabricLoader.getInstance().getModContainer("khasm").orElseThrow().getPath("agent_init.jar");
            KhasmUtilitiesKt.rethrowIfException(() -> Files.copy(path, agentJar));

            String pid = String.valueOf(ProcessHandle.current().pid());
            // apparently we can't attach to our own process, so we need to make a new process and make that attach to it instead
            String[] args = {
                    System.getProperty("java.home") + "/bin/java",
                    "-cp",
                    agentJar.toString(), // we want to load our agent jar by itself
                    "net.khasm.instrumentation.InstrumentationAgent",
                    pid,
                    agentJar.toString()
            };
            Process process = KhasmUtilitiesKt.rethrowIfException(() -> new ProcessBuilder(args).inheritIO().start());

            KhasmUtilitiesKt.rethrowIfException(process::waitFor);

            instrumentation = KhasmUtilitiesKt.rethrowIfException(() -> (Instrumentation) field.get(null));
        }

        if (instrumentation != null) {
            KhasmInstrumentationApi.setInstrumentation(instrumentation);
            NewInitializationKt.onInstrumentationLoaded();
        } else {
            KhasmUtilitiesKt.getLogger().warn("""
                    
                    
                    KHASM HAS NOT SUCCESSFULLY LOADED INSTRUMENTATION
                    
                    If you see this more than once, please immediately kill
                    all java processes to be safe and not destroy your computer
                    by consuming all your ram in child processes
                    then submit an issue at https://github.com/P03W/khasm/issues
                    
                    """.replace("\n", "\n\t\t"));

            Path agentJar = Path.of(".", "khasm-agent.jar");
            KhasmUtilitiesKt.rethrowIfException(() -> Files.deleteIfExists(agentJar));
            Path path = FabricLoader.getInstance().getModContainer("khasm").orElseThrow().getPath("agent_init.jar");
            KhasmUtilitiesKt.rethrowIfException(() -> Files.copy(path, agentJar));

            String vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments()
                    .stream().filter(s -> !s.contains("-agentlib") && !s.contains("-javaagent"))
                    .collect(Collectors.joining(" "));
            String command = System.getProperty("sun.java.command");
            String java = System.getProperty("java.home") + "/bin/java";

            if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                java = java.replace('/', '\\') + ".exe";
            }

            String cp = System.getProperty("java.class.path");

            List<String> entireCommand = new ArrayList<>();
            entireCommand.add(java);
            entireCommand.addAll(Arrays.asList(vmArgs.split(" ")));
            entireCommand.add("-javaagent:khasm-agent.jar");
//            entireCommand.add("-noverify");
            entireCommand.add("-cp");
            entireCommand.add(cp);
            entireCommand.add(command);

            Process process = KhasmUtilitiesKt.rethrowIfException(() ->
                    new ProcessBuilder(entireCommand)
                            .inheritIO()
                            .start()
            );
            try {
                System.exit(process.waitFor());
            } catch (InterruptedException e) {
                process.destroy();
                System.exit(1);
            }
        }
    }
}
