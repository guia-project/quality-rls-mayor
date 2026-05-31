package upm.tfg.moduleqr.validation;

import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.moduleqr.model.ValidatorResult;

public interface QRValidator {

    boolean isType(RuleType rule);

    boolean validateRule(String content);

    ValidatorResult validateKnowledgeGraph(String datasetId, String qrContent);
}
