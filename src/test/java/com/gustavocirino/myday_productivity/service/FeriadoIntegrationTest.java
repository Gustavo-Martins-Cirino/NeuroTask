package com.gustavocirino.myday_productivity.service;

import com.gustavocirino.myday_productivity.dto.FeriadoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class FeriadoIntegrationTest {

    @Autowired
    private FeriadoService feriadoService;

    @Test
    void deveBuscarFeriadosDoAnoAtual() {
        String anoAtual = String.valueOf(Year.now().getValue());

        List<FeriadoDTO> feriados = feriadoService.buscarFeriados(anoAtual);

        assertNotNull(feriados);
        assertFalse(feriados.isEmpty());

        FeriadoDTO primeiro = feriados.get(0);
        assertNotNull(primeiro.date());
        assertFalse(primeiro.date().isBlank());
        assertNotNull(primeiro.name());
        assertFalse(primeiro.name().isBlank());
    }
}
