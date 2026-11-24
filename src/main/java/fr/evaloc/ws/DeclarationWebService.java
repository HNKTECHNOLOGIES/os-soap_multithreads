package fr.evaloc.ws;

import fr.evaloc.ws.generated.RechercherDeclarationsRequest;
import fr.evaloc.ws.generated.RechercherDeclarationsResponse;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * Contrat SEI JAX-WS de l'OS (aligné sur declaration.wsdl).
 * Les types de messages sont générés Contract First (package {@code fr.evaloc.ws.generated}).
 */
@WebService(
        name = "DeclarationWebService",
        targetNamespace = "http://evaloc.fr/declarations",
        wsdlLocation = "META-INF/wsdl/declaration.wsdl"
)
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface DeclarationWebService {

    @WebMethod(operationName = "rechercherDeclarations",
            action = "http://evaloc.fr/declarations/rechercherDeclarations")
    @WebResult(name = "rechercherDeclarationsResponse",
            targetNamespace = "http://evaloc.fr/declarations",
            partName = "parameters")
    RechercherDeclarationsResponse rechercherDeclarations(
            @WebParam(name = "rechercherDeclarationsRequest",
                    targetNamespace = "http://evaloc.fr/declarations",
                    partName = "parameters")
            RechercherDeclarationsRequest request);
}
