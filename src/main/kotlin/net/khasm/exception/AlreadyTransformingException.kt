package net.khasm.exception

class AlreadyTransformingException(classBeingTransformed: String) :
    Exception("$classBeingTransformed was attempted to be loaded while already being transformed! (use a string if you need a TypeLike)")
