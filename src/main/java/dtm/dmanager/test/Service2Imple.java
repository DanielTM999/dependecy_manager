package dtm.dmanager.test;

import dtm.dmanager.core.annotations.Injectable;
import dtm.dmanager.enums.DependencyCreatorType;

@Injectable(createStrategy = DependencyCreatorType.PROTOTYPE, qualifier = "Service2Imple")
public class Service2Imple implements Service2{

    @Override
    public void teste2() {
       System.out.println("executando servico 2");
    }
    
}
