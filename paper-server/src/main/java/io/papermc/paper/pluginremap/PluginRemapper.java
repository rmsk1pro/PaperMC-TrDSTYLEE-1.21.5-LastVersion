package io.papermc.paper.pluginremap;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import io.papermc.paper.plugin.provider.type.PluginFileType;
import io.papermc.paper.util.AtomicFiles;
import io.papermc.paper.util.MappingEnvironment;
import io.papermc.paper.util.concurrent.ScalingThreadPool;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import net.minecraft.DefaultUncaughtExceptionHandlerWithName;
import net.minecraft.util.ExceptionCollector;
import net.neoforged.art.api.Renamer;
import net.neoforged.art.api.SignatureStripperConfig;
import net.neoforged.art.api.Transformer;
import net.neoforged.srgutils.IMappingFile;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.slf4j.Logger;

import static io.papermc.paper.plugin.PluginInitializerManager.ANSI_GREEN;
import static io.papermc.paper.plugin.PluginInitializerManager.ANSI_RESET;
import static io.papermc.paper.pluginremap.InsertManifestAttribute.addNamespaceManifestAttribute;

@DefaultQualifier(NonNull.class)
public final class PluginRemapper {
    public static final boolean DEBUG_LOGGING = Boolean.getBoolean("Paper.PluginRemapperDebug");
    private static final String PAPER_REMAPPED = ".paper-remapped";
    private static final String UNKNOWN_ORIGIN = "unknown-origin";
    private static final String LIBRARIES = "libraries";
    private static final String EXTRA_PLUGINS = "extra-plugins";
    private static final String REMAP_CLASSPATH = "remap-classpath";
    private static final String REVERSED_MAPPINGS = "mappings/reversed";
    private static final Logger LOGGER = LogUtils.getClassLogger();
    static final String ICON_SUCCESS = "✅";
    static final String ICON_LOADING = "⏳";
    static final String ICON_INFO = "ℹ️";

    // Cores ANSI
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String COLOR_PRIMARY = ANSI_GREEN;
    private static final String COLOR_SECONDARY = ANSI_WHITE;
    private static boolean usePrimaryColor = true;

    private final ExecutorService threadPool;
    private final ReobfServer reobf;
    private final RemappedPluginIndex remappedPlugins;
    private final RemappedPluginIndex extraPlugins;
    private final UnknownOriginRemappedPluginIndex unknownOrigin;
    private final UnknownOriginRemappedPluginIndex libraries;
    private @Nullable CompletableFuture<IMappingFile> reversedMappings;

    public PluginRemapper(final Path pluginsDir) {
        this.threadPool = createThreadPool();
        final CompletableFuture<IMappingFile> mappings = CompletableFuture.supplyAsync(PluginRemapper::loadReobfMappings, this.threadPool);
        final Path remappedPlugins = pluginsDir.resolve(PAPER_REMAPPED);
        this.reversedMappings = this.reversedMappingsFuture(() -> mappings, remappedPlugins, this.threadPool);
        this.reobf = new ReobfServer(remappedPlugins.resolve(REMAP_CLASSPATH), mappings, this.threadPool);
        this.remappedPlugins = new RemappedPluginIndex(remappedPlugins, false);
        this.extraPlugins = new RemappedPluginIndex(this.remappedPlugins.dir().resolve(EXTRA_PLUGINS), true);
        this.unknownOrigin = new UnknownOriginRemappedPluginIndex(this.remappedPlugins.dir().resolve(UNKNOWN_ORIGIN));
        this.libraries = new UnknownOriginRemappedPluginIndex(this.remappedPlugins.dir().resolve(LIBRARIES));
    }

    public static @Nullable PluginRemapper create(final Path pluginsDir) {
        if (MappingEnvironment.reobf() || !MappingEnvironment.hasMappings()) {
            return null;
        }

        try {
            return new PluginRemapper(pluginsDir);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create PluginRemapper, try deleting the '" + pluginsDir.resolve(PAPER_REMAPPED) + "' directory", e);
        }
    }

