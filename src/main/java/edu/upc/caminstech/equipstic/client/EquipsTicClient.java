package edu.upc.caminstech.equipstic.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upc.caminstech.equipstic.Ambit;
import edu.upc.caminstech.equipstic.Campus;
import edu.upc.caminstech.equipstic.Categoria;
import edu.upc.caminstech.equipstic.Edifici;
import edu.upc.caminstech.equipstic.Estat;
import edu.upc.caminstech.equipstic.Infraestructura;
import edu.upc.caminstech.equipstic.Marca;
import edu.upc.caminstech.equipstic.TipusInfraestructura;
import edu.upc.caminstech.equipstic.TipusUs;
import edu.upc.caminstech.equipstic.TipusXarxa;
import edu.upc.caminstech.equipstic.Unitat;

/**
 * Aquesta és la classe que implementa el client.
 * <p>
 * S'han seguit les següents convencions:
 * <ul>
 * <li>Les operacions que retornen llistes, mai no retornen {@code null}: si no
 * hi ha elements, retornen una llista buida.</li>
 * <li>Les operacions que retornen objectes, retornen {@code null} si l'objecte
 * no existeix.</li>
 * </ul>
 * <p>
 * Exemple d'utilització:
 * <p>
 * <code>
 * URI uri = URI.create("https://example.com/paht_to_api"); //veure manual SOA <br>
 * EquipsTicClient client = new EquipsTicClient(uri, "soa_user", "soa_password")); <br>
 * List&lt;Campus&gt; campus = client.getCampus();
 * </code>
 * <p>
 * Les operacions poden llançar una excepció {@link HttpServerErrorException} en
 * cas de problemes en intentar accedir al servidor SOA.
 */
public class EquipsTicClient {

    private final String baseUri;
    private final RestTemplate restTemplate;

    /**
     * Crea una nova instància del client.
     *
     * @param baseUri
     *            la URL de la API, tal com indica la documentació del bus SOA.
     *            Es pot fer servir tant la URL de desenvolupament com la de
     *            producció.
     * @param username
     *            el nostre usuari al bus SOA (ha de tenir accés a la API).
     * @param password
     *            la nostra contrasenya al bus SOA.
     */
    public EquipsTicClient(URI baseUri, String username, String password) {
        this.baseUri = baseUri.toString();
        HttpClient httpClient = prepareHttpClient(baseUri, username, password);
        restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        fixSupportedMediaTypes(restTemplate);
    }

    /**
     * Mètode auxiliar per instanciar un HttpClient a partir de les credencials
     * d'autenticació.
     */
    private HttpClient prepareHttpClient(URI baseUri, String username, String password) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        AuthScope authScope = new AuthScope(baseUri.getHost(), baseUri.getPort());
        Credentials credentials = new UsernamePasswordCredentials(username, password);
        credsProvider.setCredentials(authScope, credentials);
        HttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

