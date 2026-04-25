package upm.tfg.moduleqr.Validation;

import upm.tfg.moduleqr.model.RuleType;

public interface QRValidator {

    boolean isType(RuleType rule);

    boolean validateRule(String content);

    boolean validateKnowledgeGraph(String graphContent, String qrContent);
}
