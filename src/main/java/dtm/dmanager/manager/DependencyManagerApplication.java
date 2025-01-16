package dtm.dmanager.manager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import dtm.discovery.core.ClassFinder;
import dtm.discovery.core.ClassFinderConfigurations;
import dtm.dmanager.core.DependencyManager;
import dtm.dmanager.core.DependencyResultGet;
import dtm.dmanager.core.annotations.Inject;
import dtm.dmanager.core.annotations.Injectable;
import dtm.dmanager.enums.DependencyCreatorType;
import dtm.dmanager.exceptions.DependencyManagerInitializeException;
import lombok.Getter;

public class DependencyManagerApplication implements DependencyManager{

    private final ClassFinder classFinder;

    private Map<Class<?>, Map<String, DependencyManagerStorage>> dependencyMap;
    private Map<Class<?>, Object> singletonCache;
    
    private Set<Class<?>> applicationClasses; 

    private Set<Class<?>> applicationDependencyClasses; 

    @Getter
    private boolean initialized;

    public DependencyManagerApplication(ClassFinder classFinder, Set<Class<?>> applicationClasses) {
        this.classFinder = classFinder;
        this.applicationClasses = applicationClasses;
        applicationDependencyClasses = new HashSet<>();
        dependencyMap = new ConcurrentHashMap<>();
        singletonCache = new ConcurrentHashMap<>();
    }

    public DependencyManagerApplication(ClassFinder classFinder) {
        this.classFinder = classFinder;
        applicationClasses = new HashSet<>();
        applicationDependencyClasses = new HashSet<>(); 
        dependencyMap = new ConcurrentHashMap<>();
        singletonCache = new ConcurrentHashMap<>();
    }

    @Override
    public void initialize() throws DependencyManagerInitializeException {
        initialize(new Class[0]);
    }

    @Override
    public void initialize(Class<?>... clazzs) {
        findServices();
        applicationDependencyClasses.addAll(Arrays.asList(clazzs));
        for (Class<?> clazz : applicationDependencyClasses) {
            addInDependecyMap(clazz);
        }
        defineActivatorFuntions();
    }

    @Override
    public void addDependency(Object dependency) {
        addDependency(dependency, DependencyCreatorType.SINGLETON);
    }

    @Override
    public void addDependency(Object dependency, DependencyCreatorType strategy){
        addDependency(dependency, strategy, "default");
    }

    @Override
    public void addDependency(Object dependency, DependencyCreatorType strategy, String qualifier){
        
    }

    @Override
    public void addDependency(Class<? extends Object> dependency) {
        applicationDependencyClasses.add(dependency);
    }

    @Override
    public DependencyResultGet getDependency(Class<? extends Object> dependencyToCreate){
        String qualifier = "default";
        if(dependencyToCreate.isAnnotationPresent(Inject.class)){
            Inject inject = dependencyToCreate.getAnnotation(Inject.class);
            qualifier = inject.qualifier();
        }
        return getDependency(dependencyToCreate, qualifier);
    }

    @Override
    public DependencyResultGet getDependency(Class<? extends Object> dependencyToCreate, String qualifier) {
        qualifier = (qualifier == null || qualifier.isEmpty()) ? "default" : qualifier;
        Map<String, DependencyManagerStorage> node = dependencyMap.getOrDefault(dependencyToCreate, new HashMap<>());
        DependencyManagerStorage dependencyStorage = node.getOrDefault(qualifier, null);

        if(dependencyStorage == null){
            dependencyStorage = node.getOrDefault("default", null);
            if (dependencyStorage == null && !node.isEmpty()) {
                dependencyStorage = node.values().stream().findFirst().orElse(null);
            }
        }
        if(dependencyStorage == null){
            return new DependencyResultGetStorage(null, dependencyToCreate);
        }

        Object dependency = (dependencyStorage.getActivationFunction() == null) ? null : dependencyStorage.getActivationFunction().get();
        return new DependencyResultGetStorage(dependency, dependencyToCreate);
    }
   
    @Override
    public List<String> getDependencyNameList() {
        return dependencyMap.keySet().stream().map(c -> c.getName()).toList();
    }

    private void findServices(){
        if(applicationClasses.isEmpty()){
            applicationDependencyClasses.addAll(classFinder.find(new ClassFinderConfigurations(){
                @Override
                public Class<? extends Annotation> getFilterByAnnotation() {
                    return Injectable.class;
                } 
            }));
        }else{
            applicationDependencyClasses.addAll(applicationClasses.parallelStream().filter(c -> c.isAnnotationPresent(Injectable.class)).toList());
        }
    }

    private void addInDependecyMap(Class<?> clazz){
        Map<String, DependencyManagerStorage> node = dependencyMap.getOrDefault(clazz, new ConcurrentHashMap<>());
        DependencyManagerStorage dependencyStorage = node.getOrDefault(node, null);

        if (dependencyStorage == null) {
            String qualifier = "default";
            DependencyCreatorType strategy = DependencyCreatorType.SINGLETON;
            if(clazz.isAnnotationPresent(Injectable.class)){
                Injectable injectable = clazz.getAnnotation(Injectable.class);
                qualifier = injectable.qualifier();
                strategy = injectable.createStrategy();
            }
            boolean containsFielInject = containsFielInject(clazz);
            dependencyStorage = new DependencyManagerStorage(strategy, containsFielInject, clazz, () -> null);
            List<Class<?>> parents = getParentClassList(clazz);
            addDependecyParent(parents, clazz, qualifier, strategy, containsFielInject);
            node.put(qualifier, dependencyStorage);
            dependencyMap.put(clazz, node);
        }

    }

