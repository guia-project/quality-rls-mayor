package upm.tfg.moduleqr;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;

import org.apache.jena.riot.RDFParser;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;
import org.springframework.stereotype.Component;

import java.io.StringReader;

@Component
public class ShaclRuleValidator implements QRValidator {
    @Override
    public boolean isType(RuleType rule) {
        return rule == RuleType.SHACL;
    }

    @Override
    public boolean validateRule(String content) {
        try {
            Model model = ModelFactory.createDefaultModel();
            model.read(new StringReader(content), null, "TURTLE");
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public boolean validateKnowledgeGraph(String graphContent,String qrContent) {
        Model graphModel = ModelFactory.createDefaultModel();
        RDFParser.fromString(graphContent).lang(Lang.TURTLE).parse(graphModel);
        Model shaclModel = ModelFactory.createDefaultModel();
        RDFParser.fromString(qrContent).lang(Lang.TURTLE).parse(shaclModel);
        ValidationReport report = ShaclValidator.get().validate(shaclModel.getGraph(),graphModel.getGraph());
        return report.conforms();
    }
}
