package love.yinlin.nativebridge

import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ModuleComponentSelector
import java.math.BigInteger

internal object NativeVersionSelection {
    fun validate(request: ModuleComponentSelector, selected: String, rule: String) {
        val constraint = request.versionConstraint
        val strict = constraint.strictVersion
        if (strict.isNotEmpty() && strict != selected) {
            throw GradleException("Native Bridge: strict requirement $strict for ${request.group}:${request.module} conflicts with selected $selected ($rule)")
        }
        if (constraint.rejectedVersions.any { it == selected || !isNumeric(it) }) {
            throw GradleException("Native Bridge: rejected-version constraint ${constraint.rejectedVersions} cannot select $selected ($rule)")
        }
        val requested = constraint.requiredVersion.ifEmpty { constraint.preferredVersion }
        if (requested.isEmpty() || requested == selected) return
        if (isNumeric(requested) && isNumeric(selected) && compare(requested, selected) <= 0) return
        throw GradleException("Native Bridge: ${request.displayName} is outside the tested version $selected ($rule); a matching Native port is required, and requests are never silently downgraded")
    }

    private fun isNumeric(version: String): Boolean = version.length <= 128 && version.matches(Regex("[0-9]+(?:\\.[0-9]+)*"))

    private fun compare(left: String, right: String): Int {
        val first = left.split('.').map(::BigInteger)
        val second = right.split('.').map(::BigInteger)
        for (index in 0 ..< maxOf(first.size, second.size)) {
            val order = (first.getOrNull(index) ?: BigInteger.ZERO).compareTo(second.getOrNull(index) ?: BigInteger.ZERO)
            if (order != 0) return order
        }
        return 0
    }
}
