package edu.upc.caminstech.equipstic.client.dao;

import java.net.URI;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;

import edu.upc.caminstech.equipstic.Categoria;
import edu.upc.caminstech.equipstic.client.Response;

public class CategoriaDaoImpl extends RestDao implements CategoriaDao {

    public CategoriaDaoImpl(URI baseUri, RestTemplate restTemplate) {
        super(baseUri, restTemplate);
    }

    @Override
    @Cacheable(CacheUtils.PREFIX + "getCategories")
    public List<Categoria> getCategories() {
        List<Categoria> result = get("/categoria", new ParameterizedTypeReference<Response<List<Categoria>>>() {
        });
        return sorted(result);
    }

    @Override
    @Cacheable(CacheUtils.PREFIX + "getCategoriaById")
    public Categoria getCategoriaById(long idCategoria) {
        return get("/categoria/{id}", new ParameterizedTypeReference<Response<Categoria>>() {
        }, idCategoria);
    }

}
