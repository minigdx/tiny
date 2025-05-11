package com.github.minigdx.tiny.doc

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.mingdx.tiny.doc.TinyVariable
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor

/**
 * Visit a Lua Function which is defined in Kotlin as follows:
 *
 *
 * ```
 *     @TinyFunction
 *     inner class clip : LibFunction() {
 *         override fun call(): LuaValue {
 *             resourceAccess.frameBuffer.clipper.reset()
 *             return NONE
 *         }
 *
 *         override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
 *             resourceAccess.frameBuffer.clipper.set(a.checkint(), b.checkint(), c.checkint(), d.checkint())
 *             return NONE
 *         }
 *     }
 * ```
 */
@OptIn(KspExperimental::class)
class TinyFunctionVisitor :
    KSDefaultVisitor<MutableList<TinyFunctionDescriptor>, MutableList<TinyFunctionDescriptor>>() {
    override fun defaultHandler(
        node: KSNode,
        data: MutableList<TinyFunctionDescriptor>,
    ): MutableList<TinyFunctionDescriptor> = data

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: MutableList<TinyFunctionDescriptor>,
    ): MutableList<TinyFunctionDescriptor> {
        val functions = classDeclaration.getAnnotationsByType(TinyFunction::class)
        if (functions.count() == 0) return data

        val defaultName = classDeclaration.accept(
            object : KSDefaultVisitor<Unit, String>() {
                override fun defaultHandler(
                    node: KSNode,
                    data: Unit,
                ): String = ""

                override fun visitClassDeclaration(
                    classDeclaration: KSClassDeclaration,
                    data: Unit,
                ): String {
                    return classDeclaration.simpleName.asString()
                }
            },
            Unit,
        )

        val result =
            functions.map { function ->
                // Get the name of the class
                val name =
                    function.name.ifBlank {
                        classDeclaration.accept(
                            object : KSDefaultVisitor<Unit, String>() {
                                override fun defaultHandler(
                                    node: KSNode,
                                    data: Unit,
                                ): String = ""

                                override fun visitClassDeclaration(
                                    classDeclaration: KSClassDeclaration,
                                    data: Unit,
                                ): String {
                                    return classDeclaration.simpleName.asString()
                                }
                            },
                            Unit,
                        )
                    }

                // Get all TinyCall annotated functions.
                val calls = mutableListOf<TinyCallDescriptor>()
                classDeclaration.getDeclaredFunctions()
                    .filter { f -> f.isAnnotationPresent(TinyCall::class) }
                    .forEach { f ->
                        val call = TinyCallDescriptor()
                        call.description = f.getAnnotationsByType(TinyCall::class).firstOrNull()?.description ?: ""
                        f.parameters.map { p ->
                            val multiArgs = p.getAnnotationsByType(TinyArgs::class)
                            val multiArg = multiArgs.firstOrNull()
                            if (multiArg != null) {
                                val documentation = multiArg.documentations
                                val docs = if (documentation.size < multiArg.names.size) {
                                    documentation +
                                        (0 until multiArg.names.size - documentation.size).map { "" }
                                            .toTypedArray()
                                } else {
                                    documentation
                                }

                                call.args += multiArg.names.map { n -> TinyArgDescriptor(n) }
                                    .zip(docs) { ano, doc ->
                                        ano.apply {
                                            ano.description = doc
                                        }
                                    }
                            } else {
                                val args = p.getAnnotationsByType(TinyArg::class)
                                val arg = args.firstOrNull()?.name ?: p.name?.asString() ?: ""
                                call.args += TinyArgDescriptor(arg, args.firstOrNull()?.description ?: "")
                            }
                        }
                        calls.add(call)
                    }

                val example = function.example.ifBlank { null }
                val levelPath = function.levelPath.ifBlank { null }
                val spritePath = function.spritePath.ifBlank { null }
                TinyFunctionDescriptor(
                    example = example,
                    name = name.ifBlank { defaultName },
                    description = function.description,
                    levelPath = levelPath,
                    spritePath = spritePath,
                    calls = calls,
                )
            }
        data.addAll(result)
        return super.visitAnnotated(classDeclaration, data)
    }
}

@OptIn(KspExperimental::class)
class TinyLibVisitor : KSDefaultVisitor<TinyLibDescriptor, TinyLibDescriptor>() {
    override fun defaultHandler(
        node: KSNode,
        data: TinyLibDescriptor,
    ): TinyLibDescriptor = data

    override fun visitAnnotated(
        annotated: KSAnnotated,
        data: TinyLibDescriptor,
    ): TinyLibDescriptor {
        val libAnnotation = annotated.getAnnotationsByType(TinyLib::class)
        val lib = libAnnotation.firstOrNull() ?: return data

        data.name = lib.name
        data.description = lib.description

        return super.visitAnnotated(annotated, data)
    }

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: TinyLibDescriptor,
    ): TinyLibDescriptor {
        if (!classDeclaration.isAnnotationPresent(TinyLib::class)) return data

        val functions = classDeclaration.declarations.map { declaration ->
            declaration.accept(TinyFunctionVisitor(), mutableListOf())
        }.flatMap { functions -> functions }.filter { f -> f.name.isNotBlank() }

        val variables = classDeclaration.getDeclaredFunctions()
            .flatMap { f -> f.getAnnotationsByType(TinyVariable::class) }
            .map { annotation ->
                TinyVariableDescriptor(
                    name = annotation.name,
                    description = annotation.description,
                    hidden = annotation.hideInDocumentation,
                )
            }

        data.functions = functions.toList()
        data.variables = variables.toList()

        return super.visitClassDeclaration(classDeclaration, data)
    }
}
