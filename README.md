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
    // including is currently not recommended as khasm includes kotlin-stdlib itself
    modImplementation('khasm:khasm:VERSION')
}
```

Get the latest version (instead of `VERSION`) from the [properties file](gradle.properties)

Initialize by making a subclass of [KhasmInitializer](src/main/kotlin/net/khasm/KhasmLoad.kt)

## Example

[This](src/main/kotlin/net/khasm/test/KhasmTest.kt) is the fabric-example-mod
[example mixin](https://github.com/FabricMC/fabric-example-mod/blob/master/src/main/java/net/fabricmc/example/mixin/ExampleMixin.java)
implemented with khasm.
