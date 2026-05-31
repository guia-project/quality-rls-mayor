package upm.tfg.moduleqr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class ValidationResult {

    private final String   ruleId;
    private final String   ruleName;
    private final RuleType ruleType;
    private final String   description;
    private final boolean  passed;
    private final List<Map<String, String>> queryResults;

}