    private void addDependecyParent(List<Class<?>> parentClass, Class<?> clazzBase, String qualifier, DependencyCreatorType strategy, boolean containsFielInject){
        if(parentClass != null){
            for (Class<?> parent : parentClass) {
                Map<String, DependencyManagerStorage> node = dependencyMap.getOrDefault(parent, new ConcurrentHashMap<>());
                DependencyManagerStorage dependencyStorage = node.getOrDefault(qualifier, null);
                if (dependencyStorage == null){
                    dependencyStorage = new DependencyManagerStorage(strategy, containsFielInject, clazzBase, () -> null);
                    node.put(qualifier, dependencyStorage);
                    dependencyMap.put(parent, node);
                }
            }
        }
    }

    private List<Class<?>> getParentClassList(Class<?> clazzBase){
        List<Class<?>> interfaces = new ArrayList<>();
        interfaces.addAll(Arrays.asList(clazzBase.getInterfaces()));
        Class<?> superClass = clazzBase.getSuperclass();

        while (superClass != null && superClass != Object.class) {
            interfaces.add(superClass);
            interfaces.addAll(Arrays.asList(superClass.getInterfaces())); 
            superClass = superClass.getSuperclass();
        }

        return interfaces;
    }

    private boolean containsFielInject(Class<?> dependency){

        for (Field field : dependency.getDeclaredFields()) {
            if(field.isAnnotationPresent(Inject.class)){
                return true;
            }
        }

        return false;
    }

    private void defineActivatorFuntions(){
        for (Map.Entry<Class<?>, Map<String, DependencyManagerStorage>> dependencyNode : dependencyMap.entrySet()) {
            for (Map.Entry<String, DependencyManagerStorage> dependencyElement : dependencyNode.getValue().entrySet()){
                DependencyManagerStorage managerStorage = dependencyElement.getValue();

                if(managerStorage.getCreatorStrategy() == DependencyCreatorType.SINGLETON){
                    managerStorage.setActivationFunction(() -> getByCache(managerStorage));
                }else{
                    managerStorage.setActivationFunction(() -> createDependencyObject(managerStorage));
                }
            }
        }
    }

    private Object getByCache(DependencyManagerStorage managerStorage){
        Class<?> clazz = managerStorage.getDependencyClass();
        if(!singletonCache.containsKey(clazz)){
            final Object instance = createDependencyObject(managerStorage);
            singletonCache.put(clazz, instance);
        }
        return singletonCache.get(clazz);
    }

    private Object createDependencyObject(DependencyManagerStorage managerStorage){
        try {
            Class<?> dependencyClass = managerStorage.getDependencyClass();
            Object instance = createDependencyObjectByContructor(dependencyClass);
            if (managerStorage.isContainsFielInject()){
                List<Field> fields = Arrays.asList(dependencyClass.getDeclaredFields())
                    .stream()
                    .filter(f -> f.isAnnotationPresent(Inject.class))
                    .toList();

                for (Field field : fields) {
                    Inject inject = field.getAnnotation(Inject.class);
                    Object value = null;
                    if(!field.canAccess(instance)){
                        field.setAccessible(true);
                    } 
                    Class<?> fieldType = field.getType();
                    DependencyResultGet dependency = getDependency(fieldType, inject.qualifier());
                    if(dependency.exists()){
                        value = dependency.getDependency();
                    }
                    field.set(instance, value);
                }
            }
            return instance;
        }catch(Exception e){
            return null;
        }
    }

    private Object createDependencyObjectByContructor(Class<?> dependencyClass) throws InvocationTargetException, InstantiationException, IllegalAccessException, IllegalArgumentException{
        Constructor<?> constructor = getMinArgsContructor(dependencyClass.getConstructors());
        Class<?>[] parametersType = constructor.getParameterTypes();
        Object[] args = new Object[parametersType.length];
        if(constructor.getParameterCount() > 0){
            for(int i = 0; i < parametersType.length; i++){
                Class<?> class1 = parametersType[i];
                DependencyResultGet dependencyResultGet = getDependency(class1);
                if(dependencyResultGet.exists()){
                    args[i] = dependencyResultGet.getDependency();
                }else{
                    args[i] = null;
                }
            }
        }
        return constructor.newInstance();
    }

    private Constructor<?> getMinArgsContructor(Constructor<?>[] all){
        if(all == null){
            return null;
        }

        return Arrays.stream(all).sorted(Comparator.comparingInt(Constructor::getParameterCount)).findFirst().orElse(null);
    }

    public void showTeste(){
        for (Map.Entry<Class<?>, Map<String, DependencyManagerStorage>> entry : dependencyMap.entrySet()) {
            System.out.println("Classe: " + entry.getKey().getName());
            for (Map.Entry<String, DependencyManagerStorage> innerEntry : entry.getValue().entrySet()) {
                DependencyManagerStorage storage = innerEntry.getValue();
                Object object = storage.getActivationFunction().get();
                System.out.println("  Chave: " + innerEntry.getKey());
                System.out.println("    Creator Strategy: " + storage.getCreatorStrategy());
                System.out.println("    Contains Field Inject: " + storage.isContainsFielInject());
                System.out.println("    Dependency Class: " + (storage.getDependencyClass() != null ? storage.getDependencyClass().getName() : "N/A"));
                System.out.println("    Activation Function: " + (storage.getActivationFunction() != null ? storage.getActivationFunction().toString() : "N/A"));
                System.out.println("    Object Present: " + (storage.getActivationFunction() != null ? (object == null) ? "N/A" : object : "N/A"));
                System.out.println("------------------------------------------------");
            }
        }
    }

}
