package upm.tfg.moduleqr.validation;

import upm.tfg.moduleqr.model.RuleType;

public interface QRValidator {

    boolean isType(RuleType rule);

    boolean validateRule(String content);

    boolean validateKnowledgeGraph(String graphContent, String qrContent);
}
