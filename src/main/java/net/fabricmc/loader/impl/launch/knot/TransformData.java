package net.fabricmc.loader.impl.launch.knot;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.khasm.transform.method.KhasmMethodTransformerDispatcher;
import net.khasm.transform.method.action.SmartMethodTransformer;
import net.khasm.util.KhasmUtilitiesKt;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * This class isn't intended to exist at runtime,
 * but it might... whoops
 *
 * Implemented methods have been directly copied from KnotClassDelegate,
 * and modifications are marked with {@code // MODIFIED BELOW} and {@code // MODIFIED ABOVE}
 */
@SuppressWarnings("ALL")
abstract class TransformData {
    /**
     * Modified to loads khasm smart injects
     */
    Class<?> tryLoadClass(String name, boolean allowFromParent) throws ClassNotFoundException {
        if (name.startsWith("java.")) {
            return null;
        }

        if (!allowedPrefixes.isEmpty()) {
            URL url = itf.getResource(LoaderUtil.getClassFileName(name));
            String[] prefixes;

            if (url != null
                    && (prefixes = allowedPrefixes.get(url)) != null) {
                assert prefixes.length > 0;
                boolean found = false;

                for (String prefix : prefixes) {
                    if (name.startsWith(prefix)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new ClassNotFoundException("class "+name+" is currently restricted from being loaded");
                }
            }
        }

        byte[] input = getPostMixinClassByteArray(name, allowFromParent);
        if (input == null) return null;

        KnotClassDelegate.Metadata metadata = getMetadata(name, itf.getResource(LoaderUtil.getClassFileName(name)));

        int pkgDelimiterPos = name.lastIndexOf('.');

        if (pkgDelimiterPos > 0) {
            // TODO: package definition stub
            String pkgString = name.substring(0, pkgDelimiterPos);

            if (itf.getPackage(pkgString) == null) {
                itf.definePackage(pkgString, null, null, null, null, null, null, null);
            }
        }

        // MODIFIED BELOW
        // injecting here because classloaders cause KhasmMethodTransformerDispatcher to be loaded multiple times which causes all the problems
        Class<?> clazz = itf.defineClassFwd(name, input, 0, input.length, metadata.codeSource);
        if (!name.contains("khasm")) {
            // run khasm initialization for the class
            // the check is in case KnotClassDelegate loads any khasm classes
            String slashedName = name.replace('.', '/');
            Map<String, List<SmartMethodTransformer>> map = KhasmMethodTransformerDispatcher.INSTANCE.getAppliedFunctions();
            if (map.containsKey(slashedName)) {
                for (SmartMethodTransformer transformer : map.get(slashedName)) {
                    try {
                        KhasmUtilitiesKt.getLogger().info("Setting up {} in {}", transformer.getInternalName(), name);
                        Field field = clazz.getDeclaredField(transformer.getInternalName());
                        field.setAccessible(true);
                        field.set(null, transformer.getAction());
                    } catch (Throwable t) {
                        throw KhasmUtilitiesKt.rethrow(t);
                    }
                }
            }
        }
        return clazz;
        // MODIFIED ABOVE
    }

    /**
     * Modified to inject khasm transformers
     */
    public byte[] getPreMixinClassByteArray(String name, boolean allowFromParent) {
        // some of the transformers rely on dot notation
        name = name.replace('/', '.');

        // MODIFIED BELOW
        // Adding net.khasm.* to the classes that can't be transformed in hopes of fixing the issue of wrong classloader
        if (!transformInitialized || !canTransformClass(name) || name.startsWith("net.khasm")) {
        // MODIFIED ABOVE
            try {
                return getRawClassByteArray(name, allowFromParent);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load class file for '" + name + "'!", e);
            }
        }

        byte[] input = provider.getEntrypointTransformer().transform(name);

        if (input == null) {
            try {
                input = getRawClassByteArray(name, allowFromParent);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load class file for '" + name + "'!", e);
            }
        }

        if (input != null) {

            // MODIFIED BELOW
            input = FabricTransformer.transform(isDevelopment, envType, name, input);
            // reflection to keep the method in KnotClassLoader (or KnotCompatibilityClassLoader)
            try {
                Method method = Class.forName("net.khasm.init.NewInitializationKt", true, (ClassLoader) itf).getMethod("khasmTransform", byte[].class, String.class);
                return (byte[]) method.invoke(null, input, name);
            } catch (Throwable t) {
                throw KhasmUtilitiesKt.rethrow(t);
            }
            // MODIFIED ABOVE
        }

        return null;
    }

    // satisfy the methods but don't do anything because the method is injected and not called from here
    private Map<String, KnotClassDelegate.Metadata> metadataCache;
    private KnotClassLoaderInterface itf;
    private GameProvider provider;
    private boolean isDevelopment;
    private EnvType envType;
    private IMixinTransformer mixinTransformer;
    private boolean transformInitialized = false;
    private Map<URL, String[]> allowedPrefixes;
    static boolean canTransformClass(String name) { throw new IllegalStateException("This should never be implemented!"); }
    abstract byte[] getRawClassByteArray(String name, boolean allowFromParent) throws IOException;
    abstract KnotClassDelegate.Metadata getMetadata(String name, URL url);
    abstract byte[] getPostMixinClassByteArray(String name, boolean allowFromParent);
}
