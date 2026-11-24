package fr.evaloc.mapper;

import fr.evaloc.dto.DeclarationDTO;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.dto.SearchResult;
import fr.evaloc.ws.generated.Declaration;
import fr.evaloc.ws.generated.DeclarationList;
import fr.evaloc.ws.generated.RechercherDeclarationsRequest;
import fr.evaloc.ws.generated.RechercherDeclarationsResponse;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;
/**
 * Mapping entre objets SOAP générés (JAXB) et DTO métier.
 */
public class SoapDeclarationMapper {

    private final DatatypeFactory datatypeFactory;

    public SoapDeclarationMapper() {
        try {
            this.datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Impossible d'initialiser DatatypeFactory", e);
        }
    }

    public SearchCriteria toCriteria(RechercherDeclarationsRequest request) {
        SearchCriteria criteria = new SearchCriteria();
        if (request == null) {
            return criteria;
        }
        criteria.setIdFiche(request.getIdFiche());
        criteria.setIdDeclaFonciere(request.getIdDeclaFonciere());
        criteria.setCdDept(request.getCdDept());
        criteria.setCdCommune(request.getCdCommune());
        criteria.setInvariant(request.getInvariant());
        criteria.setIdLocal(request.getIdLocal());
        criteria.setCritereTri(request.getCritereTri());
        criteria.setSensTri(request.getSensTri());

        if (request.getEtats() != null && request.getEtats().getValeur() != null) {
            criteria.setEtats(request.getEtats().getValeur());
        }
        if (request.getTypesLocaux() != null && request.getTypesLocaux().getValeur() != null) {
            criteria.setTypesLocaux(request.getTypesLocaux().getValeur());
        }
        if (request.getNumeroPage() != null) {
            criteria.setNumeroPage(request.getNumeroPage().intValue());
        }
        if (request.getTaillePage() != null) {
            criteria.setTaillePage(request.getTaillePage().intValue());
        }
        return criteria;
    }

    public RechercherDeclarationsResponse toResponse(SearchResult result) {
        RechercherDeclarationsResponse response = new RechercherDeclarationsResponse();
        DeclarationList list = new DeclarationList();
        if (result != null) {
            for (DeclarationDTO dto : result.getDeclarations()) {
                list.getDeclaration().add(toSoapDeclaration(dto));
            }
            response.setNombreTotal(result.getNombreTotal());
            response.setNombrePages(result.getNombrePages());
            response.setNumeroPage(result.getNumeroPage());
            response.setTaillePage(result.getTaillePage());
        } else {
            response.setNombreTotal(0L);
            response.setNombrePages(0);
            response.setNumeroPage(1);
            response.setTaillePage(0);
        }
        response.setDeclarations(list);
        return response;
    }

    public Declaration toSoapDeclaration(DeclarationDTO dto) {
        Declaration declaration = new Declaration();
        if (dto == null) {
            return declaration;
        }
        declaration.setIdDeclaFonciere(dto.getIdDeclaFonciere());
        declaration.setEtat(dto.getEtat());
        declaration.setCdDept(dto.getCdDept());
        declaration.setCdCommune(dto.getCdCommune());
        declaration.setCdPrefix(dto.getCdPrefix());
        declaration.setCdSection(dto.getCdSection());
        declaration.setNoPlan(dto.getNoPlan());
        declaration.setDtChangement(toXmlDate(dto.getDtChangement()));
        declaration.setTypeLocal(dto.getTypeLocal());
        declaration.setIdLocal(dto.getIdLocal());
        declaration.setInvariant(dto.getInvariant());
        declaration.setIdFiche(dto.getIdFiche());
        declaration.setDtCreation(toXmlDate(dto.getDtCreation()));
        return declaration;
    }

    private XMLGregorianCalendar toXmlDate(Date date) {
        if (date == null) {
            return null;
        }
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return datatypeFactory.newXMLGregorianCalendar(calendar);
    }
}
