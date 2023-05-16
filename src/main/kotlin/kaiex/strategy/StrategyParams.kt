package kaiex.strategy

class StrategyParams(
    private val parameters: Map<String, String>,
    private val mandatoryParams: Set<String>
) {

    init {
        validateMandatoryParams()
    }

    private fun validateMandatoryParams() {
        for (param in mandatoryParams) {
            if (!parameters.containsKey(param)) {
                throw MissingParamException("Mandatory parameter '$param' is missing")
            }
        }
    }

    fun getString(key: String): String {
        checkKeyExists(key)
        return parameters.getValue(key)
    }

    fun getInt(key: String): Int {
        checkKeyExists(key)
        return parameters.getValue(key).toIntOrNull()
            ?: throw TypeCastException("Error casting parameter value '${parameters.getValue(key)}' to Int")
    }

    fun getDouble(key: String): Double {
        checkKeyExists(key)
        return parameters.getValue(key).toDoubleOrNull()
            ?: throw TypeCastException("Error casting parameter value '${parameters.getValue(key)}' to Double")
    }

    fun getBoolean(key: String): Boolean {
        checkKeyExists(key)
        return parameters.getValue(key).toBooleanStrictOrNull()
            ?: throw TypeCastException("Error casting parameter value '${parameters.getValue(key)}' to Boolean")
    }

    private fun checkKeyExists(key: String) {
        if (!parameters.containsKey(key)) {
            throw MissingParamException("Parameter '$key' is missing")
        }
    }

    class MissingParamException(message: String) : Exception(message)
}