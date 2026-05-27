package upm.tfg.moduleqr.service;

import org.apache.jena.rdf.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.StringWriter;

@Service
public class JsonToModelService {

    public Model convertJsonToModel(String jsonResults) {

        JSONObject json = new JSONObject(jsonResults);

        JSONArray bindings = json
                .getJSONObject("results")
                .getJSONArray("bindings");

        Model model = ModelFactory.createDefaultModel();

        for (int i = 0; i < bindings.length(); i++) {

            JSONObject binding = bindings.getJSONObject(i);

            Resource subject = createResource(
                    model,
                    binding.getJSONObject("s")
            );

            Property predicate = createProperty(
                    model,
                    binding.getJSONObject("p")
            );

            RDFNode object = createRdfNode(
                    model,
                    binding.getJSONObject("o")
            );

            model.add(subject, predicate, object);
        }

        return model;
    }

    public String convertModelToRdfString(Model model) {

        StringWriter writer = new StringWriter();

        model.write(writer, "TURTLE");

        return writer.toString();
    }


    private Resource createResource(Model model,JSONObject node) {
        String type = node.getString("type");
        String value = node.getString("value");

        if ("uri".equals(type)) {
            return model.createResource(value);
        } else if ("bnode".equals(type)) {
            return model.createResource(AnonId.create(value));
        }
        throw new IllegalArgumentException("Tipo de sujeto no soportado: " + type);
    }

    private Property createProperty(Model model, JSONObject node) {
        return model.createProperty(node.getString("value"));
    }

    private RDFNode createRdfNode(Model model, JSONObject node) {
        String type = node.getString("type");
        String value = node.getString("value");

        switch (type) {
            case "uri":
                return model.createResource(value);
            case "bnode":
                return model.createResource(AnonId.create(value));
            case "literal":
                if (node.has("datatype")) {
                    return model.createTypedLiteral(value, node.getString("datatype"));
                } else if (node.has("xml:lang")) {
                    return model.createLiteral(value, node.getString("xml:lang"));
                } else {
                    return model.createLiteral(value);
                }
            default:
                throw new IllegalArgumentException("Tipo de nodo no soportado: " + type);
        }
    }
}