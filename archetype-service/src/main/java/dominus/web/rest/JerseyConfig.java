package dominus.web.rest;

import dominus.web.rest.endpoint.EchoEndpoint;
import dominus.web.rest.endpoint.GroovyScriptEndpoint;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import javax.ws.rs.ApplicationPath;


@Component
@ApplicationPath("/rest")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
//        register(HelloWorldEndpoint.class);
        register(EchoEndpoint.class);
        register(GroovyScriptEndpoint.class);
    }

}
