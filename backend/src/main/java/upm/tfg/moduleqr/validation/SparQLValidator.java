package upm.tfg.moduleqr.validation;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb2.TDB2Factory;
import org.springframework.stereotype.Component;
import upm.tfg.moduleqr.model.RuleType;
import upm.tfg.moduleqr.model.ValidatorResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ValidatorResult validateKnowledgeGraph(String datasetId, String qrContent) {
        String datasetPath ="/app/data/datasets/" + datasetId + "/tdb";
        Dataset dataset =TDB2Factory.connectDataset(datasetPath);
        dataset.begin(ReadWrite.READ);
        try(QueryExecution queryExec = QueryExecution.create(qrContent,dataset)) {
            Query query = QueryFactory.create(qrContent);
            if (query.isAskType()) {
                return ValidatorResult.builder()
                        .passed(queryExec.execAsk())
                        .queryResults(null)
                        .build();
            } else if (query.isSelectType()) {
                ResultSet res = queryExec.execSelect();
                return ValidatorResult.builder()
                        .passed(res.hasNext())
                        .queryResults(resultSetToList(res))
                        .build();
            }else{
                throw new IllegalArgumentException("Unsupported query type");
            }
        }finally {
            dataset.end();
        }

    }

    private List<Map<String, String>> resultSetToList(ResultSet resultSet) {
        List<Map<String, String>> results =new ArrayList<>();
        List<String> variables =resultSet.getResultVars();

        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            Map<String, String> row = new HashMap<>();

            for (String var : variables) {

                RDFNode node =solution.get(var);

                row.put(var, node != null ? node.toString() : null);

            }
            results.add(row);
        }
        return results;
    }
}
