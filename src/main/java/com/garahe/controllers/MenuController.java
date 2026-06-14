package com.garahe.controllers;

import com.garahe.database.MenuDAO;
import com.garahe.models.MenuItem;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuController {

    @GetMapping
    public List<MenuItem> getAllMenuItems() {
        return MenuDAO.getAllMenuItemsAsList();
    }
}
