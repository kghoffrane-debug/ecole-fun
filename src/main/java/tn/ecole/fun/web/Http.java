package tn.ecole.fun.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Réponse unique pour « cet élément n'existe pas OU appartient à une autre école » :
 * on ne révèle jamais l'existence d'une donnée d'une autre école.
 */
public final class Http {
    private Http() {
    }

    public static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Introuvable");
    }
}