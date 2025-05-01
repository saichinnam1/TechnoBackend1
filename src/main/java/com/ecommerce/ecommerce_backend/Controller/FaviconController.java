package com.ecommerce.ecommerce_backend.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaviconController {

    private static final Logger logger = LoggerFactory.getLogger(FaviconController.class);

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> handleFavicon() {
        logger.debug("Handling favicon.ico request");
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}