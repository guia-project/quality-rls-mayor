package upm.tfg.moduleqr.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import upm.tfg.modulekg.model.Dataset;


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

    @Column(nullable = false)
    private boolean enabled;

    @ManyToOne
    @JoinColumn(name = "dataset_id")
    @JsonBackReference
    private Dataset dataset;

    public QualityRule(String content, RuleType ruleType,String name, String description, Dataset dataset) {
        this.content = content;
        this.ruleType = ruleType;
        this.name = name;
        this.description = description;
        this.enabled = true;
        this.dataset = dataset;

    }


}
