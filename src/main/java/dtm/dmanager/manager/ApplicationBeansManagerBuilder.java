package dtm.dmanager.manager;

import java.util.ArrayList;
import java.util.Collection;
import dtm.dmanager.core.BeanObject;
import dtm.dmanager.core.BeansManagerBuilder;

public class ApplicationBeansManagerBuilder implements BeansManagerBuilder{

    private Collection<BeanObject> beans;

    public ApplicationBeansManagerBuilder(Collection<BeanObject> beanObjects){
        beans = beanObjects;
        if(beans == null){
            beans = new ArrayList<>();
        } 
    }

    @Override
    public BeansManagerBuilder addBean(Class<?> classBean) {
        beans.add(createBeanObject(classBean));
        return this;
    }

    @Override
    public BeansManagerBuilder addBean(Object objectBean) {
        beans.add(createBeanObject(objectBean));
        return this;
    }

    @Override
    public Collection<BeanObject> getBeans() {
        return beans;
    }
    
    private BeanObject createBeanObject(Object objectBean){
        return new BeanObject() {

            @Override
            public Object getBean() {
                return objectBean;
            }

            @Override
            public String getBeanName() {
                return (objectBean == null) ? "NullableBean" : objectBean.getClass().getName();
            }

            @Override
            public Class<?> getBeanClass() {
                return (objectBean == null) ? null : objectBean.getClass();
            }
            
        };
    }

    private BeanObject createBeanObject(Class<?> classBean){
        return new BeanObject() {

            @Override
            public Object getBean() {
                return classBean;
            }

            @Override
            public String getBeanName() {
                return (classBean == null) ? "NullableBean" : classBean.getName();
            }

            @Override
            public Class<?> getBeanClass() {
                return classBean;
            }
            
        };
    }
}
