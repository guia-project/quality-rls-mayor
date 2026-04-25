package upm.tfg.moduleqr.model;

import lombok.Data;

@Data
public class ValidationResult {

    private final String   ruleId;
    private final String   ruleName;
    private final RuleType ruleType;
    private final String   description;
    private final boolean  passed;
    private final String   message;

    public ValidationResult(String ruleId, String ruleName, RuleType ruleType,
                            String description, boolean passed, String message) {
        this.ruleId      = ruleId;
        this.ruleName    = ruleName;
        this.ruleType    = ruleType;
        this.description = description;
        this.passed      = passed;
        this.message     = message;
    }
}
