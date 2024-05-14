/**
 * Copyright © 2024 Apple Inc. and the Pkl project authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pkl.lsp.ast

import kotlin.reflect.KClass
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.pkl.core.parser.antlr.PklParser.*

interface Node {
  val span: Span
  val parent: Node?
  val children: List<Node>
  val enclosingModule: PklModule?
}

interface QualifiedIdentifier : Node {
  val identifiers: List<Terminal>
}

interface StringConstant : Node {
  val value: String
}

interface IdentifierOwner {
  val identifier: Terminal?
}

interface ModifierListOwner : Node {
  val modifiers: List<Terminal>?

  val isAbstract: Boolean
    get() = hasModifier(TokenType.ABSTRACT)

  val isExternal: Boolean
    get() = hasModifier(TokenType.EXTERNAL)

  val isHidden: Boolean
    get() = hasModifier(TokenType.HIDDEN)

  val isLocal: Boolean
    get() = hasModifier(TokenType.LOCAL)

  val isOpen: Boolean
    get() = hasModifier(TokenType.OPEN)

  val isFixed: Boolean
    get() = hasModifier(TokenType.FIXED)

  val isConst: Boolean
    get() = hasModifier(TokenType.CONST)

  val isFixedOrConst: Boolean
    get() = hasEitherModifier(TokenType.CONST, TokenType.FIXED)

  val isAbstractOrOpen: Boolean
    get() = hasEitherModifier(TokenType.ABSTRACT, TokenType.OPEN)

  private fun hasModifier(tokenType: TokenType): Boolean {
    return modifiers?.any { it.type == tokenType } ?: false
  }

  private fun hasEitherModifier(modifier1: TokenType, modifier2: TokenType): Boolean {
    val modifiers = modifiers ?: return false
    return modifiers.any { it.type == modifier1 || it.type == modifier2 }
  }
}

interface DocCommentOwner : Node {
  // assertion: DocComment is always the first node
  val docComment: Terminal?
    get() = (children.firstOrNull() as? Terminal)?.also { assert(it.type == TokenType.DocComment) }
}

interface PklModule : Node {
  val isAmend: Boolean
  val declaration: ModuleDeclaration?
  val imports: List<ImportClause>?
  val members: List<ModuleMember>
  val typeAliases: List<TypeAlias>
  val classes: List<Clazz>
  val supermodule: PklModule?
  val cache: ModuleMemberCache
}

/** Either [moduleHeader] is set, or [moduleExtendsAmendsClause] is set. */
interface ModuleDeclaration : Node, ModifierListOwner, DocCommentOwner {
  val annotations: List<Annotation>

  val isAmend: Boolean
    get() = effectiveExtendsOrAmendsCluse?.isAmend ?: false

  val moduleHeader: ModuleHeader?

  val moduleExtendsAmendsClause: ModuleExtendsAmendsClause?

  val effectiveExtendsOrAmendsCluse: ModuleExtendsAmendsClause?
    get() = moduleHeader?.moduleExtendsAmendsClause ?: moduleExtendsAmendsClause
}

interface ModuleHeader : Node, ModifierListOwner {
  val qualifiedIdentifier: QualifiedIdentifier?
  val moduleExtendsAmendsClause: ModuleExtendsAmendsClause?
}

interface ModuleExtendsAmendsClause : Node {
  val isAmend: Boolean

  val isExtend: Boolean

  val moduleUri: String?
}

sealed interface ModuleMember : Node, DocCommentOwner, ModifierListOwner {
  val name: String
}

sealed interface TypeDefOrProperty : Node

sealed interface TypeDef : TypeDefOrProperty, ModuleMember

interface Clazz : TypeDef {
  val classHeader: ClassHeader
  val annotations: List<Annotation>?
  val classBody: ClassBody?
  val cache: ClassMemberCache
}

interface ClassBody : Node {
  val members: List<ClassMember>
}

interface Annotation : Node {
  val typeName: TypeName
  val objectBody: ObjectBody
}

interface TypeName : Node {
  val module: Terminal?
  val simpleTypeName: SimpleTypeName
}

interface SimpleTypeName : Node, IdentifierOwner

interface ClassHeader : Node, IdentifierOwner, ModifierListOwner {
  val typeParameterList: TypeParameterList?
  val extends: PklType?
}

