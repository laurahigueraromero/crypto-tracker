package com.cryptotracker.cryptos;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cryptos")
public class CryptoController {

    private final CryptoService cryptoService;

    public CryptoController(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @GetMapping
    public CryptoListResponse list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int perPage,
            @RequestParam(defaultValue = "usd") String currency
    ) {
        return cryptoService.getMarkets(page, perPage, currency);
    }
}
