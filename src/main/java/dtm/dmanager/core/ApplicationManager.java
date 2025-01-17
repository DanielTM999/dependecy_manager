package dtm.dmanager.core;

public interface ApplicationManager {
    void run();
    void run(Class<?> mainClass);
    BeansManagerBuilder getBeansManagerBuilder();
    DependencyManager getDependencyManager();
}
