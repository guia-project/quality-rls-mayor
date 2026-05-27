package upm.tfg.moduleqr.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationResult {

    private final String   ruleId;
    private final String   ruleName;
    private final RuleType ruleType;
    private final String   description;
    private final boolean  passed;

}
