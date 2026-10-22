package com.lab.school.controller;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

final class Locations {

    private Locations() {
    }

    /**
     * URI созданного ресурса для заголовка Location (текущий путь + /{id}).
     */
    static URI of(Long id) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    }
}