sealed interface ClassMember : Node, DocCommentOwner

interface ClassProperty :
  ModuleMember, ClassMember, IdentifierOwner, ModifierListOwner, TypeDefOrProperty {
  val typeAnnotation: TypeAnnotation?

  val expr: Expr?

  val objectBody: ObjectBody?
}

interface ClassMethod : ModuleMember, ClassMember {
  val methodHeader: MethodHeader
}

sealed interface ObjectMember : Node

interface ObjectProperty : ObjectMember, IdentifierOwner

interface ObjectMethod : ObjectMember

interface ObjectEntry : ObjectMember

interface MemberPredicate : ObjectMember

interface ForGenerator : ObjectMember

interface WhenGenerator : ObjectMember

interface ObjectElement : ObjectMember

interface ObjectSpread : ObjectMember

interface MethodHeader : Node, ModifierListOwner, IdentifierOwner {
  val parameterList: ParameterList?

  val typeParameterList: TypeParameterList?
}

interface ObjectBody : Node {
  val parameterList: ParameterList?

  val members: List<ObjectMember>?
}

interface TypeParameterList : Node {
  val typeParameters: List<TypeParameter>
}

enum class Variance {
  IN,
  OUT
}

interface TypeParameter : Node {
  val variance: Variance?
  val name: String
}

interface ParameterList : Node

interface TypeAlias : IdentifierOwner, ModifierListOwner, TypeDef

interface Terminal : Node {
  val type: TokenType

  /** The verbatim text of this node. */
  val text: String
}

interface ImportBase : Node {
  val isGlob: Boolean

  val moduleUri: String
}

sealed interface ImportClause : ImportBase, IdentifierOwner

sealed interface Expr

interface ThisExpr : Expr

interface OuterExpr : Expr

interface ModuleExpr : Expr

interface NullLiteralExpr : Expr

interface TrueLiteralExpr : Expr

interface FalseLiteralExpr : Expr

interface IntLiteralExpr : Expr

interface FloatLiteralExpr : Expr

interface ThrowExpr : Expr

interface TraceExpr : Expr

interface ImportExpr : ImportBase, Expr

interface ReadExpr : Expr

interface UnqualifiedAccessExpr : Expr, IdentifierOwner

interface SingleLineStringLiteral : Expr

interface MultilineStringLiteral : Expr

interface NewExpr : Expr

interface AmendExpr : Expr

interface SuperAccessExpr : Expr

interface SuperSubscriptExpr : Expr

interface QualifiedAccessExpr : Expr

interface SubscriptExpr : Expr

interface NonNullExpr : Expr

interface UnaryMinusExpr : Expr

interface LogicalNotExpr : Expr

interface ExponentiationExpr : Expr

interface MultiplicativeExpr : Expr

interface AdditiveExpr : Expr

interface ComparisonExpr : Expr

interface TypeTestExpr : Expr

interface EqualityExpr : Expr

interface LogicalAndExpr : Expr

interface LogicalOrExpr : Expr

interface PipeExpr : Expr

interface NullCoalesceExpr : Expr

interface IfExpr : Expr

interface LetExpr : Expr, IdentifierOwner

interface FunctionLiteral : Expr

interface ParenthesizedExpr : Expr

interface TypeAnnotation : Node {
  val pklType: PklType?
}

sealed interface PklType : Node

interface UnknownPklType : PklType

interface NothingPklType : PklType

interface ModulePklType : PklType

interface StringLiteralPklType : PklType

interface DeclaredPklType : PklType {
  val name: TypeName
}

interface ParenthesizedPklType : PklType

interface NullablePklType : PklType

interface ConstrainedPklType : PklType

interface UnionPklType : PklType

interface FunctionPklType : PklType

abstract class AbstractNode(override val parent: Node?, protected open val ctx: ParseTree) : Node {
  private val childrenByType: Map<KClass<out Node>, List<Node>> by lazy {
    if (ctx !is ParserRuleContext) {
      return@lazy emptyMap()
    }
    val parserCtx = ctx as ParserRuleContext
    val self = this
    // use LinkedHashMap to preserve order
    LinkedHashMap<KClass<out Node>, MutableList<Node>>().also { map ->
      for (idx in parserCtx.children.indices) {
        val node = parserCtx.children.toNode(self, idx) ?: continue
        when (val nodes = map[node::class]) {
          null -> map[node::class] = mutableListOf(node)
          else -> nodes.add(node)
        }
      }
    }
  }

