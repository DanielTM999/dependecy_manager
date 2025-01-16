package dtm.dmanager.test;

import dtm.dmanager.core.annotations.Bootable;
import dtm.dmanager.core.annotations.Inject;

@Bootable
public class Run {
    
    @Inject(qualifier = "Service1Imple")
    private Service1 service1;

    public void initialize(){
        service1.teste1();
    }

    @Override
    public String toString() {
        return "Run [service1=" + service1 + "]";
    }

}
