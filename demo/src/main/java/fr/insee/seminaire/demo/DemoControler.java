package fr.insee.seminaire.demo;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.time.Period;

import static java.util.concurrent.TimeUnit.DAYS;

@Controller /*pas de RestController pour pouvoir manipuler l'entête*/
public record DemoControler (FridgeClient fridgeClient){

    @GetMapping("/steak/{origine}")
    public ResponseEntity<Steak> getSteak(@PathVariable("origine") String origine){
        return this.fridgeClient.findSteakFromFridge(origine).map(steak->
                ResponseEntity.ok()
                        .cacheControl(this.steakHache(steak))
                        .body(steak)
        ).orElse(ResponseEntity.notFound().build());
    }

    private CacheControl steakHache(Steak steak) {
        var period= Period.between(LocalDate.now(), steak.peremption());
        return period.isNegative()?
                CacheControl.noCache():
                CacheControl.maxAge(period.getDays(), DAYS).mustRevalidate();
    }


}
