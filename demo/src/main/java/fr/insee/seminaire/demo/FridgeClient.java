package fr.insee.seminaire.demo;

import lombok.NonNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name="fridge")
public interface FridgeClient {


    @GetMapping("/{origine}")
    Optional<Steak> findSteakFromFridge(@NonNull @PathVariable("origine") String origine);
}
