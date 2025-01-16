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

    private Map<Class<?>, Map<String, DependencyManagerCreator>> dependencyMap;
    
    private Set<Class<?>> applicationClasses; 

    private Set<Class<?>> applicationDependencyClasses; 

    @Getter
    private boolean initialized;

    public DependencyManagerApplication(ClassFinder classFinder, Set<Class<?>> applicationClasses) {
        this.classFinder = classFinder;
        this.applicationClasses = applicationClasses;
        applicationDependencyClasses = new HashSet<>();
        dependencyMap = new ConcurrentHashMap<>();
    }

    public DependencyManagerApplication(ClassFinder classFinder) {
        this.classFinder = classFinder;
        applicationClasses = new HashSet<>();
        applicationDependencyClasses = new HashSet<>(); 
        dependencyMap = new ConcurrentHashMap<>();
    }

    @Override
    public void initialize() throws DependencyManagerInitializeException {
        initialize(new Class[0]);
    }

    @Override
    public void initialize(Class<?>... clazzs) {
        applicationDependencyClasses.addAll(Arrays.asList(clazzs));
        findDependency();
        autoInject();
        for (Class<?> applicationDependencyClasses : applicationDependencyClasses) {
            createDependencyFunctionInject(applicationDependencyClasses);
        }
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
        qualifier = (qualifier == null || qualifier.isEmpty()) ? "default" : qualifier;
        Class<?> clazzBase = dependency.getClass();
        List<Class<?>> interfaces = getInterfaceByClass(clazzBase);
        interfaces.add(0, clazzBase);

        for (Class<?> clazz : interfaces) {
            Map<String, DependencyManagerCreator> mapDependencyNode = dependencyMap.getOrDefault(clazz, new HashMap<>());
            DependencyManagerCreator creatorManagerCreator = mapDependencyNode.getOrDefault(mapDependencyNode, new DependencyManagerCreator(strategy, true, clazz));
            creatorManagerCreator.setCreatorAction((list) -> {
                return dependency;
            });
            mapDependencyNode.put(qualifier, creatorManagerCreator);
            dependencyMap.put(clazz, mapDependencyNode);
        }
    }

    @Override
    public void addDependency(Class<? extends Object> dependency) {
        applicationDependencyClasses.add(dependency);
    }

    @Override
    public DependencyResultGet getDependency(Class<? extends Object> dependencyToCreate){
        return getDependency(dependencyToCreate, "default");
    }

    @Override
    public DependencyResultGet getDependency(Class<? extends Object> dependencyToCreate, String qualifier) {
        qualifier = (qualifier == null || qualifier.isEmpty()) ? "default" : qualifier;
        Map<String, DependencyManagerCreator> mapDependencyNode = dependencyMap.getOrDefault(dependencyToCreate, new HashMap<>());
        DependencyManagerCreator creatorManagerCreatorTemp = mapDependencyNode.getOrDefault(qualifier, null);
        if(creatorManagerCreatorTemp == null){
            creatorManagerCreatorTemp = mapDependencyNode.getOrDefault("default", null);
            if (creatorManagerCreatorTemp == null && !mapDependencyNode.isEmpty()) {
                creatorManagerCreatorTemp = mapDependencyNode.values().stream().findFirst().orElse(null);
            }
        }


        final DependencyManagerCreator creatorManagerCreator = creatorManagerCreatorTemp;
        return new DependencyResultGet() {

            @Override
            public boolean exists() {
                return creatorManagerCreator != null 
                    &&  creatorManagerCreator.getCreatorAction() != null;
            }

            @Override
            public Object getDependency() {
                if(exists()){

                    return 
                        (creatorManagerCreator.getCreatorAction() == null) 
                        ? null 
                        : creatorManagerCreator.getCreatorAction().apply(null);
                }
                return null;
            }

            @Override
            public Class<?> getDepenedencyClass() {
                if(exists()){
                    return (creatorManagerCreator.getDependencyClass() == null)
                    ? dependencyToCreate
                    : creatorManagerCreator.getDependencyClass();
                }
                return dependencyToCreate; 
            }
            
        };
    }
   
    @Override
    public List<String> getDependencyNameList() {
        return dependencyMap.keySet().stream().map(c -> c.getName()).toList();
    }

    private void findDependency(){
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

    private void createDependencyFunctionInject(Class<?> dependency){
        if(isInstantiable(dependency)){
            DependencyResultGet resultGet = getDependency(dependency);
            if(!resultGet.exists()){
                String qualifier = "default";
                DependencyCreatorType strategy = DependencyCreatorType.SINGLETON;
                if(dependency.isAnnotationPresent(Injectable.class)){
                    Injectable injectable = dependency.getAnnotation(Injectable.class);
                    qualifier = injectable.qualifier();
                    strategy = injectable.createStrategy();
                }
                
                List<Class<?>> interfaces = getInterfaceByClass(dependency);
                injectInDependencyMap(dependency, dependency, qualifier, strategy);  
                
                for (Class<?> clazz : interfaces) {
                    injectInDependencyMap(clazz, dependency, qualifier, strategy);
                }
            }
        }
    }

    private void injectInDependencyMap(Class<?> dependencyID, Class<?> dependencyClass, String qualifier, DependencyCreatorType strategy){
        Map<String, DependencyManagerCreator> mapDependencyNode = dependencyMap.getOrDefault(dependencyID, new HashMap<>());
        boolean onlyConstructor = dependecyCreateOnlyContructor(dependencyClass);
        DependencyManagerCreator creatorManagerCreator = mapDependencyNode.getOrDefault(mapDependencyNode, new DependencyManagerCreator(strategy, onlyConstructor, dependencyClass));
        resolveDependency(dependencyClass, onlyConstructor);
        if(strategy == DependencyCreatorType.SINGLETON){
            final Object intance = createDependencyObject(dependencyClass, onlyConstructor);
            creatorManagerCreator.setCreatorAction((list) -> {
                return intance;
            });
        }else{
            creatorManagerCreator.setCreatorAction((list) -> {
                return createDependencyObject(dependencyClass, onlyConstructor);
            });
        }

        mapDependencyNode.put(qualifier, creatorManagerCreator);
        dependencyMap.put(dependencyID, mapDependencyNode);
    }

    private List<Class<?>> getInterfaceByClass(Class<?> dependency){
        Set<Class<?>> interfaces = new HashSet<>();

        interfaces.addAll(Arrays.asList(dependency.getInterfaces()));
        Class<?> superClass = dependency.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            interfaces.add(superClass);
            interfaces.addAll(Arrays.asList(superClass.getInterfaces())); 
            superClass = superClass.getSuperclass();
        }

        return new ArrayList<>(interfaces);
    }

    private boolean isInstantiable(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        return !clazz.isInterface() &&  constructors.length > 0;
    }

    private boolean dependecyCreateOnlyContructor(Class<?> dependency){

        for (Field field : dependency.getDeclaredFields()) {
            if(field.isAnnotationPresent(Inject.class)){
                return false;
            }
        }

        return true;
    }

    private void resolveDependency(Class<?> dependencyClass, boolean onlyConstructor){
        
        try {     
            if (onlyConstructor) {
                Constructor<?> constructor = getMinContructor(dependencyClass.getConstructors());
                Class<?>[] parametersType = constructor.getParameterTypes();

                for (Class<?> class1 : parametersType) {
                    DependencyResultGet get = getDependency(class1);
                    if(!get.exists()){
                        if(isInstantiable(class1)){
                            createDependencyFunctionInject(class1);
                        }
                    }
                }

            }else{
                Constructor<?> constructor = getMinContructor(dependencyClass.getConstructors());
                Class<?>[] parametersType = constructor.getParameterTypes();
                List<Field> fields = Arrays.asList(dependencyClass.getDeclaredFields()).parallelStream().filter(f -> f.isAnnotationPresent(Inject.class)).toList();

                for (Class<?> class1 : parametersType) {
                    DependencyResultGet get = getDependency(class1);
                    if(!get.exists()){
                        if(isInstantiable(class1)){
                            createDependencyFunctionInject(class1);
                        }
                    }
                }

                for (Field field : fields) {
                    Inject inject = field.getAnnotation(Inject.class);
                    Class<?> fieldType = field.getType();
                    DependencyResultGet get = getDependency(fieldType, inject.qualifier());
                    if(!get.exists()){
                        if(isInstantiable(fieldType)){
                            createDependencyFunctionInject(fieldType);
                        }
                    }
                }

            }
        } catch (Exception e) {
           
        }
    }

    private Object createDependencyObject(Class<?> dependencyClass, boolean onlyConstructor){       
        try {
            if (onlyConstructor){
                return createDependencyObjectByContructor(dependencyClass);
            }else{
                List<Field> fields = Arrays.asList(dependencyClass.getDeclaredFields());
                Object instance = createDependencyObjectByContructor(dependencyClass);
                fields = fields.parallelStream().filter(f -> f.isAnnotationPresent(Inject.class)).toList();

                for (Field field : fields) {
                    Inject inject = field.getAnnotation(Inject.class);
                    Object value = null;
                    if(!field.canAccess(instance)){
                        field.setAccessible(true);
                    } 
                    Class<?> fieldType = field.getType();
                    DependencyResultGet get = getDependency(fieldType, inject.qualifier());
                    if(get.exists()){
                        value = get.getDependency();
                    }
                    field.set(instance, value);
                    field.setAccessible(false);
                }

                return instance;
            }
        } catch (Exception e) {
            
        }
        return null;
    }

    private Object createDependencyObjectByContructor(Class<?> dependencyClass) throws InvocationTargetException, InstantiationException, IllegalAccessException, IllegalArgumentException{
        Constructor<?> constructor = getMinContructor(dependencyClass.getConstructors());
        Class<?>[] parametersType = constructor.getParameterTypes();
        Object[] args = new Object[parametersType.length];
        
        if(constructor.getParameterCount() > 0){

            for(int i = 0; i < parametersType.length; i++){
                Class<?> class1 = parametersType[i];
                DependencyResultGet get = getDependency(class1);
                if(get.exists()){
                    args[i] = get.getDependency();
                }else{
                    args[i] = null;
                }
            }

            return constructor.newInstance(args);
        }else{
            return constructor.newInstance();
        }
    }

    private void autoInject(){
        Map<String, DependencyManagerCreator> mapDependencyNode = dependencyMap.getOrDefault(DependencyManager.class, new HashMap<>());
        DependencyManagerCreator creatorManagerCreator = mapDependencyNode.getOrDefault(mapDependencyNode, new DependencyManagerCreator(DependencyCreatorType.SINGLETON, true, getClass()));
        final DependencyManager intance = this;
        creatorManagerCreator.setCreatorAction((list) -> {
            return intance;
        });
        mapDependencyNode.put("default", creatorManagerCreator);
        dependencyMap.put(DependencyManager.class, mapDependencyNode);
    }

    private Constructor<?> getMinContructor(Constructor<?>[] all){
        if(all == null){
            return null;
        }

        return Arrays.stream(all).sorted(Comparator.comparingInt(Constructor::getParameterCount)).findFirst().orElse(null);
    }
}