        return httpClient;
    }

    /**
     * Reconfigura els Converters de la RestTemplate per tal que acceptin també
     * respostes de tipus "text/plain".
     *
     * Això és necessari perquè el servidor SOA retorna erròniament un
     * Content-Type amb valor "text/plain" quan hauria de ser
     * "application/json".
     */
    private void fixSupportedMediaTypes(RestTemplate restTemplate) {
        for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
                jsonConverter.setObjectMapper(new ObjectMapper());
                jsonConverter.setSupportedMediaTypes(Arrays.asList(
                        new MediaType("application", "json", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET),
                        new MediaType("text", "plain", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET)));
            }
        }
    }

    /**
     * Retorna els àmbits existents.
     */
    @Cacheable("ambits")
    public List<Ambit> getAmbits() {
        List<Ambit> result = get("/ambit", new ParameterizedTypeReference<Response<List<Ambit>>>() {
        });
        return (result != null) ? result : new ArrayList<Ambit>();
    }

    /**
     * Cerca d'àmbits per nom.
     */
    @Cacheable("ambits")
    public List<Ambit> getAmbitsByNom(String nomAmbit) {
        if (nomAmbit == null) {
            throw new IllegalArgumentException("El nom de l'àmbit no pot ser null");
        }
        List<Ambit> result = get("/ambit/cerca/nom/{nom}", new ParameterizedTypeReference<Response<List<Ambit>>>() {
        }, nomAmbit);
        return (result != null) ? result : new ArrayList<Ambit>();
    }

    /**
     * Retorna l'àmbit amb l'identificador donat.
     */
    @Cacheable("ambits")
    public Ambit getAmbitById(long idAmbit) {
        return get("/ambit/{id}", new ParameterizedTypeReference<Response<Ambit>>() {
        }, idAmbit);
    }

    /**
     * Retorna tots els campus existents.
     */
    @Cacheable("campus")
    public List<Campus> getCampus() {
        List<Campus> result = get("/campus", new ParameterizedTypeReference<Response<List<Campus>>>() {
        });
        return (result != null) ? result : new ArrayList<Campus>();
    }

    /**
     * Retorna el campus amb el codi donat.
     */
    @Cacheable("campus")
    public Campus getCampusByCodi(String codiCampus) {
        if (codiCampus == null) {
            throw new IllegalArgumentException("El codi del campus no pot ser null");
        }
        return get("/campus/cerca/codi/{codi}", new ParameterizedTypeReference<Response<Campus>>() {
        }, codiCampus);
    }

    /**
     * Retorna el campus amb l'identificador donat.
     */
    @Cacheable("campus")
    public Campus getCampusById(long idCampus) {
        return get("/campus/{id}", new ParameterizedTypeReference<Response<Campus>>() {
        }, idCampus);
    }

    /**
     * Retorna totes les categories existents.
     */
    @Cacheable("categories")
    public List<Categoria> getCategories() {
        List<Categoria> result = get("/categoria", new ParameterizedTypeReference<Response<List<Categoria>>>() {
        });
        return (result != null) ? result : new ArrayList<Categoria>();
    }

    /**
     * Retorna una categoria a partir del seu identificador.
     */
    @Cacheable("categories")
    public Categoria getCategoriaById(long idCategoria) {
        return get("/categoria/{id}", new ParameterizedTypeReference<Response<Categoria>>() {
        }, idCategoria);
    }

    /**
     * Retorna tots els edificis existents.
     */
    @Cacheable("edificis")
    public List<Edifici> getEdificis() {
        List<Edifici> result = get("/edifici", new ParameterizedTypeReference<Response<List<Edifici>>>() {
        });
        return (result != null) ? result : new ArrayList<Edifici>();
    }

    /**
     * Retorna un edifici a partir del seu identificador.
     */
    @Cacheable("edificis")
    public Edifici getEdificiById(long idEdifici) {
        return get("/edifici/{id}", new ParameterizedTypeReference<Response<Edifici>>() {
        }, idEdifici);
    }

    /**
     * Retorna un edifici a partir d'un codi d'edifici i un codi de campus.
     */
    @Cacheable("edificis")
    public Edifici getEdificiByCodiAndCodiCampus(String codiEdifici, String codiCampus) {
        if (codiEdifici == null) {
            throw new IllegalArgumentException("El codi de l'edifici no pot ser null");
        }
        if (codiCampus == null) {
            throw new IllegalArgumentException("El codi del campus no pot ser null");
        }
        return get("/edifici/cerca/codi/{codi}/codicampus/{codiCampus}",
                new ParameterizedTypeReference<Response<Edifici>>() {
                }, codiEdifici, codiCampus);
    }

    /**
     * Retorna tots els estats existents.
     */
    @Cacheable("estats")
    public List<Estat> getEstats() {
        List<Estat> result = get("/estat", new ParameterizedTypeReference<Response<List<Estat>>>() {
        });
        return (result != null) ? result : new ArrayList<Estat>();
    }

    /**
     * Retorna un estat a partir del seu codi.
     */
    @Cacheable("estats")
    public Estat getEstatByCodi(String codiEstat) {
        if (codiEstat == null) {
            throw new IllegalArgumentException("El codi de l'estat no pot ser null");
        }
        return get("/estat/cerca/codi/{codi}", new ParameterizedTypeReference<Response<Estat>>() {
        }, codiEstat);
    }

    /**
     * Cerca d'estats a partir d'un nom.
     */
    @Cacheable("estats")
    public List<Estat> getEstatsByNom(String nomEstat) {
        if (nomEstat == null) {
            throw new IllegalArgumentException("El nom de l'estat no pot ser null");
        }
        List<Estat> result = get("/estat/cerca/nom/{nom}", new ParameterizedTypeReference<Response<List<Estat>>>() {
        }, nomEstat);
        return (result != null) ? result : new ArrayList<Estat>();
    }

    /**
     * Retorna un estat a partir del seu identificador.
     */
    @Cacheable("estats")
    public Estat getEstatById(long idEstat) {
        return get("/estat/{id}", new ParameterizedTypeReference<Response<Estat>>() {
        }, idEstat);
    }

    /**
     * Retorna totes les marques existents.
     */
    @Cacheable("marques")
    public List<Marca> getMarques() {
        List<Marca> result = get("/marca", new ParameterizedTypeReference<Response<List<Marca>>>() {
        });
        return (result != null) ? result : new ArrayList<Marca>();
    }

    /**
     * Cerca de marques pel nom.
     */
    @Cacheable("marques")
    public List<Marca> getMarquesByNom(String nom) {
        if (nom == null) {
            throw new IllegalArgumentException("El nom de la marca no pot ser null");
        }
        List<Marca> result = get("/marca/cerca/nom/{nom}", new ParameterizedTypeReference<Response<List<Marca>>>() {
        }, nom);
        return (result != null) ? result : new ArrayList<Marca>();
    }

    /**
     * Retorna una marca a partir del seu identificador.
     */
    @Cacheable("marques")
    public Marca getMarcaById(long idMarca) {
        return get("/marca/{id}", new ParameterizedTypeReference<Response<Marca>>() {
        }, idMarca);
    }

    /**
     * Retorna tots els tipus d'ús existents.
     */
    @Cacheable("tipusUs")
    public List<TipusUs> getTipusUs() {
        List<TipusUs> result = get("/tipusUs", new ParameterizedTypeReference<Response<List<TipusUs>>>() {
        });
        return (result != null) ? result : new ArrayList<TipusUs>();
    }

    /**
     * Cerca tipus d'ús a partir d'una unitat.
     */
    @Cacheable("tipusUs")
    public List<TipusUs> getTipusUsByUnitat(long idUnitat) {
        List<TipusUs> result = get("/tipusUs/cerca/unitat/{idUnitat}",
                new ParameterizedTypeReference<Response<List<TipusUs>>>() {
                }, idUnitat);
        return (result != null) ? result : new ArrayList<TipusUs>();
    }

    /**
     * Retorna un tipus d'ús a partir del seu identificador.
     */
    @Cacheable("tipusUs")
    public TipusUs getTipusUsById(long idTipusUs) {
        return get("/tipusUs/{idTipusUs}", new ParameterizedTypeReference<Response<TipusUs>>() {
        }, idTipusUs);
    }

    /**
     * Retorna tots els tipus d'infraestructura existents.
     */
    @Cacheable("tipusInfraestructura")
    public List<TipusInfraestructura> getTipusInfraestructura() {
        List<TipusInfraestructura> result = get("/tipusInfraestructura",
                new ParameterizedTypeReference<Response<List<TipusInfraestructura>>>() {
                });
        return (result != null) ? result : new ArrayList<TipusInfraestructura>();
    }

    /**
     * Cerca de tipus d'infraestructura per categoria.
     */
    @Cacheable("tipusInfraestructura")
    public List<TipusInfraestructura> getTipusInfraestructuraBycategoria(long idCategoria) {
        List<TipusInfraestructura> result = get("/tipusInfraestructura/cerca/categoria/{idCategoria}",
                new ParameterizedTypeReference<Response<List<TipusInfraestructura>>>() {
                }, idCategoria);
        return (result != null) ? result : new ArrayList<TipusInfraestructura>();
    }

    /**
     * Retorna un tipus d'infraestructura a partir del seu codi.
     */
    @Cacheable("tipusInfraestructura")
    public TipusInfraestructura getTipusInfraestructuraBycodi(String codi) {
        if (codi == null) {
            throw new IllegalArgumentException("El codi del tipus no pot ser null");
        }
        return get("/tipusInfraestructura/cerca/codi/{codi}",
                new ParameterizedTypeReference<Response<TipusInfraestructura>>() {
                }, codi);
    }

    /**
     * Cerca de tipus d'infraestructura a partir del seu nom.
     */
    @Cacheable("tipusInfraestructura")
    public List<TipusInfraestructura> getTipusInfraestructuraByNom(String nom) {
        if (nom == null) {
            throw new IllegalArgumentException("El nom del tipus no pot ser null");
        }
        List<TipusInfraestructura> result = get("/tipusInfraestructura/cerca/nom/{nom}",
                new ParameterizedTypeReference<Response<List<TipusInfraestructura>>>() {
                }, nom);
        return (result != null) ? result : new ArrayList<TipusInfraestructura>();
    }

    /**
     * Retorna un tipus d'infraestructura a partir del seu identificador.
     */
    @Cacheable("tipusInfraestructura")
    public TipusInfraestructura getTipusInfraestructuraById(long idTipus) {
        return get("/tipusInfraestructura/{id}", new ParameterizedTypeReference<Response<TipusInfraestructura>>() {
        }, idTipus);
    }

    /**
     * Retorna tots els tipus de xarxa existents.
     */
    @Cacheable("tipusXarxa")
    public List<TipusXarxa> getTipusXarxa() {
        List<TipusXarxa> result = get("/tipusXarxa", new ParameterizedTypeReference<Response<List<TipusXarxa>>>() {
        });
        return (result != null) ? result : new ArrayList<TipusXarxa>();
    }

    /**
     * Retorna un tipus de xarxa a partir del seu identificador.
     */
    @Cacheable("tipusXarxa")
    public TipusXarxa getTipusXarxaById(long idTipusXarxa) {
        return get("/tipusXarxa/{id}", new ParameterizedTypeReference<Response<TipusXarxa>>() {
        }, idTipusXarxa);
    }

    /**
     * Retorna totes les unitats existents.
     */
    @Cacheable("unitats")
    public List<Unitat> getUnitats() {
        List<Unitat> result = get("/unitat", new ParameterizedTypeReference<Response<List<Unitat>>>() {
        });
        return (result != null) ? result : new ArrayList<Unitat>();
    }

    /**
     * Retorna una unitat a partir de l'identificador (sigles UPC).
     * <p>
     * No s'ha de confondre amb el mètode {@link #getUnitatById(long)}. Aquest
     * mètode cerca a partir de l'atribut "identificador" de la classe
     * {@link Unitat}, que correspón a les sigles de la unitat, per exemple
     * "ETSECCPB".
     *
     * @see {@link Unitat#getIdentificador()}
     */
    @Cacheable("unitats")
    public Unitat getUnitatByIdentificador(String identificador) {
        if (identificador == null) {
            throw new IllegalArgumentException("L'identificador de la unitat no pot ser null");
        }

        return get("/unitat/cerca/identificador/{identificador}", new ParameterizedTypeReference<Response<Unitat>>() {
        }, identificador);
    }

    /**
     * Cerca d'unitats a partir del nom.
     */
    @Cacheable("unitats")
    public List<Unitat> getUnitatsByNom(String nom) {
        if (nom == null) {
            throw new IllegalArgumentException("El nom de la unitat no pot ser null");
        }
        List<Unitat> result = get("/unitat/cerca/nom/{nom}", new ParameterizedTypeReference<Response<List<Unitat>>>() {
        }, nom);
        return (result != null) ? result : new ArrayList<Unitat>();
    }

    /**
     * Cerca d'unitats a partir del nom, l'identificador i el codi.
     */
    @Cacheable("unitats")
    public List<Unitat> getUnitatsByNomAndIdentificadorAndCodi(String nom, String identificador, String codiUnitat) {
        if (nom == null) {
            throw new IllegalArgumentException("El nom de la unitat no pot ser null");
        }
        if (identificador == null) {
            throw new IllegalArgumentException("L'identificador de la unitat no pot ser null");
        }
        if (codiUnitat == null) {
            throw new IllegalArgumentException("El codi de la unitat no pot ser null");
        }

        List<Unitat> result = get("/unitat/cerca/nom/{nom}/identificador/{identificador}/codi/{codi}",
                new ParameterizedTypeReference<Response<List<Unitat>>>() {
                }, nom, identificador, codiUnitat);
        return (result != null) ? result : new ArrayList<Unitat>();
    }

    /**
     * Retorna una unitat a partir de l'identificador intern EquipsTIC.
     * 
     * @param idUnitat
     *            l'identificador intern d'unitat (ull: no és el mateix que el
     *            codi d'unitat que fa servir la UPC).
     */
    @Cacheable("unitats")
    public Unitat getUnitatById(long idUnitat) {
        return get("/unitat/{id}", new ParameterizedTypeReference<Response<Unitat>>() {
        }, idUnitat);
    }

    /**
     * Retorna una infraestructura a partir de la marca i el número de sèrie.
     * 
     * @param idMarca
     *            l'identificador de la marca.
     * @param sn
     *            el número de sèrie.
     */
    public Infraestructura getInfraestructuraByMarcaAndNumeroDeSerie(long idMarca, String sn) {
        if (sn == null) {
            throw new IllegalArgumentException("El número de sèrie no pot ser null");
        }
        Infraestructura i = get("/infraestructura/cerca/marca/{idMarca}/sn/{sn}",
                new ParameterizedTypeReference<Response<Infraestructura>>() {
                }, idMarca, sn);
        ompleCampsNoInicialitzatsInfraestructura(i);
        return i;
    }

    public Infraestructura getInfraestructuraById(long id) {
        Infraestructura i = get("/infraestructura/{id}", new ParameterizedTypeReference<Response<Infraestructura>>() {
        }, id);
        ompleCampsNoInicialitzatsInfraestructura(i);
        return i;
    }

    /**
     * Inicialitza els atributs d'una infraestructura que corresponen a objectes
     * JSON que només tenen inicialitzat l'identificador.
     * 
     * @param infra
     *            la infraestructura
     */
    private void ompleCampsNoInicialitzatsInfraestructura(Infraestructura infra) {
        Marca marca = getMarcaById(infra.getMarca().getIdMarca());
        TipusInfraestructura tipusInfraestructura = getTipusInfraestructuraById(
                infra.getTipusInfraestructura().getIdTipus());
        Estat estat = getEstatById(infra.getEstat().getIdEstat());
        Unitat unitat = getUnitatById(infra.getUnitat().getIdUnitat());
        Edifici edifici = getEdificiById(infra.getEdifici().getIdEdifici());
        Estat estatValidacio = getEstatById(infra.getEstatValidacio().getIdEstat());
        Unitat unitatGestora = getUnitatById(infra.getUnitatGestora().getIdUnitat());

        infra.setMarca(marca);
        infra.setTipusInfraestructura(tipusInfraestructura);
        infra.setEstat(estat);
        infra.setUnitat(unitat);
        infra.setEdifici(edifici);
        infra.setEstatValidacio(estatValidacio);
        infra.setUnitatGestora(unitatGestora);
    }

    /**
     * Mètode auxiliar que encapsula crides GET a la API, via
     * {@link RestTemplate}.
     */
    private <T> T get(String url, ParameterizedTypeReference<Response<T>> typeReference, Object... urlParams) {
        String uri = baseUri + url;
        ResponseEntity<Response<T>> entity = restTemplate.exchange(uri, HttpMethod.GET, null, typeReference, urlParams);

        Response<T> response = entity.getBody();
        RecursNoTrobatException.throwIf(isResourceNotFound(response), response.getMessage());
        return (response != null) ? response.getData() : null;
    }

    /**
     * Comprova si la {@link Response} està indicant que no s'ha trobat el
     * recurs. Això és un <em>workaround</em> ja que la API d'EquipsTIC no
     * retorna 404 en cas que el recurs no existeixi (retorna un 200, i indica
     * l'error al estat i al missatge, dins el body).
     */
    private <T> boolean isResourceNotFound(Response<T> response) {
        return (response != null) && "fail".equals(response.getStatus())
                && StringUtils.containsIgnoreCase(response.getMessage(), "no existeix");
    }
}
