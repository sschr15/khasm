# Khasm

a concern library that does what Mixin can't

## Using

```groovy
repositories {
    maven {
        url 'https://maven.concern.i.ng/'
    }
}

dependencies {
    modImplementation include('khasm:khasm:VERSION')
}
```

Get the latest version (instead of `VERSION`) from the [properties file](gradle.properties)

Initialize using the `KhasmLoad` interface and the `khasm:code-setup` entrypoint in your `fabric.mod.json`:
```json5
    // ...
    "entrypoints": {
        "khasm:code-setup": [
            "package.with.setup.Class"
        ]
    }
    // ...
```

## Example

[This](src/main/kotlin/net/khasm/test/KhasmTest.kt) is the fabric-example-mod
[example mixin](https://github.com/FabricMC/fabric-example-mod/blob/master/src/main/java/net/fabricmc/example/mixin/ExampleMixin.java)
implemented with khasm.
