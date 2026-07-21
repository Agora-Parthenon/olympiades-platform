package org.olympiades.platform.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ControllerPaths.GAME)
public class GameController {

    @GetMapping(ControllerPaths.INFO)
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "name", "Olympiades",
            "description", "Backend platform service for online board games",
            "version", "0.0.1",
            "modes", new String[] {"multiplayer", "solo", "tournament"}
        ));
    }
}
