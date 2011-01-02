package org.smartfrog.services.anubis.partition.test.mainconsole;

import org.smartfrog.services.anubis.partition.diagnostics.console.DiagnosticConsoleConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ControllerConfiguration extends DiagnosticConsoleConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(ControllerConfiguration.class);
    } 
    @Override
    public int node() {
        return 3;
    }
}
