package com.bica.web.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {"/", "/{path:^(?!api|static|assets|.*\\..*).*$}/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
