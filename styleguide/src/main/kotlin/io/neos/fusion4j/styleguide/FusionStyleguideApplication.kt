package io.neos.fusion4j.styleguide

import io.neos.fusion4j.spring.FusionSpringConfiguration
import io.neos.fusion4j.styleguide.ui.UiWebConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@EnableConfigurationProperties
@SpringBootApplication(
    scanBasePackageClasses = [
        FusionSpringConfiguration::class,
        UiWebConfig::class
    ]
)
class FusionStyleguideApplication

fun main(args: Array<String>) {
    runApplication<FusionStyleguideApplication>(*args)
}
