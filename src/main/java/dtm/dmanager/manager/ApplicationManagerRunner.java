package dtm.dmanager.manager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dtm.discovery.core.ClassFinder;
import dtm.discovery.finder.ClassFinderService;
import dtm.dmanager.core.ApplicationManager;
import dtm.dmanager.core.BeansManagerBuilder;
import dtm.dmanager.core.DependencyManager;
import dtm.dmanager.core.DependencyResultGet;
import dtm.dmanager.core.annotations.Bootable;
import dtm.dmanager.core.annotations.FactoryMethod;
import dtm.dmanager.core.annotations.Setup;
import dtm.dmanager.exceptions.ApplicationManagerInitializeException;
import lombok.Getter;

public class ApplicationManagerRunner implements ApplicationManager{
    private static ApplicationManagerRunner applicationManager;

    private final ClassFinder classFinder;

    @Getter
    private DependencyManager dependencyManager;

    private Set<Class<?>> applicationClasses; 

    private BeansManagerBuilder beansManagerBuilder;

    private ApplicationManagerRunner(){
        this.classFinder = new ClassFinderService();
        applicationClasses = new HashSet<>();
        beansManagerBuilder = new ApplicationBeansManagerBuilder(new ArrayList<>());
    }

    private ApplicationManagerRunner(ClassFinder classFinder){
        this.classFinder = classFinder;
        applicationClasses = new HashSet<>();
        beansManagerBuilder = new ApplicationBeansManagerBuilder(new ArrayList<>());
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
        List<Object> beans = getInjectableBeans();
        injectBean(beans);
        dependencyManager.initialize(bootableClass);
        DependencyResultGet dependencyResultGet = dependencyManager.getDependency(bootableClass);
        canExecute(dependencyResultGet);
        Object intanceRunner = dependencyResultGet.getDependency();
        executeMethod(intanceRunner, bootableMethod);
    }

    @Override
    public BeansManagerBuilder getBeansManagerBuilder(){
        return beansManagerBuilder;
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

    public static void runApplication(){
        ApplicationManagerRunner.getApplicationManager().run();
    }

    public static void runApplication(Class<?> mainClass){
        ApplicationManagerRunner.getApplicationManager().run(mainClass);
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

    private List<Method> getFactoryMethod(Class<?>  setupClass){
        try {
            return Arrays.stream(setupClass.getMethods()).filter(m -> m.isAnnotationPresent(FactoryMethod.class) && m.getParameterCount() == 0).toList();
        }catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void createDependencyManager(){
        dependencyManager = new DependencyManagerApplication(this.classFinder, applicationClasses);
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
           throw new ApplicationManagerInitializeException("Bootable method can not Execute: "+e.getMessage(), e);
        }
    }

    private List<Object> getInjectableBeans(){
        BeansManagerBuilder beansManagerBuilder = getBeansManagerBuilder();
        List<Object> beans = new ArrayList<>();
        List<Class<?>> setupClassList = applicationClasses.parallelStream().filter(c -> c.isAnnotationPresent(Setup.class)).toList();
        for (Class<?> setupClass : setupClassList) {
            Object instance = dependencyManager.doCreate(setupClass);
            List<Method> methodsFactories = getFactoryMethod(setupClass);

            for (Method method : methodsFactories) {
                try {
                    if (!method.canAccess(instance)) {
                        method.setAccessible(true);
                    }
    
                    Object result = method.invoke(instance);
    

                    if (result != null) {
                        beans.add(result);
                    }
    
                } catch (Exception e) {
                    
                }
            }
        }

        beans.addAll(beansManagerBuilder.getBeans().parallelStream().map(b -> b.getBean()).toList());
        return beans;
    }

    private void injectBean(List<Object> beans){
        for (Object object : beans) {
            dependencyManager.addDependency(object);
        }
        dependencyManager.addDependency(classFinder);
    }
}
