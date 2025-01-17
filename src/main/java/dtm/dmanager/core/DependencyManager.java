package dtm.dmanager.core;


import java.util.List;

import dtm.dmanager.enums.DependencyCreatorType;
import dtm.dmanager.exceptions.DependencyManagerInitializeException;

public interface DependencyManager {
    void initialize() throws DependencyManagerInitializeException;
    void initialize(Class<?>... classToAdd) throws DependencyManagerInitializeException;

    boolean isInitialized();

    void addDependency(Object dependency);
    void addDependency(Object dependency, DependencyCreatorType strategy);
    void addDependency(Object dependency, DependencyCreatorType strategy, String qualifier);
    void addDependency(Class<? extends Object> dependency);

    DependencyResultGet getDependency(Class<? extends Object> dependencyToCreate, String qualifier);
    DependencyResultGet getDependency(Class<? extends Object> dependencyToCreate);

    List<String> getDependencyNameList();

    <T> T doCreate(Class<? extends T> reference);
}
