package upm.tfg.moduleqr.validation;

import org.springframework.stereotype.Service;
import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.moduleqr.model.ValidatorResult;

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


    public ValidatorResult validateKnowledgeGraph(String datasetId, String qrContent, RuleType ruleType) {
        QRValidator validator = validators.stream()
                .filter(v -> v.isType(ruleType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No validator found"));

        return validator.validateKnowledgeGraph(datasetId,qrContent);

    }


}