  override val span: Span by lazy {
    when (ctx) {
      is ParserRuleContext -> {
        val c = ctx as ParserRuleContext
        val begin = c.start
        val end = c.stop
        val endCol = end.charPositionInLine + 1 + end.text.length
        Span(begin.line, begin.charPositionInLine + 1, end.line, endCol)
      }
      else -> {
        ctx as TerminalNode
        val token = (ctx as TerminalNode).symbol
        val endCol = token.charPositionInLine + 1 + token.text.length
        Span(token.line, token.charPositionInLine + 1, token.line, endCol)
      }
    }
  }

  override val enclosingModule: PklModule? by lazy {
    var top: Node? = this
    // navigate the parent chain until the module is reached
    while (top != null) {
      if (top is PklModule) return@lazy top
      top = top.parent
    }
    null
  }

  protected fun <T : Node> getChild(clazz: KClass<T>): T? {
    @Suppress("UNCHECKED_CAST") return childrenByType[clazz]?.firstOrNull() as T?
  }

  protected fun <T : Node> getChildren(clazz: KClass<T>): List<T>? {
    @Suppress("UNCHECKED_CAST") return childrenByType[clazz] as List<T>?
  }

  protected val terminals: List<Terminal> by lazy {
    getChildren(TerminalImpl::class) ?: emptyList()
  }

  override val children: List<Node> by lazy { childrenByType.values.flatten() }
}