    public void shutdown() {
        this.threadPool.shutdown();
        this.save(true);
        boolean didShutdown;
        try {
            didShutdown = this.threadPool.awaitTermination(3L, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            didShutdown = false;
        }
        if (!didShutdown) {
            this.threadPool.shutdownNow();
        }
    }

    public void save(final boolean clean) {
        this.remappedPlugins.write();
        this.extraPlugins.write();
        this.unknownOrigin.write(clean);
        this.libraries.write(clean);
    }

    // Método auxiliar para alternar cores
    private static String getNextColor() {
        final String color = usePrimaryColor ? COLOR_PRIMARY : COLOR_SECONDARY;
        usePrimaryColor = !usePrimaryColor;
        return color;
    }

    // Método para formatar mensagens de processo com partes coloridas
    private static String formatProcessMessage(String icon, String process, String fileName) {
        String color = getNextColor();
        return icon + " " + color + process + ANSI_RESET + " " + COLOR_SECONDARY + fileName + ANSI_RESET;
    }

    // Método para mensagens de conclusão
    private static String formatCompletionMessage(String icon, String process, String fileName, long time) {
        return icon + " " + COLOR_PRIMARY + process + " " + COLOR_SECONDARY + fileName +
            COLOR_PRIMARY + " concluído em " + time + "ms." + ANSI_RESET;
    }

    // Método para mensagens de informação
    private static String formatInfo(String icon, String message) {
        return icon + " " + COLOR_SECONDARY + message + ANSI_RESET;
    }

    // Método para mensagens de sucesso
    private static String formatSuccess(String icon, String message) {
        return icon + " " + COLOR_PRIMARY + message + ANSI_RESET;
    }

    // Called on startup and reload
    public void loadingPlugins() {
        if (this.reversedMappings == null) {
            this.reversedMappings = this.reversedMappingsFuture(
                () -> CompletableFuture.supplyAsync(PluginRemapper::loadReobfMappings, this.threadPool),
                this.remappedPlugins.dir(),
                this.threadPool
            );
        }
    }

    // Called after all plugins enabled during startup/reload
    public void pluginsEnabled() {
        this.reversedMappings = null;
        this.save(false);
    }

    public List<Path> remapLibraries(final List<Path> libraries) {
        final List<CompletableFuture<Path>> tasks = new ArrayList<>();
        for (final Path lib : libraries) {
            if (!lib.getFileName().toString().endsWith(".jar")) {
                if (DEBUG_LOGGING) {
                    LOGGER.info("Library '{}' is not a jar.", lib);
                }
                tasks.add(CompletableFuture.completedFuture(lib));
                continue;
            }
            final @Nullable Path cached = this.libraries.getIfPresent(lib);
            if (cached != null) {
                if (DEBUG_LOGGING) {
                    LOGGER.info("Library '{}' has not changed since last remap.", lib);
                }
                tasks.add(CompletableFuture.completedFuture(cached));
                continue;
            }
            tasks.add(this.remapLibrary(this.libraries, lib));
        }
        return waitForAll(tasks);
    }

    public Path rewritePlugin(final Path plugin) {
        // Already remapped
        if (plugin.getParent().equals(this.remappedPlugins.dir())
            || plugin.getParent().equals(this.extraPlugins.dir())) {
            return plugin;
        }

        final @Nullable Path cached = this.unknownOrigin.getIfPresent(plugin);
        if (cached != null) {
            if (DEBUG_LOGGING) {
                LOGGER.info("Plugin '{}' has not changed since last remap.", plugin);
            }
            return cached;
        }

        return this.remapPlugin(this.unknownOrigin, plugin).join();
    }

    public List<Path> rewriteExtraPlugins(final List<Path> plugins) {
        final @Nullable List<Path> allCached = this.extraPlugins.getAllIfPresent(plugins);
        if (allCached != null) {
            if (DEBUG_LOGGING) {
                LOGGER.info("All extra plugins have a remapped variant cached.");
            }
            return allCached;
        }

        final List<CompletableFuture<Path>> tasks = new ArrayList<>();
        for (final Path file : plugins) {
            final @Nullable Path cached = this.extraPlugins.getIfPresent(file);
            if (cached != null) {
                if (DEBUG_LOGGING) {
                    LOGGER.info("Extra plugin '{}' has not changed since last remap.", file);
                }
                tasks.add(CompletableFuture.completedFuture(cached));
                continue;
            }
            tasks.add(this.remapPlugin(this.extraPlugins, file));
        }
        return waitForAll(tasks);
    }

    public List<Path> rewritePluginDirectory(final List<Path> jars) {
        final @Nullable List<Path> remappedJars = this.remappedPlugins.getAllIfPresent(jars);
        if (remappedJars != null) {
            if (DEBUG_LOGGING) {
                LOGGER.info("All plugins have a remapped variant cached.");
            }
            return remappedJars;
        }

        final List<CompletableFuture<Path>> tasks = new ArrayList<>();
        for (final Path file : jars) {
            final @Nullable Path existingFile = this.remappedPlugins.getIfPresent(file);
            if (existingFile != null) {
                if (DEBUG_LOGGING) {
                    LOGGER.info("Plugin '{}' has not changed since last remap.", file);
                }
                tasks.add(CompletableFuture.completedFuture(existingFile));
                continue;
            }

            tasks.add(this.remapPlugin(this.remappedPlugins, file));
        }
        return waitForAll(tasks);
    }

    private static IMappingFile reverse(final IMappingFile mappings) {
        if (DEBUG_LOGGING) {
            LOGGER.info(formatProcessMessage(ICON_LOADING, "Revertendo", "mapeamentos..."));
        }
        final long start = System.currentTimeMillis();
        final IMappingFile reversed = mappings.reverse();
        if (DEBUG_LOGGING) {
            LOGGER.info(formatCompletionMessage(ICON_SUCCESS, "Reversão de", "mapeamentos", System.currentTimeMillis() - start));
        }
        return reversed;
    }

    private CompletableFuture<IMappingFile> reversedMappingsFuture(
        final Supplier<CompletableFuture<IMappingFile>> mappingsFuture,
        final Path remappedPlugins,
        final Executor executor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String mappingsHash = MappingEnvironment.mappingsHash();
                final String fName = mappingsHash + ".tiny";
                final Path reversedMappings1 = remappedPlugins.resolve(REVERSED_MAPPINGS);
                final Path file = reversedMappings1.resolve(fName);
                if (Files.isDirectory(reversedMappings1)) {
                    if (Files.isRegularFile(file)) {
                        return CompletableFuture.completedFuture(
                            loadMappings("Reversed", Files.newInputStream(file))
                        );
                    } else {
                        for (final Path oldFile : list(reversedMappings1, Files::isRegularFile)) {
                            Files.delete(oldFile);
                        }
                    }
                } else {
                    Files.createDirectories(reversedMappings1);
                }
                return mappingsFuture.get().thenApply(loadedMappings -> {
                    final IMappingFile reversed = reverse(loadedMappings);
                    try {
                        AtomicFiles.atomicWrite(file, writeTo -> {
                            reversed.write(writeTo, IMappingFile.Format.TINY, false);
                        });
                    } catch (final IOException e) {
                        throw new RuntimeException("Failed to write reversed mappings", e);
                    }
                    return reversed;
                });
            } catch (final IOException e) {
                throw new RuntimeException("Failed to load reversed mappings", e);
            }
        }, executor).thenCompose(f -> f);
    }

    private CompletableFuture<Path> remapPlugin(
        final RemappedPluginIndex index,
        final Path inputFile
    ) {
        return this.remap(index, inputFile, false);
    }

    private CompletableFuture<Path> remapLibrary(
        final RemappedPluginIndex index,
        final Path inputFile
    ) {
        return this.remap(index, inputFile, true);
    }

    /**
     * Returns the remapped file if remapping was necessary, otherwise null.
     *
     * @param index     remapped plugin index
     * @param inputFile input file
     * @return remapped file, or inputFile if no remapping was necessary
     */
    private CompletableFuture<Path> remap(
        final RemappedPluginIndex index,
        final Path inputFile,
        final boolean library
    ) {
        final Path destination = index.input(inputFile);

        try (final FileSystem fs = FileSystems.newFileSystem(inputFile, new HashMap<>())) {
            // Deixa arquivos dummy se não for necessário remapear, para podermos verificar se existem sem copiar o arquivo inteiro
            final Path manifestPath = fs.getPath("META-INF/MANIFEST.MF");
            final @Nullable String ns;
            if (Files.exists(manifestPath)) {
                final Manifest manifest;
                try (final InputStream in = new BufferedInputStream(Files.newInputStream(manifestPath))) {
                    manifest = new Manifest(in);
                }
                ns = manifest.getMainAttributes().getValue(InsertManifestAttribute.PAPERWEIGHT_NAMESPACE_MANIFEST_KEY);
            } else {
                ns = null;
            }

            if (ns != null && !InsertManifestAttribute.KNOWN_NAMESPACES.contains(ns)) {
                throw new RuntimeException("Falha ao remapear plugin " + inputFile + " com namespace de mapeamento desconhecido '" + ns + "'");
            }

            final boolean mojangMappedManifest = ns != null && (ns.equals(InsertManifestAttribute.MOJANG_NAMESPACE) || ns.equals(InsertManifestAttribute.MOJANG_PLUS_YARN_NAMESPACE));
            if (library) {
                if (mojangMappedManifest) {
                    if (DEBUG_LOGGING) {
                        LOGGER.info(formatSuccess(ICON_SUCCESS, "Biblioteca '" + inputFile.getFileName().toString() + "' já está mapeada para Mojang."));
                    }
                    index.skip(inputFile);
                    return CompletableFuture.completedFuture(inputFile);
                } else if (ns == null) {
                    if (DEBUG_LOGGING) {
                        LOGGER.info(formatInfo(ICON_INFO, "Biblioteca '" + inputFile.getFileName().toString() + "' não especifica um namespace de mapeamento (não remapeando)."));
                    }
                    index.skip(inputFile);
                    return CompletableFuture.completedFuture(inputFile);
                }
            } else {
                if (mojangMappedManifest) {
                    if (DEBUG_LOGGING) {
                        LOGGER.info(formatSuccess(ICON_SUCCESS, "Plugin '" + inputFile.getFileName().toString() + "' já está mapeado para Mojang."));
                    }
                    index.skip(inputFile);
                    return CompletableFuture.completedFuture(inputFile);
                } else if (ns == null && Files.exists(fs.getPath(PluginFileType.PAPER_PLUGIN_YML))) {
                    if (DEBUG_LOGGING) {
                        LOGGER.info(formatInfo(ICON_INFO, "Plugin '" + inputFile.getFileName().toString() + "' é um plugin Paper sem namespace especificado."));
                    }
                    index.skip(inputFile);
                    return CompletableFuture.completedFuture(inputFile);
                }
            }
        } catch (final IOException ex) {
            return CompletableFuture.failedFuture(new RuntimeException("Falha ao abrir o jar do plugin " + inputFile, ex));
        }

        return this.reobf.remapped().thenApplyAsync(reobfServer -> {
            // Mensagem de início do processo
            LOGGER.info(formatProcessMessage(ICON_LOADING, "Remapeando",
                (library ? "biblioteca '" : "plugin '") + inputFile.getFileName().toString() + "'..."));

            final long start = System.currentTimeMillis();
            try (final DebugLogger logger = DebugLogger.forOutputFile(destination)) {
                try (final Renamer renamer = Renamer.builder()
                    .add(Transformer.renamerFactory(this.mappings(), false))
                    .add(addNamespaceManifestAttribute(InsertManifestAttribute.MOJANG_PLUS_YARN_NAMESPACE))
                    .add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL))
                    .lib(reobfServer.toFile())
                    .threads(1)
                    .logger(logger)
                    .debug(logger.debug())
                    .build()) {
                    renamer.run(inputFile.toFile(), destination.toFile());
                }
            } catch (final Exception ex) {
                throw new RuntimeException("Falha ao remapear o jar do plugin '" + inputFile + "'", ex);
            }

            // Mensagem de conclusão
            LOGGER.info(formatCompletionMessage(ICON_SUCCESS, "Remapeamento de",
                (library ? "biblioteca '" : "plugin '") + inputFile.getFileName().toString() + "'",
                System.currentTimeMillis() - start));
            return destination;
        }, this.threadPool);
    }

    private IMappingFile mappings() {
        final @Nullable CompletableFuture<IMappingFile> mappings = this.reversedMappings;
        if (mappings == null) {
            return this.reversedMappingsFuture(
                () -> CompletableFuture.supplyAsync(PluginRemapper::loadReobfMappings, Runnable::run),
                this.remappedPlugins.dir(),
                Runnable::run
            ).join();
        }
        return mappings.join();
    }

    private static IMappingFile loadReobfMappings() {
        return loadMappings("Reobf", MappingEnvironment.mappingsStream());
    }

    private static IMappingFile loadMappings(final String name, final InputStream stream) {
        try (stream) {
            if (DEBUG_LOGGING) {
                LOGGER.info(formatProcessMessage(ICON_LOADING, "Carregando", "mapeamentos " + name + "..."));
            }
            final long start = System.currentTimeMillis();
            final IMappingFile load = IMappingFile.load(stream);
            if (DEBUG_LOGGING) {
                LOGGER.info(formatCompletionMessage(ICON_SUCCESS, "Carregamento de", "mapeamentos " + name, System.currentTimeMillis() - start));
            }
            return load;
        } catch (final IOException ex) {
            throw new RuntimeException("Falha ao carregar mapeamentos " + name, ex);
        }
    }

    static List<Path> list(final Path dir, final Predicate<Path> filter) {
        try (final Stream<Path> stream = Files.list(dir)) {
            return stream.filter(filter).toList();
        } catch (final IOException ex) {
            throw new RuntimeException("Falha ao listar diretório '" + dir + "'", ex);
        }
    }

    private static List<Path> waitForAll(final List<CompletableFuture<Path>> tasks) {
        final ExceptionCollector<Exception> collector = new ExceptionCollector<>();
        final List<Path> ret = new ArrayList<>();
        for (final CompletableFuture<Path> task : tasks) {
            try {
                ret.add(task.join());
            } catch (final CompletionException ex) {
                collector.add(ex);
            }
        }
        try {
            collector.throwIfPresent();
        } catch (final Exception ex) {
            // Não falhar durante o bootstrap/carregamento de plugins. O(s) plugin(s) em questão será(ão) ignorado(s)
            LOGGER.error("❌ " + COLOR_PRIMARY + "Encontrada exceção ao remapear plugins" + ANSI_RESET, ex);
        }
        return ret;
    }

    private static ThreadPoolExecutor createThreadPool() {
        return new ThreadPoolExecutor(
            0,
            4,
            5L,
            TimeUnit.SECONDS,
            ScalingThreadPool.createUnboundedQueue(),
            new ThreadFactoryBuilder()
                .setNameFormat("Thread de Remapeamento de Plugin do Paper - %1$d")
                .setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandlerWithName(LOGGER))
                .build(),
            ScalingThreadPool.defaultReEnqueuePolicy()
        );
    }
}
