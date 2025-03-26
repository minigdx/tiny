package com.github.minigdx.tiny.doc

class TinyArgDescriptor(
    var name: String,
    var description: String = "",
)

class TinyCallDescriptor(
    var description: String = "",
    var args: List<TinyArgDescriptor> = emptyList(),
)

data class TinyFunctionDescriptor(
    var name: String = "",
    var description: String = "",
    var calls: List<TinyCallDescriptor> = emptyList(),
    var example: String? = null,
    var spritePath: String? = null,
    var levelPath: String? = null,
)

class TinyLibDescriptor(
    var name: String = "",
    var description: String = "",
    var functions: List<TinyFunctionDescriptor> = emptyList(),
    var variables: List<TinyVariableDescriptor> = emptyList(),
)

class TinyVariableDescriptor(
    var name: String = "",
    var description: String = "",
)
