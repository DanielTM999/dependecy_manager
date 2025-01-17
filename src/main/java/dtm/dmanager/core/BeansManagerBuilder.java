package dtm.dmanager.core;

import java.util.Collection;

public interface BeansManagerBuilder {
    BeansManagerBuilder addBean(Class<?> classBean);
    BeansManagerBuilder addBean(Object objectBean);
    Collection<BeanObject> getBeans();
}
