package dtm.dmanager.core;

public interface DependencyResultGet {
    boolean exists();
    Object getDependency();
    Class<?> getDepenedencyClass();
}
