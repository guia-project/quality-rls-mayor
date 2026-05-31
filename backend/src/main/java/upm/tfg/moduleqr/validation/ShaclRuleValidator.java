package upm.tfg.moduleqr.validation;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;

import org.apache.jena.riot.RDFParser;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.tdb2.TDB2Factory;
import org.springframework.stereotype.Component;
import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.moduleqr.model.ValidatorResult;

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
    public ValidatorResult validateKnowledgeGraph(String datasetId, String qrContent) {
        String datasetPath = "/app/data/datasets/" + datasetId + "/tdb";
        Dataset dataset = TDB2Factory.connectDataset(datasetPath);
        dataset.begin(ReadWrite.READ);
        try{
            Model graphModel = dataset.getDefaultModel();
            Model shaclModel = ModelFactory.createDefaultModel();
            RDFParser.fromString(qrContent).lang(Lang.TURTLE).parse(shaclModel);
            ValidationReport report = ShaclValidator.get().validate(shaclModel.getGraph(),graphModel.getGraph());
            return ValidatorResult.builder()
                    .passed(report.conforms())
                    .queryResults(null)
                    .build();
        }finally {
            dataset.end();
        }
    }


}
