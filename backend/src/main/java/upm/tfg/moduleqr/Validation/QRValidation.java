package upm.tfg.moduleqr.Validation;

import org.springframework.stereotype.Service;
import upm.tfg.moduleqr.model.RuleType;

import java.util.List;

@Service
public class QRValidation {

    private final List<QRValidator> validators;
    public QRValidation(List<QRValidator> validators) {
        this.validators = validators;

    }

    public boolean validateRule(String content, RuleType ruleType) {

        QRValidator validator = validators.stream()
                .filter(v -> v.isType(ruleType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No validator found"));

        return validator.validateRule(content);

    }


    public boolean validateKnowledgeGraph(String graphContent,String qrContent, RuleType ruleType) {
        QRValidator validator = validators.stream()
                .filter(v -> v.isType(ruleType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No validator found"));

        return validator.validateKnowledgeGraph(graphContent,qrContent);

    }


}