fun List<ParseTree>.toNode(parent: Node?, idx: Int): Node? {
  return when (val parseTree = get(idx)) {
    is ModuleContext -> PklModuleImpl(parseTree)
    is ModuleDeclContext -> ModuleDeclarationImpl(parent!!, parseTree)
    is ImportClauseContext -> ImportClauseImpl(parent!!, parseTree)
    is ModuleExtendsOrAmendsClauseContext -> ModuleExtendsAmendsClauseImpl(parent!!, parseTree)
    is ClazzContext -> ClazzImpl(parent!!, parseTree)
    is ClassHeaderContext -> ClassHeaderImpl(parent!!, parseTree)
    is ClassBodyContext -> ClassBodyImpl(parent!!, parseTree)
    is ClassPropertyContext -> ClassPropertyImpl(parent!!, parseTree)
    is MethodHeaderContext -> MethodHeaderImpl(parent!!, parseTree)
    is ClassMethodContext -> ClassMethodImpl(parent!!, parseTree)
    is ParameterListContext -> ParameterListImpl(parent!!, parseTree)
    is TypeAnnotationContext -> TypeAnnotationImpl(parent!!, parseTree)
    is UnknownTypeContext -> UnknownPklTypeImpl(parent!!, parseTree)
    is NothingTypeContext -> NothingPklTypeImpl(parent!!, parseTree)
    is ModuleTypeContext -> ModulePklTypeImpl(parent!!, parseTree)
    is StringLiteralTypeContext -> StringLiteralPklTypeImpl(parent!!, parseTree)
    is DeclaredTypeContext -> DeclaredPklTypeImpl(parent!!, parseTree)
    is ParenthesizedTypeContext -> ParenthesizedPklTypeImpl(parent!!, parseTree)
    is NullableTypeContext -> NullablePklTypeImpl(parent!!, parseTree)
    is ConstrainedTypeContext -> ConstrainedPklTypeImpl(parent!!, parseTree)
    is UnionTypeContext -> UnionPklTypeImpl(parent!!, parseTree)
    is FunctionTypeContext -> FunctionPklTypeImpl(parent!!, parseTree)
    is ThisExprContext -> ThisExprImpl(parent!!, parseTree)
    is OuterExprContext -> OuterExprImpl(parent!!, parseTree)
    is ModuleExprContext -> ModuleExprImpl(parent!!, parseTree)
    is NullLiteralContext -> NullLiteralExprImpl(parent!!, parseTree)
    is TrueLiteralContext -> TrueLiteralExprImpl(parent!!, parseTree)
    is FalseLiteralContext -> FalseLiteralExprImpl(parent!!, parseTree)
    is IntLiteralContext -> IntLiteralExprImpl(parent!!, parseTree)
    is FloatLiteralContext -> FloatLiteralExprImpl(parent!!, parseTree)
    is ThrowExprContext -> ThrowExprImpl(parent!!, parseTree)
    is TraceExprContext -> TraceExprImpl(parent!!, parseTree)
    is ImportExprContext -> ImportExprImpl(parent!!, parseTree)
    is ReadExprContext -> ReadExprImpl(parent!!, parseTree)
    is UnqualifiedAccessExprContext -> UnqualifiedAccessExprImpl(parent!!, parseTree)
    is SingleLineStringLiteralContext -> SingleLineStringLiteralImpl(parent!!, parseTree)
    is MultiLineStringLiteralContext -> MultilineStringLiteralImpl(parent!!, parseTree)
    is NewExprContext -> NewExprImpl(parent!!, parseTree)
    is AmendExprContext -> AmendExprImpl(parent!!, parseTree)
    is SuperAccessExprContext -> SuperAccessExprImpl(parent!!, parseTree)
    is SuperSubscriptExprContext -> SuperSubscriptExprImpl(parent!!, parseTree)
    is QualifiedAccessExprContext -> QualifiedAccessExprImpl(parent!!, parseTree)
    is SubscriptExprContext -> SubscriptExprImpl(parent!!, parseTree)
    is NonNullExprContext -> NonNullExprImpl(parent!!, parseTree)
    is UnaryMinusExprContext -> UnaryMinusExprImpl(parent!!, parseTree)
    is LogicalNotExprContext -> LogicalNotExprImpl(parent!!, parseTree)
    is ExponentiationExprContext -> ExponentiationExprImpl(parent!!, parseTree)
    is MultiplicativeExprContext -> MultiplicativeExprImpl(parent!!, parseTree)
    is AdditiveExprContext -> AdditiveExprImpl(parent!!, parseTree)
    is ComparisonExprContext -> ComparisonExprImpl(parent!!, parseTree)
    is TypeTestExprContext -> TypeTestExprImpl(parent!!, parseTree)
    is EqualityExprContext -> EqualityExprImpl(parent!!, parseTree)
    is LogicalAndExprContext -> LogicalAndExprImpl(parent!!, parseTree)
    is LogicalOrExprContext -> LogicalOrExprImpl(parent!!, parseTree)
    is PipeExprContext -> PipeExprImpl(parent!!, parseTree)
    is NullCoalesceExprContext -> NullCoalesceExprImpl(parent!!, parseTree)
    is IfExprContext -> IfExprImpl(parent!!, parseTree)
    is LetExprContext -> LetExprImpl(parent!!, parseTree)
    is FunctionLiteralContext -> FunctionLiteralImpl(parent!!, parseTree)
    is ParenthesizedExprContext -> ParenthesizedExprImpl(parent!!, parseTree)
    is QualifiedIdentifierContext -> QualifiedIdentifierImpl(parent!!, parseTree)
    is ObjectBodyContext -> ObjectBodyImpl(parent!!, parseTree)
    is ObjectPropertyContext -> ObjectPropertyImpl(parent!!, parseTree)
    is ObjectMethodContext -> ObjectMethodImpl(parent!!, parseTree)
    is ObjectEntryContext -> ObjectEntryImpl(parent!!, parseTree)
    is MemberPredicateContext -> MemberPredicateImpl(parent!!, parseTree)
    is ForGeneratorContext -> ForGeneratorImpl(parent!!, parseTree)
    is WhenGeneratorContext -> WhenGeneratorImpl(parent!!, parseTree)
    is ObjectElementContext -> ObjectElementImpl(parent!!, parseTree)
    is ObjectSpreadContext -> ObjectSpreadImpl(parent!!, parseTree)
    is TypeParameterContext -> TypeParameterImpl(parent!!, parseTree)
    is TypeParameterListContext -> TypeParameterListImpl(parent!!, parseTree)
    // treat modifiers as terminals; matches how we do it in pkl-intellij
    is ModifierContext -> {
      val terminalNode =
        parseTree.CONST()
          ?: parseTree.ABSTRACT() ?: parseTree.HIDDEN_() ?: parseTree.FIXED()
            ?: parseTree.EXTERNAL() ?: parseTree.LOCAL() ?: parseTree.OPEN()
      terminalNode.toTerminal(parent!!)
    }
    is TerminalNode -> parseTree.toTerminal(parent!!)
    else -> null
  }
}
