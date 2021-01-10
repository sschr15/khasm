package net.khasm.init

import net.devtech.grossfabrichacks.entrypoints.PrePreLaunch
import net.devtech.grossfabrichacks.transformer.TransformerApi
import net.khasm.test.KhasmTest
import net.khasm.transform.KhasmTransformerDispatcher

object KhasmPrePreLaunch : PrePreLaunch {
    override fun onPrePreLaunch() {
        KhasmTest.registerTest()

        TransformerApi.registerPreMixinAsmClassTransformer { name, node ->
            // No recursion, maybe security as well? idk
            if (!name.startsWith("net/khasm")) {
                KhasmTransformerDispatcher.tryTransform(node)
                node.visitEnd()
            }
        }
    }
}
