package dtm.dmanager.manager;


import java.util.List;
import java.util.function.Function;
import dtm.dmanager.enums.DependencyCreatorType;
import lombok.Data;

@Data
public class DependencyManagerCreator {
    private DependencyCreatorType creatorStrategy;
    private boolean onlyContructCreator;
    private Class<?> dependencyClass;
    private Function<List<Object>, Object> creatorAction;
    
    public DependencyManagerCreator(DependencyCreatorType creatorStrategy, boolean onlyContructCreator,
            Class<?> clazz) {
        this.creatorStrategy = creatorStrategy;
        this.onlyContructCreator = onlyContructCreator;
        this.dependencyClass = clazz;
    }

    @Override
    public String toString() {
        return "DependencyManagerCreator [creatorStrategy=" + creatorStrategy + ", onlyContructCreator="
                + onlyContructCreator + ", clazz=" + dependencyClass.getName() + "]";
    }

    
}
