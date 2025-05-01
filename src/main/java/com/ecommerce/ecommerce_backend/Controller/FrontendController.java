package com.ecommerce.ecommerce_backend.Controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping(value = "/{path:[^\\.]*}")
    public String redirect() {

        return "forward:/index.html";
    }
}