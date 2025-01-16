package dtm.dmanager.test;

import dtm.dmanager.core.annotations.Inject;
import dtm.dmanager.core.annotations.Injectable;
import dtm.dmanager.enums.DependencyCreatorType;

@Injectable(createStrategy = DependencyCreatorType.PROTOTYPE, qualifier = "Service1Imple")
public class Service1Imple implements Service1{

    @Inject(qualifier = "Service2Imple")
    private Service2 service2;

    @Override
    public void teste1() {
        service2.teste2();
    }
    
}
