package upm.tfg.moduleqr.model;

import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Data
@Entity
public class QualityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 5000)
    private String content;

    @Enumerated(EnumType.STRING)
    private RuleType ruleType;
    private String name;
    private String description;

    public QualityRule(String content, RuleType ruleType,String name, String description) {
        this.content = content;
        this.ruleType = ruleType;
        this.name = name;
        this.description = description;
    }

}
