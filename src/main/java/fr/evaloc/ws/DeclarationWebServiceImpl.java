package fr.evaloc.ws;

import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.dto.SearchResult;
import fr.evaloc.exception.OsException;
import fr.evaloc.mapper.SoapDeclarationMapper;
import fr.evaloc.service.DeclarationService;
import fr.evaloc.util.AppProperties;
import fr.evaloc.util.CorrelationId;
import fr.evaloc.util.TimeBudget;
import fr.evaloc.ws.generated.RechercherDeclarationsRequest;
import fr.evaloc.ws.generated.RechercherDeclarationsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;

/**
 * Implémentation synchrone de l'opération SOAP {@code rechercherDeclarations}.
 */
@WebService(
        endpointInterface = "fr.evaloc.ws.DeclarationWebService",
        serviceName = "DeclarationWebServiceService",
        portName = "DeclarationWebServicePort",
        targetNamespace = "http://evaloc.fr/declarations",
        wsdlLocation = "META-INF/wsdl/declaration.wsdl"
)
public class DeclarationWebServiceImpl implements DeclarationWebService {

    private static final Logger LOG = LoggerFactory.getLogger(DeclarationWebServiceImpl.class);

    private final DeclarationService declarationService;
    private final SoapDeclarationMapper mapper;
    private final long osTimeoutMs;

    public DeclarationWebServiceImpl(DeclarationService declarationService,
                                     SoapDeclarationMapper mapper,
                                     AppProperties props) {
        this.declarationService = declarationService;
        this.mapper = mapper;
        this.osTimeoutMs = props.getLong("os.timeout.ms", 2500L);
    }

    @Override
    public RechercherDeclarationsResponse rechercherDeclarations(
            RechercherDeclarationsRequest request) {
        String correlationId = CorrelationId.generate();
        CorrelationId.set(correlationId);
        TimeBudget budget = new TimeBudget(osTimeoutMs);
        long start = System.nanoTime();
        try {
            LOG.info("Début appel SOAP rechercherDeclarations");
            SearchCriteria criteria = mapper.toCriteria(request);
            SearchResult result = declarationService.rechercherDeclarations(criteria, budget);
            RechercherDeclarationsResponse response = mapper.toResponse(result);
            long totalMs = (System.nanoTime() - start) / 1_000_000L;
            LOG.info("Fin appel SOAP OK totalMs={} nombreTotal={} page={}/{}",
                    totalMs, result.getNombreTotal(),
                    result.getNumeroPage(), result.getTaillePage());
            return response;
        } catch (OsException e) {
            long totalMs = (System.nanoTime() - start) / 1_000_000L;
            LOG.warn("Fin appel SOAP KO code={} totalMs={} msg={}",
                    e.getFaultCode(), totalMs, e.getMessage());
            throw SoapFaultFactory.toFault(e);
        } catch (RuntimeException e) {
            long totalMs = (System.nanoTime() - start) / 1_000_000L;
            LOG.error("Fin appel SOAP KO technique totalMs={}", totalMs);
            throw SoapFaultFactory.toFault(e);
        } finally {
            CorrelationId.clear();
        }
    }
}
