package dtm.dmanager.manager;

import dtm.dmanager.core.DependencyResultGet;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DependencyResultGetStorage implements DependencyResultGet{

    private Object dependency;
    private Class<?> dependencyClass;

    @Override
    public boolean exists() {
        return dependency != null;
    }

    @Override
    public Object getDependency() {
       return dependency;
    }

    @Override
    public Class<?> getDepenedencyClass() {
        return dependencyClass;
    }

    @Override
    public String toString() {
        return "DependencyResultGetStorage [dependency=" + dependency + ", dependencyClass=" + dependencyClass + "]";
    }

    
}
