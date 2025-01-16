package dtm.dmanager.core;


import dtm.dmanager.exceptions.DependencyManagerInitializeException;

public interface DependencyManager {
    void initialize() throws DependencyManagerInitializeException;
    void initialize(Class<?>... classToAdd) throws DependencyManagerInitializeException;

    boolean isInitialized();

    void addDependency(Object dependency);
    void addDependency(Class<? extends Object> dependency);

    DependencyResultGet getDependency(Class<? extends Object> dependencyToCreate, String qualifier);
    DependencyResultGet getDependency(Class<? extends Object> dependencyToCreate);
}
