package dtm.dmanager.manager;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dtm.discovery.core.ClassFinder;
import dtm.discovery.finder.ClassFinderService;
import dtm.dmanager.core.ApplicationManager;
import dtm.dmanager.core.DependencyManager;
import dtm.dmanager.core.DependencyResultGet;
import dtm.dmanager.core.annotations.Bootable;
import dtm.dmanager.exceptions.ApplicationManagerInitializeException;
import lombok.Getter;

@SuppressWarnings("unused")
public class ApplicationManagerRunner implements ApplicationManager{
    private static ApplicationManagerRunner applicationManager;

    private final ClassFinder classFinder;

    @Getter
    private DependencyManager dependencyManager;

    private Set<Class<?>> applicationClasses; 


    private ApplicationManagerRunner(){
        this.classFinder = new ClassFinderService();
        applicationClasses = new HashSet<>();
    }

    private ApplicationManagerRunner(ClassFinder classFinder){
        this.classFinder = classFinder;
        applicationClasses = new HashSet<>();
    }

    @Override
    public void run() {
        run(null);
    }

    @Override
    public void run(Class<?> mainClass) {
        populateApplicationClasses(mainClass);
        Class<?> bootableClass = getBootableClass().orElseThrow(() -> new ApplicationManagerInitializeException("Bootable class not found"));
        Method bootableMethod = getBootableMethod(bootableClass).orElseThrow(() -> new ApplicationManagerInitializeException("Bootable method not found"));
        createDependencyManager();
        dependencyManager.initialize(bootableClass);
        DependencyResultGet dependencyResultGet = dependencyManager.getDependency(bootableClass);
        canExecute(dependencyResultGet);
        Object intanceRunner = dependencyResultGet.getDependency();
        executeMethod(intanceRunner, bootableMethod);
    }

    public static ApplicationManager getApplicationManager(ClassFinder classFinder){
        if(applicationManager == null){
            if(classFinder == null){
                applicationManager = new ApplicationManagerRunner();
            }else{
                applicationManager = new ApplicationManagerRunner(classFinder);
            }
        }
        return applicationManager;
    }

    public static ApplicationManager getApplicationManager(){
        return getApplicationManager(null);
    }


    private Optional<Class<?>> getBootableClass(){
        return applicationClasses.parallelStream()
        .filter(bc -> bc.isAnnotationPresent(Bootable.class))
        .findFirst();
    }

    private Optional<Method> getBootableMethod(Class<?> bootableClass){
        try {
            Bootable bootable = bootableClass.getAnnotation(Bootable.class);
            if (bootable == null) {
                return Optional.empty(); 
                
            }

            return Arrays.stream(bootableClass.getDeclaredMethods())
                .filter(methodByClass -> methodByClass.getName().equals(bootable.methodName()))
                .sorted(Comparator.comparingInt(Method::getParameterCount))
            .findFirst();

        } catch (Exception e) {
            return Optional.empty();
        }
        
    }

    private void createDependencyManager(){
        if(dependencyManager == null){
            dependencyManager = new DependencyManagerApplication(this.classFinder, applicationClasses);
        }
    }

    private void populateApplicationClasses(Class<?> mainClass){
        if(mainClass == null){
            applicationClasses = classFinder.find();
        }else{
            applicationClasses = classFinder.find(mainClass);
        }
    }

    private void canExecute(DependencyResultGet dependencyResultGet){
        if (!dependencyResultGet.exists()) {
            throw new ApplicationManagerInitializeException("Bootable class can not be created");
        }else if(dependencyResultGet.getDependency() == null){
            throw new ApplicationManagerInitializeException("Bootable class can not be created");
        }
    }

    private void executeMethod(Object bootableobject, Method bootableMethod){
        try {
            Class<?>[] parameterTypes = bootableMethod.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++){
                Class<?> paramType = parameterTypes[i];
                DependencyResultGet get = dependencyManager.getDependency(paramType);
                if(get.exists()){
                    args[i] = get.getDependency();
                }else{
                    args[i] = null;
                }
            }


            bootableMethod.invoke(bootableobject, args);
        } catch (Exception e) {
           throw new ApplicationManagerInitializeException("Bootable method can not Execute: "+e.getMessage());
        }
    }
}
