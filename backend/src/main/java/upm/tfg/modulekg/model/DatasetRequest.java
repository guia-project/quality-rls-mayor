package upm.tfg.modulekg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatasetRequest {
    private String name;

    private String endpointUrl;
}
