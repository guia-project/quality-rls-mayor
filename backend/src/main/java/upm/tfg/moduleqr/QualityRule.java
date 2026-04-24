package upm.tfg.moduleqr;

import lombok.Data;

import java.util.UUID;
@Data
public class QualityRule {
    private String id;
    private String content;
    private RuleType ruleType;
    private String name;
    private String description;

    public QualityRule(String content, RuleType ruleType,String name, String description) {
        this.content = content;
        this.ruleType = ruleType;
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
    }

}
