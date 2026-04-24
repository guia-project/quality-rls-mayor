package upm.tfg.moduleqr;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.springframework.stereotype.Component;

@Component
public class SparQLValidator implements QRValidator {
    @Override
    public boolean isType(RuleType rule) {
        return rule == RuleType.SPARQL;
    }

    @Override
    public boolean validateRule(String content) {
        try {
            QueryFactory.create(content);
            return true;
        }catch (Exception e) {
            return false;
        }
    }


    @Override
    public boolean validateKnowledgeGraph(String graphContent,String qrContent) {
        Model graphModel = ModelFactory.createDefaultModel();
        RDFParser.fromString(graphContent).lang(Lang.TURTLE).parse(graphModel);
        try(QueryExecution queryExec = QueryExecution.create(qrContent,graphModel)) {
            Query query = QueryFactory.create(qrContent);
            if (query.isAskType()) {
                return queryExec.execAsk();
            } else if (query.isSelectType()) {
                ResultSet res = queryExec.execSelect();
                return !res.hasNext();
            }else{
                throw new IllegalArgumentException("Unsupported query type");
            }
        }
    }
}
