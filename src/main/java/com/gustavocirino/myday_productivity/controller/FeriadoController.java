package com.gustavocirino.myday_productivity.controller;

import com.gustavocirino.myday_productivity.dto.FeriadoDTO;
import com.gustavocirino.myday_productivity.service.FeriadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/feriados")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FeriadoController {

    private final FeriadoService feriadoService;

    @GetMapping
    public List<FeriadoDTO> listar(@RequestParam(required = false) String ano) {
        String anoAlvo = (ano == null || ano.isBlank())
                ? String.valueOf(Year.now().getValue())
                : ano;

        return feriadoService.buscarFeriados(anoAlvo);
    }
}
