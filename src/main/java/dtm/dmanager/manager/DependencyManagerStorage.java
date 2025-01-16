package dtm.dmanager.manager;


import java.util.function.Supplier;
import dtm.dmanager.enums.DependencyCreatorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DependencyManagerStorage {
    private DependencyCreatorType creatorStrategy;
    private boolean containsFielInject;
    private Class<?> dependencyClass;
    private Supplier<Object> activationFunction;
}
