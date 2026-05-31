package upm.tfg.moduleqr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ValidatorResult {

    private boolean passed;

    private List<Map<String, String>> queryResults;
}
