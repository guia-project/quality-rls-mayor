package upm.tfg.moduleqr;

public interface QRValidator {

    boolean isType(RuleType rule);

    boolean validateRule(String content);

    boolean validateKnowledgeGraph(String graphContent, String qrContent);
}
