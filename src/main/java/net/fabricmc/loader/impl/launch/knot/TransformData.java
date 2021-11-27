package net.fabricmc.loader.impl.launch.knot;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.khasm.util.KhasmUtilitiesKt;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
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
        if (!name.contains("khasm") && Boolean.getBoolean("khasm.debug.setupAtClassLoad")) {
            try {
                // KnotClassDelegate has an `itf` field which is a KnotClassLoaderInterface, but it's only implemented by Knot's classloaders
                // so we can safely cast it to ClassLoader and use it to load the class :pineapple: :tiny_potato:
                Class<?> newInitializationClass = Class.forName("net.khasm.init.NewInitializationKt", true, (ClassLoader) itf);
                Method setupSmartInjects = newInitializationClass.getDeclaredMethod("setupSmartInjects", Class.class, String.class);
                setupSmartInjects.invoke(null, clazz, name);
            } catch (Throwable t) {
                throw KhasmUtilitiesKt.rethrow(t);
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
        // protecting khasm's classes from being transformed in case of circular classloading or other issues
        if (name.startsWith("net.khasm")) {
        // MODIFIED ABOVE
            try {
                return getRawClassByteArray(name, allowFromParent);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load class file for '" + name + "'!", e);
            }
        }

        // MODIFIED BELOW
        byte[] input = new byte[0];
        try {
            // yes ternaries work yes i do this to allow *basically anything* to be transformed by khasm
            input = transformInitialized && canTransformClass(name)
                    ? provider.getEntrypointTransformer().transform(name)
                    : getRawClassByteArray(name, allowFromParent);
        } catch (IOException e) {
            throw KhasmUtilitiesKt.rethrow(e);
        }
        // MODIFIED ABOVE

        if (input == null) {
            try {
                input = getRawClassByteArray(name, allowFromParent);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load class file for '" + name + "'!", e);
            }
        }

        if (input != null) {

            // MODIFIED BELOW
            if (transformInitialized && canTransformClass(name))
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
