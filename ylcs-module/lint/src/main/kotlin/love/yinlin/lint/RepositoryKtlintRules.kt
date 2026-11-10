package love.yinlin.lint

import io.github.ktlint.core.rule.engine.api.EditorConfigOverride
import io.github.ktlint.core.rule.engine.core.api.RuleV2Provider
import love.yinlin.lint.KtlintSeverity.*

internal data class KtlintRule(
    val name: String,
    val enabled: Boolean,
    val severity: KtlintSeverity = INFO,
    val description: String
)

internal object RepositoryKtlintRules {
    val rules = listOf(
        KtlintRule("standard:annotation", true, WARNING, "注解的排列与换行"),
        KtlintRule("standard:annotation-spacing", true, ERROR, "注解前后的空格"),
        KtlintRule("standard:blank-line-before-imports", true, WARNING, "导入区前的空行"),
        KtlintRule("standard:blank-line-between-when-conditions", true, WARNING, "when 分支条件之间的空行"),
        KtlintRule("standard:block-comment-initial-star-alignment", true, WARNING, "块注释行首星号的对齐"),
        KtlintRule("standard:class-naming", true, ERROR, "类和接口的命名"),
        KtlintRule("standard:colon-spacing", true, WARNING, "冒号前后的空格"),
        KtlintRule("standard:comma-spacing", true, WARNING, "逗号前后的空格"),
        KtlintRule("standard:comment-spacing", true, WARNING, "注释标记与内容之间的空格"),
        KtlintRule("standard:comment-wrapping", true, WARNING, "注释相对于代码的换行位置"),
        KtlintRule("standard:context-parameter-list-wrapping", true, WARNING, "上下文参数列表的换行"),
        KtlintRule("standard:context-receiver-wrapping", true, WARNING, "上下文接收者的换行"),
        KtlintRule("standard:curly-spacing", true, WARNING, "花括号前后的空格"),
        KtlintRule("standard:dot-spacing", true, WARNING, "点运算符前后的空格"),
        KtlintRule("standard:double-colon-spacing", true, WARNING, "双冒号运算符前后的空格"),
        KtlintRule("standard:enum-entry-name-case", true, WARNING, "枚举项名称的大小写"),
        KtlintRule("standard:filename", true, ERROR, "源文件名及其与顶层声明的对应关系"),
        KtlintRule("standard:fun-keyword-spacing", true, WARNING, "fun 关键字周围的空格"),
        KtlintRule("standard:function-expression-body", true, INFO, "可以简化为表达式体的函数"),
        KtlintRule("standard:function-literal", true, WARNING, "函数文字量的参数及函数体布局"),
        KtlintRule("standard:function-naming", true, WARNING, "函数名的命名"),
        KtlintRule("standard:function-return-type-spacing", true, WARNING, "函数返回类型附近的空格"),
        KtlintRule("standard:function-start-of-body-spacing", true, WARNING, "函数体起始位置附近的空格"),
        KtlintRule("standard:function-type-modifier-spacing", true, WARNING, "函数类型修饰符附近的空格"),
        KtlintRule("standard:function-type-reference-spacing", true, WARNING, "函数类型引用附近的空格"),
        KtlintRule("standard:indent", true, WARNING, "代码缩进"),
        KtlintRule("standard:kdoc", true, WARNING, "KDoc 的位置与基本结构"),
        KtlintRule("standard:kdoc-wrapping", true, WARNING, "KDoc 注释的换行"),
        KtlintRule("standard:keyword-spacing", true, WARNING, "关键字前后的空格"),
        KtlintRule("standard:lambda-return", true, INFO, "Lambda 中返回表达式的写法"),
        KtlintRule("standard:mixed-condition-operators", true, ERROR, "条件表达式中混用逻辑运算符"),
        KtlintRule("standard:modifier-list-spacing", true, WARNING, "修饰符列表中的空格"),
        KtlintRule("standard:modifier-order", true, WARNING, "修饰符的顺序"),
        KtlintRule("standard:no-blank-line-at-start-of-file", true, ERROR, "文件开头的多余空行"),
        KtlintRule("standard:no-blank-line-before-rbrace", true, WARNING, "右花括号前的多余空行"),
        KtlintRule("standard:no-blank-line-in-list", true, WARNING, "列表项之间的多余空行"),
        KtlintRule("standard:no-blank-lines-in-chained-method-calls", true, WARNING, "链式调用中的空行"),
        KtlintRule("standard:no-consecutive-blank-lines", true, INFO, "连续的多余空行"),
        KtlintRule("standard:no-consecutive-comments", true, WARNING, "相邻行注释的组织"),
        KtlintRule("standard:no-empty-class-body", true, WARNING, "多余的空类体"),
        KtlintRule("standard:no-empty-file", true, ERROR, "没有实际声明的空文件"),
        KtlintRule("standard:no-empty-first-line-in-class-body", true, INFO, "类体开头的空行"),
        KtlintRule("standard:no-empty-first-line-in-method-block", true, WARNING, "函数体开头的空行"),
        KtlintRule("standard:no-line-break-after-else", true, WARNING, "else 关键字之后的多余换行"),
        KtlintRule("standard:no-line-break-before-assignment", true, WARNING, "赋值符之前的多余换行"),
        KtlintRule("standard:no-multi-spaces", true, WARNING, "多余的连续空格"),
        KtlintRule("standard:no-semi", true, ERROR, "不必要的分号"),
        KtlintRule("standard:no-trailing-spaces", true, WARNING, "行尾空白字符"),
        KtlintRule("standard:no-unit-return", true, WARNING, "不必要的显式 Unit 返回类型"),
        KtlintRule("standard:no-unused-imports", true, WARNING, "未使用的 import 声明"),
        KtlintRule("standard:nullable-type-spacing", true, WARNING, "可空类型问号附近的空格"),
        KtlintRule("standard:op-spacing", true, WARNING, "运算符前后的空格"),
        KtlintRule("standard:package-name", true, ERROR, "包名的命名"),
        KtlintRule("standard:parameter-list-spacing", true, WARNING, "形参列表周围的空格"),
        KtlintRule("standard:paren-spacing", true, WARNING, "圆括号前后的空格"),
        KtlintRule("standard:property-naming", true, WARNING, "属性名的命名"),
        KtlintRule("standard:spacing-around-angle-brackets", true, WARNING, "尖括号前后的空格"),
        KtlintRule("standard:spacing-between-function-name-and-opening-parenthesis", true, WARNING, "函数名与左圆括号之间的空格"),
        KtlintRule("standard:square-brackets-spacing", true, WARNING, "方括号前后的空格"),
        KtlintRule("standard:string-template-indent", true, WARNING, "多行字符串模板的缩进"),
        KtlintRule("standard:then-spacing", true, WARNING, "if 条件与随后分支之间的空格"),
        KtlintRule("standard:trailing-comma-on-call-site", true, WARNING, "调用处的尾随逗号"),
        KtlintRule("standard:trailing-comma-on-declaration-site", true, WARNING, "声明处的尾随逗号"),
        KtlintRule("standard:try-catch-finally-spacing", true, WARNING, "try、catch 和 finally 周围的空格"),
        KtlintRule("standard:type-argument-list-spacing", true, WARNING, "类型实参列表周围的空格"),
        KtlintRule("standard:type-parameter-list-spacing", true, WARNING, "类型形参列表周围的空格"),
        KtlintRule("standard:unary-op-spacing", true, WARNING, "一元运算符附近的空格"),
        KtlintRule("standard:unnecessary-parentheses-before-trailing-lambda", true, WARNING, "尾随 Lambda 前的多余空圆括号"),
        KtlintRule(RangeSpacingRule.ID, true, WARNING, "范围运算符两侧必须留空格"),
    )

    val custom = setOf(
        RuleV2Provider { RangeSpacingRule() }
    )

    val options = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE
}
