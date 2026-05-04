package com.gustavocirino.myday_productivity.service;

import com.gustavocirino.myday_productivity.dto.FeriadoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeriadoService {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://brasilapi.com.br/api/feriados/v1")
            .build();

    public List<FeriadoDTO> buscarFeriados(String ano) {
        try {
            FeriadoDTO[] response = restClient.get()
                    .uri("/{ano}", ano)
                    .retrieve()
                    .body(FeriadoDTO[].class);

            if (response == null) {
                return Collections.emptyList();
            }

            return Arrays.asList(response);
        } catch (RestClientException ex) {
            log.warn("Falha ao consultar BrasilAPI para o ano {}", ano, ex);
            return Collections.emptyList();
        }
    }
}
